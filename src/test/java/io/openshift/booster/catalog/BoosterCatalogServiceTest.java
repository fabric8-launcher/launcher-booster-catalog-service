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

import org.junit.Test;

import io.openshift.booster.catalog.BoosterCatalogService;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogServiceTest
{

   /**
    * Test method for
    * {@link io.openshift.booster.catalog.BoosterCatalogService#processIndex(java.nio.file.Path, java.util.Map, java.util.Map)}.
    */
   @Test
   public void testProcessIndex() throws Exception
   {
      BoosterCatalogService service = new BoosterCatalogService();
      Path metadataFile = Paths.get(getClass().getResource("metadata.json").toURI());
      Map<String, Mission> missions = new HashMap<>();
      Map<String, Runtime> runtimes = new HashMap<>();
      service.processMetadata(metadataFile, missions, runtimes);
      assertThat(missions).hasSize(5);
      assertThat(runtimes).hasSize(3);
   }

}
