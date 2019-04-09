package org.apache.cordova.videoeditorbycc.upload;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bokecc.sdk.mobile.exception.DreamwinException;
import com.bokecc.sdk.mobile.upload.UploadListener;
import com.bokecc.sdk.mobile.upload.Uploader;
import com.bokecc.sdk.mobile.upload.VideoInfo;

import org.apache.cordova.videoeditorbycc.util.ConfigUtil;
import org.apache.cordova.videoeditorbycc.util.DataSet;
import org.apache.cordova.videoeditorbycc.util.ParamsUtil;

/**
 *
 * UploadService，用于支持后台上传功能
 *
 * @author CC视频
 *
 */
public class UploadService extends Service {

	private final int NOTIFY_ID = 10;

	private Context context;

	private int progress;

	private String progressText;

	private VideoInfo videoInfo;

	private Uploader uploader;

	private String uploadId;

	private boolean stop = true;

	private UploadBinder binder = new UploadBinder();

	public class UploadBinder extends Binder {

		public String getUploadId(){

			return uploadId;
		}

		public int getProgress() {
			return progress;
		}

		public String getProgressText(){
			return progressText;
		}

		public void upload() {
			if (uploader == null) {
				return;
			} else if (uploader.getStatus() == Uploader.WAIT) {
				uploader.start();
			} else if (uploader.getStatus() == Uploader.PAUSE) {
				uploader.resume();
			}
		}

		public void pause() {
			if (uploader == null) {
				return;
			}
			uploader.pause();
		}

		public void cancle() {
			if (uploader == null) {
				return;
			}

			uploader.cancel();
			stop = true;
		}

		public boolean isStop(){
			return stop;
		}

