package io.fabric8.launcher.booster.catalog.utils

import java.io.IOException
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors

private val logger = Logger.getLogger("io.fabric8.launcher.booster.catalog.utils.git")

@Throws(IOException::class)
fun cloneRepository(repo: String, ref: String, targetPath: Path): Path {
    val exitCode: Int
    try {
        val builder = ProcessBuilder()
                .command("git", "clone", repo,
                        "--branch", ref,
                        "--recursive",
                        "--depth=1",
                        "--quiet",
                        "-c", "advice.detachedHead=false",
                        targetPath.toString())
                .inheritIO()
        logger.info { "Executing: " + builder.command().stream().collect(Collectors.joining(" ")) }
        exitCode = builder.start().waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("Process returned exit code: $exitCode")
        }
    } catch (e: InterruptedException) {
        // Restore interrupted state
        Thread.currentThread().interrupt()
        logger.log(Level.WARNING, "Interrupted cloning process")
        throw IOException("Interrupted", e)
    }

    return targetPath
}
