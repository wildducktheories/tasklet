package com.wildducktheories.tasklet;

public interface API {

	/**
	 * @return Answer the current scheduler associated with the thread. Always non-null.
	 * <p>
	 * <strong>Note:</strong> if there is not currently an invocation of {@link SchedulerAPI#with(Scheduler, Tasklet)} on
	 * the current thread's call stack, then the caller (or some other component) should arrange for {@link SchedulerAPI#reset()} to be
	 * called at some later time in order to prevent the {@link SchedulerAPI}'s {@link ClassLoader} being pinned by a {@link ThreadLocal}.
	 * @see {@link SchedulerAPI#reset()}
	 */
	abstract Scheduler getScheduler();

	/**
	 * @return Answers a new {@link Scheduler}.
	 */
	abstract Scheduler newScheduler();

	/**
	 * Used to cleanup the {@link SchedulerAPI} {@link ThreadLocal} associated with the current thread. This call is required
	 * if, and only if, {@link SchedulerAPI#getScheduler()} may have been called on a thread when a call
	 * to {@link SchedulerAPI#with(Scheduler, Tasklet)} was not already active on the same thread and no other
	 * arrangement has been made to call this method on this {@link Thread}.
	 */
	abstract void reset();

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
	Scheduler with(Scheduler scheduler, Tasklet tasklet);
}
