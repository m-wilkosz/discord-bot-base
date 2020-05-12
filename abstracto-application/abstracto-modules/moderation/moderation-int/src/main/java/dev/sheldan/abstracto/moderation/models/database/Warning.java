package dev.sheldan.abstracto.moderation.models.database;

import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * A warning which was given a member with a special reason by a moderating member. This warning is bound to a server.
 */
@Entity
@Table(name="warning")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Warning {

    /**
     * The globally unique id of this warning
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    /**
     * The {@link AUserInAServer} which was warned
     */
    @Getter
    @ManyToOne
    @JoinColumn(name = "warnedUserId", nullable = false)
    private AUserInAServer warnedUser;

    /**
     * The {@link AUserInAServer} which gave the warning
     */
    @Getter
    @ManyToOne
    @JoinColumn(name = "warningUserId", nullable = false)
    private AUserInAServer warningUser;

    /**
     * The reason why this warning was cast
     */
    @Getter
    private String reason;

    /**
     * The date at which the warning was cast
     */
    @Getter
    private Instant warnDate;

    /**
     * Whether or not the warning was already decayed and is not active anymore
     */
    @Getter
    @Setter
    private Boolean decayed;

    /**
     * The date at which the warning was decayed
     */
    @Getter
    @Setter
    private Instant decayDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warning warning = (Warning) o;
        return Objects.equals(id, warning.id) &&
                Objects.equals(warnedUser, warning.warnedUser) &&
                Objects.equals(warningUser, warning.warningUser) &&
                Objects.equals(reason, warning.reason) &&
                Objects.equals(warnDate, warning.warnDate) &&
                Objects.equals(decayed, warning.decayed) &&
                Objects.equals(decayDate, warning.decayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, warnedUser, warningUser, reason, warnDate, decayed, decayDate);
    }
}
