/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster;

import io.fabric8.launcher.booster.catalog.LauncherConfiguration;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService;
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider;
import org.yaml.snakeyaml.Yaml;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indexes a Booster catalog and logs any problems it finds to the console
 *
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
class BoosterCat {
    public static void main(String... args) throws Exception {
        String catalogRepository = args.length > 0 && !args[0].isEmpty() ? args[0] : LauncherConfiguration.boosterCatalogRepositoryURI();
        String catalogRef = args.length > 1 && !args[1].isEmpty() ? args[1] : LauncherConfiguration.boosterCatalogRepositoryRef();
        String env = args.length > 2 && !args[2].isEmpty() ? args[2] : "development";

        // Silence all INFO logging
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(Level.WARNING);
        }

        RhoarBoosterCatalogService build = new RhoarBoosterCatalogService.Builder()
                .pathProvider(new NativeGitBoosterCatalogPathProvider(catalogRepository, catalogRef, null))
                .build();

        Set<RhoarBooster> boosters = build.index().get();

        ArrayList list = new ArrayList(boosters.size());
        for (RhoarBooster metab : boosters) {
            RhoarBooster b = metab.forEnvironment(env);
            Map<String, Object> data = b.getExportableData();
            if (!env.isEmpty()) {
                data.remove("environment");
            }
            list.add(data);
        }

        Yaml yaml = new Yaml();
        yaml.dump(list, new OutputStreamWriter(System.out));
    }
}
