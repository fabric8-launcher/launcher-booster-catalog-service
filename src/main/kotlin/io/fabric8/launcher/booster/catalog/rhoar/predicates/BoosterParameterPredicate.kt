package io.fabric8.launcher.booster.catalog.rhoar.predicates

import java.util.Objects
import java.util.function.Predicate

import io.fabric8.launcher.booster.catalog.Booster
import org.apache.commons.beanutils.PropertyUtils

/**
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
class BoosterParameterPredicate(private val parameters: Map<String, List<String>>) : Predicate<Booster> {

    override fun test(booster: Booster): Boolean {
        var result = true
        for ((path, values) in parameters) {
            val expectedValue = if (!values[0].isEmpty()) values[0] else "true"
            if (!expectedValue.equals(getValueByPath(booster, path)!!, ignoreCase = true)) {
                result = false
                break
            }
        }
        return result
    }

    private fun getValueByPath(b: Booster, path: String): String? {
        var target: Any = b
        try {
            target = PropertyUtils.getNestedProperty(target, path)
        } catch (ignored: Exception) {
            return "false"
        }

        return Objects.toString(target, "false")
    }

}
