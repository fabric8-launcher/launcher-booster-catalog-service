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
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
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
        for (Booster metab : boosters) {
            for (String env : metab.getEnvironments().keySet()) {
                Booster b = metab.forEnvironment(env);
                futures.add(b.content().whenComplete((path, throwable) -> {
                    if (throwable != null) {
                        System.err.println("ERROR: Couldn't fetch Booster " + b.getId());
                        errcnt.incrementAndGet();
                    } else {
                        System.out.println("Fetched " + b.getId() + " (" + b.getName() + " for " + env + ")");
                        if (!validateBoosterData(b, path, b.getData())) {
                            errcnt.incrementAndGet();
                        }
                        if (!validOpenshiftYamlFiles(b, path)) {
                            errcnt.incrementAndGet();
                        }
                        System.out.flush();
                        System.err.flush();
                    }
                }));
            }
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
        }
        if (errcnt.get() == 0) {
            System.out.println("Done. No problems found.");
        } else {
            System.out.println("Done. " + errcnt.get() + " errors were encountered.");
            System.exit(1);
        }
    }

    private static boolean validOpenshiftYamlFiles(Booster booster, Path path) {
        final Yaml yaml = new Yaml();
        AtomicBoolean valid = new AtomicBoolean(true);
        try {
            Files.walk(path)
                    .filter(p -> p.getParent().getFileName().toString().equals(".openshiftio"))
                    .filter(p -> p.getFileName().toString().endsWith(".yaml") || p.getFileName().toString().endsWith(".yml"))
                    .forEach(p -> {
                        System.out.println("    Validating " + booster.getId() + " - " + path.relativize(p));
                        try (BufferedReader reader = Files.newBufferedReader(p)) {
                            yaml.loadAs(reader, Map.class);
                        } catch (Exception e) {
                            System.err.println("    ERROR: Parse error in " + booster.getId() + " - " + path.relativize(p) + ": " + e.getMessage());
                            valid.set(false);
                        }
                    });
        } catch (IOException e) {
            System.err.println("    ERROR: IO error: " + booster.getId() + " - " + e.getMessage());
            valid.set(false);
        }
        return valid.get();
    }

    private static boolean validateBoosterData(Booster booster, Path path, Map data) {
        boolean valid = true;
        Set<Map.Entry> entries = data.entrySet();
        for (Map.Entry entry : entries) {
            String key = entry.getKey().toString();
            if (key.contains(" ")) {
                System.err.println("    ERROR: Parse error in " + booster.getId() + " - " + path.relativize(path) + ": Keys should not contain spaces: '" + key + "'");
                valid = false;
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                valid = valid && validateBoosterData(booster, path, (Map)value);
            } else if (value instanceof Iterable) {
                valid = valid && validateBoosterData(booster, path, (Iterable)value);
            }
        }
        return valid;
    }

    private static boolean validateBoosterData(Booster booster, Path path, Iterable data) {
        boolean valid = true;
        for (Object value : data) {
            if (value instanceof Map) {
                valid = valid && validateBoosterData(booster, path, (Map)value);
            } else if (value instanceof Iterable) {
                valid = valid && validateBoosterData(booster, path, (Iterable)value);
            }
        }
        return valid;
    }
}
