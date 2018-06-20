/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

import java.util.Collections
import java.util.Objects

/**
 * This type is used to group boosters into "Runtimes",
 * where a runtime is the technology that is used to
 * to develop and execute the booster's code
 */
class Runtime @JvmOverloads constructor(id: String,
                                        name: String = id,
                                        description: String? = null,
                                        metadata: Map<String, Any?> = emptyMap(),
                                        val icon: String? = null,
                                        val versions: Map<String, Version> = emptyMap())
    : AbstractCategory(id, name, description, metadata) {

    val pipelinePlatform: String
        get() = Objects.toString(metadata.getOrDefault(KEY_PIPELINE_PLATFORM, DEFAULT_PIPELINE_PLATFORM))

    companion object {
        const val KEY_PIPELINE_PLATFORM = "pipelinePlatform"
        const val DEFAULT_PIPELINE_PLATFORM = "maven"
    }
}