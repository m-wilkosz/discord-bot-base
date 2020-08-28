package dev.sheldan.abstracto.moderation.converter;

import dev.sheldan.abstracto.core.models.FullUserInServer;
import dev.sheldan.abstracto.core.service.BotService;
import dev.sheldan.abstracto.moderation.models.database.Warning;
import dev.sheldan.abstracto.moderation.models.template.commands.WarnEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WarnEntryConverter {

    @Autowired
    private BotService botService;

    public List<WarnEntry> fromWarnings(List<Warning> warnings) {
        List<WarnEntry> entries = new ArrayList<>();
        warnings.forEach(warning -> {
            FullUserInServer warnedUser = FullUserInServer
                    .builder()
                    .member(botService.getMemberInServer(warning.getWarnedUser()))
                    .aUserInAServer(warning.getWarnedUser())
                    .build();

            FullUserInServer warningUser = FullUserInServer
                    .builder()
                    .member(botService.getMemberInServer(warning.getWarningUser()))
                    .aUserInAServer(warning.getWarningUser())
                    .build();
            WarnEntry entry = WarnEntry
                    .builder()
                    .warnedUser(warnedUser)
                    .warningUser(warningUser)
                    .warning(warning)
                    .build();
            entries.add(entry);
        });
        return entries;
    }
}
