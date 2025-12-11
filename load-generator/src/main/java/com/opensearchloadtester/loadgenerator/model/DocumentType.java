package com.opensearchloadtester.loadgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    ANO("ano-index"),
    DUO("duo-index");

    private final String index;
}

