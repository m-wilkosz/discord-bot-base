package dev.sheldan.abstracto.customcommand.model.command;

import dev.sheldan.abstracto.core.models.template.display.MemberDisplay;
import dev.sheldan.abstracto.customcommand.model.database.CustomCommand;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ListCustomCommandModel {
    private String name;
    private String content;
    private MemberDisplay creator;

    public static ListCustomCommandModel fromCustomCommand(CustomCommand customCommand) {
        MemberDisplay creatorObj;
        if(customCommand.getUserSpecific()) {
            creatorObj = MemberDisplay.fromAUser(customCommand.getCreatorUser());
        } else {
            creatorObj = MemberDisplay.fromAUserInAServer(customCommand.getCreator());
        }

        return ListCustomCommandModel
                .builder()
                .name(customCommand.getName())
                .content(customCommand.getAdditionalMessage())
                .creator(creatorObj)
                .build();
    }
}
