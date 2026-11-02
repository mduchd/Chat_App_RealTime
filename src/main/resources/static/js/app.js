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
