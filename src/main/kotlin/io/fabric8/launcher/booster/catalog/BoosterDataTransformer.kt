package io.fabric8.launcher.booster.catalog

interface BoosterDataTransformer {
    fun transform(data: Map<String, Any?>): Map<String, Any?>
}
