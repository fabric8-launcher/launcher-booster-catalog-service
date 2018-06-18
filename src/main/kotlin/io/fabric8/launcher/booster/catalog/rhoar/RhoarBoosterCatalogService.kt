package io.fabric8.launcher.booster.catalog.rhoar

import java.io.BufferedReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.HashMap
import java.util.Optional
import java.util.TreeSet
import java.util.concurrent.ExecutorService
import java.util.function.Predicate
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream

import io.fabric8.launcher.booster.catalog.AbstractBoosterCatalogService
import io.fabric8.launcher.booster.catalog.BoosterDataTransformer
import io.fabric8.launcher.booster.catalog.BoosterFetcher
import io.fabric8.launcher.booster.catalog.YamlConstructor
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogListener
import io.fabric8.launcher.booster.catalog.spi.BoosterCatalogPathProvider
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import java.util.function.Supplier

class RhoarBoosterCatalogService protected constructor(config: Builder) : AbstractBoosterCatalogService<RhoarBooster>(config), RhoarBoosterCatalog {

    override fun newBooster(data: Map<String, Any?>?, boosterFetcher: BoosterFetcher) = RhoarBooster(data, boosterFetcher)

    override fun getMissions() = toMissions(prefilteredBoosters)

    override fun getMissions(filter: Predicate<RhoarBooster>) = toMissions(prefilteredBoosters.filter(filter))

    override fun getRuntimes() = toRuntimes(prefilteredBoosters)

    override fun getRuntimes(filter: Predicate<RhoarBooster>) = toRuntimes(prefilteredBoosters.filter(filter))

    override fun getVersions(filter: Predicate<RhoarBooster>) = toVersions(prefilteredBoosters.filter(filter))

    override fun getVersions(mission: Mission, runtime: Runtime) = getVersions(BoosterPredicates.withMission(mission).and(BoosterPredicates.withRuntime(runtime)))

    override fun getBooster(mission: Mission, runtime: Runtime, version: Version?) =
            prefilteredBoosters
                .filter(BoosterPredicates.withMission(mission))
                .filter(BoosterPredicates.withRuntime(runtime))
                .filter(BoosterPredicates.withVersion(version))
                .findAny()

    private fun toRuntimes(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.runtime != null }
                .map<Runtime>({ it.runtime })
                .collect(Collectors.toCollection(Supplier<TreeSet<Runtime>> { TreeSet() }))

    private fun toMissions(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.mission != null }
                .map<Mission>({ it.mission })
                .collect(Collectors.toCollection(Supplier<TreeSet<Mission>> { TreeSet() }))

    private fun toVersions(bs: Stream<RhoarBooster>) =
            bs
                .filter { b -> b.version != null }
                .map<Version>({ it.version })
                .collect(Collectors.toCollection(Supplier<TreeSet<Version>> { TreeSet() }))

    @Throws(IOException::class)
    override fun indexBoosters(catalogPath: Path, boosters: MutableSet<RhoarBooster>) {
        super.indexBoosters(catalogPath, boosters)

        // Update the boosters with the proper info for missions, runtimes and versions
        val missions = HashMap<String, Mission>()
        val runtimes = HashMap<String, Runtime>()

        // Read the metadata for missions and runtimes
        val metadataFile = catalogPath.resolve(METADATA_FILE)
        if (Files.exists(metadataFile)) {
            processMetadata(metadataFile, missions, runtimes)
        }

        for (booster in boosters) {
            val path = booster.descriptor.path
            if (path.size >= 3) {
                var r: Runtime? = runtimes[path[0]]
                if (r == null) {
                    r = Runtime(path[0])
                    logger.log(Level.WARNING, "Runtime '{0}' not found in metadata", r.id)
                }
                booster.runtime = r

                var v: Version? = r.versions[path[1]]
                if (v == null) {
                    v = Version(path[1])
                    logger.log(Level.WARNING, "Version '{0}' not found in Runtime '{1}' metadata", arrayOf<Any>(v.id, r.id))
                }
                booster.version = v

                var m: Mission? = missions[path[2]]
                if (m == null) {
                    m = Mission(path[2])
                    logger.log(Level.WARNING, "Mission '{0}' not found in metadata", m.id)
                }
                booster.mission = m
            }
        }
    }

    /**
     * Process the metadataFile and adds to the specified missions and runtimes
     * maps
     */
    fun processMetadata(metadataFile: Path, missions: MutableMap<String, Mission>, runtimes: MutableMap<String, Runtime>) {
        logger.info { "Reading metadata at $metadataFile ..." }

        val rep = Representer()
        rep.propertyUtils.isSkipMissingProperties = true
        val yaml = Yaml(YamlConstructor(), rep)
        try {
            Files.newBufferedReader(metadataFile).use { reader ->
                val metadata = yaml.loadAs(reader, Map::class.java)

                if (metadata["missions"] is List<*>) {
                    val ms = metadata["missions"] as List<Map<String, Any?>>
                    ms.stream()
                            .map { e ->
                                Mission(
                                        e["id"] as String,
                                        e["name"] as String,
                                        e["description"] as String?,
                                        e.getOrDefault("metadata", emptyMap<Any, Any>()) as Map<String, Any?>)
                            }
                            .forEach { m -> missions[m.id] = m }
                }

                if (metadata["runtimes"] is List<*>) {
                    val rs = metadata["runtimes"] as List<Map<String, Any?>>
                    rs.stream()
                            .map { e ->
                                Runtime(
                                        e["id"] as String,
                                        e["name"] as String,
                                        e["description"] as String?,
                                        e.getOrDefault("metadata", emptyMap<String, Any?>()) as Map<String, Any?>,
                                        e["icon"] as String?,
                                        getMetadataVersions(e["versions"]))
                            }
                            .forEach { r -> runtimes[r.id] = r }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while processing metadata $metadataFile", e)
        }

    }

    private fun getMetadataVersions(versionsList: Any?): Map<String, Version> {
        if (versionsList is List<*>) {
            val versions = HashMap<String, Version>()
            val vs = versionsList as List<Map<String, Any?>>
            vs.stream()
                    .map { e ->
                        Version(
                                e["id"] as String,
                                e["name"] as String,
                                e["description"] as String?,
                                e.getOrDefault("metadata", emptyMap<Any, Any>()) as Map<String, Any?>)
                    }
                    .forEach { v -> versions[v.id] = v }
            return versions
        } else {
            return emptyMap()
        }
    }

    class Builder : AbstractBoosterCatalogService.AbstractBuilder<RhoarBooster, RhoarBoosterCatalogService>() {
        override fun catalogRef(catalogRef: String) = super.catalogRef(catalogRef) as Builder

        override fun catalogRepository(catalogRepositoryURI: String) = super.catalogRepository(catalogRepositoryURI) as Builder

        override fun pathProvider(pathProvider: BoosterCatalogPathProvider) = super.pathProvider(pathProvider) as Builder

        override fun filter(filter: Predicate<RhoarBooster>) = super.filter(filter) as Builder

        override fun listener(listener: BoosterCatalogListener) = super.listener(listener) as Builder

        override fun transformer(transformer: BoosterDataTransformer) = super.transformer(transformer) as Builder

        override fun environment(environment: String) = super.environment(environment) as Builder

        override fun executor(executor: ExecutorService) = super.executor(executor) as Builder

        override fun rootDir(root: Path) = super.rootDir(root) as Builder

        override fun build() = RhoarBoosterCatalogService(this)
    }

    companion object {

        private val METADATA_FILE = "metadata.yaml"

        private val logger = Logger.getLogger(RhoarBoosterCatalogService::class.java.name)
    }
}
