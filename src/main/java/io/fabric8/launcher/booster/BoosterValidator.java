/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster;

import io.fabric8.launcher.booster.catalog.Booster;
import io.fabric8.launcher.booster.catalog.BoosterCatalogService;
import io.fabric8.launcher.booster.catalog.LauncherConfiguration;
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indexes a Booster catalog and logs any problems it finds to the console
 *
 * @author <a href="mailto:tschotan@redhat.com">Tako Schotanus</a>
 */
class BoosterValidator {
    public static void main(String... args) throws Exception {
        String catalogRepository = args.length > 0 ? args[0] : LauncherConfiguration.boosterCatalogRepositoryURI();
        String catalogRef = args.length > 1 ? args[1] : LauncherConfiguration.boosterCatalogRepositoryRef();

        // Silence all INFO logging
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for ( int index = 0; index < handlers.length; index++ ) {
            handlers[index].setLevel(Level.WARNING);
        }

        BoosterCatalogService build = new BoosterCatalogService.Builder()
                .pathProvider(new NativeGitBoosterCatalogPathProvider(catalogRepository, catalogRef, null))
                .build();

        System.out.println("Validating Booster Catalog " + catalogRepository + "#" + catalogRef);
        System.out.println("Fetching index...");
        Set<Booster> boosters = build.index().get();
        System.out.println("Done.");

        System.out.println("Fetching boosters...");
        AtomicInteger errcnt = new AtomicInteger(0);
        ArrayList<CompletableFuture<Path>> futures = new ArrayList<>();
        for (Booster b : boosters) {
            futures.add(b.content().whenComplete((path, throwable) -> {
                if (throwable != null) {
                    System.err.println("ERROR: Couldn't fetch Booster " + b.getName());
                    errcnt.incrementAndGet();
                } else {
                    System.out.println("Fetched " + b.getName());
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        if (errcnt.get() == 0) {
            System.out.println("Done. No problems found.");
        } else {
            System.out.println("Done. " + errcnt.get() + " errors were encountered.");
            System.exit(1);
        }
    }

}
