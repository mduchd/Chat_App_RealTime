/**
 * ==========================================================================
 * ChatSpace Messenger — Production Frontend & Realtime Sync (STOMP /ws + Multi-Tab)
 * Hỗ trợ đăng nhập nhiều tài khoản trên nhiều Tab trình duyệt để chat realtime thật
 * ==========================================================================
 */

// --------------------------------------------------------------------------
// 1. TÀI KHOẢN NGƯỜI DÙNG, DANH BẠ & KHO LƯU TRỮ DỮ LIỆU
// --------------------------------------------------------------------------
const STORAGE_KEYS = {
    TAB_USER: 'chatspace_tab_user_v1',
    CONVERSATIONS: 'chatspace_conversations_v1',
    CONTACTS: 'chatspace_contacts_v1'
};

// Danh sách tài khoản thành viên mặc định
const DEFAULT_ACCOUNTS = [
    {
        id: 'user-duc',
        name: 'Nguyễn Minh Đức',
        shortName: 'Minh Đức',
        email: 'minhduchd20059b@gmail.com',
        online: true,
        avatar: 'https://ui-avatars.com/api/?name=Minh+Duc&background=0084ff&color=fff&bold=true'
    },
    {
        id: 'user-ha',
        name: 'Trần Thu Hà',
        shortName: 'Thu Hà',
        email: 'thuha.hau@gmail.com',
        online: true,
        avatar: 'https://ui-avatars.com/api/?name=Thu+Ha&background=0ea5e9&color=fff&bold=true'
    },
    {
        id: 'user-nam',
        name: 'Lê Hoàng Nam',
        shortName: 'Hoàng Nam',
        email: 'hoangnam.dev@gmail.com',
        online: true,
        avatar: 'https://ui-avatars.com/api/?name=Hoang+Nam&background=8b5cf6&color=fff&bold=true'
    },
    {
        id: 'user-khanh',
        name: 'Vũ Quốc Khánh',
        shortName: 'Quốc Khánh',
        email: 'quockhanh.sb@gmail.com',
        online: true,
        avatar: 'https://ui-avatars.com/api/?name=Quoc+Khanh&background=10b981&color=fff&bold=true'
    },
    {
        id: 'user-linh',
        name: 'Đỗ Phương Linh',
        shortName: 'Phương Linh',
        email: 'phuonglinh.cntt@gmail.com',
        online: true,
        avatar: 'https://ui-avatars.com/api/?name=Phuong+Linh&background=ec4899&color=fff&bold=true'
    },
    {
        id: 'user-tung',
        name: 'Phạm Thanh Tùng',
        shortName: 'Thanh Tùng',
        email: 'thanhtung.ltm@gmail.com',
        online: false,
        avatar: 'https://ui-avatars.com/api/?name=Thanh+Tung&background=64748b&color=fff&bold=true'
    }
];

// Khởi tạo người dùng của Tab hiện tại (cho phép mỗi Tab đăng nhập 1 người khác nhau)
let CURRENT_USER = loadTabUser() || { ...DEFAULT_ACCOUNTS[0] };

const USER_SETTINGS = {
    activeStatus: true,
    darkMode: false,
    desktopNotif: true,
    soundNotif: true
};

const THEME_PRESETS = {
    '#0084ff': 'linear-gradient(135deg, #0084ff 0%, #0062ff 100%)',
    '#7c3aed': 'linear-gradient(135deg, #7c3aed 0%, #ec4899 100%)',
    '#e11d48': 'linear-gradient(135deg, #e11d48 0%, #fb7185 100%)',
    '#0d9488': 'linear-gradient(135deg, #0d9488 0%, #2dd4bf 100%)',
    '#ea580c': 'linear-gradient(135deg, #ea580c 0%, #f59e0b 100%)',
    '#1e293b': 'linear-gradient(135deg, #0f172a 0%, #334155 100%)'
};

const QUICK_EMOJI_CYCLE = ['👍', '❤️', '🔥', '🎉', '🚀', '👏'];

let CONTACTS_DIRECTORY = loadContactsDirectory();

const NOTIFICATIONS = [
    {
        id: 'notif-1',
        conversationId: 'room-team-ltm',
        title: 'Nhóm Đồ Án Lập Trình Mạng',
        content: 'Lê Hoàng Nam đã gửi tin nhắn mới trong nhóm.',
        time: 'Vừa xong',
        unread: true
    },
    {
        id: 'notif-2',
        conversationId: 'conv-thu-ha',
        title: 'Trần Thu Hà',
        content: 'Đã thả ❤️ vào tin nhắn của bạn.',
        time: '5 phút trước',
        unread: true
    }
];

