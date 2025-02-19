package dev.sheldan.abstracto.core.command.config;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class Parameter implements Serializable {
    private String name;
    private Class type;
    private String description;
    @Builder.Default
    private boolean optional = false;
    @Builder.Default
    private boolean remainder = false;
    @Builder.Default
    private boolean isListParam = false;
    @Builder.Default
    private Boolean templated = false;
    @Builder.Default
    private Boolean slashCommandOnly = false;
    @Builder.Default
    private Integer listSize = 0;
    @Builder.Default
    private Boolean supportsAutoComplete = false;
    @Builder.Default
    private List<ParameterValidator> validators = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> additionalInfo = new HashMap<>();
    // these are the features which potentially require this parameter
    @Builder.Default
    private List<String> dependentFeatures = new ArrayList<>();
    @Builder.Default
    private List<String> choices = new ArrayList<>();
    @Builder.Default
    private Boolean useStrictParameters = false;
    @Builder.Default
    private Boolean supportsUserCommands = true;

    public String getSlashCompatibleName() {
        return name.toLowerCase(Locale.ROOT);
    }

    public static final String ADDITIONAL_TYPES_KEY = "ADDITIONAL_TYPES";
}
