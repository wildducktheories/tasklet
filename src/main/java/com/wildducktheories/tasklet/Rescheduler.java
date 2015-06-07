package com.wildducktheories.tasklet;

/**
 * Implementations of this interface may be used to reschedule a {@link Tasklet}
 * with a {@link Scheduler} at some later time. The {@link Directive} to be
 * used is determined by the caller of {@link Rescheduler#resume(Directive)} at the point of resumption
 * or ahead of time by calling {@link Rescheduler#resumeLater(Directive)}.
 *
 * @author jonseymour
 */
public interface Rescheduler {
	/**
	 * Resume a previously suspended {@link Tasklet}.
	 * @param directive The {@link Directive} to be used when the associated {@link Tasklet} is rescheduled
	 * with the associated {@link Scheduler}.
	 */
	public void resume(Directive directive);

	/**
	 * Bind a {@link Directive} argument to a resume call that can be deferred until a later time.
	 * @param directive The {@link Directive} to be used to resume the {@link Tasklet} when the {@link Runnable}
	 * returned by this method is eventually called.
	 * @return A {@link Runnable} that may be run later to schedule execution of the associated {@link Tasklet}
	 * with the specified {@link Directive} and the associated {@link Scheduler}.
	 */
	public Runnable resumeLater(Directive directive);
}
