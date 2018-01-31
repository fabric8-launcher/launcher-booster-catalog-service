package io.fabric8.launcher.booster.catalog.rhoar;

import static io.fabric8.launcher.booster.catalog.rhoar.BoosterPredicates.checkNegatableCategory;

import static java.util.Arrays.asList;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class BoosterPredicatesTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testCheckNegatableCategorySimple() {
        softly.assertThat(checkNegatableCategory("", "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory("all", "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory("none", "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory("foobar", "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory("!foobar", "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory("baz", "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory("!baz", "foobar")).isTrue();
    }

    @Test
    public void testCheckNegatableCategoryList() {
        softly.assertThat(checkNegatableCategory(asList(), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("all"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("foobar"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("!foobar"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("none"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("foobar", "none"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("!foobar", "all"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("foobar", "!foobar"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("!foobar", "foobar"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("baz"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("!baz"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("baz", "all"), "foobar")).isTrue();
        softly.assertThat(checkNegatableCategory(asList("!baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkNegatableCategory(asList("!baz", "all"), "foobar")).isTrue();
    }
}
