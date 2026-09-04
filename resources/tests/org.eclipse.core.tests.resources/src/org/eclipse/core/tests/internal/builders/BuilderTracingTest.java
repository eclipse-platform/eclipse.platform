/*******************************************************************************
 *  Copyright (c) 2026 Vogella GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.eclipse.core.internal.events.ResourceStats;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.PerformanceStats.PerformanceListener;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Tests that builder tracing can be switched on at runtime and then attributes
 * build time to the individual builder and project.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class BuilderTracingTest {

	private static final String EVENT_BUILDERS = "org.eclipse.core.resources/perf/builders";
	private static final String OPTION_PERF = "org.eclipse.core.runtime/perf";
	private static final String OPTION_PERF_SUCCESS = "org.eclipse.core.runtime/perf/success";

	private DebugOptions debugOptions;
	private ServiceReference<DebugOptions> debugOptionsReference;
	private boolean debugWasEnabled;
	private boolean tracingWasEnabled;
	private final Map<String, String> replacedOptions = new LinkedHashMap<>();

	@BeforeEach
	public void setUp() {
		BundleContext context = FrameworkUtil.getBundle(BuilderTracingTest.class).getBundleContext();
		debugOptionsReference = context.getServiceReference(DebugOptions.class);
		debugOptions = context.getService(debugOptionsReference);
		debugWasEnabled = debugOptions.isDebugEnabled();
		tracingWasEnabled = ResourceStats.isTracingBuilders();
		for (String option : Arrays.asList(OPTION_PERF, OPTION_PERF_SUCCESS, EVENT_BUILDERS)) {
			replacedOptions.put(option, debugOptions.getOption(option));
		}
		PerformanceStats.clear();
	}

	@AfterEach
	public void tearDown() throws InterruptedException {
		if (debugWasEnabled) {
			replacedOptions.forEach((option, value) -> {
				if (value == null) {
					debugOptions.removeOption(option);
				} else {
					debugOptions.setOption(option, value);
				}
			});
		} else {
			// disabling debug discards all options that were set
			debugOptions.setDebugEnabled(false);
		}
		waitUntil(() -> ResourceStats.isTracingBuilders() == tracingWasEnabled,
				"builder tracing was not restored to its previous state");
		PerformanceStats.clear();
		FrameworkUtil.getBundle(BuilderTracingTest.class).getBundleContext().ungetService(debugOptionsReference);
	}

	/**
	 * Debug options listeners are notified asynchronously, so a change to the
	 * options only takes effect a moment later.
	 */
	private static void waitUntil(BooleanSupplier condition, String message) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 30_000;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
			Thread.sleep(10);
		}
		assertTrue(condition.getAsBoolean(), message);
	}

	private void enableBuilderTracing() throws InterruptedException {
		debugOptions.setDebugEnabled(true);
		debugOptions.setOption(OPTION_PERF, "true");
		debugOptions.setOption(OPTION_PERF_SUCCESS, "true");
		// a threshold of 0 records every builder run, not just the slow ones
		debugOptions.setOption(EVENT_BUILDERS, "0");
		waitUntil(() -> ResourceStats.isTracingBuilders(), "builder tracing did not take effect");
	}

	private IProject createProjectWithSortBuilder(String name) throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(name);
		setAutoBuilding(false);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		updateProjectDescription(project).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId(name).apply();
		return project;
	}

	@Test
	public void testTracingFollowsDebugOptionsAtRuntime() throws InterruptedException {
		// start from a known state, the suite may run with tracing already enabled
		debugOptions.setOption(OPTION_PERF, "false");
		waitUntil(() -> !PerformanceStats.ENABLED, "tracing could not be switched off");
		assertFalse(PerformanceStats.isEnabled(EVENT_BUILDERS), "tracing should start out disabled");

		enableBuilderTracing();
		assertTrue(PerformanceStats.ENABLED, "the global tracing flag should follow the debug option");
		assertTrue(PerformanceStats.isEnabled(EVENT_BUILDERS), "builder tracing should be enabled");

		debugOptions.setOption(OPTION_PERF, "false");
		assertFalse(PerformanceStats.isEnabled(EVENT_BUILDERS), "builder tracing should be disabled again");
		waitUntil(() -> !PerformanceStats.ENABLED, "the global tracing flag should be switched off again");
	}

	@Test
	public void testBuildIsAttributedToBuilderAndProject() throws Exception {
		IProject project = createProjectWithSortBuilder("tracedProject");
		enableBuilderTracing();

		getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		assertTrue(Arrays.stream(PerformanceStats.getAllStats())
				.anyMatch(stats -> EVENT_BUILDERS.equals(stats.getEvent())
						&& stats.getBlameString().contains(SortBuilder.class.getSimpleName())
						&& project.getName().equals(stats.getContext())),
				"expected a builder event blaming " + SortBuilder.class.getSimpleName() + " on " + project.getName()
						+ " but got " + Arrays.toString(PerformanceStats.getAllStats()));
	}

	@Test
	public void testListenerIsNotifiedAboutBuilderRuns() throws Exception {
		IProject project = createProjectWithSortBuilder("listenedProject");
		CountDownLatch reported = new CountDownLatch(1);
		PerformanceListener listener = new PerformanceListener() {
			@Override
			public void eventFailed(PerformanceStats event, long duration) {
				if (EVENT_BUILDERS.equals(event.getEvent()) && project.getName().equals(event.getContext())) {
					reported.countDown();
				}
			}
		};

		enableBuilderTracing();
		PerformanceStats.addListener(listener);
		try {
			getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
			assertTrue(reported.await(30, TimeUnit.SECONDS), "listener was not notified about the builder run");
		} finally {
			PerformanceStats.removeListener(listener);
		}
	}
}
