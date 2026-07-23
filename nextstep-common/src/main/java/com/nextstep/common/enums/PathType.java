package com.nextstep.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 三条路径
 */
@Getter
@AllArgsConstructor
public enum PathType {

    POSTGRADUATE("PG", "考研"),
    CIVIL_SERVANT("CS", "考公"),
    EMPLOYMENT("EM", "就业");

    private final String code;
    private final String desc;
}
