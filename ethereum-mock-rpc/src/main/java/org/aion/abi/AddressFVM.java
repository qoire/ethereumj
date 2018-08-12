package org.aion.abi;

import org.ethereum.db.ByteArrayWrapper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.aion.util.ByteUtility.pad;

public class AddressFVM extends BaseTypeFVM {

    private final ByteArrayWrapper address;

    public AddressFVM(@Nonnull final ByteArrayWrapper address) {
        assert address.getData().length == 20;
        this.address = address;
    }

    @Override
    public byte[] serialize() {
        return pad(this.address.getData(), 32);
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
