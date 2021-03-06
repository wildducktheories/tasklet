package com.wildducktheories.tasklet;

import java.util.concurrent.Callable;

import com.wildducktheories.api.APIManager;
import com.wildducktheories.api.impl.AbstractAPIManagerImpl;
import com.wildducktheories.tasklet.impl.APIImpl;


/**
 * Provides methods that can construct, get and set the {@link API} instance associated with the current {@link Thread}.
 *
 * @author jonseymour
 */
public class SchedulerAPI {

	/**
	 * Delegate used for all static methods.
	 */
	private static final APIManager<API> manager = new AbstractAPIManagerImpl<API>() {
		@Override
		public API create() {
			return new APIImpl();
		}
	};
	
	/**
	 * Answer a new instance of the scheduler API.
	 * @return A new instance of the Scheduler API.
	 */
	public static API create() {
		return manager.create();
	}

	/**
	 * Answer the current instance of the scheduler API. If there is no such instance, create a new
	 * instance and initialise a ThreadLocal with a reference to this instance. The application
	 * must ensure that <code>reset()</code> is called before losing a reference to the current Thread.
	 * @return Answer an API instance. Never null.
	 */
	public static API get() {
		return manager.get();
	}
	
	/**
	 * Encapsulate the specified {@link Runnable} with one that will configure the
	 * thread's current API to be the specified API, execute the specified {@link Runnable},
	 * and then restore the thread's original API instance.
	 * @param api The specified {@link API}
	 * @param runnable The specified {@link Runnable}
	 * @return The encapsulated {@link Runnable}
	 */
	public static Runnable with(final API api, final Runnable runnable) {
		return manager.with(api, runnable);
	}
	
	/**
	 * Encapsulate the specified {@link Callable} with one that will configure the
	 * thread's current API to be the specified API, execute the specified {@link Callable},
	 * and then restore the thread's original API instance.
	 * @param api The specified {@link API}I
	 * @param callable The specified {@link Runnable}
	 * @return The encapsulated {@link Callable}
	 */
	public static <P> Callable<P> with(final API api, final Callable<P> callable) 
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
