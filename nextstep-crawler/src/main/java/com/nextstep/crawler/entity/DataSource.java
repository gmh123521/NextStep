package com.nextstep.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_data_source")
public class DataSource implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceCode;
    private String sourceName;
    private String organization;
    private String sourceType;
    private String sourceUrl;
    private Integer enabled;
    private String updateFrequency;
    private String licenseNote;
    private String parserVersion;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
