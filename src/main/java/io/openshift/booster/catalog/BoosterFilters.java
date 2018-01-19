package io.openshift.booster.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BoosterFilters {
    private BoosterFilters() {}

    public static Predicate<Booster> ignored(boolean ignored) {
        return (Booster b) -> b.isIgnore() == ignored;
    }

    public static Predicate<Booster> runtimes(Runtime runtime) {
        return (Booster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    public static Predicate<Booster> missions(Mission mission) {
        return (Booster b) -> mission == null || mission.equals(b.getMission());
    }

    public static Predicate<Booster> versions(Version version) {
        return (Booster b) -> version == null || version.equals(b.getVersion());
    }
    
    public static Predicate<Booster> runsOn(String clusterType) {
        return (Booster b) -> isSupported(b.getMetadata("runsOn"), clusterType, true);
    }

    public static Predicate<Booster> doesNotRunOn(String clusterType) {
        return (Booster b) -> isSupported(b.getMetadata("doesNotRunOn"), clusterType, false);
    }

    @SuppressWarnings("unchecked")
    private static boolean isSupported(Object supportedTypes, String clusterType, boolean defaultValue) {
        if (clusterType != null && supportedTypes != null) {
            Set<String> types;
            if (supportedTypes instanceof List) {
                // Make sure we have a list of lowercase strings
                types = ((List<String>)supportedTypes)
                        .stream()
                        .map(Objects::toString)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
            } else {
                if (supportedTypes.equals("*")) {
                    return true;
                }
                types = Collections.singleton(supportedTypes.toString());
            }
            return types.contains(clusterType.toLowerCase());
        } else {
            return defaultValue;
        }
    }
}
