package com.nextstep.analysis.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class PathScore implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** PG / CS / EM */
    private String path;
    private String pathName;

    /** 综合分 0-100 */
    private int overall;

    /** 各维度分 */
    private List<DimensionScore> dimensions = new ArrayList<>();

    /** 文字建议 */
    private List<String> advice = new ArrayList<>();

    /** Top N 推荐项（院校/岗位） */
    private List<Recommendation> recommendations = new ArrayList<>();

    @Data
    public static class DimensionScore implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String name;
        private int score;
        public DimensionScore() {}
        public DimensionScore(String name, int score) { this.name = name; this.score = score; }
    }

    @Data
    public static class Recommendation implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String type;        // school / gov / job
        private Long refId;
        private String title;       // 主标题
        private String subtitle;    // 次要描述
        private int matchScore;     // 0-100
        private String tag;         // "稳" / "冲" / "保" 之类
    }
}
