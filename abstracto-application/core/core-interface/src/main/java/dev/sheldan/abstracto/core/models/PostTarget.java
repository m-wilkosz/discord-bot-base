package dev.sheldan.abstracto.core.models;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name="posttarget")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(unique = true)
    @Getter
    private String name;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    @Getter @Setter
    private AChannel AChannel;

    public static String JOIN_LOG = "joinlog";

    public static List<String> AVAILABLE_POST_TARGETS = Arrays.asList(JOIN_LOG);
}
