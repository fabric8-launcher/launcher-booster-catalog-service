/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class Runtime implements Comparable<Runtime> {

    public static final String DEFAULT_PIPELINE_PLATFORM = "maven";

    public Runtime(String id) {
      this(id, id, null, null);
   }

    public Runtime(String id, String name, @Nullable String pipelinePlatform, @Nullable String icon) {
        this.id = id;
        this.name = name;
        this.pipelinePlatform = pipelinePlatform;
        this.icon = icon;
    }

    private final String id;

    private final String name;

    @Nullable
    private final String pipelinePlatform;

    @Nullable
    private final String icon;

    /**
     * This method is needed so the Web UI can know what's the internal ID used
     */
    public String getKey() {
        return getId();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public String getPipelinePlatform() {
        return pipelinePlatform != null ? pipelinePlatform : DEFAULT_PIPELINE_PLATFORM;
    }

    /**
     * @return the icon
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    @Override
    public int compareTo(Runtime o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.hashCode();
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
        Runtime other = (Runtime) obj;
        return id.equals(other.id);
    }

    @Override
    public String toString() {
        return "Runtime [id=" + id + ", name=" + name + ", pipelinePlatform=" + pipelinePlatform + ", icon=" + icon + "]";
    }
}