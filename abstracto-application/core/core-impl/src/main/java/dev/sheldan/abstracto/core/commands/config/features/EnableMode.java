package dev.sheldan.abstracto.core.commands.config.features;

import dev.sheldan.abstracto.core.interaction.slash.CoreSlashCommandNames;
import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandConfig;
import dev.sheldan.abstracto.core.command.config.features.CoreFeatureDefinition;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.commands.config.ConfigModuleDefinition;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.config.FeatureMode;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandPrivilegeLevels;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.service.FeatureConfigService;
import dev.sheldan.abstracto.core.service.FeatureModeService;
import dev.sheldan.abstracto.core.interaction.slash.parameter.SlashCommandParameterService;
import dev.sheldan.abstracto.core.service.management.ServerManagementService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class EnableMode extends AbstractConditionableCommand {

    @Autowired
    private FeatureConfigService featureConfigService;

    @Autowired
    private FeatureModeService featureModeService;

    @Autowired
    private SlashCommandParameterService slashCommandParameterService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ServerManagementService serverManagementService;

    private static final String ENABLE_MODE_RESPONSE_KEY = "enableMode_response";
    private static final String FEATURE_PARAMETER = "feature";
    private static final String MODE_PARAMETER = "mode";
    private static final String ENABLE_MODE_COMMAND = "enableMode";

    @Override
    public CompletableFuture<CommandResult> executeSlash(SlashCommandInteractionEvent event) {
        String featureName = slashCommandParameterService.getCommandOption(FEATURE_PARAMETER, event, String.class);
        String modeName = slashCommandParameterService.getCommandOption(MODE_PARAMETER, event, String.class);
        FeatureDefinition featureDefinition = featureConfigService.getFeatureEnum(featureName);
        FeatureMode featureMode = featureModeService.getFeatureModeForKey(featureName, modeName);
        AServer server = serverManagementService.loadServer(event.getGuild().getIdLong());
        featureModeService.enableFeatureModeForFeature(featureDefinition, server, featureMode);
        return interactionService.replyEmbed(ENABLE_MODE_RESPONSE_KEY, event)
                .thenApply(interactionHook -> CommandResult.fromSuccess());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        Parameter featureName = Parameter
                .builder()
                .name(FEATURE_PARAMETER)
                .type(String.class)
                .templated(true).build();
        Parameter mode = Parameter
                .builder().name(MODE_PARAMETER)
                .type(String.class)
                .templated(true)
                .build();

        List<Parameter> parameters = Arrays.asList(featureName, mode);

        HelpInfo helpInfo = HelpInfo
                .builder()
                .templated(true)
                .build();

        SlashCommandConfig slashCommandConfig = SlashCommandConfig
                .builder()
                .enabled(true)
                .defaultPrivilege(SlashCommandPrivilegeLevels.INVITER)
                .rootCommandName(CoreSlashCommandNames.FEATURE)
                .commandName(ENABLE_MODE_COMMAND)
                .build();

        return CommandConfiguration.builder()
                .name(ENABLE_MODE_COMMAND)
                .module(ConfigModuleDefinition.CONFIG)
                .parameters(parameters)
                .slashCommandOnly(true)
                .templated(true)
                .slashCommandConfig(slashCommandConfig)
                .supportsEmbedException(true)
                .help(helpInfo)
                .causesReaction(true)
                .build();
    }

    @Override
    public FeatureDefinition getFeature() {
        return CoreFeatureDefinition.CORE_FEATURE;
    }
}
