package com.nextstep.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ns_data_raw_record")
public class DataRawRecord implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Integer recordNo;
    private String rawPayload;
    private String payloadHash;
    private String parseStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
