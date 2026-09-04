package com.nextstep.data.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_salary_stat")
public class SalaryStat implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long positionId;
    private String city;
    private String experience;
    private String degree;
    private Integer minSalary;
    private Integer maxSalary;
    private Integer medianSalary;
    private Integer sampleSize;
    private String dataSource;
    private Integer statYear;
    private LocalDateTime createdAt;
}
