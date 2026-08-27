package com.nextstep.crawler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 爬虫任务运行记录 */
@Data
@TableName("ns_crawler_job")
public class CrawlerJob implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据源：KAOYAN / GOV_POST */
    private String source;

    /** SCHEDULE / MANUAL */
    private String triggerBy;

    /** RUNNING / SUCCESS / FAILED */
    private String status;

    private Integer fetched;
    private Integer inserted;
    private Integer skipped;

    private String message;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
