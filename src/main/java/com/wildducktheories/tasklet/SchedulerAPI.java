package com.wildducktheories.tasklet;

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
	
	public static Scheduler with(API api, Scheduler scheduler, Tasklet tasklet) {
		final API saved = perThread.get();
		try {
			perThread.set(api);
			return api.with(scheduler, tasklet);
		} finally {
			if (saved == null) {
				perThread.remove();
			} else {
				perThread.set(saved);
			}
		}
	}
	
	public static void reset() {
		get().reset();
		perThread.remove();
	}
}
