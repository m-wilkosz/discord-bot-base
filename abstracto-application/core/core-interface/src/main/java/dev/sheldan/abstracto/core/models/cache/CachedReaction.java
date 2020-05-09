package dev.sheldan.abstracto.core.models.cache;

import dev.sheldan.abstracto.core.models.database.AEmote;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CachedReaction {
    private AEmote emote;
    private List<Long> userInServersIds;
}
