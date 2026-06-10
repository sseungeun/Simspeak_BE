package com.example._rdproject.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CorrectionType {
    GRAMMAR("문법"),
    PRONUNCIATION("발음"),
    EXPRESSION("표현");

    private final String description;
}
