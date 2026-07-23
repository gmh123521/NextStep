package com.nextstep.analysis.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnalysisResult implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private Long userId;
    private LocalDateTime analyzedAt;
    private int profileCompleteness;

    /** 三条路径评分 */
    private List<PathScore> paths;

    /** 总体推荐路径 */
    private String topPath;
    private String topPathReason;
}
