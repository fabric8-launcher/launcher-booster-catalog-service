/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

import org.assertj.core.api.Assertions.assertThat

class FilesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private var catalogZip: Path? = null

    private var targetDir: Path? = null

    @Before
    @Throws(IOException::class)
    fun configurePaths() {
        catalogZip = Paths.get("src/test/resources/booster.zip")
        targetDir = tempFolder.newFolder("boosters").toPath()
    }

    @Test
    @Throws(Exception::class)
    fun should_unzip_sample_booster_with_metadata() {
        // when
        Files.unzip(catalogZip!!, targetDir!!)
        val metadata = targetDir!!.resolve("metadata.yaml")

        // then
        assertThat(metadata).exists()
    }
}
