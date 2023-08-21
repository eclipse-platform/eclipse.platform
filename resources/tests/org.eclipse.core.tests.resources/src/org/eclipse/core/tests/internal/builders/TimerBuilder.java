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

import static org.eclipse.core.tests.resources.TestUtil.waitForCondition;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
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

	private static BuildExecutionState executionState = new BuildExecutionState(-1);

	private static class BuildExecutionState {
		private final int expectedNumberOfBuilds;
		private final List<BuildEvent> events = Collections.synchronizedList(new ArrayList<>());
		private volatile boolean shallAbort = false;
		private volatile int maxSimultaneousBuilds = 0;
		private volatile int currentlyRunningBuilds = 0;

		private BuildExecutionState(int expectedNumberOfBuilds) {
			this.expectedNumberOfBuilds = expectedNumberOfBuilds;
		}

		private synchronized boolean isExecuting() {
			return getProjectBuilds(BuildEventType.FINISH).size() < executionState.expectedNumberOfBuilds;
		}

		private synchronized List<IProject> getProjectBuilds(BuildEventType eventType) {
			return events.stream().filter(event -> event.eventType == eventType)
					.map(event -> event.project).toList();
		}

		private synchronized void startedExecutingProject(IProject project) {
			currentlyRunningBuilds++;
			maxSimultaneousBuilds = Math.max(currentlyRunningBuilds, maxSimultaneousBuilds);
			events.add(new BuildEvent(project, BuildEventType.START));
		}

		private synchronized void endedExcecutingProject(IProject project) {
			currentlyRunningBuilds--;
			events.add(new BuildEvent(project, BuildEventType.FINISH));
			notifyAll();
		}

		private synchronized void abortAndWaitForAllBuilds() {
			shallAbort = true;
			while (isExecuting()) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}

	}

	private enum BuildEventType {
		START, FINISH;
	}

	private static class BuildEvent {
		private final IProject project;

		private final BuildEventType eventType;

		public BuildEvent(IProject project, BuildEventType event) {
			this.project = project;
			this.eventType = event;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof BuildEvent otherEvent) {
				return project == otherEvent.project && eventType == otherEvent.eventType;
			}
			return false;
		}
	}

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
		assertNotEquals("no expected number of builds has been set", -1, executionState.expectedNumberOfBuilds);
		executionState.startedExecutingProject(getProject());
		try {
			int durationInMillis = Integer.parseInt(args.get(DURATION_ARG));
			waitForCondition(() -> executionState.shallAbort, durationInMillis);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		executionState.endedExcecutingProject(getProject());
		return new IProject[] {getProject()};
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

	public static List<IProject> getStartedProjectBuilds() {
		return executionState.getProjectBuilds(BuildEventType.START);
	}

	public static List<IProject> getFinishedProjectBuilds() {
		return executionState.getProjectBuilds(BuildEventType.FINISH);
	}

	public static int getMaximumNumberOfSimultaneousBuilds() {
		return executionState.maxSimultaneousBuilds;
	}

	public static Iterable<BuildEvent> getBuildEvents() {
		return new ArrayList<>(executionState.events);
	}

	/**
	 * Resets the tracked execution states. Asserts that no execution is still
	 * running.
	 */
	public static void setExpectedNumberOfBuilds(int expectedNumberOfBuilds) {
		assertFalse("builds are still running while resetting TimerBuilder", executionState.isExecuting());
		executionState = new BuildExecutionState(expectedNumberOfBuilds);
	}

	public static void abortCurrentBuilds() {
		executionState.abortAndWaitForAllBuilds();
	}

	public static BuildEvent createStartEvent(IProject project) {
		return new BuildEvent(project, BuildEventType.START);
	}

	public static BuildEvent createCompleteEvent(IProject project) {
		return new BuildEvent(project, BuildEventType.FINISH);
	}

}
