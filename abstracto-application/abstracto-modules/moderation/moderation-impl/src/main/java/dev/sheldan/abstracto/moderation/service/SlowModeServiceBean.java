package dev.sheldan.abstracto.moderation.service;

import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.service.Bot;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class SlowModeServiceBean implements SlowModeService {

    @Autowired
    private Bot bot;

    @Override
    public void setSlowMode(TextChannel channel, Duration duration) {
        long seconds = duration.getSeconds();
        if(seconds > TextChannel.MAX_SLOWMODE) {
            throw new IllegalArgumentException("Slow mode duration must be < " + TextChannel.MAX_SLOWMODE + " seconds.");
        }
        channel.getManager().setSlowmode((int) seconds).queue();
    }

    @Override
    public void setSlowMode(AChannel channel, Duration duration) {
        TextChannel textChannel = bot.getTextChannelFromServer(channel.getServer().getId(), channel.getId());
        this.setSlowMode(textChannel, duration);
    }
}
