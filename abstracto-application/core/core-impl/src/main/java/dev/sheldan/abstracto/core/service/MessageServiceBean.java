package dev.sheldan.abstracto.core.service;

import dev.sheldan.abstracto.core.metrics.service.CounterMetric;
import dev.sheldan.abstracto.core.metrics.service.MetricService;
import dev.sheldan.abstracto.core.metrics.service.MetricTag;
import dev.sheldan.abstracto.core.models.cache.CachedMessage;
import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.utils.FutureUtils;
import dev.sheldan.abstracto.templating.model.MessageToSend;
import dev.sheldan.abstracto.templating.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static dev.sheldan.abstracto.core.config.MetricConstants.DISCORD_API_INTERACTION_METRIC;
import static dev.sheldan.abstracto.core.config.MetricConstants.INTERACTION_TYPE;

@Component
@Slf4j
public class MessageServiceBean implements MessageService {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MetricService metricService;

    public static final CounterMetric MESSAGE_SEND_METRIC = CounterMetric
            .builder()
            .name(DISCORD_API_INTERACTION_METRIC)
            .tagList(Arrays.asList(MetricTag.getTag(INTERACTION_TYPE, "message.send")))
            .build();

    public static final CounterMetric MESSAGE_EDIT_METRIC = CounterMetric
            .builder()
            .name(DISCORD_API_INTERACTION_METRIC)
            .tagList(Arrays.asList(MetricTag.getTag(INTERACTION_TYPE, "message.edit")))
            .build();

    public static final CounterMetric MESSAGE_LOAD_METRIC = CounterMetric
            .builder()
            .name(DISCORD_API_INTERACTION_METRIC)
            .tagList(Arrays.asList(MetricTag.getTag(INTERACTION_TYPE, "message.load")))
            .build();

    public static final CounterMetric MESSAGE_DELETE_METRIC = CounterMetric
            .builder()
            .name(DISCORD_API_INTERACTION_METRIC)
            .tagList(Arrays.asList(MetricTag.getTag(INTERACTION_TYPE, "message.delete")))
            .build();

    @Override
    public CompletableFuture<Void> deleteMessageInChannelInServer(Long serverId, Long channelId, Long messageId) {
        metricService.incrementCounter(MESSAGE_DELETE_METRIC);
        return channelService.getTextChannelFromServer(serverId, channelId).deleteMessageById(messageId).submit();
    }

    @Override
    public CompletableFuture<Message> createStatusMessage(MessageToSend messageToSend, AChannel channel) {
        return channelService.sendMessageToSendToAChannel(messageToSend, channel).get(0);
    }

    @Override
    public CompletableFuture<Message> createStatusMessage(MessageToSend messageToSend, MessageChannel channel) {
        return channelService.sendMessageToSendToChannel(messageToSend, channel).get(0);
    }

    @Override
    public CompletableFuture<Long> createStatusMessageId(MessageToSend messageToSend, MessageChannel channel) {
        return channelService.sendMessageToSendToChannel(messageToSend, channel).get(0).thenApply(ISnowflake::getIdLong);
    }

    @Override
    public void updateStatusMessage(AChannel channel, Long messageId, MessageToSend messageToSend) {
        channelService.editMessageInAChannel(messageToSend, channel, messageId);
    }

    @Override
    public void updateStatusMessage(MessageChannel channel, Long messageId, MessageToSend messageToSend) {
        channelService.editMessageInAChannel(messageToSend, channel, messageId);
    }

    @Override
    public CompletableFuture<Message> sendMessageToUser(AUserInAServer userInAServer, String text) {
        return memberService.getMemberInServerAsync(userInAServer).thenCompose(member ->
            sendMessageToUser(member.getUser(), text)
        );
    }

    @Override
    public CompletableFuture<Message> sendSimpleTemplateToUser(Long userId, String templateKey) {
        String text = templateService.renderSimpleTemplate(templateKey);
        return memberService.getUserViaId(userId)
                .thenCompose(this::openPrivateChannelForUser)
                .thenCompose(o -> channelService.sendTextToChannel(text, o));
    }

