/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi;

import java.io.IOException;
import java.nio.file.Path;

import io.fabric8.launcher.booster.catalog.Booster;
import io.fabric8.launcher.booster.catalog.BoosterCatalogService;

/**
 * A SPI interface used in {@link BoosterCatalogService}
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface BoosterCatalogPathProvider {
    /**
     * Returns the path where the booster catalog is (for indexing)
     */
    Path createCatalogPath() throws IOException;

    /**
     * Creates the content for a given booster and returns the target {@link Path}
     */
    Path createBoosterContentPath(Booster booster) throws IOException;
}
