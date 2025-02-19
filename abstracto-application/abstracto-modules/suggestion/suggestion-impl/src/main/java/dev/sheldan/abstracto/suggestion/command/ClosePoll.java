package dev.sheldan.abstracto.suggestion.command;

import dev.sheldan.abstracto.core.command.UtilityModuleDefinition;
import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.config.ParameterValidator;
import dev.sheldan.abstracto.core.command.config.validator.MinIntegerValueValidator;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandConfig;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandPrivilegeLevels;
import dev.sheldan.abstracto.core.interaction.slash.parameter.SlashCommandParameterService;
import dev.sheldan.abstracto.suggestion.config.SuggestionFeatureDefinition;
import dev.sheldan.abstracto.suggestion.config.SuggestionSlashCommandNames;
import dev.sheldan.abstracto.suggestion.service.PollService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class ClosePoll extends AbstractConditionableCommand {

    private static final String CLOSE_POLL_COMMAND = "closePoll";
    private static final String POLL_ID_PARAMETER = "pollId";
    private static final String TEXT_PARAMETER = "text";
    private static final String CLOSE_POLL_RESPONSE = "closePoll_response";

    @Autowired
    private SlashCommandParameterService slashCommandParameterService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private PollService pollService;

    @Override
    public CompletableFuture<CommandResult> executeSlash(SlashCommandInteractionEvent event) {
        Long pollId = slashCommandParameterService.getCommandOption(POLL_ID_PARAMETER, event, Integer.class).longValue();
        String text;
        if(slashCommandParameterService.hasCommandOption(TEXT_PARAMETER, event)) {
            text = slashCommandParameterService.getCommandOption(TEXT_PARAMETER, event, String.class);
        } else {
            text = "";
        }
        return pollService.closePoll(pollId, event.getGuild().getIdLong(), text, event.getMember())
                .thenCompose(unused -> interactionService.replyEmbed(CLOSE_POLL_RESPONSE, event))
                .thenApply(aVoid ->  CommandResult.fromSuccess());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<ParameterValidator> pollIdValidator = Arrays.asList(MinIntegerValueValidator.min(1L));
        Parameter pollIdParameter = Parameter
                .builder()
                .name(POLL_ID_PARAMETER)
                .validators(pollIdValidator)
                .type(Long.class)
                .templated(true)
                .build();

        Parameter textParameter = Parameter
                .builder()
                .name(TEXT_PARAMETER)
                .type(String.class)
                .optional(true)
                .remainder(true)
                .templated(true)
                .build();

        List<Parameter> parameters = Arrays.asList(pollIdParameter, textParameter);
        HelpInfo helpInfo = HelpInfo
                .builder()
                .templated(true)
                .build();

        SlashCommandConfig slashCommandConfig = SlashCommandConfig
                .builder()
                .enabled(true)
                .defaultPrivilege(SlashCommandPrivilegeLevels.INVITER)
                .rootCommandName(SuggestionSlashCommandNames.POLL)
                .commandName("close")
                .build();

        return CommandConfiguration.builder()
                .name(CLOSE_POLL_COMMAND)
                .slashCommandOnly(true)
                .module(UtilityModuleDefinition.UTILITY)
                .templated(true)
                .slashCommandConfig(slashCommandConfig)
                .async(true)
                .supportsEmbedException(true)
                .causesReaction(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureDefinition getFeature() {
        return SuggestionFeatureDefinition.POLL;
    }
}
