package dev.sheldan.abstracto.statistic.emote.service;

import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.statistic.emote.model.EmoteStatsModel;
import dev.sheldan.abstracto.statistic.emote.model.EmoteStatsResultDisplay;
import dev.sheldan.abstracto.statistic.emote.model.database.TrackedEmote;

import dev.sheldan.abstracto.statistic.emote.model.database.UsedEmoteType;
import java.time.Instant;

/**
 * Service responsible to provide operations on {@link dev.sheldan.abstracto.statistic.emote.model.database.UsedEmote}
 */
public interface UsedEmoteService {
    /**
     * Retrieves the {@link EmoteStatsModel} for the {@link AServer} since {@link Instant}.
     * This {@link EmoteStatsModel} will contain all {@link TrackedEmote} from the server
     * @param server The {@link AServer} to retrieve the emote stats for
     * @param since Emote stats should be younger than this {@link Instant}. Only the date portion is considered.
     * @return An {@link EmoteStatsModel} containing the statistics split by animated and static emote
     */
    EmoteStatsModel getEmoteStatsForServerSince(AServer server, Instant since);

    /**
     * Retrieves the {@link EmoteStatsModel} for the {@link AServer} since {@link Instant}.
     * This {@link EmoteStatsModel} will contain only deleted {@link TrackedEmote} from the server
     * @param server The {@link AServer} to retrieve the emote stats for
     * @param since Emote stats should be younger than this {@link Instant}. Only the date portion is considered.
     * @param usedEmoteType The type of interaction the emote was from
     * @return An {@link EmoteStatsModel} containing the statistics split by animated and static emote
     */
    EmoteStatsModel getDeletedEmoteStatsForServerSince(AServer server, Instant since, UsedEmoteType usedEmoteType);

    /**
     * Retrieves the {@link EmoteStatsModel} for the {@link AServer} since {@link Instant}.
     * This {@link EmoteStatsModel} will contain only external {@link TrackedEmote} from the server
     * @param server The {@link AServer} to retrieve the emote stats for
     * @param since Emote stats should be younger than this {@link Instant}. Only the date portion is considered.
     * @param usedEmoteType The type of interaction the emote was used in
     * @return An {@link EmoteStatsModel} containing the statistics split by animated and static emote
     */
    EmoteStatsModel getExternalEmoteStatsForServerSince(AServer server, Instant since, UsedEmoteType usedEmoteType);

    /**
     * Retrieves the {@link EmoteStatsModel} for the {@link AServer} since {@link Instant}.
     * This {@link EmoteStatsModel} will contain only active {@link TrackedEmote} from the server. These are emotes which are still present
     * the {@link net.dv8tion.jda.api.entities.Guild}
     * @param server The {@link AServer} to retrieve the emote stats for
     * @param since Emote stats should be younger than this {@link Instant}. Only the date portion is considered
     * @param usedEmoteType The type of emote the interaction is coming from
     * @return An {@link EmoteStatsModel} containing the statistics split by animated and static emote
     */
    EmoteStatsModel getActiveEmoteStatsForServerSince(AServer server, Instant since, UsedEmoteType usedEmoteType);
    EmoteStatsResultDisplay getEmoteStatForEmote(TrackedEmote trackedEmote, Instant since, UsedEmoteType usedEmoteType);

    /**
     * Removes all {@link dev.sheldan.abstracto.statistic.emote.model.database.UsedEmote} for the given {@link TrackedEmote} which are younger
     * than the given {@link Instant}
     * @param emote The {@link TrackedEmote} which should have its usages removed
     * @param since {@link dev.sheldan.abstracto.statistic.emote.model.database.UsedEmote} younger than this {@link Instant} shold be remoed. Only the date porition is considered.
     */
    void purgeEmoteUsagesSince(TrackedEmote emote, Instant since);

    /**
     * Removes *all* {@link dev.sheldan.abstracto.statistic.emote.model.database.UsedEmote} for the given {@link TrackedEmote}.
     * @param emote The {@link TrackedEmote} which should have its usages removed
     */
    void purgeEmoteUsages(TrackedEmote emote);
}
