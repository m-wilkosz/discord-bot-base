package dev.sheldan.abstracto.core.service;

import dev.sheldan.abstracto.core.exception.EmoteNotDefinedException;
import dev.sheldan.abstracto.core.exception.GuildException;
import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.service.management.EmoteManagementService;
import dev.sheldan.abstracto.core.models.database.AEmote;
import dev.sheldan.abstracto.templating.model.MessageToSend;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class MessageServiceBean implements MessageService {

    @Autowired
    private BotService botService;

    @Autowired
    private EmoteManagementService emoteManagementService;

    @Autowired
    private EmoteService emoteService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private MessageServiceBean self;

    @Override
    public void addReactionToMessage(String emoteKey, Long serverId, Message message) {
        Optional<Guild> guildByIdOptional = botService.getGuildById(serverId);
        AEmote emote = emoteService.getEmoteOrFakeEmote(emoteKey, serverId);
        if(guildByIdOptional.isPresent()) {
            Guild guild = guildByIdOptional.get();
                if(emote.getCustom()) {
                    Emote emoteById = botService.getInstance().getEmoteById(emote.getEmoteId());
                    if(emoteById != null) {
                        message.addReaction(emoteById).queue();
                    } else {
                        log.error("Emote with key {} and id {} for guild {} was not found.", emoteKey, emote.getEmoteId(), guild.getId());
                        throw new EmoteNotDefinedException(emoteKey);
                    }
                } else {
                    message.addReaction(emote.getEmoteKey()).queue();
                }
        } else {
            log.error("Cannot add reaction, guild not found {}", serverId);
            throw new GuildException(serverId);
        }
    }

    @Override
    public CompletableFuture<Void> deleteMessageInChannelInServer(Long serverId, Long channelId, Long messageId) {
        return botService.deleteMessage(serverId, channelId, messageId);
    }

    @Override
    public CompletableFuture<Message> createStatusMessage(MessageToSend messageToSend, AChannel channel) {
        return channelService.sendMessageToSendToAChannel(messageToSend, channel).get(0);
    }

    @Override
    public void updateStatusMessage(AChannel channel, Long messageId, MessageToSend messageToSend) {
        channelService.editMessageInAChannel(messageToSend, channel, messageId);
    }

    @Override
    public void sendMessageToUser(AUserInAServer userInAServer, String text, TextChannel feedbackChannel) {
        Member memberInServer = botService.getMemberInServer(userInAServer);
        sendMessageToUser(memberInServer.getUser(), text, feedbackChannel);
    }

    @Override
    public void sendMessageToUser(User user, String text, TextChannel feedbackChannel) {
        CompletableFuture<Message> messageFuture = new CompletableFuture<>();

        user.openPrivateChannel().queue(privateChannel ->
            privateChannel.sendMessage(text).queue(messageFuture::complete, messageFuture::completeExceptionally)
        );

        messageFuture.exceptionally(e -> {
            log.warn("Failed to send message. ", e);
            if(feedbackChannel != null){
                self.sendFeedbackAboutException(e, feedbackChannel);
            }
            return null;
        });
    }

    @Transactional
    public void sendFeedbackAboutException(Throwable e, TextChannel feedbackChannel) {
        channelService.sendTextToChannelNoFuture(String.format("Failed to send message: %s", e.getMessage()), feedbackChannel);
    }
}
