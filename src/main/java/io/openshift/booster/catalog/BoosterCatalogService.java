/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import static io.openshift.booster.Files.removeFileExtension;
import static io.openshift.booster.catalog.BoosterFilters.ignored;
import static io.openshift.booster.catalog.BoosterFilters.missions;
import static io.openshift.booster.catalog.BoosterFilters.runtimes;
import static io.openshift.booster.catalog.BoosterFilters.versions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import io.openshift.booster.CopyFileVisitor;
import io.openshift.booster.catalog.spi.BoosterCatalogListener;
import io.openshift.booster.catalog.spi.BoosterCatalogPathProvider;
import io.openshift.booster.catalog.spi.LocalBoosterCatalogPathProvider;
import io.openshift.booster.catalog.spi.NativeGitBoosterCatalogPathProvider;

/**
 * This service reads from the Booster catalog Github repository in https://github.com/openshiftio/booster-catalog and
 * marshalls into {@link Booster} objects.
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public class BoosterCatalogService implements BoosterCatalog, BoosterFetcher {
    /**
     * Files to be excluded from project creation
     */
    public static final List<String> EXCLUDED_PROJECT_FILES = Collections
            .unmodifiableList(Arrays.asList(".git", ".travis", ".travis.yml",
                                            ".ds_store",
                                            ".obsidian", ".gitmodules"));

    private BoosterCatalogService(BoosterCatalogPathProvider provider,
                                  Predicate<Booster> indexFilter,
                                  BoosterCatalogListener listener,
                                  String environment,
                                  ExecutorService executor) {
        Objects.requireNonNull(provider, "Booster catalog path provider is required");
        Objects.requireNonNull(executor, "Executor is required");
        this.provider = provider;
        this.indexFilter = indexFilter;
        this.listener = listener;
        this.environment = environment;
        this.executor = executor;
    }

    private static final String CLONED_BOOSTERS_DIR = ".boosters";

    private static final String METADATA_FILE = "metadata.json";
    
    private static final String COMMON_YAML_FILE = "common.yaml";

    private static final Logger logger = Logger.getLogger(BoosterCatalogService.class.getName());

    private volatile Set<Booster> boosters = Collections.emptySet();

    private final BoosterCatalogPathProvider provider;

    private final Predicate<Booster> indexFilter;

    private final BoosterCatalogListener listener;

    private final String environment;

    private final ExecutorService executor;

    private volatile CompletableFuture<Set<Booster>> indexResult;
    private volatile CompletableFuture<Set<Booster>> prefetchResult;

    /**
     * Indexes the existing YAML files provided by the {@link BoosterCatalogPathProvider} implementation
     */
    public synchronized CompletableFuture<Set<Booster>> index() {
        if (indexResult == null) {
            indexResult = new CompletableFuture<Set<Booster>>();
            CompletableFuture.runAsync(() -> {
                try {
                    boosters = new ConcurrentSkipListSet<>(Comparator.comparing(Booster::getId));
                    doIndex();
                    indexResult.complete(boosters);
                } catch (Exception ex) {
                    indexResult.completeExceptionally(ex);
                }
            }, executor);
        }
        return indexResult;
    }

    /**
     * Re-runs the indexing of the catalog and the boosters
     * Attention: this won't do anything if indexing is already in progress
     */
    public synchronized CompletableFuture<Set<Booster>> reindex() {
        if (indexResult != null && indexResult.isDone()) {
            indexResult = null;
        }
        return index();
    }
    
    /**
     * Pre-fetches the code for {@link Booster}s that were found when running {@link index}.
     * It's not necessary to run this because {@link Booster} code will be downloaded on
     * demand, but if you want to avoid any delays for the user you can run this method.
     */
    public synchronized CompletableFuture<Set<Booster>> prefetchBoosters() {
        assert(indexResult != null);
        if (prefetchResult == null) {
            prefetchResult = new CompletableFuture<Set<Booster>>();
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info(() -> "Pre-fetching boosters...");
                    for (Booster b : boosters) {
                        try {
                            b.content().get();
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            // We ignore errors and go on to fetch the next Booster
                            logger.log(Level.SEVERE, "Error while fetching booster '" + b.getName() + "'", e);
                        }
                    }
                    prefetchResult.complete(boosters);
                } catch (Exception ex) {
                    prefetchResult.completeExceptionally(ex);
                }
            }, executor);
        }
        return prefetchResult;
    }

    /**
     * Clones a Booster repo and provides the path where to find it as a result
     */
    @Override
    public CompletableFuture<Path> fetchBoosterContent(Booster booster) {
        synchronized (booster) {
            CompletableFuture<Path> contentResult = new CompletableFuture<>();
            Path contentPath = booster.getContentPath();
            if (Files.notExists(contentPath)) {
                try {
                    Files.createDirectories(contentPath);
                    CompletableFuture.runAsync(() -> {
                        try {
                            provider.createBoosterContentPath(booster);
                            contentResult.complete(contentPath);
                        } catch (Throwable ex) {
                            contentResult.completeExceptionally(ex);
                        }
                    }, executor);
                } catch (IOException ex) {
                    contentResult.completeExceptionally(ex);
                }
            } else {
                contentResult.complete(contentPath);
            }
            return contentResult;
        }
    }

    /**
     * Copies the {@link Booster} contents to the specified {@link Path}
     */
    @Override
    public Path copy(Booster booster, Path projectRoot) throws IOException {
        try {
            Path modulePath = booster.content().get();
            return Files.walkFileTree(modulePath,
                    new CopyFileVisitor(projectRoot,
                                        (p) -> !EXCLUDED_PROJECT_FILES.contains(p.toFile().getName().toLowerCase())));
        } catch (InterruptedException | ExecutionException ex) {
            throw new IOException("Unable to copy Booster", ex);
        }
    }

    // Return all indexed boosters, except the ones that were marked ignored
    // and the ones that don't pass the global `indexFilter`
    protected Stream<Booster> getPrefilteredBoosters() {
        return boosters.stream().filter(indexFilter).filter(ignored(false));
    }
    
    @Override
    public Set<Mission> getMissions() {
        return toMissions(getPrefilteredBoosters());
    }

    @Override
    public Set<Mission> getMissions(Predicate<Booster> filter) {
        return toMissions(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Runtime> getRuntimes() {
        return toRuntimes(getPrefilteredBoosters());
    }

    @Override
    public Set<Runtime> getRuntimes(Predicate<Booster> filter) {
        return toRuntimes(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Version> getVersions(Predicate<Booster> filter) {
        return toVersions(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Version> getVersions(Mission mission, Runtime runtime) {
        if (mission == null || runtime == null) {
            return Collections.emptySet();
        }
        return getVersions(missions(mission).and(runtimes(runtime)));
    }

    @Override
    public Optional<Booster> getBooster(Predicate<Booster> filter) {
        return getPrefilteredBoosters()
                .filter(filter)
                .findAny();
    }

    @Override
    public Optional<Booster> getBooster(Mission mission, Runtime runtime) {
        Objects.requireNonNull(mission, "Mission should not be null");
        Objects.requireNonNull(runtime, "Runtime should not be null");
        return getBooster(missions(mission).and(runtimes(runtime)));
    }

    @Override
    public Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version) {
        Objects.requireNonNull(mission, "Mission should not be null");
        Objects.requireNonNull(runtime, "Runtime should not be null");
        Objects.requireNonNull(version, "Version should not be null");
        return getPrefilteredBoosters()
                .filter(missions(mission))
                .filter(runtimes(runtime))
                .filter(versions(version))
                .findAny();
    }

    @Override
    public Collection<Booster> getBoosters() {
        return toBoosters(getPrefilteredBoosters());
    }
    
    @Override
    public Collection<Booster> getBoosters(Predicate<Booster> filter) {
        return toBoosters(getPrefilteredBoosters().filter(filter));
    }

    private Collection<Booster> toBoosters(Stream<Booster> bs) {
        return bs
                .collect(Collectors.toSet());
    }

    private Set<Runtime> toRuntimes(Stream<Booster> bs) {
        return bs
                .map(Booster::getRuntime)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Mission> toMissions(Stream<Booster> bs) {
        return bs
                .map(Booster::getMission)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Version> toVersions(Stream<Booster> bs) {
        return bs
                .filter(b -> b.getVersion() != null)
                .map(Booster::getVersion)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void doIndex() throws Exception {
        try {
            Path catalogPath = provider.createCatalogPath();
            indexBoosters(catalogPath, boosters);
            logger.info(() -> "Finished content indexing");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while indexing", e);
            throw e;
        }
    }

    private void indexBoosters(final Path catalogPath, final Set<Booster> boosters) throws IOException {
        Map<String, Mission> missions = new HashMap<>();
        Map<String, Runtime> runtimes = new HashMap<>();
        
        indexPath(catalogPath, catalogPath, new Booster(this), boosters);
        
        // Read the metadata for missions and runtimes
        Path metadataFile = catalogPath.resolve(METADATA_FILE);
        if (Files.exists(metadataFile)) {
            processMetadata(metadataFile, missions, runtimes);
        }
        
        // Update the boosters with the proper info for missions, runtimes and versions
        for (Booster booster : boosters) {
            List<String> path = booster.getMetadata("descriptor/path");
            if (path != null && !path.isEmpty()) {
                if (path.size() >= 1) {
                    booster.setMission(missions.computeIfAbsent(path.get(0), Mission::new));
                    if (path.size() >= 2) {
                        booster.setRuntime(runtimes.computeIfAbsent(path.get(1), Runtime::new));
                        if (path.size() >= 3) {
                            String versionId = path.get(2);
                            String versionName = booster.getMetadata("version/name", versionId);
                            booster.setVersion(new Version(versionId, versionName));
                        }
                    }
                }
            }
        }
        
        // Notify the listener of all the boosters that were added
        // (this excludes ignored boosters and those filtered by the global indexFilter)
        if (listener != null) {
            getPrefilteredBoosters().forEach(listener::boosterAdded);
        }
    }

    private void indexPath(final Path catalogPath, final Path path, final Booster commonBooster, final Set<Booster> boosters) {
        if (Thread.interrupted()) {
            throw new RuntimeException("Interrupted");
        }
        
        // We skip ".booster" folders, ".git" folders and "common.yaml" files
        Path moduleRoot = catalogPath.resolve(CLONED_BOOSTERS_DIR);
        if (path.startsWith(moduleRoot)
                || path.getFileName().startsWith(".git")
                || COMMON_YAML_FILE.equals(path.getFileName().toString())) {
            return;
        }
        
        try {
            File file = path.toFile();
            if (file.isDirectory()) {
                // We check if a file named `common.yaml` exists and if so
                // we merge it's data with the `commonBooster` before passing
                // that on to `indexPath()`
                File common = new File(path.toFile(), COMMON_YAML_FILE);
                final Booster activeCommonBooster;
                if (common.isFile()) {
                    Booster localCommonBooster = readBooster(common.toPath());
                    if (localCommonBooster != null) {
                        activeCommonBooster = commonBooster.merged(localCommonBooster);
                    } else {
                        activeCommonBooster = commonBooster;
                    }
                } else {
                    activeCommonBooster = commonBooster;
                }
                
                Files.list(path).forEach(subpath -> indexPath(catalogPath, subpath, activeCommonBooster, boosters));
            } else {
                File ioFile = path.toFile();
                String fileName = ioFile.getName().toLowerCase();
                // Skip any file that starts with "."
                if (!fileName.startsWith(".") && (fileName.endsWith(".yaml") || fileName.endsWith(".yml"))) {
                    String id = makeBoosterId(catalogPath, path);
                    Path modulePath = moduleRoot.resolve(id);
                    Booster b = indexBooster(commonBooster, id, catalogPath, path, modulePath);
                    if (b != null) {
                        // Check if we should get a specific environment
                        if (environment != null && !environment.isEmpty()) {
                            String[] envs = environment.split(",");
                            for (String env : envs) {
                                b = b.forEnvironment(env);
                            }
                        }
                        boosters.add(b);
                    };
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    // We take the relative path of the booster name, remove the file extension
    // and turn any path symbols into underscores to create a unique booster id.
    // Eg. "http-crud/vertx/booster.yaml" becomes "http-crud_vertx_booster"
    private String makeBoosterId(final Path catalogPath, final Path boosterPath) {
        Path relativePath = catalogPath.relativize(boosterPath);
        String pathString = removeFileExtension(relativePath.toString().toLowerCase());
        return pathString.replace('/', '_').replace('\\', '_');
    }
    
    /**
     * Takes a YAML file from the repository and indexes it
     *
     * @param file A YAML file from the booster-catalog repository
     * @return a {@link Booster} or null if the booster could not be read
     */
    protected Booster indexBooster(Booster common, String id, Path catalogPath, Path file, Path moduleDir) {
        logger.info(() -> "Indexing " + file + " ...");
        Booster booster = readBooster(file);
        if (booster != null) {
            booster = common.merged(booster);
            // Booster ID = filename without extension
            booster.setId(id);
            booster.setContentPath(moduleDir);
            
            // We set some useful values in the "metadata" section:
            
            // Information about the booster descriptor, eg if the booster descriptor
            // file was named "http/vertx/community/booster.yaml" the following will
            // be added to the metadata section:
            //    metadata:
            //      descriptor:
            //        name: booster.yaml
            //        path: [http, vertx, community]
            Map<String, Object> descriptor = new LinkedHashMap<String, Object>();
            descriptor.put("name", file.getFileName().toString());
            descriptor.put("path", getDescriptorPathList(file, catalogPath));
            booster.getMetadata().put("descriptor", descriptor);
        }
        return booster;
    }

    private Booster readBooster(Path file) {
        Representer rep = new Representer();
        rep.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new YamlConstructor(), rep);
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> boosterData = yaml.loadAs(reader, Map.class);
            return new Booster(boosterData, this);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while reading " + file, e);
            return null;
        }
    }

    private List<String> getDescriptorPathList(Path boosterPath, Path catalogPath) {
        Path relativePath = catalogPath.relativize(boosterPath);
        Path boosterDir = relativePath.getParent();
        return getPathList(boosterDir);
    }

    private List<String> getPathList(Path path) {
        if (path != null) {
            return StreamSupport.stream(path.spliterator(), false)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Process the metadataFile and adds to the specified missions and runtimes
     * maps
     */
    void processMetadata(Path metadataFile, Map<String, Mission> missions, Map<String, Runtime> runtimes) {
        logger.info(() -> "Reading metadata at " + metadataFile + " ...");

        try (BufferedReader reader = Files.newBufferedReader(metadataFile);
                JsonReader jsonReader = Json.createReader(reader)) {
            JsonObject index = jsonReader.readObject();
            index.getJsonArray("missions").stream().map(JsonObject.class::cast)
                    .map(e -> new Mission(e.getString("id"), e.getString("name"), e.getString("description", null)))
                    .forEach(m -> missions.put(m.getId(), m));

            index.getJsonArray("runtimes").stream().map(JsonObject.class::cast)
                    .map(e -> new Runtime(e.getString("id"), e.getString("name"), e.getString("icon", null)))
                    .forEach(r -> runtimes.put(r.getId(), r));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while processing metadata " + metadataFile, e);
        }
    }
    
    /**
     * {@link BoosterCatalogService} Builder class
     *
     * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
     */
    public static class Builder {

        private String catalogRepositoryURI = LauncherConfiguration.boosterCatalogRepositoryURI();

        private String catalogRef = "master";

        private Path rootDir;

        private BoosterCatalogPathProvider pathProvider;

        private Predicate<Booster> filter = x -> true;

        private BoosterCatalogListener listener;

        private String environment;
        
        private ExecutorService executor;

        public Builder catalogRef(String catalogRef) {
            this.catalogRef = catalogRef;
            return this;
        }

        public Builder catalogRepository(String catalogRepositoryURI) {
            this.catalogRepositoryURI = catalogRepositoryURI;
            return this;
        }

        public Builder pathProvider(BoosterCatalogPathProvider pathProvider) {
            this.pathProvider = pathProvider;
            return this;
        }

        public Builder filter(Predicate<Booster> filter) {
            this.filter = filter;
            return this;
        }

        public Builder listener(BoosterCatalogListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder rootDir(Path root) {
            this.rootDir = root;
            return this;
        }

        public BoosterCatalogService build() {
            assert catalogRef != null : "Catalog Ref is required";
            BoosterCatalogPathProvider provider = this.pathProvider;
            if (provider == null) {
                provider = discoverCatalogProvider();
            }
            assert provider != null : "BoosterCatalogPathProvider implementation is required";
            logger.info("Using " + provider.getClass().getName());
            if (executor == null) {
                executor = ForkJoinPool.commonPool();
            }
            return new BoosterCatalogService(provider, filter, listener, environment, executor);
        }

        private BoosterCatalogPathProvider discoverCatalogProvider() {
            final BoosterCatalogPathProvider provider;
            if (LauncherConfiguration.ignoreLocalZip()) {
                provider = new NativeGitBoosterCatalogPathProvider(catalogRepositoryURI, catalogRef, rootDir);
            } else {
                URL resource = getClass().getClassLoader()
                        .getResource(String.format("/booster-catalog-%s.zip", catalogRef));
                if (resource != null) {
                    provider = new LocalBoosterCatalogPathProvider(resource);
                } else {
                    // Resource not found, fallback to original Git resolution
                    provider = new NativeGitBoosterCatalogPathProvider(catalogRepositoryURI, catalogRef, rootDir);
                }
            }
            return provider;
        }
    }
}
