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
    TAB_USER: 'chatspace_tab_user_v2',
    CONVERSATIONS: 'chatspace_conversations_v2',
    CONTACTS: 'chatspace_contacts_v2',
    FRIENDSHIPS: 'chatspace_friendships_v2'
};

// Xóa dữ liệu mẫu của các phiên bản trước, giữ dữ liệu do người dùng tạo.
const SAMPLE_USER_IDS = new Set(['user-duc', 'user-an', 'user-minh', 'user-linh', 'user-ha', 'user-nam']);
const SAMPLE_CONVERSATION_IDS = new Set(['conv-an-nguyen', 'conv-binh-minh', 'room-lap-trinh-mang', 'conv-quynh-linh']);
const SAMPLE_MESSAGE_IDS = new Set(['m-1', 'm-2', 'm-3', 'm-101', 'm-201', 'm-202', 'm-301']);

function removeOldSampleData() {
    try {
        const savedUser = JSON.parse(sessionStorage.getItem(STORAGE_KEYS.TAB_USER) || 'null');
        if (savedUser && SAMPLE_USER_IDS.has(savedUser.id)) sessionStorage.removeItem(STORAGE_KEYS.TAB_USER);
        const contacts = JSON.parse(localStorage.getItem(STORAGE_KEYS.CONTACTS) || 'null');
        if (Array.isArray(contacts)) localStorage.setItem(STORAGE_KEYS.CONTACTS, JSON.stringify(contacts.filter(c => !SAMPLE_USER_IDS.has(c.id))));
        const friendships = JSON.parse(localStorage.getItem(STORAGE_KEYS.FRIENDSHIPS) || 'null');
        if (friendships && Array.isArray(friendships.accepted) && Array.isArray(friendships.requests)) {
            friendships.accepted = friendships.accepted.filter(pair => !pair.some(id => SAMPLE_USER_IDS.has(id)));
            friendships.requests = friendships.requests.filter(req => req.id !== 'freq-init-1');
            localStorage.setItem(STORAGE_KEYS.FRIENDSHIPS, JSON.stringify(friendships));
        }
        for (const key of Object.keys(localStorage)) {
            if (!key.startsWith(STORAGE_KEYS.CONVERSATIONS + ':')) continue;
            const conversations = JSON.parse(localStorage.getItem(key) || 'null');
            if (!Array.isArray(conversations)) continue;
            const cleaned = conversations.map(conv => ({
                ...conv,
                messages: (conv.messages || []).filter(message => !SAMPLE_CONVERSATION_IDS.has(conv.id) || !SAMPLE_MESSAGE_IDS.has(message.id))
            })).filter(conv => !SAMPLE_CONVERSATION_IDS.has(conv.id) || conv.messages.length > 0);
            localStorage.setItem(key, JSON.stringify(cleaned));
        }
    } catch (_) { }
}
removeOldSampleData();

let CURRENT_USER = loadTabUser() || { id: '', name: '', email: '' };

const USER_SETTINGS = {
    activeStatus: true,
    darkMode: false,
    desktopNotif: true,
    soundNotif: true
};

const THEME_PRESETS = {
    '#1d72b8': '#1d72b8',
    '#3797f0': '#1d72b8',
    '#0084ff': '#1d72b8',
    '#0d9488': '#0d9488',
    '#6366f1': '#6366f1',
    '#e11d48': '#e11d48',
    '#334155': '#334155'
};

const QUICK_EMOJI_CYCLE = ['👍', '❤️', '🔥', '🎉', '🚀', '👏'];

let CONTACTS_DIRECTORY = loadContactsDirectory();

const NOTIFICATIONS = [];
let CONVERSATIONS = loadConversations();

let FRIENDSHIPS = { accepted: [], requests: [] };
let serverAuthenticated = false;
let friendRefreshTimer = null;
let currentFriendsTab = 'discover';
let friendSearchFilter = '';

// Biến quản lý trạng thái giao diện & STOMP
let stompClient = null;
let isWsConnected = false;
let reconnectTimer = null;
let currentRoomSubscription = null;
let activeConversationId = CONVERSATIONS[0]?.id || null;
let contextMenuTargetConvId = null;
let currentFilter = 'all';
let searchQuery = '';
let inChatSearchQuery = '';
let replyingToMessage = null;
let typingTimeout = null;

// Lấy URL Backend linh hoạt: Chạy local hoặc trỏ tới domain Backend deploy ngoài
function getBackendUrl() {
    if (window.BACKEND_API_URL) return window.BACKEND_API_URL.replace(/\/+$/, '');
    const stored = localStorage.getItem('BACKEND_API_URL');
    if (stored) return stored.replace(/\/+$/, '');
    if (['localhost', '127.0.0.1'].includes(window.location.hostname)) {
        return '';
    }
    return (window.BACKEND_URL_DEFAULT || 'https://chatapprealtime-production-ea7b4569.up.railway.app').replace(/\/+$/, '');
}

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
    } catch (_) { }
}

function loadContactsDirectory() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.CONTACTS);
        return raw ? JSON.parse(raw) : [];
    } catch (_) {
        return [];
    }
}

// --------------------------------------------------------------------------
// QUẢN LÝ MÃ OTP XÁC THỰC (GMAIL / SĐT) & ĐIỀU HƯỚNG TAB XÁC THỰC
// --------------------------------------------------------------------------
const PENDING_AUTH_OTPS = new Map();
const OTP_COOLDOWN_TIMERS = new Map();

function sendAuthOtp(destination, buttonEl, hintEl) {
    if (!destination) return null;
    const destKey = destination.trim().toLowerCase();
    const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + 5 * 60 * 1000;

    PENDING_AUTH_OTPS.set(destKey, {
        code: otpCode,
        expiresAt: expiresAt
    });

    showToast(`Mã xác thực OTP của bạn là: ${otpCode} (Hết hạn sau 5 phút)`, 'fa-key', 9000);
    if (hintEl) {
        hintEl.innerHTML = `<span class="text-success"><i class="fa-solid fa-circle-check me-1"></i>Đã gửi OTP tới <strong>${escapeHtml(destination)}</strong>. Mã thử nghiệm: <strong class="badge bg-primary text-white">${otpCode}</strong></span>`;
    }

    if (buttonEl) {
        if (OTP_COOLDOWN_TIMERS.has(buttonEl.id)) {
            clearInterval(OTP_COOLDOWN_TIMERS.get(buttonEl.id));
        }
        let countdown = 60;
        buttonEl.disabled = true;
        const originalText = buttonEl.textContent;
        buttonEl.textContent = `Gửi lại (${countdown}s)`;

        const timerId = setInterval(() => {
            countdown -= 1;
            if (countdown <= 0) {
                clearInterval(timerId);
                OTP_COOLDOWN_TIMERS.delete(buttonEl.id);
                buttonEl.disabled = false;
                buttonEl.textContent = originalText;
            } else {
                buttonEl.textContent = `Gửi lại (${countdown}s)`;
            }
        }, 1000);
        OTP_COOLDOWN_TIMERS.set(buttonEl.id, timerId);
    }

    return otpCode;
}

function verifyAuthOtp(destination, enteredCode) {
    if (!destination || !enteredCode) return false;
    const cleanCode = enteredCode.trim();
    if (cleanCode === '123456') return true; // Cho phép mã thử nghiệm nhanh

    const destKey = destination.trim().toLowerCase();
    const record = PENDING_AUTH_OTPS.get(destKey);
    if (!record) return false;
    if (Date.now() > record.expiresAt) {
        PENDING_AUTH_OTPS.delete(destKey);
        return false;
    }
    if (record.code === cleanCode) {
        PENDING_AUTH_OTPS.delete(destKey);
        return true;
    }
    return false;
}

let currentAuthTab = 'login';
let currentLoginMethod = 'email';
let currentRegMethod = 'email';
let currentForgotMethod = 'email';

function setAuthTab(tabName) {
    currentAuthTab = tabName;
    document.querySelectorAll('[data-auth-tab]').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-auth-tab') === tabName);
    });

    const viewLogin = document.getElementById('authViewLogin');
    const viewReg = document.getElementById('authViewRegister');
    const viewForgot = document.getElementById('authViewForgot');

    if (viewLogin) viewLogin.classList.toggle('d-none', tabName !== 'login');
    if (viewReg) viewReg.classList.toggle('d-none', tabName !== 'register');
    if (viewForgot) viewForgot.classList.toggle('d-none', tabName !== 'forgot');
}

function setLoginMethod(method) {
    currentLoginMethod = method;
    document.querySelectorAll('[data-login-method]').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-login-method') === method);
    });
    const emailGroup = document.getElementById('loginEmailGroup');
    const phoneGroup = document.getElementById('loginPhoneGroup');
    if (method === 'email') {
        emailGroup?.classList.remove('d-none');
        phoneGroup?.classList.add('d-none');
        document.getElementById('loginEmailInput')?.focus();
    } else {
        emailGroup?.classList.add('d-none');
        phoneGroup?.classList.remove('d-none');
        document.getElementById('loginPhoneInput')?.focus();
    }
}

function setRegMethod(method) {
    currentRegMethod = method;
    document.querySelectorAll('[data-reg-method]').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-reg-method') === method);
    });
    const emailGroup = document.getElementById('regEmailGroup');
    const phoneGroup = document.getElementById('regPhoneGroup');
    if (method === 'email') {
        emailGroup?.classList.remove('d-none');
        phoneGroup?.classList.add('d-none');
        document.getElementById('regEmailInput')?.focus();
    } else {
        emailGroup?.classList.add('d-none');
        phoneGroup?.classList.remove('d-none');
        document.getElementById('regPhoneInput')?.focus();
    }
}

function setForgotMethod(method) {
    currentForgotMethod = method;
    document.querySelectorAll('[data-forgot-method]').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-forgot-method') === method);
    });
    const emailGroup = document.getElementById('forgotEmailGroup');
    const phoneGroup = document.getElementById('forgotPhoneGroup');
    if (method === 'email') {
        emailGroup?.classList.remove('d-none');
        phoneGroup?.classList.add('d-none');
        document.getElementById('forgotEmailInput')?.focus();
    } else {
        emailGroup?.classList.add('d-none');
        phoneGroup?.classList.remove('d-none');
        document.getElementById('forgotPhoneInput')?.focus();
    }
}

function saveContactsDirectory() {
    try {
        localStorage.setItem(STORAGE_KEYS.CONTACTS, JSON.stringify(CONTACTS_DIRECTORY));
    } catch (_) { }
}

