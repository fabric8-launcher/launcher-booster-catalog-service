/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar;

import javax.annotation.Nullable;

/**
 * This type is used to group boosters into "Runtimes",
 * where a runtime is the technology that is used to
 * to develop and execute the booster's code
 */
public class Runtime extends CategoryBase {

    public static final String DEFAULT_PIPELINE_PLATFORM = "maven";

    public Runtime(String id) {
      this(id, id, null, null, null, false);
   }

    public Runtime(String id, String name, @Nullable String pipelinePlatform, @Nullable String icon, @Nullable String description, boolean suggested) {
        super(id, name, description, suggested);
        this.pipelinePlatform = pipelinePlatform;
        this.icon = icon;
    }

    @Nullable
    private final String pipelinePlatform;

    @Nullable
    private final String icon;

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
}