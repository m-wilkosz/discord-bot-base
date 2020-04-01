package dev.sheldan.abstracto.core.service.management;

import dev.sheldan.abstracto.core.management.ChannelManagementService;
import dev.sheldan.abstracto.core.management.PostTargetManagement;
import dev.sheldan.abstracto.core.management.ServerManagementService;
import dev.sheldan.abstracto.core.management.UserManagementService;
import dev.sheldan.abstracto.core.models.database.*;
import dev.sheldan.abstracto.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ServerManagementServiceBean implements ServerManagementService {

    @Autowired
    private ServerRepository repository;

    @Autowired
    private PostTargetManagement postTargetManagement;

    @Autowired
    private ChannelManagementService channelManagementService;

    @Autowired
    private UserManagementService userManagementService;

    @Override
    public AServer createServer(Long id) {
        return repository.save(AServer.builder().id(id).build());
    }

    @Override
    public AServer loadServer(Long id) {
        if(repository.existsById(id)) {
            return repository.getOne(id);
        } else {
            return createServer(id);
        }
    }

    @Override
    public void addChannelToServer(AServer server, AChannel channel) {
        server.getChannels().add(channel);
        channel.setServer(server);
        repository.save(server);
    }

    @Override
    public AUserInAServer addUserToServer(AServer server, AUser user) {
        return this.addUserToServer(server.getId(), user.getId());
    }

    @Override
    public AUserInAServer addUserToServer(Long serverId, Long userId) {
        log.info("Adding user {} to server {}", userId, serverId);
        AServer server = repository.getOne(serverId);
        AUser user = userManagementService.loadUser(userId);
        AUserInAServer aUserInAServer = AUserInAServer.builder().serverReference(server).userReference(user).build();
        server.getUsers().add(aUserInAServer);
        return aUserInAServer;
    }

    @Override
    public AChannel getPostTarget(Long serverId, String name) {
        AServer server = this.loadServer(serverId);
        return getPostTarget(server, name);
    }

    @Override
    public AChannel getPostTarget(Long serverId, PostTarget target) {
        AServer server = this.loadServer(serverId);
        return getPostTarget(server, target);
    }

    @Override
    public AChannel getPostTarget(AServer server, PostTarget target) {
        return target.getChannelReference();
    }

    @Override
    public AChannel getPostTarget(AServer server, String name) {
        PostTarget target = postTargetManagement.getPostTarget(name, server);
        return getPostTarget(server, target);
    }

    @Override
    public List<AServer> getAllServers() {
        return repository.findAll();
    }


}