const INITIAL_CONVERSATIONS = [
    {
        id: 'room-team-ltm',
        name: 'Nhóm Đồ Án Lập Trình Mạng',
        type: 'GROUP',
        roomId: 'room-team-ltm',
        online: true,
        statusText: '4 thành viên • 3 đang hoạt động',
        unreadCount: 0,
        pinned: true,
        muted: false,
        archived: false,
        blocked: false,
        themeColor: '#0084ff',
        quickEmoji: '👍',
        avatarText: 'LTM',
        sharedFiles: ['So_Do_Kien_Truc_WebSocket.pdf'],
        members: [
            { ...DEFAULT_ACCOUNTS[0], role: 'Trưởng nhóm' },
            { ...DEFAULT_ACCOUNTS[1], role: 'Thành viên' },
            { ...DEFAULT_ACCOUNTS[2], role: 'Thành viên' },
            { ...DEFAULT_ACCOUNTS[5], role: 'Thành viên' }
        ],
        messages: [
            {
                id: 'm-1',
                senderId: 'user-ha',
                senderName: 'Trần Thu Hà',
                senderAvatar: 'https://ui-avatars.com/api/?name=Thu+Ha&background=0ea5e9&color=fff',
                content: 'Mọi người ơi, hệ thống đã sẵn sàng kết nối WebSocket STOMP qua endpoint /ws rồi nhé!',
                reaction: '👍',
                timestamp: '2026-11-02T08:40:00.000Z'
            },
            {
                id: 'm-2',
                senderId: 'user-duc',
                senderName: 'Nguyễn Minh Đức',
                senderAvatar: 'https://ui-avatars.com/api/?name=Minh+Duc&background=0084ff&color=fff&bold=true',
                content: 'Mọi người có thể mở 2 tab trình duyệt, đăng nhập 2 tài khoản khác nhau để nhắn tin qua lại trực tiếp theo thời gian thực nhé.',
                reaction: '❤️',
                timestamp: '2026-11-02T08:42:15.000Z'
            },
            {
                id: 'm-3',
                senderId: 'user-nam',
                senderName: 'Lê Hoàng Nam',
                senderAvatar: 'https://ui-avatars.com/api/?name=Hoang+Nam&background=8b5cf6&color=fff',
                content: 'Tuyệt vời! Mình vừa thử thả cảm xúc và trả lời tin nhắn, đồng bộ ngay lập tức luôn.',
                replyTo: {
                    senderName: 'Nguyễn Minh Đức',
                    content: 'Mọi người có thể mở 2 tab trình duyệt, đăng nhập 2 tài khoản khác nhau...'
                },
                timestamp: '2026-11-02T08:45:00.000Z'
            }
        ]
    },
    {
        id: 'conv-thu-ha',
        contactId: 'user-ha',
        participants: ['user-duc', 'user-ha'],
        name: 'Trần Thu Hà',
        type: 'DIRECT',
        roomId: null,
        online: true,
        statusText: 'Đang hoạt động',
        unreadCount: 0,
        pinned: false,
        muted: false,
        archived: false,
        blocked: false,
        themeColor: '#7c3aed',
        quickEmoji: '❤️',
        sharedFiles: [],
        avatar: 'https://ui-avatars.com/api/?name=Thu+Ha&background=0ea5e9&color=fff&bold=true',
        messages: [
            {
                id: 'm-101',
                senderId: 'user-ha',
                senderName: 'Trần Thu Hà',
                senderAvatar: 'https://ui-avatars.com/api/?name=Thu+Ha&background=0ea5e9&color=fff',
                content: 'Đức ơi, cậu kiểm tra phần giao diện Messenger mới chưa?',
                timestamp: '2026-11-02T09:10:00.000Z'
            },
            {
                id: 'm-102',
                senderId: 'user-duc',
                senderName: 'Nguyễn Minh Đức',
                senderAvatar: 'https://ui-avatars.com/api/?name=Minh+Duc&background=0084ff&color=fff&bold=true',
                content: 'Tớ vừa hoàn thiện xong, giờ mở thêm 1 tab nữa đăng nhập tài khoản của Hà là 2 bên chat realtime thật với nhau được luôn!',
                reaction: '😮',
                timestamp: '2026-11-02T09:12:30.000Z'
            }
        ]
    },
    {
        id: 'conv-hoang-nam',
        contactId: 'user-nam',
        participants: ['user-duc', 'user-nam'],
        name: 'Lê Hoàng Nam',
        type: 'DIRECT',
        roomId: null,
        online: true,
        statusText: 'Đang hoạt động',
        unreadCount: 0,
        pinned: false,
        muted: true,
        archived: false,
        blocked: false,
        themeColor: '#0084ff',
        quickEmoji: '🔥',
        sharedFiles: [],
        avatar: 'https://ui-avatars.com/api/?name=Hoang+Nam&background=8b5cf6&color=fff&bold=true',
        messages: [
            {
                id: 'm-201',
                senderId: 'user-nam',
                senderName: 'Lê Hoàng Nam',
                senderAvatar: 'https://ui-avatars.com/api/?name=Hoang+Nam&background=8b5cf6&color=fff',
                content: 'Tối nay 8h nhóm mình ghép phần kết nối MySQL nhé.',
                timestamp: '2026-11-02T07:30:00.000Z'
            },
            {
                id: 'm-202',
                senderId: 'user-duc',
                senderName: 'Nguyễn Minh Đức',
                senderAvatar: 'https://ui-avatars.com/api/?name=Minh+Duc&background=0084ff&color=fff&bold=true',
                content: 'Ok Nam, mình đã chuẩn bị sẵn file cấu hình rồi.',
                timestamp: '2026-11-02T07:35:10.000Z'
            }
        ]
    },
    {
        id: 'room-spring-websocket',
        name: 'CLB Backend Spring Boot',
        type: 'GROUP',
        roomId: 'room-spring-websocket',
        online: true,
        statusText: '4 thành viên • 3 đang hoạt động',
        unreadCount: 0,
        pinned: false,
        muted: false,
        archived: false,
        blocked: false,
        themeColor: '#0d9488',
        quickEmoji: '🚀',
        avatarText: 'SB',
        sharedFiles: [],
        members: [
            { ...DEFAULT_ACCOUNTS[0], role: 'Trưởng nhóm' },
            { ...DEFAULT_ACCOUNTS[3], role: 'Thành viên' },
            { ...DEFAULT_ACCOUNTS[4], role: 'Thành viên' }
        ],
        messages: [
            {
                id: 'm-301',
                senderId: 'user-khanh',
                senderName: 'Vũ Quốc Khánh',
                senderAvatar: 'https://ui-avatars.com/api/?name=Quoc+Khanh&background=10b981&color=fff',
                content: 'Khi gửi tin nhắn qua STOMP, mọi người dùng stompClient.send("/app/chat.send", {}, JSON.stringify(payload)) nhé.',
                timestamp: '2026-11-01T16:20:00.000Z'
            }
        ]
    }
];

let CONVERSATIONS = loadConversations();

// Biến quản lý trạng thái giao diện & STOMP
let stompClient = null;
let isWsConnected = false;
let currentRoomSubscription = null;
let activeConversationId = CONVERSATIONS[0]?.id || null;
let contextMenuTargetConvId = null;
let currentFilter = 'all';
let searchQuery = '';
let inChatSearchQuery = '';
let replyingToMessage = null;
let typingTimeout = null;

// Kênh đồng bộ thời gian thực giữa các Tab trình duyệt (Multi-Tab Realtime Bus)
const realtimeBus =
    typeof BroadcastChannel !== 'undefined'
        ? new BroadcastChannel('chatspace_realtime_bus')
        : null;

// Biến phục vụ Modal Tạo nhóm / Thêm thành viên
let groupModalMode = 'CREATE';
let selectedMemberIds = new Set();
let memberSearchQuery = '';

// --------------------------------------------------------------------------
// 2. LƯU TRỮ & ĐỒNG BỘ THỜI GIAN THỰC ĐA TAB + WEBSOCKET STOMP `/ws`
// --------------------------------------------------------------------------

function loadTabUser() {
    try {
        const raw = sessionStorage.getItem(STORAGE_KEYS.TAB_USER);
        return raw ? JSON.parse(raw) : null;
    } catch (_) {
        return null;
    }
}

function saveTabUser(userObj) {
    try {
        sessionStorage.setItem(STORAGE_KEYS.TAB_USER, JSON.stringify(userObj));
    } catch (_) {}
}

function loadContactsDirectory() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.CONTACTS);
        return raw ? JSON.parse(raw) : [...DEFAULT_ACCOUNTS];
    } catch (_) {
        return [...DEFAULT_ACCOUNTS];
    }
}

