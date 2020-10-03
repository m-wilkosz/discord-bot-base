package dev.sheldan.abstracto.core.command.handler;

import dev.sheldan.abstracto.core.models.FullRole;
import dev.sheldan.abstracto.core.models.database.ARole;
import dev.sheldan.abstracto.core.service.RoleService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FullRoleParameterHandlerTest {

    @InjectMocks
    private FullRoleParameterHandler testUnit;

    @Mock
    private RoleParameterHandler roleParameterHandler;

    @Mock
    private RoleService roleService;

    @Mock
    private CommandParameterIterators iterators;

    @Mock
    private Role role;

    @Mock
    private Message message;

    @Mock
    private ARole aRole;

    @Test
    public void testSuccessfulCondition() {
        Assert.assertTrue(testUnit.handles(FullRole.class));
    }

    @Test
    public void testWrongCondition() {
        Assert.assertFalse(testUnit.handles(String.class));
    }

    @Test
    public void testProperEmoteMention() {
        String input = "test";
        when(roleParameterHandler.handle(input, iterators, Role.class, message)).thenReturn(role);
        when(roleService.getFakeRoleFromRole(role)).thenReturn(aRole);
        FullRole parsed = (FullRole) testUnit.handle(input, iterators, FullRole.class, message);
        Assert.assertEquals(aRole, parsed.getRole());
        Assert.assertEquals(role, parsed.getServerRole());
    }


}