function loadConversations() {
    try {
        const raw = localStorage.getItem(conversationStorageKey());
        if (raw) {
            const parsed = JSON.parse(raw);
            if (Array.isArray(parsed)) return parsed;
        }
    } catch (_) { }
    return [];
}

function conversationStorageKey() {
    return `${STORAGE_KEYS.CONVERSATIONS}:${CURRENT_USER.id}`;
}

function loadFriendships() {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.FRIENDSHIPS);
        if (raw) {
            const parsed = JSON.parse(raw);
            if (parsed && Array.isArray(parsed.accepted) && Array.isArray(parsed.requests)) {
                return parsed;
            }
        }
    } catch (_) {}
    return { accepted: [], requests: [] };
}

function saveAndSyncFriendships(eventType = 'FRIENDS_UPDATED', extraData = {}) {
    try {
        localStorage.setItem(STORAGE_KEYS.FRIENDSHIPS, JSON.stringify(FRIENDSHIPS));
    } catch (_) {}

    if (realtimeBus) {
        realtimeBus.postMessage({
            type: eventType,
            senderTabUserId: CURRENT_USER.id,
            ...extraData
        });
    }
}

function persistAndSyncConversations(eventType = 'SYNC_STATE', extraData = {}) {
    try {
        localStorage.setItem(conversationStorageKey(), JSON.stringify(CONVERSATIONS));
    } catch (_) { }

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

        // Đồng bộ lại danh sách cuộc trò chuyện, danh bạ và kết bạn từ Kho lưu trữ chung
        CONVERSATIONS = loadConversations();
        if (!serverAuthenticated) CONTACTS_DIRECTORY = loadContactsDirectory();

        if (data.type === 'FRIEND_REQUEST_SENT' && data.toId === CURRENT_USER.id) {
            NOTIFICATIONS.unshift({
                id: 'notif-freq-' + Date.now(),
                conversationId: null,
                title: 'Lời mời kết bạn mới',
                content: `${data.fromName} đã gửi cho bạn lời mời kết bạn.`,
                time: 'Vừa xong',
                unread: true
            });
            renderNotifications();
            showToast(`${data.fromName} vừa gửi lời mời kết bạn!`, 'fa-user-plus');
            if (USER_SETTINGS.soundNotif) playSubtleBeep();
        }

        if (data.type === 'FRIEND_REQUEST_ACCEPTED' && data.fromId === CURRENT_USER.id) {
            NOTIFICATIONS.unshift({
                id: 'notif-facc-' + Date.now(),
                conversationId: data.conversationId || null,
                title: 'Kết bạn thành công',
                content: `${data.accepterName} đã đồng ý kết bạn với bạn.`,
                time: 'Vừa xong',
                unread: true
            });
            renderNotifications();
            showToast(`${data.accepterName} đã chấp nhận lời mời kết bạn!`, 'fa-user-check');
        }

        updateFriendBadges();
        renderFriendsModalList();

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
    clearTimeout(reconnectTimer);
    const previousClient = stompClient;
    stompClient = null;
    isWsConnected = false;
    currentRoomSubscription = null;
    if (previousClient?.connected) previousClient.disconnect();
    updateWsStatusUI('disconnected', 'Đang kết nối...');

    try {
        const backendBase = getBackendUrl();
        const socket = new SockJS(`${backendBase}/ws-sockjs?user=${encodeURIComponent(CURRENT_USER.id)}`);
        const client = Stomp.over(socket);
        stompClient = client;
        client.debug = null;

        client.connect(
            {},
            function onConnected() {
                if (client !== stompClient) { client.disconnect(); return; }
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
                if (client !== stompClient) return;
                isWsConnected = false;
                updateWsStatusUI('disconnected', 'Mất kết nối • Đang thử lại');
                reconnectTimer = setTimeout(connectWebSocket, 3000);
            }
        );
    } catch (error) {
        isWsConnected = false;
        updateWsStatusUI('disconnected', 'Không thể kết nối • Đang thử lại');
        reconnectTimer = setTimeout(connectWebSocket, 3000);
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
        return false;
    }

    if (conv.type === 'DIRECT' && !extraOptions.imageUrl) {
        const recipient = conv.participants?.find(id => id !== CURRENT_USER.id);
        if (!recipient) {
            showToast('Không xác định được người nhận.', 'fa-triangle-exclamation');
            return false;
        }
        if (!stompClient || !isWsConnected) {
            showToast('Chưa kết nối máy chủ. Hãy thử lại sau.', 'fa-triangle-exclamation');
            return false;
        }
        stompClient.send('/app/chat.private', {}, JSON.stringify({ recipient, content: trimmedContent }));
        cancelReplyMessage();
        return true;
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
    return true;
}

/**
 * Xử lý khi nhận được gói tin JSON từ `/user/queue/messages` hoặc `/topic/room/{roomId}`
 */
function handleIncomingSocketMessage(payload) {
    if (!payload || !payload.content) {
        return;
    }

    if (payload.sender && payload.recipient) {
        if (payload.sender !== CURRENT_USER.id && payload.recipient !== CURRENT_USER.id) return;
        const partnerId = payload.sender === CURRENT_USER.id ? payload.recipient : payload.sender;
        const contact = CONTACTS_DIRECTORY.find(c => c.id === partnerId) || {
            id: partnerId, name: 'Người dùng', avatar: ''
        };
        let direct = CONVERSATIONS.find(c => c.type === 'DIRECT' &&
            c.participants?.includes(CURRENT_USER.id) && c.participants.includes(partnerId));
        if (!direct) {
            direct = {
                id: 'conv-' + [CURRENT_USER.id, partnerId].sort().join('-'),
                contactId: partnerId, participants: [CURRENT_USER.id, partnerId],
                name: contact.name, type: 'DIRECT', roomId: null, online: false,
                statusText: '', unreadCount: 0, pinned: false, muted: false,
                archived: false, blocked: false, themeColor: '#0084ff',
                quickEmoji: '👍', sharedFiles: [], avatar: contact.avatar, messages: []
            };
            CONVERSATIONS.unshift(direct);
        }
        if (direct.blocked || direct.messages.some(m => m.id === payload.id)) return;
        const sender = CONTACTS_DIRECTORY.find(c => c.id === payload.sender);
        const message = {
            id: payload.id, senderId: payload.sender,
            senderName: sender?.name || (payload.sender === CURRENT_USER.id ? CURRENT_USER.name : 'Người dùng'),
            senderAvatar: sender?.avatar || '', content: payload.content,
            timestamp: payload.sentAt || new Date().toISOString()
        };
        appendMessageToConversation(direct.id, message, false);
        if (realtimeBus) realtimeBus.postMessage({type: 'SYNC_STATE', senderTabUserId: CURRENT_USER.id});
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

function getInitials(name, customText) {
    if (customText && customText.trim()) return customText.trim().substring(0, 3).toUpperCase();
    if (!name) return 'CS';
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function updateCurrentUserProfileUI() {
    const topbarInitials = document.getElementById('topbarUserInitials');
    const sidebarUsername = document.getElementById('sidebarCurrentUsername');
    const settingsInitials = document.getElementById('settingsUserInitials');
    const settingsName = document.getElementById('settingsUserName');
    const settingsEmail = document.getElementById('settingsUserEmail');

    const initials = getInitials(CURRENT_USER.name, CURRENT_USER.initials);
    if (topbarInitials) topbarInitials.textContent = initials;
    if (sidebarUsername) sidebarUsername.textContent = CURRENT_USER.shortName || CURRENT_USER.name || 'Đăng nhập';
    if (settingsInitials) settingsInitials.textContent = initials;
    if (settingsName) settingsName.textContent = CURRENT_USER.name;
    if (settingsEmail) settingsEmail.textContent = CURRENT_USER.email;

    document.title = `CHATSPACE — ${CURRENT_USER.name}`;
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

function loginAsUser(userObj, authenticatedByServer = false) {
    if (!authenticatedByServer) {
        serverAuthenticated = false;
        FRIENDSHIPS = { accepted: [], requests: [] };
    }
    CURRENT_USER = {
        id: userObj.id,
        name: userObj.name,
        shortName: userObj.shortName || userObj.name.split(/\s+/).slice(-2).join(' '),
        email: userObj.email || '',
        phone: userObj.phone || '',
        password: userObj.password || '123456',
        online: true,
        avatar: userObj.avatar
    };

    saveTabUser(CURRENT_USER);
    document.getElementById('authScreen')?.classList.add('d-none');
    CONVERSATIONS = loadConversations();
    activeConversationId = CONVERSATIONS[0]?.id || null;
    connectWebSocket();

    // Đảm bảo tài khoản này có mặt trong CONTACTS_DIRECTORY chung
    const existingIdx = CONTACTS_DIRECTORY.findIndex(
        (c) => c.id === CURRENT_USER.id ||
               (c.email && CURRENT_USER.email && c.email.toLowerCase() === CURRENT_USER.email.toLowerCase()) ||
               (c.phone && CURRENT_USER.phone && c.phone === CURRENT_USER.phone)
    );
    if (existingIdx === -1) {
        CONTACTS_DIRECTORY.unshift({ ...CURRENT_USER });
    } else {
        CONTACTS_DIRECTORY[existingIdx] = {
            ...CONTACTS_DIRECTORY[existingIdx],
            ...CURRENT_USER,
            online: true
        };
    }
    saveContactsDirectory();

    updateCurrentUserProfileUI();
    updateFriendBadges();
    renderFriendsModalList();
    renderActiveNowStrip();
    renderConversationList();

    const activeConv = getConversationById(activeConversationId);
    if (activeConv) {
        updateActiveChatHeaderAndDrawer(activeConv);
        renderMessages(activeConv);
    } else {
        selectConversation(null);
    }

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
    const rawColor = colorHex || conv?.themeColor || '#1d72b8';
    const normalizedColor =
        rawColor === '#0084ff' || rawColor === '#3797f0' ? '#1d72b8' : rawColor;

    document.documentElement.style.setProperty('--theme-color', normalizedColor);

    document.querySelectorAll('#themeColorSwatches .theme-swatch').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-theme-color') === normalizedColor);
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
                    ${isAlreadyInGroup
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

        if (selectedContacts.length === 0) {
            showToast('Vui lòng chọn ít nhất 1 người nhận!', 'fa-users');
            return;
        }

        const rawGroupName = groupNameInput.value.trim();

        // Nếu chỉ chọn 1 người và không đặt tên nhóm -> Mở chat cá nhân 1-1 chuẩn Instagram Direct
        if (selectedContacts.length === 1 && !rawGroupName) {
            closeGroupModal();
            openOrCreateDirectConversation(selectedContacts[0].id);
            return;
        }

        // Nếu chọn từ 2 người trở lên mà chưa nhập tên nhóm -> Tự tạo tên nhóm từ tên các thành viên
        const groupName =
            rawGroupName ||
            [CURRENT_USER.shortName || CURRENT_USER.name, ...selectedContacts.map((c) => c.shortName || c.name)].join(', ');

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
                    isSystemNotice: true,
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
                isSystemNotice: true,
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
        if (partner?.name) return partner.name;
        if (conv.name === partnerId) return 'Người dùng';
    }
    if (conv.name === conv.contactId || /^\d{15,}$/.test(conv.name || '')) return 'Người dùng';
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
                <div class="ig-story-ring">
                    <img src="${c.avatar}" alt="${escapeHtml(c.name)}" class="active-person-avatar">
                </div>
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
            (c.participants?.length
                ? c.participants.includes(contact.id) && c.participants.includes(CURRENT_USER.id)
                : c.contactId === contact.id || c.name === contact.name)
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
                <i class="fa-regular fa-paper-plane fs-3 mb-2 d-block opacity-50"></i>
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
            const lastMessage = conv.messages[conv.messages.length - 1];
            const previewText = lastMessage
                ? lastMessage.sidebarPreview
                    ? escapeHtml(lastMessage.sidebarPreview)
                    : escapeHtml(lastMessage.content || 'Đã gửi một hình ảnh')
                : 'Bắt đầu cuộc trò chuyện';

            const initialsText =
                conv.type === 'GROUP'
                    ? conv.avatarText || 'GR'
                    : getInitials(displayName, conv.avatarText);

            const avatarHtml = `<div class="conv-avatar-circle ${isActive ? 'active-avatar' : ''}">${escapeHtml(initialsText)}</div>`;

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
                    </div>
                    <div class="conv-content">
                        <div class="conv-top-row">
                            <div class="conv-name-wrap">
                                <span class="conv-name">${escapeHtml(displayName)}</span>
                                ${pinIconHtml}
                                ${muteIconHtml}
                                ${blockedIconHtml}
                            </div>
                        </div>
                        <div class="conv-bottom-row">
                            <span class="conv-last-msg">${previewText}</span>
                            ${unreadBadgeHtml}
                        </div>
                    </div>
                    <button type="button"
                            class="conv-more-btn"
                            data-more-conv-id="${conv.id}"
                            title="Tùy chọn hội thoại">
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

    applyConversationTheme(conv.themeColor || '#0084ff');
    updateActiveChatHeaderAndDrawer(conv);
    renderConversationList();
    renderMessages(conv);
    syncComposerButtons();

    if (window.innerWidth >= 768 && !conv.blocked) {
        document.getElementById('messageInput')?.focus();
    }
}

function updateActiveChatHeaderAndDrawer(conv) {
    if (!conv) return;

    const isGroup = conv.type === 'GROUP';
    const displayName = getDisplayConversationName(conv);
    const initialsText = isGroup
        ? conv.avatarText || 'GR'
        : getInitials(displayName, conv.avatarText);

    const avatarHtml = `<div class="conv-avatar-circle active-avatar">${escapeHtml(initialsText)}</div>`;

    const headerAvatarWrap = document.getElementById('chatHeaderAvatarWrapper');
    if (headerAvatarWrap) headerAvatarWrap.innerHTML = avatarHtml;
    document.getElementById('chatHeaderName').textContent = displayName;
    const headerBadgeEl = document.getElementById('chatHeaderBadge');
    if (headerBadgeEl) {
        headerBadgeEl.textContent = isGroup ? 'Nhóm chat' : 'Cá nhân';
    }
    document.getElementById('chatHeaderStatusText').textContent = conv.blocked
        ? 'Đã chặn'
        : conv.statusText;

    document.getElementById('chatHeaderPinnedIcon').classList.toggle('d-none', !conv.pinned);
    document.getElementById('chatHeaderMutedIcon').classList.toggle('d-none', !conv.muted);
    document.getElementById('btnHeaderAddMember').classList.toggle('d-none', !isGroup);

    const headerDot = document.getElementById('chatHeaderDot');
    headerDot.className = conv.online && !conv.blocked ? 'status-dot online' : 'status-dot';

    if (isGroup && conv.roomId) {
        subscribeToRoomTopic(conv.roomId);
    }

    document.getElementById('blockedBanner').classList.toggle('d-none', !conv.blocked);
    document.getElementById('chatInputFooter').classList.toggle('d-none', conv.blocked);

    document.getElementById('drawerAvatar').innerHTML = avatarHtml;
    document.getElementById('drawerName').textContent = displayName;
    document.getElementById('drawerStatus').textContent = conv.blocked
        ? 'Tài khoản đang bị chặn'
        : conv.statusText;

    document.getElementById('drawerPinLabel').textContent = conv.pinned ? 'Bỏ ghim' : 'Ghim';
    document.getElementById('drawerMuteLabel').textContent = conv.muted ? 'Bật TB' : 'Tắt TB';
    document.getElementById('drawerArchiveLabel').textContent = conv.archived
        ? 'Khôi phục'
        : 'Lưu trữ';
    document.getElementById('drawerRenameLabel').textContent = isGroup
        ? 'Đổi tên nhóm chat'
        : 'Đặt biệt danh';
    document.getElementById('drawerBlockLabel').textContent = conv.blocked
        ? isGroup
            ? 'Bỏ chặn nhóm'
            : 'Bỏ chặn người dùng'
        : isGroup
            ? 'Rời & Chặn nhóm'
            : 'Chặn người dùng';

    const sharedFilesEl = document.getElementById('drawerSharedFiles');
    if (conv.sharedFiles && conv.sharedFiles.length > 0) {
        sharedFilesEl.innerHTML = conv.sharedFiles
            .map(
                (f) => `
            <div class="d-flex align-items-center gap-2 py-1 text-truncate">
                <i class="fa-regular fa-file-lines text-primary"></i>
                <span class="text-truncate">${escapeHtml(f)}</span>
            </div>
        `
            )
            .join('');
    } else {
        sharedFilesEl.textContent = 'Chưa có tệp đính kèm nào';
    }

    const drawerGroupSection = document.getElementById('drawerGroupSection');
    drawerGroupSection.classList.toggle('d-none', !isGroup);

    if (isGroup) {
        const members = conv.members || [{ ...CURRENT_USER, role: 'Trưởng nhóm' }];
        document.getElementById('drawerMemberCount').textContent = members.length;
        const membersListEl = document.getElementById('drawerMembersList');

        membersListEl.innerHTML = members
            .map(
                (m) => `
            <div class="drawer-member-item">
                <div class="d-flex align-items-center gap-2 min-w-0">
                    <img src="${m.avatar}" alt="${escapeHtml(m.name)}" class="drawer-member-avatar">
                    <div class="min-w-0">
                        <div class="drawer-member-name text-truncate">${escapeHtml(m.name)}</div>
                        <div class="drawer-member-role">${escapeHtml(m.role || 'Thành viên')}</div>
                    </div>
                </div>
                ${m.id !== CURRENT_USER.id
                        ? `<button type="button" class="btn-remove-member" data-remove-member-id="${m.id}" title="Mời khỏi nhóm">
                               <i class="fa-solid fa-user-xmark"></i>
                           </button>`
                        : `<span class="badge bg-primary-subtle text-primary" style="font-size: 0.65rem;">Bạn</span>`
                    }
            </div>
        `
            )
            .join('');

        membersListEl.querySelectorAll('[data-remove-member-id]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const memberId = btn.getAttribute('data-remove-member-id');
                removeMemberFromActiveGroup(memberId);
            });
        });
    }
}

function renderMessages(conversation) {
    const chatLogEl = document.getElementById('chatLog');
    if (!chatLogEl || !conversation) return;

    const visibleMessages = conversation.messages.filter((msg) => {
        if (!inChatSearchQuery) return true;
        return (msg.content || '').toLowerCase().includes(inChatSearchQuery.toLowerCase());
    });

    if (visibleMessages.length === 0) {
        chatLogEl.innerHTML = `
            <div class="text-center text-muted my-auto py-4">
                <div class="small">Hãy gửi tin nhắn đầu tiên để bắt đầu trò chuyện!</div>
            </div>
        `;
        return;
    }

    const messagesHtml = visibleMessages
        .map((msg) => {
            const isSystem =
                msg.isSystemNotice ||
                String(msg.id || '').startsWith('msg-friend-') ||
                String(msg.id || '').startsWith('msg-init-') ||
                String(msg.id || '').startsWith('msg-add-');

            if (isSystem) {
                return `
                    <div class="system-notice-row">
                        <span class="system-notice-text">${escapeHtml(msg.content)}</span>
                    </div>
                `;
            }

            const isSentByMe = msg.senderId === CURRENT_USER.id;
            const rowClass = isSentByMe ? 'sent' : 'received';
            const timeFormatted = msg.displayTime || formatFullTime(msg.timestamp);

            const senderNameHtml =
                !isSentByMe && conversation.type === 'GROUP'
                    ? `<div class="msg-sender-name">${escapeHtml(msg.senderName)}</div>`
                    : '';

            const quotedBoxHtml = msg.replyTo
                ? `<div class="msg-quoted-box">
                       <div class="fw-semibold"><i class="fa-solid fa-reply me-1"></i>${escapeHtml(msg.replyTo.senderName)}</div>
                       <div class="text-truncate">${escapeHtml(msg.replyTo.content)}</div>
                   </div>`
                : '';

            const imageHtml = msg.imageUrl
                ? `<img src="${msg.imageUrl}" alt="Hình ảnh đính kèm" class="msg-attached-image">`
                : '';

            const reactionHtml = msg.reaction
                ? `<span class="msg-reaction-pill">${msg.reaction}</span>`
                : '';

            const hoverToolbarHtml = !msg.unsent
                ? `<div class="msg-hover-toolbar">
                       <button type="button" class="msg-tool-btn" data-react-id="${msg.id}" data-emoji="❤️" title="Thả tim">❤️</button>
                       <button type="button" class="msg-tool-btn" data-react-id="${msg.id}" data-emoji="😆" title="Haha">😆</button>
                       <button type="button" class="msg-tool-btn" data-react-id="${msg.id}" data-emoji="😮" title="Wow">😮</button>
                       <button type="button" class="msg-tool-btn" data-react-id="${msg.id}" data-emoji="👍" title="Thích">👍</button>
                       <button type="button" class="msg-tool-btn" data-reply-id="${msg.id}" title="Trả lời">
                           <i class="fa-solid fa-reply"></i>
                       </button>
                       ${isSentByMe
                    ? `<button type="button" class="msg-tool-btn text-danger" data-unsend-id="${msg.id}" title="Thu hồi tin nhắn">
                                      <i class="fa-regular fa-trash-can"></i>
                                  </button>`
                    : ''
                }
                   </div>`
                : '';

            return `
                <div class="message-row ${rowClass}">
                    ${senderNameHtml}
                    ${quotedBoxHtml}
                    <div class="message-bubble-wrap">
                        <div class="message-bubble ${msg.unsent ? 'unsent' : ''}" data-dblclick-msg-id="${msg.id}" title="Nhấn đúp để thả ❤️">
                            ${imageHtml}
                            ${msg.content ? `<div>${escapeHtml(msg.content)}</div>` : ''}
                            <div class="bubble-time">${timeFormatted}</div>
                            ${reactionHtml}
                        </div>
                        ${hoverToolbarHtml}
                    </div>
                </div>
            `;
        })
        .join('');

    chatLogEl.innerHTML = messagesHtml;

    chatLogEl.querySelectorAll('[data-dblclick-msg-id]').forEach((bubbleEl) => {
        bubbleEl.addEventListener('dblclick', () => {
            const msgId = bubbleEl.getAttribute('data-dblclick-msg-id');
            toggleMessageReaction(msgId, '❤️');
        });
    });

    chatLogEl.querySelectorAll('[data-react-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            toggleMessageReaction(
                btn.getAttribute('data-react-id'),
                btn.getAttribute('data-emoji')
            );
        });
    });

    chatLogEl.querySelectorAll('[data-reply-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            startReplyToMessage(btn.getAttribute('data-reply-id'));
        });
    });

    chatLogEl.querySelectorAll('[data-unsend-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            unsendMessage(btn.getAttribute('data-unsend-id'));
        });
    });

    chatLogEl.scrollTop = chatLogEl.scrollHeight;
}

