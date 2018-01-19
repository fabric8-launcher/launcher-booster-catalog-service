/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import io.openshift.booster.catalog.BoosterCatalogService.Builder;
import org.arquillian.smart.testing.rules.git.server.GitServer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BoosterCatalogServiceTest {

    @ClassRule
    public static GitServer gitServer = GitServer.bundlesFromDirectory("repos/boosters")
            .fromBundle("gastaldi-booster-catalog","repos/custom-catalogs/gastaldi-booster-catalog.bundle")
            .usingPort(8765)
            .create();

    @Rule
    public final ProvideSystemProperty launcherProperties = new ProvideSystemProperty(LauncherConfiguration.PropertyName.LAUNCHER_GIT_HOST, "http://localhost:8765/")
            .and(LauncherConfiguration.PropertyName.LAUNCHER_BOOSTER_CATALOG_REPOSITORY, "http://localhost:8765/booster-catalog/");

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private static BoosterCatalogService defaultService;

    @Test
    public void testProcessMetadata() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        Path metadataFile = Paths.get(getClass().getResource("metadata.json").toURI());
        Map<String, Mission> missions = new HashMap<>();
        Map<String, Runtime> runtimes = new HashMap<>();

        service.processMetadata(metadataFile, missions, runtimes);

        softly.assertThat(missions).hasSize(5);
        softly.assertThat(runtimes).hasSize(3);
        softly.assertThat(runtimes.get("wildfly-swarm").getIcon()).isNotEmpty();
        softly.assertThat(missions.get("configmap").getDescription()).isNotEmpty();
    }

    @Test
    public void testIndex() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef()).build();
        softly.assertThat(service.getBoosters()).isEmpty();

        service.index().get();

        softly.assertThat(service.getBoosters()).isNotEmpty();
    }

    @Test
    public void testVertxVersions() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("vertx_two_versions")
                .build();
        service.index().get();

        Set<Version> versions = service.getVersions(new Mission("rest-http"), new Runtime("vert.x"));

        softly.assertThat(versions).hasSize(2);
    }

    @Test
    public void testCommonFiles() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("common_test")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(new Mission("rest-http"), new Runtime("vert.x"), new Version("community"));

        softly.assertThat(booster.isPresent()).isTrue();
        softly.assertThat(booster.get().<Boolean>getMetadata("root")).isEqualTo(true);
        softly.assertThat(booster.get().<String>getMetadata("mymission")).isEqualTo("rest-http");
        softly.assertThat(booster.get().<String>getMetadata("myruntime")).isEqualTo("vertx");
        softly.assertThat(booster.get().<String>getMetadata("myversion")).isEqualTo("community");
    }

    @Test
    public void testMetadata() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("common_test")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(new Mission("rest-http"), new Runtime("vert.x"), new Version("community"));

        softly.assertThat(booster.isPresent()).isTrue();
        softly.assertThat(booster.get().<Integer>getMetadata("sub/foo")).isEqualTo(3);
        softly.assertThat(booster.get().<Integer>getMetadata("sub/bar")).isEqualTo(5);
        softly.assertThat(booster.get().<Integer>getMetadata("sub/baz")).isEqualTo(4);
        softly.assertThat(booster.get().<Integer>getMetadata("sub/bam")).isEqualTo(7);
        softly.assertThat(booster.get().<Integer>getMetadata("sub/fox")).isEqualTo(8);
    }

    @Test
    public void testIgnore() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("ignore_test")
                .build();
        service.index().get();

        Set<Version> versions = service.getVersions(new Mission("rest-http"), new Runtime("vert.x"));

        softly.assertThat(versions).hasSize(1);
    }

    @Test
    public void testGetBoosterRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");

        Collection<Booster> boosters = service.getBoosters(BoosterFilters.runtimes(springBoot));

        softly.assertThat(boosters.size()).isGreaterThan(1);
    }

    @Test
    public void testGetMissionByRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");

        Set<Mission> missions = service.getMissions(BoosterFilters.runtimes(springBoot));

        softly.assertThat(missions.size()).isGreaterThan(1);
    }

    @Test
    public void testGetRuntimes() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();

        softly.assertThat(service.getRuntimes()).contains(new Runtime("spring-boot"), new Runtime("vert.x"),
                                                   new Runtime("wildfly-swarm"));
    }

    @Test
    public void testFilter() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef())
                .filter(b -> b.getRuntime().getId().equals("spring-boot")).build();

        service.index().get();

        softly.assertThat(service.getRuntimes()).containsOnly(new Runtime("spring-boot"));
    }

    @Test
    public void testListener() throws Exception {
        List<Booster> boosters = new ArrayList<>();
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef())
                .listener(boosters::add).filter(b -> boosters.size() == 1).build();

        service.index().get();

        softly.assertThat(boosters).containsAll(service.getBoosters());
    }

    @Test
    public void testBoosterContent() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");

        // Just get the first booster we can find
        Optional<Booster> booster = service.getBooster(BoosterFilters.runtimes(springBoot));

        softly.assertThat(booster.isPresent()).isTrue();
        
        File boosterFolder = booster.get().content().get().toFile();
        File pomFile = new File(boosterFolder, "pom.xml");
        softly.assertThat(pomFile.isFile()).isTrue();
    }

    private BoosterCatalogService buildDefaultCatalogService() {
        if (defaultService == null) {
            Builder builder = new BoosterCatalogService.Builder();
            String repo = LauncherConfiguration.boosterCatalogRepositoryURI();
            builder.catalogRepository(repo);
            String ref = LauncherConfiguration.boosterCatalogRepositoryRef();
            builder.catalogRef(ref);
            defaultService = builder.build();
        }
        return defaultService;
    }
}
