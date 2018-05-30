package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.rhoar.predicates.MetadataParameterPredicate;
import io.fabric8.launcher.booster.catalog.rhoar.predicates.ScriptingRhoarBoosterPredicate;

public final class BoosterPredicates {
    private BoosterPredicates() {
        throw new IllegalAccessError("Utility class");
    }

    public static Predicate<RhoarBooster> withRuntime(@Nullable Runtime runtime) {
        return (RhoarBooster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    public static Predicate<RhoarBooster> withMission(@Nullable Mission mission) {
        return (RhoarBooster b) -> mission == null || mission.equals(b.getMission());
    }

    public static Predicate<RhoarBooster> withVersion(@Nullable Version version) {
        return (RhoarBooster b) -> version == null || version.equals(b.getVersion());
    }

    public static Predicate<RhoarBooster> withRunsOn(@Nullable String clusterType) {
        return (RhoarBooster b) -> b.runsOn(clusterType);
    }

    public static Predicate<RhoarBooster> withScriptFilter(@Nullable String filter) {
        return new ScriptingRhoarBoosterPredicate(filter);
    }

    public static Predicate<RhoarBooster> withParameters(Map<String, List<String>> parameters) {
        return new MetadataParameterPredicate(parameters);
    }

}
