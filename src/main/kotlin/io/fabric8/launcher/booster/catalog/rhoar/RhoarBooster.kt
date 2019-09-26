package io.fabric8.launcher.booster.catalog.rhoar

import java.util.stream.Collectors

import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.BoosterFetcher
import java.util.*

class RhoarBooster(data: Map<String, Any?>, boosterFetcher: BoosterFetcher): Booster(data , boosterFetcher) {
    var mission: Mission? = null
    var runtime: Runtime? = null
    var version: Version? = null

    override val id: String by lazy {
        val m = getMetadata<String>("mission")
        val r = getMetadata<String>("runtime")
        val v = getMetadata<String>("version")
        if (m != null && r != null && v != null) {
            "$m-$r-$v"
        } else {
            super.id
        }
    }

    /**
     * @return the booster's data in easily exportable format
     */
    override val exportableData: Map<String, Any?>
        get() {
            val exp = HashMap(super.exportableData)
            if (mission != null) exp["mission"] = mission!!.id
            if (runtime != null) exp["runtime"] = runtime!!.id
            if (version != null) exp["version"] = version!!.id
            return exp
        }

    override fun newBooster(data: Map<String, Any?>) = RhoarBooster(data, boosterFetcher)

    fun runsOn(clusterType: String?): Boolean =
            clusterType == null || clusterType.isEmpty() ||
                checkCategory(toList(getMetadata<Any>("runsOn")), clusterType)

    companion object {

        /**
         * Takes an object that is either null, a single value or a list
         * of values and makes sure that the result is always a list
         * @param data An object
         * @return A list of values
         */
        fun toList(data: Any?): List<String> =
                if (data is List<*>) {
                    val list: List<String>? = data as List<String>?
                    list!!.stream()
                            .map<String>({ Objects.toString(it) })
                            .collect(Collectors.toList())
                } else if (data != null && !data.toString().isEmpty()) {
                    listOf(data.toString())
                } else {
                    emptyList()
                }

        /**
         * Check a category name against supported categories.
         * The supported categories are either a single object or a list of objects.
         * The given category is checked against the supported categories one by one.
         * If the category name matches the supported one exactly `true` is
         * returned. The supported category can also start with a `!`
         * indicating a the result should be negated. In that case `false`
         * is returned. The special supported categories `all` and
         * `none` will always return `true` and `false`
         * respectively when encountered.
         *
         * @param supportedCategories Can be a single object or a List of objects
         * @param category            The category name to check against
         * @return if the category matches the supported categories or not
         */
        @JvmStatic
        fun checkCategory(supportedCategories: List<String>, category: String): Boolean {
            var defaultResult = true
            if (!supportedCategories.isEmpty()) {
                for (supportedCategory in supportedCategories) {
                    if (!supportedCategory.startsWith("!")) {
                        defaultResult = false
                    }
                    if (supportedCategory.equals("all", ignoreCase = true)
                            || supportedCategory.equals("*", ignoreCase = true)
                            || supportedCategory.equals(category, ignoreCase = true)) {
                        return true
                    } else if (supportedCategory.equals("none", ignoreCase = true)
                            || supportedCategory.equals("!*", ignoreCase = true)
                            || supportedCategory.equals("!$category", ignoreCase = true)) {
                        return false
                    }
                }
            }
            return defaultResult
        }
    }

}
