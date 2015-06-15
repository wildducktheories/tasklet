package com.wildducktheories.tasklet;


/**
 * This API manages the association between the current Thread and an instance of the {@link Scheduler} interface.
 * <p>
 * A new {@link Scheduler} instance is created with <code>newScheduler()</code>. 
 * <p>
 * An existing {@link Scheduler} instance is associated with the current thread for 
 * the duration of an execution of a Tasklet by calling {@link #with(Scheduler, Tasklet)}.
 * <p> 
 * The current instance associated by a Thread is returned by {@link #getScheduler()}. If there is no such
 * instance then a new one will be created with {@link #newScheduler()} and stored in a {@link ThreadLocal} 
 * for use by subsequent calls to {@link #getScheduler()}.
 * <p>
 * If an application cannot guarantee that all calls to {@link #getScheduler()}
 * occur while a call to {@link #with(Scheduler, Tasklet)} is active on the current {@link Thread}, 
 * then it should arrange to call {@link #reset()} before it loses a reference to the current {@link Thread}.
 * @author jonseymour
 */
public interface API extends com.wildducktheories.api.API {

	/**
	 * Provide a reference to the Thread's current {@link Scheduler} instance or create a new such 
	 * instance if one does not already exist.
	 * <p>
	 * @return Answer the current scheduler associated with the thread. Always non-null.
	 * <p>
	 * <strong>Note:</strong> if there is not currently an invocation of {@link API#with(Scheduler, Tasklet)} on
	 * the current thread's call stack, then the caller (or some other component) should arrange for {@link SchedulerAPI#reset()} to be
	 * called at some later time in order to prevent the {@link SchedulerAPI}'s {@link ClassLoader} being pinned by a {@link ThreadLocal}.
	 * @see SchedulerAPI#reset()
	 */
	Scheduler getScheduler();

	/**
	 * @return Answers a new {@link Scheduler}.
	 */
	Scheduler newScheduler();

	/**
	 * Used to cleanup the {@link SchedulerAPI} {@link ThreadLocal} associated with the current thread. This call is required
	 * if, and only if, {@link API#getScheduler()} may have been called on a thread when a call
	 * to {@link API#with(Scheduler, Tasklet)} was not already active on the same thread and no other
	 * arrangement has been made to call this method on this {@link Thread}.
	 */
	void reset();

	/**
	 * Set the {@link Scheduler} for the current {@link Thread} to the specified
	 * {@link Scheduler}; execute the {@link Tasklet#task()} method of the specified {@link Tasklet}; schedule the
	 * {@link Tasklet} with the specified {@link Scheduler} using the {@link Directive} returned by the call to {@link Tasklet#task()}; then
	 * restore the original {@link Scheduler}, if any, as the current
	 * {@link Scheduler} for the current {@link Thread}.
	 * <p>
	 * A call to this method will be active on each asynchronous thread started to service a tasklet that
	 * is scheduled with {@link Directive#ASYNC}.
	 * @param scheduler The new {@link Scheduler} for the current thread.
	 * @param tasklet The tasklet to be executed in the current thread.
	 * @return The specified Scheduler.
	 */
	Scheduler with(Scheduler scheduler, Tasklet tasklet);
}
