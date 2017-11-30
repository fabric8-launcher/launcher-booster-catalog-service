/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.booster.catalog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openshift.booster.catalog.BoosterCatalogService.Builder;
import io.openshift.booster.catalog.spi.BoosterCatalogListener;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class BoosterCatalogServiceTest {
    private static BoosterCatalogService defaultService;

    @Test
    public void testProcessMetadata() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        Path metadataFile = Paths.get(getClass().getResource("metadata.json").toURI());
        Map<String, Mission> missions = new HashMap<>();
        Map<String, Runtime> runtimes = new HashMap<>();
        service.processMetadata(metadataFile, missions, runtimes);
        assertThat(missions).hasSize(5);
        assertThat(runtimes).hasSize(3);
    }

    @Test
    public void testIndex() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef("openshift-online-free").build();
        assertThat(service.getBoosters()).isEmpty();
        service.index().get();
        assertThat(service.getBoosters()).isNotEmpty();
    }

    @Test
    public void testVertxVersions() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("https://github.com/gastaldi/booster-catalog.git").catalogRef("vertx_two_versions")
                .build();
        service.index().get();
        assertThat(service.getBoosters()).hasSize(2);
        Set<Version> versions = service.getVersions(new Mission("rest-http"), new Runtime("vert.x"));
        assertThat(versions).hasSize(2);
    }

    @Test
    public void testLabels() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("https://github.com/chirino/booster-catalog.git").catalogRef("filter_test")
                .build();
        service.index().get();
        assertThat(service.getBoosters("vert.x")).hasSize(2);
        assertThat(service.getBoosters("redhat")).hasSize(1);
        assertThat(service.getBoosters("community")).hasSize(1);
        assertThat(service.getBoosters("vert.x", "redhat")).hasSize(1);
        assertThat(service.getBoosters("community", "redhat")).hasSize(0);
    }

    @Test
    public void testGetBoosterRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");
        Collection<Booster> boosters = service.getBoosters(springBoot);
        assertThat(boosters.size()).isGreaterThan(1);
    }

    @Test
    public void testGetMissionByRuntime() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Runtime springBoot = new Runtime("spring-boot");
        Set<Mission> missions = service.getMissions(springBoot);
        assertThat(missions.size()).isGreaterThan(1);
    }

    @Test
    public void testGetMissionByDeploymentType() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        Set<Mission> missions = service.selector().deploymentType(DeploymentType.ZIP).getMissions();
        assertThat(missions.size()).isGreaterThan(1);
    }

    @Test
    public void testGetRuntimes() throws Exception {
        BoosterCatalogService service = buildDefaultCatalogService();
        service.index().get();
        assertThat(service.getRuntimes()).contains(new Runtime("spring-boot"), new Runtime("vert.x"),
                                                   new Runtime("wildfly-swarm"));
    }

    @Test
    public void testFilter() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef("openshift-online-free")
                .filter(b -> b.getRuntime().getId().equals("spring-boot")).build();
        service.index().get();
        assertThat(service.getRuntimes()).containsOnly(new Runtime("spring-boot"));
    }

    @Test
    public void testListener() throws Exception {
        List<Booster> boosters = new ArrayList<>();
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef("openshift-online-free")
                .listener(new BoosterCatalogListener() {
                    @Override
                    public void boosterAdded(Booster booster) {
                        boosters.add(booster);
                    }
                }).filter(b -> boosters.size() == 1).build();
        service.index().get();
        assertThat(boosters).containsAll(service.getBoosters());
    }

    private BoosterCatalogService buildDefaultCatalogService() {
        if (defaultService == null) {
            Builder builder = new BoosterCatalogService.Builder();
            String repo = System.getenv("LAUNCHPAD_BACKEND_CATALOG_GIT_REPOSITORY");
            if (repo != null) {
                builder.catalogRepository(repo);
            }
            String ref = System.getenv().getOrDefault("LAUNCHPAD_BACKEND_CATALOG_GIT_REF", "openshift-online-free");
            if (ref != null) {
                builder.catalogRef(ref);
            }
            defaultService = builder.build();
        }
        return defaultService;
    }
}
