/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Isaac Pacht (isaacp3@gmail.com) - fix for bug 206540
 *     Anton Leherbauer (Wind River) - [305858] Allow Builder to return null rule
 *     James Blackburn (Broadcom) - [306822] Provide Context for Builder getRule()
 *     Broadcom Corporation - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Torbjörn Svensson (STMicroelectronics) - bug #552606
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class BuildManager implements ICoreConstants, IManager, ILifecycleListener {

	private static final String BUILDER_INIT = "BuilderInitInfo"; //$NON-NLS-1$

	/**
	 * Cache used to optimize the common case of an autobuild against
	 * a workspace where only a single project has changed (and hence
	 * only a single delta is interesting).
	 */
	static class DeltaCache<E> {
		private final Map<IPath, E> deltas = new HashMap<>();
		private ElementTree newTree;
		private ElementTree oldTree;

		public void flush() {
			deltas.clear();
			this.oldTree = null;
			this.newTree = null;
		}

		/**
		 * Returns the cached resource delta for the given project and trees, or
		 * calls calculator to compute a new delta if there is no matching one in the cache.
		 */
		public E computeIfAbsent(IPath project, ElementTree anOldTree, ElementTree aNewTree, Supplier<E> calculator) {
			if (!(areEqual(this.oldTree, anOldTree) && areEqual(this.newTree, aNewTree))) {
				this.oldTree = anOldTree;
				this.newTree = aNewTree;
				deltas.clear();
			}
			return deltas.computeIfAbsent(project, p -> calculator.get());
		}

		private static boolean areEqual(ElementTree cached, ElementTree requested) {
			return !ElementTree.hasChanges(requested, cached, ResourceComparator.getBuildComparator(), true);
		}
	}

	/**
	 * These builders are added to build tables in place of builders that couldn't be instantiated
	 */
	class MissingBuilder extends IncrementalProjectBuilder {
		private boolean hasBeenBuilt = false;
		private String name;

		MissingBuilder(String name) {
			this.name = name;
		}

		/**
		 * Log an exception on the first build, and silently do nothing on subsequent builds.
		 */
		@Override
		protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) {
			if (!hasBeenBuilt && Policy.DEBUG_BUILD_FAILURE) {
				hasBeenBuilt = true;
				String msg = NLS.bind(Messages.events_skippingBuilder, name, getProject().getName());
				Policy.log(IStatus.WARNING, msg, null);
			}
			return null;
		}

		String getName() {
			return name;
		}

		@Override
		public ISchedulingRule getRule(int kind, Map<String, String> args) {
			return null;
		}

	}

	private static final int TOTAL_BUILD_WORK = Policy.totalWork * 1000;

	//the job for performing background autobuild
	final AutoBuildJob autoBuildJob;
	private final Set<IProject> builtProjects = Collections.synchronizedSet(new HashSet<>());

	//the following four fields only apply for the lifetime of a single builder invocation.
	protected final Set<InternalBuilder> currentBuilders;
	private ElementTree currentLastBuiltTree;
	private ElementTree currentTree;

	/**
	 * Caches the IResourceDelta for a pair of trees
	 */
	final private DeltaCache<IResourceDelta> deltaCache = new DeltaCache<>();

	private ILock lock;

	/**
	 * {@code true} if we can exit inner build loop cycle early after
	 * {@link #requestRebuild()} is set by one build config and before following
	 * build configs are executed. Default is {@code false}.
	 */
	private boolean earlyExitFromBuildLoopAllowed;

	/**
	 * Used for the build cycle looping mechanism. If true, build loop over multiple
	 * projects will be restarted again for all projects in the loop
	 */
	private boolean rebuildRequested;

	/**
	 * Set of projects for which builders requested rebuild. Has no effect if any
	 * builder requested rebuild of everything via {@link #rebuildRequested}
	 */
	private final Set<IProject> projectsToRebuild;

	/**
	 * Map of projects for which builders requested rebuild for the current build
	 * cycle. If the value is "true" - stop building project with other builders
	 * immediately, "false" to continue build and start project build again after
	 * all builders were done. If no value is set, no rebuild is requested.
	 */
	private final Map<IProject, Boolean> restartBuildImmediately;

	// Shows if we are in the parallel build loop or not
	boolean parallelBuild;

	private final Bundle systemBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$

	// protects against concurrent access of session stored builders during builder initialization
	private Object builderInitializationLock = new Object();

	//used for debug/trace timing
	private long timeStamp = -1;
	private long overallTimeStamp = -1;
	private Workspace workspace;

	public BuildManager(Workspace workspace, ILock workspaceLock) {
		this.workspace = workspace;
		this.currentBuilders = Collections.synchronizedSet(new HashSet<>());
		this.autoBuildJob = new AutoBuildJob(workspace);
		projectsToRebuild = ConcurrentHashMap.newKeySet();
		restartBuildImmediately = new ConcurrentHashMap<>();
		this.lock = workspaceLock;
		InternalBuilder.buildManager = this;
		setEarlyExitFromBuildLoopAllowed(
				Boolean.getBoolean("org.eclipse.core.resources.allowEarlyBuildLoopExit")); //$NON-NLS-1$ );
	}

	private void basicBuild(int trigger, IncrementalProjectBuilder builder, Map<String, String> args, MultiStatus status, IProgressMonitor monitor) {
		InternalBuilder currentBuilder = builder; // downcast to make package methods visible
		try {
			currentBuilders.add(currentBuilder);
			//clear any old requests to forget built state
			currentBuilder.clearLastBuiltStateRequests();
			// Figure out want kind of build is needed
			boolean clean = trigger == IncrementalProjectBuilder.CLEAN_BUILD;
			currentLastBuiltTree = currentBuilder.getLastBuiltTree();

			// Does the build command respond to this trigger?
			boolean isBuilding = builder.getCommand().isBuilding(trigger);

			// If no tree is available we have to do a full build
			if (!clean && currentLastBuiltTree == null) {
				// Bug 306746 - Don't promote build to FULL_BUILD if builder doesn't AUTO_BUILD
				if (trigger == IncrementalProjectBuilder.AUTO_BUILD && !isBuilding)
					return;
				// Without a build tree the build is promoted to FULL_BUILD
				trigger = IncrementalProjectBuilder.FULL_BUILD;
				isBuilding = isBuilding || builder.getCommand().isBuilding(trigger);
			}

			//don't build if this builder doesn't respond to the trigger
			if (!isBuilding) {
				if (clean)
					currentBuilder.setLastBuiltTree(null);
				return;
			}

			// For incremental builds, grab a pointer to the current state before computing the delta
			currentTree = ((trigger == IncrementalProjectBuilder.FULL_BUILD) || clean) ? null : workspace.getElementTree();
			int depth = -1;
			ISchedulingRule rule = null;
			try {
				//short-circuit if none of the projects this builder cares about have changed.
				if (!needsBuild(currentBuilder, trigger)) {
					//use up the progress allocated for this builder
					monitor.beginTask("", 1); //$NON-NLS-1$
					monitor.done();
					return;
				}
				rule = builder.getRule(trigger, args);
				String name = currentBuilder.getLabel();
				String message;
				if (name != null) {
					message = NLS.bind(Messages.events_invoking_2, name, builder.getProject().getFullPath());
				} else {
					message = NLS.bind(Messages.events_invoking_1, builder.getProject().getFullPath());
				}
				monitor.subTask(message);
				hookStartBuild(builder, trigger);
				// Make the current tree immutable before releasing the WS lock
				if (rule != null && currentTree != null) {
					workspace.newWorkingTree();
				}
				//release workspace lock while calling builders
				depth = getWorkManager().beginUnprotected();
				// Acquire the rule required for running this builder
				if (rule != null) {
					Job.getJobManager().beginRule(rule, monitor);
					// Now that we've acquired the rule, changes may have been made concurrently, ensure we're pointing at the
					// correct currentTree so delta contains concurrent changes made in areas guarded by the scheduling rule
					if (currentTree != null)
						currentTree = workspace.getElementTree();
				}
				//do the build
				SafeRunner.run(getSafeRunnable(currentBuilder, trigger, args, status, monitor));
			} finally {
				// Re-acquire the WS lock, then release the scheduling rule
				if (depth >= 0) {
					getWorkManager().endUnprotected(depth);
				}
				if (rule != null) {
					Job.getJobManager().endRule(rule);
				}
				// Be sure to clean up after ourselves.
				if (clean || currentBuilder.wasForgetStateRequested()) {
					currentBuilder.setLastBuiltTree(null);
				} else if (currentBuilder.wasRememberStateRequested()) {
					// If remember last build state, and FULL_BUILD
					// last tree must be set to => null for next build
					if (trigger == IncrementalProjectBuilder.FULL_BUILD) {
						currentBuilder.setLastBuiltTree(null);
					}
					// else don't modify the last built tree
				} else {
					// remember the current state as the last built state.
					ElementTree lastTree = workspace.getElementTree();
					lastTree.immutable();
					currentBuilder.setLastBuiltTree(lastTree);
				}
				hookEndBuild(builder);
			}
		} finally {
			currentBuilders.remove(currentBuilder);
			currentTree = null;
			currentLastBuiltTree = null;
		}
	}

	protected void basicBuild(IBuildConfiguration buildConfiguration, final int trigger, IBuildContext context,
			ICommand[] commands, MultiStatus status, IProgressMonitor monitor) {
		int remainingIterations = Math.max(1, workspace.getDescription().getMaxBuildIterations());

		// Planned triggers for each builder are originally all same
		// but may change if the rebuild is requested in the loop
		int[] triggers = new int[commands.length];
		Arrays.fill(triggers, trigger);
		boolean shouldRebuild = true;
		try {
			while (shouldRebuild) {
				shouldRebuild = false;
				// If rebuild was requested, the triggers for next builder executions
				// will be changed to incremental.
				int[] nextTriggers = null;
				for (int i = 0; i < commands.length; i++) {
					int currentTrigger = triggers[i];
					checkCanceled(currentTrigger, monitor);
					BuildCommand command = (BuildCommand) commands[i];
					IProgressMonitor sub = Policy.subMonitorFor(monitor, 1);
					IncrementalProjectBuilder builder = getBuilder(buildConfiguration, command, i, status, context);
					if (builder != null) {
						basicBuild(currentTrigger, builder, command.getArguments(false), status, sub);

						// Check if the builder requested rebuild
						IProject project = builder.getProject();
						Boolean restartImmediately = restartBuildImmediately.remove(project);
						if (restartImmediately != null) {
							remainingIterations--;
							if (remainingIterations > 0) {
								if (!restartImmediately) {
									// process building all builders and restart after that
									shouldRebuild = true;
									// Next build rounds for all builders should be incremental
									if (trigger != IncrementalProjectBuilder.AUTO_BUILD) {
										nextTriggers = new int[triggers.length];
										Arrays.fill(nextTriggers, IncrementalProjectBuilder.INCREMENTAL_BUILD);
									}
								} else {
									// First builder doesn't need to restart anything
									if (i > 0) {
										// Start for loop again, input can be important for all builders before
										shouldRebuild = true;
										// Next build rounds for previous builders up to current should be incremental
										if (trigger != IncrementalProjectBuilder.AUTO_BUILD) {
											nextTriggers = Arrays.copyOf(triggers, triggers.length);
											Arrays.fill(nextTriggers, 0, i + 1,
													IncrementalProjectBuilder.INCREMENTAL_BUILD);
										}
										break;
									}
								}
							}
						}
					}
				}
				if (nextTriggers != null) {
					triggers = nextTriggers;
				}
			}
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/**
	 * Runs all builders on the given project config.
	 * @return A status indicating if the build succeeded or failed
	 */
	private IStatus basicBuild(IBuildConfiguration buildConfiguration, int trigger, IBuildContext context, IProgressMonitor monitor) {
		try {
			hookStartBuild(new IBuildConfiguration[] {buildConfiguration}, trigger);
			MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, Messages.events_errors, null);
			basicBuild(buildConfiguration, trigger, context, status, monitor);
			return status;
		} finally {
			hookEndBuild(trigger);
		}
	}

	private void basicBuild(final IBuildConfiguration buildConfiguration, final int trigger, final IBuildContext context, final MultiStatus status, final IProgressMonitor monitor) {
		try {
			final IProject project = buildConfiguration.getProject();
			final ICommand[] commands;
			if (project.isAccessible())
				commands = ((Project) project).internalGetDescription().getBuildSpec(false);
			else
				commands = null;
			int work = commands == null ? 0 : commands.length;
			monitor.beginTask(NLS.bind(Messages.events_building_1, project.getFullPath()), work);
			if (work == 0)
				return;
			ISafeRunnable code = new ISafeRunnable() {
				@Override
				public void handleException(Throwable e) {
					if (e instanceof OperationCanceledException) {
						if (Policy.DEBUG_BUILD_INVOKING)
							Policy.debug("Build canceled"); //$NON-NLS-1$
						throw (OperationCanceledException) e;
					}
					// don't log the exception....it is already being logged in Workspace#run
					// should never get here because the lower-level build code wrappers
					// builder exceptions in core exceptions if required.
					String errorText = e.getMessage();
					if (errorText == null)
						errorText = NLS.bind(Messages.events_unknown, e.getClass().getName(), project.getName());
					status.add(new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, errorText, e));
				}

				@Override
				public void run() throws Exception {
					basicBuild(buildConfiguration, trigger, context, commands, status, monitor);
				}
			};
			SafeRunner.run(code);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Runs the builder with the given name on the given project config.
	 * @return A status indicating if the build succeeded or failed
	 */
	private IStatus basicBuild(IBuildConfiguration buildConfiguration, int trigger, String builderName, Map<String, String> args, IProgressMonitor monitor) {
		final IProject project = buildConfiguration.getProject();
		monitor = Policy.monitorFor(monitor);
		try {
			String message = NLS.bind(Messages.events_building_1, project.getFullPath());
			monitor.beginTask(message, 1);
			try {
				hookStartBuild(new IBuildConfiguration[] {buildConfiguration}, trigger);
				MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, Messages.events_errors, null);
				ICommand command = getCommand(project, builderName, args);
				try {
					IBuildContext context = new BuildContext(buildConfiguration);
					IncrementalProjectBuilder builder = getBuilder(buildConfiguration, command, -1, status, context);
					if (builder != null)
						basicBuild(trigger, builder, args, status, Policy.subMonitorFor(monitor, 1));
				} catch (CoreException e) {
					status.add(e.getStatus());
				}
				return status;
			} finally {
				hookEndBuild(trigger);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Loop the workspace build until no more builders request a rebuild.
	 */
	private void basicBuildLoop(IBuildConfiguration[] configs, IBuildConfiguration[] requestedConfigs, int trigger, MultiStatus status, IProgressMonitor monitor) {
		int projectWork = configs.length > 0 ? TOTAL_BUILD_WORK / configs.length : 0;
		int maxIterations = workspace.getDescription().getMaxBuildIterations();
		// Scale allowed iterations count depending on affected projects -
		// allow at least two build cycles per project
		maxIterations = Math.max(configs.length * 2, maxIterations);
		if (maxIterations <= 0) {
			maxIterations = 1;
		}

		rebuildRequested = true;
		boolean rebuildSomething = true;
		for (int iter = 0; rebuildSomething && iter < maxIterations; iter++) {
			// Used for compatibility reason with requestRebuild()
			boolean rebuildAll = rebuildRequested;
			final boolean lastIteration = iter == maxIterations - 1;

			if (rebuildAll) {
				// default build loop
				basicBuildLoop(configs, requestedConfigs, trigger, status, monitor, projectWork, lastIteration);
			} else {
				// rebuild only projects requested by builders during previous build cycle
				List<IBuildConfiguration> allConfigs = Arrays.asList(workspace.getBuildOrder());
				IBuildConfiguration[] configurations = allConfigs.stream()
						.filter(c -> projectsToRebuild.contains(c.getProject())).toArray(IBuildConfiguration[]::new);
				basicBuildLoop(configurations, requestedConfigs, trigger, status, monitor, projectWork, lastIteration);
			}
			if (rebuildRequested) {
				rebuildSomething = true;
				projectsToRebuild.clear();
				restartBuildImmediately.clear();
			} else if (!projectsToRebuild.isEmpty()) {
				rebuildSomething = true;
			} else {
				rebuildSomething = false;
			}

			// subsequent builds should always be incremental
			// i.e. autobuild if not requested by user
			// INCREMENTAL_BUILD would not be auto interrupted by user actions
			if (trigger != IncrementalProjectBuilder.AUTO_BUILD) {
				trigger = IncrementalProjectBuilder.INCREMENTAL_BUILD;
			}
		}
	}

	private void basicBuildLoop(IBuildConfiguration[] configs, IBuildConfiguration[] requestedConfigs, int trigger,
			MultiStatus status, IProgressMonitor monitor, int projectWork, final boolean lastIteration) {

		// If we are rebuilding anything, we can clear already build projects.
		// If we build only few dedicated projects, all others, already built
		// projects should be added again to the "built" list, otherwise
		// hasBeenBuilt() will return "false" for them and they would not considered
		// for a rebuild if requested by one of the projects to be re-built now.
		if (rebuildRequested) {
			builtProjects.clear();
		} else {
			builtProjects.removeAll(projectsToRebuild);
		}

		// Clear all the rebuild related flags before entering new build cycle
		rebuildRequested = false;
		projectsToRebuild.clear();
		restartBuildImmediately.clear();

		// Basic loop over projects
		for (IBuildConfiguration config : configs) {
			if (config.getProject().isAccessible()) {
				IBuildContext context = new BuildContext(config, requestedConfigs, configs);

				// Inner loop over builders in one project
				basicBuild(config, trigger, context, status, Policy.subMonitorFor(monitor, projectWork));
				builtProjects.add(config.getProject());

				// Check if we should continue with other projects
				if ((rebuildRequested || !projectsToRebuild.isEmpty())
						&& isEarlyExitFromBuildLoopAllowed()) {
					if (lastIteration) {
						// run build for all projects at least once
						continue;
					}
					// Don't build following projects if one of the predecessors
					// requested rebuild anyway, just start main loop from scratch
					break;
				}
			}
		}
	}

	/**
	 * Runs all builders on all the given project configs, in the order that
	 * they are given.
	 * @return A status indicating if the build succeeded or failed
	 */
	public IStatus build(IBuildConfiguration[] configs, IBuildConfiguration[] requestedConfigs, int trigger, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.events_building_0, TOTAL_BUILD_WORK);
			try {
				hookStartBuild(configs, trigger);
				MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.BUILD_FAILED, Messages.events_errors, null);
				basicBuildLoop(configs, requestedConfigs, trigger, status, monitor);
				return status;
			} finally {
				hookEndBuild(trigger);
			}
		} finally {
			endBuild(trigger, monitor);
		}
	}

	/**
	 * Runs all builders on all the given project configs, in the order that
	 * they are given.
	 * @return A status indicating if the build succeeded or failed
	 */
	public IStatus buildParallel(Digraph<IBuildConfiguration> configs, IBuildConfiguration[] requestedConfigs, int trigger, JobGroup buildJobGroup, IProgressMonitor monitor) {
		parallelBuild = true;
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(Messages.events_building_0, TOTAL_BUILD_WORK);
			try {
				builtProjects.clear();
				hookStartBuild(configs.vertexList.stream().map(vertex -> vertex.id).toArray(IBuildConfiguration[]::new), trigger);
				MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.BUILD_FAILED, Messages.events_errors, null);
				parallelBuildLoop(configs, requestedConfigs, trigger, buildJobGroup, status, monitor);
				return status;
			} finally {
				hookEndBuild(trigger);
			}
		} finally {
			endBuild(trigger, monitor);
			parallelBuild = false;
		}
	}

	private void endBuild(int trigger, IProgressMonitor monitor) {
		boolean cancelledBuild = monitor.isCanceled();
		monitor.done();
		if (trigger == IncrementalProjectBuilder.INCREMENTAL_BUILD || trigger == IncrementalProjectBuilder.FULL_BUILD) {
			autoBuildJob.avoidBuild();
		} else if (cancelledBuild) {
			// Bug 538789: if a build was explicitly cancelled, don't trigger auto-build jobs until a build is requested
			autoBuildJob.avoidBuildIfNotInterrupted();
		}
	}

	private void parallelBuildLoop(final Digraph<IBuildConfiguration> configs, IBuildConfiguration[] requestedConfigs, int trigger, JobGroup buildJobGroup, MultiStatus status, IProgressMonitor monitor) {
		final int projectWork = configs.vertexList.size() > 0 ? TOTAL_BUILD_WORK / configs.vertexList.size() : 0;
		builtProjects.clear();
		final GraphProcessor<IBuildConfiguration> graphProcessor = new GraphProcessor<>(configs, IBuildConfiguration.class, (config, graphCrawler) -> {
			IBuildContext context = new BuildContext(config, requestedConfigs, graphCrawler.getSequentialOrder()); // TODO consider passing Digraph to BuildConfig?
			try {
				workspace.prepareOperation(null, monitor);
				workspace.beginOperation(false);
				basicBuild(config, trigger, context, status, Policy.subMonitorFor(monitor, projectWork));
				workspace.endOperation(null, false);
				builtProjects.add(config.getProject());
			} catch (CoreException ex) {
				status.add(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, ex.getMessage(), ex));
			}
		}, config -> getRule(config, trigger, null, Collections.emptyMap()), buildJobGroup);
		graphProcessor.processGraphWithParallelJobs();
		try {
			Job.getJobManager().join(graphProcessor, monitor);
		} catch (OperationCanceledException | InterruptedException e) {
			status.add(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, e.getMessage(), e));
		}
	}

	/**
	 * Runs the builder with the given name on the given project config.
	 * @return A status indicating if the build succeeded or failed
	 */
	public IStatus build(IBuildConfiguration buildConfiguration, int trigger, String builderName, Map<String, String> args, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		rebuildRequested = false;
		projectsToRebuild.clear();
		restartBuildImmediately.clear();
		if (builderName == null) {
			IBuildContext context = new BuildContext(buildConfiguration);
			return basicBuild(buildConfiguration, trigger, context, monitor);
		}
		return basicBuild(buildConfiguration, trigger, builderName, args, monitor);
	}

	/**
	 * Cancel the build if the user has canceled or if an auto-build has been interrupted.
	 */
	private void checkCanceled(int trigger, IProgressMonitor monitor) {
		//if the system is shutting down, don't build
		if (systemBundle.getState() == Bundle.STOPPING)
			throw new OperationCanceledException();
		Policy.checkCanceled(monitor);
		//check for auto-cancel only if we are auto-building
		if (trigger != IncrementalProjectBuilder.AUTO_BUILD)
			return;
		//check for request to interrupt the auto-build
		if (autoBuildJob.isInterrupted())
			throw new OperationCanceledException();
	}

	/**
	 * Creates and returns an ArrayList of BuilderPersistentInfo.
	 * The list includes entries for all builders for all configs that are
	 * in the builder spec, and that have a last built state, even if they
	 * have not been instantiated this session.
	 *
	 * e.g.
	 * For a project with 3 builders, 2 build configurations and the second
	 * builder doesn't support configurations.
	 * The returned List of BuilderInfos is ordered:
	 * builder_id, config_name,builder_index
	 * builder_1,  config_1, 1
	 * builder_1,  config_2, 1
	 * builder_2,  null,     2
	 * builder_3,  config_1, 3
	 * builder_3,  config_1, 3
	 *
	 */
	public ArrayList<BuilderPersistentInfo> createBuildersPersistentInfo(IProject project) throws CoreException {
		/* get the old builders (those not yet instantiated) */
		ArrayList<BuilderPersistentInfo> oldInfos = getBuildersPersistentInfo(project);

		ProjectDescription desc = ((Project) project).internalGetDescription();
		ICommand[] commands = desc.getBuildSpec(false);
		if (commands.length == 0)
			return null;
		IBuildConfiguration[] configs = project.getBuildConfigs();

		/* build the new list */
		ArrayList<BuilderPersistentInfo> newInfos = new ArrayList<>(commands.length * configs.length);
		for (int i = 0; i < commands.length; i++) {
			BuildCommand command = (BuildCommand) commands[i];
			String builderName = command.getBuilderName();

			// If the builder doesn't support configurations, only 1 delta tree to persist
			boolean supportsConfigs = command.supportsConfigs();
			int numberConfigs = supportsConfigs ? configs.length : 1;

			for (int j = 0; j < numberConfigs; j++) {
				IBuildConfiguration config = configs[j];
				BuilderPersistentInfo info = null;
				IncrementalProjectBuilder builder = ((BuildCommand) commands[i]).getBuilder(config);
				if (builder == null) {
					// if the builder was not instantiated, use the old info if any.
					if (oldInfos != null)
						info = getBuilderInfo(oldInfos, builderName, supportsConfigs ? config.getName() : null, i);
				} else if (!(builder instanceof MissingBuilder)) {
					ElementTree oldTree = ((InternalBuilder) builder).getLastBuiltTree();
					//don't persist build state for builders that have no last built state
					if (oldTree != null) {
						// if the builder was instantiated, construct a memento with the important info
						info = new BuilderPersistentInfo(project.getName(), supportsConfigs ? config.getName() : null, builderName, i);
						info.setLastBuildTree(oldTree);
						info.setInterestingProjects(((InternalBuilder) builder).getInterestingProjects());
					}
				}
				if (info != null)
					newInfos.add(info);
			}
		}
		return newInfos;
	}

	private String debugBuilder() {
		return currentBuilders == null ? "<no builder>" : currentBuilders.getClass().getName(); //$NON-NLS-1$
	}

	private String debugProject() {
		if (currentBuilders == null)
			return "<no project>"; //$NON-NLS-1$
		return "[" + currentBuilders.stream().map(builder -> builder.getProject().getFullPath().toString()).collect(Collectors.joining(",")) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Returns a string representation of a build trigger for debugging purposes.
	 * @param trigger The trigger to compute a representation of
	 * @return A string describing the trigger.
	 */
	private String debugTrigger(int trigger) {
		switch (trigger) {
			case IncrementalProjectBuilder.FULL_BUILD :
				return "FULL_BUILD"; //$NON-NLS-1$
			case IncrementalProjectBuilder.CLEAN_BUILD :
				return "CLEAN_BUILD"; //$NON-NLS-1$
			case IncrementalProjectBuilder.AUTO_BUILD :
			case IncrementalProjectBuilder.INCREMENTAL_BUILD :
			default :
				return "INCREMENTAL_BUILD"; //$NON-NLS-1$
		}
	}

	/**
	 * The outermost workspace operation has finished.  Do an autobuild if necessary.
	 */
	public void endTopLevel(boolean needsBuild) {
		autoBuildJob.build(needsBuild);
	}

	/**
	 * Waits till autobuild finished. Tries to finish it as soon as possible.
	 */
	public void waitForAutoBuild() {
		waitFor(autoBuildJob);
	}

	/**
	 * Waits till noBuildJob finished. Tries to finish it as soon as possible.
	 */
	public void waitForAutoBuildOff() {
		waitFor(autoBuildJob.noBuildJob);
	}

	private static void waitFor(Job job) {
		// Need to loop because jobs can reschedule itself and concurrent running
		// background jobs change the states too
		while (!(job.getState() == Job.NONE)) {
			// Need to wake up thread to finish as soon as possible:
			while (!(job.getState() == Job.RUNNING || job.getState() == Job.NONE)) {
				Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
				Thread.yield();
				// After wakeup the woken job may go into sleep again when asynchronous
				// workspace save interrupts autobuild so we need to wait till RUNNING or NONE
				// (happens after each JUnit class)
			}
			// Need to wait till job finished:
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			} catch (OperationCanceledException | InterruptedException e) {
				// Ignore
			}
		}
	}

	/**
	 * Returns the value of the boolean configuration element attribute with the
	 * given name, or <code>false</code> if the attribute is missing.
	 */
	private boolean getBooleanAttribute(IConfigurationElement element, String name) {
		String valueString = element.getAttribute(name);
		return valueString != null && valueString.equalsIgnoreCase(Boolean.TRUE.toString());
	}

	/**
	 * 	Returns the builder instance corresponding to the given command, or
	 * <code>null</code> if the builder was not valid.
	 * @param buildConfiguration The project config this builder corresponds to
	 * @param command The build command
	 * @param buildSpecIndex The index of this builder in the build spec, or -1 if
	 * the index is unknown
	 * @param status MultiStatus for collecting errors
	 */
	private IncrementalProjectBuilder getBuilder(IBuildConfiguration buildConfiguration, ICommand command, int buildSpecIndex, MultiStatus status) throws CoreException {
		BuildCommand buildCommand = (BuildCommand) command;
		InternalBuilder result = buildCommand.getBuilder(buildConfiguration);
		String builderName = command.getBuilderName();
		IProject project = buildConfiguration.getProject();

		if (result == null) {
			// Synchronized builderInitializationLock blocks below are used to avoid
			// locking during initializeBuilder() call and to make sure two threads
			// trying to init the same builder in parallel will get properly
			// initialized builder without deadlocks or ConcurrentModificationException
			// See bug 538102 and bug 517411.
			BuilderPersistentInfo info;
			synchronized (builderInitializationLock) {
				// get the map of builders to get the last built tree
				BuilderPersistentInfo builderInitInProgress = getBuilderInitInfo(project, builderName);
				if (builderInitInProgress != null) {
					info = builderInitInProgress;
				} else {
					info = removePersistentBuilderInfo(builderName, buildConfiguration, buildSpecIndex);
					setBuilderInitInfo(project, builderName, info);
				}
			}

			result = buildCommand.getBuilder(buildConfiguration);
			if (result == null) {
				// Not synchronized on builderInitializationLock to avoid deadlocks if the builder init code
				// requests a lock held by another thread which may be waiting on builderInitializationLock
				result = initializeBuilder(command, builderName, buildConfiguration, info, status);
			}

			synchronized (builderInitializationLock) {
				// the build command holds only one builder per configuration
				// so query the builder for the configuration once more,
				// in case another builder was added since we last checked
				InternalBuilder other = buildCommand.getBuilder(buildConfiguration);
				if (other == null) {
					buildCommand.addBuilder(buildConfiguration, (IncrementalProjectBuilder) result);
				} else {
					result = other;
				}
				setBuilderInitInfo(project, builderName, null);
			}
		}

		// Ensure the build configuration stays fresh for non-config aware builders
		result.setBuildConfig(buildConfiguration);
		if (!validateNature(result, builderName)) {
			//skip this builder and null its last built tree because it is invalid
			//if the nature gets added or re-enabled a full build will be triggered
			result.setLastBuiltTree(null);
			return null;
		}
		return (IncrementalProjectBuilder) result;
	}

	/**
	 * Returns the builder instance corresponding to the given command, or
	 * <code>null</code> if the builder was not valid, and sets its context
	 * to the one supplied.
	 *
	 * @param buildConfiguration The project config this builder corresponds to
	 * @param command The build command
	 * @param buildSpecIndex The index of this builder in the build spec, or -1 if
	 * the index is unknown
	 * @param status MultiStatus for collecting errors
	 */
	private IncrementalProjectBuilder getBuilder(IBuildConfiguration buildConfiguration, ICommand command, int buildSpecIndex, MultiStatus status, IBuildContext context) throws CoreException {
		InternalBuilder builder = getBuilder(buildConfiguration, command, buildSpecIndex, status);
		if (builder != null)
			builder.setContext(context);
		return (IncrementalProjectBuilder) builder;
	}

	/**
	 * Removes the builder persistent info from the map corresponding to the
	 * given builder name, configuration name and build spec index, or <code>null</code> if not found
	 *
	 * @param configName or null if the builder doesn't support configurations
	 * @param buildSpecIndex The index in the build spec, or -1 if unknown
	 */
	private BuilderPersistentInfo getBuilderInfo(ArrayList<BuilderPersistentInfo> infos, String builderName, String configName, int buildSpecIndex) {
		//try to match on builder index, but if not match is found, use the builder name and config name
		//this is because older workspace versions did not store builder infos in build spec order
		BuilderPersistentInfo nameMatch = null;
		for (BuilderPersistentInfo info : infos) {
			// match on name, config name and build spec index if known
			// Note: the config name may be null for builders that don't support configurations, or old workspaces
			if (info.getBuilderName().equals(builderName) && (info.getConfigName() == null || info.getConfigName().equals(configName))) {
				//we have found a match on name alone
				if (nameMatch == null)
					nameMatch = info;
				//see if the index matches
				if (buildSpecIndex == -1 || info.getBuildSpecIndex() == -1 || buildSpecIndex == info.getBuildSpecIndex())
					return info;
			}
		}
		//no exact index match, so return name match, if any
		return nameMatch;
	}

	/**
	 * Returns a list of BuilderPersistentInfo.
	 * The list includes entries for all builders that are in the builder spec,
	 * and that have a last built state but have not been instantiated this session.
	 */
	@SuppressWarnings({"unchecked"})
	public ArrayList<BuilderPersistentInfo> getBuildersPersistentInfo(IProject project) throws CoreException {
		return (ArrayList<BuilderPersistentInfo>) project.getSessionProperty(K_BUILD_LIST);
	}

	/**
	 * Returns a build command for the given builder name and project.
	 * First looks for matching command in the project's build spec. If none
	 * is found, a new command is created and returned. This is necessary
	 * because IProject.build allows a builder to be executed that is not in the
	 * build spec.
	 */
	private ICommand getCommand(IProject project, String builderName, Map<String, String> args) {
		ICommand[] buildSpec = ((Project) project).internalGetDescription().getBuildSpec(false);
		for (ICommand element : buildSpec)
			if (element.getBuilderName().equals(builderName))
				return element;
		//none found, so create a new command
		BuildCommand result = new BuildCommand();
		result.setBuilderName(builderName);
		result.setArguments(args);
		return result;
	}

	/**
	 * Gets a workspace delta for a given project, based on the state of the workspace
	 * tree the last time the current builder was run.
	 * <p>
	 * Returns null if:
	 * <ul>
	 * <li> The state of the workspace is unknown. </li>
	 * <li> The current builder has not indicated that it is interested in deltas
	 * for the given project. </li>
	 * <li> If the project does not exist. </li>
	 * </ul>
	 * <p>
	 * Deltas are computed once and cached for efficiency.
	 *
	 * @param project the project to get a delta for
	 */
	IResourceDelta getDelta(IProject project) {
		try {
			lock.acquire();
			if (currentTree == null) {
				if (Policy.DEBUG_BUILD_FAILURE)
					Policy.debug("Build: no tree for delta " + debugBuilder() + " [" + debugProject() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return null;
			}
			Set<InternalBuilder> interestedBuilders = getInterestedBuilders(project);
			//check if this builder has indicated it cares about this project
			if (interestedBuilders.isEmpty()) {
				if (Policy.DEBUG_BUILD_FAILURE)
					Policy.debug("Build: project not interesting for current builders " + debugBuilder() + " [" + debugProject() + "] " + project.getFullPath()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return null;
			}

			//now check against the cache
			return getDeltaCached(project, currentLastBuiltTree, currentTree);
		} finally {
			lock.release();
		}
	}

	private IResourceDelta getDeltaCached(IProject project, ElementTree oldTree, ElementTree newTree) {
		final IPath fullPath = project.getFullPath();
		IResourceDelta resultDelta = deltaCache.computeIfAbsent(fullPath, oldTree, newTree, () -> {
			long startTime = 0L;
			if (Policy.DEBUG_BUILD_DELTA) {
				startTime = System.currentTimeMillis();
				Policy.debug("Computing delta for project: " + project.getName()); //$NON-NLS-1$
			}
			IResourceDelta result;
			if (!project.exists() && !newTree.includes(fullPath) && !oldTree.includes(fullPath)) {
				result = null;
			} else {
				result = ResourceDeltaFactory.computeDelta(workspace, oldTree, newTree, fullPath, -1);
			}
			if (Policy.DEBUG_BUILD_FAILURE && result == null)
				Policy.debug(
						"Build: no delta " + debugBuilder() + " [" + debugProject() + "] " + fullPath); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (Policy.DEBUG_BUILD_DELTA)
				Policy.debug("Finished computing delta, time: " + (System.currentTimeMillis() - startTime) + "ms" //$NON-NLS-1$ //$NON-NLS-2$
						+ ((ResourceDelta) result).toDeepDebugString());

			return result;
		});
		return resultDelta;
	}

	/**
	 * Returns the safe runnable instance for invoking a builder
	 * @param currentBuilder
	 */
	private ISafeRunnable getSafeRunnable(final InternalBuilder currentBuilder, final int trigger, final Map<String, String> args, final MultiStatus status, final IProgressMonitor monitor) {
		return new ISafeRunnable() {
			@Override
			public void handleException(Throwable e) {
				if (e instanceof OperationCanceledException) {
					if (Policy.DEBUG_BUILD_INVOKING)
						Policy.debug("Build canceled"); //$NON-NLS-1$
					//just discard built state when a builder cancels, to ensure
					//that it is called again on the very next build.
					currentBuilder.forgetLastBuiltState();
					throw (OperationCanceledException) e;
				}
				//ResourceStats.buildException(e);
				// don't log the exception....it is already being logged in SafeRunner#run

				//add a generic message to the MultiStatus
				String builderName = currentBuilder.getLabel();
				if (builderName == null || builderName.length() == 0)
					builderName = currentBuilder.getClass().getName();
				String pluginId = currentBuilder.getPluginId();
				String message = NLS.bind(Messages.events_builderError, builderName, currentBuilder.getProject().getName());
				status.add(new Status(IStatus.ERROR, pluginId, IResourceStatus.BUILD_FAILED, message, e));

				//add the exception status to the MultiStatus
				if (e instanceof CoreException)
					status.add(((CoreException) e).getStatus());
			}

			@Override
			public void run() throws Exception {
				IProject[] prereqs = null;
				//invoke the appropriate build method depending on the trigger
				if (trigger != IncrementalProjectBuilder.CLEAN_BUILD)
					prereqs = currentBuilder.build(trigger, args, monitor);
				else {
					if (currentBuilder instanceof IIncrementalProjectBuilder2) {
						((IIncrementalProjectBuilder2) currentBuilder).clean(args, monitor);
					} else {
						currentBuilder.clean(monitor);
					}
				}
				if (prereqs == null)
					prereqs = new IProject[0];
				currentBuilder.setInterestingProjects(prereqs.clone());
			}
		};
	}

	/**
	 * We know the work manager is always available in the middle of
	 * a build.
	 */
	private WorkManager getWorkManager() {
		try {
			return workspace.getWorkManager();
		} catch (CoreException e) {
			//cannot happen
		}
		//avoid compile error
		return null;
	}

	@Override
	public void handleEvent(LifecycleEvent event) {
		IProject project = null;
		switch (event.kind) {
			case LifecycleEvent.PRE_PROJECT_DELETE :
			case LifecycleEvent.PRE_PROJECT_MOVE :
				project = (IProject) event.resource;
				//make sure the builder persistent info is deleted for the project move case
				if (project.isAccessible())
					setBuildersPersistentInfo(project, null);
		}
	}

	/**
	 * Returns true if at least one of the given project's configs have been built
	 * during this build cycle; and false otherwise.
	 */
	boolean hasBeenBuilt(IProject project) {
		return builtProjects.contains(project);
	}

	/**
	 * Hook for adding trace options and debug information at the end of a build.
	 * This hook is called after each builder instance is called.
	 */
	private void hookEndBuild(IncrementalProjectBuilder builder) {
		if (ResourceStats.TRACE_BUILDERS)
			ResourceStats.endBuild();
		if (!Policy.DEBUG_BUILD_INVOKING || timeStamp == -1)
			return; //builder wasn't called or we are not debugging
		Policy.debug("Builder finished: " + toString(builder) + " time: " + (System.currentTimeMillis() - timeStamp) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		timeStamp = -1;
	}

	/**
	 * Hook for adding trace options and debug information at the end of a build.
	 * This hook is called at the end of a build cycle invoked by calling a
	 * build API method.
	 */
	private void hookEndBuild(int trigger) {
		builtProjects.clear();
		deltaCache.flush();
		//ensure autobuild runs after a clean
		if (trigger == IncrementalProjectBuilder.CLEAN_BUILD)
			autoBuildJob.forceBuild();
		if (Policy.DEBUG_BUILD_INVOKING) {
			Policy.debug("Top-level build-end time: " + (System.currentTimeMillis() - overallTimeStamp)); //$NON-NLS-1$
			overallTimeStamp = -1;
		}
	}

	/**
	 * Hook for adding trace options and debug information at the start of a build.
	 * This hook is called before each builder instance is called.
	 */
	private void hookStartBuild(IncrementalProjectBuilder builder, int trigger) {
		if (ResourceStats.TRACE_BUILDERS)
			ResourceStats.startBuild(builder);
		if (Policy.DEBUG_BUILD_INVOKING) {
			timeStamp = System.currentTimeMillis();
			Policy.debug("Invoking (" + debugTrigger(trigger) + ") on builder: " + toString(builder)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Hook for adding trace options and debug information at the start of a build.
	 * This hook is called when a build API method is called, before any builders
	 * start running.
	 */
	private void hookStartBuild(IBuildConfiguration[] configs, int trigger) {
		if (Policy.DEBUG_BUILD_STACK)
			Policy.debug(new RuntimeException("Starting build: " + debugTrigger(trigger))); //$NON-NLS-1$
		if (Policy.DEBUG_BUILD_INVOKING) {
			overallTimeStamp = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder("Top-level build-start of: "); //$NON-NLS-1$
			for (IBuildConfiguration config : configs)
				sb.append(config).append(", "); //$NON-NLS-1$
			sb.append(debugTrigger(trigger));
			Policy.debug(sb.toString());
		}
	}

	/**
	 * Instantiates the builder with the given name.  If the builder, its plugin, or its nature
	 * is missing, create a placeholder builder to takes its place.  This is needed to generate
	 * appropriate exceptions when somebody tries to invoke the builder, and to
	 * prevent trying to instantiate it every time a build is run.
	 * This method NEVER returns null.
	 */
	private InternalBuilder initializeBuilder(ICommand command, String builderName, IBuildConfiguration buildConfiguration, BuilderPersistentInfo info, MultiStatus status) {
		IProject project = buildConfiguration.getProject();
		InternalBuilder builder = null;
		try {
			builder = instantiateBuilder(builderName);
		} catch (CoreException e) {
			status.add(new ResourceStatus(IResourceStatus.BUILD_FAILED, project.getFullPath(), NLS.bind(Messages.events_instantiate_1, builderName), e));
			status.add(e.getStatus());
		}
		if (builder == null) {
			//unable to create the builder, so create a placeholder to fill in for it
			builder = new MissingBuilder(builderName);
		}

		if (info != null) {
			ElementTree tree = info.getLastBuiltTree();
			if (tree != null) {
				builder.setLastBuiltTree(tree);
			}
			builder.setInterestingProjects(info.getInterestingProjects());
		}
		builder.setCommand(command);
		builder.setBuildConfig(buildConfiguration);
		builder.startupOnInitialize();
		return builder;
	}

	private BuilderPersistentInfo removePersistentBuilderInfo(String builderName, IBuildConfiguration buildConfiguration, int buildSpecIndex) throws CoreException {
		IProject project = buildConfiguration.getProject();
		ArrayList<BuilderPersistentInfo> infos = getBuildersPersistentInfo(project);
		if (infos != null) {
			BuilderPersistentInfo info = getBuilderInfo(infos, builderName, buildConfiguration.getName(), buildSpecIndex);
			if (info != null) {
				infos.remove(info);
				// delete the build map if it's now empty
				if (infos.isEmpty()) {
					setBuildersPersistentInfo(project, null);
				}
				return info;
			}

		}
		return null;
	}

	/**
	 * Instantiates and returns the builder with the given name.  If the builder, its plugin, or its nature
	 * is missing, returns null.
	 */
	private IncrementalProjectBuilder instantiateBuilder(String builderName) throws CoreException {
		IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS, builderName);
		if (extension == null)
			return null;
		IConfigurationElement[] configs = extension.getConfigurationElements();
		if (configs.length == 0)
			return null;
		String natureId = null;
		if (getBooleanAttribute(configs[0], "hasNature")) { //$NON-NLS-1$
			//find the nature that owns this builder
			String builderId = extension.getUniqueIdentifier();
			natureId = workspace.getNatureManager().findNatureForBuilder(builderId);
			if (natureId == null)
				return null;
		}
		//The nature exists, or this builder doesn't specify a nature
		InternalBuilder builder = (InternalBuilder) configs[0].createExecutableExtension("run"); //$NON-NLS-1$
		builder.setPluginId(extension.getContributor().getName());
		builder.setLabel(extension.getLabel());
		builder.setNatureId(natureId);
		builder.setCallOnEmptyDelta(getBooleanAttribute(configs[0], "callOnEmptyDelta")); //$NON-NLS-1$
		return (IncrementalProjectBuilder) builder;
	}

	/**
	 * Another thread is attempting to modify the workspace. Cancel the
	 * autobuild and wait until it completes.
	 */
	public void interrupt() {
		autoBuildJob.interrupt();
	}

	/**
	 * Returns whether an autobuild is pending (requested but not yet completed).
	 */
	public boolean isAutobuildBuildPending() {
		return autoBuildJob.getState() != Job.NONE;

	}

	/**
	 * Returns true if the current builder is interested in changes
	 * to the given project, and false otherwise.
	 */
	private boolean isInterestingProject(InternalBuilder currentBuilder, IProject project) {
		if (project.equals(currentBuilder.getProject()))
			return true;
		IProject[] interestingProjects = currentBuilder.getInterestingProjects();
		for (IProject interestingProject : interestingProjects) {
			if (interestingProject.equals(project)) {
				return true;
			}
		}
		return false;
	}

	private Set<InternalBuilder> getInterestedBuilders(final IProject project) {
		final Set<InternalBuilder> res = new HashSet<>();
		for (final InternalBuilder builder : this.currentBuilders) {
			if (isInterestingProject(builder, project)) {
				res.add(builder);
			}
		}
		return res;
	}

	/**
	 * Returns true if the given builder needs to be invoked, and false
	 * otherwise.
	 *
	 * The algorithm is to compute the intersection of the set of build configs that
	 * have changed since the last build, and the set of build configs this builder
	 * cares about.  This is an optimization, under the assumption that computing
	 * the forward delta once (not the resource delta) is more efficient than
	 * computing project deltas and invoking builders for projects that haven't
	 * changed.
	 */
	private boolean needsBuild(InternalBuilder builder, int trigger) {
		//on some triggers we build regardless of the delta
		switch (trigger) {
			case IncrementalProjectBuilder.CLEAN_BUILD :
				return true;
			case IncrementalProjectBuilder.FULL_BUILD :
				return true;
			case IncrementalProjectBuilder.INCREMENTAL_BUILD :
				for (InternalBuilder currentBuilder : this.currentBuilders) {
					if (currentBuilder.callOnEmptyDelta()) {
						return true;
					}
				}
				//fall through and check if there is a delta
		}

		//compute the delta since the last built state
		ElementTree oldTree = builder.getLastBuiltTree();
		ElementTree newTree = workspace.getElementTree();

		//search for the builder's project
		if (hasDelta(builder, builder.getProject(), oldTree, newTree)) {
			return true;
		}

		//search for builder's interesting projects
		IProject[] projects = builder.getInterestingProjects();
		for (IProject project : projects) {
			if (project != builder.getProject() // was already checked.
					&& hasDelta(builder, project, oldTree, newTree)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasDelta(InternalBuilder builder, IProject project, ElementTree oldTree, ElementTree newTree) {
		IResourceDelta delta = getDeltaCached(project, currentLastBuiltTree, currentTree);
		if (delta == null) {
			return false;
		}
		IResourceDelta[] children = delta.getAffectedChildren();
		boolean hasDelta = delta.getKind() != IResourceDelta.NO_CHANGE || children.length > 0;
		if (hasDelta && Policy.DEBUG_BUILD_NEEDED) {
			Policy.debug(toString(builder) + " needs building because of changes in: " + project.getName()); //$NON-NLS-1$
			if (Policy.DEBUG_BUILD_NEEDED_DELTA) {
				debugPrintDeltaRecursive(delta);
			}
		}
		return hasDelta;
	}

	private void debugPrintDeltaRecursive(IResourceDelta delta) {
		if (delta.getKind() != IResourceDelta.NO_CHANGE) {
			Policy.debug(((ResourceDelta) delta).toDebugString());
		}
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			debugPrintDeltaRecursive(childDelta);
		}
	}

	/**
	 * Removes all builders with the given ID from the build spec.
	 * Does nothing if there were no such builders in the spec
	 */
	private void removeBuilders(IProject project, String builderId) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] oldSpec = desc.getBuildSpec();
		int oldLength = oldSpec.length;
		if (oldLength == 0)
			return;
		int remaining = 0;
		//null out all commands that match the builder to remove
		for (int i = 0; i < oldSpec.length; i++) {
			if (oldSpec[i].getBuilderName().equals(builderId))
				oldSpec[i] = null;
			else
				remaining++;
		}
		//check if any were actually removed
		if (remaining == oldSpec.length)
			return;
		ICommand[] newSpec = new ICommand[remaining];
		for (int i = 0, newIndex = 0; i < oldLength; i++) {
			if (oldSpec[i] != null)
				newSpec[newIndex++] = oldSpec[i];
		}
		desc.setBuildSpec(newSpec);
		project.setDescription(desc, IResource.NONE, null);
	}

	/**
	 * Hook for builders to request a global rebuild for the main build loop on next
	 * build cycle. All projects will be rebuilt at least once after the current
	 * build cycle.
	 */
	void requestRebuild() {
		rebuildRequested = true;
	}

	/**
	 * Hook for builders to request a rebuild for given project during the current
	 * build call. The builders configured to run after the current one will be
	 * still processed. To force an immediate rebuild of a project that wasn't fully
	 * built yet, {@code processOtherBuilders} argument should be set to
	 * {@code false}.
	 * <p>
	 * <b>Note</b> if {@code processOtherBuilders} is set to {@code false}, the
	 * project that is built with current builder will be only rebuilt again, if
	 * this builder is not the first one configured to run.
	 *
	 * @param processOtherBuilders to continue building project with other builders
	 *                             and not start from scratch immediately
	 */
	void requestRebuild(IProject project, boolean processOtherBuilders) {
		if (project == null) {
			return;
		}
		restartBuildImmediately.put(project, !processOtherBuilders);
	}

	/**
	 * Hook for builders to request a rebuild for given projects. This request will
	 * cause the main build loop to cycle once again <b>at least</b> for given
	 * projects but the build loop also may run over all projects in build cycle if
	 * the {@link #requestRebuild()} flag was set.
	 * <p>
	 * <b>Note</b> the current project (that is currently built with current
	 * builder) will be not rebuilt in the current builld cycle, but scheduled for
	 * rebuild on next round. To perform immediate rebuild of the current project,
	 * use {@link #requestRebuild(IProject, boolean)}.
	 *
	 * @param toBeRebuilt to be rebuilt on next build round
	 * @param current     project currently built with current builder
	 */
	void requestRebuild(Collection<IProject> toBeRebuilt, IProject current) {
		for (IProject project : toBeRebuilt) {
			if (project != null && hasBeenBuilt(project) || project.equals(current)) {
				requestRebuildOnNextRound(project);
			}
		}
	}

	/**
	 * Hook for builders to request an <b>unconditional<b> rebuild for given
	 * project, in the next build round, independently if the project was already
	 * built or not.
	 */
	void requestRebuildOnNextRound(IProject project) {
		projectsToRebuild.add(project);
	}

	/**
	 * Sets the builder infos for the given build config.  The builder infos are
	 * an ArrayList of BuilderPersistentInfo.
	 * The list includes entries for all builders that are
	 * in the builder spec, and that have a last built state, even if they
	 * have not been instantiated this session.
	 */
	public void setBuildersPersistentInfo(IProject project, List<BuilderPersistentInfo> list) {
		try {
			project.setSessionProperty(K_BUILD_LIST, list);
		} catch (CoreException e) {
			//project is missing -- build state will be lost
			//can't throw an exception because this happens on startup
			logProjectAccessError(project, e, "Project missing in setBuildersPersistentInfo"); //$NON-NLS-1$
		}
	}

	private void setBuilderInitInfo(IProject project, String builderName, BuilderPersistentInfo info) {
		try {
			project.setSessionProperty(keyForBuilderInfo(builderName), info);
		} catch (CoreException e) {
			//project is missing -- build state will be lost
			//can't throw an exception because this happens on startup
			logProjectAccessError(project, e, "Project missing in setBuilderInitInfo"); //$NON-NLS-1$
		}
	}

	private BuilderPersistentInfo getBuilderInitInfo(IProject project, String builderName) {
		try {
			return (BuilderPersistentInfo) project.getSessionProperty(keyForBuilderInfo(builderName));
		} catch (CoreException e) {
			//project is missing -- build state will be lost
			//can't throw an exception because this happens on startup
			logProjectAccessError(project, e, "Project missing in getBuilderInitInfo"); //$NON-NLS-1$
		}
		return null;
	}

	private void logProjectAccessError(IProject project, CoreException e, String message) {
		Policy.log(new ResourceStatus(IStatus.ERROR, 1, project.getFullPath(), message, e));
	}

	private static QualifiedName keyForBuilderInfo(String builderName) {
		return new QualifiedName(ResourcesPlugin.PI_RESOURCES, BUILDER_INIT + builderName);
	}

	@Override
	public void shutdown(IProgressMonitor monitor) {
		autoBuildJob.cancel();
	}

	@Override
	public void startup(IProgressMonitor monitor) {
		workspace.addLifecycleListener(this);
	}

	/**
	 * Returns a string representation of the given builder.
	 * For debugging purposes only.
	 */
	private String toString(InternalBuilder builder) {
		String name = builder.getClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		if (builder instanceof MissingBuilder)
			name = name + ": '" + ((MissingBuilder) builder).getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
		return name + "(" + builder.getBuildConfig() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Returns true if the nature membership rules are satisfied for the given
	 * builder extension on the given project, and false otherwise.  A builder that
	 * does not specify that it belongs to a nature is always valid.  A builder
	 * extension that belongs to a nature can be invalid for the following reasons:
	 * <ul>
	 * <li>The nature that owns the builder does not exist on the given project</li>
	 * <li>The nature that owns the builder is disabled on the given project</li>
	 * </ul>
	 * Furthermore, if the nature that owns the builder does not exist on the project,
	 * that builder will be removed from the build spec.
	 *
	 * Note: This method only validates nature constraints that can vary at runtime.
	 * Additional checks are done in the instantiateBuilder method for constraints
	 * that cannot vary once the plugin registry is initialized.
	 */
	private boolean validateNature(InternalBuilder builder, String builderId) throws CoreException {
		String nature = builder.getNatureId();
		if (nature == null)
			return true;
		IProject project = builder.getProject();
		if (!project.hasNature(nature)) {
			//remove this builder from the build spec
			removeBuilders(project, builderId);
			return false;
		}
		return project.isNatureEnabled(nature);
	}

	/**
	 * Returns the scheduling rule that is required for building the project.
	 */
	public ISchedulingRule getRule(IBuildConfiguration buildConfiguration, int trigger, String builderName, Map<String, String> buildArgs) {
		IProject project = buildConfiguration.getProject();
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, Messages.events_errors, null);
		if (builderName == null) {
			final ICommand[] commands;
			if (project.isAccessible()) {
				Set<ISchedulingRule> rules = new HashSet<>();
				commands = ((Project) project).internalGetDescription().getBuildSpec(false);
				boolean hasNullBuildRule = false;
				BuildContext context = new BuildContext(buildConfiguration);
				for (int i = 0; i < commands.length; i++) {
					BuildCommand command = (BuildCommand) commands[i];
					Map<String, String> allArgs = command.getArguments(true);
					if (allArgs == null) {
						allArgs = buildArgs;
					} else if (buildArgs != null) {
						allArgs.putAll(buildArgs);
					}
					try {
						IncrementalProjectBuilder builder = getBuilder(buildConfiguration, command, i, status, context);
						if (builder != null) {
							ISchedulingRule builderRule = builder.getRule(trigger, allArgs);
							if (builderRule != null)
								rules.add(builderRule);
							else
								hasNullBuildRule = true;
						}
					} catch (CoreException e) {
						status.add(e.getStatus());
					}
				}
				if (rules.isEmpty())
					return null;
				// Bug 306824 - Builders returning a null rule can't work safely if other builders require a non-null rule
				// Be pessimistic and fall back to the default build rule (workspace root) in this case.
				if (!hasNullBuildRule)
					return new MultiRule(rules.toArray(new ISchedulingRule[rules.size()]));
			}
		} else {
			// Returns the derived resources for the specified builderName
			ICommand command = getCommand(project, builderName, buildArgs);
			Map<String, String> allArgs = new HashMap<>();
			if (command.getArguments() != null) {
				allArgs.putAll(command.getArguments());
			}
			if (buildArgs != null) {
				allArgs.putAll(buildArgs);
			}
			try {
				IncrementalProjectBuilder builder = getBuilder(buildConfiguration, command, -1, status);
				if (builder != null)
					return builder.getRule(trigger, allArgs);

			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		// Log any errors
		if (!status.isOK())
			Policy.log(status);
		return workspace.getRoot();
	}

	/**
	 * @return {@code true} if the projects build loop can restart immediately after
	 *         rebuild request, {@code false} if the loop will continue building all
	 *         not yet built projects
	 */
	public boolean isEarlyExitFromBuildLoopAllowed() {
		return earlyExitFromBuildLoopAllowed;
	}

	/**
	 * @param earlyExitFromBuildLoopAllowed {@code true} if the projects build loop
	 *                                      should restart immediately after rebuild
	 *                                      request, {@code false} if the loop
	 *                                      should continue building all not yet
	 *                                      built projects
	 */
	public void setEarlyExitFromBuildLoopAllowed(boolean earlyExitFromBuildLoopAllowed) {
		this.earlyExitFromBuildLoopAllowed = earlyExitFromBuildLoopAllowed;
	}

}
