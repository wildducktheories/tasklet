package com.wildducktheories.tasklet.impl;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.wildducktheories.tasklet.API;
import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Rescheduler;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.SchedulerNotRunningException;
import com.wildducktheories.tasklet.Tasklet;

/**
 * Provides an implementation of the {@link Scheduler} interface that uses an
 * {@link ExecutorService} to execute the asynchronous phases of {@link Tasklet} instances.
 * <p>
 * An implementation assumption is that most tasklets are scheduled for synchronous execution by the
 * synchronous thread itself with the occasional need to schedule asynchronous tasklets that
 * then reschedule themselves as synchronous tasklets.
 * <p>
 * We don't require that a scheduler runs until we absolutely need it - that is, when we first
 * attempt to schedule an asynchronous {@link Tasklet}.
 * </p>
 * @author jonseymour
 */
public class AsynchronousSchedulerImpl implements Scheduler {

	/**
	 * The directives.
	 */
	private final Map<Tasklet, Directive> directives = new IdentityHashMap<Tasklet, Directive>();

	private final API api;
	
	/**
	 * The service used to execute tasks remotely.
	 */
	private final ExecutorService executor;

	/**
	 * The list of {@link Tasklet} instances to execute synchronously.
	 */
	private Set<Tasklet> sync = new LinkedHashSet<Tasklet>();

	/**
	 * The scheduler {@link Thread}.
	 */
	private Thread main;

	/**
	 * This value is 2*r+auto, where r is the number of active calls
	 * to run on the synchronous thread and auto is 1 if setAuto(boolean)
	 * was last called with true.
     *
     * 0  => run not active, run() must be called for SYNC tasklet execution
     * 1  => run not active, but SYNC tasklets will be executed by calls to schedule
     * 2+ => run active, SYNC tasklets executed by schedule calls and scheduler loop
	 */
	private int runLevel;

	/**
	 * Default constructor uses a cached thread pool.
	 */
	public AsynchronousSchedulerImpl(API api) {
		this(api,Executors.newCachedThreadPool());
	}

	/**
	 * @param service An executor service.
	 */
	public AsynchronousSchedulerImpl(API api, ExecutorService service) {
		this.api = api;
		executor = service;
	}

	/**
	 * Sets a variable which determines if a synchronous thread will be automatically started by the scheduler when
	 * no such thread already exists and there are synchronously scheduled {@link Tasklet} instances pending completion.
	 * <p>
	 * @param auto True if a synchronous thread is automatically started after the first {@link Tasklet} is scheduled
	 * with a directive of {@link Directive#SYNC}. If this method is not called or if it is called with a value of false, the
	 * consumer of this {@link Scheduler} must arrange for {@link Scheduler#run()} to be called at some later time in order
	 * to guarantee that all synchronously scheduled {@link Tasklet} instances have been executed.
	 * @return The receiver.
	 */
	public synchronized AsynchronousSchedulerImpl setAuto(boolean auto)
	{
		synchronized (this) {
			if (auto) {
				// We automatically execute the synchronous scheduling loop
				// for synchronous tasklets as part of the schedule()
				// call on this thread even if the scheduling loop itself
				// is not running

				if (this.main == null) {
					this.main = Thread.currentThread();
				}

				runLevel &= 1;
			} else {
				// Otherwise, SYNC tasks are executed only if run()
				// is active on the synchronous thread.

				runLevel ^= 1;
				if (runLevel == 0) {
					main = null;
				}
			}
		}
		return this;
	}

	@Override
	public Scheduler schedule(final Tasklet t, Directive directive) {

		Tasklet next;

		if (directive == Directive.SYNC) {

			// Optimized path for scheduling a SYNC tasklet
			// on the synchronous thread where there
			// are no other SYNC tasklets already queued.

			// If we get to DONE, then we return directly,
			// otherwise we take the slower path to
			// deal with other cases.

			do {
				next = null;
				synchronized (this) {
					if (sync.size() == 0 && main == Thread.currentThread()) {
						next = t;
					}
				}

				if (next != null) {
					directive = next.task();
					switch (directive) {
					case DONE:
						synchronized (this) {
							if (sync.size() == 0) {
								directives.remove(t);
								notifyAll();
								return this;
							}
						}
						next = null;
						break;
					case SYNC:
						continue;
					case WAIT:
					case ASYNC:
					default:
						next = null;
						continue;
					}
				}

			} while (next != null);
		}

		next = t;

		// If we are running on the synchronous thread, then aggressively
		// dequeue and execute any pending synchronous tasklets.

		while (next != null) {

			scheduleCore(next, directive);

			next = null;

			synchronized (this) {
				if (sync.size() > 0) {
					if (main == Thread.currentThread()) {
						next = dequeue();
					}
				}
			}

			if (next != null) {
				try {
					directive = next.task();
				} catch (RuntimeException e) {
					// TODO: allow scheduler to specify exception
					// handling policy for this case.
					e.printStackTrace(System.err);
					directive = Directive.DONE;
				}
			}
		}

		return this;
	}

