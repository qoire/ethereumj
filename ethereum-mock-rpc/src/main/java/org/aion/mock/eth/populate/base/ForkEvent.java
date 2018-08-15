package org.aion.mock.eth.populate.base;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import org.aion.mock.eth.populate.ExecutionUtilities;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

@Builder
public class ForkEvent {

    @Getter
    private String forkName;

    @Getter
    private long forkStartBlockNumber = 0;

    @Getter
    private long forkEndBlockNumber = 0;

    @Getter
    private long forkTriggerNumber = 0;

    @Getter
    private long forkPostTriggerNumber = 0;

    @Getter
    @Builder.Default
    private BigInteger initialDifficulty = BigInteger.ZERO;

    @Getter
    @Singular
    private List<ExecutionUtilities.TransferEvent> forkTransferEvents;

    public synchronized void sortTransfers() {
        // TODO: we can keep the comparator static
        this.forkTransferEvents.sort(new TransferEventBlockComparator());
    }

    private static class TransferEventBlockComparator implements Comparator<ExecutionUtilities.TransferEvent> {
        @Override
        public int compare(ExecutionUtilities.TransferEvent o1, ExecutionUtilities.TransferEvent o2) {
            return Long.compare(o1.getBlockNumber(), o2.getBlockNumber());
        }

        @Override
        public boolean equals(Object obj) {
            // TODO: this violates hashCode() contract
            return false;
        }
    }
}
