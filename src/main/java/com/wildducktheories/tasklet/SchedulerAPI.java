package com.wildducktheories.tasklet;

import com.wildducktheories.tasklet.impl.AsynchronousSchedulerImpl;

/**
 * Provides methods that can construct, get and set the {@link Scheduler} instance associated with the current {@link Thread}.
 *
 * @author jonseymour
 */
public class SchedulerAPI {
	/**
	 * An per thread override for the thread's current scheduler.
	 */
	private final static ThreadLocal<Scheduler> perThread = new ThreadLocal<Scheduler>();

	/**
	 * @return Answers a new {@link Scheduler}.
	 */
	public static final Scheduler newScheduler() {
		return new AsynchronousSchedulerImpl();
	}

	/**
	 * @return Answer the current scheduler associated with the thread. Always non-null.
	 * <p>
	 * <strong>Note:</strong> if there is not currently an invocation of {@link SchedulerAPI#with(Scheduler, Tasklet)} on
	 * the current thread's call stack, then the caller (or some other component) should arrange for {@link SchedulerAPI#reset()} to be
	 * called at some later time in order to prevent the {@link SchedulerAPI}'s {@link ClassLoader} being pinned by a {@link ThreadLocal}.
	 * @see {@link SchedulerAPI#reset()}
	 */
	public static final Scheduler getScheduler()
	{
		Scheduler scheduler = perThread.get();
		if (scheduler == null) {
			scheduler = new AsynchronousSchedulerImpl().setAuto(true);
			perThread.set(scheduler);
		}
		return scheduler;
	}

	/**
	 * Set the {@link Scheduler} for the current {@link Thread} to the specified
	 * {@link Scheduler}; execute the {@link Tasklet#task()} method of the specified {@link Tasklet}; schedule the
	 * {@link Tasklet} with the specified {@link Scheduler} using the {@link Directive} returned by the call to {@link Tasklet#task()}; then
	 * restore the original {@link Scheduler}, if any, as the current
	 * {@link Scheduler} for the current {@link Thread}.
	 * @param scheduler The new {@link Scheduler} for the current thread.
	 * @param tasklet The tasklet to be executed in the current thread.
	 * @return The specified Scheduler.
	 */
	public static Scheduler with(Scheduler scheduler, Tasklet tasklet) {
		final Scheduler saved = perThread.get();
		try {
			perThread.set(scheduler);
			scheduler.schedule(tasklet, tasklet.task());
			return scheduler;
		} finally {
			if (saved != null) {
				perThread.set(saved);
			} else {
				perThread.remove();
			}
		}

	}

	/**
	 * Used to cleanup the {@link SchedulerAPI} {@link ThreadLocal} associated with the current thread. This call is required
	 * if, and only if, {@link SchedulerAPI#getScheduler()} may have been called on a thread when a call
	 * to {@link SchedulerAPI#with(Scheduler, Tasklet)} was not already active on the same thread and no other
	 * arrangement has been made to call this method on this {@link Thread}.
	 */
	public static void reset() {
		perThread.remove();
	}


}
