package com.nextstep.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EnglishLevel {
    NONE("NONE", "未通过"),
    CET4("CET4", "英语四级"),
    CET6("CET6", "英语六级"),
    TEM4("TEM4", "英语专业四级"),
    TEM8("TEM8", "英语专业八级"),
    IELTS("IELTS", "雅思"),
    TOEFL("TOEFL", "托福"),
    JLPT_N1("JLPT_N1", "日语 N1"),
    JLPT_N2("JLPT_N2", "日语 N2"),
    JLPT_N3("JLPT_N3", "日语 N3"),
    JLPT_N4("JLPT_N4", "日语 N4"),
    JLPT_N5("JLPT_N5", "日语 N5"),
    TOPIK1("TOPIK1", "韩语 TOPIK I"),
    TOPIK2("TOPIK2", "韩语 TOPIK II"),
    OTHER("OTHER", "其他语言证书");

    private final String code;
    private final String desc;
}
