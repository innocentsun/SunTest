package com.sun.imagecache;

/**
 * 图片列表下载进度回调接口
 *
 * Created by ashercai on 1/2/15.
 */
public interface ThumbnailListDownloadListener {
	int CODE_OK				= 0;	//进度发生了变化
	int CODE_INVALID_INPUT	= -1;	//调用下载列表的输入参数有错，例如urls为空，或者type不对
	int CODE_CANCELLED		= -2;	//下载取消
	int CODE_FAIL			= -3;	//下载失败

	/**
	 * 下载状态发生变化时回调此方法
	 *
	 * @param errCode 参考上面的 CODE_* 常量
	 * @param percent 值范围0～100
	 */
	void onProgress(int errCode, int percent);
}
