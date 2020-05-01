package dev.sheldan.abstracto.utility.service.management;

import dev.sheldan.abstracto.core.models.AServerAChannelAUser;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.utility.models.database.Reminder;

import java.time.Instant;
import java.util.List;

public interface ReminderManagementService {
    Reminder createReminder(AServerAChannelAUser userToBeReminded, String text, Instant timeToBeRemindedAt, Long messageId);
    Reminder loadReminder(Long reminderId);
    void setReminded(Reminder reminder);
    Reminder saveReminder(Reminder reminder);
    List<Reminder> getActiveRemindersForUser(AUserInAServer aUserInAServer);
    Reminder getReminderByAndByUserNotReminded(AUserInAServer aUserInAServer, Long reminderId);
}
