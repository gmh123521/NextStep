package com.nextstep.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CurrentStatus {
    IN_SCHOOL("IN_SCHOOL", "在校"),
    GRADUATED("GRADUATED", "已毕业未就业"),
    EMPLOYED("EMPLOYED", "已就业"),
    PREPARING("PREPARING", "备考中"),
    JOB_HUNTING("JOB_HUNTING", "求职中");

    private final String code;
    private final String desc;
}
