package com.teamchat.realtime_chat.controller;

import com.teamchat.realtime_chat.entity.Conversation;
import com.teamchat.realtime_chat.entity.ConversationMember;
import com.teamchat.realtime_chat.entity.Message;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.enums.ConversationType;
import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.ConversationRepository;
import com.teamchat.realtime_chat.repository.MessageRepository;
import com.teamchat.realtime_chat.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * =========================================================================
 * FILE: AdminController.java - REST API QUẢN TRỊ VIÊN HỆ THỐNG
 * =========================================================================
 * Cung cấp dữ liệu thống kê, quản lý người dùng, quản lý nhóm và tin nhắn
 * cho cả giao diện Quản trị Web (admin.html) và Desktop Swing (AdminFrame).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdminController(UserRepository userRepository,
                           ConversationRepository conversationRepository,
                           ConversationMemberRepository conversationMemberRepository,
                           MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * 1. Lấy số liệu thống kê tổng quan (Dashboard Stats)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> res = new HashMap<>();
        long totalUsers = userRepository.count();
        long totalConversations = conversationRepository.count();
        long totalMessages = messageRepository.count();

        long totalGroups = conversationRepository.findAll().stream()
                .filter(c -> c.getType() == ConversationType.GROUP)
                .count();

        res.put("success", true);
        res.put("totalUsers", totalUsers);
        res.put("totalConversations", totalConversations);
        res.put("totalGroups", totalGroups);
        res.put("totalDirectChats", totalConversations - totalGroups);
        res.put("totalMessages", totalMessages);
        res.put("serverTime", LocalDateTime.now().format(ISO_FORMATTER));
        res.put("serverStatus", "ONLINE");
        return ResponseEntity.ok(res);
    }

    /**
     * 2. Lấy danh sách người dùng (kèm tìm kiếm)
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(@RequestParam(value = "search", required = false) String search) {
        Map<String, Object> res = new HashMap<>();
        List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        String query = (search != null) ? search.trim().toLowerCase() : "";
        List<Map<String, Object>> userList = allUsers.stream()
                .filter(u -> query.isEmpty() ||
                        (u.getDisplayName() != null && u.getDisplayName().toLowerCase().contains(query)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)) ||
                        (u.getPhone() != null && u.getPhone().contains(query)))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getDisplayName());
                    map.put("email", u.getEmail());
                    map.put("phone", u.getPhone() != null ? u.getPhone() : "Chưa cập nhật");
                    map.put("avatar", u.getAvatarUrl());
                    map.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().format(ISO_FORMATTER) : "N/A");
                    map.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().format(ISO_FORMATTER) : "Chưa đăng nhập");
                    map.put("role", "ROLE_USER");
                    return map;
                })
                .collect(Collectors.toList());

        res.put("success", true);
        res.put("total", userList.size());
        res.put("users", userList);
        return ResponseEntity.ok(res);
    }

    /**
     * 3. Xóa người dùng khỏi hệ thống
     */
    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable("id") Long userId) {
        Map<String, Object> res = new HashMap<>();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            res.put("success", false);
            res.put("message", "Không tìm thấy người dùng ID: " + userId);
            return ResponseEntity.badRequest().body(res);
        }

        try {
            // Xóa thành viên nhóm liên quan
            List<ConversationMember> members = conversationMemberRepository.findByUser_Id(userId);
            conversationMemberRepository.deleteAll(members);

            userRepository.deleteById(userId);
            res.put("success", true);
            res.put("message", "Đã xóa tài khoản ID: " + userId + " thành công!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi xóa người dùng: " + e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    /**
     * 4. Lấy danh sách hội thoại & Nhóm chat
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(@RequestParam(value = "search", required = false) String search) {
        Map<String, Object> res = new HashMap<>();
        List<Conversation> all = conversationRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        String query = (search != null) ? search.trim().toLowerCase() : "";
        List<Map<String, Object>> convList = all.stream()
                .filter(c -> query.isEmpty() || (c.getName() != null && c.getName().toLowerCase().contains(query)))
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName() != null ? c.getName() : "Hội thoại #" + c.getId());
                    map.put("type", c.getType() != null ? c.getType().name() : "PRIVATE");
                    map.put("createdByName", c.getCreatedBy() != null ? c.getCreatedBy().getDisplayName() : "Hệ thống");
                    map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().format(ISO_FORMATTER) : "N/A");

                    // Đếm thành viên
                    List<ConversationMember> members = conversationMemberRepository.findByConversation_Id(c.getId());
                    map.put("memberCount", members.size());
                    return map;
                })
                .collect(Collectors.toList());

        res.put("success", true);
        res.put("total", convList.size());
        res.put("conversations", convList);
        return ResponseEntity.ok(res);
    }

    /**
     * 5. Xóa hội thoại / nhóm chat vi phạm
     */
    @DeleteMapping("/conversations/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable("id") Long convId) {
        Map<String, Object> res = new HashMap<>();
        if (!conversationRepository.existsById(convId)) {
            res.put("success", false);
            res.put("message", "Không tìm thấy hội thoại ID: " + convId);
            return ResponseEntity.badRequest().body(res);
        }

        try {
            // Xóa thành viên
            List<ConversationMember> members = conversationMemberRepository.findByConversation_Id(convId);
            conversationMemberRepository.deleteAll(members);

            // Xóa hội thoại
            conversationRepository.deleteById(convId);
            res.put("success", true);
            res.put("message", "Đã xóa hội thoại #" + convId + " thành công!");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Lỗi khi xóa hội thoại: " + e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    /**
     * 6. Lấy 50 tin nhắn mới nhất trên toàn hệ thống
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getRecentMessages() {
        Map<String, Object> res = new HashMap<>();
        List<Message> messages = messageRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();

        List<Map<String, Object>> list = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("conversationId", m.getConversation() != null ? m.getConversation().getId() : 0);
            map.put("conversationName", m.getConversation() != null && m.getConversation().getName() != null ?
                    m.getConversation().getName() : "Hội thoại");
            map.put("senderName", m.getSender() != null ? m.getSender().getDisplayName() : "N/A");
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(ISO_FORMATTER) : "N/A");
            return map;
        }).collect(Collectors.toList());

        res.put("success", true);
        res.put("messages", list);
        return ResponseEntity.ok(res);
    }
}
