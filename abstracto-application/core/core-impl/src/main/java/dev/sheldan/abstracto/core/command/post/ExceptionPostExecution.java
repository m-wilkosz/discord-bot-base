package dev.sheldan.abstracto.core.command.post;

import dev.sheldan.abstracto.core.command.Command;
import dev.sheldan.abstracto.core.command.service.PostCommandExecution;
import dev.sheldan.abstracto.core.command.Templatable;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.command.execution.ResultState;
import dev.sheldan.abstracto.templating.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExceptionPostExecution implements PostCommandExecution {

    @Autowired
    private TemplateService templateService;

    @Override
    public void execute(CommandContext commandContext, CommandResult commandResult, Command command) {
        if(commandResult.getResult().equals(ResultState.ERROR)) {
            Throwable throwable = commandResult.getThrowable();
            if(throwable != null) {
                log.error("Exception: ", throwable);
                if(throwable instanceof Templatable) {
                    Templatable exception = (Templatable) throwable;
                    String text = templateService.renderTemplate(exception.getTemplateName(), exception.getTemplateModel());
                    commandContext.getChannel().sendMessage(text).queue();
                } else {
                    if(throwable.getCause() == null) {
                        commandContext.getChannel().sendMessage(throwable.getClass().getSimpleName() + ": " + commandResult.getMessage()).queue();
                    } else {
                        Throwable cause = throwable.getCause();
                        commandContext.getChannel().sendMessage(throwable.getClass().getSimpleName() + ": " + commandResult.getMessage() + ": " + cause.getClass().getSimpleName() + ":" + cause.getMessage()).queue();
                    }
                }
            }
        }
    }
}
