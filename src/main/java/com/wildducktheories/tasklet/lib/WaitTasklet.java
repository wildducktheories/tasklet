package com.wildducktheories.tasklet.lib;

import com.wildducktheories.tasklet.Directive;
import com.wildducktheories.tasklet.Tasklet;

public final class WaitTasklet implements Tasklet {
	@Override
	public Directive task() {
		return Directive.WAIT;
	}
}