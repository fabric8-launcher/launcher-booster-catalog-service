package io.fabric8.launcher.booster.catalog.rhoar;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import java.util.function.Predicate;

import static io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster.checkCategory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void script_should_evaluate_to_true() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withScriptFilter("booster != null");
        assertThat(predicate.test(mock(RhoarBooster.class))).isTrue();
    }

    @Test
    public void script_should_evaluate_to_false() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withScriptFilter("booster == null");
        assertThat(predicate.test(mock(RhoarBooster.class))).isFalse();
    }

    @Test
    public void script_should_evaluate_metadata_to_true() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withScriptFilter("booster.metadata.istio");
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void script_should_evaluate_metadata_to_false() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withScriptFilter("booster.metadata.istio");
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void params_default_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("")));
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_default_vs_false_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("")));
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void params_true_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("true")));
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_true_vs_false_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("true")));
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void params_not_exists() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("foo.bar", singletonList("")));
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getMetadata()).thenReturn(singletonMap("dummy", "dummy"));
        assertThat(predicate.test(booster)).isFalse();
    }
}
