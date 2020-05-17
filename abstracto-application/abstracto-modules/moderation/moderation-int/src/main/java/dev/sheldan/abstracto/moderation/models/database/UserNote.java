package dev.sheldan.abstracto.moderation.models.database;

import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name="user_notes")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "noteUser", nullable = false)
    private AUserInAServer user;

    @Column(length = 2000)
    private String note;

    @Column(name = "created")
    private Instant created;

    @PrePersist
    private void onInsert() {
        this.created = Instant.now();
    }
}
