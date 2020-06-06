package dev.sheldan.abstracto.core.repository;

import dev.sheldan.abstracto.core.models.database.AConfig;
import dev.sheldan.abstracto.core.models.database.AServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;

@Repository
public interface ConfigRepository extends JpaRepository<AConfig, Long> {

    @QueryHints(@QueryHint(name = org.hibernate.annotations.QueryHints.CACHEABLE, value = "true"))
    AConfig findAConfigByServerIdAndName(Long serverId, String name);

    boolean existsAConfigByServerIdAndName(Long serverId, String name);
    boolean existsAConfigByServerAndName(AServer server, String name);
}
