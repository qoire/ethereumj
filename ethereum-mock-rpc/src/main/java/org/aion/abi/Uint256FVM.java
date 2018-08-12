package org.aion.abi;

import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.aion.util.ByteUtility.pad;

public class Uint256FVM extends BaseTypeFVM {

    private final byte[] payload;

    public Uint256FVM(@Nonnull final ByteArrayWrapper word) {
        this.payload = pad(word.getData(), 32);
    }

    @Override
    public byte[] serialize() {
        return this.payload;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public Optional<List<BaseTypeFVM>> getEntries() {
        return Optional.empty();
    }
}
