package dev.sheldan.abstracto.core.models.command;

import dev.sheldan.abstracto.core.models.context.UserInitiatedServerContext;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
public class PingModel extends UserInitiatedServerContext {
    private Long latency;
}
