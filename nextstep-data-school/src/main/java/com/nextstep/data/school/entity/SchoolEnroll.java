package com.nextstep.data.school.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("ns_school_enroll")
public class SchoolEnroll implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long schoolId;
    private Long majorId;
    private Integer year;
    private Integer enrollPlan;
    private Integer enrollActual;
    private Integer applyCount;
    private Integer cutoffScore;
    private Integer cutoffEnglish;
    private Integer cutoffPolitical;
    private Integer lowestScore;
    private Integer highestScore;
}
