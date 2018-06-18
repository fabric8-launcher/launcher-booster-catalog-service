/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog

import java.io.IOException
import java.nio.file.Path
import java.util.Optional
import java.util.function.Predicate

/**
 * General operations for a set of [Booster] objects
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
interface BoosterCatalog<BOOSTER : Booster> {

    /**
     * Copies the [Booster] contents to the specified [Path]
     */
    @Throws(IOException::class)
    fun copy(booster: BOOSTER, projectRoot: Path): Path

    /**
     * Returns a [Collection] of [Booster] objects.
     */
    fun getBoosters(): Collection<BOOSTER>

    /**
     * Returns a [Collection] of [Booster] objects, filtered using a [Predicate].
     *
     * @param filter A [Predicate] used to filter the [Booster] objects
     * @return a [Collection] of filtered [Booster] objects
     */
    fun getBoosters(filter: Predicate<BOOSTER>): Collection<BOOSTER>

    /**
     * @param filter A [Predicate] used to filter the [Booster] objects
     * @return an [Optional] for the given method parameters
     */
    fun getBooster(filter: Predicate<BOOSTER>): Optional<BOOSTER>
}
