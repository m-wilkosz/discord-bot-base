package dev.sheldan.abstracto.core.interactive;

import dev.sheldan.abstracto.core.exception.ChannelNotFoundException;
import dev.sheldan.abstracto.core.models.AServerChannelUserId;
import dev.sheldan.abstracto.core.models.database.AChannel;
import dev.sheldan.abstracto.core.models.database.AConfig;
import dev.sheldan.abstracto.core.models.database.ADefaultConfig;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.models.template.commands.SetupSystemConfigMessageModel;
import dev.sheldan.abstracto.core.service.ConfigService;
import dev.sheldan.abstracto.core.service.management.ChannelManagementService;
import dev.sheldan.abstracto.core.service.management.DefaultConfigManagementService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.templating.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
@Slf4j
public class SystemConfigSetupStep extends AbstractConfigSetupStep {

    @Autowired
    private ConfigService configService;

    @Autowired
    private InteractiveService interactiveService;

    @Autowired
    private UserInServerManagementService userInServerManagementService;

    @Autowired
    private ChannelManagementService channelManagementService;

    @Autowired
    private SystemConfigSetupStep self;

    @Autowired
    private DefaultConfigManagementService defaultConfigManagementService;

    @Autowired
    private TemplateService templateService;

    @Override
    public CompletableFuture<SetupStepResult> execute(AServerChannelUserId user, SetupStepParameter parameter) {
        SystemConfigStepParameter systemConfigStepParameter = (SystemConfigStepParameter) parameter;
        ADefaultConfig defaultConfig = defaultConfigManagementService.getDefaultConfig(systemConfigStepParameter.getConfigKey());
        SetupSystemConfigMessageModel model = SetupSystemConfigMessageModel
                .builder()
                .configKey(systemConfigStepParameter.getConfigKey())
                .defaultConfig(defaultConfig)
                .build();
        String messageTemplateKey = "setup_system_config_message";
        String messageText =  templateService.renderTemplate(messageTemplateKey, model);
        Optional<AChannel> channel = channelManagementService.loadChannel(user.getChannelId());
        CompletableFuture<SetupStepResult> future = new CompletableFuture<>();
        AUserInAServer aUserInAServer = userInServerManagementService.loadUser(user.getGuildId(), user.getUserId());
        if(channel.isPresent()) {
            Runnable finalAction = super.getTimeoutRunnable(user.getGuildId(), user.getChannelId());
            Consumer<MessageReceivedEvent> configAction = (MessageReceivedEvent event) -> {
                try {
                    SetupStepResult result;
                    Message message = event.getMessage();
                    if(checkForExit(message)) {
                        result = SetupStepResult.fromCancelled();
                    } else {
                        AConfig config;
                        if(checkForKeep(message)) {
                            config = self.loadDefaultConfig(systemConfigStepParameter);
                        } else {
                            config = self.checkValidity(user, systemConfigStepParameter, event);
                        }
                        SystemConfigDelayedActionConfig build = SystemConfigDelayedActionConfig
                                .builder()
                                .configKey(systemConfigStepParameter.getConfigKey())
                                .serverId(user.getGuildId())
                                .value(config)
                                .build();
                        List<DelayedActionConfig> delayedSteps = Arrays.asList(build);
                        result = SetupStepResult
                                .builder()
                                .result(SetupStepResultType.SUCCESS)
                                .delayedActionConfigList(delayedSteps)
                                .build();
                    }
                    future.complete(result);
                } catch (Exception e) {
                    log.warn("Failed to handle system config. Retrying..", e);
                    future.completeExceptionally(new SetupStepException(e));
                }
            };
            interactiveService.createMessageWithResponse(messageText, aUserInAServer, channel.get(), parameter.getPreviousMessageId(), configAction, finalAction);
        } else {
            future.completeExceptionally(new ChannelNotFoundException(user.getGuildId(), user.getChannelId()));
        }
        return future;
    }

    @Transactional
    public AConfig loadDefaultConfig(SystemConfigStepParameter systemConfigStepParameter) {
        AConfig config;
        ADefaultConfig defaultConfig = defaultConfigManagementService.getDefaultConfig(systemConfigStepParameter.getConfigKey());
        config = AConfig
                .builder()
                .name(defaultConfig.getName())
                .doubleValue(defaultConfig.getDoubleValue())
                .longValue(defaultConfig.getLongValue())
                .stringValue(defaultConfig.getStringValue())
                .build();
        return config;
    }

    @Transactional
    public AConfig checkValidity(AServerChannelUserId user, SystemConfigStepParameter systemConfigStepParameter, MessageReceivedEvent event) {
        return  configService.getFakeConfigForValue(systemConfigStepParameter.getConfigKey(), user.getGuildId(), event.getMessage().getContentRaw());
    }


}
