package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.fabric8.launcher.booster.catalog.BoosterFetcher;
import io.fabric8.launcher.booster.catalog.Booster;

public class RhoarBooster extends Booster {
    private Mission mission;
    private io.fabric8.launcher.booster.catalog.rhoar.Runtime runtime;
    private Version version;

    public List<String> getRunsOn() {
        Object supportedClusters = getMetadata("runsOn");
        List<String> clusters;
        if (supportedClusters instanceof List) {
            clusters = ((List<String>) supportedClusters)
                    .stream()
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else if (supportedClusters != null && !supportedClusters.toString().isEmpty()) {
            clusters = Collections.singletonList(supportedClusters.toString());
        } else {
            clusters = Collections.emptyList();
        }
        return clusters;
    }

    /**
     * @return the mission
     */
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
    public io.fabric8.launcher.booster.catalog.rhoar.Runtime getRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(io.fabric8.launcher.booster.catalog.rhoar.Runtime runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the version
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(Version version) {
        this.version = version;
    }

    protected RhoarBooster(BoosterFetcher boosterFetcher) {
        super(boosterFetcher);
    }

    protected RhoarBooster(Map<String, Object> data, BoosterFetcher boosterFetcher) {
        super(data, boosterFetcher);
    }

    protected RhoarBooster newBooster(BoosterFetcher boosterFetcher) {
        return new RhoarBooster(boosterFetcher);
    }
    
    protected RhoarBooster merge(RhoarBooster booster) {
        super.merge(booster);
        if (booster.mission != null) mission = booster.mission;
        if (booster.runtime != null) runtime = booster.runtime;
        if (booster.version != null) version = booster.version;
        return this;
    }
    
}
