package com.opensearchloadtester.ui.config;

import java.time.Duration;
import java.util.List;

public class CustomScenarioConfig {

    private String name;
    private String document_type;
    private Duration schedule_duration;
    private int queries_per_second;
    private Duration query_response_timeout;
    private boolean enable_warm_up;
    private List<String> query_types;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocument_type() {
        return document_type;
    }

    public void setDocument_type(String document_type) {
        this.document_type = document_type;
    }

    public Duration getSchedule_duration() {
        return schedule_duration;
    }

    public void setSchedule_duration(Duration schedule_duration) {
        this.schedule_duration = schedule_duration;
    }

    public int getQueries_per_second() {
        return queries_per_second;
    }

    public void setQueries_per_second(int queries_per_second) {
        this.queries_per_second = queries_per_second;
    }

    public Duration getQuery_response_timeout() {
        return query_response_timeout;
    }

    public void setQuery_response_timeout(Duration query_response_timeout) {
        this.query_response_timeout = query_response_timeout;
    }

    public boolean isEnable_warm_up() {
        return enable_warm_up;
    }

    public void setEnable_warm_up(boolean enable_warm_up) {
        this.enable_warm_up = enable_warm_up;
    }

    public List<String> getQuery_types() {
        return query_types;
    }

    public void setQuery_types(List<String> query_types) {
        this.query_types = query_types;
    }
}
