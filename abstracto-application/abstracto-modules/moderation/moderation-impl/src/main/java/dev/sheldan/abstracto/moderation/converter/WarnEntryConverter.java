package dev.sheldan.abstracto.moderation.converter;

import dev.sheldan.abstracto.core.models.FullUserInServer;
import dev.sheldan.abstracto.core.models.FutureMemberPair;
import dev.sheldan.abstracto.core.models.ServerSpecificId;
import dev.sheldan.abstracto.core.service.BotService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.core.utils.FutureUtils;
import dev.sheldan.abstracto.moderation.models.database.Warning;
import dev.sheldan.abstracto.moderation.models.template.commands.WarnEntry;
import dev.sheldan.abstracto.moderation.service.management.WarnManagementService;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class WarnEntryConverter {

    @Autowired
    private BotService botService;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired
    private WarnManagementService warnManagementService;

    @Autowired
    private WarnEntryConverter self;

    public CompletableFuture<List<WarnEntry>> fromWarnings(List<Warning> warnings) {
        Map<ServerSpecificId, FutureMemberPair> loadedWarnings = new HashMap<>();
        List<CompletableFuture<Member>> allFutures = new ArrayList<>();
        // TODO maybe optimize to not need to look into the cache twice
        warnings.forEach(warning -> {
            CompletableFuture<Member> warningMemberFuture = botService.getMemberInServerAsync(warning.getWarningUser());
            CompletableFuture<Member> warnedMemberFuture = botService.getMemberInServerAsync(warning.getWarnedUser());
            FutureMemberPair futurePair = FutureMemberPair.builder().firstMember(warningMemberFuture).secondMember(warnedMemberFuture).build();
            loadedWarnings.put(warning.getWarnId(), futurePair);
            allFutures.add(warningMemberFuture);
            allFutures.add(warnedMemberFuture);
        });
        return FutureUtils.toSingleFutureGeneric(allFutures).thenApply(aVoid ->
            self.loadFullWarnEntries(loadedWarnings)
        );

    }

    @Transactional
    public List<WarnEntry> loadFullWarnEntries(Map<ServerSpecificId, FutureMemberPair> loadedWarnInfo) {
        List<WarnEntry> entries = new ArrayList<>();
        loadedWarnInfo.keySet().forEach(warning -> {
            FutureMemberPair memberPair = loadedWarnInfo.get(warning);
            Member warnedMember = memberPair.getSecondMember().join();
            FullUserInServer warnedUser = FullUserInServer
                    .builder()
                    .member(warnedMember)
                    .aUserInAServer(userInServerManagementService.loadUser(warnedMember))
                    .build();

            Member warningMember = memberPair.getFirstMember().join();
            FullUserInServer warningUser = FullUserInServer
                    .builder()
                    .member(warningMember)
                    .aUserInAServer(userInServerManagementService.loadUser(warningMember))
                    .build();
            WarnEntry entry = WarnEntry
                    .builder()
                    .warnedUser(warnedUser)
                    .warningUser(warningUser)
                    .warning(warnManagementService.findById(warning.getId(), warning.getServerId()))
                    .build();
            entries.add(entry);
        });
        return entries;
    }
}
