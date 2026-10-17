(function() {
    if (!window.LiteSpaceNotificationBridge || window.__litespaceNotificationsPatched) return;
    window.__litespaceNotificationsPatched = true;
    function LiteSpaceNotification(title, options) {
        options = options || {};
        this.title = String(title || '');
        this.body = String(options.body || '');
        this.tag = String(options.tag || '');
        this.icon = String(options.icon || '');
        this.onclick = null;
        this.onclose = null;
        if (LiteSpaceNotification.permission === 'granted') {
            try {
                window.LiteSpaceNotificationBridge.postWebNotification(this.title, this.body, this.tag, window.location.href);
            } catch (e) {}
        }
    }
    Object.defineProperty(LiteSpaceNotification, 'permission', {
        get: function() {
            try { return window.LiteSpaceNotificationBridge.getPermissionState(window.location.origin); }
            catch (e) { return 'default'; }
        }
    });
    LiteSpaceNotification.requestPermission = function(callback) {
        return new Promise(function(resolve) {
            var status = 'denied';
            try { status = window.LiteSpaceNotificationBridge.requestPermissionFromPage(window.location.origin); }
            catch (e) { status = 'denied'; }
            if (typeof callback === 'function') callback(status);
            resolve(status);
        });
    };
    LiteSpaceNotification.prototype.close = function() {
        if (typeof this.onclose === 'function') this.onclose();
    };
    window.Notification = LiteSpaceNotification;
})();
