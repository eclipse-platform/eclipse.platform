/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Red Hat Incorporated - loadProjectDescription(InputStream)
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add resource filtering
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Group and Project Path Variable Support
 *     Broadcom Corporation - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Christoph Läubrich - Issue #77, Issue #86, Issue #124
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildManager;
import org.eclipse.core.internal.events.ILifecycleListener;
import org.eclipse.core.internal.events.LifecycleEvent;
import org.eclipse.core.internal.events.NotificationManager;
import org.eclipse.core.internal.events.ResourceChangeEvent;
import org.eclipse.core.internal.events.ResourceComparator;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.internal.properties.IPropertyManager;
import org.eclipse.core.internal.properties.PropertyManager2;
import org.eclipse.core.internal.refresh.RefreshManager;
import org.eclipse.core.internal.resources.ComputeProjectOrder.Digraph;
import org.eclipse.core.internal.resources.ComputeProjectOrder.VertexOrder;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.internal.utils.StringPoolJob;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.TeamHook;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.Preferences;
import org.xml.sax.InputSource;

/**
 * The workspace class is the monolithic nerve center of the resources plugin.
 * All interesting functionality stems from this class.
 * <p>
 * The lifecycle of the resources plugin is encapsulated by the {@link #open(IProgressMonitor)}
 * and {@link #close(IProgressMonitor)} methods.  A closed workspace is completely
 * unusable - any attempt to access or modify interesting workspace state on a closed
 * workspace will fail.
 * </p>
 * <p>
 * All modifications to the workspace occur within the context of a workspace operation.
 * A workspace operation is implemented using the following sequence:
 * </p>
 * <pre>
 * 	try {
 *		prepareOperation(...);
 *		//check preconditions
 *		beginOperation(...);
 *		//perform changes
 *	} finally {
 *		endOperation(...);
 *	}
 * </pre>
 * Workspace operations can be nested arbitrarily. A "top level" workspace operation
 * is an operation that is not nested within another workspace operation in the current
 * thread.
 * <p>
 * See the javadoc of {@link #prepareOperation(ISchedulingRule, IProgressMonitor)},
 * {@link #beginOperation(boolean)}, and {@link #endOperation(ISchedulingRule, boolean)}
 * for more details.
 * </p>
 * <p>
 * Major areas of functionality are farmed off to various manager classes.  Open a
 * type hierarchy on {@link IManager} to see all the different managers. Each
 * manager is typically referenced three times in this class: Once in {@link #startup(IProgressMonitor)}
 * when it is instantiated, once in {@link #shutdown(IProgressMonitor)} when it
 * is destroyed, and once in a manager accessor method.
 * </p>
 */
public class Workspace extends PlatformObject implements IWorkspace, ICoreConstants {
	public static final boolean caseSensitive = Platform.OS_MACOSX.equals(Platform.getOS()) ? false : new java.io.File("a").compareTo(new java.io.File("A")) != 0; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Work manager should never be accessed directly because accessor
	 * asserts that workspace is still open.
	 */
	protected WorkManager _workManager;
	protected AliasManager aliasManager;
	protected BuildManager buildManager;
	protected volatile IBuildConfiguration[] buildOrder = null;
	protected volatile Digraph<IBuildConfiguration> buildOrderGraph;
	protected JobGroup buildJobGroup;
	protected CharsetManager charsetManager;
	protected ContentDescriptionManager contentDescriptionManager;
	/** indicates if the workspace crashed in a previous session */
	protected boolean crashed = false;
	protected final IWorkspaceRoot defaultRoot = new WorkspaceRoot(IPath.ROOT, this);
	protected WorkspacePreferences description;
	protected FileSystemResourceManager fileSystemManager;
	protected final CopyOnWriteArrayList<ILifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();
	protected final LocalMetaArea localMetaArea;
	/**
	 * Helper class for performing validation of resource names and locations.
	 */
	protected final LocationValidator locationValidator = new LocationValidator(this);
	protected MarkerManager markerManager;
	/**
	 * The currently installed Move/Delete hook.
	 */
	protected IMoveDeleteHook moveDeleteHook = null;
	protected NatureManager natureManager;
	protected FilterTypeManager filterManager;
	protected final AtomicLong nextMarkerId = new AtomicLong();
	protected final AtomicLong nextNodeId = new AtomicLong(1L);

	protected NotificationManager notificationManager;
	protected boolean openFlag = false;
	protected ElementTree operationTree; // tree at the start of the current operation
	protected PathVariableManager pathVariableManager;
	protected IPropertyManager propertyManager;

	protected RefreshManager refreshManager;

	/**
	 * Scheduling rule factory. This field is null if the factory has not been used
	 * yet.  The accessor method should be used rather than accessing this field
	 * directly.
	 */
	private IResourceRuleFactory ruleFactory;

	protected SaveManager saveManager;
	/**
	 * File modification validation.  If it is true and validator is null, we try/initialize
	 * validator first time through.  If false, there is no validator.
	 */
	protected boolean shouldValidate = true;

	/**
	 * Job that performs periodic string pool canonicalization.
	 */
	private StringPoolJob stringPoolJob;

	/**
	 * The synchronizer
	 */
	protected Synchronizer synchronizer;

	/**
	 * The currently installed team hook.
	 */
	protected TeamHook teamHook = null;

	/**
	 * The workspace tree.  The tree is an in-memory representation
	 * of the resources that make up the workspace.  The tree caches
	 * the structure and state of files and directories on disk (their existence
	 * and last modified times).  When external parties make changes to
	 * the files on disk, this representation becomes out of sync. A local refresh
	 * reconciles the state of the files on disk with this tree (@link {@link IResource#refreshLocal(int, IProgressMonitor)}).
	 * The tree is also used to store metadata associated with resources in
	 * the workspace (markers, properties, etc).
	 *
	 * While the ElementTree data structure can handle both concurrent
	 * reads and concurrent writes, write access to the tree is governed
	 * by {@link WorkManager}.
	 */
	protected volatile ElementTree tree;

	/**
	 * This field is used to control access to the workspace tree during
	 * resource change notifications. It tracks which thread, if any, is
	 * in the middle of a resource change notification.  This is used to cause
	 * attempts to modify the workspace during notifications to fail.
	 */
	protected volatile Thread treeLocked = null;

	/**
	 * The currently installed file modification validator.
	 */
	protected IFileModificationValidator validator = null;

	/**
	 * Data structure for holding the multi-part outcome of
	 * <code>IWorkspace.computeProjectBuildConfigOrder</code>.
	 * <p>
	 * This class is not intended to be instantiated by clients.
	 * </p>
	 *
	 * @see Workspace#computeProjectBuildConfigOrder(IBuildConfiguration[])
	 * @since 3.7
	 */
	public static final class ProjectBuildConfigOrder {
		/**
		 * Creates an instance with the given values.
		 * <p>
		 * This class is not intended to be instantiated by clients.
		 * </p>
		 *
		 * @param buildConfigurations initial value of <code>buildConfigurations</code> field
		 * @param hasCycles initial value of <code>hasCycles</code> field
		 * @param knots initial value of <code>knots</code> field
		 */
		public ProjectBuildConfigOrder(IBuildConfiguration[] buildConfigurations, boolean hasCycles, IBuildConfiguration[][] knots) {
			this.buildConfigurations = buildConfigurations;
			this.hasCycles = hasCycles;
			this.knots = knots;
		}

		/**
		 * A list of project buildConfigs ordered so as to honor the build configuration reference
		 * relationships between these project buildConfigs wherever possible. The elements
		 * are a subset of the ones passed as the <code>buildConfigurations</code>
		 * parameter to <code>IWorkspace.computeProjectOrder</code>, where
		 * inaccessible (closed or non-existent) projects have been omitted.
		 */
		public IBuildConfiguration[] buildConfigurations;
		/**
		 * Indicates whether any of the accessible project buildConfigs in
		 * <code>buildConfigurations</code> are involved in non-trivial cycles.
		 * <code>true</code> if the reference graph contains at least
		 * one cycle involving two or more of the project buildConfigs in
		 * <code>buildConfigurations</code>, and <code>false</code> if none of the
		 * project buildConfigs in <code>buildConfigurations</code> are involved in cycles.
		 */
		public boolean hasCycles;
		/**
		 * A list of knots in the reference graph. This list is empty if
		 * the reference graph does not contain cycles. If the
		 * reference graph contains cycles, each element is a knot of two or
		 * more accessible project buildConfigs from <code>buildConfigurations</code> that are
		 * involved in a cycle of mutually dependent references.
		 */
		public IBuildConfiguration[][] knots;
	}

	// Comparator used to provide a stable ordering of project buildConfigs
	private static class BuildConfigurationComparator implements Comparator<IBuildConfiguration> {
		public BuildConfigurationComparator() {
		}

		@Override
		public int compare(IBuildConfiguration px, IBuildConfiguration py) {
			int cmp = py.getProject().getName().compareTo(px.getProject().getName());
			if (cmp == 0)
				cmp = py.getName().compareTo(px.getName());
			return cmp;
		}
	}

	/**
	 * Deletes all the files and directories from the given root down (inclusive).
	 * Returns false if we could not delete some file or an exception occurred
	 * at any point in the deletion.
	 * Even if an exception occurs, a best effort is made to continue deleting.
	 */
	public static boolean clear(java.io.File root) {
		IFileStore fileStore = EFS.getLocalFileSystem().fromLocalFile(root);
		try {
			fileStore.delete(EFS.NONE, new NullProgressMonitor());
		} catch (CoreException e) {
			return false;
		}
		return true;
	}

	public static WorkspaceDescription defaultWorkspaceDescription() {
		return new WorkspaceDescription("Workspace"); //$NON-NLS-1$
	}

	/**
	 * Returns true if the object at the specified position has any
	 * other copy in the given array.
	 */
	private static boolean isDuplicate(Object[] array, int position) {
		if (array == null || position >= array.length)
			return false;
		for (int j = position - 1; j >= 0; j--)
			if (array[j].equals(array[position]))
				return true;
		return false;
	}

	public Workspace() {
		super();
		localMetaArea = new LocalMetaArea(this);
		tree = new ElementTree();
		/* tree should only be modified during operations */
		tree.immutable();
		treeLocked = Thread.currentThread();
		tree.setTreeData(newElement(IResource.ROOT));
	}

	/**
	 * Indicates that a build is about to occur. Broadcasts the necessary
	 * deltas before the build starts. Note that this will cause POST_BUILD
	 * to be automatically done at the end of the operation in which
	 * the build occurs.
	 */
	protected void aboutToBuild(Object source, int trigger) {
		//fire a POST_CHANGE first to ensure everyone is up to date before firing PRE_BUILD
		broadcastPostChange();
		broadcastBuildEvent(source, IResourceChangeEvent.PRE_BUILD, trigger);
	}

	/**
	 * Adds a listener for internal workspace lifecycle events.  There is no way to
	 * remove lifecycle listeners.
	 */
	public void addLifecycleListener(ILifecycleListener listener) {
		lifecycleListeners.addIfAbsent(listener);
	}

	@Override
	public void addResourceChangeListener(IResourceChangeListener listener) {
		notificationManager.addListener(listener, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
		notificationManager.addListener(listener, eventMask);
	}

	/**
	 * @deprecated Use {@link #addSaveParticipant(String, ISaveParticipant)} instead
	 */
	@Deprecated
	@Override
	public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant) throws CoreException {
		Assert.isNotNull(plugin, "Plugin must not be null"); //$NON-NLS-1$
		Assert.isNotNull(participant, "Participant must not be null"); //$NON-NLS-1$
		return saveManager.addParticipant(plugin.getBundle().getSymbolicName(), participant);
	}

