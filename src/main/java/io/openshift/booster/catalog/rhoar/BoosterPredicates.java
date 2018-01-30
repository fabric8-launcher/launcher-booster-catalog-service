package io.openshift.booster.catalog.rhoar;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        return (RhoarBooster b) -> isSupported(b.getMetadata("runsOn"), clusterType);
    }

    /**
     * Check if a given metadata object supports the given value
     *
     * @param supportedTypes can be a List or any object (in this case toString() will be called)
     * @param value          The value to compare with
     * @return if the value is present in the given type
     */
    @SuppressWarnings("unchecked")
    public static boolean isSupported(Object supportedTypes, String value) {
        if (value != null && supportedTypes != null) {
            // Make sure we have a list of strings
            Set<String> types;
            if (supportedTypes instanceof List) {
                types = ((List<String>) supportedTypes)
                        .stream()
                        .map(Objects::toString)
                        .collect(Collectors.toSet());
            } else {
                types = Collections.singleton(supportedTypes.toString());
            }

            for (String supportedType : types) {
                if (supportedType.equalsIgnoreCase("all")
                        || supportedType.equalsIgnoreCase("*")
                        || supportedType.equalsIgnoreCase(value)) {
                    return true;
                } else if (supportedType.equalsIgnoreCase("none")
                        || supportedType.equalsIgnoreCase("!*")
                        || supportedType.equalsIgnoreCase("!" + value)) {
                    return false;
                }
            }
            return false;
        } else {
            return true;
        }
    }
}
