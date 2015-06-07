package com.wildducktheories.tasklet.lib;

import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.Tasklet;

public final class TaskletLib {
	/**
	 * A {@link Tasklet} that immediately completes itself.
	 */
	public static final Tasklet DONE = new DoneTasklet();

	/**
	 * This {@link Tasklet} can be enqueued with a {@link Scheduler} to prevent that scheduler's main thread from exiting until such
	 * time as the {@link Tasklet} is requeued with the Scheduler with a DONE directive.
	 */
	public static final Tasklet WAIT = new WaitTasklet();

	private TaskletLib() {}
}
