package io.openshift.booster.catalog.rhoar;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BoosterPredicates {
    private BoosterPredicates() {
    }

    public static Predicate<RhoarBooster> runtimes(Runtime runtime) {
        return (RhoarBooster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    public static Predicate<RhoarBooster> missions(Mission mission) {
        return (RhoarBooster b) -> mission == null || mission.equals(b.getMission());
    }

    public static Predicate<RhoarBooster> versions(Version version) {
        return (RhoarBooster b) -> version == null || version.equals(b.getVersion());
    }

    public static Predicate<RhoarBooster> runsOn(String clusterType) {
        return (RhoarBooster b) -> checkRunsOn(b.getMetadata("runsOn"), clusterType);
    }

    /**
     * Check if a given "metadata/runsOn" object supports the given cluster type
     * @param supportedTypes Can be a single object or a List of objects
     * @param clusterType The cluster type name to check against
     * @return if the clusterType matches the supported types
     */
    @SuppressWarnings("unchecked")
    public static boolean checkRunsOn(Object supportedTypes, String clusterType) {
        if (clusterType != null && supportedTypes != null) {
            // Make sure we have a list of strings
            List<String> types;
            if (supportedTypes instanceof List) {
                types = ((List<String>) supportedTypes)
                        .stream()
                        .map(Objects::toString)
                        .collect(Collectors.toList());
            } else if (!supportedTypes.toString().isEmpty()) {
                types = Collections.singletonList(supportedTypes.toString());
            } else {
                types = Collections.emptyList();
            }

            if (!types.isEmpty()) {
                boolean defaultResult = true;
                for (String supportedType : types) {
                    if (!supportedType.startsWith("!")) {
                        defaultResult = false;
                    }
                    if (supportedType.equalsIgnoreCase("all")
                            || supportedType.equalsIgnoreCase("*")
                            || supportedType.equalsIgnoreCase(clusterType)) {
                        return true;
                    } else if (supportedType.equalsIgnoreCase("none")
                            || supportedType.equalsIgnoreCase("!*")
                            || supportedType.equalsIgnoreCase("!" + clusterType)) {
                        return false;
                    }
                }
                return defaultResult;
            }
        }
        return true;
    }
}