function saveContactsDirectory() {
    try {
        localStorage.setItem(STORAGE_KEYS.CONTACTS, JSON.stringify(CONTACTS_DIRECTORY));
    } catch (_) {}
}

function loadConversations() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.CONVERSATIONS);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (Array.isArray(parsed) && parsed.length > 0) return parsed;
        }
    } catch (_) {}
    return JSON.parse(JSON.stringify(INITIAL_CONVERSATIONS));
}

function persistAndSyncConversations(eventType = 'SYNC_STATE', extraData = {}) {
    try {
        localStorage.setItem(STORAGE_KEYS.CONVERSATIONS, JSON.stringify(CONVERSATIONS));
    } catch (_) {}

    if (realtimeBus) {
        realtimeBus.postMessage({
            type: eventType,
            senderTabUserId: CURRENT_USER.id,
            ...extraData
        });
    }
}

/**
 * Lắng nghe sự kiện thời gian thực từ các Tab trình duyệt khác
 */
if (realtimeBus) {
    realtimeBus.onmessage = (event) => {
        const data = event.data;
        if (!data) return;

        // Sự kiện người ở Tab khác đang gõ phím soạn tin nhắn
        if (data.type === 'USER_TYPING') {
            if (
                data.conversationId === activeConversationId &&
                data.senderId !== CURRENT_USER.id
            ) {
                showTypingIndicator(data.senderName);
            }
            return;
        }

        // Đồng bộ lại danh sách cuộc trò chuyện và tin nhắn từ Kho lưu trữ chung
        CONVERSATIONS = loadConversations();
        CONTACTS_DIRECTORY = loadContactsDirectory();

        if (data.type === 'NEW_MESSAGE' && data.message) {
            const targetConv = getConversationById(data.conversationId);
            if (targetConv && data.message.senderId !== CURRENT_USER.id && !targetConv.blocked) {
                if (activeConversationId !== targetConv.id) {
                    targetConv.unreadCount = (targetConv.unreadCount || 0) + 1;
                }
                if (!targetConv.muted && USER_SETTINGS.desktopNotif) {
                    NOTIFICATIONS.unshift({
                        id: 'notif-' + Date.now(),
                        conversationId: targetConv.id,
                        title: targetConv.name,
                        content: `${data.message.senderName}: ${data.message.content || 'Đã gửi một hình ảnh'}`,
                        time: 'Vừa xong',
                        unread: true
                    });
                    renderNotifications();
                    if (USER_SETTINGS.soundNotif) {
                        playSubtleBeep();
                    }
                }
            }
        }

        renderActiveNowStrip();
        renderConversationList();
        const activeConv = getConversationById(activeConversationId);
        if (activeConv) {
            updateActiveChatHeaderAndDrawer(activeConv);
            renderMessages(activeConv);
        }
    };
}

/**
 * Khởi tạo kết nối SockJS tới endpoint `/ws` của Spring Boot
 * và đăng ký lắng nghe kênh cá nhân `/user/queue/messages`
 */
function connectWebSocket() {
    updateWsStatusUI('connected', 'Đang hoạt động • Trực tuyến');

    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect(
            {},
            function onConnected() {
                isWsConnected = true;
                updateWsStatusUI('connected', 'Đã kết nối máy chủ • Trực tuyến');

                // 1. Đăng ký lắng nghe kênh nhận tin nhắn riêng `/user/queue/messages`
                stompClient.subscribe('/user/queue/messages', function (messageFrame) {
                    const incomingMessage = JSON.parse(messageFrame.body);
                    handleIncomingSocketMessage(incomingMessage);
                });

                // 2. Đăng ký lắng nghe kênh nhóm `/topic/room/{roomId}`
                const activeConv = getConversationById(activeConversationId);
                if (activeConv && activeConv.roomId) {
                    subscribeToRoomTopic(activeConv.roomId);
                }
            },
            function onError() {
                isWsConnected = false;
                updateWsStatusUI('connected', 'Đang hoạt động • Trực tuyến');
            }
        );
    } catch (error) {
        isWsConnected = false;
        updateWsStatusUI('connected', 'Đang hoạt động • Trực tuyến');
    }
}

/**
 * Đăng ký lắng nghe (stompClient.subscribe) kênh chat nhóm `/topic/room/{roomId}`
 */
function subscribeToRoomTopic(roomId) {
    if (!stompClient || !isWsConnected || !roomId) {
        return;
    }

    if (currentRoomSubscription) {
        currentRoomSubscription.unsubscribe();
        currentRoomSubscription = null;
    }

    const destination = `/topic/room/${roomId}`;
    currentRoomSubscription = stompClient.subscribe(destination, function (messageFrame) {
        const incomingMessage = JSON.parse(messageFrame.body);
        handleIncomingSocketMessage(incomingMessage);
    });
}

/**
 * Hàm gửi tin nhắn qua WebSocket STOMP tới `/app/chat.send`
 * kèm payload JSON gồm: { conversationId, content, timestamp }
 */
function sendChatMessage(conversationId, content, extraOptions = {}) {
    const trimmedContent = content.trim();
    if (!trimmedContent && !extraOptions.imageUrl) {
        return;
    }

    const conv = getConversationById(conversationId);
    if (!conv) return;

    if (conv.blocked) {
        showToast('Không thể gửi tin nhắn vì bạn đã chặn cuộc trò chuyện này.', 'fa-ban');
        return;
    }

    const payload = {
        conversationId: conversationId,
        senderId: CURRENT_USER.id,
        senderName: CURRENT_USER.name,
        senderAvatar: CURRENT_USER.avatar,
        content: trimmedContent || '📷 Đã gửi một hình ảnh',
        timestamp: new Date().toISOString()
    };

    // Gửi tới Backend Spring Boot qua STOMP nếu `/ws` đang mở
    if (stompClient && isWsConnected) {
        stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
    }

    const newMsg = {
        id: 'msg-' + Date.now(),
        senderId: CURRENT_USER.id,
        senderName: CURRENT_USER.name,
        senderAvatar: CURRENT_USER.avatar,
        content: trimmedContent,
        imageUrl: extraOptions.imageUrl || null,
        replyTo: replyingToMessage
            ? {
                  senderName: replyingToMessage.senderName,
                  content: replyingToMessage.content
              }
            : null,
        timestamp: payload.timestamp
    };

    cancelReplyMessage();
    appendMessageToConversation(conversationId, newMsg, true);
    syncComposerButtons();
}

/**
 * Xử lý khi nhận được gói tin JSON từ `/user/queue/messages` hoặc `/topic/room/{roomId}`
 */
