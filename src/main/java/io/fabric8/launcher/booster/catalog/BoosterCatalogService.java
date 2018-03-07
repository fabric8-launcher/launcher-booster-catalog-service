package io.fabric8.launcher.booster.catalog;

import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogListener;
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogPathProvider;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

public class BoosterCatalogService extends AbstractBoosterCatalogService<Booster> {

    protected BoosterCatalogService(Builder config) {
        super(config);
    }

    @Override
    protected Booster newBooster(Map<String, Object> data, BoosterFetcher boosterFetcher) {
        return new Booster(data, boosterFetcher);
    }
    
    public static class Builder extends AbstractBuilder<Booster, BoosterCatalogService> {
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
        public Builder filter(Predicate<Booster> filter) {
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
        public BoosterCatalogService build() {
            return new BoosterCatalogService(this);
        }
    }
}
