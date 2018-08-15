package org.aion.mock.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class ConfigLoader {

    /**
     * Keep configuration loading simple, we will always be looking to
     * load a file from the directory that this jar file was executed on.
     *
     * The file path should always be {@code config.yaml}
     *
     * @return {@code config} loaded from file
     */
    public static ServerConfig load() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ServerConfig config = null;
        try {
            config = mapper.readValue(new File("config.yaml"), ServerConfig.class);
        } catch (Exception e) {
            log.error("caught error while loading config", e);
            return null;
        }
        // config validation
        if (!validateConfigInvariants(config))
            return null;
        return config;
    }

    /**
     * Validates the configuration, treat any sort of error here as a
     * runtime exception, as we will not proceed with a broken/vague
     * state.
     *
     * @param config
     */
    public static boolean validateConfigInvariants(@Nonnull final ServerConfig config) {
        if (!isValidAddress(config.getContractAddress()))
            return false;

        if (config.getPort() < 0 || config.getPort() > 65535)
            return false;

        if (!isValidMode(config.getMode()))
            return false;

        // transfer validation
        {
            var count = config.getTransfers()
                    .values()
                    .stream()
                    .filter(ConfigLoader::isNotValidTransfer)
                    .count();

            if (count > 0)
                return false;
        }

        // TODO: fork checks here
        return true;
    }

    private static final Pattern addressPattern = Pattern.compile("^0x(0-9a-fA-F)[40]$");
    private static boolean isValidAddress(@Nonnull final String input) {
        return addressPattern.matcher(input).matches();
    }

    private static final Pattern bytes32Pattern = Pattern.compile("^0x(0-9a-fA-F)[64]");
    private static boolean isBytes32(@Nonnull final String input) {
        return bytes32Pattern.matcher(input).matches();
    }

    private static boolean isValidMode(@Nonnull final List<String> modes) {
        L:
        for (final var m : modes) {
            switch (m.toLowerCase()) {
                case "syncing":
                case "throughput":
                case "ticking":
                    continue L;
            }
            return false;
        }
        return true;
    }

    private static boolean isNotValidTransfer(@Nonnull final ServerConfig.Transfers transfer) {
        return !isValidTransfer(transfer);
    }

    private static boolean isValidTransfer(@Nonnull final ServerConfig.Transfers transfer) {
        return isValidAddress(transfer.getFrom()) &&
                isBytes32(transfer.getTo()) &&
                transfer.getAmount() > 0;
    }
}
