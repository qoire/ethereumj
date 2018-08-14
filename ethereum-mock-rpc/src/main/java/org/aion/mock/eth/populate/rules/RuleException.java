package org.aion.mock.eth.populate.rules;

import lombok.Data;

public class RuleException extends RuntimeException {

    public RuleException(String reason) {
        super(reason);
    }

    public RuleException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
