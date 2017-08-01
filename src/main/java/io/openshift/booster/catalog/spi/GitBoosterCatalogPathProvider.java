/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import io.openshift.booster.catalog.Booster;

/**
 * Default implementation for {@link BoosterCatalogPathProvider}
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class GitBoosterCatalogPathProvider implements BoosterCatalogPathProvider
{
   private static final Logger logger = Logger.getLogger(GitBoosterCatalogPathProvider.class.getName());
   private static final String GITHUB_URL = "https://github.com/";

   private final String catalogRepositoryURI;
   private final String catalogRef;
   private final Path rootDir;

   /**
    * @param catalogRepositoryURI
    * @param catalogRef
    */
   public GitBoosterCatalogPathProvider(String catalogRepositoryURI, String catalogRef, Path rootDir)
   {
      super();
      this.catalogRepositoryURI = catalogRepositoryURI;
      this.catalogRef = catalogRef;
      this.rootDir = rootDir;
   }

   @Override
   public Path createCatalogPath() throws IOException
   {
      logger.log(Level.INFO, "Indexing contents from {0} using {1} ref",
               new Object[] { catalogRepositoryURI, catalogRef });
      Path catalogPath = rootDir;
      if (catalogPath == null)
      {
         catalogPath = Files.createTempDirectory("booster-catalog");
         logger.info("Created " + catalogPath);
      }
      try
      {
         Git.cloneRepository()
                  .setURI(catalogRepositoryURI)
                  .setBranch(catalogRef)
                  .setCloneSubmodules(true)
                  .setDirectory(catalogPath.toFile())
                  .call()
                  .close();
      }
      catch (GitAPIException e)
      {
         throw new IOException("Error while performing Git operation", e);
      }
      return catalogPath;
   }

   @Override
   public Path createBoosterContentPath(Booster booster) throws IOException
   {
      try (Git git = Git.cloneRepository()
               .setDirectory(booster.getContentPath().toFile())
               .setURI(GITHUB_URL + booster.getGithubRepo())
               .setCloneSubmodules(true)
               .setBranch(booster.getGitRef())
               .call())
      {
         // Checkout on specified start point
         git.checkout()
                  .setName(booster.getGitRef())
                  .setStartPoint(booster.getGitRef())
                  .call();
      }
      catch (GitAPIException e)
      {
         throw new IOException("Error while reading git repository", e);
      }
      return booster.getContentPath();
   }
}