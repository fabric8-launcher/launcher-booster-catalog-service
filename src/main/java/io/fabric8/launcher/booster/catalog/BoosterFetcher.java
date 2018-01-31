package io.fabric8.launcher.booster.catalog;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/*
 * Interface that's used by a {@link Booster} to fetch its code from a repository
 */
public interface BoosterFetcher {
    /**
     * Starts the process of downloading the {@link Booster}'s code from a repository.
     * Returns a {@link CompletableFuture} that will at some point return a {@link Path} pointing
     * to the downloaded code. If the code was already downloaded an already satisfied
     * {@link CompletableFuture} will be returned and no download will happen.
     * @param booster THe {@link Booster} whose code should be fetched
     * @return a {@link CompletableFuture} returning a {@link Path} pointing to the downloaded code
     */
    public CompletableFuture<Path> fetchBoosterContent(Booster booster);
}
