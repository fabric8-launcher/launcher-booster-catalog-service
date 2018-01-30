package io.openshift.booster.catalog.rhoar;

import static io.openshift.booster.catalog.rhoar.BoosterPredicates.checkRunsOn;

import static java.util.Arrays.asList;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class BoosterPredicatesTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testIsSupportedSimple() {
        softly.assertThat(checkRunsOn("", "foobar")).isTrue();
        softly.assertThat(checkRunsOn("all", "foobar")).isTrue();
        softly.assertThat(checkRunsOn("none", "foobar")).isFalse();
        softly.assertThat(checkRunsOn("foobar", "foobar")).isTrue();
        softly.assertThat(checkRunsOn("!foobar", "foobar")).isFalse();
        softly.assertThat(checkRunsOn("baz", "foobar")).isFalse();
        softly.assertThat(checkRunsOn("!baz", "foobar")).isTrue();
    }

    @Test
    public void testIsSupportedList() {
        softly.assertThat(checkRunsOn(asList(), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("all"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("foobar"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("!foobar"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("none"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("foobar", "none"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("!foobar", "all"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("foobar", "!foobar"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("!foobar", "foobar"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("baz"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("!baz"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("baz", "all"), "foobar")).isTrue();
        softly.assertThat(checkRunsOn(asList("!baz", "none"), "foobar")).isFalse();
        softly.assertThat(checkRunsOn(asList("!baz", "all"), "foobar")).isTrue();
    }
}
