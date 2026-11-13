
package com.teamchat.realtime_chat.service;

import com.teamchat.realtime_chat.entity.Conversation;
import com.teamchat.realtime_chat.entity.ConversationMember;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.enums.ConversationType;
import com.teamchat.realtime_chat.enums.MemberRole;

import com.teamchat.realtime_chat.repository.ConversationMemberRepository;
import com.teamchat.realtime_chat.repository.ConversationRepository;
import com.teamchat.realtime_chat.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository memberRepository,
            UserRepository userRepository) {

        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    // Tạo cuộc trò chuyện riêng
    @Transactional
    public Conversation createPrivateConversation(
            Long creatorId,
            Long otherUserId) {

        if (creatorId.equals(otherUserId)) {
            throw new IllegalArgumentException(
                    "Không thể tạo cuộc trò chuyện với chính mình");
        }

        User creator = findUser(creatorId);
        User otherUser = findUser(otherUserId);

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.PRIVATE);
        conversation.setCreatedBy(creator);

        conversation = conversationRepository.save(conversation);

        addMember(conversation, creator, MemberRole.OWNER);
        addMember(conversation, otherUser, MemberRole.MEMBER);

        return conversation;
    }

    // Tạo nhóm chat
    @Transactional
    public Conversation createGroup(
            Long creatorId,
            String groupName,
            List<Long> memberIds) {

        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên nhóm không được để trống");
        }

        User creator = findUser(creatorId);

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(groupName);
        conversation.setCreatedBy(creator);

        conversation = conversationRepository.save(conversation);

        addMember(conversation, creator, MemberRole.OWNER);

        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (memberId.equals(creatorId)) {
                    continue;
                }

                User member = findUser(memberId);
                addMember(conversation, member, MemberRole.MEMBER);
            }
        }

        return conversation;
    }

    // Thêm thành viên vào nhóm
    @Transactional
    public ConversationMember addMember(
            Long conversationId,
            Long requesterId,
            Long newMemberId) {

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy nhóm"));

        ConversationMember requester = memberRepository
                .findByConversation_IdAndUser_Id(
                        conversationId, requesterId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bạn không thuộc cuộc trò chuyện này"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException(
                    "Chỉ có thể thêm thành viên vào nhóm chat");
        }

        if (requester.getRole() != MemberRole.OWNER) {
            throw new IllegalArgumentException(
                    "Chỉ chủ nhóm mới có quyền thêm thành viên");
        }

        if (memberRepository.existsByConversation_IdAndUser_Id(
                conversationId, newMemberId)) {
            throw new IllegalArgumentException(
                    "Người dùng đã là thành viên của nhóm");
        }

        User newMember = findUser(newMemberId);

        return addMember(conversation, newMember, MemberRole.MEMBER);
    }

    // Lấy danh sách cuộc trò chuyện của người dùng
    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(Long userId) {

        return memberRepository.findByUser_Id(userId)
                .stream()
                .map(ConversationMember::getConversation)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy người dùng: " + userId));
    }

    private ConversationMember addMember(
            Conversation conversation,
            User user,
            MemberRole role) {

        ConversationMember member = new ConversationMember();
        member.setConversation(conversation);
        member.setUser(user);
        member.setRole(role);

        return memberRepository.save(member);
    }
}
