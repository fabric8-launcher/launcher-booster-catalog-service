/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

import java.util.Collections

/**
 * This class is the base class for the types that we use to
 * divide the boosters into the categories: Missions, Runtimes
 * and Versions.
 */
abstract class AbstractCategory (
        val id: String,
        val name: String,
        val description: String?,
        val metadata: Map<String, Any?>) : Comparable<AbstractCategory> {

    /**
     * This method is needed so the Web UI can know what's the internal ID used
     */
    val key: String
        get() = id

    val isSuggested: Boolean
        get() =metadata.getOrDefault(KEY_SUGGESTED, false) as Boolean

    override fun compareTo(o: AbstractCategory): Int {
        return name.compareTo(o.name)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + id.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as AbstractCategory?
        return id == other!!.id
    }

    override fun toString(): String {
        return javaClass.simpleName + " [id=" + id + ", name=" + name + ", description=" + description + ", metadata=" + metadata + "]"
    }

    companion object {
        val KEY_SUGGESTED = "suggested"
    }
}