function openContextMenu(convId, x, y) {
    const conv = getConversationById(convId);
    if (!conv) return;

    contextMenuTargetConvId = convId;
    const menuEl = document.getElementById('conversationContextMenu');

    document.getElementById('ctxUnreadText').textContent =
        conv.unreadCount > 0 ? 'Đánh dấu là đã đọc' : 'Đánh dấu là chưa đọc';
    document.getElementById('ctxPinText').textContent = conv.pinned
        ? 'Bỏ ghim đoạn chat'
        : 'Ghim đoạn chat';
    document.getElementById('ctxMuteText').textContent = conv.muted
        ? 'Bật thông báo'
        : 'Tắt thông báo';
    document.getElementById('ctxArchiveText').textContent = conv.archived
        ? 'Bỏ lưu trữ đoạn chat'
        : 'Lưu trữ đoạn chat';
    document.getElementById('ctxBlockText').textContent = conv.blocked ? 'Bỏ chặn' : 'Chặn';

    menuEl.classList.remove('d-none');

    const menuWidth = 230;
    const menuHeight = 250;
    const left = Math.min(x, window.innerWidth - menuWidth - 12);
    const top = Math.min(y, window.innerHeight - menuHeight - 12);

    menuEl.style.left = `${Math.max(8, left)}px`;
    menuEl.style.top = `${Math.max(8, top)}px`;
}

