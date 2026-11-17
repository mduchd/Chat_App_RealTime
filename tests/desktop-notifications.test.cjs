const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('js/app.js', 'utf8');
const deliverySource = source.slice(source.indexOf('const deliveredDesktopMessages'), source.indexOf('function showToast('));
function sharedState() {
    const values = new Map(), queues = new Map();
    return {
        claim: async(body) => {
            if(body.action==='release'){values.delete(body.key);return {claimed:true};}
            if(body.action==='complete'){values.set(body.key,true);return {claimed:true};}
            if(values.has(body.key))return {claimed:false};values.set(body.key,false);return {claimed:true};
        },
        storage: {getItem: k => values.get(k) || null, setItem: (k,v) => values.set(k,v), removeItem:k=>values.delete(k), key:i=>[...values.keys()][i],get length(){return values.size;}},
        locks: {async request(key, fn) { const before = queues.get(key) || Promise.resolve(); const result = before.then(fn); queues.set(key,result.catch(()=>{})); return result; }}
    };
}
function app(options={}, shared=sharedState()) {
    const notices=[], sounds=[], toasts=[];
    const context = {
        CURRENT_USER:{id:'me'}, activeConversationId:options.reading?'conv':'other',
        document:{hidden:options.hidden!==false,hasFocus:()=>!!options.focus},
        USER_SETTINGS:{desktopNotif:options.enabled!==false,soundNotif:true}, NOTIFICATIONS:options.inbox || [],
        navigator:{locks:shared.locks}, chatApi:async(path,body)=>shared.claim(body),
        window:{isSecureContext:true,location:{href:'https://chat.example/'},focus(){}},
        Notification:class {static permission=options.permission || 'granted'; constructor(title,opts){if(options.constructorFails)throw new TypeError('Unsupported');notices.push({title,...opts});} close(){}},
        saveNotifications(){},renderNotifications(){},getDisplayConversationName:()=> 'Lan',selectConversation(){},
        showToast:(...args)=>toasts.push(args),playSubtleBeep:()=>sounds.push(1),
        URL, console:{warn(){}},setTimeout:(fn,ms)=>setTimeout(fn,ms).unref(),
    };
    context.window.Notification=context.Notification;
    if (options.worker || options.workerFails || options.registrationFails) context.navigator.serviceWorker={
        register:async()=>{if(options.registrationFails)throw Error('Registration failed');},
        ready:Promise.resolve({showNotification:async(title,opts)=>{if(options.workerFails)throw Error('Worker failed');notices.push({title,...opts});}})
    };
    vm.createContext(context); vm.runInContext(deliverySource,context);
    return {context,notices,sounds,toasts,conv:{id:'conv',muted:!!options.muted},message:{id:'msg1',senderId:'sender',senderName:'Lan',content:'hello'}};
}
const flush = () => new Promise(resolve=>setImmediate(resolve));
test('each successive message gets an independent desktop tag',async()=>{
 const a=app(); await a.context.deliverDesktopMessage(a.conv,a.message);
 await a.context.deliverDesktopMessage(a.conv,{...a.message,id:'msg2'});
 assert.equal(a.notices.length,2);assert.notEqual(a.notices[0].tag,a.notices[1].tag);assert.equal(a.notices[0].body,'Lan: hello');
});
test('saved inbox never prevents desktop delivery',async()=>{
 const a=app({inbox:[{id:'message-msg1'}]});a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.notices.length,1);
});
test('two tabs receiving a message simultaneously deliver exactly once',async()=>{
 const shared=sharedState(),a=app({},shared),b=app({},shared);
 await Promise.all([a.context.deliverDesktopMessage(a.conv,a.message),b.context.deliverDesktopMessage(b.conv,b.message)]);
 assert.equal(a.notices.length+b.notices.length,1);
});
test('focused page still sends desktop for another conversation',async()=>{
 const a=app({hidden:false,focus:true});a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.notices.length,1);
});
test('reading current conversation, muted, blocked, disabled, denied suppress desktop',async()=>{
 for(const options of [{reading:true,hidden:false,focus:true},{muted:true},{enabled:false},{permission:'denied'}]){
  const a=app(options);a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.notices.length,0);
 }
 const a=app();a.conv.blocked=true;a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.notices.length,0);
});
test('worker succeeds even when page Notification constructor is unavailable',async()=>{
 const a=app({worker:true,constructorFails:true});assert.equal(await a.context.deliverDesktopMessage(a.conv,a.message),true);assert.equal(a.notices.length,1);assert.equal(a.notices[0].data.conversationId,'conv');
});
test('worker registration or display failure falls back to page notifications',async()=>{
 for(const options of [{workerFails:true},{registrationFails:true}]){
  const a=app(options);assert.equal(await a.context.deliverDesktopMessage(a.conv,a.message),true);assert.equal(a.notices.length,1);
 }
});
test('failed desktop attempt is retryable and does not suppress sound',async()=>{
 const a=app({constructorFails:true});a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.sounds.length,1);
 a.context.Notification=class{static permission='granted';constructor(){a.notices.push(1);}};
 assert.equal(await a.context.deliverDesktopMessage(a.conv,a.message),true);assert.equal(a.notices.length,1);
});
test('disabling desktop does not disable the sound setting',async()=>{
 const a=app({enabled:false});a.context.notifyIncomingMessage(a.conv,a.message);await flush();assert.equal(a.sounds.length,1);assert.equal(a.notices.length,0);
});
test('production static copies stay synchronized',()=>{
 for(const path of ['js/app.js','notification-worker.js','index.html'])assert.equal(fs.readFileSync(path,'utf8'),fs.readFileSync('src/main/resources/static/'+path,'utf8'));
});
test('socket message already saved by another tab still attempts desktop delivery',async()=>{
 const handler=source.slice(source.indexOf('async function handleIncomingSocketMessage('),source.indexOf('function getInitials('));
 const a=app(); a.context.CONTACTS_DIRECTORY=[{id:'sender',name:'Lan'}];
 a.context.CONVERSATIONS=[{...a.conv,type:'DIRECT',participants:['me','sender'],messages:[a.message]}];
 a.context.resolveConversationProfile=async()=>{};
 vm.runInContext(handler,a.context);
 await a.context.handleIncomingSocketMessage({id:'msg1',sender:'sender',recipient:'me',senderName:'Lan',content:'hello'});
 await flush();assert.equal(a.notices.length,1);assert.equal(a.context.CONVERSATIONS[0].messages.length,1);
});
test('server sender name allows immediate delivery without waiting for profile fetch',async()=>{
 const handler=source.slice(source.indexOf('async function handleIncomingSocketMessage('),source.indexOf('function getInitials('));
 const a=app();a.context.CONTACTS_DIRECTORY=[];a.context.CONVERSATIONS=[];a.context.realtimeBus=null;
 a.context.resolveConversationProfile=()=>new Promise(()=>{});
 a.context.appendMessageToConversation=(id,message)=>a.context.CONVERSATIONS.find(c=>c.id===id).messages.push(message);
 vm.runInContext(handler,a.context);
 await a.context.handleIncomingSocketMessage({id:'msg1',sender:'sender',recipient:'me',senderName:'Lan',content:'hello'});
 await flush();assert.equal(a.notices.length,1);assert.equal(a.context.CONVERSATIONS[0].messages[0].senderName,'Lan');
});
