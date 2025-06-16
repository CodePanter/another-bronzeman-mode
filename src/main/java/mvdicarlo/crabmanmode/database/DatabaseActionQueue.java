package mvdicarlo.crabmanmode.database;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Queue processor that executes database actions sequentially with proper
 * ordering.
 * Actions are queued and processed in priority order (higher priority first).
 * Each action returns a CompletableFuture that completes when the action is
 * processed.
 */
@Slf4j
public class DatabaseActionQueue {
    private final ConcurrentLinkedQueue<DatabaseAction<?>> actionQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private volatile ActionExecutionContext executionContext;

    public DatabaseActionQueue() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DatabaseActionQueue");
            t.setDaemon(true);
            return t;
        });

        // Start the processing loop
        startProcessing();
    }

    /**
     * Submit an action to be executed asynchronously
     */
    public <T> CompletableFuture<T> submit(DatabaseAction<T> action) {
        log.debug("Queuing action: {}", action.getDescription());
        actionQueue.offer(action);

        // Trigger processing if not already running
        triggerProcessing();

        return action.getPromise();
    }

    /**
     * Update the execution context (called when database state changes)
     */
    public void updateExecutionContext(ActionExecutionContext context) {
        log.debug("Updating execution context - ready: {}", context.isReady());
        this.executionContext = context;

        // Trigger processing in case there are queued actions waiting for the context
        triggerProcessing();
    }

    /**
     * Get the current queue size
     */
    public int getQueueSize() {
        return actionQueue.size();
    }

    /**
     * Clear all pending actions without shutting down the queue.
     * This allows the queue to be reused after clearing.
     */
    public void clear() {
        log.info("Clearing database action queue");

        // Cancel all pending actions
        DatabaseAction<?> action;
        while ((action = actionQueue.poll()) != null) {
            action.completeExceptionally(new IllegalStateException("Queue cleared"));
        }

        // Clear execution context
        this.executionContext = null;
    }

    private void startProcessing() {
        executor.scheduleWithFixedDelay(this::processActions, 100, 50, TimeUnit.MILLISECONDS);
    }

    private void triggerProcessing() {
        // The scheduled task will pick up new actions automatically
        // This method exists for future optimization if needed
    }

    private void processActions() {
        if (isProcessing.get()) {
            return;
        }

        if (actionQueue.isEmpty() || executionContext == null) {
            return;
        }

        if (!isProcessing.compareAndSet(false, true)) {
            return; // Another thread is already processing
        }

        try {
            // Process actions in batches, respecting priority
            List<DatabaseAction<?>> batch = collectBatch();

            for (DatabaseAction<?> action : batch) {
                try {
                    processAction(action);
                } catch (Exception e) {
                    log.error("Unexpected error processing action {}: {}", action.getActionType(), e.getMessage(), e);
                    action.completeExceptionally(e);
                }
            }

        } finally {
            isProcessing.set(false);
        }
    }

    private List<DatabaseAction<?>> collectBatch() {
        List<DatabaseAction<?>> batch = new ArrayList<>();

        // Collect all ready actions
        DatabaseAction<?> action;
        while ((action = actionQueue.poll()) != null) {
            if (action.canExecute(executionContext)) {
                batch.add(action);
            } else {
                // Put it back and break (will be retried later)
                actionQueue.offer(action);
                break;
            }

            // Limit batch size to prevent blocking too long
            if (batch.size() >= 10) {
                break;
            }
        }
        // Sort by priority (higher first)
        batch.sort(Comparator.<DatabaseAction<?>>comparingInt(DatabaseAction::getPriority).reversed());

        return batch;
    }

    private void processAction(DatabaseAction<?> action) {
        try {
            log.debug("Processing action: {}", action.getDescription());
            action.execute(executionContext);

        } catch (Exception e) {
            log.error("Action {} failed: {}", action.getActionType(), e.getMessage());
            action.completeExceptionally(e);
        }
    }
}
