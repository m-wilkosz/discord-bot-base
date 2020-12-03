package dev.sheldan.abstracto.experience.commands;

import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureEnum;
import dev.sheldan.abstracto.core.models.database.ARole;
import dev.sheldan.abstracto.core.service.management.RoleManagementService;
import dev.sheldan.abstracto.experience.config.features.ExperienceFeature;
import dev.sheldan.abstracto.experience.service.ExperienceRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Command used to remove a role from the roles to be awarded at certain levels. If there are users with this role currently, their role
 * will be recalculated, and this will be shown with an status update message.
 */
@Component
public class UnSetExpRole extends AbstractConditionableCommand {

    @Autowired
    private ExperienceRoleService experienceRoleService;

    @Autowired
    private RoleManagementService roleManagementService;

    @Override
    public CompletableFuture<CommandResult> executeAsync(CommandContext commandContext) {
        ARole role = (ARole) commandContext.getParameters().getParameters().get(0);
        ARole actualRole = roleManagementService.findRole(role.getId());
        // do not check for the existence of the role, because if the role was deleted, users should be able
        // to get rid of it in the configuration
        return experienceRoleService.unsetRole(actualRole, commandContext.getUserInitiatedContext().getChannel())
                .thenApply(aVoid -> CommandResult.fromSuccess());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.builder().name("role").templated(true).type(ARole.class).build());
        HelpInfo helpInfo = HelpInfo.builder().templated(true).build();
        return CommandConfiguration.builder()
                .name("unSetExpRole")
                .module(ExperienceModule.EXPERIENCE)
                .templated(true)
                .async(true)
                .causesReaction(true)
                .supportsEmbedException(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureEnum getFeature() {
        return ExperienceFeature.EXPERIENCE;
    }
}
