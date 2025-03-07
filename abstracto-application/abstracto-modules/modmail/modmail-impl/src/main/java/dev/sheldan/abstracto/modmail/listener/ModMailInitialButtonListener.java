package dev.sheldan.abstracto.modmail.listener;

import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.config.ListenerPriority;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListenerResult;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListener;
import dev.sheldan.abstracto.core.models.UndoActionInstance;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListenerModel;
import dev.sheldan.abstracto.core.service.*;
import dev.sheldan.abstracto.core.interaction.ComponentPayloadManagementService;
import dev.sheldan.abstracto.modmail.config.ModMailFeatureDefinition;
import dev.sheldan.abstracto.modmail.model.dto.ServerChoicePayload;
import dev.sheldan.abstracto.modmail.model.dto.ServiceChoicesPayload;
import dev.sheldan.abstracto.modmail.service.ModMailThreadService;
import dev.sheldan.abstracto.modmail.service.ModMailThreadServiceBean;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Component
@Slf4j
public class ModMailInitialButtonListener implements ButtonClickedListener {

    @Autowired
    private GuildService guildService;

    @Autowired
    private ModMailThreadService modMailThreadService;

    @Autowired
    private UndoActionService undoActionService;

    @Autowired
    private ComponentPayloadManagementService componentPayloadService;

    @Autowired
    private ModMailInitialButtonListener self;

    @Autowired
    private ChannelService channelService;

    @Override
    public ButtonClickedListenerResult execute(ButtonClickedListenerModel model) {
        ServiceChoicesPayload choices = (ServiceChoicesPayload) model.getDeserializedPayload();

        ServerChoicePayload chosenServer = choices.getChoices().get(model.getEvent().getComponentId());

        Long userId = choices.getUserId();
        log.debug("Executing action for creationg a modmail thread in server {} for user {}.", chosenServer.getServerId(), userId);
        ArrayList<UndoActionInstance> undoActions = new ArrayList<>();
        Guild guild = guildService.getGuildById(chosenServer.getServerId());
        boolean appeal = chosenServer.getAppealModmail() != null && chosenServer.getAppealModmail();
        channelService.retrieveMessageInChannel(model.getEvent().getChannel(), choices.getMessageId())
                .thenCompose(originalMessage -> {
                    try {
                        return modMailThreadService.createModMailThreadForUser(model.getEvent().getUser(), guild, originalMessage, true, undoActions, appeal);
                    } catch (Exception ex) {
                        log.error("Failed to setup thread correctly", ex);
                        undoActionService.performActions(undoActions);
                        return null;
                    }
                })
                .thenAccept(unused -> self.cleanup(model)).exceptionally(throwable -> {
                    log.warn("Failed to setup modmail thread.");
                    undoActionService.performActions(undoActions);
                    return null;
                });
        return ButtonClickedListenerResult.ACKNOWLEDGED;
    }

    @Transactional
    public void cleanup(ButtonClickedListenerModel model) {
        ServiceChoicesPayload choices = (ServiceChoicesPayload) model.getDeserializedPayload();
        choices.getChoices().keySet().forEach(componentId -> componentPayloadService.deletePayload(componentId));
    }

    @Override
    public Boolean handlesEvent(ButtonClickedListenerModel model) {
        return ModMailThreadServiceBean.MODMAIL_INITIAL_ORIGIN.equals(model.getOrigin()) && !model.getEvent().isFromGuild();
    }

    @Override
    public Integer getPriority() {
        return ListenerPriority.LOW;
    }

    @Override
    public FeatureDefinition getFeature() {
        return ModMailFeatureDefinition.MOD_MAIL;
    }

}
