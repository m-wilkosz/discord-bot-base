package dev.sheldan.abstracto.statistic.emotes.command;

import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.exception.IncorrectParameterException;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureEnum;
import dev.sheldan.abstracto.core.service.EmoteService;
import dev.sheldan.abstracto.core.service.FeatureModeService;
import dev.sheldan.abstracto.statistic.config.StatisticFeatures;
import dev.sheldan.abstracto.statistic.emotes.command.parameter.TrackEmoteParameter;
import dev.sheldan.abstracto.statistic.emotes.config.EmoteTrackingMode;
import dev.sheldan.abstracto.statistic.emotes.config.EmoteTrackingModule;
import dev.sheldan.abstracto.statistic.emotes.model.database.TrackedEmote;
import dev.sheldan.abstracto.statistic.emotes.service.TrackedEmoteService;
import dev.sheldan.abstracto.statistic.emotes.service.management.TrackedEmoteManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * This command can be used to track one individual {@link TrackedEmote} newly, or set the emote to be tracked again.
 * This can either be done via providing the {@link net.dv8tion.jda.api.entities.Emote} or via ID.
 */
@Component
public class TrackEmote extends AbstractConditionableCommand {

    @Autowired
    private TrackedEmoteService trackedEmoteService;

    @Autowired
    private TrackedEmoteManagementService trackedEmoteManagementService;

    @Autowired
    private EmoteService emoteService;

    @Autowired
    private FeatureModeService featureModeService;

    @Override
    public CommandResult execute(CommandContext commandContext) {
        checkParameters(commandContext);
        TrackEmoteParameter emoteToTrack = (TrackEmoteParameter) commandContext.getParameters().getParameters().get(0);
        Long emoteId = emoteToTrack.getTrackedEmote().getTrackedEmoteId().getId();
        long serverId = commandContext.getGuild().getIdLong();
        // if its already a tracked emote, just set the tracking_enabled flag to true
        if(trackedEmoteManagementService.trackedEmoteExists(emoteId, serverId)) {
            TrackedEmote trackedemote = trackedEmoteManagementService.loadByEmoteId(emoteId, serverId);
            trackedEmoteManagementService.enableTrackedEmote(trackedemote);
        } else if(emoteToTrack.getEmote() != null) {
            // if its a new emote, lets see if its external
            boolean external = !emoteService.emoteIsFromGuild(emoteToTrack.getEmote(), commandContext.getGuild());
            if(external) {
                // this throws an exception if the feature mode is not enabled
                featureModeService.validateActiveFeatureMode(serverId, StatisticFeatures.EMOTE_TRACKING, EmoteTrackingMode.EXTERNAL_EMOTES);
            }
            trackedEmoteService.createTrackedEmote(emoteToTrack.getEmote(), commandContext.getGuild(), external);
        } else {
            // in case the ID was not an existing TrackedEmote, and no Emote was given, we need to fail
            throw new IncorrectParameterException(this, getConfiguration().getParameters().get(0).getName());
        }
        return CommandResult.fromSuccess();
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("emote").templated(true).type(TrackEmoteParameter.class).build());
        HelpInfo helpInfo = HelpInfo.builder().templated(true).build();
        return CommandConfiguration.builder()
                .name("trackEmote")
                .module(EmoteTrackingModule.EMOTE_TRACKING)
                .templated(true)
                .supportsEmbedException(true)
                .causesReaction(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureEnum getFeature() {
        return StatisticFeatures.EMOTE_TRACKING;
    }
}
