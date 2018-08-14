package org.aion.mock.eth.populate;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.Singular;
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

public class PopulationEngine extends PopulationStrategy {

    @Singular("transferEventList")
    private List<ExecutionUtilities.TransferEvent> transferEventList;

    @Singular("specialRules")
    private List<AbstractRule> specialRules;

    @Default private Integer startNumber = 0;
    @Default private Integer endNumber = 0;

    @Builder
    private PopulationEngine(@Nonnull final ChainState state,
                             @NonNull final Integer startNumber,
                             @NonNull final Integer endNumber,
                             @Nullable final List<ExecutionUtilities.TransferEvent> transferEventList,
                             @Nullable final List<AbstractRule> specialRules) {
        super(state);
        assert startNumber >= 0;
        assert endNumber >= startNumber;

        this.startNumber = startNumber;
        this.endNumber = endNumber;

        this.transferEventList = transferEventList == null ? Collections.emptyList() : transferEventList;
        this.specialRules = specialRules == null ? Collections.emptyList() : specialRules;
    }

    @Override
    public void populateInitialInternal() {
        for (var rules : this.specialRules) {
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
