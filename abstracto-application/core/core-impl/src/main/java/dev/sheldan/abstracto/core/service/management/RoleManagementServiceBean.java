package dev.sheldan.abstracto.core.service.management;

import dev.sheldan.abstracto.core.models.database.ARole;
import dev.sheldan.abstracto.core.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleManagementServiceBean implements RoleManagementService {

    @Autowired
    private RoleRepository repository;

    @Override
    public ARole createRole(Long id) {
        return repository.save(ARole.builder().id(id).build());
    }
}
