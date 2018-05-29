package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.Booster;
import io.fabric8.launcher.booster.catalog.BoosterFetcher;

public class RhoarBooster extends Booster {
    @Nullable
    private Mission mission;

    @Nullable
    private Runtime runtime;

    @Nullable
    private Version version;

    protected RhoarBooster(BoosterFetcher boosterFetcher) {
        super(boosterFetcher);
    }

    protected RhoarBooster(@Nullable Map<String, Object> data, BoosterFetcher boosterFetcher) {
        super(data, boosterFetcher);
    }

    /**
     * @return the mission
     */
    @Nullable
    public Mission getMission() {
        return mission;
    }

    /**
     * @param mission the mission to set
     */
    public void setMission(Mission mission) {
        this.mission = mission;
    }

    /**
     * @return the runtime
     */
    @Nullable
    public Runtime getRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the version
     */
    @Nullable
    public Version getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(Version version) {
        this.version = version;
    }

    /**
     * @return the booster's data in easily exportable format
     */
    @Override
    public Map<String, Object> getExportableData() {
        Map<String, Object> exp = new HashMap<>(super.getExportableData());
        if (mission != null) exp.put("mission", mission.getId());
        if (runtime != null) exp.put("runtime", runtime.getId());
        if (version != null) exp.put("version", version.getId());
        return exp;
    }

    @Override
    protected RhoarBooster newBooster() {
        return new RhoarBooster(getBoosterFetcher());
    }

    @Override
    protected RhoarBooster merge(Booster booster) {
        super.merge(booster);
        if (booster instanceof RhoarBooster) {
            RhoarBooster rb = (RhoarBooster) booster;
            if (rb.mission != null) mission = rb.mission;
            if (rb.runtime != null) runtime = rb.runtime;
            if (rb.version != null) version = rb.version;
        }
        return this;
    }

    @Override
    public RhoarBooster forEnvironment(String environmentName) {
        return (RhoarBooster)super.forEnvironment(environmentName);
    }

    public boolean runsOn(@Nullable String clusterType) {
        return clusterType == null || clusterType.isEmpty() ||
                checkCategory(toList(getMetadata("runsOn")), clusterType);
    }

    /**
     * Takes an object that is either null, a single value or a list
     * of values and makes sure that the result is always a list
     * @param data An object
     * @return A list of values
     */
    public static List<String> toList(@Nullable Object data) {
        List<String> clusters;
        if (data instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) data;
            clusters = list.stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else if (data != null && !data.toString().isEmpty()) {
            clusters = Collections.singletonList(data.toString());
        } else {
            clusters = Collections.emptyList();
        }
        return clusters;
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
     *
     * @param supportedCategories Can be a single object or a List of objects
     * @param category            The category name to check against
     * @return if the category matches the supported categories or not
     */
    @SuppressWarnings("unchecked")
    public static boolean checkCategory(List<String> supportedCategories, String category) {
        boolean defaultResult = true;
        if (!supportedCategories.isEmpty()) {
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
        }
        return defaultResult;
    }

}