function closeContextMenu() {
    document.getElementById('conversationContextMenu')?.classList.add('d-none');
    contextMenuTargetConvId = null;
}

function renderNotifications() {
    const badgeEl = document.getElementById('notificationBadge');
    const listEl = document.getElementById('notificationList');
    if (!badgeEl || !listEl) return;

    const unreadCount = NOTIFICATIONS.filter((n) => n.unread).length;
    badgeEl.textContent = unreadCount;
    badgeEl.classList.toggle('d-none', unreadCount === 0);

    if (NOTIFICATIONS.length === 0) {
        listEl.innerHTML = `<div class="p-4 text-center text-muted small">Chưa có thông báo nào</div>`;
        return;
    }

    listEl.innerHTML = NOTIFICATIONS.map(
        (n) => `
        <div class="notif-item ${n.unread ? 'unread' : ''}" data-notif-id="${n.id}" data-conv-id="${n.conversationId}">
            <div class="notif-icon">
                <i class="fa-regular fa-heart"></i>
            </div>
            <div class="flex-grow-1 min-w-0">
                <div class="notif-text">
                    <strong>${escapeHtml(n.title)}</strong>: ${escapeHtml(n.content)}
                </div>
                <div class="notif-time">${escapeHtml(n.time)}</div>
            </div>
        </div>
    `
    ).join('');

    listEl.querySelectorAll('.notif-item').forEach((item) => {
        item.addEventListener('click', () => {
            const notifId = item.getAttribute('data-notif-id');
            const convId = item.getAttribute('data-conv-id');
            const notif = NOTIFICATIONS.find((x) => x.id === notifId);
            if (notif) notif.unread = false;

            renderNotifications();
            document.getElementById('notificationPopover')?.classList.add('d-none');
            if (convId) selectConversation(convId);
        });
    });
}

function renderBlockedUsersInSettings() {
    const blockedConvs = CONVERSATIONS.filter((c) => c.blocked);
    const countLabel = document.getElementById('blockedCountLabel');
    const listEl = document.getElementById('blockedUsersList');
    if (!countLabel || !listEl) return;

    countLabel.textContent = blockedConvs.length;

    if (blockedConvs.length === 0) {
        listEl.innerHTML = `<div class="small text-secondary py-2">Hiện không có người dùng nào bị chặn.</div>`;
        return;
    }

    listEl.innerHTML = blockedConvs
        .map(
            (c) => `
        <div class="blocked-user-row">
            <span class="fw-semibold">${escapeHtml(getDisplayConversationName(c))}</span>
            <button type="button" class="btn btn-sm btn-outline-primary py-0 px-2" data-unblock-id="${c.id}">
                Bỏ chặn
            </button>
        </div>
    `
        )
        .join('');

    listEl.querySelectorAll('[data-unblock-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-unblock-id');
            toggleBlockConversation(id);
        });
    });
}

function appendMessageToConversation(conversationId, messageObj, broadcastToOtherTabs = true) {
    const conv = getConversationById(conversationId);
    if (!conv) return;

    conv.messages.push(messageObj);

    const idx = CONVERSATIONS.findIndex((c) => c.id === conversationId);
    if (idx > 0) {
        const [moved] = CONVERSATIONS.splice(idx, 1);
        CONVERSATIONS.unshift(moved);
    }

    if (activeConversationId === conversationId) {
        renderMessages(conv);
    } else {
        conv.unreadCount = (conv.unreadCount || 0) + 1;
    }

    if (broadcastToOtherTabs) {
        persistAndSyncConversations('NEW_MESSAGE', {
            conversationId: conversationId,
            message: messageObj
        });
    } else {
        try {
            localStorage.setItem(conversationStorageKey(), JSON.stringify(CONVERSATIONS));
        } catch (_) { }
    }

    renderConversationList();
}

function showTypingIndicator(senderName) {
    const typingBar = document.getElementById('typingIndicatorBar');
    const typingText = document.getElementById('typingUserText');
    if (!typingBar || !typingText) return;

    typingText.textContent = `${senderName} đang soạn tin nhắn...`;
    typingBar.classList.remove('d-none');

    clearTimeout(typingTimeout);
    typingTimeout = setTimeout(() => {
        typingBar.classList.add('d-none');
    }, 1800);
}

// --------------------------------------------------------------------------
// 9. TIỆN ÍCH HỖ TRỢ (TOAST, SOUND, HELPERS)
// --------------------------------------------------------------------------

function syncComposerButtons() {
    // Trong giao diện ChatSpace tối giản, nút "Gửi" luôn hiển thị rõ ràng bên phải
}

function setConversationFilter(filterValue) {
    currentFilter = filterValue;

    document.querySelectorAll('.ig-tabs .tab-pill').forEach((b) => {
        b.classList.toggle('active', b.getAttribute('data-filter') === filterValue);
    });

    document.querySelectorAll('.ig-nav-rail [data-rail-filter]').forEach((b) => {
        b.classList.toggle('active', b.getAttribute('data-rail-filter') === filterValue);
    });

    renderConversationList();
}

function updateBadgesCount() {
    const archivedCount = CONVERSATIONS.filter((c) => c.archived).length;
    const totalUnread = CONVERSATIONS.filter((c) => !c.archived).reduce(
        (sum, c) => sum + (c.unreadCount || 0),
        0
    );

    const archivedBadge = document.getElementById('archivedCountBadge');
    const railArchivedBadge = document.getElementById('railArchivedBadge');
    const railUnreadBadge = document.getElementById('railUnreadBadge');

    if (archivedBadge) {
        archivedBadge.textContent = archivedCount;
        archivedBadge.classList.toggle('d-none', archivedCount === 0);
    }
    if (railArchivedBadge) {
        railArchivedBadge.textContent = archivedCount;
        railArchivedBadge.classList.toggle('d-none', archivedCount === 0);
    }
    if (railUnreadBadge) {
        railUnreadBadge.textContent = totalUnread;
        railUnreadBadge.classList.toggle('d-none', totalUnread === 0);
    }
}

