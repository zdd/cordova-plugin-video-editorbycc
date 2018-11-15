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

VideoEditorByCC.uploadVideo = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.uploadVideo failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.uploadVideo failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "uploadVideo", [options]);
};

VideoEditorByCC.reUploadVideo = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.reUploadVideo failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.reUploadVideo failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "reUploadVideo", [options]);
};

VideoEditorByCC.pauseUpload = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.pauseUpload failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.pauseUpload failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "pauseUpload", [options]);
};

VideoEditorByCC.resumeUpload = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.resumeUpload failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.resumeUpload failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "resumeUpload", [options]);
};

VideoEditorByCC.cancelUpload = function (successCallback, errorCallback, options) {
  console.log("options" + JSON.stringify(options));

  if (errorCallback == null) { errorCallback = function () { } }

  if (typeof errorCallback != "function") {
    console.log("VideoEditorByCC.cancelUpload failure: failure parameter not a function");
    return;
  }

  if (typeof successCallback != "function") {
    console.log("VideoEditorByCC.cancelUpload failure: success callback parameter must be a function");
    return;
  }

  exec(successCallback, errorCallback, "VideoEditorByCC", "cancelUpload", [options]);
};

if (typeof module != 'undefined' && module.exports) {
  module.exports = VideoEditorByCC;
}