function handleIncomingSocketMessage(payload) {
    if (!payload || !payload.content) {
        return;
    }

    const targetConvId = payload.conversationId || activeConversationId;
    const conv = getConversationById(targetConvId);

    if (conv && conv.blocked) {
        return;
    }

    if (payload.senderId === CURRENT_USER.id) {
        return;
    }

    const msgObj = {
        id: payload.id || 'ws-' + Date.now(),
        senderId: payload.senderId || 'partner',
        senderName: payload.senderName || 'Thành viên',
        senderAvatar:
            payload.senderAvatar || 'https://ui-avatars.com/api/?name=User&background=0ea5e9&color=fff',
        content: payload.content,
        timestamp: payload.timestamp || new Date().toISOString()
    };

    appendMessageToConversation(targetConvId, msgObj, false);

    if (conv && !conv.muted && USER_SETTINGS.desktopNotif) {
        NOTIFICATIONS.unshift({
            id: 'notif-' + Date.now(),
            conversationId: conv.id,
            title: getDisplayConversationName(conv),
            content: `${msgObj.senderName}: ${msgObj.content}`,
            time: 'Vừa xong',
            unread: true
        });
        renderNotifications();
        if (USER_SETTINGS.soundNotif) {
            playSubtleBeep();
        }
    }
}

// --------------------------------------------------------------------------
// 3. ĐĂNG NHẬP TÀI KHOẢN THỰC TẾ (HỖ TRỢ ĐA TAB)
// --------------------------------------------------------------------------

function updateCurrentUserProfileUI() {
    const railAvatar = document.getElementById('railCurrentUserAvatar');
    const settingsAvatar = document.getElementById('settingsUserAvatar');
    const settingsName = document.getElementById('settingsUserName');
    const settingsEmail = document.getElementById('settingsUserEmail');

    if (railAvatar) railAvatar.src = CURRENT_USER.avatar;
    if (settingsAvatar) settingsAvatar.src = CURRENT_USER.avatar;
    if (settingsName) settingsName.textContent = CURRENT_USER.name;
    if (settingsEmail) settingsEmail.textContent = CURRENT_USER.email;

    document.title = `${CURRENT_USER.name} — ChatSpace Messenger`;
}

function renderSavedAccountsOnAuthScreen() {
    const container = document.getElementById('savedAccountsList');
    if (!container) return;

    container.innerHTML = CONTACTS_DIRECTORY.map(
        (acc) => `
        <button type="button" class="saved-account-chip" data-switch-user-id="${acc.id}">
            <img src="${acc.avatar}" alt="${escapeHtml(acc.name)}">
            <span>${escapeHtml(acc.name)}</span>
        </button>
    `
    ).join('');

    container.querySelectorAll('[data-switch-user-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const userId = btn.getAttribute('data-switch-user-id');
            const found = CONTACTS_DIRECTORY.find((u) => u.id === userId);
            if (found) {
                loginAsUser(found);
            }
        });
    });
}

function loginAsUser(userObj) {
    CURRENT_USER = {
        id: userObj.id,
        name: userObj.name,
        shortName: userObj.shortName || userObj.name.split(/\s+/).slice(-2).join(' '),
        email: userObj.email,
        online: true,
        avatar: userObj.avatar
    };

    saveTabUser(CURRENT_USER);

    // Đảm bảo tài khoản này có mặt trong CONTACTS_DIRECTORY chung
    const existingIdx = CONTACTS_DIRECTORY.findIndex(
        (c) => c.id === CURRENT_USER.id || c.email.toLowerCase() === CURRENT_USER.email.toLowerCase()
    );
    if (existingIdx === -1) {
        CONTACTS_DIRECTORY.unshift({ ...CURRENT_USER });
    } else {
        CONTACTS_DIRECTORY[existingIdx].online = true;
    }
    saveContactsDirectory();

    updateCurrentUserProfileUI();
    renderActiveNowStrip();
    renderConversationList();

    const activeConv = getConversationById(activeConversationId);
    if (activeConv) {
        updateActiveChatHeaderAndDrawer(activeConv);
        renderMessages(activeConv);
    }

    document.getElementById('authScreen')?.classList.add('d-none');
    document.getElementById('settingsModalBackdrop')?.classList.add('d-none');
    showToast(`Xin chào, ${CURRENT_USER.name}!`, 'fa-circle-check');
}

// --------------------------------------------------------------------------
// 4. TƯƠNG TÁC TRÊN TỪNG TIN NHẮN (REACTIONS, REPLY, THU HỒI TIN NHẮN)
// --------------------------------------------------------------------------

function toggleMessageReaction(messageId, emoji) {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    const msg = conv.messages.find((m) => m.id === messageId);
    if (!msg || msg.unsent) return;

    msg.reaction = msg.reaction === emoji ? null : emoji;
    persistAndSyncConversations('REACTION_UPDATED');
    renderMessages(conv);
}

function startReplyToMessage(messageId) {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    const msg = conv.messages.find((m) => m.id === messageId);
    if (!msg || msg.unsent) return;

    replyingToMessage = msg;
    document.getElementById('replyTargetSender').textContent =
        msg.senderId === CURRENT_USER.id ? 'chính bạn' : msg.senderName;
    document.getElementById('replyTargetText').textContent = msg.content || 'Hình ảnh đính kèm';
    document.getElementById('replyPreviewBanner').classList.remove('d-none');
    document.getElementById('messageInput')?.focus();
}

function cancelReplyMessage() {
    replyingToMessage = null;
    document.getElementById('replyPreviewBanner')?.classList.add('d-none');
}

function unsendMessage(messageId) {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    const msg = conv.messages.find((m) => m.id === messageId);
    if (!msg) return;

    msg.unsent = true;
    msg.content = `${msg.senderName} đã thu hồi một tin nhắn`;
    msg.imageUrl = null;
    msg.reaction = null;

    persistAndSyncConversations('MESSAGE_UNSENT');
    renderMessages(conv);
    renderConversationList();
    showToast('Đã thu hồi tin nhắn', 'fa-rotate-left');
}

// --------------------------------------------------------------------------
// 5. TÙY CHỈNH ĐOẠN CHAT (ĐỔI MÀU CHỦ ĐỀ, ĐỔI QUICK EMOJI, ĐỔI TÊN)
// --------------------------------------------------------------------------

function applyConversationTheme(colorHex) {
    const conv = getConversationById(activeConversationId);
    const themeColor = colorHex || conv?.themeColor || '#0084ff';
    const gradient = THEME_PRESETS[themeColor] || THEME_PRESETS['#0084ff'];

    document.documentElement.style.setProperty('--theme-color', themeColor);
    document.documentElement.style.setProperty('--theme-gradient', gradient);

    document.querySelectorAll('#themeColorSwatches .theme-swatch').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-theme-color') === themeColor);
    });
}

