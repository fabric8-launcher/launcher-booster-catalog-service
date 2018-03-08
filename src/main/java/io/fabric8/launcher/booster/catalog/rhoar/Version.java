/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public class Version extends CategoryBase {
    public Version(String id) {
        this(id, id);
    }

    public Version(String id, String name) {
        super(id, name, null);
    }
}