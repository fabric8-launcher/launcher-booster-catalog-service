/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import static io.openshift.booster.Files.removeFileExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.yaml.snakeyaml.Yaml;

import io.openshift.booster.CopyFileVisitor;

/**
 * This service reads from the Booster catalog Github repository in https://github.com/openshiftio/booster-catalog and
 * marshalls into {@link Booster} objects.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogService
{
   private static final String GITHUB_URL = "https://github.com/";
   private static final String CLONED_BOOSTERS_DIR = ".boosters";
   private static final String METADATA_FILE = "metadata.json";

   private static final Yaml yaml = new Yaml(new YamlConstructor());
   /**
    * Files to be excluded from project creation
    */
   private static final List<String> EXCLUDED_PROJECT_FILES = Arrays.asList(".git", ".travis", ".travis.yml",
            ".ds_store",
            ".obsidian", ".gitmodules");

   private static final Logger logger = Logger.getLogger(BoosterCatalogService.class.getName());

   private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();

   private volatile List<Booster> boosters = Collections.emptyList();

   private String catalogRepositoryURI;
   private String catalogRef;

   private BoosterCatalogService(String catalogRepositoryURI, String catalogRef)
   {
      this.catalogRepositoryURI = catalogRepositoryURI;
      this.catalogRef = catalogRef;
   }

   /**
    * Clones the catalog git repository and reads the obsidian metadata on each quickstart repository
    */
   public void index()
   {
      WriteLock lock = reentrantLock.writeLock();
      try
      {
         lock.lock();
         logger.log(Level.INFO, "Indexing contents from {0} using {1} ref",
                  new Object[] { catalogRepositoryURI, catalogRef });
         Path catalogPath = Files.createTempDirectory("booster-catalog");
         // Remove this directory on JVM termination
         catalogPath.toFile().deleteOnExit();
         logger.info(() -> "Created " + catalogPath);
         // Clone repository
         Git.cloneRepository()
                  .setURI(catalogRepositoryURI)
                  .setBranch(catalogRef)
                  .setCloneSubmodules(true)
                  .setDirectory(catalogPath.toFile())
                  .call().close();
         this.boosters = indexBoosters(catalogPath);
      }
      catch (GitAPIException e)
      {
         logger.log(Level.SEVERE, "Error while performing Git operation", e);
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Error while indexing", e);
      }
      finally
      {
         logger.info(() -> "Finished content indexing");
         lock.unlock();
      }
   }

   /**
    * @param moduleRoot
    * @return
    * @throws IOException
    */
   private List<Booster> indexBoosters(final Path catalogPath) throws IOException
   {
      Path moduleRoot = catalogPath.resolve(CLONED_BOOSTERS_DIR);
      Path metadataFile = catalogPath.resolve(METADATA_FILE);
      List<Booster> boosters = new ArrayList<>();
      Map<String, Mission> missions = new HashMap<>();
      Map<String, Runtime> runtimes = new HashMap<>();
      Map<String, Version> versions = new HashMap<>();
      if (Files.exists(metadataFile))
      {
         processMetadata(metadataFile, missions, runtimes);
      }
      Files.walkFileTree(catalogPath, new SimpleFileVisitor<Path>()
      {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
         {
            if (Thread.interrupted())
            {
               return FileVisitResult.TERMINATE;
            }
            File ioFile = file.toFile();
            String fileName = ioFile.getName().toLowerCase();
            // Skip any file that starts with .
            if (!fileName.startsWith(".") && (fileName.endsWith(".yaml") || fileName.endsWith(".yml")))
            {
               String id = removeFileExtension(fileName);
               Path modulePath = moduleRoot.resolve(id);
               indexBooster(id, catalogPath, file, modulePath, missions, runtimes, versions).ifPresent(boosters::add);
            }
            return FileVisitResult.CONTINUE;
         }

         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
         {
            if (Thread.interrupted())
            {
               return FileVisitResult.TERMINATE;
            }
            return dir.startsWith(moduleRoot) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
         }
      });
      boosters.sort(Comparator.comparing(Booster::getName));
      return Collections.unmodifiableList(boosters);
   }

   /**
    * Process the metadataFile and adds to the specified missions and runtimes maps
    * 
    * @param metadataFile
    * @param missions
    * @param runtimes
    */
   void processMetadata(Path metadataFile, Map<String, Mission> missions, Map<String, Runtime> runtimes)
   {
      logger.info(() -> "Reading metadata at " + metadataFile + " ...");

      try (BufferedReader reader = Files.newBufferedReader(metadataFile);
               JsonReader jsonReader = Json.createReader(reader))
      {
         JsonObject index = jsonReader.readObject();
         index.getJsonArray("missions")
                  .stream()
                  .map(JsonObject.class::cast)
                  .map(e -> new Mission(e.getString("id"), e.getString("name")))
                  .forEach(m -> missions.put(m.getId(), m));

         index.getJsonArray("runtimes")
                  .stream()
                  .map(JsonObject.class::cast)
                  .map(e -> new Runtime(e.getString("id"), e.getString("name")))
                  .forEach(r -> runtimes.put(r.getId(), r));
      }
      catch (IOException e)
      {
         logger.log(Level.SEVERE, "Error while processing metadata " + metadataFile, e);
      }
   }

   /**
    * Takes a YAML file from the repository and indexes it
    * 
    * @param file A YAML file from the booster-catalog repository
    * @return an {@link Optional} containing a {@link Booster}
    */
   @SuppressWarnings("unchecked")
   private Optional<Booster> indexBooster(String id, Path catalogPath, Path file, Path moduleDir, Map<String, Mission> missions,
            Map<String, Runtime> runtimes, Map<String, Version> versions)
   {
      logger.info(() -> "Indexing " + file + " ...");

      Booster booster = null;
      try (BufferedReader reader = Files.newBufferedReader(file))
      {
         // Read YAML entry
         booster = yaml.loadAs(reader, Booster.class);
      }
      catch (IOException e)
      {
         logger.log(Level.SEVERE, "Error while reading " + file, e);
      }
      if (booster != null)
      {
         try
         {
            // Booster ID = filename without extension
            booster.setId(id);

            String versionId;
            String runtimeId;
            String missionId;
            if (file.getParent().getParent().getParent().equals(catalogPath)) {
                versionId = null;
                runtimeId = file.getParent().toFile().getName();
                missionId = file.getParent().getParent().toFile().getName();
            } else {
                versionId = file.getParent().toFile().getName();
                runtimeId = file.getParent().getParent().toFile().getName();
                missionId = file.getParent().getParent().getParent().toFile().getName();
            }

            booster.setMission(missions.computeIfAbsent(missionId, Mission::new));
            booster.setRuntime(runtimes.computeIfAbsent(runtimeId, Runtime::new));
            if (versionId != null) {
                booster.setVersion(versions.computeIfAbsent(versionId, Version::new));
            }

            booster.setContentPath(moduleDir);
            // Module does not exist. Clone it
            if (Files.notExists(moduleDir))
            {
               try (Git git = Git.cloneRepository()
                        .setDirectory(moduleDir.toFile())
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
            }
            Path metadataPath = moduleDir.resolve(booster.getBoosterDescriptorPath());
            try (BufferedReader metadataReader = Files.newBufferedReader(metadataPath))
            {
               Map<String, Object> metadata = yaml.loadAs(metadataReader, Map.class);
               booster.setMetadata(metadata);
            }

            Path descriptionPath = moduleDir.resolve(booster.getBoosterDescriptionPath());
            if (Files.exists(descriptionPath))
            {
               byte[] descriptionContent = Files.readAllBytes(descriptionPath);
               booster.setDescription(new String(descriptionContent));
            }
         }
         catch (GitAPIException gitException)
         {
            logger.log(Level.SEVERE, "Error while reading git repository", gitException);
         }
         catch (Exception e)
         {
            logger.log(Level.SEVERE, "Error while reading metadata from " + file, e);
         }
      }
      return Optional.ofNullable(booster);
   }

   /**
    * Copies the {@link Booster} contents to the specified {@link Path}
    */
   public Path copy(Booster booster, Path projectRoot) throws IOException
   {
      Path modulePath = booster.getContentPath();
      return Files.walkFileTree(modulePath,
               new CopyFileVisitor(projectRoot,
                        (p) -> !EXCLUDED_PROJECT_FILES.contains(p.toFile().getName().toLowerCase())));
   }

   public Set<Mission> getMissions()
   {
      return boosters.stream()
               .map(Booster::getMission)
               .collect(Collectors.toCollection(TreeSet::new));
   }

   public Set<Runtime> getRuntimes(Mission mission)
   {
      if (mission == null)
      {
         return Collections.emptySet();
      }
      return boosters.stream()
               .filter(b -> mission.equals(b.getMission()))
               .map(Booster::getRuntime)
               .collect(Collectors.toCollection(TreeSet::new));
   }

   public Set<Version> getVersions(Mission mission, Runtime runtime)
   {
      if (mission == null || runtime == null)
      {
         return Collections.emptySet();
      }
      return boosters.stream()
               .filter(b -> mission.equals(b.getMission()))
               .filter(b -> runtime.equals(b.getRuntime()))
               .filter(b -> b.getVersion() != null)
               .map(Booster::getVersion)
               .collect(Collectors.toCollection(TreeSet::new));
   }

   public Optional<Booster> getBooster(Mission mission, Runtime runtime)
   {
       return getBooster(mission, runtime, null);
   }

   public Optional<Booster> getBooster(Mission mission, Runtime runtime, Version version)
   {
      Objects.requireNonNull(mission, "Mission should not be null");
      Objects.requireNonNull(runtime, "Runtime should not be null");
      return boosters.stream()
               .filter(b -> mission.equals(b.getMission()))
               .filter(b -> runtime.equals(b.getRuntime()))
               .filter(b -> version == null || version.equals(b.getVersion()))
               .findFirst();
   }

   public List<Booster> getBoosters()
   {
      Lock readLock = reentrantLock.readLock();
      try
      {
         readLock.lock();
         return boosters;
      }
      finally
      {
         readLock.unlock();
      }
   }

   /**
    * {@link BoosterCatalogService} Builder class
    *
    * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
    */
   public static class Builder
   {
      private String catalogRepositoryURI = "https://github.com/openshiftio/booster-catalog.git";
      private String catalogRef = "master";

      public Builder catalogRef(String catalogRef)
      {
         this.catalogRef = catalogRef;
         return this;
      }

      public Builder catalogRepository(String catalogRepositoryURI)
      {
         this.catalogRepositoryURI = catalogRepositoryURI;
         return this;
      }

      public BoosterCatalogService build()
      {
         return new BoosterCatalogService(catalogRepositoryURI, catalogRef);
      }
   }
}
