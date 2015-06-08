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

* Scheduler newScheduler()
* Scheduler with(Scheduler, Directive)
* Scheduler getScheduler()
* void reset()

### method: Scheduler newScheduler()
Creates a new Scheduler instance.

The caller of this method is expected to schedule one or more tasklets with <code>Scheduler.schedule(Tasklet, Directive)</code> or to call
<code>SchedulerAPI.with(Scheduler, Tasklet)</code> and then call the scheduler's <code>run()</code> method.
For example:

	SchedulerAPI
		.newScheduler()
		.schedule(new Tasklet() { .. }, Directive.SYNC)
		.run()

Or:

	SchedulerAPI
		.with(
			SchedulerAPI.newScheduler(),
			new Tasklet() { .. }
		 )
		.run()


### method: Scheduler with(Scheduler, Tasklet)
Executes the <code>task()</code> method of the specified tasklet after temporarily changing the
current thread's scheduler to be the specified scheduler. The specified tasklet is rescheduled
with the specified scheduler using the return value of the call to the tasklet's <code>task()</code> method.

The original scheduler associated with the current thread is restored before this call returns.

Also note that this call is used to invoke a tasklet on an asynchronous thread when a tasklet
is scheduled with a scheduler with an ASYNC directive. So, all asynchronously scheduled tasklets begin
executing with their spawning scheduler as the current scheduler on their execution thread.

### method: Scheduler getScheduler()
Answers the Scheduler instance currently associated with the current thread or create a new such instance. If a
new instance is created, the synchronous thread for this instance will be automatically created when required.

As a general rule, <code>SchedulerAPI.getScheduler()</code> should only be called on threads with
active <code>Scheduler.schedule(Tasklet, Directive)</code> or <code>SchedulerAPI.with(Scheduler, Tasklet)</code>
calls. Refer to the description of the <code>SchedulerAPI.reset()</code> method for more information about what to do in cases
where possibly unguarded calls to <code>SchedulerAPI.getScheduler()</code> cannot be avoided.

In general, it is not safe to call <code>Scheduler.run()</code> on a Scheduler
returned by <code>SchedulerAPI.getScheduler()</code> since this call may have been made
by another thread and in some circumstances this may prevent both calls terminating.

### method: void reset()
Removes any ThreadLocal associated with an unguarded use of the <code>SchedulerAPI.getScheduler()</code> call.

In cases where <code>SchedulerAPI.getScheduler()</code> calls that are not provably guarded by active
calls to <code>Scheduler.schedule(Tasklet, Directive)</code> or <code>SchedulerAPI.with(Scheduler, Tasklet)</code>,
the application must arrange to have <code>SchedulerAPI.reset()</code> called on each thread that may have called
<code>SchedulerAPI.getScheduler()</code> in an unguarded manner before a reference to each such a thread is lost. This call
is required in order to guarantee the release of a <code>ThreadLocal</code> that might
otherwise pin the SchedulerAPI's class loader.


##Scheduler
The Scheduler class provides methods for scheduling Tasklet instances and for managing the state of the
scheduler's synchronous thread.

###method: void run()

By default, a new scheduler's synchronous thread will not start running automatically and must be started
by calling the <code>Scheduler.run()</code> method.

The receiving scheduler becomes the current thread's scheduler and the current thread becomes
the scheduler's synchronous thread. The call will return when all the scheduler's suspended
tasklets have been resumed and all the scheduler's synchronous and asynchronous tasklets have completed running.

If some other thread is running as the scheduler's synchronous thread, then a call to <code>run()</code> will block until that other thread
has exited the scheduler. Also note that a live lock may result if an asynchronously scheduled tasklet calls the run() method of its own scheduler so such
calls should be avoided.

###method: Scheduler schedule(Tasklet, Directive)
The <code>Scheduler.schedule(Tasklet, Directive)</code> method allows tasklets to be scheduled with the receiving
Scheduler with one of four directives:

* SYNC
* ASYNC
* WAIT
* DONE

####SYNC
The scheduler MUST schedule the tasklet to execute on the scheduler's synchronous thread. The tasklet will not
execute until the scheduler's synchronous thread starts running.

####ASYNC
The scheduler MUST schedule the tasklet on a thread other than the scheduler's main thread.

####WAIT
The scheduler MUST not exit before the tasklet is rescheduled with the scheduler with a different directive.

####DONE
The scheduler MUST remove all references to the tasklet from the scheduler.

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




