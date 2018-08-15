package org.aion.mock.eth.populate.base;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.aion.mock.eth.populate.ExecutionUtilities;

import java.math.BigInteger;
import java.util.List;

@Data
@Builder
public class ForkEvent {
    private String forkName;

    private long forkStartBlockNumber = 0;

    private long forkEndBlockNumber = 0;

    private long forkTriggerNumber = 0;

    private long forkPostTriggerNumber = 0;

    @Builder.Default
    private BigInteger initialDifficulty = BigInteger.ZERO;

    @Singular
    private List<ExecutionUtilities.TransferEvent> forkTransferEvents;
}
