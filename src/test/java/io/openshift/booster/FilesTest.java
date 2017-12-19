/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class FilesTest {

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private Path catalogZip;

    private Path targetDir;

    @Before
    public void configurePaths() throws IOException {
        catalogZip = Paths.get("src/test/resources/booster.zip");
        targetDir = tempFolder.newFolder("boosters").toPath();
    }

    @Test
    public void should_unzip_sample_booster_with_metadata() throws Exception {
        // when
        Files.unzip(catalogZip, targetDir);
        final Path metadata = targetDir.resolve("metadata.json");

        // then
        assertThat(metadata).exists();
    }
}
