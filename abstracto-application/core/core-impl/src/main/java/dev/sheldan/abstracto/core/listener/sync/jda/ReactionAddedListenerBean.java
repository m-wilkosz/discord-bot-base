package dev.sheldan.abstracto.core.listener.sync.jda;

import dev.sheldan.abstracto.core.config.FeatureConfig;
import dev.sheldan.abstracto.core.models.ServerUser;
import dev.sheldan.abstracto.core.models.cache.CachedMessage;
import dev.sheldan.abstracto.core.models.cache.CachedReactions;
import dev.sheldan.abstracto.core.service.*;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.core.utils.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ReactionAddedListenerBean extends ListenerAdapter {

    @Autowired
    private CacheEntityService cacheEntityService;

    @Autowired
    private MessageCache messageCache;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired(required = false)
    private List<ReactionAddedListener> addedListenerList;

    @Autowired
    private ReactionAddedListenerBean self;

    @Autowired
    private FeatureConfigService featureConfigService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private BotService botService;

    @Autowired
    private EmoteService emoteService;

    @Override
    @Transactional
    public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event) {
        if(addedListenerList == null) return;
        if(event.getUserIdLong() == botService.getInstance().getSelfUser().getIdLong()) {
            return;
        }
        CompletableFuture<CachedMessage> asyncMessageFromCache = messageCache.getMessageFromCache(event.getGuild().getIdLong(), event.getChannel().getIdLong(), event.getMessageIdLong());
        asyncMessageFromCache.thenAccept(cachedMessage ->
                cacheEntityService.getCachedReactionFromReaction(event.getReaction()).thenAccept(reaction -> {
                self.callAddedListeners(event, cachedMessage, reaction);
                    messageCache.putMessageInCache(cachedMessage);
            }).exceptionally(throwable -> {
                log.error("Failed to handle add reaction to message {} ", event.getMessageIdLong(), throwable);
                return null;
            })
        ).exceptionally(throwable -> {
            log.error("Message retrieval {} from cache failed. ", event.getMessageIdLong(), throwable);
            return null;
        });
    }

    private void addReactionIfNotThere(CachedMessage message, CachedReactions reaction, ServerUser userReacting) {
        Optional<CachedReactions> existingReaction = message.getReactions().stream().filter(reaction1 ->
                reaction1.getEmote().equals(reaction.getEmote())
        ).findAny();
        if(!existingReaction.isPresent()) {
            message.getReactions().add(reaction);
        } else {
            CachedReactions cachedReaction = existingReaction.get();
            Optional<ServerUser> any = cachedReaction.getUsers().stream().filter(user -> user.getServerId().equals(userReacting.getServerId()) && user.getUserId().equals(userReacting.getUserId())).findAny();
            if(!any.isPresent()){
                cachedReaction.getUsers().add(userReacting);
            }
        }
    }

    @Transactional
    public void callAddedListeners(@Nonnull GuildMessageReactionAddEvent event, CachedMessage cachedMessage, CachedReactions reaction) {
        ServerUser serverUser = ServerUser.builder().serverId(cachedMessage.getServerId()).userId(event.getUserIdLong()).build();
        addReactionIfNotThere(cachedMessage, reaction, serverUser);
        addedListenerList.forEach(reactedAddedListener -> {
            FeatureConfig feature = featureConfigService.getFeatureDisplayForFeature(reactedAddedListener.getFeature());
            if(!featureFlagService.isFeatureEnabled(feature, event.getGuild().getIdLong())) {
                return;
            }
            try {
                self.executeIndividualReactionAddedListener(event, cachedMessage, serverUser, reactedAddedListener);
            } catch (Exception e) {
                log.warn(String.format("Failed to execute reaction added listener %s.", reactedAddedListener.getClass().getName()), e);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public void executeIndividualReactionAddedListener(@Nonnull GuildMessageReactionAddEvent event, CachedMessage cachedMessage, ServerUser serverUser, ReactionAddedListener reactedAddedListener) {
        reactedAddedListener.executeReactionAdded(cachedMessage, event, serverUser);
    }

    @PostConstruct
    public void postConstruct() {
        BeanUtils.sortPrioritizedListeners(addedListenerList);
    }

}
