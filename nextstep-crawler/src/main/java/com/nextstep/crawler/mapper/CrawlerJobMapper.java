package com.nextstep.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nextstep.crawler.entity.CrawlerJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrawlerJobMapper extends BaseMapper<CrawlerJob> {
}
