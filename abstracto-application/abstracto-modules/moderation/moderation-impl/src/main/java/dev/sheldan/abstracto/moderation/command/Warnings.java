package dev.sheldan.abstracto.moderation.command;

import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandConfig;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandPrivilegeLevels;
import dev.sheldan.abstracto.core.interaction.slash.parameter.SlashCommandParameterService;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.exception.EntityGuildMismatchException;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.service.PaginatorService;
import dev.sheldan.abstracto.core.service.management.ServerManagementService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.core.utils.FutureUtils;
import dev.sheldan.abstracto.moderation.config.ModerationModuleDefinition;
import dev.sheldan.abstracto.moderation.config.ModerationSlashCommandNames;
import dev.sheldan.abstracto.moderation.config.feature.ModerationFeatureDefinition;
import dev.sheldan.abstracto.moderation.converter.WarnEntryConverter;
import dev.sheldan.abstracto.moderation.model.database.Warning;
import dev.sheldan.abstracto.moderation.model.template.command.WarnEntry;
import dev.sheldan.abstracto.moderation.model.template.command.WarningsModel;
import dev.sheldan.abstracto.moderation.service.management.WarnManagementService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class Warnings extends AbstractConditionableCommand {

    public static final String WARNINGS_RESPONSE_TEMPLATE = "warnings_display_response";
    public static final String NO_WARNINGS_TEMPLATE_KEY = "warnings_no_warnings_found";
    public static final String WARNINGS_COMMAND = "warnings";
    public static final String USER_PARAMETER = "user";
    @Autowired
    private WarnManagementService warnManagementService;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired
    private WarnEntryConverter warnEntryConverter;

    @Autowired
    private PaginatorService paginatorService;

    @Autowired
    private ServerManagementService serverManagementService;

    @Autowired
    private Warnings self;

    @Autowired
    private SlashCommandParameterService slashCommandParameterService;

    @Autowired
    private InteractionService interactionService;

    @Transactional
    public CompletableFuture<Void> renderWarnings(InteractionHook event, List<WarnEntry> warnEntries) {
        WarningsModel model = WarningsModel
                .builder()
                .warnings(warnEntries)
                .build();

        return paginatorService.sendPaginatorToInteraction(WARNINGS_RESPONSE_TEMPLATE, model, event);
    }

    @Override
    public CompletableFuture<CommandResult> executeSlash(SlashCommandInteractionEvent event) {
        return event.deferReply().submit()
            .thenCompose((hook) -> self.loadAndRenderWarnings(event, hook))
            .thenApply(u -> CommandResult.fromSuccess());
    }

    @Transactional
    public CompletableFuture<Void> loadAndRenderWarnings(SlashCommandInteractionEvent event, InteractionHook hook) {
        List<Warning> warnsToDisplay;
        if(slashCommandParameterService.hasCommandOption(USER_PARAMETER, event)) {
            Member member = slashCommandParameterService.getCommandOption(USER_PARAMETER, event, Member.class);
            if(!member.getGuild().equals(event.getGuild())) {
                throw new EntityGuildMismatchException();
            }
            warnsToDisplay = warnManagementService.getAllWarnsForUser(userInServerManagementService.loadOrCreateUser(member));
        } else {
            AServer server = serverManagementService.loadServer(event.getGuild());
            warnsToDisplay = warnManagementService.getAllWarningsOfServer(server);
        }
        if(warnsToDisplay.isEmpty()) {
            return FutureUtils.toSingleFutureGeneric(interactionService.sendMessageToInteraction(NO_WARNINGS_TEMPLATE_KEY, new Object(), hook));
        } else {
            return warnEntryConverter.fromWarnings(warnsToDisplay)
                .thenCompose(warnEntries -> self.renderWarnings(hook, warnEntries));
        }
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<Parameter> parameters = new ArrayList<>();
        Parameter userParameter = Parameter
                .builder()
                .name(USER_PARAMETER)
                .type(Member.class)
                .templated(true)
                .optional(true)
                .build();
        parameters.add(userParameter);

        SlashCommandConfig slashCommandConfig = SlashCommandConfig
                .builder()
                .enabled(true)
                .rootCommandName(ModerationSlashCommandNames.WARNINGS)
                .defaultPrivilege(SlashCommandPrivilegeLevels.ADMIN)
                .commandName("list")
                .build();

        HelpInfo helpInfo = HelpInfo
                .builder()
                .templated(true)
                .build();
        return CommandConfiguration.builder()
                .name(WARNINGS_COMMAND)
                .module(ModerationModuleDefinition.MODERATION)
                .templated(true)
                .async(true)
                .causesReaction(false)
                .slashCommandOnly(true)
                .slashCommandConfig(slashCommandConfig)
                .supportsEmbedException(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureDefinition getFeature() {
        return ModerationFeatureDefinition.WARNING;
    }
}
