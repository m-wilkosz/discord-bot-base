package dev.sheldan.abstracto.moderation.config.features;

import dev.sheldan.abstracto.core.config.FeatureConfig;
import dev.sheldan.abstracto.core.config.FeatureEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class WarningFeature implements FeatureConfig {

    @Autowired
    private WarningDecayFeature warningDecayFeature;

    @Override
    public FeatureEnum getFeature() {
        return ModerationFeatures.WARNING;
    }

    @Override
    public List<FeatureConfig> getDependantFeatures() {
        return Arrays.asList(warningDecayFeature);
    }
}
