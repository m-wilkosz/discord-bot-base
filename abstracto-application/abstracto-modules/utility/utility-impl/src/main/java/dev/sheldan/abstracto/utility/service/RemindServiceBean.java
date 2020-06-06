package dev.sheldan.abstracto.utility.service;

import dev.sheldan.abstracto.core.exception.ChannelNotFoundException;
import dev.sheldan.abstracto.core.service.ChannelService;
import dev.sheldan.abstracto.core.service.management.ChannelManagementService;
import dev.sheldan.abstracto.core.models.AServerAChannelAUser;
import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUser;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.templating.model.MessageToSend;
import dev.sheldan.abstracto.core.service.BotService;
import dev.sheldan.abstracto.scheduling.service.SchedulerService;
import dev.sheldan.abstracto.templating.service.TemplateService;
import dev.sheldan.abstracto.utility.exception.ReminderNotFoundException;
import dev.sheldan.abstracto.utility.models.database.Reminder;
import dev.sheldan.abstracto.utility.models.template.commands.reminder.ExecutedReminderModel;
import dev.sheldan.abstracto.utility.service.management.ReminderManagementService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.*;

@Component
@Slf4j
public class RemindServiceBean implements ReminderService {

    @Autowired
    private ReminderManagementService reminderManagementService;

    @Autowired
    private ChannelManagementService channelManagementService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private BotService botService;

    @Autowired
    private ReminderService self;

    @Autowired
    private ChannelService channelService;

    @Override
    public Reminder createReminderInForUser(AUserInAServer user, String remindText, Duration remindIn, Message message) {
        AChannel channel = channelManagementService.loadChannel(message.getChannel().getIdLong()).orElseThrow(() -> new ChannelNotFoundException(message.getChannel().getIdLong(), message.getGuild().getIdLong()));
        AServerAChannelAUser aServerAChannelAUser = AServerAChannelAUser
                .builder()
                .user(user.getUserReference())
                .aUserInAServer(user)
                .guild(user.getServerReference())
                .channel(channel)
                .build();
        Instant remindAt = Instant.now().plusNanos(remindIn.toNanos());
        Reminder reminder = reminderManagementService.createReminder(aServerAChannelAUser, remindText, remindAt, message.getIdLong());
        log.info("Creating reminder for user {} in guild {} due at {}.",
                user.getUserReference().getId(), user.getServerReference().getId(), remindAt);

        if(remindIn.getSeconds() < 60) {
            log.trace("Directly scheduling the reminder, because it was below the threshold.");
            // TODO make a bean out of this
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                try {
                    self.executeReminder(reminder.getId());
                } catch (Exception exception) {
                    log.error("Failed to remind immediately.", exception);
                }
            }, remindIn.toNanos(), TimeUnit.NANOSECONDS);
        } else {
            log.trace("Starting scheduled job to execute reminder.");
            JobDataMap parameters = new JobDataMap();
            parameters.putAsString("reminderId", reminder.getId());
            String triggerKey = schedulerService.executeJobWithParametersOnce("reminderJob", "utility", parameters, Date.from(reminder.getTargetDate()));
            reminder.setJobTriggerKey(triggerKey);
            reminderManagementService.saveReminder(reminder);
        }
        return reminder;
    }

    @Override
    @Transactional
    public void executeReminder(Long reminderId)  {
        Reminder reminderToRemindFor = reminderManagementService.loadReminder(reminderId).orElseThrow(() -> new ReminderNotFoundException(reminderId));
        if(reminderToRemindFor.isReminded()) {
            return;
        }
        AServer server = reminderToRemindFor.getServer();
        AChannel channel = reminderToRemindFor.getChannel();
        log.info("Executing reminder {}.", reminderId);
        Optional<Guild> guildToAnswerIn = botService.getGuildById(server.getId());
        if(guildToAnswerIn.isPresent()) {
            Optional<TextChannel> channelToAnswerIn = botService.getTextChannelFromServer(server.getId(), channel.getId());
            // only send the message if the channel still exists, if not, only set the reminder to reminded.
            if(channelToAnswerIn.isPresent()) {
                AUser userReference = reminderToRemindFor.getRemindedUser().getUserReference();
                Member memberInServer = botService.getMemberInServer(server.getId(), userReference.getId());
                log.trace("Reminding user {}", userReference.getId());
                ExecutedReminderModel build = ExecutedReminderModel
                        .builder()
                        .reminder(reminderToRemindFor)
                        .member(memberInServer)
                        .duration(Duration.between(reminderToRemindFor.getReminderDate(), reminderToRemindFor.getTargetDate()))
                        .build();
                MessageToSend messageToSend = templateService.renderEmbedTemplate("remind_reminder", build);
                channelService.sendMessageToSendToChannel(messageToSend, channelToAnswerIn.get());
            } else {
                log.warn("Channel {} in server {} to remind user did not exist anymore. Ignoring reminder {}", channel.getId(), server.getId(), reminderId);
            }
        } else {
            log.warn("Guild {} to remind user in did not exist anymore. Ignoring reminder {}.", server.getId(), reminderId);
        }
        reminderManagementService.setReminded(reminderToRemindFor);
    }

    @Override
    public void unRemind(Long reminderId, AUserInAServer aUserInAServer) {
        Reminder reminder = reminderManagementService.getReminderByAndByUserNotReminded(aUserInAServer, reminderId).orElseThrow(() -> new ReminderNotFoundException(reminderId));
        reminder.setReminded(true);
        if(reminder.getJobTriggerKey() != null) {
            schedulerService.stopTrigger(reminder.getJobTriggerKey());
        }
    }
}
