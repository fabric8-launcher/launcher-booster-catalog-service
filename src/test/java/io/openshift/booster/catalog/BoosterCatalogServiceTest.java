/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import io.openshift.booster.catalog.BoosterCatalogService.Builder;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogServiceTest
{

   private BoosterCatalogService buildCatalogService()
   {
      Builder builder = new BoosterCatalogService.Builder();
      String repo = System.getenv("LAUNCHPAD_BACKEND_CATALOG_GIT_REPOSITORY");
      if (repo != null)
      {
         builder.catalogRepository(repo);
      }
      String ref = System.getenv("LAUNCHPAD_BACKEND_CATALOG_GIT_REF");
      if (ref != null)
      {
         builder.catalogRef(ref);
      }
      return builder.build();
   }

   @Test
   public void testProcessMetadata() throws Exception
   {
      BoosterCatalogService service = buildCatalogService();
      Path metadataFile = Paths.get(getClass().getResource("metadata.json").toURI());
      Map<String, Mission> missions = new HashMap<>();
      Map<String, Runtime> runtimes = new HashMap<>();
      service.processMetadata(metadataFile, missions, runtimes);
      assertThat(missions).hasSize(5);
      assertThat(runtimes).hasSize(3);
   }

   @Test
   public void testIndex() throws Exception
   {
      BoosterCatalogService service = buildCatalogService();
      assertThat(service.getBoosters()).isEmpty();
      service.index().get();
      assertThat(service.getBoosters()).isNotEmpty();
   }

   @Test
   public void testVertxVersions() throws Exception
   {
      BoosterCatalogService service = new BoosterCatalogService.Builder()
               .catalogRepository("https://github.com/gastaldi/booster-catalog.git").catalogRef("vertx_two_versions")
               .build();
      service.index().get();
      assertThat(service.getBoosters()).hasSize(2);
      Set<Version> versions = service.getVersions(new Mission("rest-http"), new Runtime("vert.x"));
      assertThat(versions).hasSize(2);
   }
}
