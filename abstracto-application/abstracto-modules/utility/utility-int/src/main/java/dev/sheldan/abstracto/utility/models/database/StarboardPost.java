package dev.sheldan.abstracto.utility.models.database;

import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name="starboard_post")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class StarboardPost implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_in_server_id", nullable = false)
    private AUserInAServer author;

    @Column(name = "starboard_message_id")
    private Long starboardMessageId;

    @Column(name = "post_message_id")
    private Long postMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private AChannel starboardChannel;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private AServer server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_channel_id", nullable = false)
    private AChannel sourceChannel;

    @Transient
    private Integer reactionCount;

    @Column(name = "created")
    private Instant created;

    @Column(name = "updated")
    private Instant updated;

    @PostLoad
    private void onLoad() {
        this.reactionCount = this.reactions.size();
    }

    @Getter
    @OneToMany(fetch = FetchType.LAZY,
            orphanRemoval = true,
            cascade =  {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE},
            mappedBy = "starboardPost")
    private List<StarboardPostReaction> reactions;

    @Column(name = "starred_date")
    private Instant starredDate;

    @Column(name = "ignored")
    private boolean ignored;

    public int getReactionCount() {
        if(this.reactions == null) {
            return 0;
        }
        return this.reactions.size();
    }

}
