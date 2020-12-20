package dev.sheldan.abstracto.core.service.management;

import dev.sheldan.abstracto.core.models.ServerUser;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUser;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;
import java.util.Optional;

public interface UserInServerManagementService {
    AUserInAServer loadUser(Long serverId, Long userId);
    AUserInAServer loadUser(ServerUser serverUser);
    Optional<AUserInAServer> loadUserOptional(Long serverId, Long userId);
    AUserInAServer loadUser(AServer server, AUser user);
    AUserInAServer loadUser(Member member);
    Optional<AUserInAServer> loadUserOptional(Long userInServerId);
    AUserInAServer loadUser(Long userInServerId);
    AUserInAServer createUserInServer(Member member);
    AUserInAServer createUserInServer(Long guildId, Long userId);
    List<AUserInAServer> getUserInAllServers(Long userId);
    Optional<AUserInAServer> loadAUserInAServerOptional(Long serverId, Long userId);
}
