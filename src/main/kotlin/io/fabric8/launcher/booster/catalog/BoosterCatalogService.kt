package io.fabric8.launcher.booster.catalog

import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogListener
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogPathProvider

import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.function.Predicate

class BoosterCatalogService protected constructor(config: Builder) : AbstractBoosterCatalogService<Booster>(config) {
    override fun newBooster(data: Map<String, Any?>?, boosterFetcher: BoosterFetcher): Booster {
        return Booster(data, boosterFetcher)
    }

    // No code here, it's all about the types
    class Builder : AbstractBoosterCatalogService.AbstractBuilder<Booster, BoosterCatalogService>() {
        override fun catalogRef(catalogRef: String) = super.catalogRef(catalogRef) as Builder
        override fun catalogRepository(catalogRepositoryURI: String) = super.catalogRepository(catalogRepositoryURI) as Builder
        override fun pathProvider(pathProvider: BoosterCatalogPathProvider) = super.pathProvider(pathProvider) as Builder
        override fun filter(filter: Predicate<Booster>) = super.filter(filter) as Builder
        override fun listener(listener: BoosterCatalogListener) = super.listener(listener) as Builder
        override fun transformer(transformer: BoosterDataTransformer) = super.transformer(transformer) as Builder
        override fun environment(environment: String) = super.environment(environment) as Builder
        override fun executor(executor: ExecutorService) = super.executor(executor) as Builder
        override fun rootDir(root: Path) = super.rootDir(root) as Builder
        override fun build() = BoosterCatalogService(this)
    }
}
