package com.nextstep.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class TranscriptApplyResult implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 新增课程数 */
    private int inserted;

    /** 跳过的重复课程数 */
    private int skipped;

    /** 是否同步更新了画像里的 GPA */
    private boolean profileGpaUpdated;
}
