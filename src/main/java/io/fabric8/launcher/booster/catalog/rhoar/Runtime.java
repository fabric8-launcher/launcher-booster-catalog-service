/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * This type is used to group boosters into "Runtimes",
 * where a runtime is the technology that is used to
 * to develop and execute the booster's code
 */
public class Runtime extends AbstractCategory {

    public static final String KEY_PIPELINE_PLATFORM = "pipelinePlatform";
    public static final String DEFAULT_PIPELINE_PLATFORM = "maven";

    public Runtime(String id) {
      this(id, id, null, Collections.emptyMap(), null);
   }

    public Runtime(String id, String name, @Nullable String description, Map<String, Object> metadata, @Nullable String icon) {
        super(id, name, description, metadata);
        this.icon = icon;
    }

    @Nullable
    private final String icon;

    /**
     * @return the icon
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    public String getPipelinePlatform() {
        return Objects.toString(getMetadata().getOrDefault(KEY_PIPELINE_PLATFORM, DEFAULT_PIPELINE_PLATFORM));
    }
}