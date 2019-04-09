package org.apache.cordova.videoeditorbycc;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.app.Activity;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.videoeditorbycc.compress.VideoCompress;
import org.apache.cordova.videoeditorbycc.upload.UploadInfo;
import org.apache.cordova.videoeditorbycc.upload.UploadService;
import org.apache.cordova.videoeditorbycc.util.ConfigUtil;
import org.apache.cordova.videoeditorbycc.util.DataSet;
import org.apache.cordova.videoeditorbycc.util.ParamsUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.bokecc.sdk.mobile.exception.ErrorCode;
import com.bokecc.sdk.mobile.upload.Uploader;
import com.bokecc.sdk.mobile.upload.VideoInfo;

/**
 * VideoCompress plugin for Android
 */
public class VideoEditorByCC extends CordovaPlugin {
  private static final String TAG = "VideoEditorByCC";
  private CallbackContext callback;
  private Context appContext;
  private UploadReceiver receiver;
  private UploadService.UploadBinder binder;
  private String currentUploadId = null;

  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();

    appContext = cordova.getActivity().getApplicationContext();
    DataSet.init(appContext);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Log.d(TAG, "execute method starting");

    this.callback = callbackContext;

    if (receiver == null) {
      receiver = new UploadReceiver();
      cordova.getActivity().registerReceiver(receiver, new IntentFilter(ConfigUtil.ACTION_UPLOAD));
    }
    cordova.getActivity().bindService(new Intent(appContext, UploadService.class), serviceConnection, Context.BIND_AUTO_CREATE);

