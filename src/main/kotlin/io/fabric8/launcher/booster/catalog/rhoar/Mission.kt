/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

import java.util.Collections

/**
 * This type is used to group boosters into "Missions",
 * where a mission is a certain feature or idea that the
 * booster tries to explain or teach
 */
class Mission @JvmOverloads constructor(id: String,
                                        name: String = id,
                                        description: String? = null,
                                        metadata: Map<String, Any?> = emptyMap())
    : AbstractCategory(id, name, description, metadata)
