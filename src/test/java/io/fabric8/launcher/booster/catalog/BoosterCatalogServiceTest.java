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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.BoosterCatalogService.Builder;
import io.fabric8.launcher.booster.catalog.spi.NativeGitBoosterCatalogPathProvider;
import org.arquillian.smart.testing.rules.git.server.GitServer;
import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BoosterCatalogServiceTest {

    @ClassRule
    public static GitServer gitServer = GitServer.bundlesFromDirectory("repos/boosters")
            .fromBundle("gastaldi-booster-catalog", "repos/custom-catalogs/gastaldi-booster-catalog.bundle")
            .usingPort(8765)
            .create();

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Nullable
    private static BoosterCatalogService defaultService;

    @BeforeClass
    public static void setUpSystemProperties() {
        System.setProperty(LauncherConfiguration.PropertyName.LAUNCHER_BOOSTER_CATALOG_REPOSITORY, "http://localhost:8765/booster-catalog/");
        System.setProperty(LauncherConfiguration.PropertyName.LAUNCHER_BOOSTER_CATALOG_REF, "master");
    }

    @Test
    public void testIndex() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef()).build();
        softly.assertThat(service.getBoosters()).isEmpty();

        service.index().get();

        softly.assertThat(service.getBoosters()).isNotEmpty();
    }

    @Test
    public void testReindex() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef()).build();
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
    public void testCommonFiles() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("common_test")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));

        softly.assertThat(booster).hasValueSatisfying(b -> {
            softly.assertThat(b.<Boolean>getMetadata("root")).isEqualTo(true);
            softly.assertThat(b.<String>getMetadata("mymission")).isEqualTo("rest-http");
            softly.assertThat(b.<String>getMetadata("myruntime")).isEqualTo("vertx");
            softly.assertThat(b.<String>getMetadata("myversion")).isEqualTo("community");
        });
    }

    @Test
    public void testMetadata() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("common_test")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));

        softly.assertThat(booster).hasValueSatisfying(b -> {
            softly.assertThat(b.<Integer>getMetadata("sub/foo")).isEqualTo(3);
            softly.assertThat(b.<Integer>getMetadata("sub/bar")).isEqualTo(5);
            softly.assertThat(b.<Integer>getMetadata("sub/baz")).isEqualTo(4);
            softly.assertThat(b.<Integer>getMetadata("sub/bam")).isEqualTo(7);
            softly.assertThat(b.<Integer>getMetadata("sub/fox")).isEqualTo(8);
        });
    }

    @Test
    public void testIgnore() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("ignore_test")
                .build();
        service.index().get();

        Collection<Booster> boosters = service.getBoosters(missions("rest-http").and(runtimes("vert.x")));

        softly.assertThat(boosters).hasSize(1);
    }

    @Test
    public void testManualEnvironment() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("environments_test")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));

        softly.assertThat(booster.isPresent()).isTrue();

        softly.assertThat(booster).hasValueSatisfying(b -> {
            softly.assertThat(b.getName()).isEqualTo("default-name");
            softly.assertThat(b.getGitRef()).isEqualTo("master");

            Booster envBooster = b.forEnvironment("production");
            softly.assertThat(envBooster.getName()).isEqualTo("prod-name");
            softly.assertThat(envBooster.getGitRef()).isEqualTo("v13");
            softly.assertThat(envBooster.<Integer>getMetadata("foo")).isEqualTo(3);
        });
    }

    @Test
    public void testCatalogEnvironment() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("environments_test")
                .environment("production")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));
        softly.assertThat(booster).hasValueSatisfying(b -> {
            softly.assertThat(b.getName()).isEqualTo("prod-name");
            softly.assertThat(b.getGitRef()).isEqualTo("v13");
            softly.assertThat(b.<Integer>getMetadata("foo")).isEqualTo(3);
        });
    }

    @Test
    public void testCatalogCompoundEnvironment() throws Exception {
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .catalogRepository("http://localhost:8765/gastaldi-booster-catalog").catalogRef("environments_test")
                .environment("production,extra")
                .build();
        service.index().get();

        Optional<Booster> booster = service.getBooster(missions("rest-http").and(runtimes("vert.x")).and(versions("community")));
        softly.assertThat(booster).hasValueSatisfying(b -> {
            softly.assertThat(b.getName()).isEqualTo("prod-name");
            softly.assertThat(b.getGitRef()).isEqualTo("v13");
            softly.assertThat(b.<Integer>getMetadata("foo")).isEqualTo(4);
            softly.assertThat(b.<Boolean>getMetadata("bar")).isTrue();
        });
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
        BoosterCatalogService service = new BoosterCatalogService.Builder().catalogRef(LauncherConfiguration.boosterCatalogRepositoryRef())
                .filter(b -> getPath(b, 0).equals("spring-boot")).build();

        service.index().get();

        softly.assertThat(service.getBoosters().size()).isGreaterThan(0);
        softly.assertThat(service.getBoosters().stream().map(b -> getPath(b, 0))).containsOnly("spring-boot");
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
        BoosterCatalogService service = new BoosterCatalogService.Builder()
                .transformer(new TestRepoUrlFixer("http://localhost:8765"))
                .pathProvider(unreliableProvider)
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

    public static Predicate<Booster> missions(@Nullable String mission) {
        return (Booster b) -> mission == null || mission.equals(getPath(b, 2));
    }

    public static Predicate<Booster> runtimes(@Nullable String runtime) {
        return (Booster b) -> runtime == null || runtime.equals(getPath(b, 0));
    }

    public static Predicate<Booster> versions(@Nullable String version) {
        return (Booster b) -> version == null || version.equals(getPath(b, 1));
    }

    private static String getPath(Booster b, int index) {
        List<String> path = b.getDescriptor().getPath();
        assert (index >= 0 && index < path.size());
        return path.get(index);
    }

    private BoosterCatalogService buildDefaultCatalogService() {
        if (defaultService == null) {
            defaultService = new Builder()
                    .transformer(new TestRepoUrlFixer("http://localhost:8765"))
                    .build();
        }
        return defaultService;
    }

    private class TestRepoUrlFixer implements BoosterDataTransformer {
        private final String fixedUrl;

        public TestRepoUrlFixer(String fixedUrl) {
            this.fixedUrl = fixedUrl;
        }

        @Override
        public Map<String, Object> transform(Map<String, ?> data) {
            String gitRepo = Booster.getDataValue(data, "source/git/url", null);
            if (gitRepo != null) {
                gitRepo = gitRepo.replace("https://github.com", fixedUrl);
                Booster.setDataValue((Map<String, Object>)data, "source/git/url", gitRepo);
            }
            return (Map<String, Object>)data;
        }
    }

    private static class UnreliablePathProvider extends NativeGitBoosterCatalogPathProvider {
        public boolean fail = true;

        public UnreliablePathProvider() {
            super(LauncherConfiguration.boosterCatalogRepositoryURI(), LauncherConfiguration.boosterCatalogRepositoryRef(), null);
        }

        @Override
        public Path createBoosterContentPath(Booster booster) throws IOException {
            if (fail) {
                throw new IOException("Fail flag is true");
            }
            return super.createBoosterContentPath(booster);
        }
    }
}
