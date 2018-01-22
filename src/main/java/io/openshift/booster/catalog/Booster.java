/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import java.beans.Transient;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * A quickstart representation
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public class Booster {
    private Map<String, Object> data;
    private final BoosterFetcher boosterFetcher;

    private String id;
    
    private Mission mission;
    private Runtime runtime;
    private Version version;

    private Path contentPath;

    private CompletableFuture<Path> contentResult = null;
    
    Booster(BoosterFetcher boosterFetcher) {
        this.data = new LinkedHashMap<>();
        this.boosterFetcher = boosterFetcher;
        this.data.put("metadata", new LinkedHashMap<>());
    }
    
    Booster(Map<String, Object> data, BoosterFetcher boosterFetcher) {
        this(boosterFetcher);
        mergeMaps(this.data, data);
    }
    
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return Objects.toString(data.get("name"), getId());
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return Objects.toString(data.get("description"), "No description available");
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        data.put("description", description);
    }
    
    /**
     * @return a boolean indicating if the booster should be ignored or not
     */
    public boolean isIgnore() {
        return Boolean.parseBoolean(Objects.toString(data.get("ignore"), "false"));
    }

    /**
     * @return the gitRepo
     */
    public String getGitRepo() {
        return Objects.toString(data.get("gitRepo"), null);
    }

    /**
     * @return the gitRef
     */
    public String getGitRef() {
        return Objects.toString(data.get("gitRef"), null);
    }

    /**
     * @return the environments
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEnvironments() {
        return (Map<String, Object>)data.getOrDefault("environments", Collections.emptyMap());
    }

    /**
     * This method returns a version of this Booster configured specifically
     * for the indicated environment. If the environment doesn't exist or it
     * doesn't contain any information the current Booster is returned.
     * @param  environmentName The name of the environment
     * @return the current Booster configured for the specified environment
     */
    @SuppressWarnings("unchecked")
    public Booster forEnvironment(String environmentName) {
        Map<String, Object> env = (Map<String, Object>)getEnvironments().get(environmentName);
        if (env != null && !env.isEmpty()) {
            Booster envBooster = new Booster(env, boosterFetcher);
            return merged(envBooster);
        } else {
            return this;
        }
    }

    /**
     * @return the metadata
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetadata() {
        return (Map<String, Object>)data.get("metadata");
    }

    /**
     * @param key the key to look up in the booster's meta data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @return specific meta data key value or <code>null</code> if the key wasn't found
     */
    public <T> T getMetadata(String key) {
        return getMetadata(key, null);
    }

    /**
     * @param key the key to look up in the booster's meta data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @param defaultValue the value to return if the key isn't found
     * @return specific meta data key value or <code>defaultValue</code> if the key wasn't found
     */
    public <T> T getMetadata(String key, T defaultValue) {
        return getMetadata(getMetadata(), key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T getMetadata(Map<String, Object> data, String key, T defaultValue) {
        String[] keys = key.split(Pattern.quote("/"));
        if (keys.length > 1) {
            Object item = getMetadata(data, keys[0], null);
            if (item instanceof Map) {
                String remainingKey = key.substring(keys[0].length() + 1);
                return getMetadata((Map<String, Object>)item, remainingKey, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            return (T)data.getOrDefault(key, defaultValue);
        }
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
     * @return the contentPath
     */
    @Transient
    public Path getContentPath() {
        return contentPath;
    }

    /**
     * @param contentPath the contentPath to set
     */
    public void setContentPath(Path contentPath) {
        this.contentPath = contentPath;
    }

    /**
     * Clones a Booster repo and provides the path where to find it as a result
     */
    public synchronized CompletableFuture<Path> content() {
        if (contentResult == null) {
            contentResult = boosterFetcher.fetchBoosterContent(this);
        }
        return contentResult;
    }

    public Booster merged(Booster otherBooster) {
        Booster mergedBooster = new Booster(boosterFetcher);
        return mergedBooster.merge(this).merge(otherBooster);
    }
    
    protected Booster merge(Booster booster) {
        mergeMaps(data, booster.data);
        if (booster.id != null) id = booster.id;
        if (booster.mission != null) mission = booster.mission;
        if (booster.runtime != null) runtime = booster.runtime;
        if (booster.version != null) version = booster.version;
        if (booster.contentPath != null) contentPath = booster.contentPath;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeMaps(Map<String, Object> to, Map<String, Object> from) {
        for (String key : from.keySet()) {
            Object item = from.get(key);
            if (item instanceof Map) {
                Map<String, Object> to2 = new LinkedHashMap<>();
                Map<String, Object> from2 = (Map<String, Object>)item;
                if (to.containsKey(key) && to.get(key) instanceof Map) {
                    mergeMaps(to2, (Map<String, Object>)to.get(key));
                }
                to.put(key, mergeMaps(to2, from2));
            } else if (item instanceof List) {
                to.put(key, new ArrayList<Object>((List<Object>)item));
            } else {
                to.put(key, item);
            }
        }
        return to;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Booster other = (Booster) obj;
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }

    @Override
    public String toString() {
        return "Booster [id=" + id + ", gitRepo=" + getGitRepo() + ", gitRef=" + getGitRef()
                + ", name=" + getName() + ", description=" + getDescription() + ", mission=" + mission
                + ", runtime=" + runtime + ", version=" + version + ", contentPath="+ contentPath
                + ", metadata=" + getMetadata() + ", environments=" + getEnvironments() + "]";
    }
}