	/**
	 * Perform the core scheduling actions for one tasklet and one directive.
	 *
	 * <dl>
	 * <dt>SYNC</dt>
	 * <dd>put t, SYNC into directives; t put into sync</dd>
	 * <dt>ASYNC</dt>
	 * <dd>put t, WAIT into directives; start async thread for t; remove t from sync.</dd>
	 * <dt>WAIT</dt>
	 * <dd>put t, WAIT into directives; remove t from sync.</dd>
	 * <dt>DONE</dt>
	 * <dd>remove t from directives and sync.</dd>
	 * </dl>
 	 *
	 * @param t The tasklet.
	 * @param directive
	 */
	private void scheduleCore(final Tasklet t, Directive directive)
		throws SchedulerNotRunningException
	{

		synchronized (this) {

			sync.remove(t);

			switch (directive) {
			case SYNC:
				directives.put(t, directive);
				sync.add(t);
				break;
			case WAIT:
				directives.put(t, directive);
				break;
			case ASYNC:
				if (runLevel < 2) {
					// To avoid this exception, the run method
					// (and hence a scheduling loop) must be
					// active on one thread.
					throw new SchedulerNotRunningException();
				}
				directives.put(t, Directive.WAIT);
				executor.submit(new Runnable() {
					@Override
					public void run() {
						api.with(AsynchronousSchedulerImpl.this, t);
					}
				});
				break;
			case DONE:
				directives.remove(t);
				break;
			default:
				throw new IllegalStateException(
						"illegal state: unknown directive:" + directive);
			}
			this.notifyAll();
		}
	}

	/**
	 * Runs the scheduler until there are no more {@link Tasklet} instances
	 * waiting to be scheduled.
	 */
	@Override
	public void run()
	{
		boolean done = false;
		do {
			api.with(this, new Tasklet() {
				public Directive task() {
					synchronized (this) {
						while ((main != null && main != Thread.currentThread())) {
							return Directive.DONE;
						}
						main = Thread.currentThread();
						runLevel += 2;
					}

					try {

						while (true) {

							Tasklet next = null;

							synchronized(AsynchronousSchedulerImpl.this) {
								if (sync.size() == 0) {
									if (directives.size() == 0) {
										return Directive.DONE;
									} else {
										try {
											AsynchronousSchedulerImpl.this.wait();
										} catch (InterruptedException i) {
											return Directive.DONE;
										}
									}
								} else {
									next = dequeue();
								}
							}

							if (next != null) {
								try {
									schedule(next, next.task());
								} catch (RuntimeException r) {
									schedule(next, Directive.DONE);
								}
							}
						}

					} finally {
						synchronized (this) {
							runLevel -= 2;
							if (runLevel == 0) {
								main = null;
							}
							this.notifyAll();
						}
					}
				}

			});

			synchronized (this) {
				done = (main == null);
				if (!done) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
			}

		} while (!done);
	}

	@Override
	public Rescheduler suspend(final Tasklet tasklet) {
		schedule(tasklet, Directive.WAIT);
		return new ReschedulerImpl(this, tasklet);
	}

	/**
	 * @return Answer the next synchronous Tasklet or null if there is none.
	 */
	private Tasklet dequeue() {
		Tasklet next;
		final Iterator<Tasklet> iter = sync.iterator();
		next = iter.next();
		iter.remove();
		Directive d = directives.remove(next);
		if (d == null) {
			d = Directive.DONE;
		}
		switch (d) {
		case SYNC:
			// expected case
			break;
		case WAIT:
			// re-enqueue a dequeued tasklet.
			directives.put(next, d);
			// a dequeued tasklet remains dequeued.
			next = null;
			break;
		default:
			next = null;
			break;
		}
		return next;
	}

}
