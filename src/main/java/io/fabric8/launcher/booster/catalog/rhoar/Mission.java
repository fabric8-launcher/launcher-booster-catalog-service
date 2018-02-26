/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar;

import javax.annotation.Nullable;

/**
 * This type is used to group boosters into "Missions",
 * where a mission is a certain feature or idea that the
 * booster tries to explain or teach
 */
public class Mission extends CategoryBase {
    public Mission(String id) {
        this(id, id, null, false);
    }

    public Mission(String id, String name, @Nullable String description, boolean suggested) {
        super(id, name, description, suggested);
    }
}