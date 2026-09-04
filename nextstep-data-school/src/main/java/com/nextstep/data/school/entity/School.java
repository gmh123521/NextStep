package com.nextstep.data.school.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_school")
public class School implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private String name;
    private String code;
    private String province;
    private String city;
    private String level;
    private String type;
    private Integer isSelfMarking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
