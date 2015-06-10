package com.wildducktheories.tasklet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class SchedulerAPITest {

	@After
	public void tearDown() {
		SchedulerAPI.reset();
	}

	/**
	 * Checks that SchedulerAPI.getScheduler() will automatically
	 * start the synchronous thread.
	 * @throws InterruptedException
	 */
	@Test
	public void testDefaultSchedulerIsAutomatic() throws InterruptedException {

		final Scheduler scheduler = SchedulerAPI.getScheduler();
		final Thread current = Thread.currentThread();
		final Thread[] async = new Thread[]{null};
		scheduler.schedule(new Tasklet() {
			public Directive task() {
				async[0] = Thread.currentThread();
				synchronized(async) {
					async.notifyAll();
				}
				return Directive.DONE;
			}
		}, Directive.SYNC);

		synchronized(async) {
			while (async[0] == null) {
				async.wait();
			}
		}
		Assert.assertSame(current, async[0]);
		Assert.assertNotNull(async[0]);
	}

	@Test
	public void testAsyncSchedulerIsAsynchronous() {
		final Scheduler scheduler = SchedulerAPI.newScheduler();
		final Scheduler[] asyncScheduler = new Scheduler[]{null};
		final Thread current = Thread.currentThread();
		final Thread[] async = new Thread[]{null};
		final Tasklet tasklet = new Tasklet() {
			public Directive task() {
				async[0] = Thread.currentThread();
				asyncScheduler[0] = SchedulerAPI.getScheduler();
				return Directive.DONE;
			}
		};
		scheduler.schedule(new Tasklet() {
			public Directive task() {
				scheduler.schedule(tasklet, Directive.ASYNC);
				return Directive.DONE;
			}
		}, Directive.SYNC);
		scheduler.run();
		Assert.assertNotSame(current, async[0]);
		Assert.assertNotNull(async[0]);
		Assert.assertSame(scheduler, asyncScheduler[0]);
	}

	@Test
	public void testEmptySchedulerRunsToCompletion() {
		SchedulerAPI.newScheduler().run();
	}

	@Test
	public void testWithScheduler() {
		final Scheduler scheduler = SchedulerAPI.newScheduler();
		final Scheduler[] results = new Scheduler[]{null};
		SchedulerAPI.with(scheduler, new Tasklet() {
			@Override
			public Directive task() {
				results[0] = SchedulerAPI.getScheduler();
				return Directive.DONE;
			}
		});
		Assert.assertSame(scheduler, results[0]);
	}

	@Test
	public void testExample() {
		final boolean[] done = new boolean[] { false };

		SchedulerAPI
	           .newScheduler()
		       .schedule(
		        new Tasklet() {
		            int state = 0;
		            public Directive task() {
		                switch (state) {
		                case 0:
		     	            state=1;
		                    return Directive.ASYNC;
		                case 1:

		                    state=2;
		                    return Directive.SYNC;
		                default:
		                case 2:

		                    // merge the results of the blocking I/O call into state on the synchronous thread.

		                	done[0] = true;
		                    return Directive.DONE;
		                }
		            }
		        }, Directive.SYNC)
		        .run();
		Assert.assertTrue(done[0]);
	}
}
