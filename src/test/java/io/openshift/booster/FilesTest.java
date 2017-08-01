/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class FilesTest
{
   @Rule
   public TemporaryFolder tempFolder = new TemporaryFolder();

   Path catalogZip;

   @Before
   public void setUp() throws IOException
   {
      catalogZip = Paths.get("src/test/resources/booster.zip");
   }

   /**
    * Test method for {@link io.openshift.booster.Files#unzip(java.nio.file.Path, java.nio.file.Path)}.
    */
   @Test
   public void testUnzip() throws Exception
   {
      Path targetDir = tempFolder.newFolder("tmpdir").toPath();
      Files.unzip(catalogZip, targetDir);
      assertThat(targetDir.resolve("metadata.json")).exists();
   }
}
