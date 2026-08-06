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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class TimerBuilder extends IncrementalProjectBuilder {
	public static final String BUILDER_NAME = "org.eclipse.core.tests.resources.timerbuilder";
	public static final String DURATION_ARG = "duration";
	public static final String RULE_TYPE_ARG = "ruleType";

	private static final int SHUTDOWN_TIMEOUT_IN_MILLIS = 10_000;

	private static volatile BuildExecutionState executionState = new BuildExecutionState(-1);

	/**
	 * Tracks the builds of a single test. All state is guarded by the instance
	 * monitor, which is also used to signal running builds to abort.
	 */
	private static class BuildExecutionState {
		private final int expectedNumberOfBuilds;
		private final List<BuildEvent> events = new ArrayList<>();
		private boolean shallAbort;
		private int maxSimultaneousBuilds;
		private int currentlyRunningBuilds;

		private BuildExecutionState(int expectedNumberOfBuilds) {
			this.expectedNumberOfBuilds = expectedNumberOfBuilds;
		}

		private synchronized boolean hasRunningBuilds() {
			return currentlyRunningBuilds > 0;
		}

		private synchronized int getMaxSimultaneousBuilds() {
			return maxSimultaneousBuilds;
		}

		private synchronized List<BuildEvent> getEvents() {
			return new ArrayList<>(events);
		}

		private synchronized List<IProject> getProjectBuilds(BuildEventType eventType) {
			return events.stream().filter(event -> event.eventType == eventType).map(event -> event.project).toList();
		}

		private synchronized void startedExecutingProject(IProject project) {
			currentlyRunningBuilds++;
			maxSimultaneousBuilds = Math.max(currentlyRunningBuilds, maxSimultaneousBuilds);
			events.add(new BuildEvent(project, BuildEventType.START));
		}

		private synchronized void endedExecutingProject(IProject project) {
			currentlyRunningBuilds--;
			events.add(new BuildEvent(project, BuildEventType.FINISH));
			notifyAll();
		}

		/**
		 * Blocks until the builds shall abort or the given duration has elapsed.
		 */
		private synchronized void awaitBuildDuration(long durationInMillis) {
			awaitWithTimeout(durationInMillis, () -> shallAbort);
		}

		/**
		 * Requests all running builds to abort and waits for their termination.
		 */
		private synchronized void abortAndWaitForAllBuilds() {
			shallAbort = true;
			notifyAll();
			awaitWithTimeout(SHUTDOWN_TIMEOUT_IN_MILLIS, () -> currentlyRunningBuilds == 0);
		}

		private synchronized void awaitWithTimeout(long timeoutInMillis, BooleanSupplier condition) {
			long deadlineInNanos = System.nanoTime() + timeoutInMillis * 1_000_000L;
			long remainingMillis = timeoutInMillis;
			while (!condition.getAsBoolean() && remainingMillis > 0) {
				try {
					wait(remainingMillis);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				// Round up, so that the requested duration is never undercut
				remainingMillis = (deadlineInNanos - System.nanoTime() + 999_999) / 1_000_000L;
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
		// Bind to the state of the current test, so that a leaked build does not
		// contribute events to a subsequent test
		BuildExecutionState state = executionState;
		assertNotEquals(-1, state.expectedNumberOfBuilds, "no expected number of builds has been set");
		state.startedExecutingProject(getProject());
		try {
			state.awaitBuildDuration(Integer.parseInt(args.get(DURATION_ARG)));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			state.endedExecutingProject(getProject());
		}
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
		return executionState.getMaxSimultaneousBuilds();
	}

	public static Iterable<BuildEvent> getBuildEvents() {
		return executionState.getEvents();
	}

	/**
	 * Resets the tracked execution states and defines the number of builds expected
	 * to be executed. Asserts that no execution is still running.
	 */
	public static void setExpectedNumberOfBuilds(int expectedNumberOfBuilds) {
		assertFalse(replaceExecutionState(expectedNumberOfBuilds),
				"builds are still running while resetting TimerBuilder");
	}

	/**
	 * Resets the tracked execution states and returns whether builds were still
	 * running.
	 */
	public static boolean reset() {
		return replaceExecutionState(-1);
	}

	private static boolean replaceExecutionState(int expectedNumberOfBuilds) {
		BuildExecutionState previousState = executionState;
		// Replace the state before checking it, so that a leaked build does not make
		// all subsequent tests fail as well
		executionState = new BuildExecutionState(expectedNumberOfBuilds);
		return previousState.hasRunningBuilds();
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
