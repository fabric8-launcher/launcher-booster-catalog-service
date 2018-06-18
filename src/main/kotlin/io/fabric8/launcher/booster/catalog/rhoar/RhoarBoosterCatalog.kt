/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.rhoar

import java.util.Optional
import java.util.function.Predicate

import io.fabric8.launcher.booster.catalog.Booster
import io.fabric8.launcher.booster.catalog.BoosterCatalog

/**
 * General operations for a set of [Booster] objects
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 * @author [Tako Schotanus](mailto:tschotan@redhat.com)
 */
interface RhoarBoosterCatalog : BoosterCatalog<RhoarBooster> {

    /**
     * @param mission The [Mission] belonging to the [Booster] object
     * @param runtime The [Runtime] belonging to the [Booster] object
     * @param version The [Version] belonging to the [Booster] object (can be null)
     * @return an [Optional] for the given method parameters
     */
    fun getBooster(mission: Mission, runtime: Runtime, version: Version?): Optional<RhoarBooster>

    /**
     * @return an immutable [Set] of all [Mission] objects
     */
    fun getMissions(): Set<Mission>

    /**
     * @param filter A [Predicate] used to filter the [Booster] objects
     * @return an immutable [Set] of filtered [Mission] objects
     */
    fun getMissions(filter: Predicate<RhoarBooster>): Set<Mission>

    /**
     * @return an immutable [Set] of all [Runtime] objects
     */
    fun getRuntimes(): Set<Runtime>

    /**
     * @param filter A [Predicate] used to filter the [Booster] objects
     * @return an immutable [Set] of filtered [Runtime] objects
     */
    fun getRuntimes(filter: Predicate<RhoarBooster>): Set<Runtime>

    /**
     * @param filter A [Predicate] used to filter the [Booster] objects
     * @return an immutable [Set] of filtered [Version] objects
     */
    fun getVersions(filter: Predicate<RhoarBooster>): Set<Version>

    /**
     * @param mission The [Mission] belonging to the [Version] objects
     * @param runtime The [Runtime] belonging to the [Version] objects
     * @return an immutable [Set] of filtered [Version] objects
     */
    fun getVersions(mission: Mission, runtime: Runtime): Set<Version>
}