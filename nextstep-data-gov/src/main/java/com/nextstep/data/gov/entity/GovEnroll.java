package com.nextstep.data.gov.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@TableName("ns_gov_enroll")
public class GovEnroll implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    private Long postId;
    private Integer enrollCount;
    private Integer applyCount;
    private Integer applyPass;
    private Integer attendCount;
}
