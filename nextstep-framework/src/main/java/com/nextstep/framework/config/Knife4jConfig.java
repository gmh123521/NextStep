package com.nextstep.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI nextstepOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("NextStep API")
                .description("考研/考公/就业三路径决策辅助系统")
                .version("v1.0.0")
                .contact(new Contact().name("NextStep Team")));
    }
}
