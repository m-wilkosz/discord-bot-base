package dev.sheldan.abstracto.moderation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:moderation-config.properties")
public class ModerationConfig {
}

