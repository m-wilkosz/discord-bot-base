package dev.sheldan.abstracto.core.service.management;

import dev.sheldan.abstracto.core.models.database.AConfig;

public interface ConfigManagementService {
    AConfig setOrCreateStringValue(Long serverId, String name, String value);
    AConfig setOrCreateDoubleValue(Long serverId, String name, Double value);
    AConfig createConfig(Long serverId, String name, String value);
    AConfig createConfig(Long serverId, String name, Double value);
    AConfig createConfig(Long serverId, String name, Long value);
    AConfig createIfNotExists(Long serverId, String name, String value);
    AConfig createIfNotExists(Long serverId, String name, Double value);
    AConfig loadConfig(Long serverId, String name);
    boolean configExists(Long serverId, String name);
    AConfig setDoubleValue(Long serverId, String name, Double value);
    AConfig setLongValue(Long serverId, String name, Long value);
    AConfig setStringValue(Long serverId, String name, String value);
}
