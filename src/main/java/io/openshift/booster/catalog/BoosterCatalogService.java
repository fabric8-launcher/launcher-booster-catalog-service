/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import io.openshift.booster.CopyFileVisitor;
import io.openshift.booster.catalog.spi.BoosterCatalogListener;
import io.openshift.booster.catalog.spi.BoosterCatalogPathProvider;
import io.openshift.booster.catalog.spi.LocalBoosterCatalogPathProvider;
import io.openshift.booster.catalog.spi.NativeGitBoosterCatalogPathProvider;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import static io.openshift.booster.Files.removeFileExtension;

/**
 * This service reads from the Booster catalog Github repository in https://github.com/openshiftio/booster-catalog and
 * marshalls into {@link Booster} objects.
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogService implements BoosterCatalog {
    /**
     * Files to be excluded from project creation
     */
    public static final List<String> EXCLUDED_PROJECT_FILES = Collections
            .unmodifiableList(Arrays.asList(".git", ".travis", ".travis.yml",
                                            ".ds_store",
                                            ".obsidian", ".gitmodules"));

    private BoosterCatalogService(BoosterCatalogPathProvider provider, Predicate<Booster> filter,
                                  BoosterCatalogListener listener,
                                  ExecutorService executor) {
        Objects.requireNonNull(provider, "Booster catalog path provider is required");
        Objects.requireNonNull(executor, "Executor is required");
        this.provider = provider;
        this.filter = filter;
        this.listener = listener;
        this.executor = executor;
    }

    private static final String CLONED_BOOSTERS_DIR = ".boosters";

    private static final String METADATA_FILE = "metadata.json";

    private static final Logger logger = Logger.getLogger(BoosterCatalogService.class.getName());

    private final Set<Booster> boosters = new ConcurrentSkipListSet<>(Comparator.comparing(Booster::getId));

    private final BoosterCatalogPathProvider provider;

    private final Predicate<Booster> filter;

    private final BoosterCatalogListener listener;

    private final ExecutorService executor;

    private final CompletableFuture<Set<Booster>> result = new CompletableFuture<>();

    private volatile boolean indexingStarted = false;

    /**
     * Indexes the existing YAML files provided by the {@link BoosterCatalogPathProvider} implementation
     */
    public synchronized CompletableFuture<Set<Booster>> index() {
        if (!indexingStarted) {
            indexingStarted = true;
            CompletableFuture.runAsync(() -> {
                try {
                    doIndex();
                    result.complete(boosters);
                } catch (Exception ex) {
                    result.completeExceptionally(ex);
                }
            }, executor);
        }
        return result;
    }

    /**
     * Copies the {@link Booster} contents to the specified {@link Path}
     */
    @Override
    public Path copy(Booster booster, Path projectRoot) throws IOException {
        Path modulePath = booster.getContentPath();
        return Files.walkFileTree(modulePath,
                                  new CopyFileVisitor(projectRoot,
                                                      (p) -> !EXCLUDED_PROJECT_FILES.contains(p.toFile().getName().toLowerCase())));
    }

    @Override
    public Set<Mission> getMissions(String... labels) {
        return selector().labels(labels).getMissions();
    }

    @Override
    public Set<Mission> getMissions(Runtime runtime, String... labels) {
        Objects.requireNonNull(runtime, "Runtime should not be null");
        return selector().runtime(runtime).labels(labels).getMissions();
    }

    @Override
    public Set<Runtime> getRuntimes(String... labels) {
        return selector().labels(labels).getRuntimes();
    }

    @Override
    public Set<Runtime> getRuntimes(Mission mission, String... labels) {
        if (mission == null) {
            return Collections.emptySet();
        }
        return selector().mission(mission).labels(labels).getRuntimes();
    }

    @Override
    public Set<Version> getVersions(Mission mission, Runtime runtime, String... labels) {
        if (mission == null || runtime == null) {
            return Collections.emptySet();
        }
        return selector().runtime(runtime).mission(mission).labels(labels).getVersions();
    }

    @Override
    public Optional<Booster> getBooster(Mission mission, Runtime runtime, String... labels) {
        return getBooster(mission, runtime, null, labels);
    }

    @Override
    public Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version, String... labels) {
        Objects.requireNonNull(mission, "Mission should not be null");
        Objects.requireNonNull(runtime, "Runtime should not be null");
        return selector().runtime(runtime).mission(mission).version(version).labels(labels).getBooster();
    }

    @Override
    public Collection<Booster> getBoosters(Runtime runtime, String... labels) {
        Objects.requireNonNull(runtime, "Runtime should not be null");
        return selector().runtime(runtime).labels(labels).getBoosters();
    }

    @Override
    public Collection<Booster> getBoosters(String... labels) {
        return selector().labels(labels).getBoosters();
    }

    @Override
    public Selector selector() {
        return new SelectorImpl();
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
        Path moduleRoot = catalogPath.resolve(CLONED_BOOSTERS_DIR);
        Path metadataFile = catalogPath.resolve(METADATA_FILE);
        Map<String, Mission> missions = new HashMap<>();
        Map<String, Runtime> runtimes = new HashMap<>();
        Map<String, Version> versions = new HashMap<>();
        if (Files.exists(metadataFile)) {
            processMetadata(metadataFile, missions, runtimes);
        }
        Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Thread.interrupted()) {
                    return FileVisitResult.TERMINATE;
                }
                File ioFile = file.toFile();
                String fileName = ioFile.getName().toLowerCase();
                // Skip any file that starts with .
                if (!fileName.startsWith(".") && (fileName.endsWith(".yaml") || fileName.endsWith(".yml"))) {
                    String id = removeFileExtension(fileName);
                    Path modulePath = moduleRoot.resolve(id);
                    indexBooster(id, catalogPath, file, modulePath, missions, runtimes, versions).ifPresent(b -> {
                        if (listener != null) {
                            listener.boosterAdded(b);
                        }
                        boosters.add(b);
                    });
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Thread.interrupted()) {
                    return FileVisitResult.TERMINATE;
                }
                return dir.startsWith(moduleRoot) || dir.getFileName().startsWith(".git")
                        ? FileVisitResult.SKIP_SUBTREE
                        : FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process the metadataFile and adds to the specified missions and runtimes maps
     *
     * @param metadataFile
     * @param missions
     * @param runtimes
     */
    void processMetadata(Path metadataFile, Map<String, Mission> missions, Map<String, Runtime> runtimes) {
        logger.info(() -> "Reading metadata at " + metadataFile + " ...");

        try (BufferedReader reader = Files.newBufferedReader(metadataFile);
             JsonReader jsonReader = Json.createReader(reader)) {
            JsonObject index = jsonReader.readObject();
            index.getJsonArray("missions")
                    .stream()
                    .map(JsonObject.class::cast)
                    .map(e -> new Mission(e.getString("id"), e.getString("name")))
                    .forEach(m -> missions.put(m.getId(), m));

            index.getJsonArray("runtimes")
                    .stream()
                    .map(JsonObject.class::cast)
                    .map(e -> new Runtime(e.getString("id"), e.getString("name")))
                    .forEach(r -> runtimes.put(r.getId(), r));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while processing metadata " + metadataFile, e);
        }
    }

    /**
     * Takes a YAML file from the repository and indexes it
     *
     * @param file A YAML file from the booster-catalog repository
     * @return an {@link Optional} containing a {@link Booster}
     */
    @SuppressWarnings("unchecked")
    private Optional<Booster> indexBooster(String id, Path catalogPath, Path file, Path moduleDir,
                                           Map<String, Mission> missions,
                                           Map<String, Runtime> runtimes, Map<String, Version> versions) {
        logger.info(() -> "Indexing " + file + " ...");
        Representer rep = new Representer();
        rep.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new YamlConstructor(), rep);
        Booster booster = null;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            // Read YAML entry
            booster = yaml.loadAs(reader, Booster.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while reading " + file, e);
        }
        if (booster != null) {
            try {
                // Booster ID = filename without extension
                booster.setId(id);

                String versionId;
                String runtimeId;
                String missionId;
                if (file.getParent().getParent().getParent().equals(catalogPath)) {
                    versionId = null;
                    runtimeId = file.getParent().toFile().getName();
                    missionId = file.getParent().getParent().toFile().getName();
                } else {
                    versionId = file.getParent().toFile().getName();
                    runtimeId = file.getParent().getParent().toFile().getName();
                    missionId = file.getParent().getParent().getParent().toFile().getName();
                }

                booster.setMission(missions.computeIfAbsent(missionId, Mission::new));
                booster.setRuntime(runtimes.computeIfAbsent(runtimeId, Runtime::new));
                if (versionId != null) {
                    booster.setVersion(versions.computeIfAbsent(versionId, Version::new));
                }
                if (filter != null && !filter.test(booster)) {
                    return Optional.empty();
                }
                booster.setContentPath(moduleDir);
                if (Files.notExists(moduleDir)) {
                    moduleDir = provider.createBoosterContentPath(booster);
                }
                Path metadataPath = moduleDir.resolve(booster.getBoosterDescriptorPath());
                try (BufferedReader metadataReader = Files.newBufferedReader(metadataPath)) {
                    Map<String, Object> metadata = yaml.loadAs(metadataReader, Map.class);
                    booster.setMetadata(metadata);

                    if (versionId != null) {
                        List<Map<String, Object>> vlist = (List<Map<String, Object>>) metadata.get("versions");
                        if (vlist != null) {
                            final Booster b = booster;
                            vlist
                                    .stream()
                                    .map(m -> new Version(Objects.toString(m.get("id")), Objects.toString(m.get("name"))))
                                    .filter(v -> b.getVersion().getId().equals(v.getId()))
                                    .forEach(v -> b.setVersion(v));
                        }
                    }
                }

                Path descriptionPath = moduleDir.resolve(booster.getBoosterDescriptionPath());
                if (Files.exists(descriptionPath)) {
                    byte[] descriptionContent = Files.readAllBytes(descriptionPath);
                    booster.setDescription(new String(descriptionContent));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while reading metadata from " + file, e);
            }
        }
        return Optional.ofNullable(booster);
    }

    /**
     * {@link BoosterCatalogService} Builder class
     *
     * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
     */
    public static class Builder {
        private String catalogRepositoryURI = "https://github.com/fabric8-launcher/launcher-booster-catalog.git";

        private String catalogRef = "master";

        private Path rootDir;

        private BoosterCatalogPathProvider pathProvider;

        private Predicate<Booster> filter;

        private BoosterCatalogListener listener;

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
            return new BoosterCatalogService(provider, filter, listener, executor);
        }

        private BoosterCatalogPathProvider discoverCatalogProvider() {
            final BoosterCatalogPathProvider provider;
            // Check if we can use a local ZIP
            boolean ignoreLocalZip = Boolean.getBoolean("BOOSTER_CATALOG_IGNORE_LOCAL")
                    || Boolean.parseBoolean(System.getenv("BOOSTER_CATALOG_IGNORE_LOCAL"));
            if (ignoreLocalZip) {
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

    /**
     * {@link BoosterCatalogService} Selector class
     *
     * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
     */
    public class SelectorImpl implements Selector {
        private DeploymentType deploymentType;

        private Runtime runtime;

        private Mission mission;

        private Version version;

        private String[] labels;

        @Override
        public Selector deploymentType(DeploymentType deploymentType) {
            this.deploymentType = deploymentType;
            return this;
        }

        @Override
        public Selector runtime(Runtime runtime) {
            this.runtime = runtime;
            return this;
        }

        @Override
        public Selector mission(Mission mission) {
            this.mission = mission;
            return this;
        }

        @Override
        public Selector version(Version version) {
            this.version = version;
            return this;
        }

        @Override
        public Selector labels(String... labels) {
            this.labels = labels;
            return this;
        }

        @Override
        public Set<Runtime> getRuntimes() {
            return filtered(boosters.stream())
                    .map(Booster::getRuntime)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        public Set<Mission> getMissions() {
            return filtered(boosters.stream())
                    .map(Booster::getMission)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        public Set<Version> getVersions() {
            return filtered(boosters.stream())
                    .filter(b -> b.getVersion() != null)
                    .map(Booster::getVersion)
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        @Override
        public Collection<Booster> getBoosters() {
            return filtered(boosters.stream())
                    .collect(Collectors.toSet());
        }

        @Override
        public Optional<Booster> getBooster() {
            return filtered(boosters.stream())
                    .findAny();
        }

        private Stream<Booster> filtered(Stream<Booster> stream) {
            if (deploymentType != null) {
                stream = stream.filter(b -> isSupported(b.getSupportedDeploymentTypes()));
            }

            if (runtime != null) {
                stream = stream.filter(b -> runtime.equals(b.getRuntime()));
            }

            if (mission != null) {
                stream = stream.filter(b -> mission.equals(b.getMission()));
            }

            if (version != null) {
                stream = stream.filter(b -> version.equals(b.getVersion()));
            }

            if (labels != null && labels.length > 0) {
                stream = stream.filter(x -> isMatchingLabels(x.getLabels()));
            }

            return stream;
        }

        private boolean isSupported(String supportedDeploymentTypes) {
            if (supportedDeploymentTypes != null && !supportedDeploymentTypes.isEmpty()) {
                String[] types = supportedDeploymentTypes.split(",");
                for (String t : types) {
                    if (t.equalsIgnoreCase(deploymentType.name())) {
                        return true;
                    }
                }
                return false;
            } else {
                return true;
            }
        }

        private boolean isMatchingLabels(Set<String> boosterLabels) {
            for (String label : labels) {
                if (!boosterLabels.contains(label)) {
                    return false;
                }
            }
            return true;
        }
    }
}
