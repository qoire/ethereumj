package org.aion.mock.config;

import lombok.Data;

public enum ExitCode {
    CONFIG_ERR(10);

    private final int code;

    private ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }
}
