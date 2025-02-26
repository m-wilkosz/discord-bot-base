package dev.sheldan.abstracto.core.service.paginator;

import dev.sheldan.abstracto.core.command.config.features.CoreFeatureDefinition;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.config.ListenerPriority;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListenerResult;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListener;
import dev.sheldan.abstracto.core.interaction.button.listener.ButtonClickedListenerModel;
import dev.sheldan.abstracto.core.service.MessageService;
import dev.sheldan.abstracto.core.templating.model.MessageConfiguration;
import dev.sheldan.abstracto.core.templating.model.MessageToSend;
import dev.sheldan.abstracto.core.templating.service.TemplateServiceBean;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@Slf4j
public class PaginatorButtonListener implements ButtonClickedListener {

    @Autowired
    private PaginatorServiceBean paginatorServiceBean;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TemplateServiceBean templateServiceBean;

    @Autowired
    private InteractionService interactionService;

    @Override
    public ButtonClickedListenerResult execute(ButtonClickedListenerModel model) {
        PaginatorButtonPayload payload = (PaginatorButtonPayload) model.getDeserializedPayload();

        Message originalMessage = model.getEvent().getMessage();
        if(payload.getAllowedUser() != null && model.getEvent().getUser().getIdLong() != payload.getAllowedUser()) {
            return ButtonClickedListenerResult.IGNORED;
        }
        String buttonId = model.getEvent().getComponentId();
        if(buttonId.equals(payload.getExitButtonId())) {
            log.info("Deleting paginator {} because of exit button {}.", payload.getPaginatorId(), buttonId);
            originalMessage.delete().queue();
            paginatorServiceBean.cleanupPaginatorPayloads(payload);
            return ButtonClickedListenerResult.ACKNOWLEDGED;
        }
        if(payload.getSinglePage()) {
            return ButtonClickedListenerResult.IGNORED;
        }
        int targetPage;
        if(buttonId.equals(payload.getStartButtonId())) {
            targetPage = 0;
        } else if(buttonId.equals(payload.getPreviousButtonId())) {
            targetPage = Math.max(paginatorServiceBean.getCurrentPage(payload.getPaginatorId()) - 1, 0);
        } else if(buttonId.equals(payload.getNextButtonId())) {
            targetPage = Math.min(paginatorServiceBean.getCurrentPage(payload.getPaginatorId()) + 1, payload.getEmbedConfigs().size() - 1);
        } else if(buttonId.equals(payload.getLastButtonId())) {
            targetPage = payload.getEmbedConfigs().size() - 1;
        } else {
            return ButtonClickedListenerResult.IGNORED;
        }
        log.debug("Moving to page {} in paginator {}.", targetPage, payload.getPaginatorId());
        MessageConfiguration messageConfiguration = payload.getEmbedConfigs().get(targetPage);
        MessageToSend messageToSend = templateServiceBean.convertEmbedConfigurationToMessageToSend(messageConfiguration);
        interactionService.replaceOriginal(messageToSend, model.getEvent().getHook())
                .thenAccept(unused -> log.info("Updated paginator {} to switch to page {}.", payload.getPaginatorId(), targetPage))
                .exceptionally(throwable -> {
                    log.warn("Failed to update the paginator message.", throwable);
                    return null;
                });
        String accessorId = UUID.randomUUID().toString();
        paginatorServiceBean.updateCurrentPage(payload.getPaginatorId(), targetPage, accessorId);
        paginatorServiceBean.schedulePaginationDeletion(payload.getPaginatorId(), accessorId);
        return ButtonClickedListenerResult.ACKNOWLEDGED;
    }

    @Override
    public Integer getPriority() {
        return ListenerPriority.HIGH;
    }

    @Override
    public Boolean handlesEvent(ButtonClickedListenerModel model) {
        return PaginatorServiceBean.PAGINATOR_BUTTON.equals(model.getOrigin());
    }

    @Override
    public FeatureDefinition getFeature() {
        return CoreFeatureDefinition.CORE_FEATURE;
    }
}
