package dev.sheldan.abstracto.assignableroles.models.exception;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class AssignableRolePlacePostNotFoundExceptionModel implements Serializable {
    private final Long messageId;
}
