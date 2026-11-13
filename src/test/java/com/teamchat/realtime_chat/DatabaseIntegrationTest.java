
package com.teamchat.realtime_chat;

import com.teamchat.realtime_chat.entity.Conversation;
import com.teamchat.realtime_chat.entity.Message;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.enums.ConversationType;
import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.MessageRepository;
import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.realtime_chat.service.ConversationService;
import com.teamchat.realtime_chat.service.MessageService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationMemberRepository memberRepository;

    @Test
    void testCompleteChatDatabaseFlow() {

        // 1. Tạo người dùng thứ nhất
        User user1 = new User();
        user1.setGoogleSub("test-" + UUID.randomUUID());
        user1.setEmail("user1@test.com");
        user1.setDisplayName("Nguyen Van A");

        user1 = userRepository.saveAndFlush(user1);

        // 2. Tạo người dùng thứ hai
        User user2 = new User();
        user2.setGoogleSub("test-" + UUID.randomUUID());
        user2.setEmail("user2@test.com");
        user2.setDisplayName("Tran Thi B");

        user2 = userRepository.saveAndFlush(user2);

        // 3. Tạo cuộc trò chuyện riêng
        Conversation conversation =
                conversationService.createPrivateConversation(
                        user1.getId(),
                        user2.getId()
                );

        assertNotNull(conversation.getId());
        assertEquals(ConversationType.PRIVATE, conversation.getType());

        // 4. Kiểm tra hai người đã được thêm vào cuộc trò chuyện
        assertTrue(
                memberRepository.existsByConversation_IdAndUser_Id(
                        conversation.getId(), user1.getId())
        );

        assertTrue(
                memberRepository.existsByConversation_IdAndUser_Id(
                        conversation.getId(), user2.getId())
        );

        // 5. Gửi tin nhắn
        Message message = messageService.sendMessage(
                conversation.getId(),
                user1.getId(),
                "Xin chao! Day la tin nhan kiem thu.",
                UUID.randomUUID().toString()
        );

        assertNotNull(message.getId());

        // 6. Đảm bảo tin nhắn đã được lưu trong Database
        assertTrue(messageRepository.existsById(message.getId()));

        // 7. Lấy lịch sử tin nhắn
        var messages = messageService.getMessages(
                conversation.getId(),
                user1.getId()
        );

        assertFalse(messages.isEmpty());
assertEquals(
        "Xin chao! Day la tin nhan kiem thu.",
        messages.getContent().get(0).getContent()
);
        System.out.println("================================");
        System.out.println("KIEM THU DATABASE THANH CONG");
        System.out.println("User 1 ID: " + user1.getId());
        System.out.println("User 2 ID: " + user2.getId());
        System.out.println("Conversation ID: " + conversation.getId());
        System.out.println("Message ID: " + message.getId());
        System.out.println("Message: " + message.getContent());
        System.out.println("================================");
    }
}
