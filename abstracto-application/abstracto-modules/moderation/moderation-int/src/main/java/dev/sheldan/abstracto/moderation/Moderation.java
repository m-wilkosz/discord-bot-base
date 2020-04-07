package dev.sheldan.abstracto.moderation;

import dev.sheldan.abstracto.core.command.config.ModuleInterface;
import dev.sheldan.abstracto.core.command.config.ModuleInfo;
import org.springframework.stereotype.Component;

@Component
public class Moderation implements ModuleInterface {

    public static final String MODERATION = "moderation";

    @Override
    public ModuleInfo getInfo() {
        return ModuleInfo.builder().name(MODERATION).description("Utilities to moderate the server").build();
    }

    @Override
    public String getParentModule() {
        return "default";
    }
}