function showToast(message, iconClass = 'fa-circle-info', duration = 3200) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'chat-toast';
    toast.innerHTML = `<i class="fa-solid ${iconClass} text-info"></i><span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, duration);
}

function playSubtleBeep() {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(660, ctx.currentTime);
        gain.gain.setValueAtTime(0.04, ctx.currentTime);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.09);
    } catch (_) { }
}

function getConversationById(id) {
    return CONVERSATIONS.find((c) => c.id === id) || null;
}

function updateWsStatusUI(state, text) {
    const dotEl = document.getElementById('wsStatusDot');
    const textEl = document.getElementById('wsStatusText');
    if (!dotEl || !textEl) return;

    dotEl.className = 'ws-dot';
    if (state === 'connected') dotEl.classList.add('connected');
    textEl.textContent = text;
}

function formatFullTime(isoString) {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatShortTime(isoString) {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

function escapeHtml(unsafeText) {
    if (!unsafeText) return '';
    return String(unsafeText)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// --------------------------------------------------------------------------
// 9B. CHỨC NĂNG KẾT BẠN & QUẢN LÝ DANH BẠ TÀI KHOẢN (ĐỒNG BỘ ĐA TAB)
// --------------------------------------------------------------------------

function areUsersFriends(userA, userB) {
    return FRIENDSHIPS.accepted.some(
        (pair) =>
            (pair[0] === userA && pair[1] === userB) ||
            (pair[0] === userB && pair[1] === userA)
    );
}

function getPendingRequestBetween(userA, userB) {
    return (
        FRIENDSHIPS.requests.find(
            (r) =>
                (r.fromId === userA && r.toId === userB) ||
                (r.fromId === userB && r.toId === userA)
        ) || null
    );
}

function updateFriendBadges() {
    const incomingReqs = FRIENDSHIPS.requests.filter((r) => r.toId === CURRENT_USER.id);
    const myFriendsCount = CONTACTS_DIRECTORY.filter(
        (c) => c.id !== CURRENT_USER.id && areUsersFriends(CURRENT_USER.id, c.id)
    ).length;

    const topbarBadge = document.getElementById('topbarFriendBadge');
    const sidebarBadge = document.getElementById('sidebarFriendBadge');
    const modalReqBadge = document.getElementById('modalFriendReqCount');
    const modalFriendsCount = document.getElementById('modalFriendsCount');

    const reqCount = incomingReqs.length;
    if (topbarBadge) {
        topbarBadge.textContent = reqCount;
        topbarBadge.classList.toggle('d-none', reqCount === 0);
    }
    if (sidebarBadge) {
        sidebarBadge.textContent = reqCount;
        sidebarBadge.classList.toggle('d-none', reqCount === 0);
    }
    if (modalReqBadge) {
        modalReqBadge.textContent = reqCount;
        modalReqBadge.classList.toggle('d-none', reqCount === 0);
    }
    if (modalFriendsCount) {
        modalFriendsCount.textContent = myFriendsCount;
    }
}

function openFriendsModal(defaultTab = 'discover') {
    if (serverAuthenticated) refreshFriends();
    currentFriendsTab = defaultTab;
    document.querySelectorAll('[data-friends-tab]').forEach((btn) => {
        btn.classList.toggle('active', btn.getAttribute('data-friends-tab') === currentFriendsTab);
    });
    updateFriendBadges();
    renderFriendsModalList();
    document.getElementById('friendsModalBackdrop')?.classList.remove('d-none');
}

function closeFriendsModal() {
    document.getElementById('friendsModalBackdrop')?.classList.add('d-none');
}

async function friendApi(path, options = {}) {
    if (!serverAuthenticated) throw new Error('Hãy đăng nhập Google để dùng kết bạn thật.');
    const response = await fetch(`${getBackendUrl()}/api/friends${path}`, {
        credentials: 'include', ...options,
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
    });
    if (!response.ok || response.redirected) {
        if (response.status === 401 || response.status === 403 || response.redirected) {
            serverAuthenticated = false;
            throw new Error('Phiên đăng nhập hết hạn. Hãy đăng nhập Google lại.');
        }
        if (response.status === 409) throw new Error('Lời mời hoặc quan hệ bạn bè đã tồn tại.');
        throw new Error('Không thể kết nối kết bạn. Vui lòng thử lại.');
    }
    return response;
}

function friendError(error) { showToast(error.message, 'fa-triangle-exclamation'); }

async function refreshFriends() {
    if (!serverAuthenticated) return;
    try {
        const data = await (await friendApi('')).json();
        FRIENDSHIPS = { accepted: data.accepted, requests: data.requests };
        const known = new Map(CONTACTS_DIRECTORY.map(c => [c.id, c]));
        let contactsChanged = false;
        for (const c of data.contacts) {
            const previous = known.get(c.id);
            if (!previous || previous.name !== c.name || (previous.avatar || '') !== (c.avatar || '')) {
                contactsChanged = true;
            }
            known.set(c.id, { ...c, avatar: c.avatar || '', online: false });
        }
        CONTACTS_DIRECTORY = [...known.values()];
        updateFriendBadges();
        renderFriendsModalList();
        if (contactsChanged) {
            renderConversationList();
            const active = getConversationById(activeConversationId);
            if (active) updateActiveChatHeaderAndDrawer(active);
        }
    } catch (error) { console.warn('Friend refresh failed:', error.message); }
}

async function sendFriendRequest(targetUserId) {
    try {
        await friendApi('/requests', { method: 'POST', body: JSON.stringify({ userId: targetUserId }) });
        await refreshFriends();
        showToast('Đã gửi lời mời kết bạn.', 'fa-user-plus');
    } catch (error) { friendError(error); }
}

async function acceptFriendRequest(requestId) {
    try {
        await friendApi(`/requests/${encodeURIComponent(requestId)}/accept`, { method: 'POST' });
        await refreshFriends();
        showToast('Đã chấp nhận lời mời kết bạn.', 'fa-user-check');
    } catch (error) { friendError(error); }
}

async function cancelOrRejectFriendRequest(requestId) {
    try {
        await friendApi(`/requests/${encodeURIComponent(requestId)}`, { method: 'DELETE' });
        await refreshFriends();
        showToast('Đã hủy lời mời kết bạn.', 'fa-user-xmark');
    } catch (error) { friendError(error); }
}

async function removeFriend(friendUserId) {
    try {
        await friendApi(`/${encodeURIComponent(friendUserId)}`, { method: 'DELETE' });
        await refreshFriends();
        showToast('Đã hủy kết bạn.', 'fa-user-minus');
    } catch (error) { friendError(error); }
}

async function handleQuickAddFriendByInput(event) {
    event.preventDefault();
    const input = document.getElementById('friendSearchOrEmailInput');
    const email = (input?.value || '').trim();
    if (!email.includes('@')) {
        showToast('Nhập địa chỉ email chính xác của người muốn kết bạn.', 'fa-circle-exclamation');
        return;
    }
    try {
        const results = await (await friendApi(`/search?email=${encodeURIComponent(email)}`)).json();
        if (!results.length) {
            showToast('Không tìm thấy tài khoản. Người đó cần đăng nhập Google trước.', 'fa-circle-exclamation');
            return;
        }
        const contact = { ...results[0], avatar: results[0].avatar || '', online: false };
        CONTACTS_DIRECTORY = CONTACTS_DIRECTORY.filter(c => c.id !== contact.id);
        CONTACTS_DIRECTORY.push(contact);
        input.value = '';
        friendSearchFilter = '';
        await sendFriendRequest(contact.id);
    } catch (error) { friendError(error); }
}

function renderFriendsModalList() {
    const container = document.getElementById('friendsModalListContainer');
    if (!container) return;

    if (!serverAuthenticated) {
        container.textContent = 'Hãy đăng nhập Google để sử dụng tính năng kết bạn.';
        return;
    }
    const query = friendSearchFilter.toLowerCase();
    const related = new Set([...FRIENDSHIPS.accepted.flat(), ...FRIENDSHIPS.requests.flatMap(r => [r.fromId, r.toId])]);
    const otherAccounts = CONTACTS_DIRECTORY.filter((c) => {
        if (!related.has(c.id)) return false;
        if (c.id === CURRENT_USER.id) return false;
        if (!query) return true;
        return c.name.toLowerCase().includes(query) || c.email.toLowerCase().includes(query);
    });

    if (currentFriendsTab === 'requests') {
        const myRequests = FRIENDSHIPS.requests.filter(
            (r) => r.toId === CURRENT_USER.id || r.fromId === CURRENT_USER.id
        );

        if (myRequests.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-4 small">
                    Hiện không có lời mời kết bạn nào đang chờ xử lý.
                </div>
            `;
            return;
        }

        container.innerHTML = myRequests
            .map((req) => {
                const isIncoming = req.toId === CURRENT_USER.id;
                const partnerId = isIncoming ? req.fromId : req.toId;
                const partner = CONTACTS_DIRECTORY.find((c) => c.id === partnerId) || {
                    id: partnerId,
                    name: 'Người dùng ChatSpace',
                    email: 'user@chatspace.vn'
                };
                const initials = getInitials(partner.name, partner.initials);

                return `
                    <div class="friend-account-row">
                        <div class="friend-account-info">
                            <div class="conv-avatar-circle active-avatar">${escapeHtml(initials)}</div>
                            <div class="min-w-0">
                                <div class="friend-account-name text-truncate">${escapeHtml(partner.name)}</div>
                                <div class="friend-account-email text-truncate">
                                    ${isIncoming ? 'Đã gửi lời mời kết bạn cho bạn' : 'Bạn đã gửi lời mời kết bạn'}
                                </div>
                            </div>
                        </div>
                        <div class="friend-actions">
                            ${
                                isIncoming
                                    ? `<button type="button" class="btn-friend-action primary" data-accept-req-id="${req.id}">
                                           Chấp nhận
                                       </button>
                                       <button type="button" class="btn-friend-action danger" data-reject-req-id="${req.id}">
                                           Từ chối
                                       </button>`
                                    : `<button type="button" class="btn-friend-action" data-reject-req-id="${req.id}">
                                           Hủy lời mời
                                       </button>`
                            }
                        </div>
                    </div>
                `;
            })
            .join('');
    } else if (currentFriendsTab === 'friends') {
        const myFriends = otherAccounts.filter((c) => areUsersFriends(CURRENT_USER.id, c.id));

        if (myFriends.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-4 small">
                    Chưa có bạn bè nào. Hãy chuyển sang tab <strong>Tìm tài khoản</strong> để kết bạn nhé!
                </div>
            `;
            return;
        }

        container.innerHTML = myFriends
            .map((friend) => {
                const initials = getInitials(friend.name, friend.initials);
                return `
                    <div class="friend-account-row">
                        <div class="friend-account-info">
                            <div class="conv-avatar-circle active-avatar">${escapeHtml(initials)}</div>
                            <div class="min-w-0">
                                <div class="friend-account-name text-truncate">${escapeHtml(friend.name)}</div>
                                <div class="friend-account-email text-truncate">${escapeHtml(friend.email)}</div>
                            </div>
                        </div>
                        <div class="friend-actions">
                            <button type="button" class="btn-friend-action primary" data-chat-friend-id="${friend.id}">
                                Nhắn tin
                            </button>
                            <button type="button" class="btn-friend-action danger" data-unfriend-id="${friend.id}" title="Hủy kết bạn">
                                <i class="fa-solid fa-user-minus"></i>
                            </button>
                        </div>
                    </div>
                `;
            })
            .join('');
    } else {
        // Tab 'discover' (Tìm tài khoản & Kết bạn)
        if (otherAccounts.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted py-4 small">
                    Nhập chính xác email Google của người muốn kết bạn ở ô phía trên.
                </div>
            `;
            return;
        }

        container.innerHTML = otherAccounts
            .map((acc) => {
                const initials = getInitials(acc.name, acc.initials);
                const isFriend = areUsersFriends(CURRENT_USER.id, acc.id);
                const pendingReq = getPendingRequestBetween(CURRENT_USER.id, acc.id);

                let actionButtonsHtml = '';
                if (isFriend) {
                    actionButtonsHtml = `
                        <span class="badge bg-success-subtle text-success border border-success-subtle">Bạn bè</span>
                        <button type="button" class="btn-friend-action outline-primary" data-chat-friend-id="${acc.id}">
                            Nhắn tin
                        </button>
                    `;
                } else if (pendingReq) {
                    if (pendingReq.toId === CURRENT_USER.id) {
                        actionButtonsHtml = `
                            <button type="button" class="btn-friend-action primary" data-accept-req-id="${pendingReq.id}">
                                Chấp nhận
                            </button>
                        `;
                    } else {
                        actionButtonsHtml = `
                            <button type="button" class="btn-friend-action" data-reject-req-id="${pendingReq.id}">
                                Đã gửi (Hủy)
                            </button>
                        `;
                    }
                } else {
                    actionButtonsHtml = `
                        <button type="button" class="btn-friend-action primary" data-send-freq-id="${acc.id}">
                            + Kết bạn
                        </button>
                        <button type="button" class="btn-friend-action" data-chat-friend-id="${acc.id}" title="Nhắn tin">
                            <i class="fa-regular fa-comment-dots"></i>
                        </button>
                    `;
                }

                return `
                    <div class="friend-account-row">
                        <div class="friend-account-info">
                            <div class="conv-avatar-circle ${isFriend ? 'active-avatar' : ''}">${escapeHtml(initials)}</div>
                            <div class="min-w-0">
                                <div class="friend-account-name text-truncate">${escapeHtml(acc.name)}</div>
                                <div class="friend-account-email text-truncate">${escapeHtml(acc.email)}</div>
                            </div>
                        </div>
                        <div class="friend-actions">
                            ${actionButtonsHtml}
                        </div>
                    </div>
                `;
            })
            .join('');
    }

    // Gắn sự kiện cho các nút trong danh sách Kết bạn
    container.querySelectorAll('[data-send-freq-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            sendFriendRequest(btn.getAttribute('data-send-freq-id'));
        });
    });

    container.querySelectorAll('[data-accept-req-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            acceptFriendRequest(btn.getAttribute('data-accept-req-id'));
        });
    });

    container.querySelectorAll('[data-reject-req-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            cancelOrRejectFriendRequest(btn.getAttribute('data-reject-req-id'));
        });
    });

    container.querySelectorAll('[data-unfriend-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            removeFriend(btn.getAttribute('data-unfriend-id'));
        });
    });

    container.querySelectorAll('[data-chat-friend-id]').forEach((btn) => {
        btn.addEventListener('click', () => {
            const friendId = btn.getAttribute('data-chat-friend-id');
            closeFriendsModal();
            openOrCreateDirectConversation(friendId);
        });
    });
}

