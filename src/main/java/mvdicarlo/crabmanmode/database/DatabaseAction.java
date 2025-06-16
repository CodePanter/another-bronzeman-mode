package mvdicarlo.crabmanmode.database;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for database actions that can be queued and executed
 * asynchronously.
 * Each action encapsulates a specific database operation and returns a promise
 * that completes when the action is processed.
 * 
 * @param <T> The return type of the action
 */
public interface DatabaseAction<T> {

    /**
     * Get a human-readable type identifier for this action
     */
    String getActionType();

    /**
     * Get a description of what this action does (for logging/debugging)
     */
    String getDescription();

    /**
     * Execute the action with the provided context
     * 
     * @param context The execution context containing API, cache, etc.
     * @return The result of the action
     * @throws Exception if the action fails
     */
    T execute(ActionExecutionContext context) throws Exception;

    /**
     * Get the promise that will be completed when this action is processed
     */
    CompletableFuture<T> getPromise();

    /**
     * Complete the action's promise exceptionally
     */
    void completeExceptionally(Throwable throwable);

    /**
     * Check if this action can be executed in the current state
     */
    default boolean canExecute(ActionExecutionContext context) {
        return context.isReady();
    }

    /**
     * Get the priority of this action (higher = more important)
     * Default priority is 0 (normal)
     */
    default int getPriority() {
        return 0;
    }
}
