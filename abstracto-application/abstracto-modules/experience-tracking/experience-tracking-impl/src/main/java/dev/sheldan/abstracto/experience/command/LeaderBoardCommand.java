package dev.sheldan.abstracto.experience.command;

import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.config.ParameterValidator;
import dev.sheldan.abstracto.core.command.config.validator.MinIntegerValueValidator;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureDefinition;
import dev.sheldan.abstracto.core.interaction.InteractionService;
import dev.sheldan.abstracto.core.interaction.slash.SlashCommandConfig;
import dev.sheldan.abstracto.core.interaction.slash.parameter.SlashCommandParameterService;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.service.ChannelService;
import dev.sheldan.abstracto.core.service.management.ServerManagementService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.core.templating.model.MessageToSend;
import dev.sheldan.abstracto.core.templating.service.TemplateService;
import dev.sheldan.abstracto.core.utils.FutureUtils;
import dev.sheldan.abstracto.experience.config.ExperienceFeatureDefinition;
import dev.sheldan.abstracto.experience.config.ExperienceSlashCommandNames;
import dev.sheldan.abstracto.experience.converter.LeaderBoardModelConverter;
import dev.sheldan.abstracto.experience.model.LeaderBoard;
import dev.sheldan.abstracto.experience.model.LeaderBoardEntry;
import dev.sheldan.abstracto.experience.model.template.LeaderBoardEntryModel;
import dev.sheldan.abstracto.experience.model.template.LeaderBoardModel;
import dev.sheldan.abstracto.experience.service.AUserExperienceService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shows the experience gain information of the top 10 users in the server, or if a page number is provided as a parameter, only the members which are on this page.
 */
@Component
@Slf4j
public class LeaderBoardCommand extends AbstractConditionableCommand {

    public static final String LEADER_BOARD_POST_EMBED_TEMPLATE = "leaderboard_post";
    private static final String LEDERBOARD_COMMAND_NAME = "leaderboard";
    private static final String PAGE_PARAMETER = "page";
    private static final String FOCUS_PARAMETER = "focus";
    @Autowired
    private AUserExperienceService userExperienceService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private LeaderBoardModelConverter converter;

    @Autowired
    private ServerManagementService serverManagementService;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired
    private SlashCommandParameterService slashCommandParameterService;

    @Autowired
    private InteractionService interactionService;

    @Value("${abstracto.experience.leaderboard.externalUrl}")
    private String leaderboardExternalURL;

    @Override
    public CompletableFuture<CommandResult> executeAsync(CommandContext commandContext) {
        List<Object> parameters = commandContext.getParameters().getParameters();
        // parameter is optional, in case its not present, we default to the 0th page
        Integer page = !parameters.isEmpty() ? (Integer) parameters.get(0) : 1;
        return getMessageToSend(commandContext.getAuthor(), page, false)
            .thenCompose(messageToSend -> FutureUtils.toSingleFutureGeneric(channelService.sendMessageToSendToChannel(messageToSend, commandContext.getChannel())))
            .thenApply(aVoid -> CommandResult.fromIgnored());
    }

    private CompletableFuture<MessageToSend> getMessageToSend(Member actorUser, Integer page, boolean focusMe) {
        AServer server = serverManagementService.loadServer(actorUser.getGuild());
        LeaderBoard leaderBoard;
        AUserInAServer aUserInAServer = userInServerManagementService.loadOrCreateUser(actorUser);
        if (focusMe) {
            leaderBoard = userExperienceService.findLeaderBoardDataForUserFocus(aUserInAServer);
        } else {
            leaderBoard = userExperienceService.findLeaderBoardData(server, page);
        }
        List<CompletableFuture> futures = new ArrayList<>();
        CompletableFuture<List<LeaderBoardEntryModel>> completableFutures = converter.fromLeaderBoard(leaderBoard, actorUser.getGuild().getIdLong());
        futures.add(completableFutures);
        log.info("Rendering leaderboard for page {} in server {} for user {}.", page, actorUser.getId(), actorUser.getGuild().getId());
        LeaderBoardEntry userRank = userExperienceService.getRankOfUserInServer(aUserInAServer);
        CompletableFuture<List<LeaderBoardEntryModel>> userRankFuture = converter.fromLeaderBoardEntry(Arrays.asList(userRank), actorUser.getGuild().getIdLong());
        futures.add(userRankFuture);
        String leaderboardUrl;
        if(!StringUtils.isBlank(leaderboardExternalURL)) {
            leaderboardUrl = String.format("%s/experience/leaderboards/%s", leaderboardExternalURL, actorUser.getGuild().getIdLong());
        } else {
            leaderboardUrl = null;
        }
        return FutureUtils.toSingleFuture(futures).thenCompose(aVoid -> {
            List<LeaderBoardEntryModel> finalModels = completableFutures.join();
            LeaderBoardModel leaderBoardModel = LeaderBoardModel
                    .builder()
                    .userExperiences(finalModels)
                    .leaderboardUrl(leaderboardUrl)
                    .showPlacement(!focusMe)
                    .userExecuting(userRankFuture.join().get(0))
                    .build();
            return CompletableFuture.completedFuture(templateService.renderEmbedTemplate(LEADER_BOARD_POST_EMBED_TEMPLATE, leaderBoardModel, actorUser.getGuild().getIdLong()));

        });
    }

    @Override
    public CompletableFuture<CommandResult> executeSlash(SlashCommandInteractionEvent event) {
        Integer page;
        boolean focusMe;
        if (slashCommandParameterService.hasCommandOption(FOCUS_PARAMETER, event)) {
            focusMe = slashCommandParameterService.getCommandOption(FOCUS_PARAMETER, event, Boolean.class);
        } else {
            focusMe = false;
        }
        if(slashCommandParameterService.hasCommandOption(PAGE_PARAMETER, event)) {
            page = slashCommandParameterService.getCommandOption(PAGE_PARAMETER, event, Integer.class);
        } else {
            page = 1;
        }
        return getMessageToSend(event.getMember(), page, focusMe)
                .thenCompose(messageToSend -> interactionService.replyMessageToSend(messageToSend, event))
                .thenApply(aVoid -> CommandResult.fromIgnored());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<ParameterValidator> leaderBoardPageValidators = Arrays.asList(MinIntegerValueValidator.min(0L));
        Parameter pageParameter = Parameter
                .builder()
                .name(PAGE_PARAMETER)
                .validators(leaderBoardPageValidators)
                .optional(true)
                .templated(true)
                .type(Integer.class)
                .build();

        Parameter focusMe = Parameter
            .builder()
            .name(FOCUS_PARAMETER)
            .validators(leaderBoardPageValidators)
            .optional(true)
            .slashCommandOnly(true)
            .templated(true)
            .type(Boolean.class)
            .build();
        List<Parameter> parameters = Arrays.asList(pageParameter, focusMe);
        HelpInfo helpInfo = HelpInfo
                .builder()
                .templated(true)
                .build();

        SlashCommandConfig slashCommandConfig = SlashCommandConfig
                .builder()
                .enabled(true)
                .rootCommandName(ExperienceSlashCommandNames.EXPERIENCE)
                .commandName(LEDERBOARD_COMMAND_NAME)
                .build();


        return CommandConfiguration.builder()
                .name(LEDERBOARD_COMMAND_NAME)
                .module(ExperienceModuleDefinition.EXPERIENCE)
                .templated(true)
                .async(true)
                .slashCommandConfig(slashCommandConfig)
                .supportsEmbedException(true)
                .causesReaction(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureDefinition getFeature() {
        return ExperienceFeatureDefinition.EXPERIENCE;
    }
}
