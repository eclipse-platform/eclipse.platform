/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.eclipse.core.tests.runtime.RuntimeTestsPlugin.PI_RUNTIME_TESTS;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicIntegerArray;
import junit.framework.AssertionFailedError;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.harness.TestBarrier2;
import org.eclipse.core.tests.harness.session.SessionShouldError;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for bug 412138.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class Bug_412138 {
	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RUNTIME_TESTS).create();

	private static final String FILE_NAME = FileSystemHelper.getTempDir().append(Bug_412138.class.getName()).toOSString();

	@Test
	@SessionShouldError
	public void testRunScenario() throws InterruptedException {
		// delete the file so that we don't report previous results
		new File(FILE_NAME).delete();
		final AtomicIntegerArray status = new AtomicIntegerArray(new int[] { -1 });
		final Job fakeBuild = new Job("Fake AutoBuildJob") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// synchronize on the job object
					synchronized (this) {
						// let the other thread call join on this job now
						status.set(0, TestBarrier2.STATUS_RUNNING);
						// go to sleep to allow the other thread to acquire JobManager.lock inside join
						Thread.sleep(3000);
						// call a method that requires JobManager.lock
						isBlocking();
						return Status.OK_STATUS;
					}
				} catch (InterruptedException e) {
					return new Status(IStatus.ERROR, PI_RUNTIME_TESTS, e.getMessage(), e);
				}
			}
		};
		Job job = new Job("Some job") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_RUNNING);
				try {
					fakeBuild.join();
					status.set(0, TestBarrier2.STATUS_DONE);
					return Status.OK_STATUS;
				} catch (InterruptedException e) {
					return new Status(IStatus.ERROR, PI_RUNTIME_TESTS, e.getMessage(), e);
				}
			}
		};
		try {
			job.schedule();
			fakeBuild.schedule();
			TestBarrier2.waitForStatus(status, TestBarrier2.STATUS_DONE);
			job.join();
			fakeBuild.join();
			assertTrue(job.getResult() != null && job.getResult().isOK());
			assertTrue(fakeBuild.getResult() != null && fakeBuild.getResult().isOK());
		} catch (AssertionFailedError e) {
			// the test failed so there is a deadlock, but this deadlock would prevent us
			// from reporting test results; serialize the error to a helper file and
			// exit JVM to "resolve" deadlock
			try {
				try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
					stream.writeObject(e);
				}
			} catch (IOException e1) {
				// we can't do anything if saving the error failed
				// print the original error, so that there is at least some trace
				e.printStackTrace();
			}
		} finally {
			// make sure the test always crashes to satisfy addCrashTest method contract
			// test result will be verified by the testVerifyResult method
			System.exit(1);
		}
	}

	@Test
	public void testVerifyResult() throws IOException, ClassNotFoundException {
		File file = new File(FILE_NAME);
		// if the file does not exist, there was no deadlock so the whole test pass
		if (file.exists()) {
			try {
				AssertionFailedError e;
				try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
					e = (AssertionFailedError) stream.readObject();
				}
				throw e;
			} catch (IOException | ClassNotFoundException e) {
				// re-throw since file existence already says the test failed
				throw e;
			} finally {
				// helper file is no longer needed
				file.delete();
			}
		}
	}
}
