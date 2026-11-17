package com.teamchat.realtime_chat.controller;
import com.teamchat.realtime_chat.entity.User;
import com.teamchat.realtime_chat.repository.UserRepository;
import com.teamchat.realtime_chat.service.ChatStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:chat-storage;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "spring.session.jdbc.initialize-schema=always",
 "spring.jpa.show-sql=false", "spring.session.jdbc.platform=h2"
})
@AutoConfigureMockMvc
@Sql(scripts="/db/migration/V6__server_chat_storage.sql",statements={
 "ALTER TABLE users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
 "ALTER TABLE conversations ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
 "ALTER TABLE messages ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP",
 "ALTER TABLE conversation_members ALTER COLUMN joined_at SET DEFAULT CURRENT_TIMESTAMP"
},executionPhase=Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ChatStorageIntegrationTest {
 @Autowired ChatStorageService storage; @Autowired UserRepository users; @Autowired JdbcTemplate db; @Autowired MockMvc mvc;
 User user(boolean google) { User u=new User();String id=UUID.randomUUID().toString();u.setEmail(id+"@test.invalid");u.setDisplayName("User "+id);if(google)u.setGoogleSub("google-"+id);return users.saveAndFlush(u); }
 String ext(User u){return u.getGoogleSub()==null?u.getId().toString():u.getGoogleSub();}
 Principal auth(User u){return ()->ext(u);}
 String direct(User a,User b){return "conv-"+String.join("-",List.of(ext(a),ext(b)).stream().sorted().toList());}
 Map<String,Object> msg(User u,String id){return Map.of("id",id,"senderId",ext(u),"content","hello");}
 void create(User a,User b,String mid){storage.sync(auth(a),Map.of("conversations",List.of(Map.of("id",direct(a,b),"type","DIRECT","participants",List.of(ext(a),ext(b)),"fields",Map.of("pinned",true),"messages",List.of(msg(a,mid))))));}
 @SuppressWarnings("unchecked") List<Map<String,Object>> convs(User u){return (List<Map<String,Object>>)storage.snapshot(auth(u)).get("conversations");}
 @SuppressWarnings("unchecked") List<Map<String,Object>> msgs(Map<String,Object> c){return (List<Map<String,Object>>)c.get("messages");}
 @Test void committedChatAndPreferencesSurviveReloadForBothAccounts(){
  User a=user(true),b=user(false);String mid=UUID.randomUUID().toString();create(a,b,mid);
  assertEquals(mid,msgs(convs(b).get(0)).get(0).get("id"));assertEquals(ext(a),msgs(convs(b).get(0)).get(0).get("senderId"));
  assertEquals(true,convs(a).get(0).get("pinned"));assertNotEquals(true,convs(b).get(0).get("pinned"));assertEquals(1L,((Number)convs(b).get(0).get("unreadCount")).longValue());
  assertEquals(1,((List<?>)storage.snapshot(auth(b)).get("notifications")).size());
  create(a,b,mid);assertEquals(1,msgs(convs(b).get(0)).size());
  storage.sync(auth(b),Map.of("conversations",List.of(Map.of("id",direct(a,b),"fields",Map.of("unreadCount",0)))));
  assertEquals(0L,((Number)convs(b).get(0).get("unreadCount")).longValue());
  storage.sync(auth(a),Map.of("settings",Map.of("darkMode",true,"soundNotif",false)));
  assertEquals(true,((Map<?,?>)storage.snapshot(auth(a)).get("settings")).get("darkMode"));assertTrue(((Map<?,?>)storage.snapshot(auth(b)).get("settings")).isEmpty());
 }
 @Test void outsidersCannotReadChangeOrSpoofMessages(){
  User a=user(true),b=user(false),c=user(false);String mid=UUID.randomUUID().toString();create(a,b,mid);assertTrue(convs(c).isEmpty());
  assertThrows(ResponseStatusException.class,()->storage.sync(auth(c),Map.of("conversations",List.of(Map.of("id",direct(a,b),"fields",Map.of("name","hack"))))));
  assertThrows(ResponseStatusException.class,()->storage.sync(auth(b),Map.of("conversations",List.of(Map.of("id",direct(a,b),"messages",List.of(msg(a,UUID.randomUUID().toString())))))));
  assertThrows(ResponseStatusException.class,()->storage.sync(auth(b),Map.of("conversations",List.of(Map.of("id",direct(a,b),"messages",List.of(Map.of("id",mid,"unsent",true)))))));
  assertEquals("hello",msgs(convs(a).get(0)).get(0).get("content"));
 }
 @Test void groupsAndMembershipAreDurableAndOwnerControlled(){
  User a=user(true),b=user(false),c=user(true);String key="room-"+UUID.randomUUID();
  storage.sync(auth(a),Map.of("conversations",List.of(Map.of("id",key,"type","GROUP","name","Team","members",List.of(Map.of("id",ext(a)),Map.of("id",ext(b))),"messages",List.of(msg(a,UUID.randomUUID().toString()))))));
  assertEquals("Team",convs(b).get(0).get("name"));
  assertThrows(ResponseStatusException.class,()->storage.sync(auth(b),Map.of("conversations",List.of(Map.of("id",key,"members",List.of(Map.of("id",ext(c))))))));
  storage.sync(auth(a),Map.of("conversations",List.of(Map.of("id",key,"members",List.of(Map.of("id",ext(a)),Map.of("id",ext(c)))))));
  assertTrue(convs(b).isEmpty());assertEquals(1,convs(c).size());
 }
 @Test void historicalDataImportsOnceAndStaysPrivate(){
  User a=user(true),b=user(false);String source=UUID.randomUUID().toString();
  var old=Map.of("id","old-conv","name","Old","type","DIRECT","messages",List.of(msg(b,"old-message")));
  var archive=Map.<String,Object>of("sourceId",source,"conversations",List.of(old),"settings",Map.of("soundNotif",false));
  storage.importLegacy(auth(a),archive);storage.importLegacy(auth(a),archive);
  assertEquals(1,convs(a).size());assertEquals(true,convs(a).get(0).get("legacyReadOnly"));assertEquals("hello",msgs(convs(a).get(0)).get(0).get("content"));assertTrue(convs(b).isEmpty());
  assertEquals(0L,db.queryForObject("SELECT COUNT(*) FROM messages WHERE external_id='old-message'",Long.class));
 }
 @Test void desktopDeliveryClaimsAreAtomicAndRetryable(){
  User a=user(true),b=user(false);String key=UUID.randomUUID().toString();assertTrue(storage.claimDesktop(auth(a),key,"claim","browser-one"));assertFalse(storage.claimDesktop(auth(a),key,"claim","browser-one"));
  storage.claimDesktop(auth(a),key,"release","browser-one");assertTrue(storage.claimDesktop(auth(a),key,"claim","browser-one"));storage.claimDesktop(auth(a),key,"complete","browser-one");assertFalse(storage.claimDesktop(auth(a),key,"claim","browser-one"));assertTrue(storage.claimDesktop(auth(b),key,"claim","browser-one"));assertTrue(storage.claimDesktop(auth(a),key,"claim","browser-two"));
 }
 @Test void importedDirectChatCanContinueWithHistoryPreservedAndNewMessagesShared(){
  User a=user(true),b=user(false),outsider=user(false);String source=UUID.randomUUID().toString();
  var old=Map.of("id","old-direct","type","DIRECT","name","Old friend","participants",List.of(ext(a),ext(b)),"messages",List.of(msg(b,"historic-id")));
  storage.importLegacy(auth(a),Map.of("sourceId",source,"conversations",List.of(old)));
  String archive="legacy-"+source+"-old-direct";
  assertThrows(ResponseStatusException.class,()->storage.continueLegacy(auth(outsider),archive));
  String live=storage.continueLegacy(auth(a),archive).get("conversationId");
  assertEquals(direct(a,b),live);assertEquals(live,storage.continueLegacy(auth(a),archive).get("conversationId"));
  assertEquals(1,convs(a).size());assertNotEquals(true,convs(a).get(0).get("legacyReadOnly"));
  assertEquals(true,msgs(convs(a).get(0)).get(0).get("legacyMessage"));assertTrue(msgs(convs(b).get(0)).isEmpty());
  String mid=UUID.randomUUID().toString();var change=Map.of("id",live,"messages",List.of(msg(a,mid)));storage.sync(auth(a),Map.of("conversations",List.of(change)));
  assertEquals(2,msgs(convs(a).get(0)).size());assertEquals(1,msgs(convs(b).get(0)).size());assertEquals(mid,msgs(convs(b).get(0)).get(0).get("id"));
  assertEquals(0L,db.queryForObject("SELECT COUNT(*) FROM messages WHERE external_id='historic-id'",Long.class));
 }
 @Test void importedGroupCanContinueForRealMembers(){
  User a=user(true),b=user(false);String source=UUID.randomUUID().toString();
  var old=Map.of("id","old-group","type","GROUP","name","Original group","members",List.of(Map.of("id",ext(a)),Map.of("id",ext(b))),"messages",List.of(msg(a,"old-group-message")));
  storage.importLegacy(auth(a),Map.of("sourceId",source,"conversations",List.of(old)));
  String live=storage.continueLegacy(auth(a),"legacy-"+source+"-old-group").get("conversationId");
  assertEquals("GROUP",convs(a).get(0).get("type"));assertEquals("Original group",convs(b).get(0).get("name"));
  assertEquals(live,storage.continueLegacy(auth(a),"legacy-"+source+"-old-group").get("conversationId"));
  assertEquals(1,convs(a).size());
 }
 @Test void endpointsRejectAnonymousAndUntrustedRequests() throws Exception {
  mvc.perform(get("/api/chat/snapshot")).andExpect(status().is3xxRedirection());
  mvc.perform(post("/api/chat/sync").header("Origin","https://untrusted.invalid").contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
}
