package com.wildducktheories.tasklet;

/**
 * A {@link Directive} value indicates to a {@link Scheduler} how to next schedule a {@link Tasklet}.
 */
public enum Directive {
	/**
	 * Instructs a {@link Scheduler} to run a {@link Tasklet} synchronously with respect to the
	 * scheduler's main {@link Thread}.
	 */
	SYNC,
	/**
	 * Instructs a {@link Scheduler} to run a {@link Tasklet} asynchronously with respect to the scheduler's
	 * synchrononous thread.
	 */
	ASYNC,
	/**
	 * Instructs a {@link Scheduler} that a {@link Tasklet} is waiting for an
	 * event to occur. This {@link Directive} tells the {@link Scheduler} that it should assume
	 * that something is expected to happen to the {@link Tasklet} in future.
	 * The user of this {@link Directive} must guarantee to reschedule the {@link Tasklet}
	 * with the same {@link Scheduler} at some later point in time.
	 */
	WAIT,
	/**
	 * Instructs a {@link Scheduler} that the execution of a {@link Tasklet} is complete. A {@link Scheduler}
	 * receiving a {@link Tasklet} with this directive should remove all references to the related {@link Tasklet}.
	 */
	DONE
}
