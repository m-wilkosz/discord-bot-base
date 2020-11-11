package dev.sheldan.abstracto.core.service;

import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.templating.model.MessageToSend;
import dev.sheldan.abstracto.core.models.database.AChannel;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ChannelService {
    void sendTextToAChannelNotAsync(String text, AChannel channel);
    void sendTextToChannelNotAsync(String text, MessageChannel channel);
    CompletableFuture<Message> sendTextToAChannel(String text, AChannel channel);
    CompletableFuture<Message> sendMessageToAChannel(Message message, AChannel channel);
    CompletableFuture<Message> sendMessageToChannel(Message message, MessageChannel channel);
    CompletableFuture<Message> sendTextToChannel(String text, MessageChannel channel);
    CompletableFuture<Message> sendEmbedToAChannel(MessageEmbed embed, AChannel channel);
    CompletableFuture<Message> sendEmbedToChannel(MessageEmbed embed, MessageChannel channel);
    MessageAction sendEmbedToChannelInComplete(MessageEmbed embed, MessageChannel channel);
    List<CompletableFuture<Message>> sendMessageToSendToAChannel(MessageToSend messageToSend, AChannel channel);
    CompletableFuture<Message> sendMessageToSendToAChannel(MessageToSend messageToSend, AChannel channel, Integer embedIndex);
    List<CompletableFuture<Message>> sendMessageToSendToChannel(MessageToSend messageToSend, MessageChannel textChannel);
    Optional<TextChannel> getTextChannelInGuild(Long serverId, Long channelId);
    void editMessageInAChannel(MessageToSend messageToSend, AChannel channel, Long messageId);
    void editMessageInAChannel(MessageToSend messageToSend, MessageChannel channel, Long messageId);
    CompletableFuture<Message> editMessageInAChannelFuture(MessageToSend messageToSend, MessageChannel channel, Long messageId);
    CompletableFuture<Message> editEmbedMessageInAChannel(MessageEmbed embedToSend, MessageChannel channel, Long messageId);
    CompletableFuture<Message> editTextMessageInAChannel(String text, MessageChannel channel, Long messageId);
    List<CompletableFuture<Message>> editMessagesInAChannelFuture(MessageToSend messageToSend, MessageChannel channel, List<Long> messageIds);
    CompletableFuture<Message> removeFieldFromMessage(MessageChannel channel, Long messageId, Integer index);
    CompletableFuture<Message> removeFieldFromMessage(MessageChannel channel, Long messageId, Integer index, Integer embedIndex);
    CompletableFuture<Void> deleteTextChannel(AChannel channel);
    CompletableFuture<Void> deleteTextChannel(Long serverId, Long channelId);
    List<CompletableFuture<Message>> sendEmbedTemplateInChannel(String templateKey, Object model, MessageChannel channel);
    CompletableFuture<Message> sendTextTemplateInChannel(String templateKey, Object model, MessageChannel channel);

    CompletableFuture<TextChannel> createTextChannel(String name, AServer server, Long categoryId);
    Optional<TextChannel> getChannelFromAChannel(AChannel channel);
    AChannel getFakeChannelFromTextChannel(TextChannel textChannel);
}
