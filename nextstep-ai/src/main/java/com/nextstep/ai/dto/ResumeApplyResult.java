package com.nextstep.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ResumeApplyResult implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 新增经历条数 */
    private int inserted;

    /** 跳过的重复经历条数 */
    private int skipped;
}
