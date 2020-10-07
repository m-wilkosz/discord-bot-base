package dev.sheldan.abstracto.moderation.commands;

import dev.sheldan.abstracto.core.command.exception.IncorrectParameterTypeException;
import dev.sheldan.abstracto.core.command.exception.InsufficientParametersException;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.command.execution.ResultState;
import dev.sheldan.abstracto.moderation.service.management.UserNoteManagementService;
import dev.sheldan.abstracto.templating.service.TemplateService;
import dev.sheldan.abstracto.test.command.CommandConfigValidator;
import dev.sheldan.abstracto.test.command.CommandTestUtilities;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeleteNoteTest {

    @InjectMocks
    private DeleteNote testUnit;

    @Mock
    private UserNoteManagementService userNoteManagementService;

    @Mock
    private TemplateService templateService;

    private static final Long NOTE_ID = 5L;

    @Test
    public void testDeleteExistingNote() {
        CommandContext parameters = CommandTestUtilities.getWithParameters(Arrays.asList(NOTE_ID));
        when(userNoteManagementService.noteExists(NOTE_ID, parameters.getUserInitiatedContext().getServer())).thenReturn(true);
        CommandResult result = testUnit.execute(parameters);
        CommandTestUtilities.checkSuccessfulCompletion(result);
        verify(userNoteManagementService, times(1)).deleteNote(NOTE_ID, parameters.getUserInitiatedContext().getServer());
    }

    @Test
    public void testDeleteNotExistingNote() {
        CommandContext parameters = CommandTestUtilities.getWithParameters(Arrays.asList(NOTE_ID));
        when(userNoteManagementService.noteExists(NOTE_ID, parameters.getUserInitiatedContext().getServer())).thenReturn(false);
        when(templateService.renderSimpleTemplate(DeleteNote.NOTE_NOT_FOUND_EXCEPTION_TEMPLATE)).thenReturn("error");
        CommandResult result = testUnit.execute(parameters);
        Assert.assertEquals(ResultState.ERROR, result.getResult());
        Assert.assertNotNull(result.getMessage());
    }

    @Test(expected = InsufficientParametersException.class)
    public void testTooLittleParameters() {
        CommandTestUtilities.executeNoParametersTest(testUnit);
    }

    @Test(expected = IncorrectParameterTypeException.class)
    public void testIncorrectParameterType() {
        CommandTestUtilities.executeWrongParametersTest(testUnit);
    }

    @Test
    public void validateCommand() {
        CommandConfigValidator.validateCommandConfiguration(testUnit.getConfiguration());
    }

}
