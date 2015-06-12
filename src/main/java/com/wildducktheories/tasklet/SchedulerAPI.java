package com.wildducktheories.tasklet;

import java.util.concurrent.Callable;

import com.wildducktheories.tasklet.impl.APIImpl;


/**
 * Provides methods that can construct, get and set the {@link Scheduler} instance associated with the current {@link Thread}.
 *
 * @author jonseymour
 */
public class SchedulerAPI {
	/**
	 * An per thread override for the thread's current scheduler.
	 */
	private final static ThreadLocal<API> perThread = new ThreadLocal<API>();

	public static API get() {
		API api = perThread.get();
		if (api == null) {
			api = new APIImpl();
		}
		return api;
	}
	
	/**
	 * Run the specified {@link Runnable} with the specified {@link API} as the 
	 * current {@link API} for the current thread.
	 * @param api The specified {@link API}I
	 * @param run The specified {@link Runnable}
	 */
	public static void with(final API api, final Runnable runnable) {
		final API saved = perThread.get();
		try {
			perThread.set(api);
			runnable.run();
		} finally {
			if (saved == null) {
				perThread.remove();
			} else {
				perThread.set(saved);
			}
		}
	}
	
	/**
	 * Run the specified {@link Callable} with the specified {@link API} as the 
	 * current {@link API} for the current thread. 
	 * @param api The specified {@link API}I
	 * @param run The specified {@link Runnable}
	 * @return The result of the {@link Callable}
	 * @throws Exception The exception thrown by the specified {@link Callable}, if any.
	 */
	public static <P> P with(final API api, final Callable<P> callable) 
		throws Exception
	{
		final API saved = perThread.get();
		try {
			perThread.set(api);
			return callable.call();
		} finally {
			if (saved == null) {
				perThread.remove();
			} else {
				perThread.set(saved);
			}
		}
	}
	
	/**
	 * Release all thread local resources.
	 */
	public static void reset() {
		get().reset();
		perThread.remove();
	}
}
