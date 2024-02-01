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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IBuildContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A builder used that stores the context information passed to it.
 */
public class ContextBuilder extends TestBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.contextbuilder";

	/** Stores IBuildConfiguration -&gt; ContextBuilder */
	private static HashMap<IBuildConfiguration, ContextBuilder> builders = new HashMap<>();
	/** The context information for the last run of this builder */
	IBuildContext contextForLastBuild = null;
	/** The trigger for the last run of this builder */
	int triggerForLastBuild = 0;

	private boolean getRuleCalledForLastBuild = false;
	private IBuildContext contextForLastBuildInGetRule = null;

	private IBuildConfiguration buildConfigurationForLastBuild = null;
	private IBuildConfiguration buildConfigurationForLastBuildInGetRule = null;

	public ContextBuilder() {
	}

	public static ContextBuilder getBuilder(IBuildConfiguration variant) {
		return builders.get(variant);
	}

	public static IBuildContext getContext(IBuildConfiguration variant) {
		if (!builders.containsKey(variant)) {
			return null;
		}
		return getBuilder(variant).contextForLastBuild;
	}

	public static void assertValid() {
		for (ContextBuilder builder : builders.values()) {
			if (builder.getRuleCalledForLastBuild) {
				assertThat(builder.contextForLastBuild).isEqualTo(builder.contextForLastBuildInGetRule);
				assertThat(builder.buildConfigurationForLastBuild)
						.isEqualTo(builder.buildConfigurationForLastBuildInGetRule);
			}
		}
	}

	public static void clearStats() {
		for (ContextBuilder builder : builders.values()) {
			builder.contextForLastBuild = null;
			builder.contextForLastBuildInGetRule = null;
			builder.buildConfigurationForLastBuild = null;
			builder.buildConfigurationForLastBuildInGetRule = null;
			builder.getRuleCalledForLastBuild = false;
			builder.triggerForLastBuild = 0;
		}
	}

	@Override
	protected void startupOnInitialize() {
		builders.put(getBuildConfig(), this);
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		contextForLastBuild = getContext();
		triggerForLastBuild = kind;
		buildConfigurationForLastBuild = getBuildConfig();
		return super.build(kind, args, monitor);
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		contextForLastBuild = getContext();
		triggerForLastBuild = IncrementalProjectBuilder.CLEAN_BUILD;
		buildConfigurationForLastBuild = getBuildConfig();
	}

	@Override
	public ISchedulingRule getRule(int kind, Map<String, String> args) {
		getRuleCalledForLastBuild = true;
		contextForLastBuildInGetRule = getContext();
		buildConfigurationForLastBuildInGetRule = getBuildConfig();
		return super.getRule(kind, args);
	}
}