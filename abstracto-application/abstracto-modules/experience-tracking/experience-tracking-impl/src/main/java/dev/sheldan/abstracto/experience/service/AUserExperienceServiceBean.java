package dev.sheldan.abstracto.experience.service;

import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.ARole;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.service.BotService;
import dev.sheldan.abstracto.core.service.ConfigService;
import dev.sheldan.abstracto.core.service.MessageService;
import dev.sheldan.abstracto.core.service.RoleService;
import dev.sheldan.abstracto.experience.config.features.ExperienceFeatureConfig;
import dev.sheldan.abstracto.experience.models.database.*;
import dev.sheldan.abstracto.experience.models.LeaderBoard;
import dev.sheldan.abstracto.experience.models.LeaderBoardEntry;
import dev.sheldan.abstracto.experience.models.templates.UserSyncStatusModel;
import dev.sheldan.abstracto.experience.service.management.DisabledExpRoleManagementService;
import dev.sheldan.abstracto.experience.service.management.ExperienceLevelManagementService;
import dev.sheldan.abstracto.experience.service.management.ExperienceRoleManagementService;
import dev.sheldan.abstracto.experience.service.management.UserExperienceManagementService;
import dev.sheldan.abstracto.templating.model.MessageToSend;
import dev.sheldan.abstracto.templating.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AUserExperienceServiceBean implements AUserExperienceService {


    @Autowired
    private UserExperienceManagementService userExperienceManagementService;

    @Autowired
    private ExperienceRoleService experienceRoleService;

    @Autowired
    private ExperienceLevelManagementService experienceLevelManagementService;

    @Autowired
    private ExperienceRoleManagementService experienceRoleManagementService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private DisabledExpRoleManagementService disabledExpRoleManagementService;

    @Autowired
    private BotService botService;

    @Autowired
    private RunTimeExperienceService runTimeExperienceService;

    /**
     * Creates the user in the runtime experience, if the user was not in yet. Also creates an entry for the minute, if necessary.
     * @param userInAServer The {@link AUserInAServer} to be added to the list of users gaining experience
     */
    @Override
    public void addExperience(AUserInAServer userInAServer) {
        Long second = Instant.now().getEpochSecond() / 60;
        Map<Long, List<AServer>> runtimeExperience = runTimeExperienceService.getRuntimeExperience();
        if(runtimeExperience.containsKey(second)) {
            List<AServer> existing = runtimeExperience.get(second);
            existing.forEach(server -> {
                if(server.getUsers().stream().noneMatch(userInAServer1 -> userInAServer.getUserInServerId().equals(userInAServer1.getUserInServerId()))) {
                    server.getUsers().add(userInAServer);
                }
            });

        } else {
            AServer server = AServer
                    .builder()
                    .id(userInAServer.getServerReference().getId())
                    .build();
            server.getUsers().add(userInAServer);
            runtimeExperience.put(second, Arrays.asList(server));
        }
    }

    @Override
    public Map<Long, List<AServer>> getRuntimeExperience() {
        return runTimeExperienceService.getRuntimeExperience();
    }


    /**
     * Calculates the level of the given {@link AUserExperience} according to the given {@link AExperienceLevel} list
     * @param experience The {@link AUserExperience} to calculate the level for
     * @param levels The list of {@link AExperienceLevel} representing the level configuration, this must include the initial level 0
     *      *               This level will be taken as the initial value, and if no other level qualifies, this will be taken.
     *               The levels must be ordered.
     * @return The appropriate level according to the level config
     */
    @Override
    public AExperienceLevel calculateLevel(AUserExperience experience, List<AExperienceLevel> levels) {
        AExperienceLevel lastLevel = levels.get(0);
        for (AExperienceLevel level : levels) {
            if(level.getExperienceNeeded() >= experience.getExperience()) {
                return lastLevel;
            } else {
                lastLevel = level;
            }
        }
        return lastLevel;
    }

    @Override
    public boolean updateUserLevel(AUserExperience userExperience, List<AExperienceLevel> levels) {
        AUserInAServer user = userExperience.getUser();
        AExperienceLevel correctLevel = calculateLevel(userExperience, levels);
        Integer currentLevel = userExperience.getCurrentLevel() != null ? userExperience.getCurrentLevel().getLevel() : 0;
        if(!correctLevel.getLevel().equals(currentLevel)) {
            log.info("User {} leveled from {} to {}", user.getUserReference().getId(), currentLevel, correctLevel.getLevel());
            userExperience.setCurrentLevel(correctLevel);
            return true;
        }
        return false;
    }

    /**
     * Calculates the actually gained experience for every user in the given servers and adds them to the users.
     * This method only actually increases the message count, and calls other methods for experience gain
     * and role change.
     * Loads the level and role configuration for each server and sorts them for them to be used.
     * Only actually updates the role, if the user also changed level.
     * @param servers The list of {@link AServer} containing the user which need to gain experience
     */
    @Transactional
    @Override
    public void handleExperienceGain(List<AServer> servers) {
        servers.forEach(serverExp -> {
            log.trace("Handling experience for server {}", serverExp.getId());
            int minExp = configService.getLongValue(ExperienceFeatureConfig.MIN_EXP_KEY, serverExp.getId()).intValue();
            int maxExp = configService.getLongValue(ExperienceFeatureConfig.MAX_EXP_KEY, serverExp.getId()).intValue();
            Double multiplier = configService.getDoubleValue(ExperienceFeatureConfig.EXP_MULTIPLIER_KEY, serverExp.getId());
            PrimitiveIterator.OfInt iterator = new Random().ints(serverExp.getUsers().size(), minExp, maxExp + 1).iterator();
            List<AExperienceLevel> levels = experienceLevelManagementService.getLevelConfig();
            levels.sort(Comparator.comparing(AExperienceLevel::getExperienceNeeded));
            List<AExperienceRole> roles = experienceRoleManagementService.getExperienceRolesForServer(serverExp);
            List<ADisabledExpRole> disabledExpRoles = disabledExpRoleManagementService.getDisabledRolesForServer(serverExp);
            List<ARole> disabledRoles = disabledExpRoles.stream().map(ADisabledExpRole::getRole).collect(Collectors.toList());
            roles.sort(Comparator.comparing(role -> role.getLevel().getLevel()));
            serverExp.getUsers().forEach(userInAServer -> {
                Integer gainedExperience = iterator.next();
                gainedExperience = (int) Math.floor(gainedExperience * multiplier);
                Member member = botService.getMemberInServer(userInAServer);
                if(!roleService.hasAnyOfTheRoles(member, disabledRoles)) {
                    log.trace("Handling {}. The user gains {}", userInAServer.getUserReference().getId(), gainedExperience);
                    AUserExperience aUserExperience = userExperienceManagementService.incrementExpForUser(userInAServer, gainedExperience.longValue(), 1L);
                    if(Boolean.FALSE.equals(aUserExperience.getExperienceGainDisabled())) {
                        updateUserLevel(aUserExperience, levels);
                        updateUserRole(aUserExperience, roles);
                        userExperienceManagementService.saveUser(aUserExperience);
                    } else {
                        log.trace("Experience gain was disabled. User did not gain any experience.");
                    }
                } else {
                    log.trace("User {} has a role which makes the user unable to gain experience.", userInAServer.getUserInServerId());
                }
            });
        });
    }

    /**
     * Calculates the appropriate level of the user and changes the role, if the {@link AExperienceLevel} changes.
     * This changes the config in the database, and also gives the {@link net.dv8tion.jda.api.entities.Member} the new
     * {@link net.dv8tion.jda.api.entities.Role}. If the user does not warrant an {@link AExperienceRole},
     * this method also removes it. The role is only changed, if the user does not have
     * @param userExperience The {@link AUserExperience} object to recalculate the {@link AExperienceRole} for
     * @param roles The list of {@link AExperienceRole} used as a role configuration
     */
    @Override
    public void updateUserRole(AUserExperience userExperience, List<AExperienceRole> roles) {
        AUserInAServer user = userExperience.getUser();
        log.trace("Updating experience role for user {} in server {}", user.getUserReference().getId(), user.getServerReference().getId());
        AExperienceRole role = experienceRoleService.calculateRole(userExperience, roles);
        Member member = botService.getMemberInServer(user.getServerReference(), user.getUserReference());
        boolean currentlyHasNoExperienceRole = userExperience.getCurrentExperienceRole() == null;
        if(role == null) {
            if(!currentlyHasNoExperienceRole && botService.isUserInGuild(userExperience.getUser())){
                roleService.removeRoleFromUser(user, userExperience.getCurrentExperienceRole().getRole());
            }
            userExperience.setCurrentExperienceRole(null);
            return;
        }
        boolean userHasRoleAlready = roleService.memberHasRole(member, role.getRole());
        if(!userHasRoleAlready && (currentlyHasNoExperienceRole || !role.getRole().getId().equals(userExperience.getCurrentExperienceRole().getRole().getId()))) {
            log.info("User {} in server {} gets a new role {}", user.getUserReference().getId(), user.getServerReference().getId(), role.getRole().getId());
            if(!currentlyHasNoExperienceRole && botService.isUserInGuild(userExperience.getUser())) {
                roleService.removeRoleFromUser(user, userExperience.getCurrentExperienceRole().getRole());
            }
            roleService.addRoleToUser(user, role.getRole());
        }
        userExperience.setCurrentExperienceRole(role);
    }

    /**
     * Synchronizes the {@link net.dv8tion.jda.api.entities.Role} of all {@link net.dv8tion.jda.api.entities.Member} in
     * the given {@link AServer}. This might take a long time to complete, because there are a lot of role changes.
     * @param server The {@link AServer} to update the users for
     */
    @Override
    public void syncUserRoles(AServer server) {
        List<AUserExperience> aUserExperiences = userExperienceManagementService.loadAllUsers(server);
        log.info("Found {} users to synchronize", aUserExperiences.size());
        List<AExperienceRole> roles = experienceRoleManagementService.getExperienceRolesForServer(server);
        for (int i = 0; i < aUserExperiences.size(); i++) {
            AUserExperience userExperience = aUserExperiences.get(i);
            log.trace("Synchronizing {} out of {}", i, aUserExperiences.size());
            updateUserRole(userExperience, roles);
        }
    }

    /**
     * Synchronizes the roles of all the users and provides feedback to the user executing
     * @param server The {@link AServer} to update users for
     * @param channel The {@link AChannel} in which the {@link dev.sheldan.abstracto.experience.models.templates.UserSyncStatusModel}
     */
    @Override
    public void syncUserRolesWithFeedback(AServer server, AChannel channel) {
        List<AUserExperience> aUserExperiences = userExperienceManagementService.loadAllUsers(server);
        log.info("Found {} users to synchronize", aUserExperiences.size());
        List<AExperienceRole> roles = experienceRoleManagementService.getExperienceRolesForServer(server);
        executeActionOnUserExperiencesWithFeedBack(aUserExperiences, channel, (AUserExperience experience) -> updateUserRole(experience, roles));
    }

    /**
     * Executes the given {@link Consumer} on each of the experiences and provides feedback in the given AChannel in the form of a status message
     * @param experiences The list of {@link AUserExperience} to be working on
     * @param channel The {@link AChannel} used to provide feedback to the user
     * @param toExecute The {@link Consumer} which should be executed on each element of the passed list
     */
    @Override
    public void executeActionOnUserExperiencesWithFeedBack(List<AUserExperience> experiences, AChannel channel, Consumer<AUserExperience> toExecute) {
        MessageToSend status = getUserSyncStatusUpdateModel(0, experiences.size());
        try {
            Message statusMessage = messageService.createStatusMessage(status, channel).get();
            int interval = Math.min(Math.max(experiences.size() / 10, 1), 100);
            for (int i = 0; i < experiences.size(); i++) {
                if((i % interval) == 1) {
                    log.trace("Updating feedback message with new index {} out of {}", i, experiences.size());
                    status = getUserSyncStatusUpdateModel(i, experiences.size());
                    messageService.updateStatusMessage(channel, statusMessage.getIdLong(), status);
                }
                toExecute.accept(experiences.get(i));
                log.trace("Synchronizing {} out of {}", i, experiences.size());
            }
            status = getUserSyncStatusUpdateModel(experiences.size(), experiences.size());
            messageService.updateStatusMessage(channel, statusMessage.getIdLong(), status);
        } catch (InterruptedException | ExecutionException e) {
            log.info("Failed to synchronize users.", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void disableExperienceForUser(AUserInAServer userInAServer) {
        AUserExperience userExperience = userExperienceManagementService.findUserInServer(userInAServer);
        userExperience.setExperienceGainDisabled(true);
    }

    @Override
    public void enableExperienceForUser(AUserInAServer userInAServer) {
        AUserExperience userExperience = userExperienceManagementService.findUserInServer(userInAServer);
        userExperience.setExperienceGainDisabled(false);
    }

    private MessageToSend getUserSyncStatusUpdateModel(Integer current, Integer total) {
        UserSyncStatusModel statusModel = UserSyncStatusModel.builder().currentCount(current).totalUserCount(total).build();
        return templateService.renderEmbedTemplate("user_sync_status_message", statusModel);
    }

    /**
     * Retrieves the role configuration and executes the method responsible to sync the experience role of the user
     * @param userExperience The {@link AUserExperience} to synchronize the role for
     */
    @Override
    public void syncForSingleUser(AUserExperience userExperience) {
        AUserInAServer user = userExperience.getUser();
        log.info("Synchronizing for user {} in server {}", user.getUserReference().getId(), user.getServerReference().getId());
        List<AExperienceRole> roles = experienceRoleManagementService.getExperienceRolesForServer(user.getServerReference());
        updateUserRole(userExperience, roles);
    }

    /**
     * Retrieves the leaderboard data for the given page of the given server
     * @param server The {@link AServer} to retrieve the leaderboard for
     * @param page The desired page on the leaderboard. The pagesize is 10
     * @return The {@link LeaderBoard} containing all necessary information concerning the leaderboard
     */
    @Override
    public LeaderBoard findLeaderBoardData(AServer server, Integer page) {
        if(page <= 0) {
            throw new IllegalArgumentException("Page needs to be >= 1");
        }
        page--;
        int pageSize = 10;
        List<AUserExperience> experiences = userExperienceManagementService.findLeaderBoardUsersPaginated(server, page * pageSize, (page + 1) * pageSize);
        List<LeaderBoardEntry> entries = new ArrayList<>();
        for (int i = 0; i < experiences.size(); i++) {
            AUserExperience userExperience = experiences.get(i);
            entries.add(LeaderBoardEntry.builder().experience(userExperience).rank((page * pageSize) + i + 1).build());
        }
        return LeaderBoard.builder().entries(entries).build();
    }

    /**
     * Builds an {@link AUserExperience} and loads the appropriate rank of the passed {@link AUserInAServer}
     * @param userInAServer The {@link AUserInAServer} to retrieve the {@link LeaderBoardEntry} for
     * @return The {@link LeaderBoardEntry} representing one single row in the leaderboard
     */
    @Override
    public LeaderBoardEntry getRankOfUserInServer(AUserInAServer userInAServer) {
        log.info("Retrieving rank for {}", userInAServer.getUserReference().getId());
        AUserExperience aUserExperience = userExperienceManagementService.findUserInServer(userInAServer);
        Integer rank = 0;
        if(aUserExperience != null) {
            LeaderBoardEntryResult rankOfUserInServer = userExperienceManagementService.getRankOfUserInServer(aUserExperience);
            if(rankOfUserInServer != null) {
                rank = rankOfUserInServer.getRank();
            }
        }
        return LeaderBoardEntry.builder().experience(aUserExperience).rank(rank).build();
    }

}
