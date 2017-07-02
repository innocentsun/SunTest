/*
 * 缩略图下载模块下载状态回调
 * 20130707 leofan
 */
package com.sun.imagecache;

public interface ImageCacheRequestListener
{
	void requestCompleted(RequestResult request);
	
	void requestCancelled(String url);

	void requestFailed(String url);
}
