package dev.sheldan.abstracto.utility.models.database;

import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AUser;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name="starboard_post")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StarboardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poster", nullable = false)
    private AUser author;

    @Column
    private Long starboardMessageId;

    @Column
    private Long postMessageId;

    @ManyToOne
    @JoinColumn(name = "channelId", nullable = false)
    private AChannel starboardChannel;

    @ManyToOne
    @JoinColumn(name = "sourceChannelId", nullable = false)
    private AChannel sourceChanel;

    @Transient
    private Integer reactionCount;

    @PostLoad
    private void onLoad() {
        this.reactionCount = this.reactions.size();
    }

    @Getter
    @OneToMany(fetch = FetchType.LAZY,
            orphanRemoval = true,
            cascade =  {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name="postId")
    private List<StarboardPostReaction> reactions;

    @Column
    private Instant starredDate;

    @Column
    private boolean ignored;

    public int getReactionCount() {
        if(this.reactions == null) {
            return 0;
        }
        return this.reactions.size();
    }
}
