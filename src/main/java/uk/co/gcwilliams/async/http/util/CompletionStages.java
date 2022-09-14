package uk.co.gcwilliams.async.http.util;

import uk.co.gcwilliams.async.http.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The completable futures
 *
 * @author : Gareth Williams
 **/
public class CompletionStages {

    private CompletionStages() { // static
    }

    /**
     * Converts a task to a completion stage
     *
     * @param task the task
     * @return the completion stage
     */
    public static <T> CompletionStage<T> toCompletionStage(Task<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        task.fork(future::complete, future::completeExceptionally);
        return future;
    }
}
