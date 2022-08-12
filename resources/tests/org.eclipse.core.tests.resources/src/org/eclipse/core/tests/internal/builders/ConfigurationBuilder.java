/*******************************************************************************
 * Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Assert;

/**
 * A builder used that stores statistics, such as which target was built, per project build config.
 */
public class ConfigurationBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.configbuilder";

	/** Stores IBuildConfiguration -&gt; ConfigurationBuilder */
	private static HashMap<IBuildConfiguration, ConfigurationBuilder> builders = new HashMap<>();
	/** Order in which builders have been run */
	static List<IBuildConfiguration> buildOrder = new ArrayList<>();

	// Per builder instance stats
	int triggerForLastBuild;
	IResourceDelta deltaForLastBuild;
	int buildCount;

	public ConfigurationBuilder() {
	}

	public static ConfigurationBuilder getBuilder(IBuildConfiguration config) {
		return builders.get(config);
	}

	public static void clearBuildOrder() {
		buildOrder = new ArrayList<>();
	}

	public static void clearStats() {
		for (ConfigurationBuilder builder : builders.values()) {
			builder.buildCount = 0;
			builder.triggerForLastBuild = 0;
			builder.deltaForLastBuild = null;
		}
	}

	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();
		builders.put(getBuildConfig(), this);
		buildCount = 0;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		buildCount++;
		triggerForLastBuild = kind;
		deltaForLastBuild = getDelta(getProject());
		buildOrder.add(getBuildConfig());
		return super.build(kind, args, monitor);
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		IResourceDelta delta = getDelta(getProject());
		Assert.assertNull(delta);
		buildCount++;
		triggerForLastBuild = IncrementalProjectBuilder.CLEAN_BUILD;
	}
}
