package com.wildducktheories.tasklet;

/**
 * This exception is thrown if an ASYNC directive is used with a  {@link Scheduler#schedule(Tasklet, Directive)} c
 * call and there is no synchronous thread running at the time the call is made.
 * 
 * @author jonseymour
 */
public class SchedulerNotRunningException extends IllegalStateException {

	private static final long serialVersionUID = 1162718568404959615L;

	public SchedulerNotRunningException() {
		super("Attempt to use ASYNC directive when there is dedicate SYNC thread");
	}

}
