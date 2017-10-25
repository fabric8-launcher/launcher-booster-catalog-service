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
 * General operations for a set of {@link Booster} objects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
public interface BoosterCatalog
{
   /**
    * Copies the {@link Booster} contents to the specified {@link Path}
    */
   Path copy(Booster booster, Path projectRoot) throws IOException;

   /**
    * Returns a {@link Collection} of {@link Booster} objects, filtering by a set of labels.
    * 
    * @param mission The {@link Mission} belonging to the {@link Booster} objects
    * @param runtime The {@link Runtime} belonging to the {@link Booster} objects
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return a {@link Collection} of {@link Booster} objects
    */
   Collection<Booster> getBoosters(String... labels);

   // Query methods

   /**
    * 
    * @param runtime The {@link Runtime} belonging to the {@link Version} objects
    * @param labels labels for filtering purposes
    * @return a {@link Collection} of {@link Booster} objects
    */
   Collection<Booster> getBoosters(Runtime runtime, String... labels);

   /**
    * @param labels labels for filtering purposes
    * @return an immutable {@link Set} of {@link Mission} matching the given labels
    */
   Set<Mission> getMissions(String... labels);

   /**
    * @param labels labels for filtering purposes
    * @return an immutable {@link Set} of {@link Mission} matching the given {@link Runtime} and labels
    */
   Set<Mission> getMissions(Runtime runtime, String... labels);

   /**
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return an immutable {@link Set} of {@link Runtime} matching the given labels
    */
   Set<Runtime> getRuntimes(String... labels);

   /**
    * @param mission The {@link Mission} belonging to the {@link Runtime} objects
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return an immutable {@link Set} of {@link Runtime} matching the given labels
    */
   Set<Runtime> getRuntimes(Mission mission, String... labels);

   /**
    * @param mission The {@link Mission} belonging to the {@link Version} objects
    * @param runtime The {@link Runtime} belonging to the {@link Version} objects
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return an immutable {@link Set} of {@link Version} matching the given labels
    */
   Set<Version> getVersions(Mission mission, Runtime runtime, String... labels);

   /**
    * @param mission The {@link Mission} belonging to the {@link Booster} object
    * @param runtime The {@link Runtime} belonging to the {@link Booster} object
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return an {@link Optional} for the given method parameters
    */
   Optional<Booster> getBooster(Mission mission, Runtime runtime, String... labels);

   /**
    * @param mission The {@link Mission} belonging to the {@link Booster} objects
    * @param runtime The {@link Runtime} belonging to the {@link Booster} objects
    * @param labels The labels belonging to the {@link Runtime} objects
    * @return an {@link Optional} for the given method parameters
    */
   Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version, String... labels);

   /**
    * Creates a new <code>Selector</code>
    * @return A selector
    */
   Selector selector();

   /**
    * Performs filtering on a set of {@link Booster} objects and returns
    * the set of {@link Runtime}s, {@link Mission}s, {@link Version}s or
    * {@link Booster}s that satisfy the filters
    */
   public interface Selector
   {
      /**
       * Apply a {@link Runtime} filter to all the boosters
       * @param runtime The runtime to filter on
       * @return The selector itself for chaining calls
       */
      Selector runtime(Runtime runtime);
      
      /**
       * Apply a <code>Misson</code> filter to all the boosters
       * @param mission The mission to filter on
       * @return The selector itself for chaining calls
       */
      Selector mission(Mission mission);
      
      /**
       * Apply a {@link Version} filter to all the boosters
       * @param version The version to filter on
       * @return The selector itself for chaining calls
       */
      Selector version(Version version);
      
      /**
       * Filter out any boosters that don't have all the given labels
       * @param labels The labels to filter on
       * @return The selector itself for chaining calls
       */
      Selector labels(String[] labels);
      
      /**
       * Returns all the {@link Runtime}s that satisfy the applied filters
       * @return A set of runtimes
       */
      Set<Runtime> getRuntimes();
      
      /**
       * Returns all the {@link Mission}s that satisfy the applied filters
       * @return A set of missions
       */
      Set<Mission> getMissions();
      
      /**
       * Returns all the {@link Version}s that satisfy the applied filters
       * @return A set of versions
       */
      Set<Version> getVersions();
      
      /**
       * Returns all the {@link Booster}s that satisfy the applied filters
       * @return A collection of boosters
       */
      Collection<Booster> getBoosters();
      
      /**
       * Returns the {@link Booster} that satisfies the applied filters.
       * (In the case that several boosters are found one will be selected)
       * @return An optional booster value
       */
      Optional<Booster> getBooster();
   }
}