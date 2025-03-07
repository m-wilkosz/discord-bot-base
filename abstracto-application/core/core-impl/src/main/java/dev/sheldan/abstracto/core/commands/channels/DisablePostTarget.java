package dev.sheldan.abstracto.core.commands.channels;

import dev.sheldan.abstracto.core.interaction.slash.CoreSlashCommandNames;
import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandConfig;
import dev.sheldan.abstracto.core.command.config.features.CoreFeatureDefinition;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.exception.PostTargetNotValidException;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandPrivilegeLevels;
import dev.sheldan.abstracto.core.service.PostTargetService;
import dev.sheldan.abstracto.core.interaction.slash.parameter.SlashCommandParameterService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class DisablePostTarget extends AbstractConditionableCommand {

    private static final String DISABLE_POSTTARGET_COMMAND = "disablePosttarget";
    private static final String NAME_PARAMETER = "name";
    private static final String DISABLE_POST_TARGET_RESPONSE = "disablePosttarget_response";

    @Autowired
    private PostTargetService postTargetService;

    @Autowired
    private SlashCommandParameterService slashCommandParameterService;

    @Autowired
    private InteractionService interactionService;

    @Override
    public CompletableFuture<CommandResult> executeSlash(SlashCommandInteractionEvent event) {
        String postTargetName = slashCommandParameterService.getCommandOption(NAME_PARAMETER, event, String.class);
        if(!postTargetService.validPostTarget(postTargetName)) {
            throw new PostTargetNotValidException(postTargetName, postTargetService.getAvailablePostTargets());
        }
        postTargetService.disablePostTarget(postTargetName, event.getGuild().getIdLong());
        return interactionService.replyEmbed(DISABLE_POST_TARGET_RESPONSE, event)
                .thenApply(interactionHook -> CommandResult.fromSuccess());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        Parameter postTargetName = Parameter
                .builder()
                .name(NAME_PARAMETER)
                .type(String.class)
                .templated(true)
                .build();
        List<Parameter> parameters = Arrays.asList(postTargetName);
        HelpInfo helpInfo = HelpInfo
                .builder()
                .templated(true)
                .build();

        SlashCommandConfig slashCommandConfig = SlashCommandConfig
                .builder()
                .enabled(true)
                .rootCommandName(CoreSlashCommandNames.POST_TARGET)
                .defaultPrivilege(SlashCommandPrivilegeLevels.INVITER)
                .commandName("disable")
                .build();

        return CommandConfiguration.builder()
                .name(DISABLE_POSTTARGET_COMMAND)
                .module(ChannelsModuleDefinition.CHANNELS)
                .parameters(parameters)
                .slashCommandOnly(true)
                .slashCommandConfig(slashCommandConfig)
                .supportsEmbedException(true)
                .help(helpInfo)
                .templated(true)
                .causesReaction(true)
                .build();
    }

    @Override
    public FeatureDefinition getFeature() {
        return CoreFeatureDefinition.CORE_FEATURE;
    }
}
