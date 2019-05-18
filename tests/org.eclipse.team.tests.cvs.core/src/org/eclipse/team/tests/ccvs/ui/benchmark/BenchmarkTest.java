/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui.benchmark;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.zip.ZipException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Benchmark test superclass
 */
public abstract class BenchmarkTest extends EclipseTest {

	private HashMap<String, PerformanceMeter> groups;
	private PerformanceMeter currentMeter;

	protected BenchmarkTest() {
	}

	protected BenchmarkTest(String name) {
		super(name);
	}

	protected IProject createUniqueProject(File zipFile) throws TeamException, CoreException, ZipException, IOException, InterruptedException, InvocationTargetException {
		return createAndImportProject(getName(), zipFile);
	}
	
	protected IProject createAndImportProject(String prefix, File zipFile) throws TeamException, CoreException, ZipException, IOException, InterruptedException, InvocationTargetException {
		// create a project with no contents
		IProject project = getUniqueTestProject(prefix);
		BenchmarkUtils.importZip(project, zipFile);
		return project;
	}
	
	/**
	 * @param string
	 */
	protected void startTask(String string) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 
	 */
	protected void endTask() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Create a set of performance meters that can be started with the
	 * startGroup method.
	 * @param performance_groups
	 */
	protected void setupGroups(String[] performance_groups) {
		setupGroups(performance_groups, null, false);
	}
	
	protected void setupGroups(String[] performance_groups, String globalName, boolean global) {
		groups = new HashMap<>();
		Performance perf = Performance.getDefault();
		PerformanceMeter meter = null;
		if (global) {
			// Use one meter for all groups - provides a single timing result
			meter = perf.createPerformanceMeter(perf.getDefaultScenarioId(this));
			for (String suffix : performance_groups) {
				groups.put(suffix, meter);
			}
			perf.tagAsGlobalSummary(meter, globalName, Dimension.ELAPSED_PROCESS);
		} else {
			// Use a meter for each group, provides fine grain results
			for (String suffix : performance_groups) {
				meter = perf.createPerformanceMeter(perf.getDefaultScenarioId(this) + suffix);
				Performance.getDefault().setComment(meter, Performance.EXPLAINS_DEGRADATION_COMMENT, "The current setup for the CVS test does not provide reliable timings. Only consistent test failures over time can be considered significant.");
				groups.put(suffix, meter);
//				if (globalName != null) {
//					perf.tagAsSummary(meter, suffix, Dimension.ELAPSED_PROCESS);
//				}
			}
		}
	}
	
	/**
	 * Commit the performance meters that were created by setupGroups and
	 * started and stopped using startGroup/endGroup
	 */
	protected void commitGroups(boolean global) {
		for (PerformanceMeter meter : groups.values()) {
			meter.commit();
			if(global)
				break;
		}
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setModelSync(false);
	}
	
	@Override
	protected void tearDown() throws Exception {
		try {
			if (groups != null) {
				Performance perf = Performance.getDefault();
				try {
					for (PerformanceMeter meter : groups.values()) {
						perf.assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -100, 20);
					}
				} finally {
					for (PerformanceMeter meter : groups.values()) {
						meter.dispose();
					}
				}
				groups = null;
			}
		} finally {
			super.tearDown();
		}
	}
	
	/**
	 * Start the meter that was created for the given key
	 * @param string
	 */
	protected void startGroup(String key) {
		assertNull(currentMeter);
		currentMeter = groups.get(key);
		currentMeter.start();
	}
	
	/**
	 * End the current meter
	 */
	protected void endGroup() {
		currentMeter.stop();
		currentMeter = null;
	}
	
	protected void disableLog() {
		// TODO:
	}
	
	protected void enableLog() {
		// TODO:
	}
	
	protected void syncResources(SyncInfoSource source, Subscriber subscriber, IResource[] resources) throws TeamException {
		startTask("Synchronize with Repository action");
		source.refresh(subscriber, resources);
		endTask();
	}

	/**
	 * @param resources
	 * @param string
	 * @throws CoreException
	 * @throws TeamException
	 */
	protected void syncCommitResources(SyncInfoSource source, IResource[] resources, String comment) throws TeamException, CoreException {
		startTask("Synchronize outgoing changes");
		syncResources(source, source.createWorkspaceSubscriber(), resources);
		endTask();
		// TODO: Commit all outgoing changes that are children of the given resource
		// by extracting them from the subscriber sync set
		startTask("Commit outgoing changes");
		commitResources(resources, IResource.DEPTH_INFINITE);
		endTask();
	}
	
	/**
	 * @param resources
	 * @throws TeamException
	 */
	protected void syncUpdateResources(SyncInfoSource source, IResource[] resources) throws TeamException {
		startTask("Synchronize incoming changes");
		syncResources(source, source.createWorkspaceSubscriber(), resources);
		endTask();
		// TODO: Update all incoming changes that are children of the given resource
		// by extracting them from the subscriber sync set
		startTask("Update incoming changes");
		updateResources(resources, false);
		endTask();
	}
	
	protected void openEmptyPerspective() throws WorkbenchException {
		// First close any open perspectives
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		try {
			handlerService.executeCommand(
					"org.eclipse.ui.window.closeAllPerspectives", null);
		} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e1) {
		}
		// Now open our empty perspective
		PlatformUI.getWorkbench().showPerspective("org.eclipse.team.tests.cvs.ui.perspective1", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
	}
}
