#NAME
tasklet - a Java library for co-operative task scheduling

#DESCRIPTION

##Tasklet
Tasklets are small units of work that co-operate with a tasklet scheduler to decide how (synchronously or asynchronously) and when they are next executed.

Tasklets implement a <code>task()</code> method which is called by the scheduler to perform the tasklet's work. When a tasklet completes
execution it returns a new directive to the scheduler to indicate how the scheduler should next schedule
that tasklet, if at all.

As a motivating example, consider a 3-state, state machine in which a blocking call is executed in an asynchronous thread and the
call preparation and post-processing are both performed on the scheduler's synchronous thread.

	SchedulerAPI
		.with(
			SchedulerAPI.newScheduler(),
			new Tasklet() {
				int state = 0;
				Request req;
				Response rsp;
				public Directive task() {
					switch (state) {
					case 0:
						// create call request object from state in the synchronous thread

						req = ...

						state=1;
						return Directive.ASYNC;

					case 1:

						// make a blocking I/O call in an asynchronous thread
						rsp = call(req);

						state=2;
						return Directive.SYNC;

					case 2:
					default:

						// merge the results of the blocking I/O call into state on the synchronous thread.

						return Directive.DONE;
					}
				}
			}
		).run();

In this example:

* the tasklet, initially in state 0, executes on the current thread where it can use thread-safe access to the
main thread's state to build a I/O request
* it then moves to state 1 and reschedules itself onto an asynchronous thread by returning Directive.ASYNC.
* in state 1, while executing on an asynchronous thread, it makes a blocking I/O call
* when the blocking I/O call completes, it moves to state 2 and reschedules itself back onto the main thread by returning
Directive.SYNC
* in state 2, while executing on the main thread, it completes integration of the call's result into the main thread's state
and then removes itself from the scheduler by returning Directive.DONE.

Several consequences follow from this structure:

* the state maintained by the main thread is not referenced or mutated by an asynchronous thread, so no explicit locks are required
* no blocking I/O calls are made on the main thread, so the main thread can remain responsive for other purposes

It should be noted that this example is not meant to imply that application programmers should use tasklets
directly in this way. Rather, it is expected that application programmers would use higher level abstractions like
promises and actors and that the implementations of those abstractions might usefully be implemented by making
use of the tasklet and scheduler abstractions.

##SchedulerAPI

The SchedulerAPI class provides 4 static methods that manage the association between the current thread
and its Scheduler instance. These calls are:

* newScheduler() - creates a new Scheduler instance. The caller of this method is expected to schedule one or more tasklets
with <code>schedule(Tasklet, Directive)</code> or to call <code>with(Scheduler, Tasklet)</code> and then call the scheduler's <code>run()</code> method.
* with(Scheduler, Tasklet) - executes the <code>task()</code> method of the specified tasklet after temporarily changing the current thread's scheduler to be the specified
scheduler. The specified tasklet is rescheduled with the specified scheduler using the return value of the call to the tasklet's <code>task()</code> method.
* getScheduler() - answers the Scheduler instance currently associated with the current thread or create a new such instance.
* reset() - removes any ThreadLocal associated with an unguarded use of the <code>SchedulerAPI.getScheduler()</code> call.

As a general rule, <code>SchedulerAPI.getScheduler()</code> should only be called within
the body of a <code>Tasklet.task()</code> method (or something that it calls) that is executing as the result of
an active <code>Scheduler.schedule(Tasklet, Directive)</code> call or an active <code>SchedulerAPI.with(Scheduler, Tasklet)</code> call. In cases of calls that
are not provably guarded in this way, the
application must arrange to call have <code>SchedulerAPI.reset()</code> called on each thread that may have called this method in an
unguarded manner before a reference to the thread is lost. This call is required in order to guarantee the release of a <code>ThreadLocal</code>
that might otherwise pin the SchedulerAPI's class loader.

Also as a general rule, code that calls <code>SchedulerAPI.getScheduler()</code> SHOULD NOT also call <code>Scheduler.run()</code>
since it is the creator of a scheduler's responsibility to decide when the synchronous thread of a scheduler starts
running and the caller of <code>SchedulerAPI.getScheduler()</code> usually cannot prove that it is ultimately the creator
of the instance returned by that method. (A thread that can prove this fact should usually call SchedulerAPI.newScheduler() instead).

##Scheduler
The Scheduler class provides methods for scheduling Tasklet instances and for managing the state of the
scheduler's synchronous thread.

###method: void run()

By default, a new scheduler's synchronous thread will not start running automatically and must be started
by calling the <code>Scheduler.run()</code> method. The receiving scheduler will become the current thread's scheduler and
the current thread will become the scheduler's synchronous thread. The call will return when all the scheduler's suspended
tasklets have been resumed and all the scheduler's synchronous and asynchronous tasklets have completed running.

If some other thread is running as the scheduler's synchronous thread, then a call to <code>run()</code> will block until that other thread
has exited the scheduler. Also note that a live lock may result if an asynchronously scheduled tasklet calls the run() method of its own scheduler so such
calls should be avoided.

###method: Scheduler schedule(Tasklet, Directive)
The <code>schedule(Tasklet, Directive)</code> method allows tasklets to be scheduled with one of four directives.

In the following example, the statement creates a new scheduler, schedules a tasklet to run on the scheduler's synchronous thread, then runs
the scheduler with the current thread as the scheduler's synchronous thread. The statement does not complete until the synchronous
tasklet (and any tasklets it has spawned in the same scheduler) have completed execution.

	SchedulerAPI
		.newScheduler()
		.schedule(new Tasklet() {
			// ...
		 }, Directive.SYNC)
		.run();

###method: Rescheduler suspend(Tasklet)
It may sometimes be necessary to schedule a suspended tasklet to indicate that the scheduler should not exit
until some external event, such as a timeout, has resumed the suspended tasklet. Such tasklets may be scheduled with a WAIT directive indicating
to the scheduler that some external event will eventually reschedule the tasklet with another directive. To simplify
the task of resuming the tasklet with the correct scheduler, the <code>suspend()</code> method may be used to obtain a
Rescheduler object that can be later used to resume the suspended tasklet. For example:

	new Tasklet() {

		public Directive task() {

			// ...

			final Rescheduler rescheduler = SchedulerAPI
				.getScheduler()
				.suspend(this);

			Timer.schedule(new TimerTask() {
				public void run() {
					rescheduler.resume(Directive.ASYNC);
				}
			}, 1000L);

			return Directive.WAIT;

			// ...
		}
 	}


##Directives
A scheduling directive indicates to a scheduler one of 4 possible actions it may take with respect to a
related tasklet. These directives are:

* SYNC
* ASYNC
* WAIT
* DONE

###SYNC
The scheduler MUST schedule the tasklet on the scheduler's synchronous thread.

###ASYNC
The scheduler MUST schedule the tasklet on a thread other than the scheduler's main thread.

###WAIT
The scheduler MUST not exit before a subsequent directive for the tasklet is received.

###DONE
The scheduler MUST remove all references to the tasklet from the scheduler.


