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
public class Mission extends CategoryBase {
    public Mission(String id) {
        this(id, id, null, false);
    }

    public Mission(String id, String name, @Nullable String description, boolean suggested) {
        super(id, name, description);
        this.suggested = suggested;
    }

    private final boolean suggested;

    /**
     * @return get suggested
     */
    public boolean isSuggested() {
        return suggested;
    }
}