package dev.sheldan.abstracto.core.models.listener;

import dev.sheldan.abstracto.core.listener.FeatureAwareListenerModel;
import dev.sheldan.abstracto.core.models.ServerUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

@Getter
@Setter
@Builder
public class UserUnBannedModel implements FeatureAwareListenerModel {
    private ServerUser unBannedServerUser;
    private ServerUser unBanningServerUser;
    private User unBannedUser;
    private User unBanningUser;
    private Guild guild;
    private String reason;
    @Override
    public Long getServerId() {
        return guild.getIdLong();
    }
}
