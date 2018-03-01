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

/**
 * This is the type that is used to group boosters into "Versions"
 * within the larger group of the booster's <code>Runtime</code>.
 * A version determines which specific implementation of a runtime
 * should be used for the booster
 */
public class Version extends CategoryBase {
    public Version(String id) {
        this(id, id, null, Collections.emptyMap());
    }

    public Version(String id, String name, @Nullable String description, Map<String, Object> metadata) {
        super(id, name, description, metadata);
    }
}
