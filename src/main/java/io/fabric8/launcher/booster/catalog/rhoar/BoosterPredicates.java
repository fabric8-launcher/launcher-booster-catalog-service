package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.Booster;
import io.fabric8.launcher.booster.catalog.rhoar.predicates.BoosterParameterPredicate;
import io.fabric8.launcher.booster.catalog.rhoar.predicates.BoosterScriptingPredicate;

/**
 * Utility class to provide {@link Predicate} instances for {@link Booster} instances and its subtypes
 */
public final class BoosterPredicates {
    private BoosterPredicates() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Returns a {@link Predicate} for a {@link RhoarBooster} testing if its {@link Runtime}
     * is equal to the provided parameter
     *
     * @param runtime the {@link Runtime} to be compared against a given {@link RhoarBooster}.
     *                If null, always returns true;
     * @return a {@link Predicate} testing against the given {@link Runtime}
     */
    public static Predicate<RhoarBooster> withRuntime(@Nullable Runtime runtime) {
        return (RhoarBooster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    /**
     * Returns a {@link Predicate} for a {@link RhoarBooster} testing if its {@link Mission}
     * is equal to the provided parameter
     *
     * @param mission the {@link Mission} to be compared against a given {@link RhoarBooster}.
     *                If null, always returns true;
     * @return a {@link Predicate} testing against the given {@link Mission}
     */
    public static Predicate<RhoarBooster> withMission(@Nullable Mission mission) {
        return (RhoarBooster b) -> mission == null || mission.equals(b.getMission());
    }

    /**
     * Returns a {@link Predicate} for a {@link RhoarBooster} testing if its {@link Version}
     * is equal to the provided parameter
     *
     * @param version the {@link Version} to be compared against a given {@link RhoarBooster}.
     *                If null, always returns true;
     * @return a {@link Predicate} testing against the given {@link Version}
     */
    public static Predicate<RhoarBooster> withVersion(@Nullable Version version) {
        return (RhoarBooster b) -> version == null || version.equals(b.getVersion());
    }

    /**
     * Returns a {@link Predicate} for a {@link RhoarBooster} testing if the runsOn attribute defined inside
     * its metadata matches the provided parameter
     *
     * @param clusterType the cluster type to be compared against a given {@link RhoarBooster}.
     *                    If null, always returns true;
     * @return a {@link Predicate} testing against the given cluster type
     * @see RhoarBooster#checkCategory(List, String)
     */
    public static Predicate<RhoarBooster> withRunsOn(@Nullable String clusterType) {
        return (RhoarBooster b) -> b.runsOn(clusterType);
    }

    /**
     * Returns a {@link Predicate} for a {@link Booster} testing if against a script expression that must be evaluated
     *
     * @param script the script expression to be tested against a given {@link Booster}. Should never be null.
     * @return a {@link BoosterScriptingPredicate} instance
     */
    public static <T extends Booster> Predicate<T> withScriptFilter(@Nullable String script) {
        //noinspection unchecked
        return (script != null) ? (Predicate<T>) new BoosterScriptingPredicate(script) : b -> true;
    }

    /**
     * Returns a {@link Predicate} for a {@link Booster} testing if the given parameters match.
     *
     * @param parameters The parameters belonging to the tested {@link Booster} instance. Should never be null
     * @return a {@link BoosterScriptingPredicate} instance
     * @throws NullPointerException if parameters is null
     */
    public static <T extends Booster> Predicate<T> withParameters(Map<String, List<String>> parameters) {
        //noinspection unchecked
        return (Predicate<T>) new BoosterParameterPredicate(parameters);
    }

    /**
     * Returns a {@link Predicate} for a {@link Booster} testing if the given application argument
     * matches a property in the metadata section named "app.$application.enabled". The booster will
     * be filtered out if the property evaluated to "false".
     *
     * @param application The name of the application
     * @return a {@link Predicate} testing if ttthe given application is enabled
     */
    public static Predicate<RhoarBooster> withAppEnabled(@Nullable String application) {
        return (RhoarBooster b) -> application == null || b.getMetadata("app/" + application + "/enabled", true);
    }
}
