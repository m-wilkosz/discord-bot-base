package dev.sheldan.abstracto.moderation.command;

import dev.sheldan.abstracto.core.command.execution.CommandContext;
import dev.sheldan.abstracto.core.command.execution.CommandResult;
import dev.sheldan.abstracto.core.models.database.AServer;
import dev.sheldan.abstracto.core.models.database.AUserInAServer;
import dev.sheldan.abstracto.core.service.ChannelService;
import dev.sheldan.abstracto.core.service.management.ServerManagementService;
import dev.sheldan.abstracto.core.service.management.UserInServerManagementService;
import dev.sheldan.abstracto.core.test.command.CommandConfigValidator;
import dev.sheldan.abstracto.core.test.command.CommandTestUtilities;
import dev.sheldan.abstracto.moderation.converter.UserNotesConverter;
import dev.sheldan.abstracto.moderation.model.database.UserNote;
import dev.sheldan.abstracto.moderation.model.template.command.ListNotesModel;
import dev.sheldan.abstracto.moderation.model.template.command.NoteEntryModel;
import dev.sheldan.abstracto.moderation.service.management.UserNoteManagementService;
import net.dv8tion.jda.api.entities.Member;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserNotesTest {

    @InjectMocks
    private UserNotes testUnit;

    @Mock
    private UserNoteManagementService userNoteManagementService;

    @Mock
    private UserInServerManagementService userInServerManagementService;

    @Mock
    private ChannelService channelService;

    @Mock
    private UserNotesConverter userNotesConverter;

    @Mock
    private ServerManagementService serverManagementService;

    @Captor
    private ArgumentCaptor<ListNotesModel> captor;

    @Test
    public void testExecuteUserNotesCommandForMember() {
        Member member = Mockito.mock(Member.class);
        CommandContext parameters = CommandTestUtilities.getWithParameters(Arrays.asList(member));
        when(member.getGuild()).thenReturn(parameters.getGuild());
        AUserInAServer userNoteUser = Mockito.mock(AUserInAServer.class);
        when(userInServerManagementService.loadOrCreateUser(member)).thenReturn(userNoteUser);
        UserNote firstNote = Mockito.mock(UserNote.class);
        UserNote secondNote = Mockito.mock(UserNote.class);
        List<UserNote> userNotes = Arrays.asList(firstNote, secondNote);
        when(userNoteManagementService.loadNotesForUser(userNoteUser)).thenReturn(userNotes);
        NoteEntryModel firstConvertedNote = Mockito.mock(NoteEntryModel.class);
        NoteEntryModel secondConvertedNote = Mockito.mock(NoteEntryModel.class);
        CompletableFuture<List<NoteEntryModel>> convertedNotes = CompletableFuture.completedFuture(Arrays.asList(firstConvertedNote, secondConvertedNote));
        when(userNotesConverter.fromNotes(userNotes)).thenReturn(convertedNotes);
        CompletableFuture<CommandResult> result = testUnit.executeAsync(parameters);
        verify(channelService, times(1)).sendEmbedTemplateInMessageChannel(eq(UserNotes.USER_NOTES_RESPONSE_TEMPLATE), captor.capture(), eq(parameters.getChannel()));
        ListNotesModel usedModel = captor.getValue();
        List<NoteEntryModel> notes = convertedNotes.join();
        Assert.assertEquals(notes.size(), usedModel.getUserNotes().size());
        for (int i = 0; i < usedModel.getUserNotes().size(); i++) {
            NoteEntryModel usedEntry = usedModel.getUserNotes().get(i);
            NoteEntryModel expectedEntry = notes.get(i);
            Assert.assertEquals(expectedEntry, usedEntry);
        }
        Assert.assertEquals(userNoteUser, usedModel.getSpecifiedUser().getAUserInAServer());
        Assert.assertEquals(member, usedModel.getSpecifiedUser().getMember());
        CommandTestUtilities.checkSuccessfulCompletionAsync(result);
    }

    @Test
    public void testExecuteUserNotesCommandForServer() {
        CommandContext parameters = CommandTestUtilities.getNoParameters();
        UserNote firstNote = Mockito.mock(UserNote.class);
        UserNote secondNote = Mockito.mock(UserNote.class);
        List<UserNote> userNotes = Arrays.asList(firstNote, secondNote);
        AServer server = Mockito.mock(AServer.class);
        when(serverManagementService.loadServer(parameters.getGuild())).thenReturn(server);
        when(userNoteManagementService.loadNotesForServer(server)).thenReturn(userNotes);
        NoteEntryModel firstConvertedNote = Mockito.mock(NoteEntryModel.class);
        NoteEntryModel secondConvertedNote = Mockito.mock(NoteEntryModel.class);
        CompletableFuture<List<NoteEntryModel>> convertedNotes = CompletableFuture.completedFuture(Arrays.asList(firstConvertedNote, secondConvertedNote));
        when(userNotesConverter.fromNotes(userNotes)).thenReturn(convertedNotes);
        CompletableFuture<CommandResult> result = testUnit.executeAsync(parameters);
        List<NoteEntryModel> notes = convertedNotes.join();
        verify(channelService, times(1)).sendEmbedTemplateInMessageChannel(eq(UserNotes.USER_NOTES_RESPONSE_TEMPLATE), captor.capture(), eq(parameters.getChannel()));
        ListNotesModel usedModel = captor.getValue();
        Assert.assertEquals(notes.size(), usedModel.getUserNotes().size());
        for (int i = 0; i < usedModel.getUserNotes().size(); i++) {
            NoteEntryModel usedEntry = usedModel.getUserNotes().get(i);
            NoteEntryModel expectedEntry = notes.get(i);
            Assert.assertEquals(expectedEntry, usedEntry);
        }
        Assert.assertNull(usedModel.getSpecifiedUser());
        CommandTestUtilities.checkSuccessfulCompletionAsync(result);
    }

    @Test
    public void validateCommand() {
        CommandConfigValidator.validateCommandConfiguration(testUnit.getConfiguration());
    }

}
