package org.aion.mock.eth.populate.pipeline;

import lombok.Data;
import lombok.NonNull;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionReceipt;

import java.util.List;

@Data
public class BlockItem {
    @NonNull
    private String fork;

    @NonNull
    private Block block;

    @NonNull
    private List<TransactionReceipt> receipts;
}
