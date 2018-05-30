package io.fabric8.launcher.booster.catalog.rhoar.predicates;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import io.fabric8.launcher.booster.catalog.rhoar.RhoarBooster;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class MetadataParameterPredicate implements Predicate<RhoarBooster> {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final Map<String, List<String>> parameters;

    public MetadataParameterPredicate(Map<String, List<String>> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean test(RhoarBooster rhoarBooster) {
        boolean result = true;
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String path = entry.getKey();
            List<String> values = entry.getValue();
            String expectedValue = !values.get(0).isEmpty() ? values.get(0) : "true";
            if (!expectedValue.equalsIgnoreCase(getValueByPath(rhoarBooster, path))) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Nullable
    private static String getValueByPath(RhoarBooster b, String path) {
        Object target = b;
        for (String part : DOT_PATTERN.split(path)) {
            try {
                target = PropertyUtils.getProperty(target, part);
            } catch (Exception ignored) {
                return "false";
            }
        }
        return target != null ? target.toString() : "false";
    }

}
