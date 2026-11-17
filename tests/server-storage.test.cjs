const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const {webcrypto} = require('node:crypto');
const source = fs.readFileSync('js/app.js','utf8');
const migration = source.slice(source.indexOf('async function importLegacyBrowserData('),source.indexOf('/**', source.indexOf('async function importLegacyBrowserData(')));
function migrationApp(fail=false) {
 const values = new Map([
  ['chatspace_conversations_v2:me', JSON.stringify([{id:'old',messages:[{id:'m1',content:'history'}]}])],
  ['chatspace_conversations_v2:other',JSON.stringify([{id:'other-history'}])],
  ['chatspace_notifications_v1:me',JSON.stringify([{id:'n1',unread:false}])],
  ['chatspace_notification_settings_v1',JSON.stringify({soundNotif:false})],
  ['chatspace_contacts_v2',JSON.stringify([{id:'someone',name:'Friend'}])]
 ]);
 const calls=[];
 const local = new Proxy({}, {ownKeys:()=>[...values.keys()],getOwnPropertyDescriptor:()=>({enumerable:true,configurable:true}),get:(_,k)=> k==='getItem' ? key=>values.get(key)||null : k==='removeItem' ? key=>values.delete(key):undefined});
 const context={CURRENT_USER:{id:'me'},window:{localStorage:local,sessionStorage:{removeItem(){}}},crypto:webcrypto,TextEncoder,showToast(){},
  chatApi:async(path,body)=>{calls.push({path,body});if(fail)throw Error('Offline');}
 };
 vm.createContext(context);vm.runInContext(migration,context);return {values,calls,context};
}
test('historical data is uploaded before removing only the authenticated account keys',async()=>{
 const a=migrationApp();await a.context.importLegacyBrowserData();
 assert.equal(a.calls[0].path,'/import');assert.equal(a.calls[0].body.conversations[0].messages[0].content,'history');
 assert.equal(a.values.has('chatspace_conversations_v2:me'),false);assert.equal(a.values.has('chatspace_notifications_v1:me'),false);
 assert.equal(a.values.has('chatspace_conversations_v2:other'),true);assert.equal(a.values.has('chatspace_contacts_v2'),true);
});
test('failed import keeps original browser history intact for retry',async()=>{
 const a=migrationApp(true);const raw=a.values.get('chatspace_conversations_v2:me');
 await assert.rejects(a.context.importLegacyBrowserData());assert.equal(a.values.get('chatspace_conversations_v2:me'),raw);
 assert.equal(a.values.has('chatspace_notification_settings_v1'),true);
});
test('retry after lost server response uses the same idempotency key',async()=>{
 const a=migrationApp(true);await assert.rejects(a.context.importLegacyBrowserData());await assert.rejects(a.context.importLegacyBrowserData());
 assert.equal(a.calls[0].body.sourceId,a.calls[1].body.sourceId);
});
test('no application code writes user data to browser storage',()=>{
 for(const file of ['js/app.js','src/main/resources/static/js/app.js']) {
  const code=fs.readFileSync(file,'utf8');assert.doesNotMatch(code,/(?:localStorage|sessionStorage)\s*\.\s*setItem\s*\(/);
  const withoutMigration=code.replace(migration,'');assert.doesNotMatch(withoutMigration,/(?:localStorage|sessionStorage)/);
 }
});
test('new writes send only changed conversation data without deleting unseen server conversations',async()=>{
 const storage=source.slice(source.indexOf('let serverHydrated ='),source.indexOf('// Read old browser data once'));
 const requests=[];const initial={id:'conv-me-other',type:'DIRECT',participants:['me','other'],name:'Other',messages:[],muted:false};
 const context={CURRENT_USER:{id:'me'},serverAuthenticated:true,CONVERSATIONS:[structuredClone(initial)],NOTIFICATIONS:[],
  USER_SETTINGS:{},DEFAULT_USER_SETTINGS:{},CONTACTS_DIRECTORY:[],structuredClone,AbortSignal,
  document:{body:{classList:{toggle(){}}},getElementById(){return null},hidden:true},activeConversationId:null,
  renderConversationList(){},renderNotifications(){},renderBlockedUsersInSettings(){},getConversationById(){return null},showToast(){},
  getBackendUrl:()=> 'https://api.example', fetch:async(url,options)=> {requests.push({url,body:options.body?JSON.parse(options.body):null});return {ok:true,status:200,headers:{get:()=>null},text:async()=>''};}
 };
 vm.createContext(context);vm.runInContext(storage,context);
 vm.runInContext('serverHydrated=true;serverShadow=new Map(CONVERSATIONS.map(c=>[c.id,structuredClone(c)]));CONVERSATIONS[0].muted=true;',context);
 await context.persistAndSyncConversations();
 assert.deepEqual(JSON.parse(JSON.stringify(requests[0].body.conversations[0].fields)),{muted:true});
 assert.equal(requests[0].body.conversations[0].messages.length,0);
});
test('opening imported history links once and selects the live chat for continued sending',async()=>{
 const continuation=source.slice(source.indexOf('const continuingLegacyConversations ='),source.indexOf('function selectConversation('));
 const calls=[],selections=[];let live=null;
 const context={CURRENT_USER:{id:'me'},activeConversationId:'legacy-old',
  chatApi:async(path,body)=>{calls.push({path,body});return {conversationId:'conv-me-other'};},
  refreshServerState:async()=>{live={id:'conv-me-other',messages:[{content:'old',legacyMessage:true}]};},
  getConversationById:id=>id===live?.id?live:null,setConversationFilter:filter=>selections.push(filter),selectConversation:id=>selections.push(id)
 };
 vm.createContext(context);vm.runInContext(continuation,context);
 const results=await Promise.all([context.resumeLegacyConversation({id:'legacy-old'}),context.resumeLegacyConversation({id:'legacy-old'})]);
 assert.deepEqual(results,['conv-me-other','conv-me-other']);assert.equal(calls.length,1);assert.equal(calls[0].path,'/continue');assert.deepEqual(selections,['all','conv-me-other']);assert.equal(live.messages[0].content,'old');
});
