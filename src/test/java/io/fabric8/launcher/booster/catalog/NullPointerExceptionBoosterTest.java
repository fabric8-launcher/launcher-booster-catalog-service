package io.fabric8.launcher.booster.catalog;

import java.util.Optional;

import io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates;
import io.fabric8.launcher.booster.catalog.rhoar.Mission;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService;
import io.fabric8.launcher.booster.catalog.rhoar.Runtime;
import io.fabric8.launcher.booster.catalog.rhoar.Version;
import org.arquillian.smart.testing.rules.git.server.GitServer;
import org.junit.ClassRule;
import org.junit.Test;

import static io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates.withMission;
import static io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates.withRuntime;
import static io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates.withVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NullPointerExceptionBoosterTest {

    @ClassRule
    public static GitServer gitServer = GitServer
            .fromBundle("empty-metadata-booster-catalog", "repos/custom-catalogs/empty-metadata-booster-catalog.bundle")
            .usingAnyFreePort()
            .create();

    @Test
    public void should_not_throw_null_pointer_exception() throws Exception {
        RhoarBoosterCatalogService service = new RhoarBoosterCatalogService.Builder()
                .catalogRepository("http://localhost:" + gitServer.getPort() + "/empty-metadata-booster-catalog/")
                .catalogRef("master")
                .build();
        // Wait for index to complete
        service.index().get();
        //  spring-boot/current-redhat/health-check
        Optional<RhoarBooster> booster = service.getBooster(
                BoosterPredicates.withMission(new Mission("health-check"))
                        .and(BoosterPredicates.withRuntime(new Runtime("spring-boot")))
                        .and(BoosterPredicates.withVersion(new Version("current-redhat")))
        );
        assertThat(booster).hasValueSatisfying(b -> {
            assertThatCode(() -> b.getMetadata()).doesNotThrowAnyException();
        });
    }
}
