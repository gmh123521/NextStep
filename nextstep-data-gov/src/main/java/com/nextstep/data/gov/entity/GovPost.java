package com.nextstep.data.gov.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_gov_post")
public class GovPost implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Integer year;
    private String examType;
    private String province;
    private String deptName;
    private String postCode;
    private String postName;
    private String region;
    private String degreeRequired;
    private String majorRequired;
    private String political;
    private String extraRequired;
    private LocalDateTime createdAt;
}
