package org.aion.mock.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig {

    public String contractAddress;

    public int port;

    public Map<String, Forks> forks;

    public Map<String, Transfers> transfers;

    public List<String> mode;

    @JsonIgnore
    public ParsedConfig parsed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Forks {
        public long startNumber;
        public long endNumber;
        public long triggerNumber;
        public long postTriggerNumber;
        public Map<String, Long> transfers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transfers {
        public String from;
        public String to;
        public long amount;
    }

    /**
     * Holder class for storing configs that we parse from
     * server configs
     */
    @Data
    public static class ParsedConfig {

    }
}
