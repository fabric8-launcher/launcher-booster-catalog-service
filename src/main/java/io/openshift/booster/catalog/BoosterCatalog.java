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
import java.util.function.Predicate;

/**
 * General operations for a set of {@link Booster} objects
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public interface BoosterCatalog<BOOSTER extends Booster> {
    /**
     * Copies the {@link Booster} contents to the specified {@link Path}
     */
    Path copy(BOOSTER booster, Path projectRoot) throws IOException;

    /**
     * Returns a {@link Collection} of {@link Booster} objects.
     * 
     * @return a {@link Collection} of {@link Booster} objects
     */
    Collection<BOOSTER> getBoosters();

    /**
     * Returns a {@link Collection} of {@link Booster} objects, filtered using a {@link Predicate}.
     *
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return a {@link Collection} of filtered {@link Booster} objects
     */
    Collection<BOOSTER> getBoosters(Predicate<BOOSTER> filter);

    /**
     * @param filter A {@link Predicate} used to filter the {@link Booster} objects
     * @return an {@link Optional} for the given method parameters
     */
    Optional<BOOSTER> getBooster(Predicate<BOOSTER> filter);
}