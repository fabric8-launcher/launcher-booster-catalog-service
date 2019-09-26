/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.fabric8.launcher.booster.catalog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.BoosterCatalogService.Builder;
import io.fabric8.launcher.booster.catalog.spi.NativeGitCatalogSourceProvider;
import io.fabric8.launcher.booster.catalog.utils.JsonKt;
import kotlin.jvm.functions.Function1;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BoosterCatalogServiceTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Nullable
    private static BoosterCatalogService defaultService;

    @Test
    public void testIndex() throws Exception {
        BoosterCatalogService service = defaultCatalogBuilder().build();
        softly.assertThat(service.getBoosters()).isEmpty();

        service.index().get();

        softly.assertThat(service.getBoosters()).isNotEmpty();
    }

    @Test
    public void testReindex() throws Exception {
        BoosterCatalogService service = defaultCatalogBuilder().build();
        softly.assertThat(service.getBoosters()).isEmpty();

        service.index().get();

        Optional<Booster> booster1 = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));
        softly.assertThat(booster1).hasValueSatisfying(b1 -> {
            Optional<Booster> booster2 = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));
            softly.assertThat(booster2).hasValueSatisfying(b2 -> {
                softly.assertThat(b2).isSameAs(b1);
            });

            assertThatCode(() -> {
                service.reindex().get();
            }).doesNotThrowAnyException();

            Optional<Booster> booster3 = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));
            softly.assertThat(booster3).hasValueSatisfying(b3 -> {
                softly.assertThat(b3).isNotSameAs(b1);
            });
        });
    }

    @Test
    public void testIgnore() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogProvider(() -> JsonKt.readCatalog(Paths.get("src/test/resources/custom-catalogs/test-catalog-ignore.json")))
                .build();
        service.index().get();

        Collection<Booster> boosters = service.getBoosters(missions("rest-http").and(runtimes("vert.x")));

        softly.assertThat(boosters).hasSize(1);
    }

    @Test
    public void testGetBoosterRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();

        Collection<Booster> boosters = service.getBoosters(runtimes("spring-boot"));

        softly.assertThat(boosters.size()).isGreaterThan(1);
    }

    @Test
    public void testFilter() throws Exception {
        BoosterCatalogService service = defaultCatalogBuilder()
                .filter(b -> "spring-boot".equals(b.getMetadata("runtime"))).build();

        service.index().get();

        softly.assertThat(service.getBoosters().size()).isGreaterThan(0);
        softly.assertThat(service.getBoosters().stream().map(b -> b.getMetadata("runtime"))).containsOnly("spring-boot");
    }

    @Test
    public void testListener() throws Exception {
        List<Booster> boosters = new ArrayList<>();
        BoosterCatalogService service = defaultCatalogBuilder()
                .listener(boosters::add).filter(b -> boosters.size() == 1).build();

        service.index().get();

        softly.assertThat(boosters).containsAll(service.getBoosters());
    }

    @Test
    public void testBoosterContent() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();

        // Just get the first booster we can find
        Optional<Booster> booster = service.getBooster(runtimes("spring-boot"));
        softly.assertThat(booster).hasValueSatisfying(b -> {
            assertThatCode(() -> {
                File boosterFolder = b.content().get().toFile();
                File pomFile = new File(boosterFolder, "pom.xml");
                softly.assertThat(pomFile.isFile()).isTrue();
            }).doesNotThrowAnyException();
        });
    }

    @Test
    public void testBoosterFetchRecovery() throws Exception {
        UnreliablePathProvider unreliableProvider = new UnreliablePathProvider();
        BoosterCatalogService service = defaultCatalogBuilder()
                .sourceProvider((Booster b) -> unreliableProvider.getFetchSource().invoke(b))
                .build();
        service.index().get();

        // Just get the first booster we can find
        Optional<Booster> booster = service.getBooster(runtimes("spring-boot"));
        softly.assertThat(booster).hasValueSatisfying(b -> {
            // First we test a failing condition
            assertThatThrownBy(() -> b.content().get()).hasMessageContaining("Fail flag is true");

            // Now we reset the fail status
            unreliableProvider.fail = false;

            assertThatCode(() -> {
                // And now we test if the service can recover from the previous error
                Path boosterFolder = b.content().get();
                softly.assertThat(boosterFolder.resolve("pom.xml")).isRegularFile();
            }).doesNotThrowAnyException();
        });
    }

    private static Predicate<Booster> missions(@Nullable String mission) {
        return (Booster b) -> mission == null || mission.equals(b.getMetadata("mission"));
    }

    private static Predicate<Booster> runtimes(@Nullable String runtime) {
        return (Booster b) -> runtime == null || runtime.equals(b.getMetadata("runtime"));
    }

    private static Predicate<Booster> versions(@Nullable String version) {
        return (Booster b) -> version == null || version.equals(b.getMetadata("version"));
    }

    private AbstractBoosterCatalogService.AbstractBuilder<Booster, BoosterCatalogService> defaultCatalogBuilder() {
        return new Builder()
                    .catalogProvider(() -> JsonKt.readCatalog(Paths.get("src/test/resources/custom-catalogs/test-catalog.json")));
    }

    private BoosterCatalogService buildDefaultCatalogService() {
        if (defaultService == null) {
            defaultService = defaultCatalogBuilder().build();
        }
        return defaultService;
    }

    private static class UnreliablePathProvider extends NativeGitCatalogSourceProvider {
        public boolean fail = true;

        public UnreliablePathProvider() {
            super(null);
        }

        @NotNull
        @Override
        public Function1<Booster, Path> getFetchSource() {
            return (Booster b) -> {
                if (fail) {
                    throw new RuntimeException("Fail flag is true");
                }
                return super.getFetchSource().invoke(b);
            };
        }
    }
}
