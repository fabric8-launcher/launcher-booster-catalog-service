package io.fabric8.launcher.booster.catalog.rhoar;

import static io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster.checkCategory;

import static java.util.Arrays.asList;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class BoosterPredicatesTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testCheckNegatableCategoryList() {
        softly.assertThat(checkCategory(asList(), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("all"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("foobar"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("!foobar"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("none"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("foobar", "none"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("!foobar", "all"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("foobar", "!foobar"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("!foobar", "foobar"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("baz"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("!baz"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("baz", "all"), "foobar")).isTrue();
        softly.assertThat(checkCategory(asList("!baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkCategory(asList("!baz", "all"), "foobar")).isTrue();
    }
}
