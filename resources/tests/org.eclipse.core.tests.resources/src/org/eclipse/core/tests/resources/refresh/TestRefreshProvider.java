/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.refresh;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import junit.framework.AssertionFailedError;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.refresh.*;

/**
 *
 */
public class TestRefreshProvider extends RefreshProvider implements IRefreshMonitor {
	private final List<AssertionFailedError> failures = new CopyOnWriteArrayList<>();
	private final Set<Object> monitoredResources = Collections.synchronizedSet(new HashSet<>());
	private static volatile TestRefreshProvider instance;

	public static TestRefreshProvider getInstance() {
		return instance;
	}

	public TestRefreshProvider() {
		instance = this;
	}

	/**
	 * Resets the provider for the next test.
	 */
	public static void reset() {
		if (instance != null) {
			instance.failures.clear();
			instance.monitoredResources.clear();
		}
	}

	/**
	 * Returns the failures, or an empty array if there were no failures.
	 */
	public AssertionFailedError[] getFailures() {
		return failures.toArray(new AssertionFailedError[failures.size()]);
	}

	/**
	 * Returns the resources that are currently being monitored by this refresh provider.
	 */
	public IResource[] getMonitoredResources() {
		return monitoredResources.toArray(new IResource[monitoredResources.size()]);
	}

	@Override
	public IRefreshMonitor installMonitor(IResource resource, IRefreshResult result) {
		if (!monitoredResources.add(resource)) {
			failures.add(new AssertionFailedError("installMonitor on resource that is already monitored: " + resource));
		}
		return this;
	}

	@Override
	public void unmonitor(IResource resource) {
		if (resource == null) {
			monitoredResources.clear();
			return;
		}
		if (!monitoredResources.remove(resource)) {
			failures.add(new AssertionFailedError("Unmonitor on resource that is not monitored: " + resource));
		}
	}
}
