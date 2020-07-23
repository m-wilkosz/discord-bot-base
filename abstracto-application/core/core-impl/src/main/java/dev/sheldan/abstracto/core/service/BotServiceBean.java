package dev.sheldan.abstracto.core.service;

import dev.sheldan.abstracto.core.exception.ChannelNotFoundException;
import dev.sheldan.abstracto.core.exception.GuildNotFoundException;
import dev.sheldan.abstracto.core.models.GuildChannelMember;
import dev.sheldan.abstracto.core.models.database.AEmote;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUser;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.dv8tion.jda.api.requests.GatewayIntent.*;

@Service
@Slf4j
public class BotServiceBean implements BotService {

    private JDA instance;

    @Override
    public void login() throws LoginException {
        JDABuilder builder = JDABuilder.create(System.getenv("TOKEN"), GatewayIntent.GUILD_MEMBERS, GUILD_VOICE_STATES,
                GUILD_EMOJIS, GUILD_MEMBERS, GUILD_MESSAGE_REACTIONS, GUILD_MESSAGES,
                GUILD_MESSAGE_REACTIONS, DIRECT_MESSAGE_REACTIONS, DIRECT_MESSAGES, GUILD_PRESENCES);

        builder.setBulkDeleteSplittingEnabled(false);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);

        this.instance = builder.build();
    }

    @Override
    public JDA getInstance() {
        return instance;
    }

    @Override
    public GuildChannelMember getServerChannelUser(Long serverId, Long channelId, Long userId)  {
        Optional<Guild> guildOptional = getGuildById(serverId);
        if(guildOptional.isPresent()) {
            Guild guild = guildOptional.get();
            Optional<TextChannel> textChannelOptional = this.getTextChannelFromServerOptional(guild, channelId);
            if(textChannelOptional.isPresent()) {
                TextChannel textChannel = textChannelOptional.get();
                Member member = guild.getMemberById(userId);
                return GuildChannelMember.builder().guild(guild).textChannel(textChannel).member(member).build();
            } else {
                throw new ChannelNotFoundException(channelId);
            }
        }
        else {
            throw new GuildNotFoundException(serverId);
        }
    }

    @Override
    public Member getMemberInServer(Long serverId, Long memberId) {
        Guild guildById = instance.getGuildById(serverId);
        if(guildById != null) {
            return guildById.getMemberById(memberId);
        } else {
            throw new GuildNotFoundException(serverId);
        }
    }

    @Override
    public boolean isUserInGuild(AUserInAServer aUserInAServer) {
        Guild guildById = instance.getGuildById(aUserInAServer.getServerReference().getId());
        if(guildById != null) {
            return isUserInGuild(guildById, aUserInAServer);
        } else {
            throw new GuildNotFoundException(aUserInAServer.getServerReference().getId());
        }
    }

    @Override
    public boolean isUserInGuild(Guild guild, AUserInAServer aUserInAServer) {
        return guild.getMemberById(aUserInAServer.getUserReference().getId()) != null;
    }

    @Override
    public Member getMemberInServer(AUserInAServer aUserInAServer) {
        return getMemberInServer(aUserInAServer.getServerReference().getId(), aUserInAServer.getUserReference().getId());
    }

    @Override
    public Member getMemberInServer(AServer server, AUser member) {
        return getMemberInServer(server.getId(), member.getId());
    }

    @Override
    public CompletableFuture<Void> deleteMessage(Long serverId, Long channelId, Long messageId)  {
        Optional<TextChannel> textChannelOptional = getTextChannelFromServerOptional(serverId, channelId);
        if(textChannelOptional.isPresent()) {
            TextChannel textChannel = textChannelOptional.get();
            return textChannel.deleteMessageById(messageId).submit().exceptionally(throwable -> {
                log.warn("Deleting the message {} in channel {} in guild {} failed.", messageId, channelId, serverId, throwable);
                return null;
            });
        } else {
            log.warn("Could not find channel {} in guild {} to delete message {} in.", channelId, serverId, messageId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> deleteMessage(Long channelId, Long messageId) {
        TextChannel textChannel = getInstance().getTextChannelById(channelId);
        if(textChannel != null) {
            return textChannel.deleteMessageById(messageId).submit();
        } else {
            log.warn("Could not find channel {} to delete message {} in.", channelId, messageId);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Member> forceReloadMember(Member member) {
        return member.getGuild().retrieveMember(member.getUser()).submit();
    }

    @Override
    public Optional<Emote> getEmote(Long serverId, AEmote emote)  {
        if(Boolean.FALSE.equals(emote.getCustom())) {
            return Optional.empty();
        }
        Optional<Guild> guildById = getGuildById(serverId);
        if(guildById.isPresent()) {
            Guild guild = guildById.get();
            Emote emoteById = guild.getEmoteById(emote.getEmoteId());
            return Optional.ofNullable(emoteById);
        }
        throw new GuildNotFoundException(serverId);
    }

    @Override
    public Optional<Emote> getEmote(AEmote emote) {
        if(Boolean.FALSE.equals(emote.getCustom())) {
            return Optional.empty();
        }
        return Optional.ofNullable(instance.getEmoteById(emote.getEmoteId()));
    }

    @Override
    public Optional<TextChannel> getTextChannelFromServerOptional(Guild guild, Long textChannelId) {
        return Optional.ofNullable(guild.getTextChannelById(textChannelId));
    }

    @Override
    public TextChannel getTextChannelFromServer(Guild guild, Long textChannelId) {
        return getTextChannelFromServerOptional(guild, textChannelId).orElseThrow(() -> new ChannelNotFoundException(textChannelId));
    }

    @Override
    public Optional<TextChannel> getTextChannelFromServerOptional(Long serverId, Long textChannelId)  {
        Optional<Guild> guildOptional = getGuildById(serverId);
        if(guildOptional.isPresent()) {
            Guild guild = guildOptional.get();
            return Optional.ofNullable(guild.getTextChannelById(textChannelId));
        }
        throw new GuildNotFoundException(serverId);
    }

    @Override
    public TextChannel getTextChannelFromServer(Long serverId, Long textChannelId) {
        return getTextChannelFromServerOptional(serverId, textChannelId).orElseThrow(() -> new ChannelNotFoundException(textChannelId));
    }

    @Override
    public Optional<Guild> getGuildById(Long serverId) {
        return Optional.ofNullable(instance.getGuildById(serverId));
    }

    @Override
    public Guild getGuildByIdNullable(Long serverId) {
        return instance.getGuildById(serverId);
    }

    @Override
    public Member getBotInGuild(AServer server) {
        Optional<Guild> guildOptional = getGuildById(server.getId());
        if(guildOptional.isPresent()) {
            Guild guild = guildOptional.get();
            return guild.getMemberById(instance.getSelfUser().getId());
        }
        return null;
    }
}
