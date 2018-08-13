package org.aion.mock.eth.populate.rules;

import org.aion.mock.eth.state.ChainState;

/**
 * Define rule as an action on a blockchain, rules are applied
 * to the state per execution
 */
public abstract class AbstractRule {

    // some rules may need to implement a start method (not all)
    public abstract void start();

    public abstract void apply(ChainState state);

    public abstract void applyStep(ChainState state);
}
