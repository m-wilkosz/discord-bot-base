package dev.sheldan.abstracto.statistic.emote.service;

import dev.sheldan.abstracto.core.models.cache.CachedEmote;
import dev.sheldan.abstracto.statistic.emote.model.PersistingEmote;
import dev.sheldan.abstracto.statistic.emote.model.database.UsedEmoteType;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for managing and containing the runtime storage for emote statistics
 */
public interface TrackedEmoteRuntimeService {

    /**
     * Returns the current runtime configuration. You should acquire the lock with `takeLock` before.
     * @return The Map containing the current runtime emote stats
     */
    Map<Long, Map<Long, List<PersistingEmote>>> getRuntimeConfig();

    /**
     * Adds the given {@link CachedEmote} used in the {@link Guild} to the runtime storage.
     * The necessary lock will be acquired by this method.
     * @param emote The {@link CachedEmote} to add to the runtime storage
     * @param guild The {@link Guild} in which the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} is used
     * @param count The amount of usages which should be added
     * @param external Whether the emote is external
     * @param usedEmoteType The type of the emote
     */
    void addEmoteForServer(CachedEmote emote, Guild guild, Long count, boolean external, UsedEmoteType usedEmoteType);

    /**
     * Calculates the key used for the Map containing the emote statistics.
     * @return The calculated key to be used in the Map
     */
    Long getKey();

    /**
     * Creates a {@link PersistingEmote} from the given parameters.
     * @param guild The {@link Guild} in which the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} is used
     * @param emote The {@link CachedEmote} to create a {@link PersistingEmote} from
     * @param external Whether or not the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} is external
     * @return A created {@link PersistingEmote} instance from the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji}
     */
    PersistingEmote createFromEmote(Guild guild, CachedEmote emote, boolean external, UsedEmoteType type);

    /**
     * Creates a {@link PersistingEmote} from the given parameters.
     * @param guild The {@link Guild} in which the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} is used
     * @param emote The {@link CachedEmote} to create a {@link PersistingEmote} from
     * @param count The amount of usages the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} has been used
     * @param external Whether or not the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji} is external
     * @return A created {@link PersistingEmote} instance from the {@link net.dv8tion.jda.api.entities.emoji.CustomEmoji}
     */
    PersistingEmote createFromEmote(Guild guild, CachedEmote emote, Long count, boolean external, UsedEmoteType usedEmoteType);

    /**
     * Acquires the lock which should be used when accessing the runtime storage
     */
    void takeLock();

    /**
     * Releases the lock which should be used then accessing the runtime storage
     */
    void releaseLock();
}
