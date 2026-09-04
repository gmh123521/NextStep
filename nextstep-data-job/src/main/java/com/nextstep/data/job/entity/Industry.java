package com.nextstep.data.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_industry")
public class Industry implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private String code;
    private String name;
    private Long parentId;
    private LocalDateTime createdAt;
}
