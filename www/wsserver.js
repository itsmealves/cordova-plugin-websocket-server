/*global cordova, module*/

module.exports = {
    start: function (port, successCallback, errorCallback) {
        console.log('start called');
        cordova.exec(successCallback, errorCallback, "Server", "start", [port]);
    },
    stop: function(successCallback, errorCallback) {
        console.log('stop called');
        cordova.exec(successCallback, errorCallback, "Server", "stop", []);
    },
    onConnection: function(callback) {
        console.log('onConnection called');
        cordova.exec(callback, null, "Server", "setConnectionObserver", []);
    },
    onDisconnection: function(callback) {
        console.log('onDisconnection called');
        cordova.exec(callback, null, "Server", "setDisconnectionObserver", []);
    },
    onError: function(callback) {
        console.log('onError called');
        cordova.exec(callback, null, "Server", "setErrorObserver", []);
    },
    onMessage: function(callback) {
        console.log('setMessageObserver called');
        cordova.exec(callback, null, "Server", "setMessageObserver", []);
    },
    broadcast: function(message, callback) {
        console.log('broadcast called');
        cordova.exec(callback, null, "Server", "broadcast", [message]);
    },
    send: function(client, message, callback) {
        console.log('send called');
        var id = client.id;
        cordova.exec(callback, null, "Server", "send", [id, message]);
    }
};
