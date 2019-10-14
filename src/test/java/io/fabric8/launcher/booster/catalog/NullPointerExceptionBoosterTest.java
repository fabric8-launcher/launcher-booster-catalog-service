package io.fabric8.launcher.booster.catalog;

import java.nio.file.Paths;
import java.util.Optional;

import io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates;
import io.fabric8.launcher.booster.catalog.rhoar.Mission;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster;
import io.fabric8.launcher.booster.catalog.rhoar.RhoarBoosterCatalogService;
import io.fabric8.launcher.booster.catalog.rhoar.Runtime;
import io.fabric8.launcher.booster.catalog.rhoar.Version;
import io.fabric8.launcher.booster.catalog.utils.JsonKt;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NullPointerExceptionBoosterTest {

    @Test
    public void should_not_throw_null_pointer_exception() throws Exception {
        RhoarBoosterCatalogService service = new RhoarBoosterCatalogService.Builder()
                .catalogProvider(() -> JsonKt.readCatalog(Paths.get("src/test/resources/custom-catalogs/test-catalog.json")))
                .metadataProvider(() -> JsonKt.readMetadata(Paths.get("src/test/resources/custom-catalogs/test-metadata.json")))
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
