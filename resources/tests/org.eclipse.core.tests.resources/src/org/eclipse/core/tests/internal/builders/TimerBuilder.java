/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 *
 */
public class TimerBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.timerbuilder";
	public static final String DURATION_ARG = "duration";
	public static final String RULE_TYPE_ARG = "ruleType";

	private static int totalBuilds = 0;
	private static int currentBuilds = 0;
	private static int maxSimultaneousBuilds = 0;
	public static final List<Object> events = Collections.synchronizedList(new ArrayList<>());

	public static enum RuleType {
		NO_CONFLICT, CURRENT_PROJECT, WORKSPACE_ROOT, CURRENT_PROJECT_RELAXED;
	}

	final ISchedulingRule noConflictRule = new ISchedulingRule() {
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}
	};

	final ISchedulingRule relaxedProjetRule = new ISchedulingRule() {

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule || ResourcesPlugin.getWorkspace().getRoot().contains(rule);
		}
	};

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		synchronized (TimerBuilder.class) {
			totalBuilds++;
			currentBuilds++;
			maxSimultaneousBuilds = Math.max(currentBuilds, maxSimultaneousBuilds);
			events.add(buildStartEvent(getProject()));
		}
		int duration = 0;
		try {
			duration = Integer.parseInt(args.get(DURATION_ARG));
			Thread.sleep(duration);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		synchronized (TimerBuilder.class) {
			currentBuilds--;
			events.add(buildCompleteEvent(getProject()));
		}
		return new IProject[] {getProject()};
	}

	public static Object buildCompleteEvent(IProject project) {
		return "Compete " + project.getName();
	}

	public static Object buildStartEvent(IProject project) {
		return "Started " + project.getName();
	}

	@Override
	public ISchedulingRule getRule(int trigger, Map<String, String> args) {
		if (args != null) {
			RuleType ruleType = RuleType.valueOf(args.get(RULE_TYPE_ARG));
			switch (ruleType) {
				case NO_CONFLICT :
					return noConflictRule;
				case CURRENT_PROJECT :
					return getProject();
				case WORKSPACE_ROOT :
					return getProject().getWorkspace().getRoot();
				case CURRENT_PROJECT_RELAXED :
					return relaxedProjetRule;
			}
		}
		return noConflictRule;
	}

	public static int getTotalBuilds() {
		synchronized (TimerBuilder.class) {
			return totalBuilds;
		}
	}

	public static int getMaxSimultaneousBuilds() {
		synchronized (TimerBuilder.class) {
			return maxSimultaneousBuilds;
		}
	}

	public static void reset() {
		synchronized (TimerBuilder.class) {
			totalBuilds = 0;
			currentBuilds = 0;
			maxSimultaneousBuilds = 0;
			events.clear();
		}
	}
}
