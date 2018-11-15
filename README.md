# cordova-plugin-video-editorbycc
本插件是基于CC视频Android SDK实现。开发人员可基于此插件实现对接CC视频云服务平台，进行视频压缩、视频上传、暂停上传、继续上传、取消上传、断点续传操作。

### 安装
```
cordova plugin add https://github.com/CrispyFriedPig/cordova-plugin-video-editorbycc.git --variable CC_ACCOUNT_INFO="API_KEY;USERID"
```
安装时需要根据应用的运行环境设置CC帐户信息USERID（帐户ID）和API KEY； 如果未设置variable，则默认为SIT环境的账户信息。

### 视频压缩
用法如下：
```
let options = {
	fileUri: "视频的Uri路径",
	outputFileName: "压缩后视频文件的名字",
	saveToLibrary: false,
	deleteInputFile: false,
	width: "输出视频的宽",			// 只有当quality: 4时有效
	height: "输出视频的高",			// 只有当quality: 4时有效
	bitrate: "输出视频的比特率",	// 只有当quality: 4时有效
	quality: 3  // COMPRESS_QUALITY_HIGH: 1; COMPRESS_QUALITY_MEDIUM: 2; COMPRESS_QUALITY_LOW: 3; COMPRESS_QUALITY_CUSTOMIZE: 4
};

cordova.plugins.VideoEditorByCC.compressVideo((data) => {
	console.log("compressVideo success.");

	if (data.status == 200) {
		// 更新压缩进度条
		let progress = Number(data.progress).toFixed(0);
	} else if (data.status == 400) {
		// 返回压缩后视频路径
		let outputVideoPath = data.result;
	} else {
		console.log(data);
	}
}, (error) => {
	console.log("compressVideo failed");
}, options);
```
**注：quality默认为3（低质量压缩），如果想要自定义压缩参数，需要将quality设置为4，同时需要定义width, height, bitrate三个参数**


### 视频上传
用法如下：
```
let options = {
  filePath: "视频路径(必选)",
  title: "视频标题(必选)",
  tag: "视频标签(可选)",
  desc: "视频描述(可选)",
  categoryId: "分类Id(可选)"
};

cordova.plugins.VideoEditorByCC.uploadVideo((data) => {
  console.log("uploadVideo success.");

  if (data) {
    // 保存上传Id(暂停、继续、取消操作用)
    this.currentUploadId = data.uploadId;

	// status=100(等待上传)、200(正在上传)、300(暂停上传)、400(上传完成)、500(上传失败)
	if (data.status == 200) {
	  var progress = Number(data.progress).toFixed(0) + '%';
	  console.log("上传进度:" + progress);
	} else if (data.status == 400) {
	  console.log("视频ID:" + data.videoId)
	}
  }
}, (error) => {
  console.log("uploadVideo failed.");
  
  if (error.status == 500) {
	console.log("error:" + error.info);
  }
}, options);
```
**注: 此插件的上传操作是单独启动一个后台service执行，所以当应用正常退出后（比如用户点击Back键退出），上传操作并不会中断。**

### 暂停上传
用法如下：
```
cordova.plugins.VideoEditorByCC.pauseUpload((data) => {
  if (data && data.status == 200) {
	console.log("pauseUpload success.");
  }
}, (error) => {
  console.log("pauseUpload failed.");
}, {uploadId: this.currentUploadId});
```

### 恢复暂停、继续上传
用法如下：
```
cordova.plugins.VideoEditorByCC.resumeUpload((data) => {
  console.log("resumeUpload success.");

  if (data) {
    // 保存上传Id(暂停、继续、取消操作用)
    this.currentUploadId = data.uploadId;

	// status=100(等待上传)、200(正在上传)、300(暂停上传)、400(上传完成)、500(上传失败)
	if (data.status == 200) {
	  var progress = Number(data.progress).toFixed(0) + '%';
	  console.log("上传进度:" + progress);
	} else if (data.status == 400) {
	  console.log("视频ID:" + data.videoId)
	}
  }
}, (error) => {
  console.log("resumeUpload failed.");
  if (error.status == 500) {
	console.log("error:" + error.info);
  }
}, {uploadId: this.currentUploadId});
```

### 取消上传
用法如下：
```
cordova.plugins.VideoEditorByCC.cancelUpload((data) => {
  if (data && data.status == 200) {
	console.log("cancelUpload success.");
  }
}, (error) => {
  console.log("cancelUpload failed.");
}, {uploadId: this.currentUploadId});
```

### 断点续传
用法如下：
```
    cordova.plugins.VideoEditorByCC.reUploadVideo((data) => {
      console.log("reUploadVideo success.");

      if (data) {
	    // 保存上传Id(暂停、继续、取消操作用)
        this.currentUploadId = data.uploadId;

		// status=100(等待上传)、200(正在上传)、300(暂停上传)、400(上传完成)、500(上传失败)
		if (data.status == 200) {
		  var progress = Number(data.progress).toFixed(0) + '%';
		  console.log("上传进度:" + progress);
		} else if (data.status == 400) {
		  console.log("视频ID:" + data.videoId)
		}
      }
    }, (error) => {
      console.log("reUploadVideo failed.");
	  if (error.status == 500) {
		console.log("error:" + error.info);
	  }
    }, {});
```

**注：原生端在应用正常退出时存储视频上传状态，由于正常退出并不会中断上传操作，故只有当应用正常退出后由于某些原因导致后台上传Service中断（比如手动杀进程，或者由于内存消耗过大，系统自动杀掉上传服务），这时断点续传方法才会生效。**
