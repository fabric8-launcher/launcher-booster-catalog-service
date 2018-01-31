package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class BoosterPredicates {
    private BoosterPredicates() {
    }

    public static Predicate<RhoarBooster> withRuntime(io.fabric8.launcher.booster.catalog.rhoar.Runtime runtime) {
        return (RhoarBooster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    public static Predicate<RhoarBooster> withMission(Mission mission) {
        return (RhoarBooster b) -> mission == null || mission.equals(b.getMission());
    }

    public static Predicate<RhoarBooster> withVersion(Version version) {
        return (RhoarBooster b) -> version == null || version.equals(b.getVersion());
    }

    public static Predicate<RhoarBooster> withRunsOn(String clusterType) {
        return (RhoarBooster b) -> checkNegatableCategory(b.getRunsOn(), clusterType);
    }

    /**
     * Check a category name against supported categories.
     * The supported categories are either a single object or a list of objects.
     * The given category is checked against the supported categories one by one.
     * If the category name matches the supported one exactly <code>true</code> is
     * returned. The supported category can also start with a <code>!</code> 
     * indicating a the result should be negated. In that case <code>false</code>
     * is returned. The special supported categories <code>all</code> and
     * <code>none</code> will always return <code>true</code> and <code>false</code>
     * respectively when encountered.
     * @param supportedCategories Can be a single object or a List of objects
     * @param category The category name to check against
     * @return if the category matches the supported categories or not
     */
    @SuppressWarnings("unchecked")
    public static boolean checkNegatableCategory(List<String> supportedCategories, String category) {
        if (category != null && supportedCategories != null) {
            if (!supportedCategories.isEmpty()) {
                boolean defaultResult = true;
                for (String supportedCategory : supportedCategories) {
                    if (!supportedCategory.startsWith("!")) {
                        defaultResult = false;
                    }
                    if (supportedCategory.equalsIgnoreCase("all")
                            || supportedCategory.equalsIgnoreCase("*")
                            || supportedCategory.equalsIgnoreCase(category)) {
                        return true;
                    } else if (supportedCategory.equalsIgnoreCase("none")
                            || supportedCategory.equalsIgnoreCase("!*")
                            || supportedCategory.equalsIgnoreCase("!" + category)) {
                        return false;
                    }
                }
                return defaultResult;
            }
        }
        return true;
    }
}