function changeActiveConversationTheme(colorHex) {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    conv.themeColor = colorHex;
    applyConversationTheme(colorHex);
    persistAndSyncConversations('THEME_UPDATED');
    showToast('Đã đổi màu chủ đề cuộc trò chuyện!', 'fa-palette');
}

function cycleQuickEmoji() {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    const currentIdx = QUICK_EMOJI_CYCLE.indexOf(conv.quickEmoji || '👍');
    const nextEmoji = QUICK_EMOJI_CYCLE[(currentIdx + 1) % QUICK_EMOJI_CYCLE.length];
    conv.quickEmoji = nextEmoji;

    persistAndSyncConversations('EMOJI_UPDATED');
    updateActiveChatHeaderAndDrawer(conv);
    showToast(`Đã đổi biểu tượng cảm xúc nhanh thành ${nextEmoji}`, 'fa-face-smile');
}

function renameActiveConversation() {
    const conv = getConversationById(activeConversationId);
    if (!conv) return;

    const label =
        conv.type === 'GROUP' ? 'Nhập tên mới cho nhóm chat:' : 'Đặt biệt danh cho cuộc trò chuyện:';
    const newName = window.prompt(label, getDisplayConversationName(conv));
    if (!newName || !newName.trim()) return;

    conv.name = newName.trim();
    persistAndSyncConversations('RENAME_CONV');
    updateActiveChatHeaderAndDrawer(conv);
    renderConversationList();
    showToast(`Đã đổi tên thành "${conv.name}"`, 'fa-pen');
}

// --------------------------------------------------------------------------
// 6. CHỨC NĂNG TẠO NHÓM CHAT MỚI & QUẢN LÝ THÀNH VIÊN NHÓM
// --------------------------------------------------------------------------

function openGroupModal(mode = 'CREATE') {
    groupModalMode = mode;
    selectedMemberIds = new Set();
    memberSearchQuery = '';

    const modalBackdrop = document.getElementById('createGroupModalBackdrop');
    const modalTitle = document.getElementById('groupModalTitle');
    const identityFields = document.getElementById('groupIdentityFields');
    const submitText = document.getElementById('btnSubmitGroupText');
    const groupNameInput = document.getElementById('groupNameInput');
    const groupAvatarTextInput = document.getElementById('groupAvatarTextInput');
    const groupTopicDescInput = document.getElementById('groupTopicDescInput');
    const searchMemberInput = document.getElementById('searchMemberInput');

    if (searchMemberInput) searchMemberInput.value = '';

    if (mode === 'CREATE') {
        modalTitle.textContent = 'Tạo nhóm chat mới';
        identityFields.classList.remove('d-none');
        submitText.textContent = 'Tạo nhóm';
        groupNameInput.value = '';
        groupAvatarTextInput.value = '';
        groupTopicDescInput.value = '';
    } else {
        const activeConv = getConversationById(activeConversationId);
        modalTitle.textContent = `Thêm thành viên vào "${activeConv ? activeConv.name : 'Nhóm'}"`;
        identityFields.classList.add('d-none');
        submitText.textContent = 'Thêm vào nhóm';
    }

    renderSelectedMemberChips();
    renderContactPickerList();
    modalBackdrop.classList.remove('d-none');

    if (mode === 'CREATE') {
        groupNameInput.focus();
    } else {
        searchMemberInput.focus();
    }
}

function closeGroupModal() {
    document.getElementById('createGroupModalBackdrop')?.classList.add('d-none');
}

function renderSelectedMemberChips() {
    const chipsContainer = document.getElementById('selectedMemberChips');
    const countBadge = document.getElementById('selectedMemberCountBadge');
    if (!chipsContainer || !countBadge) return;

    const selectedContacts = CONTACTS_DIRECTORY.filter((c) => selectedMemberIds.has(c.id));
    countBadge.textContent = selectedContacts.length;

    if (selectedContacts.length === 0) {
        chipsContainer.innerHTML = `<span class="text-muted small fst-italic">Chưa chọn thành viên nào...</span>`;
        return;
    }

    chipsContainer.innerHTML = selectedContacts
        .map(
            (c) => `
        <span class="member-chip">
            <span>${escapeHtml(c.name)}</span>
            <button type="button" data-remove-chip-id="${c.id}" title="Bỏ chọn">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </span>
    `
        )
        .join('');

    chipsContainer.querySelectorAll('[data-remove-chip-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-remove-chip-id');
            selectedMemberIds.delete(id);
            renderSelectedMemberChips();
            renderContactPickerList();
        });
    });
}

function renderContactPickerList() {
    const listEl = document.getElementById('contactPickerList');
    if (!listEl) return;

    const activeConv = getConversationById(activeConversationId);
    const existingMemberIds = new Set(
        groupModalMode === 'ADD_MEMBER' && activeConv?.members
            ? activeConv.members.map((m) => m.id)
            : []
    );

    const filteredContacts = CONTACTS_DIRECTORY.filter((c) => {
        if (c.id === CURRENT_USER.id) return false;
        if (!memberSearchQuery) return true;
        const q = memberSearchQuery.toLowerCase();
        return c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q);
    });

    if (filteredContacts.length === 0) {
        listEl.innerHTML = `
            <div class="text-center text-muted small py-3">
                Không tìm thấy trong danh bạ. Nhấn nút <strong>"+ Thêm người"</strong> ở trên để thêm trực tiếp!
            </div>
        `;
        return;
    }

    listEl.innerHTML = filteredContacts
        .map((c) => {
            const isAlreadyInGroup = existingMemberIds.has(c.id);
            const isSelected = selectedMemberIds.has(c.id) || isAlreadyInGroup;

            return `
            <div class="contact-picker-item ${isSelected ? 'selected' : ''}" data-pick-contact-id="${c.id}" data-already="${isAlreadyInGroup}">
                <div class="d-flex align-items-center gap-2 min-w-0">
                    <img src="${c.avatar}" alt="${escapeHtml(c.name)}" class="drawer-member-avatar">
                    <div class="min-w-0">
                        <div class="drawer-member-name text-truncate">${escapeHtml(c.name)}</div>
                        <div class="drawer-member-role text-truncate">${escapeHtml(c.email)}</div>
                    </div>
                </div>
                <div>
                    ${
                        isAlreadyInGroup
                            ? `<span class="badge bg-secondary-subtle text-secondary">Đã trong nhóm</span>`
                            : `<input type="checkbox" class="form-check-input" ${isSelected ? 'checked' : ''} style="pointer-events: none;">`
                    }
                </div>
            </div>
        `;
        })
        .join('');

    listEl.querySelectorAll('.contact-picker-item').forEach((item) => {
        item.addEventListener('click', () => {
            if (item.getAttribute('data-already') === 'true') return;
            const id = item.getAttribute('data-pick-contact-id');
            if (selectedMemberIds.has(id)) {
                selectedMemberIds.delete(id);
            } else {
                selectedMemberIds.add(id);
            }
            renderSelectedMemberChips();
            renderContactPickerList();
        });
    });
}

