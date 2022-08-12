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
 *     James Blackburn (Broadcom Corp.) Bug306824 add call-backs for getRule/build
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.junit.Assert;

/**
 * An abstract builder that is designed to be extended for testing purposes.
 */
public abstract class TestBuilder extends IncrementalProjectBuilder {

	/**
	 * A test specific call-back which can be ticked on #getRule(...) &amp; #build(...)
	 */
	public static class BuilderRuleCallback {
		IncrementalProjectBuilder builder;

		public BuilderRuleCallback() {
		}

		/**
		 * Fetch the scheduling rule for the build
		 */
		public ISchedulingRule getRule(String name, IncrementalProjectBuilder builder, int trigger, Map<String, String> args) {
			return ResourcesPlugin.getWorkspace().getRoot();
		}

		/**
		 * Build call-back
		 */
		public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
			return new IProject[0];
		}

		public IResourceDelta getDelta(IProject project) {
			return builder.getDelta(project);
		}
	}

	/**
	 * Build rule getter
	 */
	volatile BuilderRuleCallback ruleCallBack;

	/**
	 * Build command parameters.
	 */
	public static final String BUILD_ID = "BuildID";
	public static final String INTERESTING_PROJECT = "InterestingProject";
	/**
	 * Lifecycle event identifiers
	 */
	public static final String STARTUP_ON_INITIALIZE = "StartupOnInitialize";
	public static final String SET_INITIALIZATION_DATA = "SetInitializationData";
	public static final String DEFAULT_BUILD_ID = "Build0";
	/**
	 * The arguments for one run of the builder.
	 */
	protected Map<String, String> arguments;
	private IConfigurationElement config = null;
	private String name = null;
	private Object data = null;
	/**
	 * These are static because we want one event set for all builder instances.
	 */
	private static final ArrayList<String> expectedEvents = new ArrayList<>();
	private static final ArrayList<String> actualEvents = new ArrayList<>();

	/**
	 * Logs the given plug-in lifecycle event for this builder's plugin.
	 */
	public void addExpectedLifecycleEvent(String event) {
		expectedEvents.add(event);
	}

	/**
	 * Verifies that the given lifecycle events occurred. Throws an assertion
	 * failure if expectations are not met. If successful, clears the list of
	 * expected and actual events in preparation for the next test.
	 */
	public void assertLifecycleEvents(String text) {
		Assert.assertEquals(text, expectedEvents, actualEvents);
		reset();
	}

	/**
	 * Implements the inherited abstract method in
	 * <code>InternalBuilder</code>.
	 *
	 * @see InternalBuilder#build(IResourceDelta,int,IProgressMonitor)
	 */
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		arguments = args == null ? new HashMap<>(1) : args;
		logPluginLifecycleEvent(getBuildId());
		if (ruleCallBack == null) {
			return new IProject[0];
		}
		ruleCallBack.builder = this;
		return ruleCallBack.build(kind, args, monitor);
	}

	/**
	 * Allow overriding the default scheduling rule
	 * @see IncrementalProjectBuilder#getRule(int, Map)
	 */
	@Override
	public ISchedulingRule getRule(int trigger, Map<String, String> args) {
		if (ruleCallBack == null) {
			return super.getRule(trigger, args);
		}
		ruleCallBack.builder = this;
		return ruleCallBack.getRule(name, this, trigger, args);
	}

	/**
	 * @param callback callback to be used for fetching rules
	 */
	public void setRuleCallback(BuilderRuleCallback callback) {
		ruleCallBack = callback;
	}

	/**
	 * Returns an ID that identifies the current build.
	 */
	private String getBuildId() {
		String buildId = arguments.get(BUILD_ID);
		if (buildId == null) {
			buildId = DEFAULT_BUILD_ID;
		}
		return buildId;
	}

	/**
	 * Return the configuration element that created this builder.
	 *
	 * @see setInitializationData(IConfigurationElement, String, Object)
	 */
	public IConfigurationElement getConfigurationElement() {
		return config;
	}

	/**
	 * Return the data, always a <code>Hashtable</code> or <code>String</code>,
	 * that was set when this builder was initialized.
	 *
	 * @see setInitializationData(IConfigurationElement, String, Object)
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Return the name of the child configuration element that named this
	 * builder in its class attribute.
	 *
	 * @see setInitializationData(IConfigurationElement, String, Object)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Logs the given plug-in lifecycle event for this builder's plugin.
	 */
	private void logPluginLifecycleEvent(String event) {
		actualEvents.add(event);
	}

	/**
	 * Resets expected and actual lifecycle events for this builder.
	 */
	public void reset() {
		expectedEvents.clear();
		actualEvents.clear();
		ruleCallBack = null;
	}

	/**
	 * Part of <code>IExecutableExtensionAdaptor</code> interface.
	 *
	 * @see IExecutableExtensionAdaptor
	 * @see IConfigurationElement#createExecutableExtension(String)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String name, Object data) {
		logPluginLifecycleEvent(SET_INITIALIZATION_DATA);
		this.config = config;
		this.name = name;
		this.data = data;
	}

	/**
	 * Implemented inherited method from <code>BaseBuilder</code>.
	 *
	 * @see BaseBuilder#startupOnInitialize
	 */
	@Override
	protected void startupOnInitialize() {
		logPluginLifecycleEvent(STARTUP_ON_INITIALIZE);
	}
}
