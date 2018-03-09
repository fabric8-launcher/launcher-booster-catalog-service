package io.fabric8.launcher.booster.catalog.rhoar;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.launcher.booster.catalog.AbstractBoosterCatalogService;
import io.fabric8.launcher.booster.catalog.Booster;
import io.fabric8.launcher.booster.catalog.BoosterCatalogService;
import io.fabric8.launcher.booster.catalog.BoosterDataTransformer;
import io.fabric8.launcher.booster.catalog.BoosterFetcher;
import io.fabric8.launcher.booster.catalog.YamlConstructor;
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogListener;
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogPathProvider;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import javax.annotation.Nullable;

public class RhoarBoosterCatalogService extends AbstractBoosterCatalogService<RhoarBooster> implements RhoarBoosterCatalog {

    private static final String METADATA_FILE = "metadata.yaml";

    private static final Logger logger = Logger.getLogger(RhoarBoosterCatalogService.class.getName());

    protected RhoarBoosterCatalogService(Builder config) {
        super(config);
    }

    @Override
    protected RhoarBooster newBooster(@Nullable Map<String, Object> data, BoosterFetcher boosterFetcher) {
        return new RhoarBooster(data, boosterFetcher);
    }

    @Override
    public Set<Mission> getMissions() {
        return toMissions(getPrefilteredBoosters());
    }

    @Override
    public Set<Mission> getMissions(Predicate<RhoarBooster> filter) {
        return toMissions(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Runtime> getRuntimes() {
        return toRuntimes(getPrefilteredBoosters());
    }

    @Override
    public Set<Runtime> getRuntimes(Predicate<RhoarBooster> filter) {
        return toRuntimes(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Version> getVersions(Predicate<RhoarBooster> filter) {
        return toVersions(getPrefilteredBoosters().filter(filter));
    }

    @Override
    public Set<Version> getVersions(Mission mission, Runtime runtime) {
        return getVersions(BoosterPredicates.withMission(mission).and(BoosterPredicates.withRuntime(runtime)));
    }
    
    @Override
    public Optional<RhoarBooster> getBooster(Mission mission, Runtime runtime, Version version) {
        return getPrefilteredBoosters()
                .filter(BoosterPredicates.withMission(mission))
                .filter(BoosterPredicates.withRuntime(runtime))
                .filter(BoosterPredicates.withVersion(version))
                .findAny();
    }

    private Set<Runtime> toRuntimes(Stream<RhoarBooster> bs) {
        return bs
                .filter(b -> b.getRuntime() != null)
                .map(RhoarBooster::getRuntime)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Mission> toMissions(Stream<RhoarBooster> bs) {
        return bs
                .filter(b -> b.getMission() != null)
                .map(RhoarBooster::getMission)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<Version> toVersions(Stream<RhoarBooster> bs) {
        return bs
                .filter(b -> b.getVersion() != null)
                .map(RhoarBooster::getVersion)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    protected void indexBoosters(final Path catalogPath, final Set<RhoarBooster> boosters) throws IOException {
        super.indexBoosters(catalogPath, boosters);

        // Update the boosters with the proper info for missions, runtimes and versions
        Map<String, Mission> missions = new HashMap<>();
        Map<String, Runtime> runtimes = new HashMap<>();

        // Read the metadata for missions and runtimes
        Path metadataFile = catalogPath.resolve(METADATA_FILE);
        if (Files.exists(metadataFile)) {
            processMetadata(metadataFile, missions, runtimes);
        }

        for (RhoarBooster booster : boosters) {
            List<String> path = booster.getDescriptor().path;
            if (path.size() >= 3) {
                booster.setRuntime(runtimes.computeIfAbsent(path.get(0), Runtime::new));

                String versionId = path.get(1);
                String versionName = booster.getMetadata("version/name", versionId);
                assert versionName != null;
                String description = booster.getMetadata("version/description");
                Map<String, Object> metadata = booster.getMetadata("version/metadata", Collections.emptyMap());
                booster.setVersion(new Version(versionId, versionName, description, metadata));

                booster.setMission(missions.computeIfAbsent(path.get(2), Mission::new));
            }
        }
    }

    /**
     * Process the metadataFile and adds to the specified missions and runtimes
     * maps
     */
    @SuppressWarnings("unchecked")
    public void processMetadata(Path metadataFile, Map<String, Mission> missions, Map<String, Runtime> runtimes) {
        logger.info(() -> "Reading metadata at " + metadataFile + " ...");

        Representer rep = new Representer();
        rep.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(new YamlConstructor(), rep);
        try (BufferedReader reader = Files.newBufferedReader(metadataFile)) {
            Map<String, Object> metadata = yaml.loadAs(reader, Map.class);

            if (metadata.get("missions") instanceof List) {
                List<Map<String, Object>> ms = (List<Map<String, Object>>)metadata.get("missions");
                ms.stream()
                        .map(e -> new Mission(
                                (String)e.get("id"),
                                (String)e.get("name"),
                                (String)e.get("description"),
                                (Map<String, Object>)e.getOrDefault("metadata", Collections.emptyMap())))
                        .forEach(m -> missions.put(m.getId(), m));
            }

            if (metadata.get("runtimes") instanceof List) {
                List<Map<String, Object>> rs = (List<Map<String, Object>>)metadata.get("runtimes");
                rs.stream()
                        .map(e -> new Runtime(
                                (String)e.get("id"),
                                (String)e.get("name"),
                                (String)e.get("description"),
                                (Map<String, Object>)e.getOrDefault("metadata", Collections.emptyMap()),
                                (String)e.get("icon")))
                        .forEach(r -> runtimes.put(r.getId(), r));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while processing metadata " + metadataFile, e);
        }
    }

    public static class Builder extends AbstractBuilder<RhoarBooster, RhoarBoosterCatalogService> {
        @Override
        public Builder catalogRef(String catalogRef) {
            return (Builder)super.catalogRef(catalogRef);
        }

        @Override
        public Builder catalogRepository(String catalogRepositoryURI) {
            return (Builder)super.catalogRepository(catalogRepositoryURI);
        }

        @Override
        public Builder pathProvider(BoosterCatalogPathProvider pathProvider) {
            return (Builder)super.pathProvider(pathProvider);
        }

        @Override
        public Builder filter(Predicate<RhoarBooster> filter) {
            return (Builder)super.filter(filter);
        }

        @Override
        public Builder listener(BoosterCatalogListener listener) {
            return (Builder)super.listener(listener);
        }

        @Override
        public Builder transformer(BoosterDataTransformer transformer) {
            return (Builder)super.transformer(transformer);
        }

        @Override
        public Builder environment(String environment) {
            return (Builder)super.environment(environment);
        }

        @Override
        public Builder executor(ExecutorService executor) {
            return (Builder)super.executor(executor);
        }

        @Override
        public Builder rootDir(Path root) {
            return (Builder)super.rootDir(root);
        }

        @Override
        public RhoarBoosterCatalogService build() {
            return new RhoarBoosterCatalogService(this);
        }
    }
}
