package io.fabric8.launcher.booster.catalog

import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/*
 * Interface that's used by a {@link Booster} to fetch its code from a repository
 */
interface BoosterFetcher {
    /**
     * Starts the process of downloading the [Booster]'s code from a repository.
     * Returns a [CompletableFuture] that will at some point return a [Path] pointing
     * to the downloaded code. If the code was already downloaded an already satisfied
     * [CompletableFuture] will be returned and no download will happen.
     * @param booster THe [Booster] whose code should be fetched
     * @return a [CompletableFuture] returning a [Path] pointing to the downloaded code
     */
    fun fetchBoosterContent(booster: Booster): CompletableFuture<Path>
}
