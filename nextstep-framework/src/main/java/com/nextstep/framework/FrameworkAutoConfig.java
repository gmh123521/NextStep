package com.nextstep.framework;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 由业务模块通过 @Import(FrameworkAutoConfig.class) 启用框架层 Bean
 */
@Configuration
@ComponentScan(basePackages = "com.nextstep.framework")
public class FrameworkAutoConfig {
}
