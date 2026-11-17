# Server storage audit

All durable chat data is stored in MySQL. The browser holds the current UI state in memory only. Authentication uses the server-managed session cookie and Spring Session JDBC; account profiles and passwords are never copied into browser storage.

## Data ownership

| Data | Server storage |
| --- | --- |
| Accounts and Google profile pictures | `users` |
| Friend requests and accepted friendships | `friendships` |
| Direct/group conversations and shared appearance | `conversations` and `metadata` |
| Members, owner roles, read cursor, pins, mute, archive, block, nickname and hide flags | `conversation_members` and `preferences` |
| Message content, sender, timestamp, reply, reaction and recall state | `messages` and `metadata` |
| Theme, presence visibility and notification preferences | `user_preferences` |
| Notification inbox and read state | `user_notifications` |
| Desktop delivery claims shared by tabs of a browser session | `desktop_deliveries` |
| Imported client-only history, contacts and old preferences | `legacy_chat_imports` |
| Authentication sessions | `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` |

## Runtime behavior

- `GET /api/chat/snapshot` loads the authenticated account's state after login and on reconnect.
- `POST /api/chat/sync` writes changed fields and messages in a transaction. The server resolves the account from the authenticated principal and checks conversation membership and owner permissions.
- Only the authenticated sender can create a new message or recall their message. Direct conversation keys are canonical for the two actual accounts. Group membership changes require the owner.
- The existing `/app/chat.private` WebSocket endpoint also uses durable storage. Server notifications are published after the transaction commits, never before the message is saved.
- Desktop claims are stored in the database and scoped to the browser's authenticated session so multiple tabs do not duplicate banners, while another browser/device can still display its own notification.
- A failed send retains the compose text. The application reports server write failures and reloads committed state rather than treating browser memory as a durable fallback.
- Snapshot polling recovers state if a WebSocket event is missed. Closing a browser does not lose history. Receiving a new system banner with the browser closed still requires a separate Web Push implementation.

## Legacy import

After authenticating, the client reads only the current account's old conversation and notification keys. It uploads an idempotent archive to `/api/chat/import`. The account-specific keys are removed only after a successful response. Another account's history remains untouched until that account signs in and imports it.

Historical client messages are preserved as private, read-only archives in the Archived view. They are not inserted as authenticated messages supposedly sent by another account. Real new chats use the normalized conversation/member/message tables. Repeating an import after a lost response does not create a duplicate archive.

There are no `localStorage.setItem` or `sessionStorage.setItem` calls. Browser storage is accessed exclusively by this one-time legacy importer and its cleanup. Pending writes, search text, modal state, typing indicators and the currently selected conversation are temporary memory state.

## Schema and validation

Flyway migration `V6__server_chat_storage.sql` adds the required columns/tables without deleting existing records. Existing database conversations and messages receive stable external IDs. Deploy backend and frontend together, then reload any tabs running the old client.

Verification:

- `node --test tests/desktop-notifications.test.cjs tests/server-storage.test.cjs`
- `./mvnw.cmd -Dtest=ChatStorageIntegrationTest,PrivateChatControllerTest test`

The integration suite runs against an isolated H2 database in MySQL compatibility mode and applies V6 on a test schema. An additional opt-in `ChatStorageMysqlIntegrationTest` uses `CHAT_MYSQL_TEST_URL`. The full migration chain and 15 backend tests were also verified on an isolated MySQL 8.4 Docker instance, alongside 17 frontend tests. No test connected to production. Real-browser Google authentication and deployment must still be checked on the live environment.