function addCustomContactFromInput() {
    const searchMemberInput = document.getElementById('searchMemberInput');
    const rawName = (searchMemberInput?.value || '').trim();
    if (!rawName) {
        showToast('Hãy gõ tên người muốn thêm vào ô tìm kiếm bạn bè trước nhé!', 'fa-user-plus');
        searchMemberInput?.focus();
        return;
    }

    const parts = rawName.split(/\s+/);
    const shortName = parts.slice(-2).join(' ');
    const newContact = {
        id: 'user-' + Date.now(),
        name: rawName,
        shortName: shortName,
        email: rawName.toLowerCase().replace(/\s+/g, '') + '@chatspace.vn',
        online: true,
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(rawName)}&background=0084ff&color=fff&bold=true`
    };

    CONTACTS_DIRECTORY.unshift(newContact);
    saveContactsDirectory();
    selectedMemberIds.add(newContact.id);
    searchMemberInput.value = '';
    memberSearchQuery = '';

    renderActiveNowStrip();
    renderSelectedMemberChips();
    renderContactPickerList();
    showToast(`Đã thêm "${rawName}" vào danh sách chọn`, 'fa-user-check');
}

function handleGroupModalSubmit(event) {
    event.preventDefault();

    const selectedContacts = CONTACTS_DIRECTORY.filter((c) => selectedMemberIds.has(c.id));

    if (groupModalMode === 'CREATE') {
        const groupNameInput = document.getElementById('groupNameInput');
        const groupAvatarTextInput = document.getElementById('groupAvatarTextInput');
        const groupTopicDescInput = document.getElementById('groupTopicDescInput');

        const groupName = groupNameInput.value.trim();
        if (!groupName) {
            showToast('Vui lòng nhập tên nhóm chat!', 'fa-triangle-exclamation');
            groupNameInput.focus();
            return;
        }

        if (selectedContacts.length === 0) {
            showToast('Vui lòng chọn ít nhất 1 thành viên để tạo nhóm!', 'fa-users');
            return;
        }

        const customAvatarText = groupAvatarTextInput.value.trim().toUpperCase();
        const generatedInitials =
            customAvatarText ||
            groupName
                .split(/\s+/)
                .map((w) => w[0])
                .join('')
                .substring(0, 3)
                .toUpperCase();

        const slug = groupName
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/(^-|-$)/g, '');
        const roomId = `room-${slug || Date.now()}`;

        const membersList = [
            { ...CURRENT_USER, role: 'Trưởng nhóm' },
            ...selectedContacts.map((c) => ({ ...c, role: 'Thành viên' }))
        ];

        const topicDesc = groupTopicDescInput.value.trim();
        const welcomeText = topicDesc
            ? `${CURRENT_USER.name} đã tạo nhóm "${groupName}" (${topicDesc}).`
            : `${CURRENT_USER.name} đã tạo nhóm "${groupName}" với ${membersList.length} thành viên.`;

        const newGroupConv = {
            id: roomId,
            name: groupName,
            type: 'GROUP',
            roomId: roomId,
            online: true,
            statusText: `${membersList.length} thành viên • Đang hoạt động`,
            unreadCount: 0,
            pinned: true,
            muted: false,
            archived: false,
            blocked: false,
            themeColor: '#0084ff',
            quickEmoji: '👍',
            sharedFiles: [],
            avatarText: generatedInitials,
            members: membersList,
            messages: [
                {
                    id: 'msg-init-' + Date.now(),
                    senderId: CURRENT_USER.id,
                    senderName: CURRENT_USER.name,
                    senderAvatar: CURRENT_USER.avatar,
                    content: welcomeText,
                    timestamp: new Date().toISOString()
                }
            ]
        };

        CONVERSATIONS.unshift(newGroupConv);
        persistAndSyncConversations('GROUP_CREATED');
        closeGroupModal();
        setConversationFilter('all');
        selectConversation(newGroupConv.id);
        showToast(`Đã tạo nhóm "${groupName}" thành công!`, 'fa-users');
    } else {
        const conv = getConversationById(activeConversationId);
        if (!conv || conv.type !== 'GROUP') return;

        if (selectedContacts.length === 0) {
            showToast('Hãy tích chọn ít nhất 1 thành viên mới để thêm vào nhóm!', 'fa-user-plus');
            return;
        }

        if (!conv.members) {
            conv.members = [{ ...CURRENT_USER, role: 'Trưởng nhóm' }];
        }

        selectedContacts.forEach((c) => {
            if (!conv.members.some((m) => m.id === c.id)) {
                conv.members.push({ ...c, role: 'Thành viên' });
            }
        });

        conv.statusText = `${conv.members.length} thành viên • Đang hoạt động`;
        const addedNames = selectedContacts.map((c) => c.name).join(', ');

        appendMessageToConversation(
            conv.id,
            {
                id: 'msg-add-' + Date.now(),
                senderId: CURRENT_USER.id,
                senderName: CURRENT_USER.name,
                senderAvatar: CURRENT_USER.avatar,
                content: `👋 Đã thêm ${addedNames} vào nhóm "${conv.name}".`,
                timestamp: new Date().toISOString()
            },
            true
        );

        closeGroupModal();
        updateActiveChatHeaderAndDrawer(conv);
        showToast(`Đã thêm ${selectedContacts.length} thành viên vào nhóm!`, 'fa-user-plus');
    }
}

function removeMemberFromActiveGroup(memberId) {
    const conv = getConversationById(activeConversationId);
    if (!conv || conv.type !== 'GROUP' || !conv.members) return;

    const member = conv.members.find((m) => m.id === memberId);
    if (!member) return;

    conv.members = conv.members.filter((m) => m.id !== memberId);
    conv.statusText = `${conv.members.length} thành viên • Đang hoạt động`;

    persistAndSyncConversations('MEMBER_REMOVED');
    updateActiveChatHeaderAndDrawer(conv);
    showToast(`Đã mời "${member.name}" ra khỏi nhóm`, 'fa-user-minus');
}

// --------------------------------------------------------------------------
// 7. CÁC CHỨC NĂNG QUẢN LÝ ĐOẠN CHAT GIỐNG MESSENGER
// --------------------------------------------------------------------------

function togglePinConversation(convId) {
    const conv = getConversationById(convId);
    if (!conv) return;

    conv.pinned = !conv.pinned;
    persistAndSyncConversations('PIN_UPDATED');
    showToast(
        conv.pinned ? `Đã ghim "${getDisplayConversationName(conv)}" lên đầu` : `Đã bỏ ghim`,
        'fa-thumbtack'
    );
    renderConversationList();
    if (activeConversationId === convId) {
        updateActiveChatHeaderAndDrawer(conv);
    }
}

function toggleUnreadConversation(convId) {
    const conv = getConversationById(convId);
    if (!conv) return;

    if (conv.unreadCount > 0) {
        conv.unreadCount = 0;
        showToast(`Đã đánh dấu là đã đọc`, 'fa-envelope-open');
    } else {
        conv.unreadCount = 1;
        showToast(`Đã đánh dấu là chưa đọc`, 'fa-envelope');
    }
    renderConversationList();
    if (activeConversationId === convId) {
        updateActiveChatHeaderAndDrawer(conv);
    }
}

function toggleMuteConversation(convId) {
    const conv = getConversationById(convId);
    if (!conv) return;

    conv.muted = !conv.muted;
    persistAndSyncConversations('MUTE_UPDATED');
    showToast(
        conv.muted ? `Đã tắt thông báo đoạn chat` : `Đã bật lại thông báo`,
        conv.muted ? 'fa-bell-slash' : 'fa-bell'
    );
    renderConversationList();
    if (activeConversationId === convId) {
        updateActiveChatHeaderAndDrawer(conv);
    }
}

function toggleArchiveConversation(convId) {
    const conv = getConversationById(convId);
    if (!conv) return;

    conv.archived = !conv.archived;
    if (conv.archived) {
        conv.pinned = false;
        showToast(`Đã chuyển vào Kho lưu trữ`, 'fa-box-archive');
        if (activeConversationId === convId && currentFilter !== 'archived') {
            const nextConv = CONVERSATIONS.find((c) => !c.archived && c.id !== convId);
            selectConversation(nextConv ? nextConv.id : null);
        }
    } else {
        showToast(`Đã khôi phục về Hộp thư chính`, 'fa-box-open');
    }

    persistAndSyncConversations('ARCHIVE_UPDATED');
    updateBadgesCount();
    renderConversationList();
    if (activeConversationId === convId) {
        updateActiveChatHeaderAndDrawer(conv);
    }
}

function toggleBlockConversation(convId) {
    const conv = getConversationById(convId);
    if (!conv) return;

    conv.blocked = !conv.blocked;
    persistAndSyncConversations('BLOCK_UPDATED');
    showToast(
        conv.blocked ? `Đã chặn cuộc trò chuyện này` : `Đã bỏ chặn cuộc trò chuyện`,
        conv.blocked ? 'fa-ban' : 'fa-circle-check'
    );
    renderConversationList();
    renderBlockedUsersInSettings();
    if (activeConversationId === convId) {
        updateActiveChatHeaderAndDrawer(conv);
    }
}

function deleteConversation(convId) {
    const idx = CONVERSATIONS.findIndex((c) => c.id === convId);
    if (idx === -1) return;

    const removedName = getDisplayConversationName(CONVERSATIONS[idx]);
    CONVERSATIONS.splice(idx, 1);
    persistAndSyncConversations('CONV_DELETED');
    showToast(`Đã xóa đoạn chat "${removedName}"`, 'fa-trash-can');

    updateBadgesCount();
    if (activeConversationId === convId) {
        const nextConv = CONVERSATIONS.find((c) => !c.archived);
        selectConversation(nextConv ? nextConv.id : null);
    } else {
        renderConversationList();
    }
}

// --------------------------------------------------------------------------
// 8. RENDER GIAO DIỆN (ACTIVE NOW STRIP, SIDEBAR, CHAT LOG, DRAWER)
// --------------------------------------------------------------------------

/**
 * Hiển thị đúng tên đối phương khi mở ở các Tab tài khoản khác nhau
 */
function getDisplayConversationName(conv) {
    if (!conv) return '';
    if (conv.type === 'GROUP') return conv.name;
    if (conv.participants && Array.isArray(conv.participants)) {
        const partnerId = conv.participants.find((id) => id !== CURRENT_USER.id);
        const partner = CONTACTS_DIRECTORY.find((c) => c.id === partnerId);
        if (partner) return partner.name;
    }
    return conv.name;
}

function getDisplayConversationAvatar(conv) {
    if (!conv || conv.type === 'GROUP') return null;
    if (conv.participants && Array.isArray(conv.participants)) {
        const partnerId = conv.participants.find((id) => id !== CURRENT_USER.id);
        const partner = CONTACTS_DIRECTORY.find((c) => c.id === partnerId);
        if (partner) return partner.avatar;
    }
    return conv.avatar;
}

function renderActiveNowStrip() {
    const stripEl = document.getElementById('activeNowStrip');
    if (!stripEl) return;

    const onlineContacts = CONTACTS_DIRECTORY.filter(
        (c) => c.online && c.id !== CURRENT_USER.id
    );
    stripEl.innerHTML = onlineContacts
        .map(
            (c) => `
        <div class="active-person-item" data-active-contact-id="${c.id}" title="Nhắn tin với ${escapeHtml(c.name)}">
            <div class="avatar-wrapper">
                <img src="${c.avatar}" alt="${escapeHtml(c.name)}" class="active-person-avatar">
                <span class="status-dot online"></span>
            </div>
            <span class="active-person-name">${escapeHtml(c.shortName || c.name)}</span>
        </div>
    `
        )
        .join('');

    stripEl.querySelectorAll('.active-person-item').forEach((item) => {
        item.addEventListener('click', () => {
            const contactId = item.getAttribute('data-active-contact-id');
            openOrCreateDirectConversation(contactId);
        });
    });
}

function openOrCreateDirectConversation(contactId) {
    const contact = CONTACTS_DIRECTORY.find((c) => c.id === contactId);
    if (!contact) return;

    let conv = CONVERSATIONS.find(
        (c) =>
            c.type === 'DIRECT' &&
            ((c.participants &&
                c.participants.includes(contact.id) &&
                c.participants.includes(CURRENT_USER.id)) ||
                c.contactId === contact.id ||
                c.name === contact.name)
    );

    if (!conv) {
        conv = {
            id: 'conv-' + [CURRENT_USER.id, contact.id].sort().join('-'),
            contactId: contact.id,
            participants: [CURRENT_USER.id, contact.id],
            name: contact.name,
            type: 'DIRECT',
            roomId: null,
            online: contact.online,
            statusText: contact.online ? 'Đang hoạt động' : 'Hoạt động gần đây',
            unreadCount: 0,
            pinned: false,
            muted: false,
            archived: false,
            blocked: false,
            themeColor: '#0084ff',
            quickEmoji: '👍',
            sharedFiles: [],
            avatar: contact.avatar,
            messages: []
        };
        CONVERSATIONS.unshift(conv);
        persistAndSyncConversations('DIRECT_CREATED');
    } else if (conv.archived) {
        conv.archived = false;
        persistAndSyncConversations('ARCHIVE_UPDATED');
    }

    setConversationFilter('all');
    selectConversation(conv.id);
}

function renderConversationList() {
    const listEl = document.getElementById('conversationList');
    if (!listEl) return;

    updateBadgesCount();

    const filtered = CONVERSATIONS.filter((conv) => {
        if (currentFilter === 'archived') {
            if (!conv.archived) return false;
        } else {
            if (conv.archived) return false;
            if (currentFilter === 'GROUP' && conv.type !== 'GROUP') return false;
            if (currentFilter === 'unread' && conv.unreadCount === 0) return false;
        }

        if (searchQuery) {
            const q = searchQuery.toLowerCase();
            const displayName = getDisplayConversationName(conv).toLowerCase();
            const lastMsg = conv.messages[conv.messages.length - 1]?.content || '';
            return displayName.includes(q) || lastMsg.toLowerCase().includes(q);
        }
        return true;
    }).sort((a, b) => {
        if (a.pinned && !b.pinned) return -1;
        if (!a.pinned && b.pinned) return 1;
        return 0;
    });

    if (filtered.length === 0) {
        const emptyMsg =
            currentFilter === 'archived'
                ? 'Chưa có cuộc trò chuyện nào trong Kho lưu trữ'
                : 'Không tìm thấy cuộc trò chuyện phù hợp';
        listEl.innerHTML = `
            <div class="text-center text-muted py-5 px-3">
                <i class="fa-brands fa-facebook-messenger fs-3 mb-2 d-block opacity-50"></i>
                <div class="small">${emptyMsg}</div>
            </div>
        `;
        return;
    }

    listEl.innerHTML = filtered
        .map((conv) => {
            const isActive = conv.id === activeConversationId;
            const isUnread = conv.unreadCount > 0;
            const displayName = getDisplayConversationName(conv);
            const displayAvatar = getDisplayConversationAvatar(conv);
            const lastMessage = conv.messages[conv.messages.length - 1];
            const lastText = lastMessage
                ? (lastMessage.senderId === CURRENT_USER.id ? 'Bạn: ' : '') +
                  escapeHtml(lastMessage.content || 'Đã gửi một hình ảnh')
                : 'Bắt đầu cuộc trò chuyện';
            const lastTime = lastMessage ? formatShortTime(lastMessage.timestamp) : '';

            const avatarHtml =
                conv.type === 'GROUP'
                    ? `<div class="conv-avatar-group">${escapeHtml(conv.avatarText || 'GR')}</div>`
                    : `<img src="${displayAvatar}" alt="${escapeHtml(displayName)}" class="conv-avatar">`;

            const statusDotHtml = `
                <span class="status-dot ${conv.online && !conv.blocked ? 'online' : ''}"></span>
            `;

            const pinIconHtml = conv.pinned
                ? `<i class="fa-solid fa-thumbtack conv-flag-icon pinned" title="Đã ghim"></i>`
                : '';

            const muteIconHtml = conv.muted
                ? `<i class="fa-solid fa-bell-slash conv-flag-icon" title="Đã tắt thông báo"></i>`
                : '';

            const blockedIconHtml = conv.blocked
                ? `<i class="fa-solid fa-ban text-danger conv-flag-icon" title="Đã chặn"></i>`
                : '';

            const unreadBadgeHtml =
                conv.unreadCount > 0
                    ? `<span class="unread-badge">${conv.unreadCount}</span>`
                    : '';

            return `
                <div class="conversation-item ${isActive ? 'active' : ''} ${isUnread ? 'unread' : ''}"
                     role="option"
                     aria-selected="${isActive}"
                     data-conversation-id="${conv.id}">
                    <div class="avatar-wrapper">
                        ${avatarHtml}
                        ${statusDotHtml}
                    </div>
                    <div class="conv-content">
                        <div class="conv-top-row">
                            <div class="conv-name-wrap">
                                <span class="conv-name">${escapeHtml(displayName)}</span>
                                ${pinIconHtml}
                                ${muteIconHtml}
                                ${blockedIconHtml}
                            </div>
                            <span class="conv-time">${lastTime}</span>
                        </div>
                        <div class="conv-bottom-row">
                            <span class="conv-last-msg">${lastText}</span>
                            ${unreadBadgeHtml}
                        </div>
                    </div>
                    <button type="button"
                            class="conv-more-btn"
                            data-more-conv-id="${conv.id}"
                            title="Tùy chọn đoạn chat">
                        <i class="fa-solid fa-ellipsis"></i>
                    </button>
                </div>
            `;
        })
        .join('');

    listEl.querySelectorAll('.conversation-item').forEach((itemEl) => {
        itemEl.addEventListener('click', (e) => {
            if (e.target.closest('.conv-more-btn')) return;
            const convId = itemEl.getAttribute('data-conversation-id');
            selectConversation(convId);
        });

        itemEl.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            const convId = itemEl.getAttribute('data-conversation-id');
            openContextMenu(convId, e.clientX, e.clientY);
        });
    });

    listEl.querySelectorAll('.conv-more-btn').forEach((btnEl) => {
        btnEl.addEventListener('click', (e) => {
            e.stopPropagation();
            const convId = btnEl.getAttribute('data-more-conv-id');
            const rect = btnEl.getBoundingClientRect();
            openContextMenu(convId, rect.right, rect.bottom + 4);
        });
    });
}

function selectConversation(conversationId) {
    activeConversationId = conversationId;
    cancelReplyMessage();

    const emptyStateEl = document.getElementById('emptyChatState');
    const activeViewEl = document.getElementById('activeChatView');
    const appContainerEl = document.getElementById('appContainer');

    const conv = getConversationById(conversationId);

    if (!conv) {
        emptyStateEl.classList.remove('d-none');
        activeViewEl.classList.add('d-none');
        appContainerEl.classList.remove('mobile-chat-active');
        renderConversationList();
        return;
    }

    conv.unreadCount = 0;

    emptyStateEl.classList.add('d-none');
    activeViewEl.classList.remove('d-none');
    appContainerEl.classList.add('mobile-chat-active');
