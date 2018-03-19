/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog;

import javax.annotation.Nullable;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A quickstart representation
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public class Booster {
    public static final Descriptor EMPTY_DESCRIPTOR = new Descriptor("", Collections.emptyList());

    private static final String KEY_METADATA = "metadata";

    private Map<String, Object> data;

    private final BoosterFetcher boosterFetcher;

    @Nullable
    private String id;

    @Nullable
    private Path contentPath;

    @Nullable
    private String appliedEnvironment;

    private Descriptor descriptor = EMPTY_DESCRIPTOR;

    @Nullable
    private CompletableFuture<Path> contentResult = null;

    protected Booster(BoosterFetcher boosterFetcher) {
        this.data = new LinkedHashMap<>();
        this.boosterFetcher = boosterFetcher;
    }
    
    protected Booster(@Nullable Map<String, Object> data, BoosterFetcher boosterFetcher) {
        this(boosterFetcher);
        if (data != null) {
            mergeMaps(this.data, data);
        }
    }

    /**
     * @return the data
     */
    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * @return the boosterFetcher
     */
    public BoosterFetcher getBoosterFetcher() {
        return boosterFetcher;
    }

    /**
     * @return the id
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    protected void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * @return the contentPath
     */
    @Transient @Nullable
    public Path getContentPath() {
        return contentPath;
    }

    /**
     * @param contentPath the contentPath to set
     */
    protected void setContentPath(@Nullable Path contentPath) {
        this.contentPath = contentPath;
    }

    /**
     * @return the appliedEnvironment
     */
    @Transient @Nullable
    public String getAppliedEnvironment() {
        return appliedEnvironment;
    }

    /**
     * @param appliedEnvironment the appliedEnvironment to set
     */
    protected void setAppliedEnvironment(String appliedEnvironment) {
        this.appliedEnvironment = appliedEnvironment;
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
    protected void setDescription(String description) {
        data.put("description", description);
    }
    
    /**
     * @return a boolean indicating if the booster should be ignored or not
     */
    public boolean isIgnore() {
        return Boolean.parseBoolean(Objects.toString(data.get("ignore"), "false"));
    }

    /**
     * @return the source/git/url
     */
    @Nullable
    public String getGitRepo() {
        return getDataValue(data, "source/git/url", null);
    }

    /**
     * @return the source/git/ref
     */
    @Nullable
    public String getGitRef() {
        return getDataValue(data, "source/git/ref", null);
    }

    public static class Descriptor {
        public final String name;
        public final List<String> path;

        public Descriptor(String name, List<String> path) {
            this.name = name;
            this.path = path;
        }

        @Override
        public String toString() {
            return "Descriptor(path=" + path + ", name=" + name + ")";
        }
    }

    /**
     * Returns a {@link Descriptor} object containing the name
     * and path elements of the descriptor file that was used
     * to create this Booster
     * @return a {@link Descriptor} object
     */
    @Transient
    public Descriptor getDescriptor() {
        return descriptor;
    }

    protected void setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    protected void setDescriptorFromPath(Path relativeBoosterPath) {
        Path boosterDir = relativeBoosterPath.getParent();
        setDescriptor(new Descriptor(relativeBoosterPath.getFileName().toString(), getPathList(boosterDir)));
    }

    private List<String> getPathList(@Nullable Path path) {
        if (path != null) {
            return StreamSupport.stream(path.spliterator(), false)
                    .map(Objects::toString)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
    /**
     * @return the environments
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEnvironments() {
        return (Map<String, Object>)data.getOrDefault("environment", Collections.emptyMap());
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
            Booster mergedBooster = merged(envBooster);
            // Set the "applied environment" so we can distinguish it from the original
            mergedBooster.setAppliedEnvironment(environmentName);
            // Make sure the id and content path are unique for this new booster
            mergedBooster.setId(mergedBooster.getId() + "_" + environmentName);
            Path contentPath = mergedBooster.getContentPath();
            if (contentPath != null) {
                contentPath = contentPath.getParent().resolve(contentPath.getFileName().toString() + "_" + environmentName);
                mergedBooster.setContentPath(contentPath);
            }
            return mergedBooster;
        } else {
            return this;
        }
    }

    /**
     * @return the metadata
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetadata() {
        return data.containsKey(KEY_METADATA) ? Collections.unmodifiableMap((Map<String, Object>)data.get(KEY_METADATA)) : Collections.emptyMap();
    }

    /**
     * @param key the key to look up in the booster's meta data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @return specific meta data key value or <code>null</code> if the key wasn't found
     */
    @Nullable
    public <T> T getMetadata(String key) {
        return getDataValue(getMetadata(), key, null);
    }

    /**
     * @param key the key to look up in the booster's meta data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @param defaultValue the value to return if the key isn't found
     * @return specific meta data key value or <code>defaultValue</code> if the key wasn't found
     */
    public <T> T getMetadata(String key, T defaultValue) {
        T result = getDataValue(getMetadata(), key, defaultValue);
        assert result != null;
        return result;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getDataValue(Map<String, Object> data, String key, @Nullable T defaultValue) {
        String[] keys = key.split(Pattern.quote("/"));
        if (keys.length > 1) {
            Object item = getDataValue(data, keys[0], null);
            if (item instanceof Map) {
                String remainingKey = key.substring(keys[0].length() + 1);
                return getDataValue((Map<String, Object>)item, remainingKey, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            return (T)data.getOrDefault(key, defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    public static void setDataValue(Map<String, Object> data, String key, Object value) {
        String[] keys = key.split(Pattern.quote("/"));
        if (keys.length > 1) {
            Object item = getDataValue(data, keys[0], null);
            if (!(item instanceof Map)) {
                item = new LinkedHashMap<String, Object>();
                data.put(keys[0], item);
            }
            String remainingKey = key.substring(keys[0].length() + 1);
            setDataValue((Map<String, Object>)item, remainingKey, value);
        } else {
            data.put(key, value);
        }
    }

    /**
     * Clones a Booster repo and provides the path where to find it as a result.
     * Will automatically retry on the next call if the result of a previous
     * call terminated with an exception.
     */
    public synchronized CompletableFuture<Path> content() {
        CompletableFuture<Path> cr = contentResult;
        if (cr == null || cr.isCompletedExceptionally()) {
            contentResult = cr = boosterFetcher.fetchBoosterContent(this);
        }
        return cr;
    }

    public Booster merged(Booster otherBooster) {
        Booster mergedBooster = newBooster();
        return mergedBooster.merge(this).merge(otherBooster);
    }
    
    protected Booster newBooster() {
        return new Booster(boosterFetcher);
    }
    
    protected Booster merge(Booster booster) {
        mergeMaps(data, booster.data);
        if (booster.id != null) id = booster.id;
        if (booster.contentPath != null) contentPath = booster.contentPath;
        if (booster.descriptor != EMPTY_DESCRIPTOR) descriptor = booster.descriptor;
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mergeMaps(Map<String, Object> to, Map<String, Object> from) {
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
    public boolean equals(@Nullable Object obj) {
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
                + ", name=" + getName() + ", description=" + getDescription() + ", contentPath="+ contentPath
                + ", metadata=" + getMetadata() + ", environments=" + getEnvironments() + "]";
    }
}
