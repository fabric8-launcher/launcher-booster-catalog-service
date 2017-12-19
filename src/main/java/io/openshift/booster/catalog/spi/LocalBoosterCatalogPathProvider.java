/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog.spi;

import io.openshift.booster.catalog.Booster;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class LocalBoosterCatalogPathProvider implements BoosterCatalogPathProvider {
    public LocalBoosterCatalogPathProvider(URL catalogZip) {
        this.catalogZipURL = catalogZip;
    }

    private static final Logger logger = Logger.getLogger(LocalBoosterCatalogPathProvider.class.getName());

    private final URL catalogZipURL;

    @Override
    public Path createCatalogPath() throws IOException {
        Path catalogPath = Files.createTempDirectory("booster-catalog");
        logger.info("Unzipping booster contents to " + catalogPath);
        Path catalogPathZip = catalogPath.resolve("booster.zip");
        // Copy to temp folder
        try (InputStream is = catalogZipURL.openStream()) {
            Files.copy(is, catalogPathZip);
        }
        io.openshift.booster.Files.unzip(catalogPathZip, catalogPath);
        Files.delete(catalogPathZip);
        return catalogPath;
    }

    @Override
    public Path createBoosterContentPath(Booster booster) {
        throw new IllegalStateException("Could not find the content path for " + booster);
    }
}