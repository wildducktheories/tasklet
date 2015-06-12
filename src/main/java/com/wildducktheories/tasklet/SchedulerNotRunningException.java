package com.wildducktheories.tasklet;

/**
 * This exception is thrown when an attempt is made to asynchronously schedule a {@link Tasklet} with 
 * a {@link Scheduler} that does not currently have a thread executing the {@link Scheduler#run()} method.
 * <p>
 * Usually, the way to fix this problem is to encapsulate code that needs to asynchronously schedule a
 * tasklet into a {@link Tasklet} and then invoke the tasklet like this:
 * <p>
 * <pre>
 * SchedulerAPI
 *   .newScheduler()
 *   .schedule(new Tasklet() {
 *      public Directive tasklet() {
 *        //code that needs to perform asynchronous scheduling.
 *        return Directive.DONE;
 *      }
 *    }, Directive.SYNC)
 *    .run();
 * </pre> 
 * The call to <code>run()</code> will return when all the synchronous and asynchronous effects implied by the body of the
 * {@link Tasklet} instance's {@link Tasklet#task()} method have completed execution.
 * <p>
 * Higher-level APIs, such as a {@link Tasklet}-based Promise API might also have other ways to arrange 
 * for the execution of {@link Scheduler#run()} method that are more appropriate to the use cases of those APIs so
 * it is best to check the documentation of those APIs for further guidance about what those options might be.
 * @author jonseymour
 */
public class SchedulerNotRunningException extends IllegalStateException {

	private static final long serialVersionUID = 1162718568404959615L;

	public SchedulerNotRunningException() {
		super("Illegal attempt to asynchronously schedule a tasklet on a thread whose scheduler does not have an active scheduler loop.");
	}

}
