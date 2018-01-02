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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoosterCatalogServiceTest {

    @ClassRule
    public static GitServer gitServer = GitServer.bundlesFromDirectory("repos/boosters")
            .fromBundle("chirino-booster-catalog","repos/custom-catalogs/chirino-booster-catalog.bundle")
            .fromBundle("gastaldi-booster-catalog","repos/custom-catalogs/gastaldi-booster-catalog.bundle")
            .usingPort(8765)
            .create();

    @Rule
    public final ProvideSystemProperty launcherProperties = new ProvideSystemProperty("LAUNCHER_GIT_HOST", "http://localhost:8765/")
            .and("LAUNCHER_BOOSTER_CATALOG_REPOSITORY", "http://localhost:8765/booster-catalog/");

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
    }

    @Test
    public void testIndex() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(Configuration.catalogRepositoryRef()).build();
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
    public void testLabels() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/chirino-booster-catalog").catalogRef("filter_test")
                .build();

        service.index().get();

        softly.assertThat(service.getBoosters("vert.x")).hasSize(2);
        softly.assertThat(service.getBoosters("redhat")).hasSize(1);
        softly.assertThat(service.getBoosters("community")).hasSize(1);
        softly.assertThat(service.getBoosters("vert.x", "redhat")).hasSize(1);
        softly.assertThat(service.getBoosters("community", "redhat")).hasSize(0);
    }

    @Test
    public void testGetBoosterRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");

        Collection<Booster> boosters = service.getBoosters(springBoot);

        softly.assertThat(boosters.size()).isGreaterThan(1);
    }

    @Test
    public void testGetMissionByRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");

        Set<Mission> missions = service.getMissions(springBoot);

        softly.assertThat(missions.size()).isGreaterThan(1);
    }

    @Test
    public void testGetMissionByDeploymentType() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();

        Set<Mission> missions = service.selector().deploymentType(DeploymentType.ZIP).getMissions();

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
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(Configuration.catalogRepositoryRef())
                .filter(b -> b.getRuntime().getId().equals("spring-boot")).build();

        service.index().get();

        softly.assertThat(service.getRuntimes()).containsOnly(new Runtime("spring-boot"));
    }

    @Test
    public void testListener() throws Exception {
        List<Booster> boosters = new ArrayList<>();
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(Configuration.catalogRepositoryRef())
                .listener(boosters::add).filter(b -> boosters.size() == 1).build();

        service.index().get();

        softly.assertThat(boosters).containsAll(service.getBoosters());
    }

    private BoosterCatalogService buildDefaultCatalogService() {
        if (defaultService == null) {
            Builder builder = new BoosterCatalogService.Builder();
            String repo = Configuration.catalogRepositoryURI();
            builder.catalogRepository(repo);
            String ref = Configuration.catalogRepositoryRef();
            builder.catalogRef(ref);
            defaultService = builder.build();
        }
        return defaultService;
    }
}
