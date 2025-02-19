package dev.sheldan.abstracto.remind.service;

import dev.sheldan.abstracto.core.models.database.AUser;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.remind.model.database.Reminder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;

public interface ReminderService {
    Reminder createReminderInForUser(AUserInAServer user, String remindText, Duration remindIn, Message message);
    Reminder createReminderInForUser(AUserInAServer user, String remindText, Duration remindIn, Long channelId, Long messageId);
    Reminder createReminderInForUser(AUser aUser, String remindText, Duration remindIn);
    Reminder createReminderInForUser(AUserInAServer user, String remindText, Duration remindIn, Long channelId);
    void executeReminder(Long reminderId);
    void unRemind(Long reminderId, AUserInAServer userInAServer);
    void unRemind(Long reminderId, AUser aUser);
    void snoozeReminder(Long reminderId, AUserInAServer user, Duration newDuration);
}
