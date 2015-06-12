package com.wildducktheories.tasklet.impl;

import com.wildducktheories.tasklet.API;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.Tasklet;

public class APIImpl implements API {

	/**
	 * An per thread override for the thread's current scheduler.
	 */
	private static ThreadLocal<Scheduler> perThread = new ThreadLocal<Scheduler>();

	/* (non-Javadoc)
	 * @see com.wildducktheories.tasklet.impl.API#getScheduler()
	 */
	@Override
	public final Scheduler getScheduler()
	{
		Scheduler scheduler = perThread.get();
		if (scheduler == null) {
			scheduler = new AsynchronousSchedulerImpl(this).setAuto(true);
			perThread.set(scheduler);
		}
		return scheduler;
	}

	/* (non-Javadoc)
	 * @see com.wildducktheories.tasklet.impl.API#newScheduler()
	 */
	@Override
	public Scheduler newScheduler() {
		return new AsynchronousSchedulerImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.wildducktheories.tasklet.impl.API#reset()
	 */
	@Override
	public void reset() {
		perThread.remove();
	}

	@Override
	public Scheduler with(Scheduler scheduler, Tasklet tasklet) {
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

}
