package org.aion.mock.eth.populate;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.aion.mock.eth.core.BlockConstructor;
import org.aion.mock.eth.populate.rules.AbstractRule;
import org.aion.mock.eth.state.ChainState;
import org.ethereum.core.TransactionInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
public class PopulationEngine extends PopulationStrategy {

    @Singular("transferEventList")
    private List<ExecutionUtilities.TransferEvent> transferEventList;

    @Singular
    private List<AbstractRule> specialRules;


    @Builder
    private PopulationEngine(@Nonnull final ChainState state,
                             @Nullable final List<ExecutionUtilities.TransferEvent> transferEventList,
                             @Nullable final List<AbstractRule> specialRules) {
        super(state);
        this.transferEventList = transferEventList == null ? Collections.emptyList() : transferEventList;
        this.specialRules = specialRules == null ? Collections.emptyList() : specialRules;
    }

    @Override
    public void populateInitialInternal() {
        for (var rules : this.specialRules) {
            log.info("rule {} initial application", rules.toString());
            rules.apply(this.state);
        }
    }

    @Override
    public void populateStep(Properties props) {
        // by default, the strategy does not have any default behaviour
        for (AbstractRule rule : specialRules) {
            rule.applyStep(this.state);
        }
    }
}
