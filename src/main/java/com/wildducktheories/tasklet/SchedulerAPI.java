package com.wildducktheories.tasklet;

import java.util.concurrent.Callable;

import com.wildducktheories.api.APIManager;
import com.wildducktheories.api.impl.AbstractAPIManagerImpl;
import com.wildducktheories.tasklet.impl.APIImpl;


/**
 * Provides methods that can construct, get and set the {@link Scheduler} instance associated with the current {@link Thread}.
 *
 * @author jonseymour
 */
public class SchedulerAPI {

	/**
	 * Delegate used for all static methods.
	 */
	private static final APIManager<API> manager = new AbstractAPIManagerImpl<API>() {
		@Override
		public API newAPI() {
			return new APIImpl();
		}
	};
	
	/**
	 * @return A new instance of the Scheduler API.
	 */
	public static API newAPI() {
		return manager.newAPI();
	}

	public static API get() {
		return manager.get();
	}
	
	/**
	 * Run the specified {@link Runnable} with the specified {@link API} as the 
	 * current {@link API} for the current thread.
	 * @param api The specified {@link API}I
	 * @param run The specified {@link Runnable}
	 */
	public static void with(final API api, final Runnable runnable) {
		manager.with(api, runnable);
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
		return manager.with(api, callable);
	}
	
	/**
	 * Release all thread local resources.
	 */
	public static void reset() {
		manager.reset();
	}
}
