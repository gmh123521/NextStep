package com.nextstep.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnglishLevel {
    NONE("NONE", "未通过"),
    CET4("CET4", "英语四级"),
    CET6("CET6", "英语六级"),
    IELTS("IELTS", "雅思"),
    TOEFL("TOEFL", "托福");

    private final String code;
    private final String desc;
}
