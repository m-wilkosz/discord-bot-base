package dev.sheldan.abstracto.modmail.command.condition;

import dev.sheldan.abstracto.core.command.Command;
import dev.sheldan.abstracto.core.command.condition.ConditionResult;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.service.management.ChannelManagementService;
import dev.sheldan.abstracto.core.utils.ContextUtils;
import dev.sheldan.abstracto.modmail.condition.ModMailContextCondition;
import dev.sheldan.abstracto.modmail.condition.detail.NotInModMailThreadConditionDetail;
import dev.sheldan.abstracto.modmail.model.database.ModMailThread;
import dev.sheldan.abstracto.modmail.service.management.ModMailThreadManagementService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * This {@link dev.sheldan.abstracto.core.command.condition.CommandCondition} checks the channel it is executed in
 * and checks if the channel is a valid and open mod mail thread.
 */
@Component
public class RequiresModMailCondition implements ModMailContextCondition {

    @Autowired
    private ModMailThreadManagementService modMailThreadManagementService;

    @Autowired
    private ChannelManagementService channelManagementService;

    @Override
    public ConditionResult shouldExecute(CommandContext commandContext, Command command) {
        Optional<ModMailThread> threadOptional = modMailThreadManagementService.getByChannelOptional(channelManagementService.loadChannel(commandContext.getChannel()));
        if(threadOptional.isPresent()) {
            return ConditionResult
                    .builder()
                    .result(true)
                    .build();
        }
        return ConditionResult
                .builder()
                .result(false)
                .conditionDetail(new NotInModMailThreadConditionDetail())
                .build();
    }

    @Override
    public ConditionResult shouldExecute(SlashCommandInteractionEvent slashCommandInteractionEvent, Command command) {
        if(ContextUtils.isUserCommand(slashCommandInteractionEvent)) {
            return ConditionResult.SUCCESS;
        }
        Optional<ModMailThread> threadOptional = modMailThreadManagementService.getByChannelOptional(channelManagementService.loadChannel(slashCommandInteractionEvent.getChannel()));
        if(threadOptional.isPresent()) {
            return ConditionResult
                    .builder()
                    .result(true)
                    .build();
        }
        return ConditionResult
                .builder()
                .result(false)
                .conditionDetail(new NotInModMailThreadConditionDetail())
                .build();
    }

    @Override
    public boolean supportsSlashCommands() {
        return true;
    }
}