    @Override
    public CompletableFuture<Message> sendTemplateToUser(User user, String template, Object model) {
        String message = templateService.renderTemplate(template, model);
        return sendMessageToUser(user, message);
    }

    @Override
    public CompletableFuture<Void> sendEmbedToUser(User user, String template, Object model) {
        return openPrivateChannelForUser(user).thenCompose(privateChannel ->
                FutureUtils.toSingleFutureGeneric(channelService.sendEmbedTemplateInChannel(template, model, privateChannel)));
    }

    @NotNull
    public CompletableFuture<PrivateChannel> openPrivateChannelForUser(User user) {
        return user.openPrivateChannel().submit();
    }

    @Override
    public CompletableFuture<Message> sendEmbedToUserWithMessage(User user, String template, Object model) {
        log.trace("Sending direct message with template {} to user {}.", template, user.getIdLong());
        return openPrivateChannelForUser(user).thenCompose(privateChannel ->
                channelService.sendEmbedTemplateInChannel(template, model, privateChannel).get(0));
    }

    @Override
    public CompletableFuture<Message> sendMessageToSendToUser(User user, MessageToSend messageToSend) {
        return openPrivateChannelForUser(user).thenCompose(privateChannel -> channelService.sendMessageToSendToChannel(messageToSend, privateChannel).get(0));
    }

    @Override
    public CompletableFuture<Message> sendMessageToUser(User user, String text) {
        log.trace("Sending direct string message to user {}.", user.getIdLong());
        return user.openPrivateChannel().flatMap(privateChannel -> privateChannel.sendMessage(text)).submit();
    }

    @Override
    public CompletableFuture<Void> deleteMessageInChannelWithUser(User user, Long messageId) {
        log.info("Deleting message {} in channel with user {}.", messageId, user.getIdLong());
        return user.openPrivateChannel().flatMap(privateChannel -> privateChannel.deleteMessageById(messageId)).submit();
    }

    @Override
    public CompletableFuture<Void> editMessageInDMChannel(User user, MessageToSend messageToSend, Long messageId) {
        return openPrivateChannelForUser(user).thenCompose(privateChannel -> channelService.editMessageInAChannelFuture(messageToSend, privateChannel, messageId).thenApply(message -> null));
    }

    @Override
    public CompletableFuture<Message> loadMessageFromCachedMessage(CachedMessage cachedMessage) {
        return loadMessage(cachedMessage.getServerId(), cachedMessage.getChannelId(), cachedMessage.getMessageId());
    }

    @Override
    public CompletableFuture<Message> loadMessage(Long serverId, Long channelId, Long messageId) {
        return channelService.retrieveMessageInChannel(serverId, channelId, messageId);
    }

    @Override
    public MessageAction editMessage(Message message, MessageEmbed messageEmbed) {
        metricService.incrementCounter(MESSAGE_EDIT_METRIC);
        return message.editMessage(messageEmbed);
    }

    @Override
    public MessageAction editMessage(Message message, String text, MessageEmbed messageEmbed) {
        metricService.incrementCounter(MESSAGE_EDIT_METRIC);
        return message.editMessage(text).embed(messageEmbed);
    }

    @Override
    public AuditableRestAction<Void> deleteMessage(Message message) {
        metricService.incrementCounter(MESSAGE_DELETE_METRIC);
        return message.delete();
    }

    @PostConstruct
    public void postConstruct() {
        metricService.registerCounter(MESSAGE_SEND_METRIC, "Messages send to discord");
        metricService.registerCounter(MESSAGE_EDIT_METRIC, "Messages edited in discord");
        metricService.registerCounter(MESSAGE_LOAD_METRIC, "Messages loaded from discord");
        metricService.registerCounter(MESSAGE_DELETE_METRIC, "Messages deleted from discord");
    }
}
