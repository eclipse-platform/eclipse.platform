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
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.Map;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Assert;

/**
 * A test builder that allows specification of what build triggers it responds to.
 */
public class CustomTriggerBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.flexbuilder";
	private static CustomTriggerBuilder singleton;
	int triggerForLastBuild;

	/**
	 * Returns the singleton instance, or null if none has been created.
	 */
	public static CustomTriggerBuilder getInstance() {
		return singleton;
	}

	public static void resetSingleton() {
		singleton = null;
	}

	public CustomTriggerBuilder() {
		singleton = this;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		triggerForLastBuild = kind;
		return super.build(kind, args, monitor);
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		triggerForLastBuild = IncrementalProjectBuilder.CLEAN_BUILD;
		IResourceDelta delta = getDelta(getProject());
		Assert.assertNull(delta);
	}

	public void clearBuildTrigger() {
		triggerForLastBuild = 0;
	}

	public boolean wasAutobuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.AUTO_BUILD;
	}

	public boolean wasCleanBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.CLEAN_BUILD;
	}

	public boolean wasFullBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.FULL_BUILD;
	}

	public boolean wasIncrementalBuild() {
		return triggerForLastBuild == IncrementalProjectBuilder.INCREMENTAL_BUILD;
	}
}
