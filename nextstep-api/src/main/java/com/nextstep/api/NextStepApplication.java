package com.nextstep.api;

import com.nextstep.framework.FrameworkAutoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(FrameworkAutoConfig.class)
@ComponentScan(basePackages = {
        "com.nextstep.api",
        "com.nextstep.auth",
        "com.nextstep.user",
        "com.nextstep.data",
        "com.nextstep.analysis",
        "com.nextstep.planner",
        "com.nextstep.ai",
        "com.nextstep.report"
})
public class NextStepApplication {

    public static void main(String[] args) {
        SpringApplication.run(NextStepApplication.class, args);
    }
}
