package io.fabric8.launcher.booster.catalog.rhoar;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster.checkCategory;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BoosterPredicatesTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testCheckNegatableCategoryList() {
        softly.assertThat(checkCategory(emptyList(), "foobar")).isTrue();
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
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void script_should_evaluate_metadata_to_false() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withScriptFilter("booster.metadata.istio");
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void params_default_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_nested_default_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.osio.enabled", singletonList("")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("osio", singletonMap("enabled", "true")));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_nested_true_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.osio.enabled", singletonList("true")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("osio", singletonMap("enabled", "true")));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_default_vs_false_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void params_true_vs_true_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("true")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "true"));
        assertThat(predicate.test(booster)).isTrue();
    }

    @Test
    public void params_true_vs_false_boolean() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.istio", singletonList("true")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("istio", "false"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @Test
    public void should_filter_all_boosters_having_value_equal_to_one_of_multi_value_parameter() {
        // given
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("metadata.level", asList("novice", "advanced")));
        RhoarBooster noviceBooster = boosterWithMetadata(singletonMap("level", "novice"));
        RhoarBooster advancedBooster = boosterWithMetadata(singletonMap("level", "advanced"));
        RhoarBooster expertBooster = boosterWithMetadata(singletonMap("level", "expert"));

        final List<RhoarBooster> rhoarBoosters = asList(noviceBooster, advancedBooster, expertBooster);

        // when
        final List<RhoarBooster> filteredBoosters = rhoarBoosters.stream().filter(predicate).collect(toList());

        // then
        assertThat(filteredBoosters).containsOnly(noviceBooster, advancedBooster);
    }

    @Test
    public void should_filter_matched_versions() {
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getVersion()).thenReturn(new Version("community-current"));
        assertThat(BoosterPredicates.withVersionMatches(Pattern.compile("community-.*"))).accepts(booster);
        when(booster.getVersion()).thenReturn(new Version("redhat-current"));
        assertThat(BoosterPredicates.withVersionMatches(Pattern.compile("community-.*"))).rejects(booster);

    }

    @Test
    public void should_filter_matched_runtimes() {
        RhoarBooster booster = mock(RhoarBooster.class);
        when(booster.getRuntime()).thenReturn(new Runtime("spring-boot"));
        assertThat(BoosterPredicates.withRuntimeMatches(Pattern.compile("spring-boot.*"))).accepts(booster);
        when(booster.getRuntime()).thenReturn(new Runtime("thorntail"));
        assertThat(BoosterPredicates.withRuntimeMatches(Pattern.compile("spring-boot.*"))).rejects(booster);
    }

    @Test
    public void params_not_exists() {
        Predicate<RhoarBooster> predicate = BoosterPredicates.withParameters(singletonMap("foo.bar", singletonList("")));
        RhoarBooster booster = boosterWithMetadata(singletonMap("dummy", "dummy"));
        assertThat(predicate.test(booster)).isFalse();
    }

    @NotNull
    private RhoarBooster boosterWithMetadata(Map<String, Object> metadata) {
        RhoarBooster noviceBooster = mock(RhoarBooster.class);
        when(noviceBooster.getMetadata()).thenReturn(metadata);
        return noviceBooster;
    }
}
