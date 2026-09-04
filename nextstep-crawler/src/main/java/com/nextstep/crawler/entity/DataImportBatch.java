package com.nextstep.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_data_import_batch")
public class DataImportBatch implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceCode;
    private Integer dataYear;
    private String contentHash;
    private String parserVersion;
    private String sourceUrl;
    private String snapshotPath;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer skippedCount;
    private Integer failedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
