package io.fabric8.launcher.booster.catalog.rhoar.predicates

import io.fabric8.launcher.booster.catalog.Booster
import java.util.function.Predicate
import java.util.logging.Level
import java.util.logging.Logger
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleScriptContext


/**
 * This predicate takes a JavaScript expression and evaluates to true or false, using "booster" as the argument
 *
 * @author [George Gastaldi](mailto:ggastald@redhat.com)
 */
class BoosterScriptingPredicate(evalScript: String) : Predicate<Booster> {

    private val script: CompiledScript

    init {
        val manager = ScriptEngineManager()
        val engine = manager.getEngineByName("js")
        try {
            this.script = (engine as Compilable).compile(evalScript)
        } catch (e: ScriptException) {
            throw IllegalArgumentException("script is invalid", e)
        }

    }

    override fun test(rhoarBooster: Booster): Boolean {
        val context = SimpleScriptContext()
        context.setAttribute("booster", rhoarBooster, ScriptContext.ENGINE_SCOPE)
        var result: Any = java.lang.Boolean.FALSE
        try {
            result = script.eval(context)
        } catch (e: ScriptException) {
            log.log(Level.WARNING, "Error while evaluating script", e)
        }

        return if (result is Boolean)
            result
        else
            java.lang.Boolean.valueOf(result.toString())
    }

    companion object {
        private val log = Logger.getLogger(BoosterScriptingPredicate::class.java.name)
    }
}
