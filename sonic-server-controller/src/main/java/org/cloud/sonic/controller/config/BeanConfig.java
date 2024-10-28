package org.cloud.sonic.controller.config;

import com.qaq.base.config.RestTemplateConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RestTemplateConfig.class})
public class BeanConfig {
}