    if (action.equals("compressVideo")) {
      try {
        this.compressVideo(args);
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    } else if (action.equals("uploadVideo")) {
      try {
        this.uploadVideo(args);
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    } else if (action.equals("reUploadVideo")) {
      try {
        this.reUploadVideo();
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    } else if (action.equals("pauseUpload")) {
      try {
        this.pauseUpload(args);
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    } else if (action.equals("resumeUpload")) {
      try {
        this.resumeUpload(args);
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    } else if (action.equals("cancelUpload")) {
      try {
        this.cancelUpload(args);
      } catch (Exception e) {
        callback.error(e.toString());
      }
      return true;
    }

    return false;
  }

  @Override
  public void onDestroy() {
    try{

      super.onDestroy();

      if (receiver != null) {
        cordova.getActivity().unregisterReceiver(receiver);
      }
      cordova.getActivity().unbindService(serviceConnection);
      DataSet.saveUploadData();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      binder = (UploadService.UploadBinder) service;
    }
  };

  /**
   * 视频压缩
   * @param args
     */
  private void compressVideo(JSONArray args) {
    JSONObject options = args.optJSONObject(0);
    Log.d(TAG, "compressVideo() options: " + options.toString());

    try {
      final File inFile = this.resolveLocalFileSystemURI(options.getString("fileUri"));
      if (!inFile.exists()) {
        Log.d(TAG, "input file does not exist");
        callback.error("input video does not exist.");
        return;
      }

      final String videoSrcPath = inFile.getAbsolutePath();
      Log.d(TAG, "videoSrcPath: " + videoSrcPath);


      final String outputFileName = options.optString(
        "outputFileName",
        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
      );
      final String outputExtension = ".mp4";

      final PackageManager pm = appContext.getPackageManager();

      ApplicationInfo ai;
      try {
        ai = pm.getApplicationInfo(cordova.getActivity().getPackageName(), 0);
      } catch (final NameNotFoundException e) {
        ai = null;
      }
      final String appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Unknown");

      final boolean saveToLibrary = options.optBoolean("saveToLibrary", true);
      File mediaStorageDir;

      if (saveToLibrary) {
        mediaStorageDir = new File(
          Environment.getExternalStorageDirectory() + "/Movies",
          appName
        );
      } else {
        mediaStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + cordova.getActivity().getPackageName() + "/files/files/videos");
      }

      if (!mediaStorageDir.exists()) {
        if (!mediaStorageDir.mkdirs()) {
          callback.error("Can't access or make Movies directory");
          return;
        }
      }

      final String outputFilePath = new File(
        mediaStorageDir.getPath(),
        outputFileName + outputExtension
      ).getAbsolutePath();

      Log.d(TAG, "outputFilePath: " + outputFilePath);

      final boolean deleteInputFile = options.optBoolean("deleteInputFile", false);
//      final int width = options.optInt("width", 0);
//      final int height = options.optInt("height", 0);
//      final int fps = options.optInt("fps", 24);
//      final int videoBitrate = options.optInt("videoBitrate", 1000000); // default to 1 megabit
//      final int audioBitrate = options.optInt("audioBitrate", 128000); // default to 1 megabit
//      final int audioChannels = options.optInt("audioChannels", 2); // default to 1 megabit
//      final int audioSampleRate = options.optInt("audioSampleRate", 44100); // default to 1 megabit
//      final long videoDuration = options.optLong("duration", 0) * 1000 * 1000;
      final int quality = options.optInt("quality", 3);

      // 视频压缩回调函数
      VideoCompress.CompressListener mCompressListener = new VideoCompress.CompressListener() {
        @Override
        public void onStart() {
          Log.d(TAG, "onStart()");
        }

        @Override
        public void onSuccess() {
          Log.d(TAG, "onSuccess()");

          File outFile = new File(outputFilePath);
          if (!outFile.exists()) {
            Log.d(TAG, "outputFile doesn't exist!");
            callback.error("an error ocurred during transcoding");
            return;
          }

          // make the gallery display the new file if saving to library
          if (saveToLibrary) {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(inFile));
            scanIntent.setData(Uri.fromFile(outFile));
            appContext.sendBroadcast(scanIntent);
          }

          if (deleteInputFile) {
            inFile.delete();
          }

          JSONObject jsonObj = new JSONObject();
          try {
            jsonObj.put("status", 400);
            jsonObj.put("result", outputFilePath);
          } catch (JSONException e) {
            e.printStackTrace();
            callback.error(e.toString());
          }

          callback.success(jsonObj);
        }

        @Override
        public void onFail() {
          Log.d(TAG, "onFail()");

          callback.error("transcode error");
        }

        @Override
        public void onProgress(float percent) {
          Log.d(TAG, "onProgress() percent:" + percent);

          JSONObject jsonObj = new JSONObject();
          try {
            jsonObj.put("status", 200);
            jsonObj.put("progress", percent);
          } catch (JSONException e) {
            e.printStackTrace();
            callback.error(e.toString());
          }

          PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
          progressResult.setKeepCallback(true);
          callback.sendPluginResult(progressResult);
        }
      };

      switch (quality) {
        case 1:
          VideoCompress.compressVideoHigh(videoSrcPath, outputFilePath, mCompressListener);
          break;
        case 2:
          VideoCompress.compressVideoMedium(videoSrcPath, outputFilePath, mCompressListener);
          break;
        case 3:
          VideoCompress.compressVideoLow(videoSrcPath, outputFilePath, mCompressListener);
          break;
        case 4:
          int resultWidth = options.optInt("width", 960);
          int resultHeight = options.optInt("height", 540);
          int bitrate = options.optInt("bitrate", 1000000);
          VideoCompress.compressVideoCustomize(resultWidth, resultHeight, bitrate, videoSrcPath, outputFilePath, mCompressListener);
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
      callback.error(e.toString());
    }
  }

  /**
   * 视频上传方法
   * @param args
     */
  private void uploadVideo(JSONArray args) {
    try {
      JSONObject options = args.optJSONObject(0);
      Log.d(TAG, "uploadVideo() options: " + options.toString());

      final String uploadId = UploadInfo.UPLOAD_PRE.concat(System.currentTimeMillis() + "");
      final VideoInfo videoInfo = new VideoInfo();
      videoInfo.setTitle(options.optString("title", ""));
      videoInfo.setTags(options.optString("tag", ""));
      videoInfo.setDescription(options.optString("desc", ""));
      videoInfo.setCategoryId(options.optString("categoryId", ""));
      videoInfo.setFilePath(options.optString("filePath", ""));
      videoInfo.setNotifyUrl(options.optString("notifyUrl", ""));

      DataSet.addUploadInfo(new UploadInfo(uploadId, videoInfo, Uploader.WAIT, 0, null));
      cordova.getActivity().sendBroadcast(new Intent(ConfigUtil.ACTION_UPLOAD));

      if (binder.isStop()) {
        Intent service = new Intent(appContext, UploadService.class);

        service.putExtra("title", videoInfo.getTitle());
        service.putExtra("tag", videoInfo.getTags());
        service.putExtra("desc", videoInfo.getDescription());
        service.putExtra("categoryId", videoInfo.getCategoryId());
        service.putExtra("filePath", videoInfo.getFilePath());
        service.putExtra("uploadId", uploadId);
        service.putExtra("notifyUrl", videoInfo.getNotifyUrl());

        cordova.getActivity().startService(service);
      }
    } catch (Exception e) {
      callback.error(e.toString());
    }
  }

  /**
   * 在应用启动的时候调用此方法初始化数据库
   */
  private void initDB() {
    DataSet.init(appContext);
  }

  /**
   * 在导致上传中断的位置调用此方法，保存视频上传进度信息，以便进行断点续传
   */
  private void saveUploadInfo() {
    DataSet.saveUploadData();
  }

  /**
   * 若有未上传完成的视频，则触发断点续传
   */
  private void reUploadVideo() {
    List<UploadInfo> uploadInfos = DataSet.getUploadInfos();

    boolean flag = false;
    // 获取数据库中存储的视频状态，若未上传完成，则触发断点续传
    for (UploadInfo uploadInfo: uploadInfos) {
      if (uploadInfo != null && (uploadInfo.getStatus() != Uploader.FINISH)) {
        flag = true;
        startUploadService(uploadInfo);
        break;
      }
    }
    if (!flag) {
      Log.d(TAG, "不存在未上传完成的视频");
      JSONObject jsonObj = new JSONObject();
      try {
        jsonObj.put("status", 500);
        jsonObj.put("info", "不存在未上传完成的视频");
        callback.error(jsonObj);
      } catch (JSONException e) {
        e.printStackTrace();
        callback.error(e.toString());
      }
    }
  }

  private void startUploadService(UploadInfo uploadInfo) {
    Intent service = new Intent(appContext, UploadService.class);
    VideoInfo videoInfo = uploadInfo.getVideoInfo();
    service.putExtra("title", videoInfo.getTitle());
    service.putExtra("tag", videoInfo.getTags());
    service.putExtra("desc", videoInfo.getDescription());
    service.putExtra("filePath", videoInfo.getFilePath());
    service.putExtra("uploadId", uploadInfo.getUploadId());
    service.putExtra("notifyUrl", videoInfo.getNotifyUrl());

    String videoId = videoInfo.getVideoId();
    if (videoId != null && !"".equals(videoId)) {
      service.putExtra("videoId", videoId);
    }

    cordova.getActivity().startService(service);
  }

  /**
   * 暂停上传
   */
  private void pauseUpload(JSONArray args) {
    try {
      JSONObject options = args.optJSONObject(0);
      Log.d(TAG, "pauseUpload() options: " + options.toString());
      String uploadId = options.optString("uploadId", "");
      int uploaderStatus = binder.getUploaderStatus();
      if (uploaderStatus == Uploader.UPLOAD && uploadId.equals(currentUploadId)) {
        binder.pause();

        JSONObject jsonObj = new JSONObject();
        try {
          jsonObj.put("status", 200);
          jsonObj.put("result", "pause success");
        } catch (JSONException e) {
          e.printStackTrace();
          callback.error(e.toString());
        }

        callback.success(jsonObj);
      }
    } catch (Exception e) {
      callback.error(e.toString());
    }
  }

  /**
   * 开始上传
   */
  private void resumeUpload(JSONArray args) {
    try {
      JSONObject options = args.optJSONObject(0);
      Log.d(TAG, "resumeUpload() options: " + options.toString());
      String uploadId = options.optString("uploadId", "");
      if (uploadId.equals(currentUploadId)) {
        binder.upload();
      }
    } catch (Exception e) {
      callback.error(e.toString());
    }
  }

  /**
   * 取消上传
   */
  private void cancelUpload(JSONArray args) {
    try {
      JSONObject options = args.optJSONObject(0);
      Log.d(TAG, "cancelUpload() options: " + options.toString());
      String uploadId = options.optString("uploadId", "");

      if (uploadId.equals(currentUploadId)) {
        //  TODO(CC SDK BUG: 上传过程中取消会失败，取消之前需要先暂停)
        binder.pause();
        //通知service取消上传
        if (!binder.isStop()) {
          binder.cancle();
        }

        //删除记录
        DataSet.removeUploadInfo(currentUploadId);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("status", 200);
        jsonObj.put("result", "cancel success");
        callback.success(jsonObj);
      }
    } catch (Exception e) {
      callback.error(e.toString());
    }
  }

  /**
   * 视频上传回调函数
   */
  private class UploadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      try {
        JSONObject jsonObj = new JSONObject();

        String uploadId = intent.getStringExtra("uploadId");
        if (uploadId != null) {
          currentUploadId = uploadId;
        }

        // 上传中
        int uploadStatus = intent.getIntExtra("status", ParamsUtil.INVALID);
        if (uploadStatus == Uploader.UPLOAD) {
          Log.d(TAG, "Uploader.UPLOAD");

          int progress = intent.getIntExtra("progress", 0);
          Log.d(TAG, "progress:" + progress);

          jsonObj.put("status", Uploader.UPLOAD);
          jsonObj.put("progress", progress);
          jsonObj.put("uploadId", currentUploadId);
          jsonObj.put("zdd", "my name is zdd");
          jsonObj.put("notifyUrl", intent.getStringExtra("notifyUrl")); // For debug only.

          PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
          progressResult.setKeepCallback(true);
          callback.sendPluginResult(progressResult);
        }
       // 已上传
       if (uploadStatus == Uploader.FINISH) {
          Log.d(TAG, "Uploader.FINISH");

          currentUploadId = null;
          String videoId = intent.getStringExtra("videoId");
          Log.d(TAG, "videoId: " + videoId);

          jsonObj.put("status", Uploader.FINISH);
          jsonObj.put("videoId", videoId);
          jsonObj.put("info", "success");
          callback.success(jsonObj);
        }

        // 若出现异常，提示用户处理
        int errorCode = intent.getIntExtra("errorCode", ParamsUtil.INVALID);
        if (errorCode == ErrorCode.NETWORK_ERROR.Value()) {
          jsonObj.put("status", "500");
          jsonObj.put("info", "网络异常，请检查");
          callback.error(jsonObj);
        } else if (errorCode == ErrorCode.PROCESS_FAIL.Value()) {
          jsonObj.put("status", "500");
          jsonObj.put("info", "上传失败，请重试");
          callback.error(jsonObj);
        } else if (errorCode == ErrorCode.INVALID_REQUEST.Value()) {
          jsonObj.put("status", "500");
          jsonObj.put("info", "上传失败，请检查账户信息");
          callback.error(jsonObj);
        }
      } catch (Exception e) {
        callback.error(e.toString());
      }
    }
  }

  /**
   * 动态添加权限
   */
  private static final int REQUEST_EXTERNAL_STORAGE = 1;

  private static String[] PERMISSIONS_STORAGE = {
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE};

  public static void verifyStoragePermissions(Activity activity) {
    // Check if we have write permission
    int permission = ActivityCompat.checkSelfPermission(activity,
      Manifest.permission.WRITE_EXTERNAL_STORAGE);

    if (permission != PackageManager.PERMISSION_GRANTED) {
      // We don't have permission so prompt the user
      ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
        REQUEST_EXTERNAL_STORAGE);
    }
  }

