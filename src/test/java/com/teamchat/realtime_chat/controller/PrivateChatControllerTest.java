package com.teamchat.realtime_chat.controller;
import com.teamchat.realtime_chat.dto.PrivateMessageRequest;
import com.teamchat.realtime_chat.service.ChatStorageService;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PrivateChatControllerTest {
    @Test void socketMessagesUseDurableStorageWithAuthenticatedIdentity() {
        var storage=mock(ChatStorageService.class);Principal principal=()->"google-sub";
        new PrivateChatController(storage).sendPrivateMessage(new PrivateMessageRequest("recipient","hello"),principal);
        verify(storage).sendPrivate(principal,"recipient","hello");
    }
    @Test void unauthenticatedMessagesCannotBeStored() {
        var storage=mock(ChatStorageService.class);
        assertThrows(IllegalStateException.class,()->new PrivateChatController(storage).sendPrivateMessage(new PrivateMessageRequest("recipient","hello"),null));
        verifyNoInteractions(storage);
    }
}
