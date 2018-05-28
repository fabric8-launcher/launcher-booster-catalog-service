package io.fabric8.launcher.booster.catalog.rhoar;

import org.apache.commons.beanutils.PropertyUtils;

import javax.annotation.Nullable;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class BoosterPredicates {
    private static final Logger log = Logger.getLogger(BoosterPredicates.class.getName());

    private BoosterPredicates() {
    }

    public static Predicate<RhoarBooster> withRuntime(@Nullable Runtime runtime) {
        return (RhoarBooster b) -> runtime == null || runtime.equals(b.getRuntime());
    }

    public static Predicate<RhoarBooster> withMission(@Nullable Mission mission) {
        return (RhoarBooster b) -> mission == null || mission.equals(b.getMission());
    }

    public static Predicate<RhoarBooster> withVersion(@Nullable Version version) {
        return (RhoarBooster b) -> version == null || version.equals(b.getVersion());
    }

    public static Predicate<RhoarBooster> withRunsOn(@Nullable String clusterType) {
        return (RhoarBooster b) -> b.runsOn(clusterType);
    }

    public static Predicate<RhoarBooster> withScriptFilter(@Nullable String filter) {
        if (filter != null) {
            ScriptEngineManager manager = new ScriptEngineManager(BoosterPredicates.class.getClassLoader());
            ScriptEngine engine = manager.getEngineByExtension("js");
            try {
                CompiledScript script = ((Compilable) engine).compile(filter);
                return (RhoarBooster b) -> testScriptFilter(script, b);
            } catch (ScriptException e) {
                throw new IllegalArgumentException("Invalid script", e);
            }
        } else {
            return (RhoarBooster b) -> true;
        }
    }

    private static boolean testScriptFilter(CompiledScript script, RhoarBooster rhoarBooster) {
        ScriptContext context = new SimpleScriptContext();
        context.setAttribute("booster", rhoarBooster, ScriptContext.ENGINE_SCOPE);
        Object result = Boolean.FALSE;
        try {
            result = script.eval(context);
        } catch (ScriptException e) {
            log.log(Level.WARNING, "Error while evaluating script", e);
        }
        return (result instanceof Boolean) ? ((Boolean) result).booleanValue() :
                Boolean.valueOf(String.valueOf(result));
    }

    public static Predicate<RhoarBooster> withParameters(Map<String, List<String>> parameters) {
        Predicate<RhoarBooster> p = b -> true;
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String path = entry.getKey();
            List<String> values = entry.getValue();
            String expectedValue = !values.get(0).isEmpty() ? values.get(0) : "true";
            p = p.and(b -> expectedValue.equals(getValueByPath(b, path)));
        }
        return p;
    }

    @Nullable
    private static String getValueByPath(RhoarBooster b, String path) {
        Object target = b;
        String parts[] = path.split("\\.");
        for (String part : parts) {
            try {
                target = PropertyUtils.getProperty(target, part);
            } catch (Exception e) {
                return "false";
            }
        }
        return target != null ? target.toString() : "false";
    }
}