		public int getUploaderStatus(){
			if (uploader == null) {
				return Uploader.WAIT;
			}

			return uploader.getStatus();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		context = getApplicationContext();

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("service", "start service");

		if (intent == null) {
			Log.i("upload service", "intent is null");
			return Service.START_STICKY;
		}

		if (uploader != null) {
			Log.i("upload service", "uploader is working.");
			return Service.START_STICKY;
		}

		// 获取当前上传ID
		uploadId = intent.getStringExtra("uploadId");

		// 根据是否存在videoId，判断是否为首次上传
		String videoId = intent.getStringExtra("videoId");
		if (videoId == null) {// 首次上传
			videoInfo = new VideoInfo();
			videoInfo.setTitle(intent.getStringExtra("title"));
			videoInfo.setTags(intent.getStringExtra("tag"));
			videoInfo.setDescription(intent.getStringExtra("desc"));
			videoInfo.setFilePath(intent.getStringExtra("filePath"));
			videoInfo.setCategoryId(intent.getStringExtra("categoryId"));
			videoInfo.setNotifyUrl(intent.getStringExtra("notifyUrl"));
		} else {// 续传
			videoInfo = DataSet.getUploadInfo(uploadId).getVideoInfo();
		}

		if (videoInfo == null) {
			return Service.START_STICKY;
		}

		resetUploadService();

    try {
      ComponentName cn = new ComponentName(this, UploadService.class);
      ServiceInfo info = this.getPackageManager().getServiceInfo(cn, PackageManager.GET_META_DATA);
      String ccAccountInfo = info.metaData.getString("CC_ACCOUNT_INFO");
      Log.d("-------UploadService", "AccountInfo:" + ccAccountInfo);
      Log.d("-------UploadService", "API_KEY:" + ccAccountInfo.split(";")[0]);
      Log.d("-------UploadService", "USERID:" + ccAccountInfo.split(";")[1]);

      // 通知Upload receiver
      Intent broadCastIntent = new Intent(ConfigUtil.ACTION_UPLOAD);
      broadCastIntent.putExtra("uploadId", uploadId);
      broadCastIntent.putExtra("notifyUrl", videoInfo.getNotifyUrl()); // For debug only.
      DataSet.updateUploadInfo(new UploadInfo(uploadId, videoInfo, Uploader.WAIT, progress, progressText));
      sendBroadcast(broadCastIntent);
      stop = false;

      videoInfo.setUserId(ccAccountInfo.split(";")[1]);

      uploader = new Uploader(videoInfo, ccAccountInfo.split(";")[0]);
      uploader.setUploadListener(uploadListenner);
      uploader.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

		Log.i("init " + videoInfo.getTitle(), "uploadId: " + uploadId);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.i("Upload service", "task removed.");

		if (uploader != null) {
			uploader.cancel();
			resetUploadService();
		}

		super.onTaskRemoved(rootIntent);
	}

  private Intent uploadIntent = new Intent(ConfigUtil.ACTION_UPLOAD);

  UploadListener uploadListenner = new UploadListener() {

		@SuppressLint("NewApi")
		@SuppressWarnings("deprecation")
		@Override
		public void handleStatus(VideoInfo v, int status) {
			videoInfo = v;

      uploadIntent.putExtra("uploadId", uploadId);
      uploadIntent.putExtra("status", status);

			updateUploadInfoByStatus(status);

			switch (status) {
			case Uploader.PAUSE:
				Log.i("upload service", "pause.");
				break;
			case Uploader.UPLOAD:
				Log.i("upload service", "upload.");
				break;
			case Uploader.FINISH:
        Log.i("upload service", "finish.");
        // 停掉服务自身
        stopSelf();

        resetUploadService();
        // 通知上传队列更新
        uploadIntent.putExtra("videoId", videoInfo.getVideoId());
				break;
			//上传失败
			case Uploader.FAIL:
				Log.i("upload service", "fail.");
				break;
			}
      sendBroadcast(uploadIntent);
		}

		@Override
		public void handleProcess(long range, long size, String videoId) {
			if (stop) {
				return;
			}

			int p = (int) ((double) range / size * 100);
			if (progress <= 100) {
				if (p == progress) {
					return;
				}
				progressText = ParamsUtil.byteToM(range).
						concat("M / ").
						concat(ParamsUtil.byteToM(ParamsUtil.getLong(videoInfo.getFileByteSize()))).
						concat("M");
				progress = p;
//				if (progress % 10 == 0) {
//
//					progress = p;
//				}
        Log.i("UploadService", "progress:" + progress);
        Log.i("UploadService", "videoId:" + videoId);
//          Intent intent = new Intent(ConfigUtil.ACTION_UPLOAD);
        uploadIntent.putExtra("status", Uploader.UPLOAD);
        uploadIntent.putExtra("progress", progress);
        sendBroadcast(uploadIntent);
			}
		}

		@Override
		public void handleException(DreamwinException exception, int status) {
			updateUploadInfoByStatus(status);
			// 停掉服务自身
			stopSelf();

			Intent intent = new Intent(ConfigUtil.ACTION_UPLOAD);
			intent.putExtra("errorCode", exception.getErrorCode().Value());
			sendBroadcast(intent);

			Log.e("上传失败", exception.getMessage());
		}

		@Override
		public void handleCancel(String videoId) {
			stopSelf();
			resetUploadService();
		}

		@Override
		public void onVideoInfoUpdate(VideoInfo videoInfo) {
      Log.i("UploadService", "onVideoInfoUpdate() videoInfo:" + videoInfo);
		}
	};

	private void resetUploadService(){
		progress = 0;
		progressText = null;
		uploader = null;
		stop = true;
	}

	private void updateUploadInfoByStatus(int status){
		UploadInfo uploadInfo = DataSet.getUploadInfo(uploadId);
		if (uploadInfo == null) {
			return;
		}

		uploadInfo.setStatus(status);
		uploadInfo.setVideoInfo(videoInfo);

		if (progress > 0) {
			uploadInfo.setProgress(progress);
		}

		if (progressText != null) {
			uploadInfo.setProgressText(progressText);
		}

		DataSet.updateUploadInfo(uploadInfo);
	}
}
