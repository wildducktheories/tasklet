package com.wildducktheories.tasklet.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.wildducktheories.tasklet.API;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.Tasklet;

public class APIImpl implements API {

	/**
	 * An per thread override for the thread's current scheduler.
	 */
	private static ThreadLocal<Scheduler> perThread = new ThreadLocal<Scheduler>();

	private final ExecutorService executor;

	public APIImpl() {
		this(Executors.newCachedThreadPool());
	}

	public APIImpl(ExecutorService service) {
		this.executor = service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wildducktheories.tasklet.impl.API#getScheduler()
	 */
	@Override
	public final Scheduler getScheduler() {
		Scheduler scheduler = perThread.get();
		if (scheduler == null) {
			scheduler = new AsynchronousSchedulerImpl(this, executor)
					.setAuto(true);
			perThread.set(scheduler);
		}
		return scheduler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wildducktheories.tasklet.impl.API#newScheduler()
	 */
	@Override
	public Scheduler newScheduler() {
		return new AsynchronousSchedulerImpl(this, executor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wildducktheories.tasklet.impl.API#reset()
	 */
	@Override
	public void reset() {
		perThread.remove();
	}

	@Override
	public Scheduler with(final Scheduler scheduler, final Tasklet tasklet) {
		try {
			return SchedulerAPI.with(this, new Callable<Scheduler>() {

				@Override
				public Scheduler call() {
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

			}).call();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("unexpected exception", e);
		}

	}

}
