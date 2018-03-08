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
public class Runtime extends CategoryBase {

    public static final String DEFAULT_PIPELINE_PLATFORM = "maven";

    public Runtime(String id) {
      this(id, id, null, null);
   }

    public Runtime(String id, String name, @Nullable String pipelinePlatform, @Nullable String icon) {
        super(id, name, null);
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