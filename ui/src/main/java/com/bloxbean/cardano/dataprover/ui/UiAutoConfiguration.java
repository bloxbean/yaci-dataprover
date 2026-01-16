package com.bloxbean.cardano.dataprover.ui;

import com.bloxbean.cardano.dataprover.ui.controller.SpaController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for the DataProver Admin UI.
 * Only activates when dataprover.ui.enabled=true.
 */
@AutoConfiguration
@EnableConfigurationProperties(UiProperties.class)
@ConditionalOnProperty(name = "dataprover.ui.enabled", havingValue = "true")
public class UiAutoConfiguration {

    @Bean
    public SpaController spaController() {
        return new SpaController();
    }

    @Bean
    public WebMvcConfigurer uiWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/ui/**")
                        .addResourceLocations("classpath:/static/");
            }
        };
    }
}
