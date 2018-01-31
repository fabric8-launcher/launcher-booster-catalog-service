/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog.spi;

import io.fabric8.launcher.booster.catalog.BoosterCatalog;
import io.fabric8.launcher.booster.catalog.Booster;

/**
 * Listens for {@link BoosterCatalog} events
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface BoosterCatalogListener {

    /**
     * Invoked when a booster is added to the catalog
     *
     * @param booster
     */
    void boosterAdded(Booster booster);

}
