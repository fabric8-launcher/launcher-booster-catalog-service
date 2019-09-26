/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog

import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

/**
 * A quickstart representation
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
open class Booster(val data: Map<String, Any?>, val boosterFetcher: BoosterFetcher) {

    open val id: String by lazy {
        // Some silly way to generate an Id
        val str = data.values.filter { it is String }.joinToString()
        UUID.nameUUIDFromBytes(str.toByteArray()).getMostSignificantBits().toString()
    }

    private var contentResult: CompletableFuture<Path>? = null

    /**
     * @return the booster's _data in easily exportable format
     */
    open val exportableData: Map<String, Any?>
        get() = data

    val name: String
        get() = Objects.toString(data["name"], id)

    val description: String
        get() = Objects.toString(data["description"], "No description available")

    /**
     * @return a boolean indicating if the booster should be ignored or not
     */
    val isIgnore: Boolean
        get() = java.lang.Boolean.parseBoolean(Objects.toString(data["ignore"], "false"))

    /**
     * @return the source/git/url
     */
    val gitRepo: String?
        get() = getDataValue<String>(data, "repo", null)

    /**
     * @return the source/git/ref
     */
    val gitRef: String?
        get() = getDataValue<String>(data, "ref", null)

    val metadata: Map<String, Any?>
        get() = data[KEY_METADATA] as Map<String, Any?>? ?: emptyMap()

    /**
     * @param key the key to look up in the booster's meta _data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @return specific meta _data key value or `null` if the key wasn't found
     */
    fun <T> getMetadata(key: String): T? = getDataValue<T>(metadata, key, null)

    /**
     * @param key          the key to look up in the booster's meta _data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @param defaultValue the value to return if the key isn't found
     * @return specific meta _data key value or `defaultValue` if the key wasn't found
     */
    fun <T> getMetadata(key: String, defaultValue: T): T = getDataValue(metadata, key, defaultValue)!!

    /**
     * Clones a Booster repo and provides the path where to find it as a result.
     * Will automatically retry on the next call if the result of a previous
     * call terminated with an exception.
     */
    @Synchronized
    fun content(): CompletableFuture<Path> {
        var cr = contentResult
        if (cr == null || cr.isCompletedExceptionally) {
            cr = boosterFetcher.fetchBoosterContent(this)
            contentResult = cr
        }
        return cr
    }

    protected open fun newBooster(data: Map<String, Any?>) = Booster(data, boosterFetcher)

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (id == null) 0 else id!!.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (javaClass != other.javaClass)
            return false
        val other = other as Booster?
        return if (id == null) {
            other!!.id == null
        } else
            id == other!!.id
    }

    override fun toString(): String {
        return ("Booster [id=" + id + ", gitRepo=" + gitRepo + ", gitRef=" + gitRef
                + ", name=" + name + ", description=" + description
                + ", metadata=" + metadata + "]")
    }

    companion object {
        private const val KEY_METADATA = "metadata"

        @JvmStatic
        fun <T> getDataValue(data: Map<String, Any?>, key: String, defaultValue: T?): T? {
            val keys = key.split(Pattern.quote("/").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (keys.size > 1) {
                val item = getDataValue<Any>(data, keys[0], null)
                if (item is Map<*, *>) {
                    val remainingKey = key.substring(keys[0].length + 1)
                    return getDataValue((item as Map<String, Any>?)!!, remainingKey, defaultValue)
                } else {
                    return defaultValue
                }
            } else {
                return data.getOrDefault(key, defaultValue) as T
            }
        }
    }
}
