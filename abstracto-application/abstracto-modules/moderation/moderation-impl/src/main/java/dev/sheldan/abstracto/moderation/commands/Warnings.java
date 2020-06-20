package dev.sheldan.abstracto.moderation.commands;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.menu.Paginator;
import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.command.execution.ContextConverter;
import dev.sheldan.abstracto.core.config.FeatureEnum;
import dev.sheldan.abstracto.core.service.PaginatorService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.moderation.config.ModerationModule;
import dev.sheldan.abstracto.moderation.config.features.ModerationFeatures;
import dev.sheldan.abstracto.moderation.converter.WarnEntryConverter;
import dev.sheldan.abstracto.moderation.models.database.Warning;
import dev.sheldan.abstracto.moderation.models.template.commands.WarnEntry;
import dev.sheldan.abstracto.moderation.models.template.commands.WarningsModel;
import dev.sheldan.abstracto.moderation.service.management.WarnManagementService;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Warnings extends AbstractConditionableCommand {

    public static final String WARNINGS_RESPONSE_TEMPLATE = "warnings_response";
    @Autowired
    private WarnManagementService warnManagementService;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired
    private WarnEntryConverter warnEntryConverter;

    @Autowired
    private PaginatorService paginatorService;

    @Autowired
    private EventWaiter eventWaiter;

    @Override
    public CommandResult execute(CommandContext commandContext) {
        checkParameters(commandContext);
        List<Warning> warnsToDisplay;
        if(!commandContext.getParameters().getParameters().isEmpty()) {
            Member member = (Member) commandContext.getParameters().getParameters().get(0);
            warnsToDisplay = warnManagementService.getAllWarnsForUser(userInServerManagementService.loadUser(member));
        } else {
            warnsToDisplay = warnManagementService.getAllWarningsOfServer(commandContext.getUserInitiatedContext().getServer());
        }
        List<WarnEntry> warnEntries = warnEntryConverter.fromWarnings(warnsToDisplay);

        WarningsModel model = (WarningsModel) ContextConverter.fromCommandContext(commandContext, WarningsModel.class);
        model.setWarnings(warnEntries);

        Paginator paginator = paginatorService.createPaginatorFromTemplate(WARNINGS_RESPONSE_TEMPLATE, model, eventWaiter);
        paginator.display(commandContext.getChannel());
        return CommandResult.fromSuccess();
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("user").type(Member.class).templated(true).optional(true).build());
        HelpInfo helpInfo = HelpInfo.builder().templated(true).build();
        return CommandConfiguration.builder()
                .name("warnings")
                .module(ModerationModule.MODERATION)
                .templated(true)
                .causesReaction(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureEnum getFeature() {
        return ModerationFeatures.WARNING;
    }
}