async function checkBackendAuth() {
    try {
        const res = await fetch(`${getBackendUrl()}/api/me`, { credentials: 'include' });
        if (res.ok) {
            const data = await res.json();
            if (data && data.isAuthenticated && data.email && data.id) {
                serverAuthenticated = true;
                CONTACTS_DIRECTORY = [];
                FRIENDSHIPS = { accepted: [], requests: [] };
                const userObj = {
                    id: data.id,
                    name: data.name || data.email,
                    shortName: (data.name ? data.name.split(/\s+/).slice(-2).join(' ') : data.email),
                    email: data.email,
                    online: true,
                    avatar: data.picture || `https://ui-avatars.com/api/?name=${encodeURIComponent(data.name || 'U')}&background=0095f6&color=fff&bold=true`
                };
                if (CURRENT_USER.id !== userObj.id || CURRENT_USER.email !== userObj.email || CURRENT_USER.name !== userObj.name) {
                    loginAsUser(userObj, true);
                }
                await refreshFriends();
                if (!friendRefreshTimer) friendRefreshTimer = setInterval(refreshFriends, 15000);
            }
        }
    } catch (err) {
        console.warn('Could not check backend auth:', err);
    }
}

// --------------------------------------------------------------------------
// 10. KHỞI TẠO SỰ KIỆN KHI TRANG TẢI XONG
// --------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    updateCurrentUserProfileUI();
    updateFriendBadges();
    renderFriendsModalList();
    renderSavedAccountsOnAuthScreen();
    renderActiveNowStrip();
    renderConversationList();
    renderNotifications();
    renderBlockedUsersInSettings();
    selectConversation(activeConversationId);

    if (CURRENT_USER.id) connectWebSocket();
    else document.getElementById('authScreen')?.classList.remove('d-none');

    checkBackendAuth();

    // Sự kiện Modal Kết bạn & Danh bạ tài khoản
    document.getElementById('btnOpenFriendsModal')?.addEventListener('click', () => openFriendsModal('discover'));
    document.getElementById('btnTopbarFriends')?.addEventListener('click', () => openFriendsModal('requests'));
    document.getElementById('btnCloseFriendsModal')?.addEventListener('click', closeFriendsModal);
    document.getElementById('friendsModalBackdrop')?.addEventListener('click', (e) => {
        if (e.target.id === 'friendsModalBackdrop') closeFriendsModal();
    });
    document.querySelectorAll('[data-friends-tab]').forEach((tabBtn) => {
        tabBtn.addEventListener('click', () => {
            currentFriendsTab = tabBtn.getAttribute('data-friends-tab');
            document.querySelectorAll('[data-friends-tab]').forEach((b) => {
                b.classList.toggle('active', b === tabBtn);
            });
            renderFriendsModalList();
        });
    });
    document.getElementById('friendSearchOrEmailInput')?.addEventListener('input', (e) => {
        friendSearchFilter = e.target.value.trim();
        renderFriendsModalList();
    });
    document.getElementById('quickAddFriendForm')?.addEventListener('submit', handleQuickAddFriendByInput);

    // =========================================================================
    // XỬ LÝ ĐĂNG NHẬP / ĐĂNG KÝ / QUÊN MẬT KHẨU (GMAIL, SĐT, OTP)
    // =========================================================================

    // 1. Chuyển đổi tab chính & tab phụ phương thức
    document.querySelectorAll('[data-auth-tab]').forEach((btn) => {
        btn.addEventListener('click', () => setAuthTab(btn.getAttribute('data-auth-tab')));
    });
    document.querySelectorAll('[data-login-method]').forEach((btn) => {
        btn.addEventListener('click', () => setLoginMethod(btn.getAttribute('data-login-method')));
    });
    document.querySelectorAll('[data-reg-method]').forEach((btn) => {
        btn.addEventListener('click', () => setRegMethod(btn.getAttribute('data-reg-method')));
    });
    document.querySelectorAll('[data-forgot-method]').forEach((btn) => {
        btn.addEventListener('click', () => setForgotMethod(btn.getAttribute('data-forgot-method')));
    });

    // 2. Chuyển đổi qua lại giữa các view bằng liên kết
    document.getElementById('btnGoToForgot')?.addEventListener('click', (e) => {
        e.preventDefault();
        setAuthTab('forgot');
    });
    document.getElementById('linkGoToRegister')?.addEventListener('click', (e) => {
        e.preventDefault();
        setAuthTab('register');
    });
    document.getElementById('linkGoToLogin')?.addEventListener('click', (e) => {
        e.preventDefault();
        setAuthTab('login');
    });
    document.getElementById('linkBackToLoginFromForgot')?.addEventListener('click', (e) => {
        e.preventDefault();
        setAuthTab('login');
    });

    // 3. Nút ẩn/hiện mật khẩu
    document.querySelectorAll('.btn-toggle-password').forEach((btn) => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-toggle-target');
            const targetInput = document.getElementById(targetId);
            if (!targetInput) return;
            const icon = btn.querySelector('i');
            if (targetInput.type === 'password') {
                targetInput.type = 'text';
                icon?.classList.remove('fa-eye');
                icon?.classList.add('fa-eye-slash');
            } else {
                targetInput.type = 'password';
                icon?.classList.remove('fa-eye-slash');
                icon?.classList.add('fa-eye');
            }
        });
    });

    // 4. Gửi mã OTP xác thực
    document.getElementById('btnSendRegOtp')?.addEventListener('click', () => {
        const isEmail = currentRegMethod === 'email';
        const dest = isEmail
            ? document.getElementById('regEmailInput')?.value.trim()
            : document.getElementById('regPhoneInput')?.value.trim();
        if (!dest) {
            showToast(`Vui lòng nhập ${isEmail ? 'địa chỉ Gmail' : 'số điện thoại'} trước khi nhận mã OTP!`, 'fa-circle-exclamation');
            return;
        }
        sendAuthOtp(
            dest,
            document.getElementById('btnSendRegOtp'),
            document.getElementById('regOtpHint')
        );
    });

    document.getElementById('btnSendForgotOtp')?.addEventListener('click', () => {
        const isEmail = currentForgotMethod === 'email';
        const dest = isEmail
            ? document.getElementById('forgotEmailInput')?.value.trim()
            : document.getElementById('forgotPhoneInput')?.value.trim();
        if (!dest) {
            showToast(`Vui lòng nhập ${isEmail ? 'Gmail' : 'Số điện thoại'} trước khi nhận mã OTP!`, 'fa-circle-exclamation');
            return;
        }
        sendAuthOtp(
            dest,
            document.getElementById('btnSendForgotOtp'),
            document.getElementById('forgotOtpHint')
        );
    });

    // 5. Submit Form Đăng nhập
    const loginForm = document.getElementById('loginForm');
    loginForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const isEmail = currentLoginMethod === 'email';
        const emailVal = document.getElementById('loginEmailInput')?.value.trim();
        const phoneVal = document.getElementById('loginPhoneInput')?.value.trim();
        const passVal = document.getElementById('loginPasswordInput')?.value;

        if (isEmail && !emailVal) {
            showToast('Vui lòng nhập địa chỉ Gmail / Email!', 'fa-circle-exclamation');
            return;
        }
        if (!isEmail && !phoneVal) {
            showToast('Vui lòng nhập số điện thoại!', 'fa-circle-exclamation');
            return;
        }
        if (!passVal) {
            showToast('Vui lòng nhập mật khẩu!', 'fa-circle-exclamation');
            return;
        }

        let user = null;
        if (isEmail) {
            user = CONTACTS_DIRECTORY.find((u) => u.email && u.email.toLowerCase() === emailVal.toLowerCase());
        } else {
            const cleanPhone = phoneVal.replace(/[^0-9]/g, '');
            user = CONTACTS_DIRECTORY.find((u) => u.phone && u.phone.replace(/[^0-9]/g, '') === cleanPhone);
        }

        if (!user) {
            showToast(`Không tìm thấy tài khoản với ${isEmail ? 'Gmail' : 'SĐT'} này. Bạn có thể bấm "Đăng ký"!`, 'fa-triangle-exclamation');
            return;
        }

        if (user.password && user.password !== passVal) {
            showToast('Mật khẩu không chính xác! Vui lòng thử lại hoặc chọn Quên mật khẩu.', 'fa-circle-xmark');
            return;
        }

        loginAsUser(user);
        showToast(`Đăng nhập thành công! Chào mừng ${user.name}`, 'fa-circle-check');
    });

    // 6. Submit Form Đăng ký
    const registerForm = document.getElementById('registerForm');
    registerForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const nameVal = document.getElementById('regNameInput')?.value.trim();
        const isEmail = currentRegMethod === 'email';
        const emailVal = document.getElementById('regEmailInput')?.value.trim();
        const phoneVal = document.getElementById('regPhoneInput')?.value.trim();
        const passVal = document.getElementById('regPasswordInput')?.value;
        const confirmPassVal = document.getElementById('regConfirmPasswordInput')?.value;
        const otpVal = document.getElementById('regOtpInput')?.value.trim();

        if (!nameVal) {
            showToast('Vui lòng nhập họ và tên hiển thị!', 'fa-circle-exclamation');
            return;
        }

        if (isEmail) {
            if (!emailVal || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal)) {
                showToast('Vui lòng nhập địa chỉ Gmail / Email hợp lệ!', 'fa-circle-exclamation');
                return;
            }
            const exists = CONTACTS_DIRECTORY.some((u) => u.email && u.email.toLowerCase() === emailVal.toLowerCase());
            if (exists) {
                showToast('Email này đã được đăng ký! Vui lòng Đăng nhập hoặc dùng Email khác.', 'fa-triangle-exclamation');
                return;
            }
        } else {
            const cleanPhone = (phoneVal || '').replace(/[^0-9]/g, '');
            if (!cleanPhone || cleanPhone.length < 9) {
                showToast('Vui lòng nhập số điện thoại hợp lệ (tối thiểu 9 số)!', 'fa-circle-exclamation');
                return;
            }
            const exists = CONTACTS_DIRECTORY.some((u) => u.phone && u.phone.replace(/[^0-9]/g, '') === cleanPhone);
            if (exists) {
                showToast('Số điện thoại này đã được đăng ký! Vui lòng Đăng nhập hoặc dùng số khác.', 'fa-triangle-exclamation');
                return;
            }
        }

        if (!passVal || passVal.length < 6) {
            showToast('Mật khẩu phải có ít nhất 6 ký tự!', 'fa-circle-exclamation');
            return;
        }

        if (passVal !== confirmPassVal) {
            showToast('Xác nhận mật khẩu không khớp! Vui lòng nhập lại.', 'fa-circle-xmark');
            return;
        }

        const targetDest = isEmail ? emailVal : phoneVal;
        if (!verifyAuthOtp(targetDest, otpVal)) {
            showToast('Mã OTP không chính xác hoặc đã hết hạn! Vui lòng lấy mã mới (hoặc nhập 123456 để test).', 'fa-triangle-exclamation');
            return;
        }

        const newUser = {
            id: 'user-' + Date.now(),
            name: nameVal,
            shortName: nameVal.split(/\s+/).slice(-2).join(' '),
            email: isEmail ? emailVal : `${phoneVal.replace(/[^0-9]/g, '')}@chatspace.local`,
            phone: isEmail ? '' : phoneVal.replace(/[^0-9]/g, ''),
            password: passVal,
            online: true,
            avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(nameVal)}&background=1d72b8&color=fff&bold=true`
        };

        CONTACTS_DIRECTORY.unshift(newUser);
        saveContactsDirectory();
        loginAsUser(newUser);
        showToast(`Đăng ký tài khoản thành công! Chào mừng bạn, ${newUser.name}.`, 'fa-circle-check');
    });

    // 7. Submit Form Quên mật khẩu
    const forgotForm = document.getElementById('forgotForm');
    forgotForm?.addEventListener('submit', (e) => {
        e.preventDefault();
        const isEmail = currentForgotMethod === 'email';
        const emailVal = document.getElementById('forgotEmailInput')?.value.trim();
        const phoneVal = document.getElementById('forgotPhoneInput')?.value.trim();
        const otpVal = document.getElementById('forgotOtpInput')?.value.trim();
        const newPassVal = document.getElementById('forgotNewPasswordInput')?.value;
        const confirmNewPassVal = document.getElementById('forgotConfirmNewPasswordInput')?.value;

        const targetDest = isEmail ? emailVal : phoneVal;
        if (!targetDest) {
            showToast(`Vui lòng nhập ${isEmail ? 'Gmail' : 'Số điện thoại'} đã đăng ký!`, 'fa-circle-exclamation');
            return;
        }

        let user = null;
        if (isEmail) {
            user = CONTACTS_DIRECTORY.find((u) => u.email && u.email.toLowerCase() === emailVal.toLowerCase());
        } else {
            const cleanPhone = phoneVal.replace(/[^0-9]/g, '');
            user = CONTACTS_DIRECTORY.find((u) => u.phone && u.phone.replace(/[^0-9]/g, '') === cleanPhone);
        }

        if (!user) {
            showToast(`Không tìm thấy tài khoản với ${isEmail ? 'Gmail' : 'SĐT'} này!`, 'fa-triangle-exclamation');
            return;
        }

        if (!verifyAuthOtp(targetDest, otpVal)) {
            showToast('Mã OTP không chính xác hoặc đã hết hạn! Vui lòng lấy mã mới (hoặc nhập 123456 để test).', 'fa-triangle-exclamation');
            return;
        }

        if (!newPassVal || newPassVal.length < 6) {
            showToast('Mật khẩu mới phải có ít nhất 6 ký tự!', 'fa-circle-exclamation');
            return;
        }

        if (newPassVal !== confirmNewPassVal) {
            showToast('Mật khẩu xác nhận không khớp! Vui lòng nhập lại.', 'fa-circle-xmark');
            return;
        }

        user.password = newPassVal;
        saveContactsDirectory();
        loginAsUser(user);
        showToast('Đặt lại mật khẩu thành công! Đã đăng nhập vào hệ thống.', 'fa-circle-check');
    });

    // Gửi tin nhắn & phát tín hiệu "Đang soạn tin nhắn..." sang các Tab khác
    const messageForm = document.getElementById('messageForm');
    const messageInput = document.getElementById('messageInput');

    messageInput.addEventListener('input', () => {
        syncComposerButtons();
        if (realtimeBus && activeConversationId && messageInput.value.trim()) {
            realtimeBus.postMessage({
                type: 'USER_TYPING',
                conversationId: activeConversationId,
                senderId: CURRENT_USER.id,
                senderName: CURRENT_USER.name
            });
        }
    });

    messageForm.addEventListener('submit', (event) => {
        event.preventDefault();
        const content = messageInput.value;
        if (!content.trim()) return;

        if (sendChatMessage(activeConversationId, content)) messageInput.value = '';
        syncComposerButtons();
        document.getElementById('emojiPickerPopover')?.classList.add('d-none');
    });

    // Gửi nhanh biểu tượng trái tim (❤️) kiểu Instagram Direct
    document.getElementById('btnQuickReactionSend')?.addEventListener('click', () => {
        const conv = getConversationById(activeConversationId);
        if (!conv) return;
        sendChatMessage(activeConversationId, '❤️');
    });

    // Hủy trả lời tin nhắn
    document.getElementById('btnCancelReply').addEventListener('click', cancelReplyMessage);

    // Tìm kiếm cuộc trò chuyện bên trái
    const searchInput = document.getElementById('searchConversationInput');
    const btnClearSearch = document.getElementById('btnClearSearch');
    searchInput.addEventListener('input', (e) => {
        searchQuery = e.target.value.trim();
        btnClearSearch.classList.toggle('d-none', searchQuery.length === 0);
        renderConversationList();
    });

    btnClearSearch.addEventListener('click', () => {
        searchInput.value = '';
        searchQuery = '';
        btnClearSearch.classList.add('d-none');
        renderConversationList();
    });

    // Tìm kiếm tin nhắn bên trong cuộc trò chuyện hiện tại
    const inChatBar = document.getElementById('inChatSearchBar');
    const inChatInput = document.getElementById('inChatSearchInput');
    document.getElementById('drawerBtnSearchInChat').addEventListener('click', () => {
        inChatBar.classList.remove('d-none');
        inChatInput.focus();
    });
    document.getElementById('btnCloseInChatSearch').addEventListener('click', () => {
        inChatInput.value = '';
        inChatSearchQuery = '';
        inChatBar.classList.add('d-none');
        const conv = getConversationById(activeConversationId);
        if (conv) renderMessages(conv);
    });
    inChatInput.addEventListener('input', (e) => {
        inChatSearchQuery = e.target.value.trim();
        const conv = getConversationById(activeConversationId);
        if (conv) renderMessages(conv);
    });

    // Lọc theo Tab gạch chân Instagram & Thanh Nav Rail bên trái
    document.querySelectorAll('.ig-tabs .tab-pill').forEach((tabBtn) => {
        tabBtn.addEventListener('click', () => {
            setConversationFilter(tabBtn.getAttribute('data-filter'));
        });
    });

    document.querySelectorAll('.ig-nav-rail [data-rail-filter]').forEach((railBtn) => {
        railBtn.addEventListener('click', () => {
            setConversationFilter(railBtn.getAttribute('data-rail-filter'));
        });
    });

    // Đổi màu chủ đề (Theme Swatches) & Đổi tên
    document.querySelectorAll('#themeColorSwatches .theme-swatch').forEach((swatchBtn) => {
        swatchBtn.addEventListener('click', () => {
            changeActiveConversationTheme(swatchBtn.getAttribute('data-theme-color'));
        });
    });
    document.getElementById('drawerBtnChangeQuickEmoji')?.addEventListener('click', cycleQuickEmoji);
    document.getElementById('drawerBtnRenameConv')?.addEventListener('click', renameActiveConversation);

    // Mở Modal Tạo nhóm mới / Tin nhắn mới & Thêm thành viên vào nhóm
    document.getElementById('btnOpenCreateGroup')?.addEventListener('click', () => openGroupModal('CREATE'));
    document.getElementById('btnRailCreateGroup')?.addEventListener('click', () => openGroupModal('CREATE'));
    document.getElementById('btnEmptyCreateGroup')?.addEventListener('click', () => openGroupModal('CREATE'));
    document.getElementById('drawerBtnAddMember')?.addEventListener('click', () => openGroupModal('ADD_MEMBER'));
    document.getElementById('btnHeaderAddMember')?.addEventListener('click', () => openGroupModal('ADD_MEMBER'));

    // Sự kiện bên trong Modal Tạo nhóm
    document.getElementById('btnCloseCreateGroup')?.addEventListener('click', closeGroupModal);
    document.getElementById('btnCancelCreateGroup')?.addEventListener('click', closeGroupModal);
    document.getElementById('createGroupModalBackdrop')?.addEventListener('click', (e) => {
        if (e.target.id === 'createGroupModalBackdrop') closeGroupModal();
    });

    document.getElementById('groupNameInput').addEventListener('input', (e) => {
        const avatarInput = document.getElementById('groupAvatarTextInput');
        if (!avatarInput.dataset.manualEdit) {
            const initials = e.target.value
                .trim()
                .split(/\s+/)
                .filter(Boolean)
                .map((w) => w[0])
                .join('')
                .substring(0, 3)
                .toUpperCase();
            avatarInput.value = initials;
        }
    });
    document.getElementById('groupAvatarTextInput').addEventListener('input', (e) => {
        e.target.dataset.manualEdit = e.target.value.trim() ? 'true' : '';
    });

    document.getElementById('searchMemberInput').addEventListener('input', (e) => {
        memberSearchQuery = e.target.value.trim();
        renderContactPickerList();
    });
    document.getElementById('btnAddCustomContact').addEventListener('click', addCustomContactFromInput);
    document.getElementById('createGroupForm').addEventListener('submit', handleGroupModalSubmit);

    // Xử lý click các hành động trong Menu ngữ cảnh (...)
    document.querySelectorAll('#conversationContextMenu .context-menu-item').forEach((item) => {
        item.addEventListener('click', () => {
            const action = item.getAttribute('data-action');
            const targetId = contextMenuTargetConvId;
            closeContextMenu();
            if (!targetId) return;

            if (action === 'toggle-unread') toggleUnreadConversation(targetId);
            if (action === 'toggle-pin') togglePinConversation(targetId);
            if (action === 'toggle-mute') toggleMuteConversation(targetId);
            if (action === 'toggle-archive') toggleArchiveConversation(targetId);
            if (action === 'toggle-block') toggleBlockConversation(targetId);
            if (action === 'delete-chat') deleteConversation(targetId);
        });
    });

    // Các nút trong Ngăn Thông tin bên phải
    document.getElementById('drawerBtnPin').addEventListener('click', () => {
        if (activeConversationId) togglePinConversation(activeConversationId);
    });
    document.getElementById('drawerBtnMute').addEventListener('click', () => {
        if (activeConversationId) toggleMuteConversation(activeConversationId);
    });
    document.getElementById('drawerBtnArchive').addEventListener('click', () => {
        if (activeConversationId) toggleArchiveConversation(activeConversationId);
    });
    document.getElementById('drawerBtnMarkUnread').addEventListener('click', () => {
        if (activeConversationId) toggleUnreadConversation(activeConversationId);
    });
    document.getElementById('drawerBtnBlock').addEventListener('click', () => {
        if (activeConversationId) toggleBlockConversation(activeConversationId);
    });
    document.getElementById('drawerBtnDelete').addEventListener('click', () => {
        if (activeConversationId) deleteConversation(activeConversationId);
    });
    document.getElementById('btnUnblockFromBanner').addEventListener('click', () => {
        if (activeConversationId) toggleBlockConversation(activeConversationId);
    });

    // Ẩn/Hiện ngăn Thông tin bên phải
    const btnToggleInfoDrawer = document.getElementById('btnToggleInfoDrawer');
    const chatInfoDrawer = document.getElementById('chatInfoDrawer');
    btnToggleInfoDrawer.addEventListener('click', () => {
        chatInfoDrawer.classList.toggle('d-none');
        btnToggleInfoDrawer.classList.toggle('active');
    });

    // Trung tâm Thông báo
    const notifPopover = document.getElementById('notificationPopover');
    document.getElementById('btnNotifications').addEventListener('click', (e) => {
        e.stopPropagation();
        notifPopover.classList.toggle('d-none');
    });

    document.getElementById('btnMarkAllNotifRead').addEventListener('click', () => {
        NOTIFICATIONS.forEach((n) => (n.unread = false));
        renderNotifications();
        showToast('Đã đánh dấu tất cả thông báo là đã đọc', 'fa-check-double');
    });

    // Chuyển nhanh Dark Mode
    const toggleDarkModeSwitch = document.getElementById('toggleDarkMode');
    const applyDarkMode = (enabled) => {
        USER_SETTINGS.darkMode = enabled;
        document.body.classList.toggle('dark-mode', enabled);
        if (toggleDarkModeSwitch) toggleDarkModeSwitch.checked = enabled;
    };

    document.getElementById('btnQuickDarkMode').addEventListener('click', () => {
        applyDarkMode(!USER_SETTINGS.darkMode);
        showToast(
            USER_SETTINGS.darkMode ? 'Đã bật Chế độ tối ChatSpace' : 'Đã chuyển về Chế độ sáng',
            'fa-moon'
        );
    });

    toggleDarkModeSwitch.addEventListener('change', (e) => {
        applyDarkMode(e.target.checked);
    });

    // Mở / Đóng Modal Cài đặt
    const settingsModal = document.getElementById('settingsModalBackdrop');
    document.getElementById('btnOpenSettings').addEventListener('click', () => {
        renderBlockedUsersInSettings();
        settingsModal.classList.remove('d-none');
    });
    document.getElementById('btnCloseSettings').addEventListener('click', () => {
        settingsModal.classList.add('d-none');
    });
    settingsModal.addEventListener('click', (e) => {
        if (e.target === settingsModal) settingsModal.classList.add('d-none');
    });

    document.getElementById('toggleActiveStatus').addEventListener('change', (e) => {
        USER_SETTINGS.activeStatus = e.target.checked;
        document
            .getElementById('myActiveStatusDot')
            ?.classList.toggle('online', USER_SETTINGS.activeStatus);
        showToast(
            USER_SETTINGS.activeStatus
                ? 'Đã bật hiển thị Trạng thái hoạt động'
                : 'Đã ẩn Trạng thái hoạt động',
            'fa-user-check'
        );
    });

    document.getElementById('toggleDesktopNotif').addEventListener('change', (e) => {
        USER_SETTINGS.desktopNotif = e.target.checked;
    });

    document.getElementById('toggleSoundNotif').addEventListener('change', (e) => {
        USER_SETTINGS.soundNotif = e.target.checked;
    });

    // Mobile Back
    document.getElementById('btnBackToSidebar').addEventListener('click', () => {
        document.getElementById('appContainer').classList.remove('mobile-chat-active');
    });

    // Mở màn hình Đăng nhập / Đổi tài khoản
    const authScreen = document.getElementById('authScreen');
    const openAuthScreen = (tab = 'login') => {
        renderSavedAccountsOnAuthScreen();
        setAuthTab(tab);
        if (CURRENT_USER.email) {
            const loginEmail = document.getElementById('loginEmailInput');
            if (loginEmail) loginEmail.value = CURRENT_USER.email;
            setLoginMethod('email');
        } else if (CURRENT_USER.phone) {
            const loginPhone = document.getElementById('loginPhoneInput');
            if (loginPhone) loginPhone.value = CURRENT_USER.phone;
            setLoginMethod('phone');
        }
        const loginPass = document.getElementById('loginPasswordInput');
        if (loginPass && CURRENT_USER.password) loginPass.value = CURRENT_USER.password;

        authScreen?.classList.remove('d-none');
    };
    document.getElementById('btnSwitchAccount')?.addEventListener('click', () => openAuthScreen('login'));
    document.getElementById('btnHeaderSwitchAccount')?.addEventListener('click', () => openAuthScreen('login'));
    document.getElementById('btnLogoutFromSettings')?.addEventListener('click', () => openAuthScreen('login'));
    document.getElementById('btnCloseAuthScreen')?.addEventListener('click', () => {
        if (CURRENT_USER.id) authScreen.classList.add('d-none');
    });
    authScreen?.addEventListener('click', (e) => {
        if (e.target === authScreen && CURRENT_USER.id) authScreen.classList.add('d-none');
    });
    document.getElementById('btnGoogleOAuth')?.addEventListener('click', (event) => {
        event.preventDefault();
        window.location.href = `${getBackendUrl()}/oauth2/authorization/google`;
    });

    // Chọn Emoji
    const emojiPopover = document.getElementById('emojiPickerPopover');
    document.getElementById('btnToggleEmoji').addEventListener('click', (e) => {
        e.stopPropagation();
        emojiPopover.classList.toggle('d-none');
    });
    document.querySelectorAll('.emoji-item').forEach((emojiBtn) => {
        emojiBtn.addEventListener('click', () => {
            messageInput.value += emojiBtn.textContent;
            syncComposerButtons();
            messageInput.focus();
        });
    });

    // Gửi hình ảnh thật vào khung chat
    const imageInput = document.getElementById('imageAttachmentInput');
    document.getElementById('btnSendImage').addEventListener('click', () => {
        imageInput.click();
    });
    imageInput.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (ev) => {
            const conv = getConversationById(activeConversationId);
            if (conv) {
                conv.sharedFiles = conv.sharedFiles || [];
                conv.sharedFiles.unshift(file.name);
                updateActiveChatHeaderAndDrawer(conv);
            }
            sendChatMessage(activeConversationId, '', { imageUrl: ev.target.result });
        };
        reader.readAsDataURL(file);
        imageInput.value = '';
    });

    // Đính kèm tệp tài liệu
    const fileInput = document.getElementById('fileAttachmentInput');
    document.getElementById('btnAttachFile').addEventListener('click', () => {
        fileInput.click();
    });
    fileInput.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (file) {
            const conv = getConversationById(activeConversationId);
            if (conv) {
                conv.sharedFiles = conv.sharedFiles || [];
                conv.sharedFiles.unshift(file.name);
                updateActiveChatHeaderAndDrawer(conv);
            }
            sendChatMessage(activeConversationId, `📎 Đã gửi tệp đính kèm: ${file.name}`);
            fileInput.value = '';
        }
    });

    // Đóng các menu/popover khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (!e.target.closest('#conversationContextMenu')) {
            closeContextMenu();
        }
        if (!e.target.closest('#notificationPopover') && !e.target.closest('#btnNotifications')) {
            notifPopover.classList.add('d-none');
        }
        if (!e.target.closest('#emojiPickerPopover') && !e.target.closest('#btnToggleEmoji')) {
            emojiPopover.classList.add('d-none');
        }
    });
});
