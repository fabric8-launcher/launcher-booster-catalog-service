/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

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
        get() = metadata.getOrDefault(KEY_SUGGESTED, false) as Boolean

    override fun compareTo(other: AbstractCategory) = name.compareTo(other.name)

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + id.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (javaClass != other.javaClass)
            return false
        val obj = other as AbstractCategory?
        return id == obj!!.id
    }

    override fun toString(): String {
        return javaClass.simpleName + " [id=" + id + ", name=" + name + ", description=" + description + ", metadata=" + metadata + "]"
    }

    companion object {
        const val KEY_SUGGESTED = "suggested"
    }
}