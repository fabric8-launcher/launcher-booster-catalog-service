/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog

import org.yaml.snakeyaml.constructor.Constructor

/**
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
class YamlConstructor : Constructor() {
    @Throws(ClassNotFoundException::class)
    override fun getClassForName(name: String): Class<*> =
            if ("java.util.Map" == name) {
                java.util.HashMap::class.java
            } else {
                Class.forName(name, true, javaClass.classLoader)
            }
}
