package dev.sheldan.abstracto.commands.service.management;

import dev.sheldan.abstracto.command.models.AModule;
import dev.sheldan.abstracto.command.service.management.ModuleManagementService;
import dev.sheldan.abstracto.commands.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModuleManagementServiceBean implements ModuleManagementService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Override
    public AModule createModule(String name) {
        AModule module = AModule.
                builder()
                .name(name)
                .build();
        moduleRepository.save(module);
        return module;
    }

    @Override
    public AModule getOrCreate(String name) {
        AModule module = findModuleByName(name);
        if(module == null) {
            return createModule(name);
        }
        return module;
    }

    @Override
    public AModule findModuleByName(String name) {
        return moduleRepository.findByName(name);
    }

    @Override
    public Boolean doesModuleExist(String name) {
        return findModuleByName(name) != null;
    }
}
