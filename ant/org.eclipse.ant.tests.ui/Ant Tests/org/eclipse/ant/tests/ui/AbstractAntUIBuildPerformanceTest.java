/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.After;
import org.junit.Before;

public class AbstractAntUIBuildPerformanceTest extends AbstractAntUIBuildTest {

	protected PerformanceMeter fPerformanceMeter;

	/**
	 * Overridden to create a default performance meter for this test case.
	 */
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Performance performance = Performance.getDefault();
		fPerformanceMeter = performance.createPerformanceMeter(performance.getDefaultScenarioId(this.getClass()));
	}

	/**
	 * Overridden to dispose of the performance meter.
	 */
	@After
	public void tearDown() throws Exception {
		fPerformanceMeter.dispose();
	}

	/**
	 * Mark the scenario of this test case to be included into the global performance summary. The summary shows the given dimension of the scenario
	 * and labels the scenario with the short name.
	 * 
	 * @param shortName
	 *            a short (shorter than 40 characters) descritive name of the scenario
	 * @param dimension
	 *            the dimension to show in the summary
	 */
	public void tagAsGlobalSummary(String shortName, Dimension dimension) {
		Performance performance = Performance.getDefault();
		performance.tagAsGlobalSummary(fPerformanceMeter, shortName, new Dimension[] { dimension });
	}

	/**
	 * Mark the scenario of this test case to be included into the performance summary. The summary shows the given dimension of the scenario and
	 * labels the scenario with the short name.
	 * 
	 * @param shortName
	 *            a short (shorter than 40 characters) descriptive name of the scenario
	 * @param dimension
	 *            the dimension to show in the summary
	 */
	public void tagAsSummary(String shortName, Dimension dimension) {
		Performance performance = Performance.getDefault();
		performance.tagAsSummary(fPerformanceMeter, shortName, new Dimension[] { dimension });
	}

	/**
	 * Mark the scenario represented by the given PerformanceMeter to be included into the global performance summary. The summary shows the given
	 * dimensions of the scenario and labels the scenario with the short name.
	 * 
	 * @param shortName
	 *            a short (shorter than 40 characters) descriptive name of the scenario
	 * @param dimensions
	 *            an array of dimensions to show in the summary
	 */
	public void tagAsGlobalSummary(String shortName, Dimension[] dimensions) {
		Performance performance = Performance.getDefault();
		performance.tagAsGlobalSummary(fPerformanceMeter, shortName, dimensions);
	}

	/**
	 * Called from within a test case immediately before the code to measure is run. It starts capturing of performance data. Must be followed by a
	 * call to {@link #stopMeasuring()} before subsequent calls to this method or {@link #commitMeasurements()}.
	 */
	protected void startMeasuring() {
		fPerformanceMeter.start();
	}

	protected void stopMeasuring() {
		fPerformanceMeter.stop();
	}

	protected void commitMeasurements() {
		fPerformanceMeter.commit();
	}

	/**
	 * Asserts default properties of the measurements captured for this test case.
	 * 
	 * @throws RuntimeException
	 *             if the properties do not hold
	 */
	protected void assertPerformance() {
		Performance.getDefault().assertPerformance(fPerformanceMeter);
	}

	/**
	 * Asserts that the measurement specified by the given dimension is within a certain range with respect to some reference value. If the specified
	 * dimension isn't available, the call has no effect.
	 * 
	 * @param dim
	 *            the Dimension to check
	 * @param lowerPercentage
	 *            a negative number indicating the percentage the measured value is allowed to be smaller than some reference value
	 * @param upperPercentage
	 *            a positive number indicating the percentage the measured value is allowed to be greater than some reference value
	 * @throws RuntimeException
	 *             if the properties do not hold
	 */
	protected void assertPerformanceInRelativeBand(Dimension dim, int lowerPercentage, int upperPercentage) {
		Performance.getDefault().assertPerformanceInRelativeBand(fPerformanceMeter, dim, lowerPercentage, upperPercentage);
	}

	/**
	 * Launches the Ant build for this config. Waits for all of the lines to be appended to the console.
	 * 
	 * @param config
	 *            the launch configuration to execute
	 * @param i
	 *            the number of times to perform the launch
	 */
	protected void launch(ILaunchConfiguration config, int i) throws CoreException {
		startMeasuring();
		for (int j = 0; j < i; j++) {
			super.launch(config);
		}
		stopMeasuring();
	}
}
