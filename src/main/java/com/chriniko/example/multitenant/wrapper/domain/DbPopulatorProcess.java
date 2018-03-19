package com.chriniko.example.multitenant.wrapper.domain;

public class DbPopulatorProcess {

    private final String jdbcUrl;
    private final Process process;

    public DbPopulatorProcess(String jdbcUrl, Process process) {
        this.jdbcUrl = jdbcUrl;
        this.process = process;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public Process getProcess() {
        return process;
    }
}
