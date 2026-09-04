package com.nextstep.data.school.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_school_major")
public class SchoolMajor implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long schoolId;
    private String majorCode;
    private String majorName;
    private String category;
    private String degreeType;
    private Integer year;
    private LocalDateTime createdAt;
}
