package com.nextstep.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_user_experience")
public class UserExperience implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** INTERNSHIP / PROJECT / AWARD / RESEARCH / PAPER / COMPETITION */
    private String type;

    private String title;
    private String role;
    private String startDate;
    private String endDate;
    private String description;

    /** AI 生成的摘要（≤500字），供学长对话系统提示词使用 */
    private String summary;

    /** RESUME / MANUAL / CHAT */
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
