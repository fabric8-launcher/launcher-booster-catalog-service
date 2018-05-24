package io.fabric8.launcher.booster.catalog.rhoar;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

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
}