  @SuppressWarnings("deprecation")
  private File resolveLocalFileSystemURI(String url) throws IOException, JSONException {
    String decoded = URLDecoder.decode(url, "UTF-8");

    File fp = null;

    // Handle the special case where you get an Android content:// uri.
    if (decoded.startsWith("content:")) {
      fp = new File(getPath(this.cordova.getActivity().getApplicationContext(), Uri.parse(decoded)));
    } else {
      // Test to see if this is a valid URL first
      // @SuppressWarnings("unused")
      // URL testUrl = new URL(decoded);

      if (decoded.startsWith("file://")) {
        int questionMark = decoded.indexOf("?");
        if (questionMark < 0) {
          fp = new File(decoded.substring(7, decoded.length()));
        } else {
          fp = new File(decoded.substring(7, questionMark));
        }
      } else if (decoded.startsWith("file:/")) {
        fp = new File(decoded.substring(6, decoded.length()));
      } else {
        fp = new File(decoded);
      }
    }

    if (!fp.exists()) {
      throw new FileNotFoundException();
    }
        if (!fp.canRead()) {
            throw new IOException();
        }
    return fp;
  }

  /**
   * Get a file path from a Uri. This will get the the path for Storage Access
   * Framework Documents, as well as the _data field for the MediaStore and
   * other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri     The Uri to query.
   * @author paulburke
   */
  public static String getPath(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }

        // TODO handle non-primary volumes
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(
          Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(context, contentUri, null, null);
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{
          split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context       The context.
   * @param uri           The Uri to query.
   * @param selection     (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(Context context, Uri uri, String selection,
                                     String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
      column
    };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
        null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }
}
