package dev.sheldan.abstracto.assignableroles.command;

import dev.sheldan.abstracto.assignableroles.config.features.AssignableRoleFeature;
import dev.sheldan.abstracto.assignableroles.exceptions.AssignableRoleAlreadyDefinedException;
import dev.sheldan.abstracto.assignableroles.exceptions.AssignableRoleNotUsableException;
import dev.sheldan.abstracto.assignableroles.service.AssignableRolePlaceService;
import dev.sheldan.abstracto.core.command.condition.AbstractConditionableCommand;
import dev.sheldan.abstracto.core.command.config.CommandConfiguration;
import dev.sheldan.abstracto.core.command.config.HelpInfo;
import dev.sheldan.abstracto.core.command.config.Parameter;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.config.FeatureEnum;
import dev.sheldan.abstracto.core.models.FullEmote;
import dev.sheldan.abstracto.core.models.FullRole;
import dev.sheldan.abstracto.core.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AddRoleToAssignableRolePost extends AbstractConditionableCommand {

    @Autowired
    private AssignableRolePlaceService service;

    @Autowired
    private RoleService roleService;

    @Override
    public CompletableFuture<CommandResult> executeAsync(CommandContext commandContext) {
        List<Object> parameters = commandContext.getParameters().getParameters();
        String name = (String) parameters.get(0);
        FullEmote emote = (FullEmote) parameters.get(1);
        String description = (String) parameters.get(2);
        FullRole role = (FullRole) parameters.get(3);
        if(service.hasAssignableRolePlaceEmote(commandContext.getUserInitiatedContext().getServer(), name, emote.getFakeEmote())) {
            throw new AssignableRoleAlreadyDefinedException(emote, name);
        }
        if(!roleService.canBotInteractWithRole(role.getRole())) {
            throw new AssignableRoleNotUsableException(role, commandContext.getGuild());
        }
        return service.addRoleToAssignableRolePlace(commandContext.getUserInitiatedContext().getServer(), name, role.getRole(), emote, description)
                .thenApply(aVoid -> CommandResult.fromSuccess());
    }

    @Override
    public CommandConfiguration getConfiguration() {
        Parameter rolePostName = Parameter.builder().name("name").type(String.class).templated(true).build();
        Parameter emote = Parameter.builder().name("emote").type(FullEmote.class).templated(true).build();
        Parameter description = Parameter.builder().name("description").type(String.class).templated(true).build();
        Parameter role = Parameter.builder().name("role").type(FullRole.class).templated(true).build();
        List<Parameter> parameters = Arrays.asList(rolePostName, emote, description, role);
        HelpInfo helpInfo = HelpInfo.builder().templated(true).build();
        return CommandConfiguration.builder()
                .name("addRoleToAssignableRolePlace")
                .module(AssignableRoleModule.ASSIGNABLE_ROLES)
                .templated(true)
                .causesReaction(true)
                .async(true)
                .supportsEmbedException(true)
                .parameters(parameters)
                .help(helpInfo)
                .build();
    }

    @Override
    public FeatureEnum getFeature() {
        return AssignableRoleFeature.ASSIGNABLE_ROLES;
    }
}
