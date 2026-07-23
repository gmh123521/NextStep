package com.nextstep.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SchoolLevel {
    C9("C9", "C9 联盟"),
    PROJECT_985("985", "985 工程"),
    PROJECT_211("211", "211 工程"),
    DOUBLE_FIRST("DOUBLE_FIRST", "双一流"),
    REGULAR("REGULAR", "普通本科"),
    COLLEGE("COLLEGE", "专科");

    private final String code;
    private final String desc;
}
