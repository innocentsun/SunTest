package com.sun.base;

/**
 * 提供一个单例类，全局管理内存oom的通告，接收内存警告的通知，
 * 然后将所有关注内存警告的对象丢到统一后台线程去执行内存释放动作
 * @author junzhang
 *
 */

public class MemoryWarningManager 
{
	private static MemoryWarningManager _instance;
	private ListenerManager<IMemoryWarningListener> mListenerMgr = new ListenerManager<IMemoryWarningListener>();

	private MemoryWarningManager() {
	}
	
	public synchronized static MemoryWarningManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new MemoryWarningManager();
		}
		return _instance;
	}
	
	/**
	 * 当捕获到警告的时候调用这个函数
	 */
	public void SystemOutOfMemory()
	{
		//切换到后台低优先级线程，执行通知动作
		ThreadManager.getInstance().execute(new Runnable()
		{
			@Override
			public void run ()
			{
				mListenerMgr.startNotify(new ListenerManager.INotifyCallback<IMemoryWarningListener>() {
					@Override
					public void onNotify(IMemoryWarningListener listener) {
						listener.onMemoryWarning();
					}
				});
			}
		});
	}
	
	/**
	 * 注册监听
	 * 
	 * @param listener
	 */
	public void register(IMemoryWarningListener listener) {
		mListenerMgr.register(listener);
	}
	
	/**
	 * 取消注册监听
	 * 
	 * @param listener
	 */
	public void unregister(IMemoryWarningListener listener) {
		mListenerMgr.unregister(listener);
	}
	
	
	/**
	 * 内存警告回调，这个会在后台线程回调，所以，所有的实现者需要控制好线程安全
	 * @author junzhang
	 *
	 */
	public interface IMemoryWarningListener
	{
		/**
		 * 收到内存警告通知
		 */
		void onMemoryWarning();
		
	}
}
