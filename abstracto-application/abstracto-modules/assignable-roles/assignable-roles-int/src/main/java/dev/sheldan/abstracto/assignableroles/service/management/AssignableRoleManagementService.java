package dev.sheldan.abstracto.assignableroles.service.management;

import dev.sheldan.abstracto.assignableroles.models.database.AssignableRole;
import dev.sheldan.abstracto.assignableroles.models.database.AssignableRolePlace;
import dev.sheldan.abstracto.assignableroles.models.database.AssignableRolePlacePost;
import dev.sheldan.abstracto.core.models.cache.CachedEmote;
import dev.sheldan.abstracto.core.models.database.AEmote;
import dev.sheldan.abstracto.core.models.database.ARole;
import net.dv8tion.jda.api.entities.MessageReaction;

public interface AssignableRoleManagementService {
    AssignableRole addRoleToPlace(AssignableRolePlace place, AEmote emote, ARole role, String description, AssignableRolePlacePost post);
    AssignableRole addRoleToPlace(Long placeId, Integer emoteId, Long roleId, String description,  Long messageId);
    AssignableRole addRoleToPlace(Long placeId, Integer emoteId, Long roleId, String description);
    AssignableRole getByAssignableRoleId(Long assignableRoleId);
    AssignableRole getRoleForReactionEmote(MessageReaction.ReactionEmote emote, AssignableRolePlace assignableRolePlace);
    AssignableRole getRoleForReactionEmote(CachedEmote cachedEmote, AssignableRolePlace assignableRolePlace);
}
