package mvdicarlo.crabmanmode.database;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * Base implementation for database actions providing common functionality.
 * 
 * @param <T> The return type of the action
 */
@Slf4j
public abstract class BaseDatabaseAction<T> implements DatabaseAction<T> {

    protected final CompletableFuture<T> promise = new CompletableFuture<>();

    @Override
    public CompletableFuture<T> getPromise() {
        return promise;
    }

    @Override
    public void completeExceptionally(Throwable throwable) {
        log.warn("Action {} failed: {}", getActionType(), throwable.getMessage());
        promise.completeExceptionally(throwable);
    }

    /**
     * Complete the action's promise with a successful result
     */
    protected void complete(T result) {
        log.debug("Action {} completed successfully", getActionType());
        promise.complete(result);
    }

    /**
     * Template method that handles the execution and promise completion.
     * Subclasses should override executeInternal() instead of execute().
     */
    @Override
    public final T execute(ActionExecutionContext context) throws Exception {
        try {
            log.debug("Executing action: {}", getDescription());
            T result = executeInternal(context);
            complete(result);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute action {}: {}", getActionType(), e.getMessage(), e);
            throw e; // Let the queue handler deal with promise completion
        }
    }

    /**
     * Subclasses implement this method to perform the actual work
     */
    protected abstract T executeInternal(ActionExecutionContext context) throws Exception;
}
