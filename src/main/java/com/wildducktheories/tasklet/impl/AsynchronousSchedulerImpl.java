package com.wildducktheories.tasklet.impl;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Rescheduler;
import com.wildducktheories.tasklet.Scheduler;
import com.wildducktheories.tasklet.SchedulerAPI;
import com.wildducktheories.tasklet.Tasklet;

/**
 * Provides an implementation of the {@link Scheduler} interface that uses an
 * {@link ExecutorService} to execute the asynchronous phases of {@link Tasklet} instances.
 *
 * @author jonseymour
 */
public class AsynchronousSchedulerImpl implements Scheduler {

	/**
	 * The directives.
	 */
	private final Map<Tasklet, Directive> directives = new IdentityHashMap<Tasklet, Directive>();

	/**
	 * The service used to execute tasks remotely.
	 */
	private final ExecutorService executor;

	/**
	 * The list of {@link Tasklet} instances to execute synchronously.
	 */
	private LinkedHashSet<Tasklet> sync = new LinkedHashSet<Tasklet>();

	/**
	 * The scheduler {@link Thread}.
	 */
	private Thread main;

	/**
	 * True if the enqueuing thread immediately executes the queued.
	 */
	private boolean auto;

	/**
	 * Default constructor uses a cached thread pool.
	 */
	public AsynchronousSchedulerImpl() {
		executor = Executors.newCachedThreadPool();
	}

	/**
	 * @param service An executor service.
	 */
	public AsynchronousSchedulerImpl(ExecutorService service) {
		executor = service;
	}

	@Override
	public Scheduler schedule(final Tasklet t, Directive directive) {

		Tasklet next;

		if (directive == Directive.SYNC) {

			// optimized path for scheduling a SYNC tasklet on the synchronous thread where there
			// are no other SYNC tasklets already queued.

			// if we get to DONE, then we return directly, otherwise we take the slower path to
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

		synchronized (this) {

			directives.put(t, directive);
			sync.remove(t);
			switch (directive) {
			case SYNC:
				sync.add(t);
				break;
			case WAIT:
				break;
			case ASYNC:
				directives.put(t, Directive.WAIT);
				executor.submit(new Runnable() {
					@Override
					public void run() {
						SchedulerAPI.with(AsynchronousSchedulerImpl.this, t);
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

		// if we are running on the synchronous thread, then aggressively
		// dequeue and execute any pending synchronous tasklets.

		do {
			next = null;

			synchronized (this) {
				if (sync.size() > 0) {
					if (main == Thread.currentThread()) {
						next = dequeue();
					} else if (main == null) {
						conditionallyStartSynchronousThread();
					}
				}
			}

			if (next != null) {
				try {
					final Directive d = next.task();
					schedule(next, d);
				} catch (RuntimeException e) {
					schedule(next, Directive.DONE);
				}
			}
		} while (next != null);

		return this;
	}

	private void conditionallyStartSynchronousThread() {
		if (main == null && auto) {
			// if there is no synchronous thread, then start one
			schedule(new Tasklet() {

				@Override
				public Directive task() {
					boolean run = false;
					synchronized (this) {
						if (main == null) {
							main = Thread.currentThread();
							directives.remove(this);
							run = true;
						}
					}
					if (run) {
						run();
					}
					return Directive.DONE;
				}

			}, Directive.ASYNC);
		}
	}

	/**
	 * Runs the scheduler until there are no more {@link Tasklet} waiting to be
	 * scheduled.
	 */
	@Override
	public void run()
	{
		boolean done = false;
		do {
			SchedulerAPI.with(this, new Tasklet() {
				public Directive task() {
					synchronized (this) {
						while ((main != null && main != Thread.currentThread())) {
							return Directive.DONE;
						}
						main = Thread.currentThread();
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
							main = null;
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
			this.auto = auto;
			conditionallyStartSynchronousThread();
		}
		return this;
	}

	/**
	 * @return Return the next synchronous tasklet.
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
