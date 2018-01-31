package io.fabric8.launcher.booster.catalog;

import java.util.Map;

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
        public BoosterCatalogService build() {
            assert catalogRef != null : "Catalog Ref is required";
            return new BoosterCatalogService(this);
        }
    }
}
