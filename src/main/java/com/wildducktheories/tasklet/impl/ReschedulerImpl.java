package com.wildducktheories.tasklet.impl;

import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Rescheduler;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.Tasklet;

/**
 * An implementation of the {@link Rescheduler} interface.
 * @author jonseymour
 */
public final class ReschedulerImpl implements Rescheduler {
	private final Scheduler scheduler;
	private final Tasklet tasklet;

	public ReschedulerImpl(Scheduler scheduler, Tasklet tasklet) {
		this.scheduler = scheduler;
		this.tasklet = tasklet;
	}

	@Override
	public Runnable resumeLater(final Directive directive) {
		return new Runnable() {
			public void run() {
				resume(directive);
			}
		};
	}

	@Override
	public void resume(Directive directive) {
		scheduler.schedule(tasklet, directive);
	}
}