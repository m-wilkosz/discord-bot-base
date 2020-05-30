package dev.sheldan.abstracto.experience.config;

import dev.sheldan.abstracto.core.listener.ServerConfigListener;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.service.management.ConfigManagementService;
import dev.sheldan.abstracto.experience.config.features.ExperienceFeatureConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener responsible to configure the required experience configurations in case the bot joins a new server.
 */
@Component
@Slf4j
public class ExperienceConfigListener implements ServerConfigListener {


    @Autowired
    private ExperienceConfig experienceConfig;

    @Autowired
    private ConfigManagementService service;

    @Override
    public void updateServerConfig(AServer server) {
        log.info("Setting up experience configuration for server {}.", server.getId());
        service.createIfNotExists(server.getId(), ExperienceFeatureConfig.MIN_EXP_KEY, experienceConfig.getMinExp().longValue());
        service.createIfNotExists(server.getId(), ExperienceFeatureConfig.MAX_EXP_KEY, experienceConfig.getMaxExp().longValue());
        service.createIfNotExists(server.getId(), ExperienceFeatureConfig.EXP_MULTIPLIER_KEY, experienceConfig.getExpMultiplier());
    }
}
