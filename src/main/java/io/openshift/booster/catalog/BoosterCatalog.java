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

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface BoosterCatalog
{
   /**
    * Copies the {@link Booster} contents to the specified {@link Path}
    */
   Path copy(Booster booster, Path projectRoot) throws IOException;

   Set<Mission> getMissions(String ...labels);

   Set<Runtime> getRuntimes(Mission mission, String ...labels);

   Set<Version> getVersions(Mission mission, Runtime runtime, String ...labels);

   Optional<Booster> getBooster(Mission mission, Runtime runtime, String ...labels);

   Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version, String ...labels);

   Collection<Booster> getBoosters(String ...labels);

}