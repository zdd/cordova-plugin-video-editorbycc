//
//  VideoEditorByCC.js
//

var exec = require('cordova/exec');

var VideoEditorByCC = function() {
};

VideoEditorByCC.compressVideo = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.compressVideo failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.compressVideo failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "compressVideo", [options]);
};

if (typeof module != 'undefined' && module.exports) {
  module.exports = VideoEditorByCC;
}
