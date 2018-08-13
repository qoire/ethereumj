package org.aion.mock.eth.populate;

import lombok.Builder;
import org.aion.mock.eth.state.ChainState;

import java.util.Properties;

/**
 * Strategy for populating the mocks, this defines an abstract
 * interface for populating blocks, the expectation is that the
 * facade will call this on every relevant (defined by implementer)
 * API call.
 *
 * In turn, the population strategy is responsible for deriving the
 * future state the chain.
 */
public abstract class PopulationStrategy {

    protected final ChainState state;

    private boolean runOnce;

    public PopulationStrategy(ChainState state) {
        this.state = state;
        runOnce = false;
    }

    public synchronized void populateInitial() {
        // check this as an invariant, should not happen
        assert !runOnce;
        if (!runOnce)
            populateInitialInternal();
    }

    /**
     * Called by the facade on initial population, this should be run only once.
     * At which point, populateStep() is called for subsuquent methods.
     */
    public abstract void populateInitialInternal();

    /**
     * Populate per step, where a step can refer to any artibrary API call.
     * {@code props} is used depending on which PopulationStrategy is selected.
     */
    public abstract void populateStep(Properties props);
}
