/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog;

import org.yaml.snakeyaml.constructor.Constructor;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
class YamlConstructor extends Constructor {
    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        return Class.forName(name, true, getClass().getClassLoader());
    }
}
