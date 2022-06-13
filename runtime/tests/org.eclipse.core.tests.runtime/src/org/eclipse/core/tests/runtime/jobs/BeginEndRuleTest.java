/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.concurrent.atomic.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.junit.Test;

/**
 * Tests API methods IJobManager.beginRule and IJobManager.endRule
 */
public class BeginEndRuleTest extends AbstractJobManagerTest {

	/**
	 * This runnable will try to end the given rule in the Job Manager
	 */
	private class RuleEnder implements Runnable {
		private ISchedulingRule rule;
		private AtomicIntegerArray status;

		public RuleEnder(ISchedulingRule rule, AtomicIntegerArray status) {
			this.rule = rule;
			this.status = status;
		}

		@Override
		public void run() {
			try {
				status.set(0, TestBarrier2.STATUS_RUNNING);
				manager.endRule(rule);
				fail("Ending Rule");

			} catch (RuntimeException e) {
				//should fail
			}

		}
	}

	@Test
	public void testRuleCallsProgressProvider_monitorFor() {
		AtomicBoolean createdMonitor = new AtomicBoolean();
		AtomicReference<IProgressMonitor> passedMonitor = new AtomicReference<>();
		manager.setProgressProvider(new ProgressProvider() {

			@Override
			public IProgressMonitor createMonitor(Job job) {
				return new NullProgressMonitor();
			}

			@Override
			public IProgressMonitor monitorFor(IProgressMonitor monitor) {
				assertEquals(passedMonitor.get(), monitor);
				createdMonitor.set(true);
				return super.monitorFor(monitor);
			}
		});
		IdentityRule rule = new IdentityRule();
		IProgressMonitor[] monitors = new IProgressMonitor[] { null, new NullProgressMonitor(),
				SubMonitor.convert(null) };
		for (IProgressMonitor monitor : monitors) {
			createdMonitor.set(false);
			passedMonitor.set(monitor);
			manager.beginRule(rule, monitor);
			try {
				assertTrue("Monitor not created for " + monitor, createdMonitor.get());
			} finally {
				manager.endRule(rule);
			}
		}
	}

