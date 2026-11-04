package com.fu.example.starter.model.enums;

import lombok.Getter;

@Getter
public enum LuaStatusEnum {

    SUCCESS(1L),
    FAIL(-1L),
    ;

    private final long value;

    LuaStatusEnum(long value) {
        this.value = value;
    }
}
