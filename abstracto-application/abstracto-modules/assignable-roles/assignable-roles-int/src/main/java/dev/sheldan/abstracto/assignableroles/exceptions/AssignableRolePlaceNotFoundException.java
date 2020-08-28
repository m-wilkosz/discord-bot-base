package dev.sheldan.abstracto.assignableroles.exceptions;

import dev.sheldan.abstracto.assignableroles.models.exception.AssignableRolePlaceNotFoundExceptionModel;
import dev.sheldan.abstracto.core.exception.AbstractoRunTimeException;
import dev.sheldan.abstracto.templating.Templatable;

public class AssignableRolePlaceNotFoundException extends AbstractoRunTimeException implements Templatable {

    private final AssignableRolePlaceNotFoundExceptionModel model;

    public AssignableRolePlaceNotFoundException(Long placeId) {
        super("Assignable role place not found");
        this.model = AssignableRolePlaceNotFoundExceptionModel.builder().placeId(placeId).build();
    }

    @Override
    public String getTemplateName() {
        return "assignable_role_place_not_found_exception";
    }

    @Override
    public Object getTemplateModel() {
        return model;
    }
}
