package com.teamchat.realtime_chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamchat.realtime_chat.security.AccountIdentity;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatStorageService {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final AccountIdentity identity;
    private final SimpMessagingTemplate bus;
    private final boolean webStorageEnabled;
    private final boolean directOnlyBlocking;
    private static final Set<String> PERSONAL = Set.of("nickname", "pinned", "muted", "archived", "blocked", "unreadCount", "hidden");
    private static final Set<String> SHARED = Set.of("themeColor", "quickEmoji", "avatarText", "topic");
    public ChatStorageService(JdbcTemplate db, ObjectMapper json, AccountIdentity identity, SimpMessagingTemplate bus,
            @Value("${chat.storage.web-features:true}") boolean webStorageEnabled,
            @Value("${chat.storage.direct-only-blocking:false}") boolean directOnlyBlocking) {
        this.db=db; this.json=json; this.identity=identity; this.bus=bus;
        this.webStorageEnabled = webStorageEnabled;
        this.directOnlyBlocking = directOnlyBlocking;
    }
    private Map<String,Object> object(String value) {
        if(value==null || value.isBlank()) return new LinkedHashMap<>();
        try { return json.readValue(value,new TypeReference<LinkedHashMap<String,Object>>(){}); }
        catch(Exception e) { throw new IllegalStateException("Invalid stored chat data",e); }
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch(Exception e) { throw new IllegalArgumentException("Invalid chat data",e); }
    }
    @SuppressWarnings("unchecked") private Map<String,Object> map(Object value) {
        return value instanceof Map<?,?> ? new LinkedHashMap<>((Map<String,Object>)value) : new LinkedHashMap<>();
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> list(Object value) {
        return value instanceof List<?> ? ((List<?>)value).stream().map(this::map).toList() : List.of();
    }
    private String text(Object value) { return value==null ? "" : value.toString(); }
    private long number(Object value) { return Long.parseLong(value.toString()); }
    private ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.FORBIDDEN); }
    private Map<String,Object> account(long id) {
        return db.queryForObject("SELECT id, google_sub, display_name, email, avatar_url FROM users WHERE id=?",(r,n)->{
            Map<String,Object> out=new LinkedHashMap<>();
            out.put("id", r.getString("google_sub")==null?r.getString("id"):r.getString("google_sub"));
            out.put("name",r.getString("display_name")); out.put("email",r.getString("email"));
            out.put("avatar",r.getString("avatar_url"));out.put("online",false);return out;
        },id);
    }
    private long accountId(String external) {
        var ids=db.query("SELECT id FROM users WHERE google_sub=?",(r,n)->r.getLong(1),external);
        if(!ids.isEmpty()) return ids.get(0);
        try {
            long id=Long.parseLong(external);
            if(Boolean.TRUE.equals(db.queryForObject("SELECT COUNT(*)>0 FROM users WHERE id=?",Boolean.class,id))) return id;
        } catch(NumberFormatException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown account");
    }
    private static Long databaseConversationId(String key) {
        if (!key.matches("db-[1-9][0-9]*")) return null;
        try {
            return Long.valueOf(key.substring(3));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversation ID");
        }
    }
    private Map<String,Object> membership(long me,String key) {
        Long databaseId = databaseConversationId(key);
        String query = "SELECT c.id, c.type, c.created_by, c.name, c.metadata, cm.preferences, cm.role "
                + "FROM conversations c JOIN conversation_members cm ON cm.conversation_id=c.id WHERE cm.user_id=? AND ";
        // Groups created through /api/groups use the database ID exposed by snapshot().
        var rows = databaseId == null
                ? db.queryForList(query + "c.external_id=? FOR UPDATE", me, key)
                : db.queryForList(query + "c.id=? AND (c.external_id IS NULL OR c.external_id=?) FOR UPDATE",
                        me, databaseId, key);
        if(rows.isEmpty()) throw denied();return rows.get(0);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> snapshot(Principal principal) {
        long me=identity.id(principal);
        List<Map<String,Object>> conversations=new ArrayList<>();
        var rows=db.queryForList("SELECT c.*, cm.preferences, cm.last_read_message_id FROM conversations c "
            +"JOIN conversation_members cm ON cm.conversation_id=c.id WHERE cm.user_id=? ORDER BY c.id DESC",me);
        for(var row:rows) {
            long cid=number(row.get("id"));
            Map<String,Object> prefs=object(text(row.get("preferences")));
            if(Boolean.TRUE.equals(prefs.get("hidden"))) continue;
            Map<String,Object> conv=object(text(row.get("metadata")));
            String key=row.get("external_id")==null?"db-"+cid:text(row.get("external_id"));
            boolean direct = "PRIVATE".equals(row.get("type"));
            conv.put("id",key);conv.put("type",direct?"DIRECT":"GROUP");
            conv.put("roomId",key);conv.put("sharedFiles",List.of());conv.put("messages",messages(cid));
            List<Map<String,Object>> members = new ArrayList<>();
            boolean sendBlocked = false;
            for (var memberRow : db.queryForList(
                    "SELECT user_id,role,preferences FROM conversation_members WHERE conversation_id=? ORDER BY id", cid)) {
                var member = account(number(memberRow.get("user_id")));
                member.put("role", "OWNER".equals(memberRow.get("role")) ? "Trưởng nhóm" : "Thành viên");
                members.add(member);
                if (directOnlyBlocking && direct && Boolean.TRUE.equals(object(text(memberRow.get("preferences"))).get("blocked"))) {
                    sendBlocked = true;
                }
            }
            conv.put("members",members);conv.put("participants",members.stream().map(m->m.get("id")).toList());
            conv.put("name",row.get("name"));
            if(direct) {
                var partner=members.stream().filter(m->!text(m.get("id")).equals(text(account(me).get("id")))).findFirst().orElse(account(me));
                conv.put("name",partner.get("name"));conv.put("avatar",partner.get("avatar"));conv.put("contactId",partner.get("id"));
            }
            conv.put("statusText","GROUP".equals(conv.get("type"))?members.size()+" thành viên":"");
            long lastRead=row.get("last_read_message_id")==null?0:number(row.get("last_read_message_id"));
            long unread=db.queryForObject("SELECT COUNT(*) FROM messages WHERE conversation_id=? AND sender_id<>? AND id>?",Long.class,cid,me,lastRead);
            if (!webStorageEnabled) {
                prefs.remove("unreadCount");
                if (Boolean.TRUE.equals(prefs.get("markedUnread"))) unread = Math.max(1, unread);
            }
            conv.put("unreadCount",unread);conv.putAll(prefs);
            if (directOnlyBlocking) {
                conv.put("sendBlocked", sendBlocked);
                if (!direct) conv.remove("blocked");
            }
            conversations.add(conv);
        }
        if (!webStorageEnabled) {
            return Map.of("conversations", conversations, "settings", Map.of(), "notifications", List.of());
        }
        var imports=db.query("SELECT payload FROM legacy_chat_imports WHERE user_id=? ORDER BY created_at",(r,n)->object(r.getString(1)),me);
        for(var archive:imports) for(var old:list(archive.get("conversations"))) {
            String linked=text(old.get("linkedConversation"));
            if(!linked.isBlank()) {
                var live=conversations.stream().filter(c->linked.equals(c.get("id"))).findFirst();
                if(live.isPresent()) {
                    List<Map<String,Object>> combined=new ArrayList<>();
                    for(var historical:list(old.get("messages"))) {
                        historical.put("id","history-"+UUID.nameUUIDFromBytes((archive.get("sourceId")+":"+historical.get("id")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                        historical.put("legacyMessage",true);combined.add(historical);
                    }
                    combined.addAll(list(live.get().get("messages")));live.get().put("messages",combined);
                }
                continue;
            }
            var legacyPrefs=map(old.remove("legacyPreferences"));
            if (Boolean.TRUE.equals(legacyPrefs.get("hidden"))) continue;
            old.put("id","legacy-"+text(archive.get("sourceId"))+"-"+text(old.get("id")));
            old.put("legacyReadOnly",true);old.put("archived",true);old.put("pinned",false);old.put("unreadCount",0);
            old.put("name",text(old.get("name"))+" (lịch sử cũ)");old.put("statusText","Lịch sử đã nhập vào database");
            old.putAll(legacyPrefs);
            conversations.add(old);
        }
        var prefs=db.query("SELECT settings FROM user_preferences WHERE user_id=?",(r,n)->object(r.getString(1)),me);
        var notifications=db.query("SELECT payload FROM user_notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 100",(r,n)->object(r.getString(1)),me);
        return Map.of("conversations",conversations,"settings",prefs.isEmpty()?Map.of():prefs.get(0),"notifications",notifications);
    }
    public List<Map<String,Object>> messages(long cid) {
        return db.query("SELECT id,external_id,sender_id,content,created_at,metadata FROM messages WHERE conversation_id=? ORDER BY id",(r,n)->{
            var message=object(r.getString("metadata"));var sender=account(r.getLong("sender_id"));
            message.put("id",r.getString("external_id")==null?"db-msg-"+r.getLong("id"):r.getString("external_id"));
            message.put("senderId",sender.get("id"));message.put("senderName",sender.get("name"));message.put("senderAvatar",sender.get("avatar"));
            message.put("content",r.getString("content"));message.put("timestamp",r.getTimestamp("created_at").toInstant().toString());return message;
        },cid);
    }
    private record ChatRecipient(String googleSub, String id, boolean muted) {}

    private void publish(long cid,Object message) {
        String key = db.queryForObject("SELECT COALESCE(external_id, CONCAT('db-', id)) FROM conversations WHERE id=?",
                String.class, cid);
        Map<String, Object> event = Map.of("type", "CHAT_UPDATED", "conversationId", key,
                "message", message == null ? Map.of() : message);
        Map<String, Object> mutedEvent = new LinkedHashMap<>(event);
        mutedEvent.put("muted", true);
        var recipients = db.query("SELECT u.google_sub,u.id,cm.preferences FROM conversation_members cm "
                        + "JOIN users u ON cm.user_id=u.id WHERE cm.conversation_id=?",
                (r, n) -> new ChatRecipient(r.getString(1), r.getString(2), !webStorageEnabled && message != null
                        && Boolean.TRUE.equals(object(r.getString(3)).get("muted"))), cid);
        Map<String, Map<String, Object>> targets = new LinkedHashMap<>();
        // Local sessions use the numeric ID even when the account also has a Google subject.
        for (var recipient : recipients) {
            var payload = recipient.muted() ? mutedEvent : event;
            if (recipient.googleSub() != null) {
                targets.putIfAbsent(recipient.googleSub(), payload);
            }
            targets.put(recipient.id(), payload);
        }
        Runnable send = () -> targets.forEach((id, payload) -> bus.convertAndSendToUser(id, "/queue/messages", payload));
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){send.run();}});
        else send.run();
    }

    public void publishConversationUpdate(long conversationId) {
        publish(conversationId, null);
    }

    @Transactional
    public void sync(Principal principal,Map<String,Object> request) {
        long me=identity.id(principal);
        for(var change:list(request.get("conversations"))) syncConversation(me,change);
        if (!webStorageEnabled) return;
        if(request.containsKey("settings")) {
            var old=db.query("SELECT settings FROM user_preferences WHERE user_id=?",(r,n)->object(r.getString(1)),me);
            var settings=old.isEmpty()?new LinkedHashMap<String,Object>():old.get(0);
            map(request.get("settings")).forEach((k,v)->{if(Set.of("activeStatus","darkMode","desktopNotif","soundNotif").contains(k) && v instanceof Boolean)settings.put(k,v);});
            db.update("INSERT INTO user_preferences(user_id,settings) VALUES (?,?) ON DUPLICATE KEY UPDATE settings=VALUES(settings)",me,encode(settings));
        }
        for(var notification:list(request.get("notifications"))) {
            String id=text(notification.get("id")); if(id.length()>255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            // Only update read state of server-created notifications; never trust a client to fabricate senders.
            var existing=db.query("SELECT payload FROM user_notifications WHERE user_id=? AND notification_id=?",(r,n)->object(r.getString(1)),me,id);
            if(!existing.isEmpty()) {var value=existing.get(0);value.put("unread",Boolean.TRUE.equals(notification.get("unread")));db.update("UPDATE user_notifications SET payload=? WHERE user_id=? AND notification_id=?",encode(value),me,id);}
        }
    }
    private void syncConversation(long me,Map<String,Object> change) {
        if (text(change.get("name")).length()>150 || text(map(change.get("fields")).get("name")).length()>150) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Conversation name is too long");
        String key=text(change.get("id"));
        boolean addMembers = change.containsKey("addMembers");
        if (key.startsWith("legacy-")) { updateLegacyPreferences(me,key,map(change.get("fields"))); return; }
        if(!key.matches("[A-Za-z0-9_-]{1,255}") || key.startsWith("legacy-")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        Long databaseId = databaseConversationId(key);
        if(databaseId == null && db.queryForList("SELECT id FROM conversations WHERE external_id=?",key).isEmpty()) {
            if (addMembers) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm chat.");
            String type="GROUP".equals(change.get("type"))?"GROUP":"PRIVATE";
            Set<Long> memberIds=new LinkedHashSet<>();memberIds.add(me);
            if("PRIVATE".equals(type)) {
                for(Object id:(List<?>)change.getOrDefault("participants",List.of()))memberIds.add(accountId(text(id)));
                if(memberIds.size()!=2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Direct chat requires two accounts");
                var external=memberIds.stream().map(id->text(account(id).get("id"))).sorted().toList();
                if(!key.equals("conv-"+String.join("-",external))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid direct conversation key");
            } else for(var member:list(change.get("members")))memberIds.add(accountId(text(member.get("id"))));
            db.update("INSERT INTO conversations(external_id,type,name,created_by) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE external_id=VALUES(external_id)",key,type,text(change.get("name")),me);
            long cid=db.queryForObject("SELECT id FROM conversations WHERE external_id=?",Long.class,key);
            for(long member:memberIds)db.update("INSERT INTO conversation_members(conversation_id,user_id,role) VALUES (?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)",cid,member,member==me?"OWNER":"MEMBER");
        }
        if (!webStorageEnabled) {
            // A simultaneous send and preference update must read and write the same current member preferences.
            if (databaseId == null) {
                db.queryForList("SELECT id FROM conversations WHERE external_id=? FOR UPDATE", key);
            } else {
                db.queryForList("SELECT id FROM conversations WHERE id=? FOR UPDATE", databaseId);
            }
        }
        var row=membership(me,key);long cid=number(row.get("id"));
        Map<String,Object> fields=map(change.get("fields"));var prefs=object(text(row.get("preferences")));var meta=object(text(row.get("metadata")));
        boolean group = "GROUP".equals(row.get("type"));
        if (addMembers && !group) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể thêm thành viên vào nhóm chat.");
        }
        boolean enforceBlocking = !directOnlyBlocking || "PRIVATE".equals(row.get("type"));
        if (!enforceBlocking && Boolean.TRUE.equals(fields.get("blocked"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể chặn hội thoại riêng.");
        }
        for(var entry:fields.entrySet()) {
            if(PERSONAL.contains(entry.getKey())) prefs.put(entry.getKey(),entry.getValue());
            if(SHARED.contains(entry.getKey())) meta.put(entry.getKey(),entry.getValue());
        }
        if (!webStorageEnabled && Boolean.TRUE.equals(fields.get("markedUnread"))) {
            prefs.put("markedUnread", true);
        }
        if(fields.containsKey("unreadCount") && number(fields.get("unreadCount"))==0) {
            var readIds=fields.containsKey("readThrough")
                ? db.query("SELECT id FROM messages WHERE conversation_id=? AND COALESCE(external_id, CONCAT('db-msg-', id))=?",
                        (r,n)->r.getLong(1),cid,text(fields.get("readThrough")))
                : db.query("SELECT MAX(id) FROM messages WHERE conversation_id=?",(r,n)->r.getLong(1),cid);
            if(!readIds.isEmpty() && readIds.get(0)>0) {
                long readId = readIds.get(0);
                db.update("UPDATE conversation_members SET last_read_message_id=? WHERE conversation_id=? AND user_id=? "
                                + "AND (last_read_message_id IS NULL OR last_read_message_id<?)", readId, cid, me, readId);
            }
            prefs.remove("unreadCount");
            if (!webStorageEnabled) prefs.remove("markedUnread");
        }
        db.update("UPDATE conversation_members SET preferences=? WHERE conversation_id=? AND user_id=?",encode(prefs),cid,me);
        db.update("UPDATE conversations SET metadata=? WHERE id=?",encode(meta),cid);
        boolean owner=number(row.get("created_by"))==me;
        if(fields.containsKey("name") && group) {
            if(!owner) throw denied();
            db.update("UPDATE conversations SET name=? WHERE id=?",text(fields.get("name")),cid);
        }
        boolean replaceMembers = change.containsKey("members");
        if (group && (replaceMembers || addMembers)) {
            if(!owner) throw denied();
            Set<Long> wanted=new LinkedHashSet<>();wanted.add(me);
            for(var member:list(change.get("members")))wanted.add(accountId(text(member.get("id"))));
            if (addMembers) {
                if (!(change.get("addMembers") instanceof List<?> ids) || ids.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một thành viên.");
                }
                for (Object id : ids) wanted.add(accountId(text(id)));
            }
            for(long id:wanted)db.update("INSERT INTO conversation_members(conversation_id,user_id,role) VALUES (?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)",cid,id,id==me?"OWNER":"MEMBER");
            if (replaceMembers) {
                var previous=db.query("SELECT user_id FROM conversation_members WHERE conversation_id=?",(r,n)->r.getLong(1),cid);
                for(long id:previous)if(!wanted.contains(id))db.update("DELETE FROM conversation_members WHERE conversation_id=? AND user_id=?",cid,id);
            }
        }
        for(var message:list(change.get("messages")))saveMessage(me,cid,key,message,enforceBlocking);
        publish(cid,null);
    }
    private void saveMessage(long me,long cid,String key,Map<String,Object> incoming,boolean enforceBlocking) {
        String id=text(incoming.get("id"));if(!id.matches("[A-Za-z0-9_-]{1,64}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid message id");
        var found=db.queryForList("SELECT id,sender_id,metadata FROM messages WHERE external_id=? AND conversation_id=? FOR UPDATE",id,cid);
        if(!found.isEmpty()) {
            var old=found.get(0);var metadata=object(text(old.get("metadata")));
            if(incoming.containsKey("reaction"))metadata.put("reaction",incoming.get("reaction"));
            if(Boolean.TRUE.equals(incoming.get("unsent"))) {
                if(number(old.get("sender_id"))!=me)throw denied();
                metadata.put("unsent",true);metadata.remove("imageData");metadata.remove("fileData");metadata.remove("imageUrl");
                db.update("UPDATE messages SET content=? WHERE id=?","Đã thu hồi một tin nhắn",old.get("id"));
            }
            db.update("UPDATE messages SET metadata=? WHERE id=?",encode(metadata),old.get("id"));return;
        }
        if(!text(account(me).get("id")).equals(text(incoming.get("senderId"))))throw denied();
        // A blocked recipient must not receive or persist new incoming messages.
        var memberPrefs=db.query("SELECT user_id,preferences FROM conversation_members WHERE conversation_id=?",(r,n)->Map.of("user",r.getLong(1),"prefs",object(r.getString(2))),cid);
        if (enforceBlocking && memberPrefs.stream().anyMatch(p -> Boolean.TRUE.equals(map(p.get("prefs")).get("blocked")))) {
            if (directOnlyBlocking) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hội thoại đang bị chặn.");
            }
            throw denied();
        }
        String content=text(incoming.get("content"));
        if ((content.isBlank() && !incoming.containsKey("imageUrl")) || content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid message content");
        Map<String,Object> metadata=new LinkedHashMap<>();
        for(String field:List.of("replyTo","imageUrl","imageData","fileData","fileName","fileSize","fileType","isSystemNotice","sidebarPreview"))if(incoming.containsKey(field))metadata.put(field,incoming.get(field));
        db.update("INSERT INTO messages(conversation_id,sender_id,content,external_id,metadata) VALUES (?,?,?,?,?)",cid,me,text(incoming.get("content")),id,encode(metadata));
        var message=messages(cid).stream().filter(m->id.equals(m.get("id"))).findFirst().orElseThrow();
        for(var recipient:memberPrefs) {
            long uid=number(recipient.get("user"));if(uid==me)continue;
            if (webStorageEnabled) {
                var notification=new LinkedHashMap<String,Object>();notification.put("id","message-"+id);notification.put("conversationId",key);
                notification.put("title","GROUP".equals(db.queryForObject("SELECT type FROM conversations WHERE id=?",String.class,cid))?db.queryForObject("SELECT name FROM conversations WHERE id=?",String.class,cid):message.get("senderName"));
                notification.put("content",text(message.get("senderName"))+": "+text(message.get("content")));notification.put("unread",true);notification.put("type","message");notification.put("time","Vừa xong");
                db.update("INSERT INTO user_notifications(user_id,notification_id,payload) VALUES (?,?,?)",uid,"message-"+id,encode(notification));
            }
            var pref=map(recipient.get("prefs"));pref.remove("unreadCount");pref.remove("hidden");db.update("UPDATE conversation_members SET preferences=? WHERE conversation_id=? AND user_id=?",encode(pref),cid,uid);
        }
        publish(cid,message);
    }
    private void requireWebStorage() {
        if (!webStorageEnabled) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private void updateLegacyPreferences(long me,String key,Map<String,Object> fields) {
        requireWebStorage();
        if (key.length()<44) throw denied();
        String source=key.substring(7,43);
        var rows=db.query("SELECT payload FROM legacy_chat_imports WHERE user_id=? AND source_id=? FOR UPDATE",(r,n)->object(r.getString(1)),me,source);
        if (rows.isEmpty()) throw denied();
        var archive=rows.get(0);var conversations=list(archive.get("conversations"));boolean found=false;
        for (var old:conversations) if (key.equals("legacy-"+source+"-"+text(old.get("id")))) {
            var prefs=map(old.get("legacyPreferences"));
            fields.forEach((k,v)->{if(PERSONAL.contains(k) || SHARED.contains(k)) prefs.put(k,v);});
            old.put("legacyPreferences",prefs);found=true;
        }
        if(!found) throw denied();archive.put("conversations",conversations);
        db.update("UPDATE legacy_chat_imports SET payload=? WHERE user_id=? AND source_id=?",encode(archive),me,source);
    }
    private Optional<Long> legacyAccount(String external,List<Map<String,Object>> contacts) {
        if(external.isBlank()) return Optional.empty();
        var actual=identity.lookup(external);
        if(actual.isPresent()) return Optional.of(actual.get().getId());
        for(var contact:contacts) if(external.equals(text(contact.get("id")))) {
            String email=text(contact.get("email"));
            if(!email.isBlank()) {
                var matches=db.query("SELECT id FROM users WHERE email=?",(r,n)->r.getLong(1),email);
                if(matches.size()==1) return Optional.of(matches.get(0));
            }
        }
        return Optional.empty();
    }
    @Transactional
    public Map<String,String> continueLegacy(Principal principal,String key) {
        requireWebStorage();
        long me=identity.id(principal);
        if(key.length()<44 || !key.startsWith("legacy-")) throw denied();
        String source=key.substring(7,43);
        var rows=db.query("SELECT payload FROM legacy_chat_imports WHERE user_id=? AND source_id=? FOR UPDATE",(r,n)->object(r.getString(1)),me,source);
        if(rows.isEmpty()) throw denied();
        var archive=rows.get(0);var oldConversations=list(archive.get("conversations"));
        var old=oldConversations.stream().filter(c->key.equals("legacy-"+source+"-"+text(c.get("id")))).findFirst().orElseThrow(this::denied);
        String linked=text(old.get("linkedConversation"));
        if(!linked.isBlank()) { membership(me,linked);return Map.of("conversationId",linked); }
        var contacts=new ArrayList<>(list(archive.get("contacts")));contacts.addAll(list(old.get("members")));
        Set<String> identifiers=new LinkedHashSet<>();
        for(Object id:(List<?>)old.getOrDefault("participants",List.of())) identifiers.add(text(id));
        for(var member:list(old.get("members"))) identifiers.add(text(member.get("id")));
        if(old.get("contactId")!=null) identifiers.add(text(old.get("contactId")));
        if(!"GROUP".equals(old.get("type")) && identifiers.isEmpty()) {
            for(var message:list(old.get("messages"))) identifiers.add(text(message.get("senderId")));
        }
        Set<Long> members=new LinkedHashSet<>();members.add(me);
        for(String id:identifiers) {
            var resolved=legacyAccount(id,contacts);
            if(resolved.isPresent()) members.add(resolved.get());
            else if(!id.isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Không tìm thấy tài khoản thật của thành viên cũ. Hãy chọn người nhận từ danh sách bạn bè.");
        }
        boolean group="GROUP".equals(old.get("type"));
        if(members.size()<2 || (!group && members.size()!=2)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Không xác định được người nhận của lịch sử cũ. Hãy mở chat từ danh sách bạn bè.");
        var participants=members.stream().map(id->text(account(id).get("id"))).sorted().toList();
        linked=group?"room-"+UUID.nameUUIDFromBytes((old.get("id")+":"+String.join(",",participants)).getBytes(java.nio.charset.StandardCharsets.UTF_8)):"conv-"+String.join("-",participants);
        Map<String,Object> command=new LinkedHashMap<>();command.put("id",linked);command.put("type",group?"GROUP":"DIRECT");
        command.put("name",text(old.get("name")));command.put("participants",participants);
        if(group && db.queryForObject("SELECT COUNT(*) FROM conversations WHERE external_id=?",Long.class,linked)==0) command.put("members",members.stream().map(this::account).toList());
        command.put("fields",Map.of("archived",false,"hidden",false));
        syncConversation(me,command);
        old.put("linkedConversation",linked);archive.put("conversations",oldConversations);
        db.update("UPDATE legacy_chat_imports SET payload=? WHERE user_id=? AND source_id=?",encode(archive),me,source);
        return Map.of("conversationId",linked);
    }
    private Object removeCredentials(Object value) {
        if(value instanceof Map<?,?> values) {
            Map<String,Object> safe=new LinkedHashMap<>();
            values.forEach((k,v)->{if(!Set.of("password","passwordHash","password_hash","accessToken","refreshToken").contains(k.toString())) safe.put(k.toString(),removeCredentials(v));});
            return safe;
        }
        if(value instanceof List<?> values) return values.stream().map(this::removeCredentials).toList();
        return value;
    }
    @Transactional
    public void importLegacy(Principal principal,Map<String,Object> archive) {
        requireWebStorage();
        archive=map(removeCredentials(archive));
        long me=identity.id(principal);String source=text(archive.get("sourceId"));
        if(!source.matches("[a-fA-F0-9-]{36}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        // Historical client data is an account-private archive, never attributed to other accounts as verified messages.
        db.update("INSERT INTO legacy_chat_imports(user_id,source_id,payload) VALUES (?,?,?) ON DUPLICATE KEY UPDATE source_id=VALUES(source_id)",me,source,encode(archive));
        for (var old : list(archive.get("notifications"))) {
            String oldId=text(old.get("id"));
            String id="legacy-"+source+"-"+oldId;
            if (id.length()>255) continue;
            old.put("id",id);
            if (old.get("conversationId")!=null) old.put("conversationId","legacy-"+source+"-"+old.get("conversationId"));
            db.update("INSERT INTO user_notifications(user_id,notification_id,payload) VALUES (?,?,?) ON DUPLICATE KEY UPDATE notification_id=VALUES(notification_id)",me,id,encode(old));
        }
        if (!map(archive.get("settings")).isEmpty() && db.queryForObject("SELECT COUNT(*) FROM user_preferences WHERE user_id=?",Long.class,me)==0) {
            sync(principal,Map.of("settings",map(archive.get("settings"))));
        }
    }
    @Transactional
    public void sendPrivate(Principal principal,String recipient,String content) {
        long me=identity.id(principal);long other=accountId(recipient);
        var participants=List.of(text(account(me).get("id")),text(account(other).get("id"))).stream().sorted().toList();
        String key="conv-"+String.join("-",participants);
        var message=Map.of("id",UUID.randomUUID().toString(),"senderId",account(me).get("id"),"content",content);
        sync(principal,Map.of("conversations",List.of(Map.of("id",key,"type","DIRECT","participants",participants,"messages",List.of(message)))));
    }
    @Transactional
    public boolean claimDesktop(Principal principal,String key,String action,String browserSession) {
        requireWebStorage();
        long me=identity.id(principal);if(key.length()>255)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        String session;
        try { session=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(browserSession.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        long now=System.currentTimeMillis();
        if("complete".equals(action)) {db.update("UPDATE desktop_deliveries SET delivered=TRUE WHERE user_id=? AND session_key=? AND message_key=?",me,session,key);return true;}
        if("release".equals(action)) {db.update("DELETE FROM desktop_deliveries WHERE user_id=? AND session_key=? AND message_key=? AND delivered=FALSE",me,session,key);return true;}
        db.update("INSERT INTO desktop_deliveries(user_id,session_key,message_key,claimed_at) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE message_key=VALUES(message_key)",me,session,key,0);
        int changed=db.update("UPDATE desktop_deliveries SET claimed_at=? WHERE user_id=? AND session_key=? AND message_key=? AND delivered=FALSE AND claimed_at<?",now,me,session,key,now-15000);
        db.update("DELETE FROM desktop_deliveries WHERE user_id=? AND delivered=TRUE AND claimed_at<?",me,now-86400000);
        return changed==1;
    }
}
