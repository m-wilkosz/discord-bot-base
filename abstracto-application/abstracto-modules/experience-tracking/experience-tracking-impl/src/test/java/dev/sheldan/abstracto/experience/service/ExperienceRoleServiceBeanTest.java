package dev.sheldan.abstracto.experience.service;

import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AChannelType;
import dev.sheldan.abstracto.core.models.database.ARole;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.service.management.RoleManagementService;
import dev.sheldan.abstracto.core.utils.CompletableFutureList;
import dev.sheldan.abstracto.experience.ExperienceRelatedTest;
import dev.sheldan.abstracto.experience.models.RoleCalculationResult;
import dev.sheldan.abstracto.experience.models.database.AExperienceLevel;
import dev.sheldan.abstracto.experience.models.database.AExperienceRole;
import dev.sheldan.abstracto.experience.models.database.AUserExperience;
import dev.sheldan.abstracto.experience.service.management.ExperienceLevelManagementService;
import dev.sheldan.abstracto.experience.service.management.ExperienceRoleManagementService;
import dev.sheldan.abstracto.test.MockUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExperienceRoleServiceBeanTest extends ExperienceRelatedTest {

    @InjectMocks
    private ExperienceRoleServiceBean testingUnit;

    @Mock
    private ExperienceRoleManagementService experienceRoleManagementService;

    @Mock
    private ExperienceLevelManagementService experienceLevelService;

    @Mock
    private AUserExperienceService userExperienceService;

    @Mock
    private RoleManagementService roleManagementService;

    @Mock
    private ExperienceRoleServiceBean self;


    @Test
    public void testSettingRoleToLevelWithoutOldUsers() {
        AServer server = MockUtils.getServer();
        Integer levelCount = 10;
        AExperienceLevel level = AExperienceLevel.builder().experienceNeeded(10L).level(levelCount).build();
        ARole roleToChange = getRole(1L, server);
        AExperienceRole previousExperienceRole = AExperienceRole.builder().role(roleToChange).roleServer(server).level(level).build();
        when(experienceRoleManagementService.getRoleInServerOptional(roleToChange)).thenReturn(Optional.of(previousExperienceRole));
        CompletableFuture<Void> future = testingUnit.setRoleToLevel(roleToChange, levelCount, getFeedbackChannel(server));

        future.join();
        verify(experienceRoleManagementService, times(1)).unsetRole(previousExperienceRole);
    }

    @Test
    public void testUnsetRoleInDb() {
        AServer server = MockUtils.getServer();
        Integer levelCount = 10;
        AExperienceLevel level = AExperienceLevel.builder().experienceNeeded(10L).level(levelCount).build();
        ARole roleToChange = getRole(1L, server);
        when(experienceLevelService.getLevel(levelCount)).thenReturn(Optional.of(level));
        when(roleManagementService.findRole(roleToChange.getId())).thenReturn(roleToChange);
        testingUnit.unsetRoleInDb(levelCount, roleToChange.getId());

        verify(experienceRoleManagementService, times(1)).removeAllRoleAssignmentsForLevelInServer(level, server);
        verify(experienceRoleManagementService, times(1)).setLevelToRole(level, roleToChange);
        verify(experienceRoleManagementService, times(0)).getExperienceRolesForServer(server);
    }


    @Test
    public void testSettingRoleToLevelExistingUsers() {
        AServer server = MockUtils.getServer();
        Integer levelCount = 10;
        AExperienceLevel level = AExperienceLevel.builder().experienceNeeded(10L).level(levelCount).build();
        ARole roleToChange = getRole(1L, server);
        ARole newRoleToAward = getRole(2L, server);
        AUserExperience firstUser = AUserExperience.builder().build();
        AUserExperience secondUser = AUserExperience.builder().build();
        List<AUserExperience> users = Arrays.asList(firstUser, secondUser);
        AExperienceRole previousExperienceRole = AExperienceRole.builder().role(roleToChange).id(roleToChange.getId()).roleServer(server).level(level).users(users).build();
        AExperienceRole newExperienceRole = AExperienceRole.builder().role(newRoleToAward).id(newRoleToAward.getId()).roleServer(server).level(level).build();
        when(experienceRoleManagementService.getRoleInServerOptional(roleToChange)).thenReturn(Optional.of(previousExperienceRole));
        when(experienceRoleManagementService.getExperienceRolesForServer(server)).thenReturn(new ArrayList<>(Arrays.asList(newExperienceRole, previousExperienceRole)));
        AChannel feedBackChannel = getFeedbackChannel(server);
        List<CompletableFuture<RoleCalculationResult>> futures = new ArrayList<>();
        futures.add(CompletableFuture.completedFuture(null));
        CompletableFutureList<RoleCalculationResult> futuresList = new CompletableFutureList<>(futures);
        when(userExperienceService.executeActionOnUserExperiencesWithFeedBack(eq(users), eq(feedBackChannel), any())).thenReturn(futuresList);
        CompletableFuture<Void> future = testingUnit.setRoleToLevel(roleToChange, levelCount, feedBackChannel);
        future.join();
        verify(experienceRoleManagementService, times(0)).unsetRole(previousExperienceRole);
    }

    @Test
    public void testCalculateRoleForLevelInBetween() {
        List<AExperienceRole> roles = getExperienceRoles();
        AUserExperience userExperience = AUserExperience.builder().currentLevel(AExperienceLevel.builder().level(6).build()).build();
        AExperienceRole aExperienceRole = testingUnit.calculateRole(roles, userExperience.getLevelOrDefault());
        Assert.assertEquals(aExperienceRole.getLevel().getLevel().intValue(), 5);
    }

    @Test
    public void testCalculateRoleForLevelBelow() {
        List<AExperienceRole> roles = getExperienceRoles();
        AUserExperience userExperience = AUserExperience.builder().currentLevel(AExperienceLevel.builder().level(4).build()).build();
        AExperienceRole aExperienceRole = testingUnit.calculateRole(roles, userExperience.getLevelOrDefault());
        Assert.assertNull(aExperienceRole);
    }

    @Test
    public void testCalculateRoleForLevelOver() {
        List<AExperienceRole> roles = getExperienceRoles();
        AUserExperience userExperience = AUserExperience.builder().currentLevel(AExperienceLevel.builder().level(11).build()).build();
        AExperienceRole aExperienceRole = testingUnit.calculateRole(roles, userExperience.getLevelOrDefault());
        Assert.assertEquals(aExperienceRole.getLevel().getLevel().intValue(), 10);
    }

    @Test
    public void testCalculateRoleForLevelExact() {
        List<AExperienceRole> roles = getExperienceRoles();
        AUserExperience userExperience = AUserExperience.builder().currentLevel(AExperienceLevel.builder().level(10).build()).build();
        AExperienceRole aExperienceRole = testingUnit.calculateRole(roles,  userExperience.getLevelOrDefault());
        Assert.assertEquals(aExperienceRole.getLevel().getLevel().intValue(), 10);
    }

    @Test
    public void testCalculateRoleForNoRoleConfigFound() {
        List<AExperienceRole> roles = new ArrayList<>();
        AUserExperience userExperience = AUserExperience.builder().currentLevel(AExperienceLevel.builder().level(6).build()).build();
        AExperienceRole aExperienceRole = testingUnit.calculateRole(roles,  userExperience.getLevelOrDefault());
        Assert.assertNull(aExperienceRole);
    }

    @Test
    public void testCalculatingLevelOfNextRole() {
        AServer server = MockUtils.getServer();
        when(experienceRoleManagementService.getExperienceRolesForServer(server)).thenReturn(getExperienceRoles());
        AExperienceLevel levelToCheckFor =  AExperienceLevel.builder().level(7).build();
        AExperienceLevel levelOfNextRole = testingUnit.getLevelOfNextRole(levelToCheckFor, server);
        Assert.assertEquals(10, levelOfNextRole.getLevel().intValue());
    }

    @Test
    public void testCalculatingLevelOfNextRoleIfThereIsNone() {
        AServer server = MockUtils.getServer();
        when(experienceRoleManagementService.getExperienceRolesForServer(server)).thenReturn(getExperienceRoles());
        AExperienceLevel levelToCheckFor =  AExperienceLevel.builder().level(15).build();
        AExperienceLevel levelOfNextRole = testingUnit.getLevelOfNextRole(levelToCheckFor, server);
        Assert.assertEquals(200, levelOfNextRole.getLevel().intValue());
    }

    private List<AExperienceRole> getExperienceRoles() {
        AExperienceRole level5ExperienceRole = getExperienceRoleForLevel(5);
        AExperienceRole level10ExperienceRole = getExperienceRoleForLevel(10);
        return Arrays.asList(level5ExperienceRole, level10ExperienceRole);
    }

    private AExperienceRole getExperienceRoleForLevel(int levelToBuild) {
        AExperienceLevel firstLevel = AExperienceLevel.builder().level(levelToBuild).build();
        return AExperienceRole.builder().level(firstLevel).build();
    }

    private ARole getRole(Long id, AServer server) {
        return ARole.builder().id(id).server(server).deleted(false).build();
    }

    private AChannel getFeedbackChannel(AServer server) {
        return AChannel.builder().id(1L).server(server).type(AChannelType.TEXT).build();
    }

}
