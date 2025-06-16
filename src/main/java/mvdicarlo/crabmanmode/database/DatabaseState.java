package mvdicarlo.crabmanmode.database;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 * Manages the state of the database connection and loading process.
 * Provides thread-safe state transitions and notifications.
 */
@Slf4j
public class DatabaseState {

    public enum State {
        NOT_INITIALIZED("Not initialized"),
        INITIALIZING("Connecting to database..."),
        LOADING_DATA("Loading unlocked items..."),
        READY("Ready"),
        ERROR("Connection failed");

        private final String displayName;

        State(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isReady() {
            return this == READY;
        }

        public boolean canAcceptActions() {
            return this != NOT_INITIALIZED && this != ERROR;
        }
    }

    private final AtomicReference<State> currentState = new AtomicReference<>(State.NOT_INITIALIZED);
    private final List<Consumer<State>> stateListeners = new CopyOnWriteArrayList<>();
    private volatile Throwable lastError;

    /**
     * Get the current state
     */
    public State getCurrentState() {
        return currentState.get();
    }

    /**
     * Check if the database is ready for operations
     */
    public boolean isReady() {
        return currentState.get().isReady();
    }

    /**
     * Check if the database can accept new actions
     */
    public boolean canAcceptActions() {
        return currentState.get().canAcceptActions();
    }

    /**
     * Get the last error that occurred (if any)
     */
    public Throwable getLastError() {
        return lastError;
    }

    /**
     * Transition to a new state
     */
    public void setState(State newState) {
        setState(newState, null);
    }

    /**
     * Transition to a new state with an optional error
     */
    public void setState(State newState, Throwable error) {
        State oldState = currentState.getAndSet(newState);

        if (error != null) {
            this.lastError = error;
        } else if (newState != State.ERROR) {
            // Clear error on successful state transitions
            this.lastError = null;
        }

        if (oldState != newState) {
            log.debug("Database state changed: {} -> {}", oldState, newState);
            notifyListeners(newState);
        }
    }

    /**
     * Add a listener for state changes
     */
    public void addStateListener(Consumer<State> listener) {
        stateListeners.add(listener);
        // Immediately notify of current state
        listener.accept(currentState.get());
    }

    /**
     * Remove a state listener
     */
    public void removeStateListener(Consumer<State> listener) {
        stateListeners.remove(listener);
    }

    /**
     * Reset to initial state
     */
    public void reset() {
        setState(State.NOT_INITIALIZED);
        this.lastError = null;
    }

    private void notifyListeners(State newState) {
        for (Consumer<State> listener : stateListeners) {
            try {
                listener.accept(newState);
            } catch (Exception e) {
                log.error("Error notifying state listener", e);
            }
        }
    }
}
