/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

import java.util.Collections

/**
 * This is the type that is used to group boosters into "Versions"
 * within the larger group of the booster's `Runtime`.
 * A version determines which specific implementation of a runtime
 * should be used for the booster
 */
class Version @JvmOverloads constructor(id: String, name: String = id, description: String? = null, metadata: Map<String, Any?> = emptyMap()) : AbstractCategory(id, name, description, metadata)
