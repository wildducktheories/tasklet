package com.wildducktheories.tasklet;




/**
 * A co-operative Scheduler of {@link Tasklet} instances.
 * <p>
 * Tasklets are objects that implement the {@link Tasklet#task()} method. When a {@link Tasklet} instance's current task() invocation finishes, it returns a {@link Directive}
 * to the {@link Scheduler} that indicates how the {@link Tasklet} should be scheduled in the future, if at all.
 * <p>
 * Synchronous {@link Tasklet} instances execute on the scheduler's synchronous thread; asynchronous tasklets run on some other thread.
 * <p>
 * The core idea is to simplify concurrent programming by structuring programs as a collection of {@link Tasklet} instances
 * some of which execute some of the time co-operatively and synchronously with respect to each other and a shared data structure
 * and some of which execute asynchronously, usually for the purposes of executing blocking-IO calls.
 * <p>
 * For example, if a {@link Tasklet} executing in a {@link Scheduler}'s synchronous {@link Thread} needs to make a blocking call, it can
 * transition itself into a new state and return to the {@link Scheduler} with a {@link Directive} value of {@link Directive#ASYNC} to request
 * that the {@link Scheduler} reschedules it (the {@link Tasklet}) onto an asynchronous {@link Thread}. This frees up the main
 * {@link Scheduler} {@link Thread} for other non-blocking {@link Tasklet} instances that are ready to run. After the blocking call completes,
 * the {@link Tasklet} can return with a {@link Directive} value of {@link Directive#SYNC} to indicate that the {@link Tasklet} is ready to execute
 * on the main thread again. Once execution resumes on the synchronous thread, the {@link Tasklet} may update state that
 * is shared with other tasklets with a guarantee that no other Tasklet that respects the protocol will
 * be modifying that state at the same time.
 * @author jonseymour
 */
public interface Scheduler
	extends Runnable
{
	/**
	 * Enqueues a {@link Tasklet} with the specified {@link Directive}.
	 *
	 * @param t The {@link Tasklet} to be scheduled with the scheduler.
	 * @param directive The scheduling directive.
	 * <p>
	 * If directive is SYNC, then the {@link Tasklet} will be executed in the scheduler's main thread.
	 * <p>
	 * If directive is ASYNC, then the {@link Tasklet} will be executed in some other thread other than
	 * the scheduler's main thread.
	 * <p>
	 * If directive is WAIT, then the scheduler may retain a reference to the {@link Tasklet} but doesn't otherwise execute it.
	 * <p>
	 * Callers use the WAIT directive to prevent the scheduler's main thread terminating before the {@link Tasklet}
	 * is requeued by some asynchronous, external thread. Such callers must guarantee to eventually re-queue the {@link Tasklet} with some
	 * directive..
	 * <p>
	 * If directive is DONE, then the scheduler removes a previous reference to the {@link Tasklet}, allowing
	 * the {@link Scheduler} to stop if it has run out of {@link Tasklet} instances to schedule.
	 */
	Scheduler schedule(Tasklet t, Directive directive);

	/**
	 * Suspend the execution of the specified {@link Tasklet} in exchange for a {@link Rescheduler}
	 * which may be used to resume execution of the {@link Tasklet} at some later time.
	 * <p>
	 * This call is equivalent to <code>scheduler(t, Directive.WAIT)</code>
	 *
	 * @param t The tasklet to be requeued at a later time.
	 * @return A {@link Rescheduler} that MUST be used to requeue the specified
	 * {@link Tasklet} with the receiver at some later time. Failure to invoke
	 * the requeuer may cause a resource leak.
	 */
	public Rescheduler suspend(Tasklet t);

	/**
	 * Blocks until all suspended {@link Tasklet} instances have been resumed and all synchronous and asynchronous {@link Tasklet} instances
	 * have finished executing. If the scheduler doesn't currently have a synchronous thread, then
	 * the current thread becomes the scheduler's synchronous thread, otherwise this call blocks until
	 * the synchronous thread terminates.
	 */
	void run();

}