	@Override
	public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
		Assert.isNotNull(pluginId, "Plugin id must not be null"); //$NON-NLS-1$
		Assert.isNotNull(participant, "Participant must not be null"); //$NON-NLS-1$
		return saveManager.addParticipant(pluginId, participant);
	}

	public void beginOperation(boolean createNewTree) throws CoreException {
		WorkManager workManager = getWorkManager();
		workManager.incrementNestedOperations();
		if (!workManager.isBalanced())
			Assert.isTrue(false, "Operation was not prepared."); //$NON-NLS-1$
		if (workManager.getPreparedOperationDepth() > 1) {
			if (createNewTree && tree.isImmutable())
				newWorkingTree();
			return;
		}
		// stash the current tree as the basis for this operation.
		operationTree = tree;
		if (createNewTree && tree.isImmutable())
			newWorkingTree();
	}

	public void broadcastBuildEvent(Object source, int type, int buildTrigger) {
		ResourceChangeEvent event = new ResourceChangeEvent(source, type, buildTrigger, null);
		notificationManager.broadcastChanges(tree, event, false);
	}

	/**
	 * Broadcasts an internal workspace lifecycle event to interested
	 * internal listeners.
	 */
	protected void broadcastEvent(LifecycleEvent event) throws CoreException {
		for (ILifecycleListener listener : lifecycleListeners)
			listener.handleEvent(event);
	}

	public void broadcastPostChange() {
		ResourceChangeEvent event = new ResourceChangeEvent(this, IResourceChangeEvent.POST_CHANGE, 0, null);
		notificationManager.broadcastChanges(tree, event, true);
	}

	/**
	 * Add all IBuildConfigurations reachable from config to the configs collection.
	 * @param configs collection of configurations to extend
	 * @param config config to find reachable configurations to.
	 */
	private void recursivelyAddBuildConfigs(Collection/*<IBuildConfiguration>*/<IBuildConfiguration> configs, IBuildConfiguration config) {
		try {
			IBuildConfiguration[] referenced = config.getProject().getReferencedBuildConfigs(config.getName(), false);
			for (IBuildConfiguration element : referenced) {
				if (configs.contains(element))
					continue;
				configs.add(element);
				recursivelyAddBuildConfigs(configs, element);
			}
		} catch (CoreException e) {
			// Not possible, we've checked that the project + configuration are accessible.
			Assert.isTrue(false);
		}
	}

	@Override
	public void build(int trigger, IProgressMonitor monitor) throws CoreException {
		buildInternal(EMPTY_BUILD_CONFIG_ARRAY, trigger, true, monitor);
	}

	@Override
	public void build(IBuildConfiguration[] configs, int trigger, boolean buildReferences, IProgressMonitor monitor) throws CoreException {
		if (configs.length == 0)
			return;
		buildInternal(configs, trigger, buildReferences, monitor);
	}

	/**
	 * Build the passed in configurations or the whole workspace.
	 * @param requestedConfigs to build or EMPTY_BUILD_CONFIG_ARRAY for the whole workspace
	 * @param trigger build trigger
	 * @param buildReferences transitively build referenced build configurations
	 */
	private void buildInternal(IBuildConfiguration[] requestedConfigs, int trigger, boolean buildReferences,
			IProgressMonitor monitor) throws CoreException {


		// Bug 343256 use a relaxed scheduling rule if the config we're building uses a relaxed rule.
		// Otherwise fall-back to WR.
		// PRE + POST_BUILD, and the build itself are allowed to modify resources, so require the current thread's scheduling rule
		// to either contain the WR or be null. Therefore, if not null, ensure it contains the WR rule...
		final boolean noEnclosingRule = Job.getJobManager().currentRule() == null;
		boolean relaxed = noEnclosingRule && requestedConfigs.length > 0 && allRelaxed(requestedConfigs, trigger);
		final ISchedulingRule notificationRule = getRuleFactory().buildRule();
		ISchedulingRule currentRule = null;
		boolean buildParallel = noEnclosingRule && getDescription().getMaxConcurrentBuilds() > 1 && getDescription().getBuildOrder() == null;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		SubMonitor newChild = subMonitor.newChild(1);
		try {
			try {

				// Must run the PRE_BUILD with the WRule held before acquiring WS lock
				// Can remove this if we run notifications without the WS lock held: bug 249951
				prepareOperation(notificationRule, newChild);
				currentRule = notificationRule;
				beginOperation(true);
				aboutToBuild(this, trigger);
			} finally {
				if (relaxed) {
					endOperation(currentRule, false);
					prepareOperation(null, subMonitor.newChild(1));
					currentRule = null;
					beginOperation(false);
				}
			}

			IStatus result;
			try {
				// Calculate the build-order having called the pre-build notification (which may
				// change build order)
				// If configs == EMPTY_BUILD_CONFIG_ARRAY => This is a full workspace build.
				IBuildConfiguration[] allConfigs = requestedConfigs;
				Digraph<IBuildConfiguration> buildGraph = null;
				if (allConfigs == EMPTY_BUILD_CONFIG_ARRAY) {
					if (trigger != IncrementalProjectBuilder.CLEAN_BUILD) {
						if (getDescription().getBuildOrder() != null) {
							allConfigs = getBuildOrder();
						} else {
							buildGraph = getBuildGraph();
						}
					} else {
						// clean all accessible configurations
						List<IBuildConfiguration> configArr = new ArrayList<>();
						IProject[] prjs = getRoot().getProjects();
						for (IProject prj : prjs)
							if (prj.isAccessible())
								configArr.addAll(Arrays.asList(prj.getBuildConfigs()));
						allConfigs = configArr.toArray(new IBuildConfiguration[configArr.size()]);
					}
				} else {
					// Order the passed in build configurations + resolve references if requested
					Set<IBuildConfiguration> refsList = new HashSet<>();
					for (IBuildConfiguration config : allConfigs) {
						// Check project + build configuration are accessible.
						if (!config.getProject().isAccessible()
								|| !config.getProject().hasBuildConfig(config.getName()))
							continue;
						refsList.add(config);
						// Find transitive closure of referenced project buildConfigs
						if (buildReferences)
							recursivelyAddBuildConfigs(refsList, config);
					}

					// Order the referenced project buildConfigs
					buildGraph = computeProjectBuildConfigOrderGraph(refsList);
				}
				if (buildGraph != null) {
					buildGraph.freeze();
					if (Policy.DEBUG_BUILD_CYCLE && buildGraph.containsCycles()) {
						List<IBuildConfiguration[]> nonTrivialComponents = buildGraph.nonTrivialComponents();
						for (IBuildConfiguration[] iBuildConfigurations : nonTrivialComponents) {
							Policy.debug("Cycle: " + Arrays.toString(iBuildConfigurations)); //$NON-NLS-1$
						}
					}
					allConfigs = ComputeProjectOrder.computeVertexOrder(buildGraph, IBuildConfiguration.class).vertexes;
				}

				buildParallel &= (buildGraph != null && buildGraph.vertexList.size() > 1);
				if (buildParallel) {
					relaxed = noEnclosingRule && allRelaxed(allConfigs, trigger);
					buildParallel &= relaxed;
				}
				if (buildParallel) {
					endOperation(currentRule, false); // operation needs to be ended to allow concurrent edits
					currentRule = null;
					result = getBuildManager().buildParallel(buildGraph, requestedConfigs, trigger, getBuildJobGroup(),
							subMonitor.newChild(97));
				} else {
					result = getBuildManager().build(allConfigs, requestedConfigs, trigger,
							subMonitor.newChild(97));
				}
			} finally {
				// Run the POST_BUILD with the WRule held
				if (relaxed) {
					if (!buildParallel) {
						endOperation(currentRule, false);
					}
					prepareOperation(notificationRule, subMonitor.newChild(1));
					currentRule = notificationRule;
					beginOperation(false);
				}
				// must fire POST_BUILD if PRE_BUILD has occurred
				broadcastBuildEvent(this, IResourceChangeEvent.POST_BUILD, trigger);
			}
			if (!result.isOK())
				throw new ResourceException(result);
		} finally {
			subMonitor.done();
			// building may close the tree, but we are still inside an operation so open it
			if (tree.isImmutable())
				newWorkingTree();
			// Rule will be the build-rule from the POST_BUILD refresh
			endOperation(currentRule, false);
		}
	}

	private JobGroup getBuildJobGroup() {
		if (buildJobGroup == null || buildJobGroup.getMaxThreads() != description.getMaxConcurrentBuilds()) {
			buildJobGroup = new JobGroup(getClass().getName(), description.getMaxConcurrentBuilds(), 0);
		}
		return buildJobGroup;
	}

	private boolean allRelaxed(IBuildConfiguration[] requestedConfigs, int trigger) {
		return Arrays.stream(requestedConfigs).map(cfg -> getBuildManager().getRule(cfg, trigger, null, null)).allMatch(this::isRelaxedRule);
	}

	boolean isRelaxedRule(ISchedulingRule rule) {
		return rule == null || !rule.contains(getRoot());
	}

	/**
	 * Returns whether creating executable extensions is acceptable
	 * at this point in time.  In particular, returns <code>false</code>
	 * when the system bundle is shutting down, which only occurs
	 * when the entire framework is exiting.
	 */
	private boolean canCreateExtensions() {
		return Platform.getBundle("org.eclipse.osgi").getState() != Bundle.STOPPING; //$NON-NLS-1$
	}

	@Override
	public void checkpoint(boolean build) {
		try {
			final ISchedulingRule rule = getWorkManager().getNotifyRule();
			try {
				prepareOperation(rule, null);
				beginOperation(true);
				broadcastPostChange();
			} finally {
				endOperation(rule, build);
			}
		} catch (CoreException e) {
			Policy.log(e.getStatus().getSeverity(), e.getMessage(), e);
		}
	}

	/**
	 * Closes this workspace; ignored if this workspace is not open.
	 * The state of this workspace is not saved before the workspace
	 * is shut down.
	 * <p>
	 * If the workspace was saved immediately prior to closing,
	 * it will have the same set of projects
	 * (open or closed) when reopened for a subsequent session.
	 * Otherwise, closing a workspace may lose some or all of the
	 * changes made since the last save or snapshot.
	 * </p>
	 * <p>
	 * Note that session properties are discarded when a workspace is closed.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @exception CoreException if the workspace could not be shutdown.
	 */
	public void close(IProgressMonitor monitor) throws CoreException {
		//nothing to do if the workspace failed to open
		if (!isOpen())
			return;
		String msg = Messages.resources_closing_0;
		SubMonitor subMonitor = SubMonitor.convert(monitor, msg, 20);
		subMonitor.subTask(msg);
		// this operation will never end because the world is going away
		SubMonitor newChild = subMonitor.newChild(1);
		try {
			try {
				stringPoolJob.cancel();
				// stop accepting refresh tasks & doing refresh
				refreshManager.shutdown(null);
				//shutdown save manager now so a last snapshot can be taken before we close
				//note: you can't call #save() from within a nested operation
				saveManager.shutdown(null);
				saveManager.reportSnapshotRequestor();
				prepareOperation(getRoot(), newChild);
				//shutdown notification first to avoid calling third parties during shutdown
				notificationManager.shutdown(null);
				beginOperation(true);
				IProject[] projects = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
				subMonitor.setWorkRemaining(projects.length + 2);
				for (IProject project : projects) {
					//notify managers of closing so they can cleanup
					broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_CLOSE, project));
					subMonitor.worked(1);
				}
				//empty the workspace tree so we leave in a clean state
				deleteResource(getRoot());
				openFlag = false;
				// endOperation not needed here
			} finally {
				// Shutdown needs to be executed regardless of failures
				shutdown(subMonitor.newChild(2, SubMonitor.SUPPRESS_SUBTASK));
			}
		} finally {
			//release the scheduling rule to be a good job citizen
			Job.getJobManager().endRule(getRoot());
		}
	}

	/**
	 * Computes the global total ordering of all open projects in the
	 * workspace based on project references. If an existing and open project P
	 * references another existing and open project Q also included in the list,
	 * then Q should come before P in the resulting ordering. Closed and non-
	 * existent projects are ignored, and will not appear in the result. References
	 * to non-existent or closed projects are also ignored, as are any self-
	 * references.
	 * <p>
	 * When there are choices, the choice is made in a reasonably stable way. For
	 * example, given an arbitrary choice between two projects, the one with the
	 * lower collating project name is usually selected.
	 * </p>
	 * <p>
	 * When the project reference graph contains cyclic references, it is
	 * impossible to honor all of the relationships. In this case, the result
	 * ignores as few relationships as possible.  For example, if P2 references P1,
	 * P4 references P3, and P2 and P3 reference each other, then exactly one of the
	 * relationships between P2 and P3 will have to be ignored. The outcome will be
	 * either [P1, P2, P3, P4] or [P1, P3, P2, P4]. The result also contains
	 * complete details of any cycles present.
	 * </p>
	 *
	 * @return result describing the global project order
	 * @since 2.1
	 */
	private VertexOrder<IProject> computeFullProjectOrder() {
		// determine the full set of accessible projects in the workspace
		// order the set in descending alphabetical order of project name
		SortedSet<IProject> allAccessibleProjects = new TreeSet<>(Comparator.comparing(IProject::getName).reversed());
		IProject[] allProjects = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		// List<IProject[]> edges
		List<IProject[]> edges = new ArrayList<>(allProjects.length);
		for (IProject p : allProjects) {
			Project project = (Project) p;
			// ignore projects that are not accessible
			if (!project.isAccessible())
				continue;
			ProjectDescription desc = project.internalGetDescription();
			if (desc == null)
				continue;
			//obtain both static and dynamic project references
			IProject[] refs = desc.getAllReferences(project, false);
			allAccessibleProjects.add(project);
			for (IProject ref : refs) {
				// ignore self references and references to projects that are not accessible
				if (ref.isAccessible() && !ref.equals(project))
					edges.add(new IProject[] {project, ref});
			}
		}
		return ComputeProjectOrder.computeVertexOrder(allAccessibleProjects, edges, IProject.class);
	}

	/**
	 * Computes the global total ordering of all open projects' active buildConfigs in the
	 * workspace based on build configuration references. If an existing and open project's build config P
	 * references another existing and open project's build config Q, then Q should come before P
	 * in the resulting ordering. If a build config references a non-active build config it is
	 * added to the resulting ordered list. Closed and non-existent projects/buildConfigs are
	 * ignored, and will not appear in the result. References to non-existent or closed
	 * projects/buildConfigs are also ignored, as are any self-references.
	 * <p>
	 * When there are choices, the choice is made in a reasonably stable way. For
	 * example, given an arbitrary choice between two project buildConfigs, the one with the
	 * lower collating project name and build config name will appear earlier in the list.
	 * </p>
	 * <p>
	 * When the build configuration reference graph contains cyclic references, it is
	 * impossible to honor all of the relationships. In this case, the result
	 * ignores as few relationships as possible.  For example, if P2 references P1,
	 * P4 references P3, and P2 and P3 reference each other, then exactly one of the
	 * relationships between P2 and P3 will have to be ignored. The outcome will be
	 * either [P1, P2, P3, P4] or [P1, P3, P2, P4]. The result also contains
	 * complete details of any cycles present.
	 * </p>
	 *
	 * @return result describing the global active build configuration order
	 */
	private VertexOrder<IBuildConfiguration> computeActiveBuildConfigOrder() {
		Digraph<IBuildConfiguration> activeBuildConfigurationGraph = computeActiveBuildConfigGraph();
		return ComputeProjectOrder.computeVertexOrder(activeBuildConfigurationGraph, IBuildConfiguration.class);
	}

	private Digraph<IBuildConfiguration> computeActiveBuildConfigGraph() {
		// Determine the full set of accessible active project buildConfigs in the workspace,
		// and all the accessible project buildConfigs that they reference. This forms a set
		// of all the project buildConfigs that will be returned.
		// Order the set in descending alphabetical order of project name then build config name,
		// as a secondary sort applied after sorting based on references, to achieve a stable
		// ordering.
		SortedSet<IBuildConfiguration> allAccessibleBuildConfigs = new TreeSet<>(new BuildConfigurationComparator());

		// For each project's active build config, perform a depth first search in the reference graph
		// rooted at that build config.
		// This generates the required subset of the reference graph that is required to order all
		// the dependencies of the active project buildConfigs.
		IProject[] allProjects = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		List<IBuildConfiguration[]> edges = new ArrayList<>(allProjects.length);

		for (IProject allProject : allProjects) {
			Project project = (Project) allProject;
			// Ignore projects that are not accessible
			if (!project.isAccessible())
				continue;

			// If the active build configuration hasn't already been explored
			// perform a depth first search rooted at it
			if (!allAccessibleBuildConfigs.contains(project.internalGetActiveBuildConfig())) {
				allAccessibleBuildConfigs.add(project.internalGetActiveBuildConfig());
				Deque<IBuildConfiguration> stack = new ArrayDeque<>();
				stack.push(project.internalGetActiveBuildConfig());

				while (!stack.isEmpty()) {
					IBuildConfiguration buildConfiguration = stack.pop();

					// Add all referenced buildConfigs from the current configuration
					// (it is guaranteed to be accessible as it was pushed onto the stack)
					Project subProject = (Project) buildConfiguration.getProject();
					IBuildConfiguration[] refs = subProject.internalGetReferencedBuildConfigs(buildConfiguration.getName(), false);
					for (IBuildConfiguration ref : refs) {
						// Ignore self references and references to projects that are not accessible
						if (ref.equals(buildConfiguration))
							continue;

						// Add the referenced accessible configuration
						edges.add(new IBuildConfiguration[] {buildConfiguration, ref});

						// If we have already explored the referenced configuration, don't explore it again
						if (allAccessibleBuildConfigs.contains(ref))
							continue;

						allAccessibleBuildConfigs.add(ref);

						// Push the referenced configuration onto the stack so that it is explored by the depth first search
						stack.push(ref);
					}
				}
			}
		}
		return ComputeProjectOrder.computeGraph(allAccessibleBuildConfigs, edges, IBuildConfiguration.class);
	}

	/**
	 * Computes the global total ordering of all project buildConfigs in the workspace based
	 * on build config references. If an existing and open build config P
	 * references another existing and open project build config Q, then Q should come before P
	 * in the resulting ordering. Closed and non-existent projects/buildConfigs are
	 * ignored, and will not appear in the result. References to non-existent or closed
	 * projects/buildConfigs are also ignored, as are any self-references.
	 * <p>
	 * When there are choices, the choice is made in a reasonably stable way. For
	 * example, given an arbitrary choice between two project buildConfigs, the one with the
	 * lower collating project name and build config name will appear earlier in the list.
	 * </p>
	 * <p>
	 * When the build config reference graph contains cyclic references, it is
	 * impossible to honor all of the relationships. In this case, the result
	 * ignores as few relationships as possible.  For example, if P2 references P1,
	 * P4 references P3, and P2 and P3 reference each other, then exactly one of the
	 * relationships between P2 and P3 will have to be ignored. The outcome will be
	 * either [P1, P2, P3, P4] or [P1, P3, P2, P4]. The result also contains
	 * complete details of any cycles present.
	 * </p>
	 *
	 * @return result describing the global project build configuration order
	 * @deprecated Use {@link #computeFullBuildConfigGraph()} instead
	 */
	@Deprecated
	private VertexOrder<IBuildConfiguration> computeFullBuildConfigOrder() {
		Digraph<IBuildConfiguration> graph = computeFullBuildConfigGraph();
		return ComputeProjectOrder.computeVertexOrder(graph, IBuildConfiguration.class);
	}

	private Digraph<IBuildConfiguration> computeFullBuildConfigGraph() {
		// Compute the order for all accessible project buildConfigs
		SortedSet<IBuildConfiguration> allAccessibleBuildConfigurations = new TreeSet<>(new BuildConfigurationComparator());

		IProject[] allProjects = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		List<IBuildConfiguration[]> edges = new ArrayList<>(allProjects.length);

		for (IProject p : allProjects) {
			Project project = (Project) p;
			// Ignore projects that are not accessible
			if (!project.isAccessible())
				continue;

			IBuildConfiguration[] configs = project.internalGetBuildConfigs(false);
			for (IBuildConfiguration config : configs) {
				allAccessibleBuildConfigurations.add(config);
				IBuildConfiguration[] refs = project.internalGetReferencedBuildConfigs(config.getName(), false);
				for (IBuildConfiguration ref : refs) {
					// Ignore self references
					if (ref.equals(config))
						continue;

					// Add the reference to the set of reachable configs + add an edge
					allAccessibleBuildConfigurations.add(ref);
					edges.add(new IBuildConfiguration[] {config, ref});
				}
			}
		}
		return ComputeProjectOrder.computeGraph(allAccessibleBuildConfigurations, edges, IBuildConfiguration.class);
	}

	private static ProjectOrder vertexOrderToProjectOrder(VertexOrder<IProject> order) {
		IProject[] projects = new IProject[order.vertexes.length];
		System.arraycopy(order.vertexes, 0, projects, 0, order.vertexes.length);
		IProject[][] knots = new IProject[order.knots.length][];
		for (int i = 0; i < order.knots.length; i++) {
			knots[i] = new IProject[order.knots[i].length];
			System.arraycopy(order.knots[i], 0, knots[i], 0, order.knots[i].length);
		}
		return new ProjectOrder(projects, order.hasCycles, knots);
	}

	private static ProjectBuildConfigOrder vertexOrderToProjectBuildConfigOrder(VertexOrder<IBuildConfiguration> order) {
		IBuildConfiguration[] buildConfigs = new IBuildConfiguration[order.vertexes.length];
		System.arraycopy(order.vertexes, 0, buildConfigs, 0, order.vertexes.length);
		IBuildConfiguration[][] knots = new IBuildConfiguration[order.knots.length][];
		for (int i = 0; i < order.knots.length; i++) {
			knots[i] = new IBuildConfiguration[order.knots[i].length];
			System.arraycopy(order.knots[i], 0, knots[i], 0, order.knots[i].length);
		}
		return new ProjectBuildConfigOrder(buildConfigs, order.hasCycles, knots);
	}

	@Deprecated
	@Override
	public IProject[][] computePrerequisiteOrder(IProject[] targets) {
		return computePrerequisiteOrder1(targets);
	}

	/*
	 * Compatible reimplementation of
	 * <code>IWorkspace.computePrerequisiteOrder</code> using
	 * <code>IWorkspace.computeProjectOrder</code>.
	 *
	 * @since 2.1
	 */
	private IProject[][] computePrerequisiteOrder1(IProject[] projects) {
		IWorkspace.ProjectOrder r = computeProjectOrder(projects);
		if (!r.hasCycles) {
			return new IProject[][] {r.projects, new IProject[0]};
		}
		// when there are cycles, we need to remove all knotted projects from
		// r.projects to form result[0] and merge all knots to form result[1]
		// Set<IProject> bad
		Set<IProject> bad = new HashSet<>();
		// Set<IProject> bad
		Set<IProject> keepers = new HashSet<>(Arrays.asList(r.projects));
		for (IProject[] knot : r.knots) {
			for (IProject project : knot) {
				// keep only selected projects in knot
				if (keepers.contains(project)) {
					bad.add(project);
				}
			}
		}
		IProject[] result2 = new IProject[bad.size()];
		bad.toArray(result2);
		// List<IProject> p
		List<IProject> p = new LinkedList<>();
		p.addAll(Arrays.asList(r.projects));
		for (Iterator<IProject> it = p.listIterator(); it.hasNext();) {
			IProject project = it.next();
			if (bad.contains(project)) {
				// remove knotted projects from the main answer
				it.remove();
			}
		}
		IProject[] result1 = new IProject[p.size()];
		p.toArray(result1);
		return new IProject[][] {result1, result2};
	}

	@Override
	public ProjectOrder computeProjectOrder(IProject[] projects) {
		// Compute the full project order for all accessible projects
		VertexOrder<IProject> fullProjectOrder = computeFullProjectOrder();

		// Create a filter to remove all projects that are not in the list asked for
		final Set<IProject> projectSet = new HashSet<>(projects.length);
		projectSet.addAll(Arrays.asList(projects));
		Predicate<IProject> filter = vertex -> !projectSet.contains(vertex);

		// Filter the order and return it
		return vertexOrderToProjectOrder(ComputeProjectOrder.filterVertexOrder(fullProjectOrder, filter, IProject.class));
	}

	/**
	 * Computes a total ordering of the given projects buildConfigs based on both static and
	 * dynamic project references. If an existing and open project's build configuratioin P references
	 * another existing and open project's configuration Q also included in the list, then Q
	 * should come before P in the resulting ordering. Closed and non-existent
	 * projects are ignored, and will not appear in the result. References to
	 * non-existent or closed projects/buildConfigs are also ignored, as are any
	 * self-references. The total ordering is always consistent with the global
	 * total ordering of all open projects' buildConfigs in the workspace.
	 * <p>
	 * When there are choices, the choice is made in a reasonably stable way.
	 * For example, given an arbitrary choice between two project buildConfigs, the one with
	 * the lower collating configuration name is usually selected.
	 * </p>
	 * <p>
	 * When the project reference graph contains cyclic references, it is
	 * impossible to honor all of the relationships. In this case, the result
	 * ignores as few relationships as possible. For example, if P2 references
	 * P1, P4 references P3, and P2 and P3 reference each other, then exactly
	 * one of the relationships between P2 and P3 will have to be ignored. The
	 * outcome will be either [P1, P2, P3, P4] or [P1, P3, P2, P4]. The result
	 * also contains complete details of any cycles present.
	 * </p>
	 * <p>
	 * This method is time-consuming and should not be called unnecessarily.
	 * There are a very limited set of changes to a workspace that could affect
	 * the outcome: creating, renaming, or deleting a project; opening or
	 * closing a project; deleting a build configuration; adding or removing a build configuration reference.
	 * </p>
	 *
	 * @param buildConfigs the build configurations to order
	 * @return result describing the build configuration order
	 * @since 3.7
	 */
	public ProjectBuildConfigOrder computeProjectBuildConfigOrder(IBuildConfiguration[] buildConfigs) {
		// Compute the full project order for all accessible projects
		VertexOrder<IBuildConfiguration> fullBuildConfigOrder = computeFullBuildConfigOrder();

		// Create a filter to remove all project buildConfigs that are not in the list asked for
		final Set<IBuildConfiguration> projectConfigSet = new HashSet<>(buildConfigs.length);
		projectConfigSet.addAll(Arrays.asList(buildConfigs));
		Predicate<IBuildConfiguration> filter = vertex -> !projectConfigSet.contains(vertex);

		// Filter the order and return it
		return vertexOrderToProjectBuildConfigOrder(ComputeProjectOrder.filterVertexOrder(fullBuildConfigOrder, filter, IBuildConfiguration.class));
	}

	private Digraph<IBuildConfiguration> computeProjectBuildConfigOrderGraph(Collection<IBuildConfiguration> buildConfigs) {
		Digraph<IBuildConfiguration> fullBuildConfigOrder = computeFullBuildConfigGraph();

		// Create a filter to remove all project buildConfigs that are not in the list asked for
		final Set<IBuildConfiguration> projectConfigSet = new HashSet<>(buildConfigs);
		Predicate<IBuildConfiguration> filter = vertex -> !projectConfigSet.contains(vertex);

		// Filter the order and return it
		return ComputeProjectOrder.buildFilteredDigraph(fullBuildConfigOrder, filter, IBuildConfiguration.class);
	}

	@Override
	public IStatus copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		return copy(resources, destination, updateFlags, monitor);
	}

	@Override
	public IStatus copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		Assert.isLegal(resources != null);

		if (resources.length == 0) {
			return Status.OK_STATUS;
		}
		// to avoid concurrent changes to this array
		resources = resources.clone();
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.resources_copying_0, resources.length);
		IPath parentPath = null;
		String message = Messages.resources_copyProblem;
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, message, null);
		try {
			prepareOperation(getRoot(), subMonitor);
			beginOperation(true);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (resource == null || isDuplicate(resources, i)) {
					subMonitor.split(1);
					continue;
				}
				// test siblings
				if (parentPath == null)
					parentPath = resource.getFullPath().removeLastSegments(1);
				if (parentPath.equals(resource.getFullPath().removeLastSegments(1))) {
					// test copy requirements
					try {
						IPath destinationPath = destination.append(resource.getName());
						IStatus requirements = ((Resource) resource).checkCopyRequirements(destinationPath, resource.getType(), updateFlags);
						if (requirements.isOK()) {
							try {
								resource.copy(destinationPath, updateFlags, Policy.subMonitorFor(monitor, 1));
							} catch (CoreException e) {
								status.merge(e.getStatus());
							}
						} else {
							status.merge(requirements);
						}
					} catch (CoreException e) {
						status.merge(e.getStatus());
					}
				} else {
					message = NLS.bind(Messages.resources_notChild, resources[i].getFullPath(), parentPath);
					status.merge(new ResourceStatus(IResourceStatus.OPERATION_FAILED, resources[i].getFullPath(), message));
				}
				subMonitor.worked(1);
			}
		} catch (OperationCanceledException e) {
			getWorkManager().operationCanceled();
			throw e;
		} finally {
			subMonitor.done();
			endOperation(getRoot(), true);
		}
		if (status.matches(IStatus.ERROR))
			throw new ResourceException(status);
		return status.isOK() ? Status.OK_STATUS : (IStatus) status;
	}

	protected void copyTree(IResource source, IPath destination, int depth, int updateFlags, boolean keepSyncInfo) throws CoreException {
		copyTree(source, destination, depth, updateFlags, keepSyncInfo, false, source.getType() == IResource.PROJECT);
	}

	private void copyTree(IResource source, IPath destination, int depth, int updateFlags, boolean keepSyncInfo, boolean moveResources, boolean movingProject) throws CoreException {

		// retrieve the resource at the destination if there is one (phantoms included).
		// if there isn't one, then create a new handle based on the type that we are
		// trying to copy
		IResource destinationResource = getRoot().findMember(destination, true);
		int destinationType;
		if (destinationResource == null) {
			if (source.getType() == IResource.FILE)
				destinationType = IResource.FILE;
			else if (destination.segmentCount() == 1)
				destinationType = IResource.PROJECT;
			else
				destinationType = IResource.FOLDER;
			destinationResource = newResource(destination, destinationType);
		} else
			destinationType = destinationResource.getType();

		// create the resource at the destination
		ResourceInfo sourceInfo = ((Resource) source).getResourceInfo(true, false);
		if (destinationType != source.getType()) {
			sourceInfo = (ResourceInfo) sourceInfo.clone();
			sourceInfo.setType(destinationType);
		}
		ResourceInfo newInfo = createResource(destinationResource, sourceInfo, false, true, keepSyncInfo);
		// get/set the node id from the source's resource info so we can later put it in the
		// info for the destination resource. This will help us generate the proper deltas,
		// indicating a move rather than a add/delete
		newInfo.setNodeId(sourceInfo.getNodeId());

		// preserve local sync info but not location info
		newInfo.setFlags(newInfo.getFlags() | (sourceInfo.getFlags() & M_LOCAL_EXISTS));
		newInfo.setFileStoreRoot(null);

		// forget content-related caching flags
		newInfo.clear(M_CONTENT_CACHE);

		// update link locations in project descriptions
		if (source.isLinked()) {
			LinkDescription linkDescription;
			URI sourceLocationURI = transferVariableDefinition(source, destinationResource, source.getLocationURI());
			if (((updateFlags & IResource.SHALLOW) != 0) || ((Resource) source).isUnderVirtual()) {
				//for shallow move the destination is a linked resource with the same location
				newInfo.set(ICoreConstants.M_LINK);
				linkDescription = new LinkDescription(destinationResource, sourceLocationURI);
			} else {
				//for deep move the destination is not a linked resource
				newInfo.clear(ICoreConstants.M_LINK);
				linkDescription = null;
			}
			if (moveResources && !movingProject) {
				if (((Project) source.getProject()).internalGetDescription().setLinkLocation(source.getProjectRelativePath(), null))
					((Project) source.getProject()).writeDescription(updateFlags);
			}
			Project project = (Project) destinationResource.getProject();
			project.internalGetDescription().setLinkLocation(destinationResource.getProjectRelativePath(), linkDescription);
			project.writeDescription(updateFlags);
			newInfo.setFileStoreRoot(null);
		}

		// Update folder filters in project descriptions - copy filters for any container except project.
		// Filters that are set on project itself are copied automatically.
		if (!movingProject && source instanceof Container && source.getProject().exists()
				&& ((Container) source).hasFilters()) {
			Project sourceProject = (Project) source.getProject();
			LinkedList<FilterDescription> originalDescriptions = sourceProject.internalGetDescription().getFilter(source.getProjectRelativePath());
			LinkedList<FilterDescription> filterDescriptions = FilterDescription.copy(originalDescriptions, destinationResource);
			if (moveResources) {
				if (((Project) source.getProject()).internalGetDescription().setFilters(source.getProjectRelativePath(), null))
					((Project) source.getProject()).writeDescription(updateFlags);
			}
			Project project = (Project) destinationResource.getProject();
			project.internalGetDescription().setFilters(destinationResource.getProjectRelativePath(), filterDescriptions);
			project.writeDescription(updateFlags);
		}

		// do the recursion. if we have a file then it has no members so return. otherwise
		// recursively call this method on the container's members if the depth tells us to
		if (depth == IResource.DEPTH_ZERO || source.getType() == IResource.FILE)
			return;
		if (depth == IResource.DEPTH_ONE)
			depth = IResource.DEPTH_ZERO;
		//copy .project file first if project is being copied, otherwise links won't be able to update description
		boolean projectCopy = source.getType() == IResource.PROJECT && destinationType == IResource.PROJECT;
		if (projectCopy) {
			IResource dotProject = ((Project) source).findMember(IProjectDescription.DESCRIPTION_FILE_NAME);
			if (dotProject != null)
				copyTree(dotProject, destination.append(dotProject.getName()), depth, updateFlags, keepSyncInfo, moveResources, movingProject);
		}
		IResource[] children = ((IContainer) source).members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
		for (IResource element : children) {
			String childName = element.getName();
			if (!projectCopy || !childName.equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
				IPath childPath = destination.append(childName);
				copyTree(element, childPath, depth, updateFlags, keepSyncInfo, moveResources, movingProject);
			}
		}
	}

	public URI transferVariableDefinition(IResource source, IResource dest, URI sourceURI) throws CoreException {
		IPath srcLoc = source.getLocation();
		IPath srcRawLoc = source.getRawLocation();
		if ((srcLoc != null) && (srcRawLoc != null) && !srcLoc.equals(srcRawLoc)) {
			// the location is variable relative
			if (!source.getProject().equals(dest.getProject())) {
				String variable = srcRawLoc.segment(0);
				variable = copyVariable(source, dest, variable);
				IPath newLocation = IPath.fromPortableString(variable).append(srcRawLoc.removeFirstSegments(1));
				sourceURI = toURI(newLocation);
			} else {
				sourceURI = toURI(srcRawLoc);
			}
		}
		return sourceURI;
	}

	URI toURI(IPath path) {
		if (path.isAbsolute())
			return org.eclipse.core.filesystem.URIUtil.toURI(path);
		try {
			return new URI(null, null, path.toPortableString(), null);
		} catch (URISyntaxException e) {
			return org.eclipse.core.filesystem.URIUtil.toURI(path);
		}
	}

	String copyVariable(IResource source, IResource dest, String variable) throws CoreException {
		IPathVariableManager destPathVariableManager = dest.getPathVariableManager();
		IPathVariableManager srcPathVariableManager = source.getPathVariableManager();

		IPath srcValue = URIUtil.toPath(srcPathVariableManager.getURIValue(variable));
		if (srcValue == null) // if the variable doesn't exist, return another
								// variable that doesn't exist either
			return PathVariableUtil.getUniqueVariableName(variable, dest);
		IPath resolvedSrcValue = URIUtil.toPath(srcPathVariableManager.resolveURI(URIUtil.toURI(srcValue)));

		boolean variableExisted = false;
		// look if the exact same variable exists
		if (destPathVariableManager.isDefined(variable)) {
			variableExisted = true;
			IPath destValue = URIUtil.toPath(destPathVariableManager.getURIValue(variable));
			if (destValue != null && URIUtil.toPath(destPathVariableManager.resolveURI(URIUtil.toURI(destValue))).equals(resolvedSrcValue))
				return variable;
		}
		// look if one variable in the destination project matches
		String[] variables = destPathVariableManager.getPathVariableNames();
		for (String other : variables) {
			if (!PathVariableUtil.isPreferred(other))
				continue;
			IPath resolveDestVariable = URIUtil.toPath(destPathVariableManager.resolveURI(destPathVariableManager.getURIValue(other)));
			if (resolveDestVariable != null && resolveDestVariable.equals(resolvedSrcValue)) {
				return other;
			}
		}
		// if the variable doesn't exist in the dest project, or
		// if the value is different than the source project, we have to create
		// an equivalent.
		String destVariable = PathVariableUtil.getUniqueVariableName(variable, dest);

		boolean shouldConvertToRelative = true;
		if (!srcValue.equals(resolvedSrcValue) && !variableExisted) {
			// the variable content contains references to more variables

			String[] referencedVariables = PathVariableUtil.splitVariableNames(srcValue.toPortableString());
			shouldConvertToRelative = false;
			// If the variable value is of type ${PARENT-COUNT-VAR},
			// we can avoid generating an intermediate variable and convert it directly.
			if (referencedVariables.length == 1) {
				if (PathVariableUtil.isParentVariable(referencedVariables[0]))
					shouldConvertToRelative = true;
			}

			if (!shouldConvertToRelative) {
				String[] segments = PathVariableUtil.splitVariablesAndContent(srcValue.toPortableString());
				StringBuilder result = new StringBuilder();
				for (String segment : segments) {
					String var = PathVariableUtil.extractVariable(segment);
					if (var.length() > 0) {
						String copiedVariable = copyVariable(source, dest, var);
						int index = segment.indexOf(var);
						if (index != -1) {
							result.append(segment.substring(0, index));
							result.append(copiedVariable);
							int start = index + var.length();
							int end = segment.length();
							result.append(segment.substring(start, end));
						}
					} else
						result.append(segment);
				}
				srcValue = IPath.fromPortableString(result.toString());
			}
		}
		if (shouldConvertToRelative) {
			IPath relativeSrcValue = PathVariableUtil.convertToPathRelativeMacro(destPathVariableManager, resolvedSrcValue, dest, true, null);
			if (relativeSrcValue != null)
				srcValue = relativeSrcValue;
		}
		destPathVariableManager.setURIValue(destVariable, URIUtil.toURI(srcValue));
		return destVariable;
	}

	/**
	 * Returns the number of resources in a subtree of the resource tree.
	 *
	 * @param root The subtree to count resources for
	 * @param depth The depth of the subtree to count
	 * @param phantom If true, phantoms are included, otherwise they are ignored.
	 */
	public int countResources(IPath root, int depth, final boolean phantom) {
		if (!tree.includes(root))
			return 0;
		switch (depth) {
			case IResource.DEPTH_ZERO :
				return 1;
			case IResource.DEPTH_ONE :
				return 1 + tree.getChildCount(root);
			case IResource.DEPTH_INFINITE :
				final int[] count = new int[1];
				IElementContentVisitor visitor = (aTree, requestor, elementContents) -> {
					if (phantom || !((ResourceInfo) elementContents).isSet(M_PHANTOM))
						count[0]++;
					return true;
				};
				new ElementTreeIterator(tree, root).iterate(visitor);
				return count[0];
		}
		return 0;
	}

	/*
	 * Creates the given resource in the tree and returns the new resource info object.
	 * If phantom is true, the created element is marked as a phantom.
	 * If there is already be an element in the tree for the given resource
	 * in the given state (i.e., phantom), a CoreException is thrown.
	 * If there is already a phantom in the tree and the phantom flag is false,
	 * the element is overwritten with the new element. (but the synchronization
	 * information is preserved)
	 */
	public ResourceInfo createResource(IResource resource, boolean phantom) throws CoreException {
		return createResource(resource, null, phantom, false, false);
	}

	/**
	 * Creates a resource, honoring update flags requesting that the resource
	 * be immediately made derived, hidden and/or team private
	 */
	public ResourceInfo createResource(IResource resource, int updateFlags) throws CoreException {
		ResourceInfo info = createResource(resource, null, false, false, false);
		if ((updateFlags & IResource.DERIVED) != 0)
			info.set(M_DERIVED);
		if ((updateFlags & IResource.TEAM_PRIVATE) != 0)
			info.set(M_TEAM_PRIVATE_MEMBER);
		if ((updateFlags & IResource.HIDDEN) != 0)
			info.set(M_HIDDEN);
		//		if ((updateFlags & IResource.VIRTUAL) != 0)
		//			info.set(M_VIRTUAL);
		return info;
	}

	/*
	 * Creates the given resource in the tree and returns the new resource info object.
	 * If phantom is true, the created element is marked as a phantom.
	 * If there is already be an element in the tree for the given resource
	 * in the given state (i.e., phantom), a CoreException is thrown.
	 * If there is already a phantom in the tree and the phantom flag is false,
	 * the element is overwritten with the new element. (but the synchronization
	 * information is preserved) If the specified resource info is null, then create
	 * a new one.
	 *
	 * If keepSyncInfo is set to be true, the sync info in the given ResourceInfo is NOT
	 * cleared before being created and thus any sync info already existing at that namespace
	 * (as indicated by an already existing phantom resource) will be lost.
	 */
	public ResourceInfo createResource(IResource resource, ResourceInfo info, boolean phantom, boolean overwrite, boolean keepSyncInfo) throws CoreException {
		info = info == null ? newElement(resource.getType()) : (ResourceInfo) info.clone();
		ResourceInfo original = getResourceInfo(resource.getFullPath(), true, false);
		if (phantom) {
			info.set(M_PHANTOM);
			info.clearModificationStamp();
		}
		// if nothing existed at the destination then just create the resource in the tree
		if (original == null) {
			// we got here from a copy/move. we don't want to copy over any sync info
			// from the source so clear it.
			if (!keepSyncInfo)
				info.setSyncInfo(null);
			tree.createElement(resource.getFullPath(), info);
		} else {
			// if overwrite==true then slam the new info into the tree even if one existed before
			if (overwrite || (!phantom && original.isSet(M_PHANTOM))) {
				// copy over the sync info and flags from the old resource info
				// since we are replacing a phantom with a real resource
				// DO NOT set the sync info dirty flag because we want to
				// preserve the old sync info so its not dirty
				// XXX: must copy over the generic sync info from the old info to the new
				// XXX: do we really need to clone the sync info here?
				if (!keepSyncInfo)
					info.setSyncInfo(original.getSyncInfo(true));
				// mark the markers bit as dirty so we snapshot an empty marker set for
				// the new resource
				info.set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
				tree.setElementData(resource.getFullPath(), info);
			} else {
				String message = NLS.bind(Messages.resources_mustNotExist, resource.getFullPath());
				throw new ResourceException(IResourceStatus.RESOURCE_EXISTS, resource.getFullPath(), message, null);
			}
		}
		return info;
	}

	@Override
	public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= IResource.KEEP_HISTORY;
		return delete(resources, updateFlags, monitor);
	}

	@Override
	public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor) throws CoreException {

		String message = Messages.resources_deleteProblem;
		MultiStatus result = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, message,
				null);
		if (resources.length == 0) {
			return result;
		}
		resources = resources.clone(); // to avoid concurrent changes to this array
		SubMonitor subMonitor = SubMonitor.convert(monitor, message, resources.length);
		try {
			prepareOperation(getRoot(), subMonitor);
			beginOperation(true);
			for (IResource r : resources) {
				Resource resource = (Resource) r;
				if (resource == null) {
					subMonitor.split(1);
					continue;
				}
				try {
					resource.delete(updateFlags, subMonitor.newChild(1));
				} catch (CoreException e) {
					// Don't really care about the exception unless the resource is still around.
					ResourceInfo info = resource.getResourceInfo(false, false);
					if (resource.exists(resource.getFlags(info), false)) {
						message = NLS.bind(Messages.resources_couldnotDelete, resource.getFullPath());
						result.merge(new ResourceStatus(IResourceStatus.FAILED_DELETE_LOCAL, resource.getFullPath(),
								message));
						result.merge(e.getStatus());
					}
				}
				subMonitor.worked(1);
			}
			if (result.matches(IStatus.ERROR))
				throw new ResourceException(result);
			return result;
		} catch (OperationCanceledException e) {
			getWorkManager().operationCanceled();
			throw e;
		} finally {
			subMonitor.done();
			endOperation(getRoot(), true);
		}
	}

	@Override
	public void deleteMarkers(IMarker[] markers) throws CoreException {
		Assert.isNotNull(markers);
		if (markers.length == 0)
			return;
		// clone to avoid outside changes
		markers = markers.clone();
		try {
			prepareOperation(null, null);
			beginOperation(true);
			for (IMarker marker : markers)
				if (marker != null && marker.getResource() != null)
					markerManager.removeMarker(marker.getResource(), marker.getId());
		} finally {
			endOperation(null, false);
		}
	}

	/**
	 * Delete the given resource from the current tree of the receiver.
	 * This method simply removes the resource from the tree.  No cleanup or
	 * other management is done.  Use IResource.delete for proper deletion.
	 * If the given resource is the root, all of its children (i.e., all projects) are
	 * deleted but the root is left.
	 */
	void deleteResource(IResource resource) {
		IPath path = resource.getFullPath();
		if (path.equals(IPath.ROOT)) {
			IProject[] children = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
			for (IProject element : children)
				tree.deleteElement(element.getFullPath());
		} else
			tree.deleteElement(path);
	}

	/**
	 * End an operation (group of resource changes).
	 * Notify interested parties that resource changes have taken place.  All
	 * registered resource change listeners are notified.  If autobuilding is
	 * enabled, a build is run.
	 */
	public void endOperation(ISchedulingRule rule, boolean build) throws CoreException {
		WorkManager workManager = getWorkManager();
		//don't do any end operation work if we failed to check in
		if (workManager.checkInFailed(rule))
			return;
		// This is done in a try finally to ensure that we always decrement the operation count
		// and release the workspace lock.  This must be done at the end because snapshot
		// and "hasChanges" comparison have to happen without interference from other threads.
		boolean hasTreeChanges = false;
		boolean depthOne = false;
		try {
			workManager.setBuild(build);
			// if we are not exiting a top level operation then just decrement the count and return
			depthOne = workManager.getPreparedOperationDepth() == 1;
			if (!(notificationManager.shouldNotify() || depthOne)) {
				notificationManager.requestNotify();
				return;
			}
			// do the following in a try/finally to ensure that the operation tree is nulled at the end
			// as we are completing a top level operation.
			try {
				notificationManager.beginNotify();
				// check for a programming error on using beginOperation/endOperation
				Assert.isTrue(workManager.getPreparedOperationDepth() > 0, "Mismatched begin/endOperation"); //$NON-NLS-1$

				// At this time we need to re-balance the nested operations. It is necessary because
				// build() and snapshot() should not fail if they are called.
				workManager.rebalanceNestedOperations();

				//find out if any operation has potentially modified the tree
				hasTreeChanges = workManager.shouldBuild();
				//double check if the tree has actually changed
				if (hasTreeChanges)
					hasTreeChanges = operationTree != null && ElementTree.hasChanges(tree, operationTree, ResourceComparator.getBuildComparator(), true);
				broadcastPostChange();
				// Request a snapshot if we are sufficiently out of date.
				saveManager.snapshotIfNeeded(hasTreeChanges);
			} finally {
				// make sure the tree is immutable if we are ending a top-level operation.
				if (depthOne) {
					tree.immutable();
					operationTree = null;
				} else
					newWorkingTree();
			}
		} finally {
			workManager.checkOut(rule);
		}
		if (depthOne)
			buildManager.endTopLevel(hasTreeChanges);
	}

	/**
	 * Flush the build order cache for the workspace.  The buildOrder cache contains the total
	 * order of the build configurations in the workspace, including projects not mentioned in
	 * the workspace description.
	 */
	protected void flushBuildOrder() {
		buildOrder = null;
		buildOrderGraph = null;
	}

	@Override
	public void forgetSavedTree(String pluginId) {
		saveManager.forgetSavedTree(pluginId);
	}

	public AliasManager getAliasManager() {
		return aliasManager;
	}

	/**
	 * Returns this workspace's build manager
	 */
	public BuildManager getBuildManager() {
		return buildManager;
	}

	/**
	 * Returns the order in which open projects in this workspace will be built.
	 * The result returned is a list of project buildConfigs, that need to be built
	 * in order to successfully build the active config of every project in this
	 * workspace.
	 * <p>
	 * The build configuration order is based on information specified in the workspace
	 * description. The project build configs are built in the order specified by
	 * <code>IWorkspaceDescription.getBuildOrder</code>; closed or non-existent
	 * projects are ignored and not included in the result. If any open projects are
	 * not specified in this order, they are appended to the end of the build order
	 * sorted by project name (to provide a stable ordering).
	 * </p>
	 * <p>
	 * If <code>IWorkspaceDescription.getBuildOrder</code> is non-null, the default
	 * build order is used (calculated based on references); again, only open projects'
	 * buildConfigs are included in the result.
	 * </p>
	 * <p>
	 * The returned value is cached in the <code>buildOrder</code> field.
	 * </p>
	 *
	 * @return the list of currently open projects active buildConfigs (and the project buildConfigs
	 * they depend on) in the workspace in the order in which they would be built by <code>IWorkspace.build</code>.
	 * @see IWorkspace#build(int, IProgressMonitor)
	 * @see IWorkspaceDescription#getBuildOrder()
	 */
	public IBuildConfiguration[] getBuildOrder() {
		// Return the build order cache.
		if (buildOrder != null)
			return buildOrder;

		// see if a particular build order is specified
		String[] order = description.getBuildOrder(false);
		if (order != null) {
			LinkedHashSet<IBuildConfiguration> configs = new LinkedHashSet<>();

			// convert from project names to active project buildConfigs
			// and eliminate non-existent and closed projects
			for (String element : order) {
				IProject project = getRoot().getProject(element);
				if (project.isAccessible())
					configs.add(((Project) project).internalGetActiveBuildConfig());
			}

			// Add projects not mentioned in the build order to the end, in a sensible reference order
			configs.addAll(Arrays.asList(vertexOrderToProjectBuildConfigOrder(computeActiveBuildConfigOrder()).buildConfigurations));

			// Update the cache - Java 5 volatile memory barrier semantics
			IBuildConfiguration[] bo = new IBuildConfiguration[configs.size()];
			configs.toArray(bo);
			this.buildOrder = bo;
		} else {
			// use default project build order
			// computed for all accessible projects in workspace
			Digraph<IBuildConfiguration> buildGraph = getBuildGraph();
			buildOrder = vertexOrderToProjectBuildConfigOrder(ComputeProjectOrder.computeVertexOrder(buildGraph, IBuildConfiguration.class)).buildConfigurations;
		}

		return buildOrder;
	}

	/**
	 *
	 * @return the build graph, or null if a build order has been specified (in which case, use {@link #getBuildOrder()} directly
	 */
	private Digraph<IBuildConfiguration> getBuildGraph() {
		if (buildOrderGraph != null) {
			return buildOrderGraph;
		}
		String[] order = description.getBuildOrder(false);
		if (order != null) {
			return null;
		}
		buildOrderGraph = computeActiveBuildConfigGraph();
		return buildOrderGraph;
	}

	public CharsetManager getCharsetManager() {
		return charsetManager;
	}

	public ContentDescriptionManager getContentDescriptionManager() {
		return contentDescriptionManager;
	}

	@Override
	public Map<IProject, IProject[]> getDanglingReferences() {
		IProject[] projects = getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		Map<IProject, IProject[]> result = new HashMap<>(projects.length);
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
                        }
			IProject[] refs = ((Project)project).internalGetDescription().getReferencedProjects(false);
			List<IProject> dangling = new ArrayList<>(refs.length);
			for (IProject ref : refs) {
				if (!ref.exists()) {
					dangling.add(ref);
				}
			}
			if (!dangling.isEmpty()) {
				result.put(project, dangling.toArray(new IProject[dangling.size()]));
			}
		}
		return result;
	}

	@Override
	public IWorkspaceDescription getDescription() {
		WorkspaceDescription workingCopy = defaultWorkspaceDescription();
		description.copyTo(workingCopy);
		return workingCopy;
	}

	/**
	 * Returns the current element tree for this workspace
	 */
	public ElementTree getElementTree() {
		return tree;
	}

	public FileSystemResourceManager getFileSystemManager() {
		return fileSystemManager;
	}

	/**
	 * Returns the marker manager for this workspace
	 */
	public MarkerManager getMarkerManager() {
		return markerManager;
	}

	public LocalMetaArea getMetaArea() {
		return localMetaArea;
	}

	protected IMoveDeleteHook getMoveDeleteHook() {
		if (moveDeleteHook == null)
			initializeMoveDeleteHook();
		return moveDeleteHook;
	}

	@Override
	public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
		return filterManager.getFilterDescriptor(filterMatcherId);
	}

	@Override
	public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
		return filterManager.getFilterDescriptors();
	}

	@Override
	public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
		return natureManager.getNatureDescriptor(natureId);
	}

	@Override
	public IProjectNatureDescriptor[] getNatureDescriptors() {
		return natureManager.getNatureDescriptors();
	}

	/**
	 * Returns the nature manager for this workspace.
	 */
	public NatureManager getNatureManager() {
		return natureManager;
	}

	public NotificationManager getNotificationManager() {
		return notificationManager;
	}

	@Override
	public IPathVariableManager getPathVariableManager() {
		return pathVariableManager;
	}

	public IPropertyManager getPropertyManager() {
		return propertyManager;
	}

	/**
	 * Returns the refresh manager for this workspace
	 */
	public RefreshManager getRefreshManager() {
		return refreshManager;
	}

	/**
	 * Returns the resource info for the identified resource.
	 * null is returned if no such resource can be found.
	 * If the phantom flag is true, phantom resources are considered.
	 * If the mutable flag is true, the info is opened for change.
	 *
	 * This method DOES NOT throw an exception if the resource is not found.
	 */
	public ResourceInfo getResourceInfo(IPath path, boolean phantom, boolean mutable) {
		try {
			if (path.segmentCount() == 0) {
				ResourceInfo info = (ResourceInfo) tree.getTreeData();
				Assert.isNotNull(info, "Tree root info must never be null"); //$NON-NLS-1$
				return info;
			}
			ResourceInfo result = null;
			if (!tree.includes(path))
				return null;
			if (mutable)
				result = (ResourceInfo) tree.openElementData(path);
			else
				result = (ResourceInfo) tree.getElementData(path);
			if (result != null && (!phantom && result.isSet(M_PHANTOM)))
				return null;
			return result;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public IWorkspaceRoot getRoot() {
		return defaultRoot;
	}

	@Override
	public IResourceRuleFactory getRuleFactory() {
		//note that the rule factory is created lazily because it
		//requires loading the teamHook extension
		if (ruleFactory == null)
			ruleFactory = new Rules(this);
		return ruleFactory;
	}

	public SaveManager getSaveManager() {
		return saveManager;
	}

	@Override
	public ISynchronizer getSynchronizer() {
		return synchronizer;
	}

	/**
	 * Returns the installed team hook.  Never returns null.
	 */
	protected TeamHook getTeamHook() {
		if (teamHook == null)
			initializeTeamHook();
		return teamHook;
	}

	/**
	 * We should not have direct references to this field. All references should go through
	 * this method.
	 */
	public WorkManager getWorkManager() throws CoreException {
		if (_workManager == null) {
			String message = Messages.resources_shutdown;
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, null);
		}
		return _workManager;
	}

	/**
	 * A move/delete hook hasn't been initialized. Check the extension point and
	 * try to create a new hook if a user has one defined as an extension. Otherwise
	 * use the Core's implementation as the default.
	 */
	protected void initializeMoveDeleteHook() {
		try {
			if (!canCreateExtensions())
				return;
			IConfigurationElement[] configs = Platform.getExtensionRegistry().getConfigurationElementsFor(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_MOVE_DELETE_HOOK);
			// no-one is plugged into the extension point so disable validation
			if (configs == null || configs.length == 0) {
				return;
			}
			// can only have one defined at a time. log a warning
			if (configs.length > 1) {
				//XXX: should provide a meaningful status code
				IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_oneHook, null);
				Policy.log(status);
				return;
			}
			// otherwise we have exactly one hook extension. Try to create a new instance
			// from the user-specified class.
			try {
				IConfigurationElement config = configs[0];
				moveDeleteHook = (IMoveDeleteHook) config.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				//ignore the failure if we are shutting down (expected since extension
				//provider plugin has probably already shut down
				if (canCreateExtensions()) {
					IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_initHook, e);
					Policy.log(status);
				}
			}
		} finally {
			// for now just use Core's implementation
			if (moveDeleteHook == null)
				moveDeleteHook = new MoveDeleteHook();
		}
	}

	/*
	 * Add the project scope to the preference service's default look-up order so
	 * people get it for free
	 */
	private void initializePreferenceLookupOrder() throws CoreException {
		PreferencesService service = PreferencesService.getDefault();
		// put the project scope first on the list
		service.prependScopeToDefaultDefaultLookupOrder(ProjectScope.SCOPE);
		Preferences node = service.getRootNode().node(ProjectScope.SCOPE);
		if (node instanceof ProjectPreferences projectPreferences) {
			projectPreferences.setWorkspace(this);
		} else {
			throw new CoreException(Status.error(MessageFormat.format(
					"Internal error while open workspace, expected ProjectPreferences for the scope {0} but got {1}", //$NON-NLS-1$
					ProjectScope.SCOPE, node.getClass().getSimpleName())));
		}
	}

	/**
	 * A team hook hasn't been initialized. Check the extension point and
	 * try to create a new hook if a user has one defined as an extension.
	 * Otherwise use the Core's implementation as the default.
	 */
	protected void initializeTeamHook() {
		try {
			if (!canCreateExtensions())
				return;
			IConfigurationElement[] configs = Platform.getExtensionRegistry().getConfigurationElementsFor(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_TEAM_HOOK);
			// no-one is plugged into the extension point so disable validation
			if (configs == null || configs.length == 0) {
				return;
			}
			// can only have one defined at a time. log a warning
			if (configs.length > 1) {
				//XXX: should provide a meaningful status code
				IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_oneTeamHook, null);
				Policy.log(status);
				return;
			}
			// otherwise we have exactly one hook extension. Try to create a new instance
			// from the user-specified class.
			try {
				IConfigurationElement config = configs[0];
				teamHook = (TeamHook) config.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				//ignore the failure if we are shutting down (expected since extension
				//provider plugin has probably already shut down
				if (canCreateExtensions()) {
					IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_initTeamHook, e);
					Policy.log(status);
				}
			}
		} finally {
			// default to use Core's implementation
			//create anonymous subclass because TeamHook is abstract
			if (teamHook == null)
				teamHook = new TeamHook(this) {
					// empty
				};
		}
	}

	/**
	 * A file modification validator hasn't been initialized. Check the extension point and
	 * try to create a new validator if a user has one defined as an extension.
	 */
	protected void initializeValidator() {
		shouldValidate = false;
		if (!canCreateExtensions())
			return;
		IConfigurationElement[] configs = Platform.getExtensionRegistry().getConfigurationElementsFor(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_FILE_MODIFICATION_VALIDATOR);
		// no-one is plugged into the extension point so disable validation
		if (configs == null || configs.length == 0) {
			return;
		}
		// can only have one defined at a time. log a warning, disable validation, but continue with
		// the #setContents (e.g. don't throw an exception)
		if (configs.length > 1) {
			//XXX: should provide a meaningful status code
			IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_oneValidator, null);
			Policy.log(status);
			return;
		}
		// otherwise we have exactly one validator extension. Try to create a new instance
		// from the user-specified class.
		try {
			IConfigurationElement config = configs[0];
			validator = (IFileModificationValidator) config.createExecutableExtension("class"); //$NON-NLS-1$
			shouldValidate = true;
		} catch (CoreException e) {
			//ignore the failure if we are shutting down (expected since extension
			//provider plugin has probably already shut down
			if (canCreateExtensions()) {
				IStatus status = new ResourceStatus(IStatus.ERROR, 1, null, Messages.resources_initValidator, e);
				Policy.log(status);
			}
		}
	}

	public WorkspaceDescription internalGetDescription() {
		return description;
	}

	@Override
	public boolean isAutoBuilding() {
		return description.isAutoBuilding();
	}

	public boolean isOpen() {
		return openFlag;
	}

	@Override
	public boolean isTreeLocked() {
		return treeLocked == Thread.currentThread();
	}

	/**
	 * Link the given tree into the receiver's tree at the specified resource.
	 */
	protected void linkTrees(IPath path, ElementTree[] newTrees) {
		tree = tree.mergeDeltaChain(path, newTrees);
	}

	@Override
	public IProjectDescription loadProjectDescription(InputStream stream) throws CoreException {
		IProjectDescription result = new ProjectDescriptionReader(this).read(new InputSource(stream));
		if (result == null) {
			String message = NLS.bind(Messages.resources_errorReadProject, stream.toString());
			IStatus status = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, message, null);
			throw new ResourceException(status);
		}
		return result;
	}

	@Override
	public IProjectDescription loadProjectDescription(IPath path) throws CoreException {
		IProjectDescription result = null;
		IOException e = null;
		try {
			result = new ProjectDescriptionReader(this).read(path);
			if (result != null) {
				// check to see if we are using in the default area or not. use java.io.File for
				// testing equality because it knows better w.r.t. drives and case sensitivity
				IPath user = path.removeLastSegments(1);
				IPath platform = getRoot().getLocation().append(result.getName());
				if (!user.toFile().equals(platform.toFile()))
					result.setLocation(user);
			}
		} catch (IOException ex) {
			e = ex;
		}
		if (result == null || e != null) {
			String message = NLS.bind(Messages.resources_errorReadProject, path.toOSString());
			IStatus status = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, message, e);
			throw new ResourceException(status);
		}
		return result;
	}

	@Override
	public IStatus move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= IResource.KEEP_HISTORY;
		return move(resources, destination, updateFlags, monitor);
	}

	@Override
	public IStatus move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isLegal(resources != null);

		if (resources.length == 0) {
			return Status.OK_STATUS;
		}
		resources = resources.clone(); // to avoid concurrent changes to this array

		String message = Messages.resources_moving_0;
		SubMonitor subMonitor = SubMonitor.convert(monitor, message, resources.length);
		IPath parentPath = null;
		message = Messages.resources_moveProblem;
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, message,
				null);
		try {
			prepareOperation(getRoot(), subMonitor);
			beginOperation(true);
			for (int i = 0; i < resources.length; i++) {
				Resource resource = (Resource) resources[i];
				if (resource == null || isDuplicate(resources, i)) {
					subMonitor.split(1);
					continue;
				}
				// test siblings
				if (parentPath == null)
					parentPath = resource.getFullPath().removeLastSegments(1);
				if (parentPath.equals(resource.getFullPath().removeLastSegments(1))) {
					// test move requirements
					try {
						IStatus requirements = resource.checkMoveRequirements(destination.append(resource.getName()),
								resource.getType(), updateFlags);
						if (requirements.isOK()) {
							try {
								resource.move(destination.append(resource.getName()), updateFlags,
										subMonitor.newChild(1));
							} catch (CoreException e) {
								status.merge(e.getStatus());
							}
						} else {
							subMonitor.worked(1);
							status.merge(requirements);
						}
					} catch (CoreException e) {
						subMonitor.worked(1);
						status.merge(e.getStatus());
					}
				} else {
					subMonitor.worked(1);
					message = NLS.bind(Messages.resources_notChild, resource.getFullPath(), parentPath);
					status.merge(new ResourceStatus(IResourceStatus.OPERATION_FAILED, resource.getFullPath(), message));
				}
			}
		} catch (OperationCanceledException e) {
			getWorkManager().operationCanceled();
			throw e;
		} finally {
			subMonitor.done();
			endOperation(getRoot(), true);
		}
		if (status.matches(IStatus.ERROR))
			throw new ResourceException(status);
		return status.isOK() ? (IStatus) Status.OK_STATUS : (IStatus) status;
	}

	/**
	 * Moves this resource's subtree to the destination. This operation should only be
	 * used by move methods. Destination must be a valid destination for this resource.
	 * The keepSyncInfo boolean is used to indicated whether or not the sync info should
	 * be moved from the source to the destination.
	 */

	/* package */
	void move(Resource source, IPath destination, int depth, int updateFlags, boolean keepSyncInfo) throws CoreException {
		// overlay the tree at the destination path, preserving any important info
		// in any already existing resource information
		copyTree(source, destination, depth, updateFlags, keepSyncInfo, true, source.getType() == IResource.PROJECT);
		source.fixupAfterMoveSource();
	}

	/**
	 * Create and return a new tree element of the given type.
	 */
	protected ResourceInfo newElement(int type) {
		ResourceInfo result = null;
		switch (type) {
			case IResource.FILE :
			case IResource.FOLDER :
				result = new ResourceInfo();
				break;
			case IResource.PROJECT :
				result = new ProjectInfo();
				break;
			case IResource.ROOT :
				result = new RootInfo();
				break;
		}
		result.setNodeId(nextNodeId());
		updateModificationStamp(result);
		result.setType(type);
		return result;
	}

	@Override
	public IBuildConfiguration newBuildConfig(String projectName, String configName) {
		return new BuildConfiguration(getRoot().getProject(projectName), configName);
	}

	@Override
	public IProjectDescription newProjectDescription(String projectName) {
		IProjectDescription result = new ProjectDescription();
		result.setName(projectName);
		return result;
	}

	public Resource newResource(IPath path, int type) {
		String message;
		switch (type) {
			case IResource.FOLDER :
				if (path.segmentCount() < ICoreConstants.MINIMUM_FOLDER_SEGMENT_LENGTH) {
					message = "Path must include project and resource name: " + path.toString(); //$NON-NLS-1$
					Assert.isLegal(false, message);
				}
				return new Folder(path.makeAbsolute(), this);
			case IResource.FILE :
				if (path.segmentCount() < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
					message = "Path must include project and resource name: " + path.toString(); //$NON-NLS-1$
					Assert.isLegal(false, message);
				}
				return new File(path.makeAbsolute(), this);
			case IResource.PROJECT :
				return (Resource) getRoot().getProject(path.lastSegment());
			case IResource.ROOT :
				return (Resource) getRoot();
		}
		Assert.isLegal(false);
		// will never get here because of assertion.
		return null;
	}

	/**
	 * Opens a new mutable element tree layer, thus allowing
	 * modifications to the tree.
	 */
	public ElementTree newWorkingTree() {
		// synchronized for atomic swap. Should have already synchronized by
		// getWorkManager().checkIn/checkout, but it's not guaranteed
		synchronized (this) {
			tree = tree.newEmptyDelta();
			return tree;
		}
	}

	/**
	 * Returns the next, previously unassigned, marker id.
	 */
	protected long nextMarkerId() {
		return nextMarkerId.getAndIncrement();
	}

	protected long nextNodeId() {
		return nextNodeId.getAndIncrement();
	}

	/**
	 * Opens this workspace using the data at its location in the local file system.
	 * This workspace must not be open.
	 * If the operation succeeds, the result will detail any serious
	 * (but non-fatal) problems encountered while opening the workspace.
	 * The status code will be <code>OK</code> if there were no problems.
	 * An exception is thrown if there are fatal problems opening the workspace,
	 * in which case the workspace is left closed.
	 * <p>
	 * This method is long-running; progress and cancellation are provided
	 * by the given progress monitor.
	 * </p>
	 *
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 * @return status with code <code>OK</code> if no problems;
	 *     otherwise status describing any serious but non-fatal problems.
	 *
	 * @exception CoreException if the workspace could not be opened.
	 * Reasons include:
	 * <ul>
	 * <li> There is no valid workspace structure at the given location
	 *      in the local file system.</li>
	 * <li> The workspace structure on disk appears to be hopelessly corrupt.</li>
	 * </ul>
	 * @see ResourcesPlugin#getWorkspace()
	 */
	public IStatus open(IProgressMonitor monitor) throws CoreException {
		if (!localMetaArea.hasSavedWorkspace()) {
			localMetaArea.createMetaArea();
		}
		PlatformURLResourceConnection.startup(getRoot().getLocation());

		// This method is not inside an operation because it is the one responsible for
		// creating the WorkManager object (who takes care of operations).
		String message = Messages.resources_workspaceOpen;
		Assert.isTrue(!isOpen(), message);
		if (!getMetaArea().hasSavedWorkspace()) {
			message = Messages.resources_readWorkspaceMeta;
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, Platform.getLocation(), message, null);
		}
		description = new WorkspacePreferences();

		// Set explicit workspace encoding if no projects exist in the workspace
		if (!localMetaArea.hasSavedProjects()) {
			setExplicitWorkspaceEncoding();
		}
		initializePreferenceLookupOrder();

		// create root location
		localMetaArea.locationFor(getRoot()).toFile().mkdirs();

		SubMonitor subMonitor = SubMonitor.convert(null);
		startup(subMonitor);
		// restart the notification manager so it is initialized with the right tree
		notificationManager.startup(null);
		openFlag = true;
		if (crashed || refreshRequested()) {
			try {
				refreshManager.refresh(getRoot());
			} catch (RuntimeException e) {
				//don't fail entire open if refresh failed, just report as warning
				return new ResourceStatus(IResourceStatus.INTERNAL_ERROR, IPath.ROOT,
						Messages.resources_errorMultiRefresh, e);
			}
		}
		//finally register a string pool participant
		stringPoolJob = new StringPoolJob();
		stringPoolJob.addStringPoolParticipant(saveManager, getRoot());
		return Status.OK_STATUS;
	}

	/**
	 * Writes explicit workspace encoding to workspace preferences
	 * ("org.eclipse.core.resources/encoding"). If the user started Eclipse with
	 * explicit encoding set (-Dfile.encoding=XYZ), this encoding used, otherwise
	 * UTF-8.
	 * <p>
	 * With that, if user did not specified any encoding, all projects created in
	 * this workspace will get explicit UTF-8 encoding set.
	 */
	private void setExplicitWorkspaceEncoding() {
		// ResourcesPlugin.getEncoding() defaults to JVM "file.encoding" value
		// which is *always* set in the JVM, so it can't be used.
		// Therefore check preferences directly (encoding could be set even in a new
		// workspace via product customization file, like
		// org.eclipse.core.resources/encoding=ISO-8859-1)
		String defaultEncoding = Platform.getPreferencesService().getString(ResourcesPlugin.PI_RESOURCES,
				ResourcesPlugin.PREF_ENCODING, /* default */null, /* all scopes */null);

		if (defaultEncoding == null || defaultEncoding.isBlank()) {
			// Check if -Dfile.encoding= startup argument was given to Eclipse's JVM
			List<String> commandLineArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
			for (String arg : commandLineArgs) {
				if (arg.startsWith("-Dfile.encoding=")) { //$NON-NLS-1$
					defaultEncoding = arg.substring("-Dfile.encoding=".length()); //$NON-NLS-1$
					// continue, it can be specified multiple times, last one wins.
				}
			}
			// Now we are sure user didn't specified encoding, so we can set
			// default encoding for the workspace
			if (defaultEncoding == null || defaultEncoding.isBlank()) {
				defaultEncoding = "UTF-8"; //$NON-NLS-1$
			}

			// Persist encoding to the workspace preferences - if same workspace is started
			// later by other Eclipse instance without product customization or system
			// property or with differently set properties, we want keep same encoding as on
			// the first startup. Otherwise users may end up with projects in same workspace
			// created with different encodings. This is surely supported but would surprise
			// users sooner or later. So better to set it first time, users can always
			// change it later if needed.
			IEclipsePreferences workspacePrefs = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
			workspacePrefs.put(ResourcesPlugin.PREF_ENCODING, defaultEncoding);
			Assert.isTrue(defaultEncoding.equals(ResourcesPlugin.getEncoding()));
		}
	}

	/**
	 * Called before checking the pre-conditions of an operation.  Optionally supply
	 * a scheduling rule to determine when the operation is safe to run.  If a scheduling
	 * rule is supplied, this method will block until it is safe to run.
	 *
	 * @param rule the scheduling rule that describes what this operation intends to modify.
	 */
	public void prepareOperation(ISchedulingRule rule, IProgressMonitor monitor) throws CoreException {
		try {
			//make sure autobuild is not running if it conflicts with this operation
			ISchedulingRule buildRule = getRuleFactory().buildRule();
			if (rule != null && buildRule != null && (rule.isConflicting(buildRule) || buildRule.isConflicting(rule)))
				buildManager.interrupt();
		} finally {
			getWorkManager().checkIn(rule, monitor);
		}
		if (!isOpen()) {
			String message = Messages.resources_workspaceClosed;
			throw new ResourceException(IResourceStatus.OPERATION_FAILED, null, message, null);
		}
	}

	protected boolean refreshRequested() {
		String[] args = Platform.getCommandLineArgs();
		for (String arg : args)
			if (arg.equalsIgnoreCase(REFRESH_ON_STARTUP))
				return true;
		return false;
	}

	@Override
	public void removeResourceChangeListener(IResourceChangeListener listener) {
		notificationManager.removeListener(listener);
	}

	@Deprecated
	@Override
	public void removeSaveParticipant(Plugin plugin) {
		Assert.isNotNull(plugin, "Plugin must not be null"); //$NON-NLS-1$
		saveManager.removeParticipant(plugin.getBundle().getSymbolicName());
	}

	@Override
	public void removeSaveParticipant(String pluginId) {
		Assert.isNotNull(pluginId, "Plugin id must not be null"); //$NON-NLS-1$
		saveManager.removeParticipant(pluginId);
	}

	@Override
	public void run(ICoreRunnable action, IProgressMonitor monitor) throws CoreException {
		run(action, defaultRoot, IWorkspace.AVOID_UPDATE, monitor);
	}

	@Override
	public void run(ICoreRunnable action, ISchedulingRule rule, int options, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Policy.totalWork); // $NON-NLS-1$
		int depth = -1;
		boolean avoidNotification = (options & IWorkspace.AVOID_UPDATE) != 0;
		try {
			prepareOperation(rule, subMonitor);
			beginOperation(true);
			if (avoidNotification)
				avoidNotification = notificationManager.beginAvoidNotify();
			depth = getWorkManager().beginUnprotected();
			action.run(subMonitor.newChild(Policy.opWork));
		} catch (OperationCanceledException e) {
			getWorkManager().operationCanceled();
			throw e;
		} catch (CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.CANCEL)
				getWorkManager().operationCanceled();
			throw e;
		} finally {
			subMonitor.done();
			if (avoidNotification)
				notificationManager.endAvoidNotify();
			if (depth >= 0)
				getWorkManager().endUnprotected(depth);
			endOperation(rule, false);
		}
	}

	@Override
	public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
		run((ICoreRunnable) action, defaultRoot, IWorkspace.AVOID_UPDATE, monitor);
	}

	@Override
	public void run(IWorkspaceRunnable action, ISchedulingRule rule, int options, IProgressMonitor monitor) throws CoreException {
		run((ICoreRunnable) action, rule, options, monitor);
	}

	@Override
	public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
		return this.save(full, false, monitor);
	}

	public IStatus save(boolean full, boolean keepConsistencyWhenCanceled, IProgressMonitor monitor) throws CoreException {
		String message;
		if (full) {
			//according to spec it is illegal to start a full save inside another operation
			if (getWorkManager().isLockAlreadyAcquired()) {
				message = Messages.resources_saveOp;
				throw new ResourceException(IResourceStatus.OPERATION_FAILED, null, message, new IllegalStateException());
			}
			return saveManager.save(ISaveContext.FULL_SAVE, keepConsistencyWhenCanceled, null, monitor);
		}
		// A snapshot was requested.  Start an operation (if not already started) and
		// signal that a snapshot should be done at the end.
		try {
			prepareOperation(getRoot(), monitor);
			beginOperation(false);
			saveManager.requestSnapshot();
			message = Messages.resources_snapRequest;
			return new ResourceStatus(IStatus.OK, message);
		} finally {
			endOperation(getRoot(), false);
		}
	}

	public void setCrashed(boolean value) {
		crashed = value;
		if (crashed) {
			String msg = "The workspace exited with unsaved changes in the previous session; refreshing workspace to recover changes."; //$NON-NLS-1$
			Policy.log(new ResourceStatus(ICoreConstants.CRASH_DETECTED, msg));
			if (Policy.DEBUG)
				Policy.debug(msg);
		}
	}

	/**
	 * @return true if workspace crash in previous session was detected at startup
	 */
	public boolean isCrashed() {
		return crashed;
	}

	@Override
	public void setDescription(IWorkspaceDescription value) {
		// if both the old and new description's build orders are null, leave the
		// workspace's build order slot because it is caching the computed order.
		// Otherwise, set the slot to null to force recomputing or building from the description.
		WorkspaceDescription newDescription = (WorkspaceDescription) value;
		String[] newOrder = newDescription.getBuildOrder(false);
		if (description.getBuildOrder(false) != null || newOrder != null) {
			flushBuildOrder();
		}
		description.copyFrom(newDescription);
		ResourcesPlugin.getPlugin().savePluginPreferences();
	}

	public void setTreeLocked(boolean locked) {
		Assert.isTrue(!locked || treeLocked == null, "The workspace tree is already locked"); //$NON-NLS-1$
		treeLocked = locked ? Thread.currentThread() : null;
	}

	/**
	 * Shuts down the workspace managers.
	 */
	protected void shutdown(IProgressMonitor monitor) throws CoreException {
		IManager[] managers = { buildManager, propertyManager, pathVariableManager, charsetManager, fileSystemManager,
				markerManager, _workManager, aliasManager, refreshManager, contentDescriptionManager, natureManager,
				filterManager };
		SubMonitor subMonitor = SubMonitor.convert(monitor, managers.length); // $NON-NLS-1$
		try {
			String message = Messages.resources_shutdownProblems;
			MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, message,
					null);
			// best effort to shutdown every object and free resources
			for (IManager manager : managers) {
				if (manager == null)
					subMonitor.worked(1);
				else {
					try {
						manager.shutdown(subMonitor.newChild(1));
					} catch (Exception e) {
						message = Messages.resources_shutdownProblems;
						status.add(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR,
								message, e));
					}
				}
			}
			buildManager = null;
			notificationManager = null;
			propertyManager = null;
			pathVariableManager = null;
			fileSystemManager = null;
			markerManager = null;
			synchronizer = null;
			saveManager = null;
			_workManager = null;
			aliasManager = null;
			refreshManager = null;
			charsetManager = null;
			contentDescriptionManager = null;
			if (!status.isOK())
				throw new CoreException(status);
		} finally {
			subMonitor.done();
		}
	}

	@Override
	public String[] sortNatureSet(String[] natureIds) {
		return natureManager.sortNatureSet(natureIds);
	}

	/**
	 * Starts all the workspace manager classes.
	 */
	protected void startup(IProgressMonitor monitor) throws CoreException {
		// ensure the tree is locked during the startup notification
		try {
			_workManager = new WorkManager(this);
			_workManager.startup(null);
			fileSystemManager = new FileSystemResourceManager(this);
			fileSystemManager.startup(monitor);
			pathVariableManager = new PathVariableManager();
			pathVariableManager.startup(null);
			natureManager = new NatureManager(this);
			natureManager.startup(null);
			filterManager = new FilterTypeManager();
			filterManager.startup(null);
			buildManager = new BuildManager(this, getWorkManager().getLock());
			buildManager.startup(null);
			notificationManager = new NotificationManager(this);
			notificationManager.startup(null);
			markerManager = new MarkerManager(this);
			markerManager.startup(null);
			synchronizer = new Synchronizer(this);
			saveManager = new SaveManager(this);
			saveManager.startup(null);
			propertyManager = new PropertyManager2(this);
			propertyManager.startup(monitor);
			charsetManager = new CharsetManager(this);
			charsetManager.startup(null);
			contentDescriptionManager = new ContentDescriptionManager(this);
			contentDescriptionManager.startup(null);
			//must start after save manager, because (read) access to tree is needed
			//must start after other managers to avoid potential cyclic dependency on uninitialized managers (see bug 316182)
			//must start before alias manager (see bug 94829)
			refreshManager = new RefreshManager(this);
			refreshManager.startup(null);
			//must start at the end to avoid potential cyclic dependency on other uninitialized managers (see bug 369177)
			aliasManager = new AliasManager(this);
			aliasManager.startup(null);
		} finally {
			//unlock tree even in case of failure, otherwise shutdown will also fail
			treeLocked = null;
			_workManager.postWorkspaceStartup();
		}
	}

	/**
	 * Returns a string representation of this working state's
	 * structure suitable for debug purposes.
	 */
	public String toDebugString() {
		final StringBuilder buffer = new StringBuilder("\nDump of " + this + ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("  parent: " + tree.getParent()); //$NON-NLS-1$
		IElementContentVisitor visitor = (aTree, requestor, elementContents) -> {
			buffer.append("\n  " + requestor.requestPath() + ": " + elementContents); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		};
		new ElementTreeIterator(tree, IPath.ROOT).iterate(visitor);
		return buffer.toString();
	}

	/** for debugging only **/
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[localMetaArea=" + localMetaArea.getLocation() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void updateModificationStamp(ResourceInfo info) {
		info.incrementModificationStamp();
	}

	@Override
	public IStatus validateEdit(final IFile[] files, final Object context) {
		// if validation is turned off then just return
		if (!shouldValidate) {
			String message = Messages.resources_readOnly2;
			MultiStatus result = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.READ_ONLY_LOCAL, message, null);
			for (IFile file : files) {
				if (file.isReadOnly()) {
					IPath filePath = file.getFullPath();
					message = NLS.bind(Messages.resources_readOnly, filePath);
					result.add(new ResourceStatus(IResourceStatus.READ_ONLY_LOCAL, filePath, message));
				}
			}
			return result.getChildren().length == 0 ? Status.OK_STATUS : (IStatus) result;
		}
		// first time through the validator hasn't been initialized so try and create it
		if (validator == null)
			initializeValidator();
		// we were unable to initialize the validator. Validation has been turned off and
		// a warning has already been logged so just return.
		if (validator == null)
			return Status.OK_STATUS;
		// otherwise call the API and throw an exception if appropriate
		final IStatus[] status = new IStatus[1];
		ISafeRunnable body = new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				status[0] = new ResourceStatus(IStatus.ERROR, null, Messages.resources_errorValidator, exception);
			}

			@Override
			public void run() throws Exception {
				Object c = context;
				//must null any reference to FileModificationValidationContext for backwards compatibility
				if (!(validator instanceof FileModificationValidator))
					if (c instanceof FileModificationValidationContext)
						c = null;
				status[0] = validator.validateEdit(files, c);
			}
		};
		SafeRunner.run(body);
		return status[0];
	}

	@Override
	public IStatus validateLinkLocation(IResource resource, IPath unresolvedLocation) {
		return locationValidator.validateLinkLocation(resource, unresolvedLocation);
	}

	@Override
	public IStatus validateLinkLocationURI(IResource resource, URI unresolvedLocation) {
		return locationValidator.validateLinkLocationURI(resource, unresolvedLocation);
	}

	@Override
	public IStatus validateName(String segment, int type) {
		return locationValidator.validateName(segment, type);
	}

	@Override
	public IStatus validateNatureSet(String[] natureIds) {
		return natureManager.validateNatureSet(natureIds);
	}

	@Override
	public IStatus validatePath(String path, int type) {
		return locationValidator.validatePath(path, type);
	}

	@Override
	public IStatus validateProjectLocation(IProject context, IPath location) {
		return locationValidator.validateProjectLocation(context, location);
	}

	@Override
	public IStatus validateProjectLocationURI(IProject project, URI location) {
		return locationValidator.validateProjectLocationURI(project, location);
	}

	/**
	 * Internal method. To be called only from the following methods:
	 * <ul>
	 * <li><code>IFile#appendContents</code></li>
	 * <li><code>IFile#setContents(InputStream, boolean, boolean, IProgressMonitor)</code></li>
	 * <li><code>IFile#setContents(IFileState, boolean, boolean, IProgressMonitor)</code></li>
	 * </ul>
	 *
	 * @see IFileModificationValidator#validateSave(IFile)
	 */
	protected void validateSave(final IFile file) throws CoreException {
		// if validation is turned off then just return
		if (!shouldValidate)
			return;
		// first time through the validator hasn't been initialized so try and create it
		if (validator == null)
			initializeValidator();
		// we were unable to initialize the validator. Validation has been turned off and
		// a warning has already been logged so just return.
		if (validator == null)
			return;
		// otherwise call the API and throw an exception if appropriate
		final IStatus[] status = new IStatus[1];
		ISafeRunnable body = new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				status[0] = new ResourceStatus(IStatus.ERROR, null, Messages.resources_errorValidator, exception);
			}

			@Override
			public void run() throws Exception {
				status[0] = validator.validateSave(file);
			}
		};
		SafeRunner.run(body);
		if (!status[0].isOK())
			throw new ResourceException(status[0]);
	}

	@Override
	public IStatus validateFiltered(IResource resource) {
		try {
			if (((Resource) resource).isFilteredWithException(true))
				return new ResourceStatus(IStatus.ERROR, Messages.resources_errorResourceIsFiltered);
		} catch (CoreException e) {
			// if we can't validate it, we return OK
		}
		return Status.OK_STATUS;
	}
}
