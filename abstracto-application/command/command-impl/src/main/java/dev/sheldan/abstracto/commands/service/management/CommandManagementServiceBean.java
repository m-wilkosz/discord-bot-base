package dev.sheldan.abstracto.commands.service.management;

import dev.sheldan.abstracto.command.models.ACommand;
import dev.sheldan.abstracto.command.models.AModule;
import dev.sheldan.abstracto.command.service.management.CommandManagementService;
import dev.sheldan.abstracto.command.service.management.ModuleManagementService;
import dev.sheldan.abstracto.commands.repository.CommandRepository;
import dev.sheldan.abstracto.core.service.management.ServerManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommandManagementServiceBean implements CommandManagementService {

    @Autowired
    private ServerManagementService serverManagementService;

    @Autowired
    private ModuleManagementService moduleManagementService;

    @Autowired
    private CommandRepository commandRepository;

    @Override
    public ACommand createCommand(String name, String moduleName) {
        AModule module = moduleManagementService.findModuleByName(moduleName);
        return createCommand(name, module);
    }

    @Override
    public ACommand createCommand(String name, AModule module) {
        ACommand command = ACommand
                .builder()
                .name(name)
                .module(module)
                .build();
        commandRepository.save(command);
        return command;
    }

    @Override
    public ACommand findCommandByName(String name) {
        return commandRepository.findByName(name);
    }

    @Override
    public Boolean doesCommandExist(String name) {
        return findCommandByName(name) != null;
    }

}
