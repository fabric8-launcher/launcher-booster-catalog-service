/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog

import java.beans.Transient
import java.nio.file.Path
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Objects
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.StreamSupport

/**
 * A quickstart representation
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
open class Booster protected constructor(val boosterFetcher: BoosterFetcher) {

    private val _data: MutableMap<String, Any?> = LinkedHashMap()

    var id: String? = null

    @get:Transient
    var contentPath: Path? = null

    @get:Transient
    var appliedEnvironment: String? = null
        protected set

    /**
     * Returns a [Descriptor] object containing the name
     * and path elements of the descriptor file that was used
     * to create this Booster
     *
     * @return a [Descriptor] object
     */
    @get:Transient
    var descriptor = EMPTY_DESCRIPTOR
        protected set

    private var contentResult: CompletableFuture<Path>? = null

    val data: Map<String, Any?>
        get() = Collections.unmodifiableMap(_data)

    /**
     * @return the booster's _data in easily exportable format
     */
    open val exportableData: Map<String, Any?>
        get() = data

    val name: String
        get() = Objects.toString(_data["name"], id)

    var description: String
        get() = Objects.toString(_data["description"], "No description available")
        protected set(description) {
            _data["description"] = description
        }

    /**
     * @return a boolean indicating if the booster should be ignored or not
     */
    val isIgnore: Boolean
        get() = java.lang.Boolean.parseBoolean(Objects.toString(_data["ignore"], "false"))

    /**
     * @return the source/git/url
     */
    val gitRepo: String?
        get() = getDataValue<String>(_data, "source/git/url", null)

    /**
     * @return the source/git/ref
     */
    val gitRef: String?
        get() = getDataValue<String>(_data, "source/git/ref", null)

    val environments: Map<String, Any?>
        get() = (_data as Map<String, Any?>).getOrDefault("environment", emptyMap<String, Any?>()) as Map<String, Any?>

    val metadata: Map<String, Any?>
        get() {
            val metadata = _data[KEY_METADATA] as Map<String, Any?>?
            return if (metadata != null)
                Collections.unmodifiableMap(metadata)
            else
                emptyMap()
        }

    constructor(data: Map<String, Any?>?, boosterFetcher: BoosterFetcher) : this(boosterFetcher) {
        if (data != null) {
            mergeMaps(this._data, data)
        }
    }

    class Descriptor(val name: String, val path: List<String>) {
        override fun toString(): String {
            return "Descriptor(path=$path, name=$name)"
        }
    }

    fun setDescriptorFromPath(relativeBoosterPath: Path) {
        val boosterDir = relativeBoosterPath.parent
        descriptor = Descriptor(relativeBoosterPath.fileName.toString(), getPathList(boosterDir))
    }

    private fun getPathList(path: Path?): List<String> {
        return if (path != null) {
            StreamSupport.stream(path.spliterator(), false)
                    .map<String>({ Objects.toString(it) })
                    .collect(Collectors.toList())
        } else {
            emptyList()
        }
    }

    /**
     * This method returns a version of this Booster configured specifically
     * for the indicated environment. If the environment doesn't exist or it
     * doesn't contain any information the current Booster is returned.
     *
     * @param environmentName The name of the environment
     * @return the current Booster configured for the specified environment
     */
    open fun forEnvironment(environmentName: String): Booster {
        val env = environments[environmentName] as Map<String, Any?>?
        if (env != null && !env.isEmpty()) {
            val envBooster = Booster(env, boosterFetcher)
            val mergedBooster = merged(envBooster)
            // Set the "applied environment" so we can distinguish it from the original
            mergedBooster.appliedEnvironment = environmentName
            // Make sure the id and content path are unique for this new booster
            mergedBooster.id = mergedBooster.id + "_" + environmentName
            var contentPath = mergedBooster.contentPath
            if (contentPath != null) {
                contentPath = contentPath.parent.resolve(contentPath.fileName.toString() + "_" + environmentName)
                mergedBooster.contentPath = contentPath
            }
            return mergedBooster
        } else {
            return this
        }
    }

    /**
     * @param key the key to look up in the booster's meta _data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @return specific meta _data key value or `null` if the key wasn't found
     */
    fun <T> getMetadata(key: String): T? {
        return getDataValue<T>(metadata, key, null)
    }

    /**
     * @param key          the key to look up in the booster's meta _data section. Can take the form
     * of a path where keys are separated by "/" to identify sub items
     * @param defaultValue the value to return if the key isn't found
     * @return specific meta _data key value or `defaultValue` if the key wasn't found
     */
    fun <T> getMetadata(key: String, defaultValue: T): T {
        return getDataValue(metadata, key, defaultValue)!!
    }

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

    fun merged(otherBooster: Booster): Booster {
        val mergedBooster = newBooster()
        return mergedBooster.merge(this).merge(otherBooster)
    }

    protected open fun newBooster(): Booster {
        return Booster(boosterFetcher)
    }

    protected open fun merge(booster: Booster): Booster {
        mergeMaps(_data, booster._data)
        if (booster.id != null) id = booster.id
        if (booster.contentPath != null) contentPath = booster.contentPath
        if (booster.descriptor !== EMPTY_DESCRIPTOR) descriptor = booster.descriptor
        return this
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (id == null) 0 else id!!.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj)
            return true
        if (obj == null)
            return false
        if (javaClass != obj.javaClass)
            return false
        val other = obj as Booster?
        return if (id == null) {
            other!!.id == null
        } else
            id == other!!.id
    }

    override fun toString(): String {
        return ("Booster [id=" + id + ", gitRepo=" + gitRepo + ", gitRef=" + gitRef
                + ", name=" + name + ", description=" + description + ", contentPath=" + contentPath
                + ", metadata=" + metadata + ", environments=" + environments + "]")
    }

    companion object {
        val EMPTY_DESCRIPTOR = Descriptor("", emptyList())

        private val KEY_METADATA = "metadata"

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

        @JvmStatic
        fun setDataValue(data: MutableMap<String, Any?>, key: String, value: Any) {
            val keys = key.split(Pattern.quote("/").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (keys.size > 1) {
                var item = getDataValue<Any>(data, keys[0], null)
                if (item !is Map<*, *>) {
                    item = LinkedHashMap<String, Any>()
                    data[keys[0]] = item
                }
                val remainingKey = key.substring(keys[0].length + 1)
                setDataValue((item as MutableMap<String, Any?>), remainingKey, value)
            } else {
                data[key] = value
            }
        }

        @JvmStatic
        fun mergeMaps(to: MutableMap<String, Any?>, from: Map<String, Any?>): Map<String, Any?> {
            for (key in from.keys) {
                val item = from[key]
                if (item is Map<*, *>) {
                    val to2 = LinkedHashMap<String, Any?>()
                    val from2 = item as Map<String, Any?>
                    if (to.containsKey(key) && to[key] is Map<*, *>) {

                        mergeMaps(to2, to[key] as Map<String, Any?>)
                    }
                    to[key] = mergeMaps(to2, from2)
                } else if (item is List<*>) {
                    to[key] = ArrayList(item as List<Any>)
                } else if (item == null) {
                    // Should not happen but let's handle it anyway
                    to.remove(key)
                } else {
                    to[key] = item
                }
            }
            return to
        }
    }
}
