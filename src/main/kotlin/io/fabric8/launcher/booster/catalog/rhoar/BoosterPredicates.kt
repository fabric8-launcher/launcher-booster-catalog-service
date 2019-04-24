package io.fabric8.launcher.booster.catalog.rhoar

import java.util.function.Predicate

import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.rhoar.predicates.BoosterParameterPredicate
import io.fabric8.launcher.booster.catalog.rhoar.predicates.BoosterScriptingPredicate
import java.util.regex.Pattern

/**
 * Utility class to provide [Predicate] instances for [Booster] instances and its subtypes
 */
class BoosterPredicates private constructor() {
    init {
        throw IllegalAccessError("Utility class")
    }

    companion object {

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if its [Runtime]
         * is equal to the provided parameter
         *
         * @param runtime the [Runtime] to be compared against a given [RhoarBooster].
         * If null, always returns true;
         * @return a [Predicate] testing against the given [Runtime]
         */
        @JvmStatic
        fun withRuntime(runtime: Runtime?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            runtime == null || runtime == b.runtime
        }

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if the provided [Pattern]
         * matches the booster's [Runtime.id]
         *
         * @param version the [Pattern] to be compared against a given [RhoarBooster] version.
         * If null, always returns true;
         * @return a [Predicate] testing against the given [Version]
         */
        @JvmStatic
        fun withRuntimeMatches(runtime: Pattern?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            runtime == null || b.runtime == null || runtime.matcher(b.runtime?.id).matches()
        }

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if its [Mission]
         * is equal to the provided parameter
         *
         * @param mission the [Mission] to be compared against a given [RhoarBooster].
         * If null, always returns true;
         * @return a [Predicate] testing against the given [Mission]
         */
        @JvmStatic
        fun withMission(mission: Mission?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            mission == null || mission == b.mission
        }

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if its [Version]
         * is equal to the provided parameter
         *
         * @param version the [Version] to be compared against a given [RhoarBooster].
         * If null, always returns true;
         * @return a [Predicate] testing against the given [Version]
         */
        @JvmStatic
        fun withVersion(version: Version?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            version == null || version == b.version
        }

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if its [Pattern]
         * matches the booster's [Version.id]
         *
         * @param version the [Pattern] to be compared against a given [RhoarBooster] version.
         * If null, always returns true;
         * @return a [Predicate] testing against the given [Version]
         */
        @JvmStatic
        fun withVersionMatches(version: Pattern?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            version == null || b.version == null || version.matcher(b.version?.id).matches()
        }

        /**
         * Returns a [Predicate] for a [RhoarBooster] testing if the runsOn attribute defined inside
         * its metadata matches the provided parameter
         *
         * @param clusterType the cluster type to be compared against a given [RhoarBooster].
         * If null, always returns true;
         * @return a [Predicate] testing against the given cluster type
         * @see RhoarBooster.checkCategory
         */
        @JvmStatic
        fun withRunsOn(clusterType: String?) = Predicate<RhoarBooster> { b: RhoarBooster -> b.runsOn(clusterType) }

        /**
         * Returns a [Predicate] for a [Booster] testing if against a script expression that must be evaluated
         *
         * @param script the script expression to be tested against a given [Booster].
         * @return a [BoosterScriptingPredicate] instance
         */
        @JvmStatic
        fun <T : Booster> withScriptFilter(script: String?) =
                if (script != null)
                    BoosterScriptingPredicate(script) as Predicate<T>
                else
                    Predicate<T> { _ -> true }

        /**
         * Returns a [Predicate] for a [Booster] testing if the given parameters match.
         *
         * @param parameters The parameters belonging to the tested [Booster] instance. Should never be null
         * @return a [BoosterScriptingPredicate] instance
         * @throws NullPointerException if parameters is null
         */
        @JvmStatic
        fun <T : Booster> withParameters(parameters: Map<String, List<String>>) =
                BoosterParameterPredicate(parameters) as Predicate<T>

        /**
         * Returns a [Predicate] for a [Booster] testing if the given application argument
         * matches a property in the metadata section named "app.$application.enabled". The booster will
         * be filtered out if the property evaluated to "false".
         *
         * @param application The name of the application
         * @return a [Predicate] testing if ttthe given application is enabled
         */
        @JvmStatic
        fun withAppEnabled(application: String?) = Predicate<RhoarBooster> { b: RhoarBooster ->
            application == null || b.getMetadata("app/${application.toLowerCase()}/enabled", true)
        }
    }
}
