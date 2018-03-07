package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.fabric8.launcher.booster.catalog.BoosterFetcher;
import io.fabric8.launcher.booster.catalog.Booster;

import javax.annotation.Nullable;

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

    @Override
    protected RhoarBooster newBooster() {
        return new RhoarBooster(getBoosterFetcher());
    }

    @Override
    protected RhoarBooster merge(Booster booster) {
        super.merge(booster);
        if (booster instanceof RhoarBooster) {
            RhoarBooster rb = (RhoarBooster)booster;
            if (rb.mission != null) mission = rb.mission;
            if (rb.runtime != null) runtime = rb.runtime;
            if (rb.version != null) version = rb.version;
        }
        return this;
    }

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

}
