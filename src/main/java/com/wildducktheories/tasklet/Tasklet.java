package com.wildducktheories.tasklet;


/**
 * A Tasklet is a unit of work which can be executed either synchronously
 * or asynchronously with respect to the main thread of the current {@link Scheduler}.
 * <p>
 * If the Tasklet is scheduled synchronously, then it is queued for execution
 * by the main scheduler thread. If it is scheduled asynchronously, then it is
 * queued for execution on some other thread. Tasklets that run synchronously
 * to the scheduler thread should not block but may modify state that is shared with other
 * synchronously scheduled {@link Tasklet} instances (since, by definition, at most one of these
 * can execute concurrently). Tasklets that run asynchronously with the main thread may block, but may not
 * modify state that is shared with synchronous {@link Tasklet} instances.
 * <p>
 * @author jonseymour
 */
public interface Tasklet {

	/**
	 * Execute a phase of task execution, then indicate to the {@link Scheduler}
	 * how it should schedule the {@link Tasklet} next.
	 * <p>
	 * Implementors should take care not to block indefinitely while running on the current {@link Scheduler}'s
	 * main thread and to not mutate shared data structures otherwise.
	 * <p>
	 * @return A {@link Directive} to the {@link Scheduler} to indicate how the {@link Scheduler}
	 * should schedule the receiver next.
	 */
	Directive task();
}
