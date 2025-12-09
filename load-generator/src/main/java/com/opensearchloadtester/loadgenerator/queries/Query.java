package com.opensearchloadtester.loadgenerator.queries;

public interface Query {

    String generateQuery();
    String getQueryTemplatePath();
}
