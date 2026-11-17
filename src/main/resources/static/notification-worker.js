// Notifications only: do not cache or intercept application requests.
self.addEventListener('install', event => event.waitUntil(self.skipWaiting()));
self.addEventListener('activate', event => event.waitUntil(self.clients.claim()));
self.addEventListener('notificationclick', event => {
    event.notification.close();
    const { conversationId, url } = event.notification.data || {};
    const target = new URL(url || './', self.location.href);
    if (target.origin !== self.location.origin) return;
    if (conversationId) target.searchParams.set('conversation', conversationId);
    event.waitUntil((async () => {
        const tabs = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
        const tab = tabs.find(client => new URL(client.url).pathname === target.pathname);
        if (tab) {
            await tab.focus();
            tab.postMessage({ type: 'OPEN_CONVERSATION', conversationId });
        } else {
            await self.clients.openWindow(target.href);
        }
    })());
});
