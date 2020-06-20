package dev.sheldan.abstracto.moderation.commands;

import dev.sheldan.abstracto.core.command.exception.IncorrectParameter;
import dev.sheldan.abstracto.core.command.exception.InsufficientParameters;
import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.moderation.models.database.Warning;
import dev.sheldan.abstracto.moderation.service.management.WarnManagementService;
import dev.sheldan.abstracto.test.MockUtils;
import dev.sheldan.abstracto.test.command.CommandConfigValidator;
import dev.sheldan.abstracto.test.command.CommandTestUtilities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeleteWarningTest {
    @InjectMocks
    private DeleteWarning testUnit;

    @Mock
    private WarnManagementService warnManagementService;

    private static final Long WARN_ID = 5L;

    @Test
    public void testDeleteExistingWarning() {
        AServer server = MockUtils.getServer();
        AUserInAServer warnedUser = MockUtils.getUserObject(5L, server);
        AUserInAServer warningUser = MockUtils.getUserObject(6L, server);
        Warning existingWarning = Warning.builder().warnedUser(warnedUser).warningUser(warningUser).build();
        when(warnManagementService.findById(WARN_ID)).thenReturn(Optional.of(existingWarning));
        CommandContext parameters = CommandTestUtilities.getWithParameters(Arrays.asList(WARN_ID));
        CommandResult result = testUnit.execute(parameters);
        CommandTestUtilities.checkSuccessfulCompletion(result);
    }

    @Test
    public void testDeleteNotExistingWarning() {
        when(warnManagementService.findById(WARN_ID)).thenReturn(Optional.empty());
        CommandContext parameters = CommandTestUtilities.getWithParameters(Arrays.asList(WARN_ID));
        CommandResult result = testUnit.execute(parameters);
        CommandTestUtilities.checkSuccessfulCompletion(result);
    }


    @Test(expected = InsufficientParameters.class)
    public void testTooLittleParameters() {
        CommandTestUtilities.executeNoParametersTest(testUnit);
    }

    @Test(expected = IncorrectParameter.class)
    public void testIncorrectParameterType() {
        CommandTestUtilities.executeWrongParametersTest(testUnit);
    }

    @Test
    public void validateCommand() {
        CommandConfigValidator.validateCommandConfiguration(testUnit.getConfiguration());
    }
}
