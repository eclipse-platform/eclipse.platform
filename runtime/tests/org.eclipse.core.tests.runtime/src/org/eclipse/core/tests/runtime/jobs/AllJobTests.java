/*******************************************************************************
 *  Copyright (c) 2003, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *     Thirumala Reddy Mutchukota - Bug 432049, JobGroup API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs all job tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		YieldTest.class, IJobManagerTest.class, JobGroupTest.class, JobQueueTest.class, OrderedLockTest.class,
		BeginEndRuleTest.class, JobTest.class, DeadlockDetectionTest.class, Bug_129551.class, Bug_211799.class,
		Bug_307282.class, Bug_307391.class, MultiRuleTest.class, Bug_311756.class, Bug_311863.class, Bug_316839.class,
		Bug_320329.class, Bug_478634.class, Bug_550738.class, Bug_574883.class, Bug_412138.class,
		Bug_574883Join.class, GithubBug_193.class, JobEventTest.class,
		WorkerPoolTest.class
})
public class AllJobTests {
}
