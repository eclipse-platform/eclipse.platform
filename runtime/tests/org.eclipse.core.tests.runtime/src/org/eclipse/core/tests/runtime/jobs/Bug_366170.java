/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	ivan.motsch@bsiag.com - initial example
 *     IBM Corporation - converted to JUnit test
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import java.util.concurrent.Semaphore;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Tests that scheduling multiple jobs with a delay and then blocking all active
 * worker threads doesn't cause job manager to starve. This is a regression
 * test for a situation where all worker threads were blocked and no workers
 * were available to process sleeping jobs awaiting execution. For details see:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=366170
 */
public class Bug_366170 extends AbstractJobManagerTest {
	private Semaphore m_jobBStopHint = new Semaphore(1);

	public void testBug() throws Exception {
		System.out.println("--- Running the examle ---");
		m_jobBStopHint.acquire();
		scheduleJobA(200);
		scheduleJobC(300);

		Thread.sleep(2000L);

		//lock should now be free if C is finished
		assertTrue("Failed: Job C was not run", m_jobBStopHint.tryAcquire());
	}

	private void scheduleJobA(long delay) throws Exception {
		Job job = new Job("A") {
			@Override
			public boolean shouldSchedule() {
				System.out.println("schedule " + getName());
				return true;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				System.out.println("begin " + getName());
				try {
					scheduleJobB(0).join();
				} catch (Throwable t) {
					t.printStackTrace();
				}
				System.out.println("end   " + getName());
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule(delay);
	}

	private Job scheduleJobB(long delay) throws Exception {
		Job job = new Job("B") {
			@Override
			public boolean shouldSchedule() {
				System.out.println("schedule " + getName());
				return true;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				System.out.println("begin " + getName());
				try {
					m_jobBStopHint.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				m_jobBStopHint.release();
				System.out.println("end   " + getName());
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule(delay);
		return job;
	}

	private void scheduleJobC(long delay) throws Exception {
		Job job = new Job("C") {
			@Override
			public boolean shouldSchedule() {
				System.out.println("schedule " + getName());
				return true;
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				System.out.println("begin " + getName());
				m_jobBStopHint.release();
				System.out.println("end   " + getName());
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule(delay);
	}

}