	public void testComplexRuleStarting() {
		//test how the manager reacts when several different threads try to begin conflicting rules
		final int NUM_THREADS = 3;
		//array to communicate with the launched threads
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_WAIT_FOR_START,
				TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });
		//number of times to start each rule
		int NUM_REPEATS = 10;

		Job[] jobs = new Job[NUM_THREADS];
		jobs[0] = new JobRuleRunner("ComplexJob1", new PathRule("/testComplexRuleStarting"), status, 0, NUM_REPEATS, true);
		jobs[1] = new JobRuleRunner("ComplexJob2", new PathRule("/testComplexRuleStarting/B"), status, 1, NUM_REPEATS, true);
		jobs[2] = new JobRuleRunner("ComplexJob3", new PathRule("/testComplexRuleStarting/B/C"), status, 2, NUM_REPEATS, true);

		//schedule the jobs
		for (Job job : jobs) {
			job.schedule();
		}

		//wait until all the jobs start
		for (int i = 0; i < jobs.length; i++) {
			TestBarrier2.waitForStatus(status, i, TestBarrier2.STATUS_START);
		}

		//all jobs should be running
		//the status flag should be set to START
		for (int i = 0; i < status.length(); i++) {
			assertEquals("1." + i, Job.RUNNING, jobs[i].getState());
			assertEquals("2." + i, TestBarrier2.STATUS_START, status.get(i));
		}

		//the order that the jobs will be executed
		int[] order = {0, 1, 2};

		for (int j = 0; j < NUM_REPEATS; j++) {
			//let the first job in the order run
			status.set(order[0], TestBarrier2.STATUS_WAIT_FOR_RUN);
			//wait until the first job in the order reads the flag
			TestBarrier2.waitForStatus(status, order[0], TestBarrier2.STATUS_RUNNING);

			//let all subsequent jobs run (they will be blocked)
			//before starting next job, wait until previous job is blocked by JobManager
			for (int i = 1; i < order.length; i++) {
				status.set(order[i], TestBarrier2.STATUS_WAIT_FOR_RUN);
				TestBarrier2.waitForStatus(status, order[i], TestBarrier2.STATUS_BLOCKED);
			}

			//the first job should be running, the remaining jobs should be waiting
			assertEquals("3.0", TestBarrier2.STATUS_RUNNING, status.get(order[0]));
			assertEquals("3.0", TestBarrier2.STATUS_BLOCKED, status.get(order[1]));
			assertEquals("3.0", TestBarrier2.STATUS_BLOCKED, status.get(order[2]));

			//let the first job finish
			status.set(order[0], TestBarrier2.STATUS_WAIT_FOR_DONE);
			TestBarrier2.waitForStatus(status, order[0], TestBarrier2.STATUS_DONE);

			//the remaining jobs will now compete for execution (order NOT guaranteed)
			//let them both start and wait until they complete
			int doneCount = 0;
			while (doneCount < 2) {
				if (status.get(order[1]) == TestBarrier2.STATUS_RUNNING) {
					status.set(order[1], TestBarrier2.STATUS_WAIT_FOR_DONE);
					TestBarrier2.waitForStatus(status, order[1], TestBarrier2.STATUS_DONE);
					doneCount++;
				}
				if (status.get(order[2]) == TestBarrier2.STATUS_RUNNING) {
					status.set(order[2], TestBarrier2.STATUS_WAIT_FOR_DONE);
					TestBarrier2.waitForStatus(status, order[2], TestBarrier2.STATUS_DONE);
					doneCount++;
				}
			}
			//change the order of the jobs, nothing should change in the execution
			int temp = order[0];
			order[0] = order[2];
			order[2] = order[1];
			order[1] = temp;
		}

		//wait until all jobs are done
		for (int element : order) {
			waitForEnd(jobs[element]);
		}

		for (int i = 0; i < jobs.length; i++) {
			//check that the final status of all jobs is correct
			assertEquals("9." + i, TestBarrier2.STATUS_DONE, status.get(i));
			assertEquals("10." + i, Job.NONE, jobs[i].getState());
			assertEquals("11." + i, IStatus.OK, jobs[i].getResult().getSeverity());
		}
	}

	public void testSimpleRuleStarting() {
		//start two jobs, each of which will begin and end a rule several times
		//while one job starts a rule, the second job's call to begin rule should block that thread
		//until the first job calls end rule
		final AtomicIntegerArray status = new AtomicIntegerArray(
				new int[] { TestBarrier2.STATUS_WAIT_FOR_START, TestBarrier2.STATUS_WAIT_FOR_START });
		//number of repetitions of beginning and ending the rule
		final int NUM_REPEATS = 10;
		Job[] jobs = new Job[2];
		jobs[0] = new JobRuleRunner("SimpleJob1", new PathRule("/testSimpleRuleStarting"), status, 0, NUM_REPEATS, false);
		jobs[1] = new JobRuleRunner("SimpleJob2", new PathRule("/testSimpleRuleStarting/B"), status, 1, NUM_REPEATS, false);

		//schedule both jobs to start their execution
		jobs[0].schedule();
		jobs[1].schedule();

		//make sure both jobs are running and their respective run methods have been invoked
		//waitForStart(jobs[1]);
		TestBarrier2.waitForStatus(status, 0, TestBarrier2.STATUS_START);
		TestBarrier2.waitForStatus(status, 1, TestBarrier2.STATUS_START);

		assertEquals("2.0", Job.RUNNING, jobs[0].getState());
		assertEquals("2.1", Job.RUNNING, jobs[1].getState());
		assertEquals("2.2", TestBarrier2.STATUS_START, status.get(0));
		assertEquals("2.3", TestBarrier2.STATUS_START, status.get(1));

		//the order of execution of the jobs (by their index in the status array)
		int first = 0;
		int second = 1;

		//now both jobs are waiting for the STATUS_WAIT_FOR_RUN flag
		for (int j = 0; j < NUM_REPEATS; j++) {
			//let the first job start executing
			status.set(first, TestBarrier2.STATUS_WAIT_FOR_RUN);

			//wait for the first job to read the flag
			TestBarrier2.waitForStatus(status, first, TestBarrier2.STATUS_RUNNING);

			//let the second job start, its thread will be blocked by the beginRule method
			status.set(second, TestBarrier2.STATUS_WAIT_FOR_RUN);

			//only the first job should be running
			//the other job should be blocked by the beginRule method
			assertEquals("3.1", TestBarrier2.STATUS_RUNNING, status.get(first));
			assertEquals("3.2", TestBarrier2.STATUS_WAIT_FOR_RUN, status.get(second));

			//let the first job finish execution and call endRule
			//the second thread will then become unblocked
			status.set(first, TestBarrier2.STATUS_WAIT_FOR_DONE);

			//wait until the first job is done
			TestBarrier2.waitForStatus(status, first, TestBarrier2.STATUS_DONE);

			//now wait until the second job begins execution
			TestBarrier2.waitForStatus(status, second, TestBarrier2.STATUS_RUNNING);

			//the first job is done, the second job is executing
			assertEquals("4.1", TestBarrier2.STATUS_DONE, status.get(first));
			assertEquals("4.2", TestBarrier2.STATUS_RUNNING, status.get(second));

			//let the second job finish execution
			status.set(second,  TestBarrier2.STATUS_WAIT_FOR_DONE);

			//wait until the second job is finished
			TestBarrier2.waitForStatus(status, second, TestBarrier2.STATUS_DONE);

			//both jobs are done now
			assertEquals("5.1", TestBarrier2.STATUS_DONE, status.get(first));
			assertEquals("5.2", TestBarrier2.STATUS_DONE, status.get(second));

			//flip the order of execution of the jobs
			int temp = first;
			first = second;
			second = temp;
		}

		//wait until both jobs are done
		waitForEnd(jobs[second]);
		waitForEnd(jobs[first]);

		//check that the final status of both jobs is correct
		assertEquals("6.1", TestBarrier2.STATUS_DONE, status.get(0));
		assertEquals("6.2", TestBarrier2.STATUS_DONE, status.get(1));
		assertEquals("6.3", Job.NONE, jobs[0].getState());
		assertEquals("6.4", Job.NONE, jobs[1].getState());
		assertEquals("6.5", IStatus.OK, jobs[0].getResult().getSeverity());
		assertEquals("6.6", IStatus.OK, jobs[1].getResult().getSeverity());
	}

	public void testComplexRuleContainment() {
		ISchedulingRule rules[] = new ISchedulingRule[4];

		rules[0] = new PathRule("/testComplexRuleContainment");
		rules[1] = new PathRule("/testComplexRuleContainment/B");
		rules[2] = new PathRule("/testComplexRuleContainment/B/C");
		rules[3] = new PathRule("/testComplexRuleContainment/D");

		//adding multiple rules in correct order
		int RULE_REPEATS = 10;
		try {
			for (int i = 0; i < rules.length - 1; i++) {
				for (int j = 0; j < RULE_REPEATS; j++) {
					manager.beginRule(rules[i], null);
				}
			}
			for (int i = rules.length - 1; i > 0; i--) {
				for (int j = 0; j < RULE_REPEATS; j++) {
					manager.endRule(rules[i - 1]);
				}
			}
		} catch (RuntimeException e) {
			fail("4.0");
		}

		//adding rules in proper order, then adding a rule from a bypassed branch
		//trying to end previous rules should not work
		for (ISchedulingRule rule : rules) {
			manager.beginRule(rule, null);
		}
		try {
			manager.endRule(rules[2]);
			fail("4.1");
		} catch (RuntimeException e) {
			//should fail
			try {
				manager.endRule(rules[1]);
				fail("4.2");
			} catch (RuntimeException e1) {
				//should fail
				try {
					manager.endRule(rules[0]);
					fail("4.3");
				} catch (RuntimeException e2) {
					//should fail
				}
			}
		}
		for (int i = rules.length; i > 0; i--) {
			manager.endRule(rules[i - 1]);
		}
	}

	public void _testEndNullRule() {
		//see bug #43460
		//end null IScheduleRule without begin
		try {
			manager.endRule(null);
			fail("1.1");
		} catch (RuntimeException e) {
			//should fail
		}
	}

	public void testFailureCase() {
		ISchedulingRule rule1 = new IdentityRule();
		ISchedulingRule rule2 = new IdentityRule();

		//end without begin
		try {
			manager.endRule(rule1);
			fail("1.0");
		} catch (RuntimeException e) {
			//should fail
		}
		//simple mismatched begin/end
		manager.beginRule(rule1, null);
		try {
			manager.endRule(rule2);
			fail("1.2");
		} catch (RuntimeException e) {
			//should fail
		}
		//should still be able to end the original rule
		manager.endRule(rule1);

		//mismatched begin/end, ending a null rule
		manager.beginRule(rule1, null);
		try {
			manager.endRule(null);
			fail("1.3");
		} catch (RuntimeException e) {
			//should fail
		}
		//should still be able to end the original rule
		manager.endRule(rule1);
	}

	/**
	 * Tests create a job with one scheduling rule, and then attempting
	 * to acquire an unrelated rule from within that job.
	 */
	public void testFailedNestRuleInJob() {
		final ISchedulingRule rule1 = new PathRule("/testFailedNestRuleInJob/A/");
		final ISchedulingRule rule2 = new PathRule("/testFailedNestRuleInJob/B/");
		final Exception[] exception = new Exception[1];
		Job job = new Job("testFailedNestRuleInJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					try {
						manager.beginRule(rule2, monitor);
					} finally {
						manager.endRule(rule2);
					}
				} catch (RuntimeException e) {
					exception[0] = e;
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule1);
		job.schedule();
		waitForEnd(job);
		assertNotNull("1.0", exception[0]);
		assertTrue("1.1", exception[0].getMessage().indexOf("does not match outer scope rule") > 0);
	}

	public void testNestedCase() {
		ISchedulingRule rule1 = new PathRule("/testNestedCase");
		ISchedulingRule rule2 = new PathRule("/testNestedCase/B");

		//ending an outer rule before an inner one
		manager.beginRule(rule1, null);
		manager.beginRule(rule2, null);
		try {
			manager.endRule(rule1);
			fail("2.0");
		} catch (RuntimeException e) {
			//should fail
		}
		manager.endRule(rule2);
		manager.endRule(rule1);

		//ending a rule that is not open
		manager.beginRule(rule1, null);
		manager.beginRule(rule2, null);
		try {
			manager.endRule(null);
			fail("2.1");
		} catch (RuntimeException e) {
			//should fail
		}
		manager.endRule(rule2);
		manager.endRule(rule1);

		//adding rules starting with null rule
		try {
			manager.beginRule(null, null);
			manager.beginRule(rule1, null);
			manager.endRule(rule1);
			manager.beginRule(rule2, null);
			manager.endRule(rule2);
			manager.endRule(null);
		} catch (RuntimeException e) {
			//should not fail
			fail("2.2");
		}

		//adding numerous instances of the same rule
		int NUM_ADDITIONS = 100;
		try {
			for (int i = 0; i < NUM_ADDITIONS; i++) {
				manager.beginRule(rule1, null);
			}
			for (int i = 0; i < NUM_ADDITIONS; i++) {
				manager.endRule(rule1);
			}
		} catch (RuntimeException e) {
			//should not fail
			fail("2.3");
		}

		//adding numerous instances of the null rule
		try {
			for (int i = 0; i < NUM_ADDITIONS; i++) {
				manager.beginRule(null, null);
			}
			manager.beginRule(rule1, null);
			manager.endRule(rule1);
			for (int i = 0; i < NUM_ADDITIONS; i++) {
				manager.endRule(null);
			}
		} catch (RuntimeException e) {
			//should not fail
			fail("2.4");
		}
	}

	/**
	 * Tests a failure where canceling an attempt to beginRule resulted in implicit jobs
	 * being recycled before they were finished.
	 */
	public void testBug44299() {
		ISchedulingRule rule = new IdentityRule();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		manager.beginRule(rule, monitor);
		AtomicIntegerArray status = new AtomicIntegerArray(new int[1]);
		SimpleRuleRunner runner = new SimpleRuleRunner(rule, status, monitor);
		new Thread(runner).start();

		//wait for the job to start
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
		//give the job a chance to enter the wait loop
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			//ignore
		}
		//cancel the monitor
		monitor.setCanceled(true);

		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
		if (runner.exception != null) {
			fail("1.0", runner.exception);
		}

		//finally clear the rule
		manager.endRule(rule);
	}

	public void testRuleContainment() {
		ISchedulingRule rules[] = new ISchedulingRule[4];

		rules[0] = new PathRule("/testRuleContainment");
		rules[1] = new PathRule("/testRuleContainment/B");
		rules[2] = new PathRule("/testRuleContainment/B/C");
		rules[3] = new PathRule("/testRuleContainment/D");

		//simple addition of rules in incorrect containment order
		manager.beginRule(rules[1], null);
		try {
			manager.beginRule(rules[0], null);
			fail("3.0");
		} catch (RuntimeException e) {
			//should fail
		} finally {
			//still need to end the rule
			manager.endRule(rules[0]);
		}
		manager.endRule(rules[1]);

		//adding rules in proper order, then adding a rule from different hierarchy
		manager.beginRule(rules[1], null);
		manager.beginRule(rules[2], null);
		try {
			manager.beginRule(rules[3], null);
			fail("3.2");
		} catch (RuntimeException e) {
			//should fail
		} finally {
			manager.endRule(rules[3]);
		}
		//should still be able to end the rules
		manager.endRule(rules[2]);
		manager.endRule(rules[1]);
	}

	public void testSimpleOtherThreadAccess() {
		//ending a rule started on this thread from another thread
		ISchedulingRule rule1 = new IdentityRule();
		AtomicIntegerArray status = new AtomicIntegerArray(new int[] { TestBarrier2.STATUS_START });
		Thread endingThread = new Thread(new RuleEnder(rule1, status));
		manager.beginRule(rule1, null);

		endingThread.start();
		TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);

		try {
			endingThread.join();
		} catch (InterruptedException e) {
			//ignore
		}
		//the thread should be dead now
		assertTrue("1.0", !endingThread.isAlive());

		//should be able to end the rule from this thread
		manager.endRule(rule1);

		//starting several rules on this thread, and trying to end them from other threads
		ISchedulingRule rules[] = new ISchedulingRule[3];

		rules[0] = new PathRule("/testSimpleOtherThreadAccess");
		rules[1] = new PathRule("/testSimpleOtherThreadAccess/B");
		rules[2] = new PathRule("/testSimpleOtherThreadAccess/C");

		//end the rules right after starting them
		for (int i = 0; i < rules.length; i++) {
			manager.beginRule(rules[i], null);
			status.set(0, TestBarrier2.STATUS_START);
			Thread t = new Thread(new RuleEnder(rules[i], status));
			t.start();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
			try {
				t.join();
			} catch (InterruptedException e1) {
				//ignore
			}
			//the thread should be dead now
			assertTrue("2." + i, !t.isAlive());
		}

		//try to end the rules when they are all started
		for (int i = 0; i < rules.length; i++) {
			status.set(0, TestBarrier2.STATUS_START);
			Thread t = new Thread(new RuleEnder(rules[i], status));
			t.start();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
			try {
				t.join();
			} catch (InterruptedException e1) {
				//ignore
			}
			//the thread should be dead now
			assertTrue("3." + i, !t.isAlive());
		}

		//try to end the rules after manager.endRule() has been called
		for (int i = rules.length; i > 0; i--) {
			manager.endRule(rules[i - 1]);
			status.set(0, TestBarrier2.STATUS_START);
			Thread t = new Thread(new RuleEnder(rules[i - 1], status));
			t.start();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
			try {
				t.join();
			} catch (InterruptedException e1) {
				//ignore
			}
			//the thread should be dead now
			assertTrue("4." + i, !t.isAlive());
		}
	}

	/**
	 * A job is running.  Wait until it is finished.
	 */
	private void waitForEnd(Job job) {
		int i = 0;
		while (job.getState() != Job.NONE) {
			Thread.yield();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
			Thread.yield();
			//sanity test to avoid hanging tests
			assertTrue("Timeout waiting for job to end", i++ < 100);
		}
	}

	public void testIgnoreScheduleThreadJob() throws Exception {
		final int[] count = new int[1];
		JobChangeAdapter a = new JobChangeAdapter() {
			@Override
			public void running(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				count[0]++;
			}
		};
		Job.getJobManager().addJobChangeListener(a);
		IdentityRule rule = new IdentityRule();
		try {
			Job.getJobManager().beginRule(rule, null);
			Job.getJobManager().currentJob().schedule();
		} finally {
			Job.getJobManager().endRule(rule);
		}
		Thread.sleep(250);
		Job.getJobManager().removeJobChangeListener(a);
		assertEquals("ThreadJob did not ignore reschedule", 0, count[0]);
	}

	public void testRunThreadJobIsNotRescheduled() throws Exception {
		final int[] count = new int[1];
		JobChangeAdapter a = new JobChangeAdapter() {
			@Override
			public void running(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				count[0]++;
			}
		};
		Job.getJobManager().addJobChangeListener(a);
		IdentityRule rule = new IdentityRule();
		try {
			Job.getJobManager().beginRule(rule, null);
		} finally {
			Job.getJobManager().endRule(rule);
		}
		Thread.sleep(250);
		Job.getJobManager().removeJobChangeListener(a);
		assertEquals("ThreadJob did not ignore reschedule", 0, count[0]);
	}

	public void testRunNestedAcquireThreadIsNotRescheduled() throws Exception {
		final PathRule rule = new PathRule(getName());
		final PathRule subRule = new PathRule(getName() + "/subRule");

		final int[] count = new int[1];

		final Job job = new Job(getName() + "acquire") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					Job.getJobManager().beginRule(subRule, null);
				} finally {
					Job.getJobManager().endRule(subRule);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(rule);

		JobChangeAdapter a = new JobChangeAdapter() {
			@Override
			public void running(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
				if (event.getJob() == job) {
					return;
				}
				count[0]++;
			}
		};
		Job.getJobManager().addJobChangeListener(a);
		job.schedule();
		Thread.sleep(250);
		Job.getJobManager().removeJobChangeListener(a);
		assertEquals("ThreadJob did not ignore reschedule", 0, count[0]);
	}

}
