/*global cordova, module*/

module.exports = {
    start: function (port, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Server", "start", [port]);
    },
    stop: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Server", "stop", []);
    },
    onMessage: function(callback) {
        cordova.exec(callback, null, "Server", "setMessageObserver", []);
    }
};
