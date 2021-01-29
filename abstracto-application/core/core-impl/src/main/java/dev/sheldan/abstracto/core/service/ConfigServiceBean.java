package dev.sheldan.abstracto.core.service;

import dev.sheldan.abstracto.core.exception.ConfigurationKeyNotFoundException;
import dev.sheldan.abstracto.core.models.database.AConfig;
import dev.sheldan.abstracto.core.service.management.ConfigManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigServiceBean implements ConfigService{

    @Autowired
    private ConfigManagementService configManagementService;

    @Override
    public Double getDoubleValue(String name, Long serverId) {
       return getDoubleValue(name, serverId, 0D);
    }

    @Override
    public Long getLongValue(String name, Long serverId) {
        return getLongValue(name, serverId, 0L);
    }

    @Override
    public Double getDoubleValue(String name, Long serverId, Double defaultValue) {
        AConfig config = configManagementService.loadConfig(serverId, name);
        if(config == null) {
            return defaultValue;
        }
        return config.getDoubleValue();
    }

    @Override
    public String getStringValue(String name, Long serverId, String defaultValue) {
        AConfig config = configManagementService.loadConfig(serverId, name);
        if(config == null) {
            return defaultValue;
        }
        return config.getStringValue();
    }

    @Override
    public Long getLongValue(String name, Long serverId, Long defaultValue) {
        AConfig config = configManagementService.loadConfig(serverId, name);
        if(config == null) {
            return defaultValue;
        }
        return config.getLongValue();
    }

    @Override
    public void setDoubleValue(String name, Long serverId, Double value) {
        if(configManagementService.configExists(serverId, name)) {
            configManagementService.setDoubleValue(serverId, name, value);
        } else {
            throw new ConfigurationKeyNotFoundException(name);
        }
    }

    @Override
    public void setLongValue(String name, Long serverId, Long value) {
        if(configManagementService.configExists(serverId, name)) {
            configManagementService.setLongValue(serverId, name, value);
        } else {
            throw new ConfigurationKeyNotFoundException(name);
        }
    }

    @Override
    public void setConfigValue(String name, Long serverId, String value) {
        if(configManagementService.configExists(serverId, name)) {
            AConfig existing = configManagementService.loadConfig(serverId, name);
            if(existing.getDoubleValue() != null) {
                setDoubleValue(name, serverId, Double.parseDouble(value));
            } else if(existing.getLongValue() != null) {
                setLongValue(name, serverId, Long.parseLong(value));
            } else {
                setStringValue(name, serverId, value);
            }
        } else {
            throw new ConfigurationKeyNotFoundException(name);
        }

    }

    @Override
    public void setConfigValue(String name, Long serverId, AConfig value) {
        if(value.getDoubleValue() != null) {
            setDoubleValue(name, serverId, value.getDoubleValue());
        } else if(value.getLongValue() != null) {
            setLongValue(name, serverId, value.getLongValue());
        } else {
            setStringValue(name, serverId, value.getStringValue());
        }
    }

    @Override
    public void setStringValue(String name, Long serverId, String value) {
        if(configManagementService.configExists(serverId, name)) {
            configManagementService.setStringValue(serverId, name, value);
        } else {
            throw new ConfigurationKeyNotFoundException(name);
        }
    }

    @Override
    public boolean configIsFitting(String name, Long serverId, String value) {
        try {
            getFakeConfigForValue(name, serverId, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public AConfig getFakeConfigForValue(String name, Long serverId, String value) {
        if(configManagementService.configExists(serverId, name)) {
            AConfig newConfig = AConfig.builder().name(value).build();
            AConfig existing = configManagementService.loadConfig(serverId, name);
            if(existing.getDoubleValue() != null) {
                newConfig.setDoubleValue(Double.parseDouble(value));
            } else if(existing.getLongValue() != null) {
                newConfig.setLongValue(Long.parseLong(value));
            } else {
                newConfig.setStringValue(value);
            }
            return newConfig;
        } else {
            throw new ConfigurationKeyNotFoundException(name);
        }
    }
}
