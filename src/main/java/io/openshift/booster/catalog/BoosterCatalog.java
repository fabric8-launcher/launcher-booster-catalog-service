/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * General operations for a set of {@link Booster} objects
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public interface BoosterCatalog {
    /**
     * Copies the {@link Booster} contents to the specified {@link Path}
     */
    Path copy(Booster booster, Path projectRoot) throws IOException;

    /**
     * Returns a {@link Collection} of {@link Booster} objects.
     * 
     * @return a {@link Collection} of {@link Booster} objects
     */
    Collection<Booster> getBoosters();

    /**
     * Returns a {@link Collection} of {@link Booster} objects, filtered using a {@link Predicate}.
     *
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return a {@link Collection} of filtered {@link Booster} objects
     */
    Collection<Booster> getBoosters(Predicate<Booster> filter);

    /**
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return an {@link Optional} for the given method parameters
     */
    Optional<Booster> getBooster(Predicate<Booster> filter);

    /**
     * @param mission The {@link Mission} belonging to the {@link Booster} object
     * @param runtime The {@link Runtime} belonging to the {@link Booster} object
     * @return an {@link Optional} for the given method parameters
     */
    Optional<Booster> getBooster(Mission mission, Runtime runtime);

    /**
     * @param mission The {@link Mission} belonging to the {@link Booster} object
     * @param runtime The {@link Runtime} belonging to the {@link Booster} object
     * @return an {@link Optional} for the given method parameters
     */
    Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version);

    /**
     * @return an immutable {@link Set} of all {@link Mission} objects
     */
    Set<Mission> getMissions();

    /**
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return an immutable {@link Set} of filtered {@link Mission} objects
     */
    Set<Mission> getMissions(Predicate<Booster> filter);

    /**
     * @return an immutable {@link Set} of all {@link Runtime} objects
     */
    Set<Runtime> getRuntimes();

    /**
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return an immutable {@link Set} of filtered {@link Runtime} objects
     */
    Set<Runtime> getRuntimes(Predicate<Booster> filter);

    /**
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return an immutable {@link Set} of filtered {@link Version} objects
     */
    Set<Version> getVersions(Predicate<Booster> filter);

    /**
     * @param mission The {@link Mission} belonging to the {@link Version} objects
     * @param runtime The {@link Runtime} belonging to the {@link Version} objects
     * @return an immutable {@link Set} of filtered {@link Version} objects
     */
    Set<Version> getVersions(Mission mission, Runtime runtime);
}