package dev.sheldan.abstracto.core.listener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public interface JoinListener extends FeatureAware {
    void execute(Member member, Guild guild);
}
