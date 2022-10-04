/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Francis Lynch (Wind River) - [301563] Save and load tree snapshots
 *     Francis Lynch (Wind River) - [305718] Allow reading snapshot into renamed project
 *     Baltasar Belyavsky (Texas Instruments) - [361675] Order mismatch when saving/restoring workspace trees
 *     Broadcom Corporation - ongoing development
 *     Sergey Prigogin (Google) - [437005] Out-of-date .snap file prevents Eclipse from running
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.*;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.zip.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.events.*;
import org.eclipse.core.internal.localstore.*;
import org.eclipse.core.internal.utils.*;
import org.eclipse.core.internal.watson.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class SaveManager implements IElementInfoFlattener, IManager, IStringPoolParticipant {
	class MasterTable extends Properties {
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Object put(Object key, Object value) {
			Object prev = super.put(key, value);
			if (prev != null && ROOT_SEQUENCE_NUMBER_KEY.equals(key)) {
				int prevSeqNum = Integer.parseInt((String) prev);
				int currSeqNum = Integer.parseInt((String) value);
				if (prevSeqNum > currSeqNum) {
					//revert last put operation
					super.put(key, prev);
					//notify about the problem, do not throw exception but add the exception to know where it occurred
					String message = "Cannot set lower sequence number for root (previous: " + prevSeqNum + ", new: " + currSeqNum + "). Ignoring the new value."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					Policy.log(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR, message, new IllegalArgumentException(message)));
				}
			}
			return prev;
		}
	}

	protected static final String ROOT_SEQUENCE_NUMBER_KEY = Path.ROOT + LocalMetaArea.F_TREE;
	protected static final String CLEAR_DELTA_PREFIX = "clearDelta_"; //$NON-NLS-1$
	protected static final String DELTA_EXPIRATION_PREFIX = "deltaExpiration_"; //$NON-NLS-1$
	protected static final int DONE_SAVING = 3;

	/**
	 * The minimum delay, in milliseconds, between workspace snapshots
	 */
	private static final long MIN_SNAPSHOT_DELAY = 1000 * 30L; //30 seconds

	/**
	 * The number of empty operations that are equivalent to a single non-
	 * trivial operation.
	 */
	protected static final int NO_OP_THRESHOLD = 20;

	/** constants */
	protected static final int PREPARE_TO_SAVE = 1;
	protected static final int ROLLBACK = 4;
	protected static final String SAVE_NUMBER_PREFIX = "saveNumber_"; //$NON-NLS-1$
	protected static final int SAVING = 2;
	protected ElementTree lastSnap;
	protected final MasterTable masterTable;

	/**
	 * A flag indicating that a save operation is occurring.  This is a signal
	 * that snapshot should not be scheduled if a nested operation occurs during
	 * save.
	 */
	private volatile boolean isSaving = false;

	/**
	 * The number of empty (non-changing) operations since the last snapshot.
	 */
	protected int noopCount = 0;
	/**
	 * The number of non-trivial operations since the last snapshot.
	 */
	protected int operationCount = 0;

	// Count up the time taken for all saves/snaps on markers and sync info
	protected long persistMarkers = 0l;
	protected long persistSyncInfo = 0l;

	/**
	 * In-memory representation of plugins saved state. Maps String (plugin id)-&gt; SavedState.
	 * This map is accessed from API that is not synchronized, so it requires
	 * independent synchronization. This is accomplished using a synchronized
	 * wrapper map.
	 */
	protected Map<String, SavedState> savedStates;

	/**
	 * Ids of plugins that participate on a workspace save. Maps String (plugin id)-&gt; ISaveParticipant.
	 * This map is accessed from API that is not synchronized, so it requires
	 * independent synchronization. This is accomplished using a synchronized
	 * wrapper map.
	 */
	protected Map<String, ISaveParticipant> saveParticipants;

	protected final DelayedSnapshotJob snapshotJob;

	protected volatile boolean snapshotRequested;
	private IStatus snapshotRequestor;
	protected Workspace workspace;
	private Set<Entry<Object, Object>> savedState;
	//declare debug messages as fields to get sharing
	private static final String DEBUG_START = " starting..."; //$NON-NLS-1$
	private static final String DEBUG_FULL_SAVE = "Full save on workspace: "; //$NON-NLS-1$
	private static final String DEBUG_PROJECT_SAVE = "Save on project "; //$NON-NLS-1$
	private static final String DEBUG_SNAPSHOT = "Snapshot: "; //$NON-NLS-1$
	private static final int TREE_BUFFER_SIZE = 1024 * 64;//64KB buffer

	public SaveManager(Workspace workspace) {
		this.workspace = workspace;
		this.masterTable = new MasterTable();
		this.snapshotJob = new DelayedSnapshotJob(this, workspace);
		snapshotRequested = false;
		snapshotRequestor = null;
		saveParticipants = Collections.synchronizedMap(new HashMap<>(10));
	}

	public ISavedState addParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
		// If the plugin was already registered as a save participant we return null
		if (saveParticipants.put(pluginId, participant) != null)
			return null;
		SavedState state = savedStates.get(pluginId);
		if (state != null) {
			if (isDeltaCleared(pluginId)) {
				// this plugin was marked not to receive deltas
				state.forgetTrees();
				removeClearDeltaMarks(pluginId);
			} else {
				try {
					// thread safety: (we need to guarantee that the tree is immutable when computing deltas)
					// so, the tree inside the saved state needs to be immutable
					workspace.prepareOperation(null, null);
					workspace.beginOperation(true);
					state.newTree = workspace.getElementTree();
				} finally {
					workspace.endOperation(null, false);
				}
				return state;
			}
		}
		// if the plug-in has a previous save number, we return a state, otherwise we return null
		if (getSaveNumber(pluginId) > 0)
			return new SavedState(workspace, pluginId, null, null);
		return null;
	}

	protected void broadcastLifecycle(final int lifecycle, Map<String, SaveContext> contexts, final MultiStatus warnings, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, contexts.size());
		try {
			for (final Iterator<Map.Entry<String, SaveContext>> it = contexts.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, SaveContext> entry = it.next();
				String pluginId = entry.getKey();
				final ISaveParticipant participant = saveParticipants.get(pluginId);
				// save participants can be removed concurrently
				if (participant == null) {
					subMonitor.worked(1);
					continue;
				}
				final SaveContext context = entry.getValue();
				/* Be extra careful when calling lifecycle method on arbitrary plugin */
				ISafeRunnable code = new ISafeRunnable() {

					@Override
					public void handleException(Throwable e) {
						String message = Messages.resources_saveProblem;
						IStatus status = new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES,
								IResourceStatus.INTERNAL_ERROR, message, e);
						warnings.add(status);

						/* Remove entry for defective plug-in from this save operation */
						it.remove();
					}

					@Override
					public void run() throws Exception {
						executeLifecycle(lifecycle, participant, context);
					}
				};
				SafeRunner.run(code);
				subMonitor.worked(1);
			}
		} finally {
			subMonitor.done();
		}
	}

	/**
	 * Remove the delta expiration timestamp from the master table, either
	 * because the saved state has been processed, or the delta has expired.
	 */
	protected void clearDeltaExpiration(String pluginId) {
		masterTable.remove(DELTA_EXPIRATION_PREFIX + pluginId);
	}

	protected void cleanMasterTable() {
		//remove tree file entries for everything except closed projects
		for (Iterator<Object> it = masterTable.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			if (!key.endsWith(LocalMetaArea.F_TREE))
				continue;
			String prefix = key.substring(0, key.length() - LocalMetaArea.F_TREE.length());
			//always save the root tree entry
			if (prefix.equals(Path.ROOT.toString()))
				continue;
			IProject project = workspace.getRoot().getProject(prefix);
			if (!project.exists() || project.isOpen())
				it.remove();
		}
		savedState = null;
		IPath location = workspace.getMetaArea().getSafeTableLocationFor(ResourcesPlugin.PI_RESOURCES);
		IPath backup = workspace.getMetaArea().getBackupLocationFor(location);
		try {
			saveMasterTable(ISaveContext.FULL_SAVE, backup);
		} catch (CoreException e) {
			Policy.log(e.getStatus());
			backup.toFile().delete();
			return;
		}
		if (location.toFile().exists() && !location.toFile().delete())
			return;
		try {
			saveMasterTable(ISaveContext.FULL_SAVE, location);
		} catch (CoreException e) {
			Policy.log(e.getStatus());
			location.toFile().delete();
			return;
		}
		backup.toFile().delete();
	}

	/**
	 * Marks the current participants to not receive deltas next time they are registered
	 * as save participants. This is done in order to maintain consistency if we crash
	 * after a snapshot. It would force plug-ins to rebuild their state.
	 */
	protected void clearSavedDelta() {
		synchronized (saveParticipants) {
			for (String pluginId : saveParticipants.keySet()) {
				masterTable.setProperty(CLEAR_DELTA_PREFIX + pluginId, "true"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Collects the set of ElementTrees we are still interested in,
	 * and removes references to any other trees.
	 */
	protected void collapseTrees(Map<String, SaveContext> contexts) throws CoreException {
		//collect trees we're interested in

		//forget saved trees, if they are not used by registered participants
		synchronized (savedStates) {
			for (SaveContext context : contexts.values()) {
				forgetSavedTree(context.getPluginId());
			}
		}

		//trees for plugin saved states
		ArrayList<ElementTree> trees = new ArrayList<>();
		synchronized (savedStates) {
			for (SavedState state : savedStates.values()) {
				if (state.oldTree != null) {
					trees.add(state.oldTree);
				}
			}
		}

		//trees for builders
		IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects) {
			if (project.isOpen()) {
				ArrayList<BuilderPersistentInfo> builderInfos = workspace.getBuildManager().createBuildersPersistentInfo(project);
				if (builderInfos != null) {
					for (BuilderPersistentInfo info : builderInfos) {
						trees.add(info.getLastBuiltTree());
					}
				}
			}
		}

		//no need to collapse if there are no trees at this point
		if (trees.isEmpty())
			return;

		//the complete tree
		trees.add(workspace.getElementTree());

		//collapse the trees
		//sort trees in topological order, and set the parent of each
		//tree to its parent in the topological ordering.
		ElementTree[] treeArray = new ElementTree[trees.size()];
		trees.toArray(treeArray);
		ElementTree[] sorted = sortTrees(treeArray);
		// if there was a problem sorting the tree, bail on trying to collapse.
		// We will be able to GC the layers at a later time.
		if (sorted == null)
			return;
		for (int i = 1; i < sorted.length; i++)
			sorted[i].collapseTo(sorted[i - 1]);
	}

	protected void commit(Map<String, SaveContext> contexts) throws CoreException {
		for (SaveContext saveContext : contexts.values())
			saveContext.commit();
	}

	/**
	 * Given a collection of save participants, compute the collection of
	 * <code>SaveContexts</code> to use during the save lifecycle.
	 * The keys are plugins and values are SaveContext objects.
	 */
	protected Map<String, SaveContext> computeSaveContexts(String[] pluginIds, int kind, IProject project) {
		HashMap<String, SaveContext> result = new HashMap<>(pluginIds.length);
		for (String pluginId : pluginIds) {
			try {
				SaveContext context = new SaveContext(pluginId, kind, project, workspace);
				result.put(pluginId, context);
			} catch (CoreException e) {
				// FIXME: should return a status to the user and not just log it
				Policy.log(e.getStatus());
			}
		}
		return result;
	}

	/**
	 * Returns a table mapping having the plug-in id as the key and the old tree
	 * as the value.
	 * This table is based on the union of the current <code>savedStates</code>
	 * and the given table of contexts.  The specified tree is used as the tree for
	 * any newly created saved states.  This method is used to compute the set of
	 * saved states to be written out.
	 */
	protected Map<String, ElementTree> computeStatesToSave(Map<String, SaveContext> contexts, ElementTree current) {
		HashMap<String, ElementTree> result = new HashMap<>(savedStates.size() * 2);
		synchronized (savedStates) {
			for (SavedState state : savedStates.values()) {
				if (state.oldTree != null)
					result.put(state.pluginId, state.oldTree);
			}
		}
		for (SaveContext context : contexts.values()) {
			if (!context.isDeltaNeeded())
				continue;
			String pluginId = context.getPluginId();
			result.put(pluginId, current);
		}
		return result;
	}

	protected void executeLifecycle(int lifecycle, ISaveParticipant participant, SaveContext context) throws CoreException {
		switch (lifecycle) {
			case PREPARE_TO_SAVE :
				participant.prepareToSave(context);
				break;
			case SAVING :
				try {
					if (ResourceStats.TRACE_SAVE_PARTICIPANTS)
						ResourceStats.startSave(participant);
					participant.saving(context);
				} finally {
					if (ResourceStats.TRACE_SAVE_PARTICIPANTS)
						ResourceStats.endSave();
				}
				break;
			case DONE_SAVING :
				participant.doneSaving(context);
				break;
			case ROLLBACK :
				participant.rollback(context);
				break;
			default :
				Assert.isTrue(false, "Invalid save lifecycle code"); //$NON-NLS-1$
		}
	}

	public void forgetSavedTree(String pluginId) {
		if (pluginId == null) {
			synchronized (savedStates) {
				for (SavedState savedState : savedStates.values())
					savedState.forgetTrees();
			}
		} else {
			SavedState state = savedStates.get(pluginId);
			if (state != null)
				state.forgetTrees();
		}
	}

	/**
	 * Used in the policy for cleaning up tree's of plug-ins that are not often activated.
	 */
	protected long getDeltaExpiration(String pluginId) {
		String result = masterTable.getProperty(DELTA_EXPIRATION_PREFIX + pluginId);
		return (result == null) ? System.currentTimeMillis() : Long.parseLong(result);
	}

	protected Properties getMasterTable() {
		return masterTable;
	}

	public int getSaveNumber(String pluginId) {
		String value = masterTable.getProperty(SAVE_NUMBER_PREFIX + pluginId);
		return (value == null) ? 0 : Integer.parseInt(value);
	}

	protected String[] getSaveParticipantPluginIds() {
		synchronized (saveParticipants) {
			return saveParticipants.keySet().toArray(new String[saveParticipants.size()]);
		}
	}

	/**
	 * Hooks the end of a save operation, for debugging and performance
	 * monitoring purposes.
	 */
	private void hookEndSave(int kind, IProject project, long start) {
		if (ResourceStats.TRACE_SNAPSHOT && kind == ISaveContext.SNAPSHOT)
			ResourceStats.endSnapshot();
		if (Policy.DEBUG_SAVE) {
			String endMessage = null;
			switch (kind) {
				case ISaveContext.FULL_SAVE :
					endMessage = DEBUG_FULL_SAVE;
					break;
				case ISaveContext.SNAPSHOT :
					endMessage = DEBUG_SNAPSHOT;
					break;
				case ISaveContext.PROJECT_SAVE :
					endMessage = DEBUG_PROJECT_SAVE + project.getFullPath() + ": "; //$NON-NLS-1$
					break;
			}
			if (endMessage != null)
				Policy.debug(endMessage + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$
		}
	}

	/**
	 * Hooks the start of a save operation, for debugging and performance
	 * monitoring purposes.
	 */
	private void hookStartSave(int kind, Project project) {
		if (ResourceStats.TRACE_SNAPSHOT && kind == ISaveContext.SNAPSHOT)
			ResourceStats.startSnapshot();
		if (Policy.DEBUG_SAVE) {
			switch (kind) {
				case ISaveContext.FULL_SAVE :
					Policy.debug(DEBUG_FULL_SAVE + DEBUG_START);
					break;
				case ISaveContext.SNAPSHOT :
					Policy.debug(DEBUG_SNAPSHOT + DEBUG_START);
					break;
				case ISaveContext.PROJECT_SAVE :
					Policy.debug(DEBUG_PROJECT_SAVE + project.getFullPath() + DEBUG_START);
					break;
			}
		}
	}

	/**
	 * Initializes the snapshot mechanism for this workspace.
	 */
	protected void initSnap(IProgressMonitor monitor) {
		// Discard any pending snapshot request.
		snapshotJob.cancel();
		// The "lastSnap" tree must be frozen as the exact tree obtained from startup,
		// otherwise ensuing snapshot deltas may be based on an incorrect tree (see bug 12575).
		lastSnap = workspace.getElementTree();
		lastSnap.immutable();
		workspace.newWorkingTree();
		operationCount = 0;
		// Delete the snapshot files, if any.
		IPath location = workspace.getMetaArea().getSnapshotLocationFor(workspace.getRoot());
		java.io.File target = location.toFile().getParentFile();
		FilenameFilter filter = (dir, name) -> {
			if (!name.endsWith(LocalMetaArea.F_SNAP))
				return false;
			for (int i = 0; i < name.length() - LocalMetaArea.F_SNAP.length(); i++) {
				char c = name.charAt(i);
				if (c < '0' || c > '9')
					return false;
			}
			return true;
		};
		String[] candidates = target.list(filter);
		if (candidates != null)
			removeFiles(target, candidates, Collections.<String> emptyList());
	}

	protected boolean isDeltaCleared(String pluginId) {
		String clearDelta = masterTable.getProperty(CLEAR_DELTA_PREFIX + pluginId);
		return clearDelta != null && clearDelta.equals("true"); //$NON-NLS-1$
	}

	protected boolean isOldPluginTree(String pluginId) {
		// first, check if this plug-ins was marked not to receive a delta
		if (isDeltaCleared(pluginId))
			return false;
		//see if the plugin is still installed
		if (Platform.getBundle(pluginId) == null)
			return true;

		//finally see if the delta has past its expiry date
		long deltaAge = System.currentTimeMillis() - getDeltaExpiration(pluginId);
		return deltaAge > workspace.internalGetDescription().getDeltaExpiration();
	}

	/**
	 * @see IElementInfoFlattener#readElement(IPath, DataInput)
	 */
	@Override
	public Object readElement(IPath path, DataInput input) throws IOException {
		Assert.isNotNull(path);
		Assert.isNotNull(input);
		// read the flags and pull out the type.
		int flags = input.readInt();
		int type = (flags & ICoreConstants.M_TYPE) >> ICoreConstants.M_TYPE_START;
		ResourceInfo info = workspace.newElement(type);
		info.readFrom(flags, input);
		return info;
	}

	private void rememberSnapshotRequestor() {
		if (Policy.DEBUG_SAVE)
			Policy.debug(new RuntimeException("Scheduling workspace snapshot")); //$NON-NLS-1$
		if (snapshotRequestor == null) {
			String msg = "The workspace will exit with unsaved changes in this session."; //$NON-NLS-1$
			snapshotRequestor = new ResourceStatus(ICoreConstants.CRASH_DETECTED, msg);
		}
	}

	/**
	 * Remove marks from current save participants. This marks prevent them to receive their
	 * deltas when they register themselves as save participants.
	 */
	protected void removeClearDeltaMarks() {
		synchronized (saveParticipants) {
			for (String pluginId : saveParticipants.keySet()) {
				removeClearDeltaMarks(pluginId);
			}
		}
	}

	protected void removeClearDeltaMarks(String pluginId) {
		masterTable.setProperty(CLEAR_DELTA_PREFIX + pluginId, "false"); //$NON-NLS-1$
	}

	protected void removeFiles(java.io.File root, String[] candidates, List<String> exclude) {
		for (String candidate : candidates) {
			boolean delete = true;
			for (ListIterator<String> it = exclude.listIterator(); it.hasNext();) {
				String s = it.next();
				if (s.equals(candidate)) {
					it.remove();
					delete = false;
					break;
				}
			}
			if (delete)
				new java.io.File(root, candidate).delete();
		}
	}

	private void removeGarbage(DataOutputStream output, IPath location, IPath tempLocation) throws IOException {
		if (output.size() == 0) {
			output.close();
			location.toFile().delete();
			tempLocation.toFile().delete();
		}
	}

	public void removeParticipant(String pluginId) {
		saveParticipants.remove(pluginId);
	}

	protected void removeUnusedSafeTables() {
		List<String> valuables = new ArrayList<>(10);
		IPath location = workspace.getMetaArea().getSafeTableLocationFor(ResourcesPlugin.PI_RESOURCES);
		valuables.add(location.lastSegment()); // add master table
		for (Enumeration<Object> e = masterTable.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			if (key.startsWith(SAVE_NUMBER_PREFIX)) {
				String pluginId = key.substring(SAVE_NUMBER_PREFIX.length());
				valuables.add(workspace.getMetaArea().getSafeTableLocationFor(pluginId).lastSegment());
			}
		}
		java.io.File target = location.toFile().getParentFile();
		String[] candidates = target.list();
		if (candidates == null)
			return;
		removeFiles(target, candidates, valuables);
	}

	protected void removeUnusedTreeFiles() {
		// root resource
		List<String> valuables = new ArrayList<>(10);
		IPath location = workspace.getMetaArea().getTreeLocationFor(workspace.getRoot(), false);
		valuables.add(location.lastSegment());
		java.io.File target = location.toFile().getParentFile();
		FilenameFilter filter = (dir, name) -> name.endsWith(LocalMetaArea.F_TREE);
		String[] candidates = target.list(filter);
		if (candidates != null)
			removeFiles(target, candidates, valuables);

		// projects
		IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects) {
			location = workspace.getMetaArea().getTreeLocationFor(project, false);
			valuables.add(location.lastSegment());
			target = location.toFile().getParentFile();
			candidates = target.list(filter);
			if (candidates != null)
				removeFiles(target, candidates, valuables);
		}
	}

	protected void reportSnapshotRequestor() {
		if (snapshotRequestor != null)
			Policy.log(snapshotRequestor);
	}

	public void requestSnapshot() {
		snapshotRequested = true;
	}

	/**
	 * Reset the snapshot mechanism for the non-workspace files. This
	 * includes the markers and sync info.
	 */
	protected void resetSnapshots(IResource resource) throws CoreException {
		Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
		String message;

		// delete the snapshot file, if any
		java.io.File file = workspace.getMetaArea().getMarkersSnapshotLocationFor(resource).toFile();
		if (file.exists())
			file.delete();
		if (file.exists()) {
			message = Messages.resources_resetMarkers;
			throw new ResourceException(IResourceStatus.FAILED_DELETE_METADATA, resource.getFullPath(), message, null);
		}

		// delete the snapshot file, if any
		file = workspace.getMetaArea().getSyncInfoSnapshotLocationFor(resource).toFile();
		if (file.exists())
			file.delete();
		if (file.exists()) {
			message = Messages.resources_resetSync;
			throw new ResourceException(IResourceStatus.FAILED_DELETE_METADATA, resource.getFullPath(), message, null);
		}

		// if we have the workspace root then recursive over the projects.
		// only do open projects since closed ones are saved elsewhere
		if (resource.getType() == IResource.PROJECT)
			return;
		IProject[] projects = ((IWorkspaceRoot) resource).getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects)
			resetSnapshots(project);
	}

	/**
	 * Restores the state of this workspace by opening the projects
	 * which were open when it was last saved.
	 */
	protected void restore(IProgressMonitor monitor) throws CoreException {
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore workspace: starting..."); //$NON-NLS-1$
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask("", 50); //$NON-NLS-1$
			// need to open the tree to restore, but since we're not
			// inside an operation, be sure to close it afterwards
			workspace.newWorkingTree();
			try {
				String msg = Messages.resources_startupProblems;
				MultiStatus problems = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_READ_METADATA, msg, null);

				restoreMasterTable();
				// restore the saved tree and overlay the snapshots if any
				restoreTree(Policy.subMonitorFor(monitor, 10));
				restoreSnapshots(Policy.subMonitorFor(monitor, 10));

				// tolerate failure for non-critical information
				// if startup fails, the entire workspace is shot
				try {
					restoreMarkers(workspace.getRoot(), false, Policy.subMonitorFor(monitor, 10));
				} catch (CoreException e) {
					problems.merge(e.getStatus());
				}
				try {
					restoreSyncInfo(workspace.getRoot(), Policy.subMonitorFor(monitor, 10));
				} catch (CoreException e) {
					problems.merge(e.getStatus());
				}
				// restore meta info last because it might close a project if its description is not readable
				restoreMetaInfo(problems, Policy.subMonitorFor(monitor, 10));
				IProject[] roots = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
				for (IProject root : roots)
					((Project) root).startup();
				if (!problems.isOK())
					Policy.log(problems);
			} finally {
				workspace.getElementTree().immutable();
			}
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore workspace: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Restores the contents of this project.  Throw
	 * an exception if the project could not be restored.
	 * @return <code>true</code> if the project data was restored successfully,
	 * and <code>false</code> if non-critical problems occurred while restoring.
	 * @exception CoreException if the project could not be restored.
	 */
	protected boolean restore(Project project, IProgressMonitor monitor) throws CoreException {
		boolean status = true;
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore project " + project.getFullPath() + ": starting..."); //$NON-NLS-1$ //$NON-NLS-2$
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask("", 40); //$NON-NLS-1$
			if (project.isOpen()) {
				status = restoreTree(project, Policy.subMonitorFor(monitor, 10));
			} else {
				monitor.worked(10);
			}
			restoreMarkers(project, true, Policy.subMonitorFor(monitor, 10));
			restoreSyncInfo(project, Policy.subMonitorFor(monitor, 10));
			// restore meta info last because it might close a project if its description is not found
			restoreMetaInfo(project, Policy.subMonitorFor(monitor, 10));
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore project " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return status;
	}

	/**
	 * Restores the contents of this project from a refresh snapshot, if possible.
	 * Throws an exception if the snapshot is found but an error occurs when reading
	 * the file.
	 * @return <code>true</code> if the project data was restored successfully,
	 * and <code>false</code> if the refresh snapshot was not found or could not be opened.
	 * @exception CoreException if an error occurred reading the snapshot file.
	 */
	protected boolean restoreFromRefreshSnapshot(Project project, IProgressMonitor monitor) throws CoreException {
		boolean status = true;
		IPath snapshotPath = workspace.getMetaArea().getRefreshLocationFor(project);
		java.io.File snapshotFile = snapshotPath.toFile();
		if (!snapshotFile.exists())
			return false;
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore project " + project.getFullPath() + ": starting..."); //$NON-NLS-1$ //$NON-NLS-2$
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask("", 40); //$NON-NLS-1$
			status = restoreTreeFromRefreshSnapshot(project, snapshotFile, Policy.subMonitorFor(monitor, 40));
			if (status) {
				// load the project description and set internal description
				ProjectDescription description = workspace.getFileSystemManager().read(project, true);
				project.internalSetDescription(description, false);
				workspace.getMetaArea().clearRefresh(project);
			}
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE)
			Policy.debug("Restore project " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return status;
	}

	/**
	 * Reads the markers which were originally saved
	 * for the tree rooted by the given resource.
	 */
	protected void restoreMarkers(IResource resource, boolean generateDeltas, IProgressMonitor monitor) throws CoreException {
		Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
		long start = System.currentTimeMillis();
		MarkerManager markerManager = workspace.getMarkerManager();
		// when restoring a project, only load markers if it is open
		if (resource.isAccessible())
			markerManager.restore(resource, generateDeltas, monitor);

		// if we have the workspace root then restore markers for its projects
		if (resource.getType() == IResource.PROJECT) {
			if (Policy.DEBUG_RESTORE_MARKERS) {
				Policy.debug("Restore Markers for " + resource.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return;
		}
		IProject[] projects = ((IWorkspaceRoot) resource).getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects)
			if (project.isAccessible())
				markerManager.restore(project, generateDeltas, monitor);
		if (Policy.DEBUG_RESTORE_MARKERS) {
			Policy.debug("Restore Markers for workspace: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected void restoreMasterTable() throws CoreException {
		long start = System.currentTimeMillis();
		masterTable.clear();
		IPath location = workspace.getMetaArea().getSafeTableLocationFor(ResourcesPlugin.PI_RESOURCES);
		java.io.File target = location.toFile();
		if (!target.exists()) {
			location = workspace.getMetaArea().getBackupLocationFor(location);
			target = location.toFile();
			if (!target.exists())
				return;
		}
		try (SafeChunkyInputStream input = new SafeChunkyInputStream(target)) {
			masterTable.load(input);
		} catch (IOException e) {
			String message = Messages.resources_exMasterTable;
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, e);
		}
		if (Policy.DEBUG_RESTORE_MASTERTABLE)
			Policy.debug("Restore master table for " + location + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Restores the state of this workspace by opening the projects
	 * which were open when it was last saved.
	 */
	protected void restoreMetaInfo(MultiStatus problems, IProgressMonitor monitor) {
		if (Policy.DEBUG_RESTORE_METAINFO)
			Policy.debug("Restore workspace metainfo: starting..."); //$NON-NLS-1$
		long start = System.currentTimeMillis();
		IProject[] roots = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject root : roots) {
			//fatal to throw exceptions during startup
			try {
				restoreMetaInfo((Project) root, monitor);
			} catch (CoreException e) {
				String message = NLS.bind(Messages.resources_readMeta, root.getName());
				problems.merge(new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, root.getFullPath(), message, e));
			}
		}
		if (Policy.DEBUG_RESTORE_METAINFO)
			Policy.debug("Restore workspace metainfo: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Restores the contents of this project.  Throw an exception if the
	 * project description could not be restored.
	 */
	protected void restoreMetaInfo(Project project, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		ProjectDescription description = null;
		CoreException failure = null;
		try {
			if (project.isOpen())
				description = workspace.getFileSystemManager().read(project, true);
			else
				//for closed projects, just try to read the legacy .prj file,
				//because the project location is stored there.
				description = workspace.getMetaArea().readOldDescription(project);
		} catch (CoreException e) {
			failure = e;
		}
		// If we had an open project and there was an error reading the description
		// from disk, close the project and give it a default description. If the project
		// was already closed then just set a default description.
		if (description == null) {
			description = new ProjectDescription();
			description.setName(project.getName());
			//try to read private metadata and add to the description
			workspace.getMetaArea().readPrivateDescription(project, description);
		}
		project.internalSetDescription(description, false);
		if (failure != null) {
			// write the project tree ...
			writeTree(project, IResource.DEPTH_INFINITE);
			// ... and close the project
			project.internalClose(monitor);
			throw failure;
		}
		if (Policy.DEBUG_RESTORE_METAINFO)
			Policy.debug("Restore metainfo for " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Restores the workspace tree from snapshot files in the event
	 * of a crash.  The workspace tree must be open when this method
	 * is called, and will be open at the end of this method.  In the
	 * event of a crash recovery, the snapshot file is not deleted until
	 * the next successful save.
	 */
	protected void restoreSnapshots(IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		String message;
		try {
			monitor.beginTask("", Policy.totalWork); //$NON-NLS-1$
			IPath snapLocation = workspace.getMetaArea().getSnapshotLocationFor(workspace.getRoot());
			java.io.File localFile = snapLocation.toFile();

			if (!localFile.exists()) {
				// The snapshot corresponding to the current tree version doesn't exist.
				// Try the legacy non-versioned snapshot, but ignore it if it is older than
				// the tree.
				snapLocation = workspace.getMetaArea().getLegacySnapshotLocationFor(workspace.getRoot());
				localFile = snapLocation.toFile();
				if (!localFile.exists() || isSnapshotOlderThanTree(localFile)) {
					// If the snapshot file doesn't exist, there was no crash.
					// Just initialize the snapshot file and return.
					initSnap(Policy.subMonitorFor(monitor, Policy.totalWork / 2));
					return;
				}
			}
			// If we have a snapshot file, the workspace was shutdown without being saved or crashed.
			workspace.setCrashed(true);
			try {
				/* Read each of the snapshots and lay them on top of the current tree.*/
				ElementTree complete = workspace.getElementTree();
				complete.immutable();
				try (
					DataInputStream input = new DataInputStream(new SafeChunkyInputStream(localFile));
				) {
					WorkspaceTreeReader reader = WorkspaceTreeReader.getReader(workspace, input.readInt());
					complete = reader.readSnapshotTree(input, complete, monitor);
				} finally {
					//reader returned an immutable tree, but since we're inside
					//an operation, we must return an open tree
					lastSnap = complete;
					complete = complete.newEmptyDelta();
					workspace.tree = complete;
				}
			} catch (Exception e) {
				// only log the exception, we should not fail restoring the snapshot
				message = Messages.resources_snapRead;
				Policy.log(new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, null, message, e));
			}
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE_SNAPSHOTS)
			Policy.debug("Restore snapshots for workspace: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Checks if the given snapshot file is older than the tree file.
	 *
	 * @param snapshot the snapshot file to check
	 * @return {@code true} if the snapshot file is older than the tree file or the tree file
	 *     does not exist
	 */
	private boolean isSnapshotOlderThanTree(File snapshot) {
		IPath treeLocation = workspace.getMetaArea().getTreeLocationFor(workspace.getRoot(), false);
		File tree = treeLocation.toFile();
		if (!tree.exists()) {
			treeLocation = workspace.getMetaArea().getBackupLocationFor(treeLocation);
			tree = treeLocation.toFile();
			if (!tree.exists())
				return false;
		}
		return snapshot.lastModified() < tree.lastModified();
	}

	/**
	 * Reads the sync info which was originally saved
	 * for the tree rooted by the given resource.
	 */
	protected void restoreSyncInfo(IResource resource, IProgressMonitor monitor) throws CoreException {
		Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
		long start = System.currentTimeMillis();
		Synchronizer synchronizer = (Synchronizer) workspace.getSynchronizer();
		// when restoring a project, only load sync info if it is open
		if (resource.isAccessible())
			synchronizer.restore(resource, monitor);

		// restore sync info for all projects if we were given the workspace root.
		if (resource.getType() == IResource.PROJECT) {
			if (Policy.DEBUG_RESTORE_SYNCINFO) {
				Policy.debug("Restore SyncInfo for " + resource.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return;
		}
		IProject[] projects = ((IWorkspaceRoot) resource).getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects)
			if (project.isAccessible())
				synchronizer.restore(project, monitor);
		if (Policy.DEBUG_RESTORE_SYNCINFO) {
			Policy.debug("Restore SyncInfo for workspace: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Reads the contents of the tree rooted by the given resource from the
	 * file system. This method is used when restoring a complete workspace
	 * after workspace save/shutdown.
	 * @exception CoreException if the workspace could not be restored.
	 */
	protected void restoreTree(IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		IPath treeLocation = workspace.getMetaArea().getTreeLocationFor(workspace.getRoot(), false);
		IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(treeLocation);
		if (!treeLocation.toFile().exists() && !tempLocation.toFile().exists()) {
			savedStates = Collections.synchronizedMap(new HashMap<>(10));
			return;
		}
		try (DataInputStream input = new DataInputStream(new SafeFileInputStream(treeLocation.toOSString(), tempLocation.toOSString(), TREE_BUFFER_SIZE))) {
			WorkspaceTreeReader.getReader(workspace, input.readInt()).readTree(input, monitor);
		} catch (Exception e) { // "Unknown format" is passed as ResourceException
			String msg = NLS.bind(Messages.resources_readMeta, treeLocation.toOSString());
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, treeLocation, msg, e);
		}
		if (Policy.DEBUG_RESTORE_TREE) {
			Policy.debug("Restore Tree for workspace: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Restores the trees for the builders of this project from the local disk.
	 * Does nothing if the tree file does not exist (this means the
	 * project has never been saved).  This method is
	 * used when restoring a saved/closed project.  restoreTree(Workspace) is
	 * used when restoring a complete workspace after workspace save/shutdown.
	 * @return <code>true</code> if the tree file exists, <code>false</code> otherwise.
	 * @exception CoreException if the project could not be restored.
	 */
	protected boolean restoreTree(Project project, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		String message;
		try {
			monitor.beginTask("", Policy.totalWork); //$NON-NLS-1$
			IPath treeLocation = workspace.getMetaArea().getTreeLocationFor(project, false);
			IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(treeLocation);
			if (!treeLocation.toFile().exists() && !tempLocation.toFile().exists())
				return false;
			try (
				DataInputStream input = new DataInputStream(new SafeFileInputStream(treeLocation.toOSString(), tempLocation.toOSString()));
			) {
				WorkspaceTreeReader reader = WorkspaceTreeReader.getReader(workspace, input.readInt());
				reader.readTree(project, input, Policy.subMonitorFor(monitor, Policy.totalWork));
			}
		} catch (IOException e) {
			message = NLS.bind(Messages.resources_readMeta, project.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, project.getFullPath(), message, e);
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE_TREE) {
			Policy.debug("Restore Tree for " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return true;
	}

	/**
	 * Restores a tree saved as a refresh snapshot to a specified URI.
	 * @return <code>true</code> if the snapshot exists, <code>false</code> otherwise.
	 * @exception CoreException if the project could not be restored.
	 */
	protected boolean restoreTreeFromRefreshSnapshot(Project project, java.io.File snapshotFile, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		monitor = Policy.monitorFor(monitor);
		String message;
		IPath snapshotPath = null;
		try {
			monitor.beginTask("", Policy.totalWork); //$NON-NLS-1$
			InputStream snapIn = new FileInputStream(snapshotFile);
			ZipInputStream zip = new ZipInputStream(snapIn);
			ZipEntry treeEntry = zip.getNextEntry();
			if (treeEntry == null || !treeEntry.getName().equals("resource-index.tree")) { //$NON-NLS-1$
				zip.close();
				return false;
			}
			try (
				DataInputStream input = new DataInputStream(zip);
			) {
				WorkspaceTreeReader reader = WorkspaceTreeReader.getReader(workspace, input.readInt(), true);
				reader.readTree(project, input, Policy.subMonitorFor(monitor, Policy.totalWork));
			} finally {
				zip.close();
			}
		} catch (IOException e) {
			snapshotPath = new Path(snapshotFile.getPath());
			message = NLS.bind(Messages.resources_readMeta, snapshotPath);
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, snapshotPath, message, e);
		} finally {
			monitor.done();
		}
		if (Policy.DEBUG_RESTORE_TREE) {
			Policy.debug("Restore Tree for " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return true;
	}

	class InternalMonitorWrapper extends ProgressMonitorWrapper {
		private boolean ignoreCancel;

		public InternalMonitorWrapper(IProgressMonitor monitor) {
			super(SubMonitor.convert(monitor));
		}

		public void ignoreCancelState(boolean ignore) {
			this.ignoreCancel = ignore;
		}

		@Override
		public boolean isCanceled() {
			return ignoreCancel ? false : super.isCanceled();
		}
	}

	public IStatus save(int kind, Project project, IProgressMonitor monitor) throws CoreException {
		return save(kind, false, project, monitor);
	}

	public IStatus save(int kind, boolean keepConsistencyWhenCanceled, Project project, IProgressMonitor parentMonitor) throws CoreException {
		InternalMonitorWrapper monitor = new InternalMonitorWrapper(parentMonitor);
		monitor.ignoreCancelState(keepConsistencyWhenCanceled);
		try {
			isSaving = true;
			String message = Messages.resources_saving_0;
			monitor.beginTask(message, 7);
			message = Messages.resources_saveWarnings;
			MultiStatus warnings = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IStatus.WARNING, message, null);
			ISchedulingRule rule = project != null ? (IResource) project : workspace.getRoot();
			try {
				workspace.prepareOperation(rule, monitor);
				workspace.beginOperation(false);
				hookStartSave(kind, project);
				long start = System.currentTimeMillis();
				Map<String, SaveContext> contexts = computeSaveContexts(getSaveParticipantPluginIds(), kind, project);
				broadcastLifecycle(PREPARE_TO_SAVE, contexts, warnings, Policy.subMonitorFor(monitor, 1));
				try {
					broadcastLifecycle(SAVING, contexts, warnings, Policy.subMonitorFor(monitor, 1));
					switch (kind) {
						case ISaveContext.FULL_SAVE :
							// save the complete tree and remember all of the required saved states
							saveTree(contexts, Policy.subMonitorFor(monitor, 1));
							// reset the snapshot state.
							initSnap(null);
							snapshotRequestor = null;
							//save master table right after saving tree to ensure correct tree number is saved
							cleanMasterTable();
							// save all of the markers and all sync info in the workspace
							persistMarkers = 0l;
							persistSyncInfo = 0l;
							visitAndSave(workspace.getRoot());
							monitor.worked(1);
							if (Policy.DEBUG_SAVE) {
								Policy.debug("Total Save Markers: " + persistMarkers + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
								Policy.debug("Total Save Sync Info: " + persistSyncInfo + "ms"); //$NON-NLS-1$	 //$NON-NLS-2$
							}
							// reset the snap shot files
							resetSnapshots(workspace.getRoot());
							//remove unused files
							removeUnusedSafeTables();
							removeUnusedTreeFiles();

							// history pruning can be always canceled
							monitor.ignoreCancelState(false);
							workspace.getFileSystemManager().getHistoryStore().clean(Policy.subMonitorFor(monitor, 1));
							monitor.ignoreCancelState(keepConsistencyWhenCanceled);

							// write out all metainfo (e.g., workspace/project descriptions)
							saveMetaInfo(warnings, Policy.subMonitorFor(monitor, 1));
							break;
						case ISaveContext.SNAPSHOT :
							snapTree(workspace.getElementTree(), Policy.subMonitorFor(monitor, 1));
							// snapshot the markers and sync info for the workspace
							persistMarkers = 0l;
							persistSyncInfo = 0l;
							visitAndSnap(workspace.getRoot());
							monitor.worked(1);
							if (Policy.DEBUG_SAVE) {
								Policy.debug("Total Snap Markers: " + persistMarkers + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
								Policy.debug("Total Snap Sync Info: " + persistSyncInfo + "ms"); //$NON-NLS-1$	 //$NON-NLS-2$
							}
							collapseTrees(contexts);
							clearSavedDelta();
							// write out all metainfo (e.g., workspace/project descriptions)
							saveMetaInfo(warnings, Policy.subMonitorFor(monitor, 1));
							break;
						case ISaveContext.PROJECT_SAVE :
							writeTree(project, IResource.DEPTH_INFINITE);
							monitor.worked(1);
							// save markers and sync info
							visitAndSave(project);
							monitor.worked(1);
							// reset the snapshot file
							resetSnapshots(project);
							IStatus result = saveMetaInfo(project, null);
							if (!result.isOK())
								warnings.merge(result);
							monitor.worked(1);
							break;
					}
					// save contexts
					commit(contexts);
					if (kind == ISaveContext.FULL_SAVE)
						removeClearDeltaMarks();
					//this must be done after committing save contexts to update participant save numbers
					saveMasterTable(kind);
					broadcastLifecycle(DONE_SAVING, contexts, warnings, Policy.subMonitorFor(monitor, 1));
					hookEndSave(kind, project, start);
					return warnings;
				} catch (CoreException e) {
					broadcastLifecycle(ROLLBACK, contexts, warnings, Policy.subMonitorFor(monitor, 1));
					// rollback ResourcesPlugin master table
					restoreMasterTable();
					throw e; // re-throw
				}
			} catch (OperationCanceledException e) {
				workspace.getWorkManager().operationCanceled();
				throw e;
			} finally {
				workspace.endOperation(rule, false);
			}
		} finally {
			isSaving = false;
			monitor.done();
		}
	}

	protected void saveMasterTable(int kind) throws CoreException {
		Set<Entry<Object, Object>> state = Set.copyOf(getMasterTable().entrySet());
		if (Objects.equals(state, savedState)) {
			return;
		}
		saveMasterTable(kind, workspace.getMetaArea().getSafeTableLocationFor(ResourcesPlugin.PI_RESOURCES));
		savedState = state;
	}

	protected void saveMasterTable(int kind, IPath location) throws CoreException {
		long start = System.currentTimeMillis();
		java.io.File target = location.toFile();
		try {
			if (kind == ISaveContext.FULL_SAVE || kind == ISaveContext.SNAPSHOT)
				validateMasterTableBeforeSave(target);
			try (
				SafeChunkyOutputStream output = new SafeChunkyOutputStream(target);
			) {
				masterTable.store(output, "master table"); //$NON-NLS-1$
				output.succeed();
			}
		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, NLS.bind(Messages.resources_exSaveMaster, location.toOSString()), e);
		}
		if (Policy.DEBUG_SAVE_MASTERTABLE)
			Policy.debug("Save master table for " + location + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Writes the metainfo (e.g. descriptions) of the given workspace and
	 * all projects to the local disk.
	 */
	protected void saveMetaInfo(MultiStatus problems, IProgressMonitor monitor) throws CoreException {
		if (Policy.DEBUG_SAVE_METAINFO)
			Policy.debug("Save workspace metainfo: starting..."); //$NON-NLS-1$
		long start = System.currentTimeMillis();
		// save preferences (workspace description, path variables, etc)
		ResourcesPlugin.getPlugin().savePluginPreferences();
		// save projects' meta info
		IProject[] roots = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject root : roots)
			if (root.isAccessible()) {
				IStatus result = saveMetaInfo((Project) root, null);
				if (!result.isOK())
					problems.merge(result);
			}
		if (Policy.DEBUG_SAVE_METAINFO)
			Policy.debug("Save workspace metainfo: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Ensures that the project meta-info is saved.  The project meta-info
	 * is usually saved as soon as it changes, so this is just a sanity check
	 * to make sure there is something on disk before we shutdown.
	 *
	 * @return Status object containing non-critical warnings, or an OK status.
	 */
	protected IStatus saveMetaInfo(Project project, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		//if there is nothing on disk, write the description
		if (!workspace.getFileSystemManager().hasSavedDescription(project)) {
			workspace.getFileSystemManager().writeSilently(project);
			String msg = NLS.bind(Messages.resources_missingProjectMetaRepaired, project.getName());
			return new ResourceStatus(IResourceStatus.MISSING_DESCRIPTION_REPAIRED, project.getFullPath(), msg);
		}
		if (Policy.DEBUG_SAVE_METAINFO)
			Policy.debug("Save metainfo for " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return Status.OK_STATUS;
	}

	/**
	 * Writes a snapshot of project refresh information to the specified
	 * location.
	 * @param project the project to write a refresh snapshot for
	 * @param monitor progress monitor
	 * @exception CoreException if there is a problem writing the snapshot.
	 */
	public void saveRefreshSnapshot(Project project, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
		IFileStore store = EFS.getStore(snapshotLocation);
		IPath snapshotPath = new Path(snapshotLocation.getPath());
		java.io.File tmpTree = null;
		try {
			tmpTree = java.io.File.createTempFile("tmp", ".tree"); //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, snapshotPath, Messages.resources_copyProblem, e);
		}
		ZipOutputStream out = null;
		try {
			FileOutputStream fis = new FileOutputStream(tmpTree);
			try (
				DataOutputStream output = new DataOutputStream(fis);
			) {
				output.writeInt(ICoreConstants.WORKSPACE_TREE_VERSION_2);
				writeTree(project, output, monitor);
			}
			OutputStream snapOut = store.openOutputStream(EFS.NONE, monitor);
			out = new ZipOutputStream(snapOut);
			out.setLevel(Deflater.BEST_COMPRESSION);
			ZipEntry e = new ZipEntry("resource-index.tree"); //$NON-NLS-1$
			out.putNextEntry(e);
			int read = 0;
			byte[] buffer = new byte[4096];
			try (
				InputStream in = new FileInputStream(tmpTree);
			) {
				while ((read = in.read(buffer)) >= 0) {
					out.write(buffer, 0, read);
				}
				out.closeEntry();
			}
			out.close();
		} catch (IOException e) {
			throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, snapshotPath, Messages.resources_copyProblem, e);
		} finally {
			FileUtil.safeClose(out);
			if (tmpTree != null)
				tmpTree.delete();
		}
	}

	/**
	 * Writes the current state of the entire workspace tree to disk.
	 * This is used during workspace save.  saveTree(Project)
	 * is used to save the state of an individual project.
	 * @exception CoreException if there is a problem writing the tree to disk.
	 */
	protected void saveTree(Map<String, SaveContext> contexts, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		IPath treeLocation = workspace.getMetaArea().getTreeLocationFor(workspace.getRoot(), true);
		try {
			IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(treeLocation);
			try (
				DataOutputStream output = new DataOutputStream(new SafeFileOutputStream(treeLocation.toOSString(), tempLocation.toOSString()));
			) {
				output.writeInt(ICoreConstants.WORKSPACE_TREE_VERSION_2);
				writeTree(computeStatesToSave(contexts, workspace.getElementTree()), output, monitor);
			}
		} catch (Exception e) {
			String msg = NLS.bind(Messages.resources_writeWorkspaceMeta, treeLocation);
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, Path.ROOT, msg, e);
		}
		if (Policy.DEBUG_SAVE_TREE)
			Policy.debug("Save Workspace Tree: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Should only be used for read purposes.
	 */
	void setPluginsSavedState(HashMap<String, SavedState> savedStates) {
		this.savedStates = Collections.synchronizedMap(savedStates);
	}

	protected void setSaveNumber(String pluginId, int number) {
		masterTable.setProperty(SAVE_NUMBER_PREFIX + pluginId, Integer.toString(number));
	}

	@Override
	public void shareStrings(StringPool pool) {
		lastSnap.shareStrings(pool);
	}

	@Override
	public void shutdown(final IProgressMonitor monitor) {
		// do a last snapshot if it was scheduled
		// we force it in the same thread because it would not
		// help if the job runs after we close the workspace
		int state = snapshotJob.getState();
		if (state == Job.WAITING || state == Job.SLEEPING)
			// we cannot pass null to Job#run
			snapshotJob.run(SubMonitor.convert(monitor));
		// cancel the snapshot job
		snapshotJob.cancel();
	}

	/**
	 * Performs a snapshot if one is deemed necessary.
	 * Encapsulates rules for determining when a snapshot is needed.
	 * This should be called at the end of every top level operation.
	 */
	public void snapshotIfNeeded(boolean hasTreeChanges) {
		// never schedule a snapshot while save is occurring.
		if (isSaving)
			return;
		if (snapshotRequested || operationCount >= workspace.internalGetDescription().getOperationsPerSnapshot()) {
			rememberSnapshotRequestor();
			if (snapshotJob.getState() == Job.NONE)
				snapshotJob.schedule();
			else
				snapshotJob.wakeUp();
		} else {
			if (hasTreeChanges) {
				operationCount++;
				if (snapshotJob.getState() == Job.NONE) {
					rememberSnapshotRequestor();
					long interval = workspace.internalGetDescription().getSnapshotInterval();
					snapshotJob.schedule(Math.max(interval, MIN_SNAPSHOT_DELAY));
				}
			} else {
				//only increment the operation count if we've had a sufficient number of no-ops
				if (++noopCount > NO_OP_THRESHOLD) {
					operationCount++;
					noopCount = 0;
				}
			}
		}
	}

	/**
	 * Performs a snapshot of the workspace tree.
	 */
	protected void snapTree(ElementTree tree, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		String message;
		SubMonitor subMonitor = SubMonitor.convert(monitor, Policy.totalWork);
		try {
			// the tree must be immutable
			tree.immutable();
			// don't need to snapshot if there are no changes
			if (tree == lastSnap)
				return;
			operationCount = 0;
			IPath snapPath = workspace.getMetaArea().getSnapshotLocationFor(workspace.getRoot());
			ElementTreeWriter writer = new ElementTreeWriter(this);
			java.io.File localFile = snapPath.toFile();
			try {
				SafeChunkyOutputStream safeStream = new SafeChunkyOutputStream(localFile);
				try (DataOutputStream out = new DataOutputStream(safeStream);) {
					out.writeInt(ICoreConstants.WORKSPACE_TREE_VERSION_2);
					writeWorkspaceFields(out, subMonitor);
					writer.writeDelta(tree, lastSnap, Path.ROOT, ElementTreeWriter.D_INFINITE, out,
							ResourceComparator.getSaveComparator());
					safeStream.succeed();
					out.close();
				}
			} catch (IOException e) {
				message = NLS.bind(Messages.resources_writeWorkspaceMeta, localFile.getAbsolutePath());
				throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, Path.ROOT, message, e);
			}
			lastSnap = tree;
			if (Policy.DEBUG_SAVE_TREE)
				Policy.debug("Snapshot Workspace Tree: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			subMonitor.done();
		}
	}

	/**
	 * Returns a sorted copy of a chain of trees.
	 *
	 * @param trees an unordered array of ElementTrees that should be immutable. The
	 *              array may contain duplicates. The ElementTrees should have been
	 *              created by repeated calls to Workspace.newWorkingTree() such
	 *              that the getParent() relationship forms an unambiguous sequence
	 *              (except of duplicates) from newest ElementTree to oldest. I.e.
	 *              the newest ElementTree (and its duplicates) has no parent (or at
	 *              least no ancestor within the given array), while all other
	 *              ElementTreess have the newest ElementTree as ancestor
	 *              (transitive parent). The given trees do not need to contain all
	 *              ElementTrees from the getParent() relationship.
	 * @return null or trees ordered by ElementTree.treeStamp descending. i.e.
	 *         newest ElementTree (without ancestor within the trees) first. Returns
	 *         null when the preconditions for sorting are not met.
	 */
	public static ElementTree[] sortTrees(ElementTree[] trees) {
		int numTrees = trees.length;
		ElementTree[] sorted = new ElementTree[numTrees];

		/* first build a table of ElementTree -> Number of duplicates */
		Map<ElementTree, Integer> duplicateCount = new LinkedHashMap<>(numTrees * 2 + 1);
		for (ElementTree tree : trees) {
			duplicateCount.compute(tree, (k, duplicates) -> (duplicates == null ? 0 : duplicates) + 1);
		}

		/* find the oldest tree (a descendent of all other trees) */
		ElementTree oldest = trees[ElementTree.findOldest(trees)];

		/**
		 * Walk through the chain of trees from oldest to newest,
		 * adding them to the sorted list as we go.
		 */
		int i = numTrees - 1;
		while (oldest != null) {
			/* add "oldest" and its duplicates at the end of the sorted list: */
			Integer duplicates = duplicateCount.remove(oldest);
			for (int j = 0; j < duplicates; j++) {
				sorted[i] = oldest;
				i--;
			}
			/* find the next tree in the list */
			oldest = oldest.getParent();
			while (oldest != null && duplicateCount.get(oldest) == null) {
				/* skip elements that are not elements of "trees" */
				oldest = oldest.getParent();
			}
		}
		if (!duplicateCount.isEmpty()) {
			// could happen if trees contains elements t3,t2,t1 where the parent relations
			// are t3->t1, t2->t1, t1->null
			// either t2 or t3 is not found
			// because it's not unambiguous defined if t3 or t2 is the "older"
			// t3 should have parent t2 instead.
			// happens while trees are mutable.
			Exception e = new NullPointerException(
					"Unable to save workspace - Given trees not in unambiguous order (Bug 352867)"); //$NON-NLS-1$
			IStatus status = new Status(IStatus.WARNING, ResourcesPlugin.PI_RESOURCES, IResourceStatus.INTERNAL_ERROR,
					e.getMessage(), e);
			Policy.log(status);
			return null;
		}
		return sorted;
	}

	static String parentChain(ElementTree e) {
		ArrayList<ElementTree> chain = new ArrayList<>();
		ElementTree el = e;
		while (el != null) {
			chain.add(el);
			el = el.getParent();
		}
		return chain.stream().map(t -> "" + t.getTreeStamp()).collect(Collectors.joining("->")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void startup(IProgressMonitor monitor) throws CoreException {
		restore(monitor);
		java.io.File table = workspace.getMetaArea().getSafeTableLocationFor(ResourcesPlugin.PI_RESOURCES).toFile();
		if (!table.exists())
			table.getParentFile().mkdirs();
	}

	/**
	 * Update the expiration time for the given plug-in's tree.  If the tree was never
	 * loaded, use the current value in the master table. If the tree has been loaded,
	 * use the provided new timestamp.
	 *
	 * The timestamp is used in the policy for cleaning up tree's of plug-ins that are
	 * not often activated.
	 */
	protected void updateDeltaExpiration(String pluginId) {
		String key = DELTA_EXPIRATION_PREFIX + pluginId;
		if (!masterTable.containsKey(key))
			masterTable.setProperty(key, Long.toString(System.currentTimeMillis()));
	}

	private void validateMasterTableBeforeSave(java.io.File target) throws IOException {
		if (target.exists()) {
			MasterTable previousMasterTable = new MasterTable();
			try (
				SafeChunkyInputStream input = new SafeChunkyInputStream(target);
			) {
				previousMasterTable.load(input);
				String stringValue = previousMasterTable.getProperty(ROOT_SEQUENCE_NUMBER_KEY);
				// if there was a full save, then there must be a non-null entry for root
				if (stringValue != null) {
					int valueInFile = Integer.parseInt(stringValue);
					int valueInMemory = Integer.parseInt(masterTable.getProperty(ROOT_SEQUENCE_NUMBER_KEY));
					// new master table must provide greater or equal sequence number for root
					// throw exception if new value is lower than previous one - we cannot allow to desynchronize master table on disk
					if (valueInMemory < valueInFile) {
						String message = getBadSequenceNumberErrorMessage(target, valueInFile, valueInMemory);
						Assert.isLegal(false, message);
					}
				}
			}
		}
	}

	private static String getBadSequenceNumberErrorMessage(java.io.File target, int valueInFile, int valueInMemory) {
		StringBuilder messageBuffer = new StringBuilder();
		messageBuffer.append("Cannot set lower sequence number for root (previous: "); //$NON-NLS-1$
		messageBuffer.append(valueInFile);
		messageBuffer.append(", new: "); //$NON-NLS-1$
		messageBuffer.append(valueInMemory);
		messageBuffer.append("). Location: "); //$NON-NLS-1$
		messageBuffer.append(target.getAbsolutePath());
		try {
			messageBuffer.append("Timestamps and tree sequence numbers from file:"); //$NON-NLS-1$
			java.nio.file.Path targetPath = Paths.get(target.getAbsolutePath());
			List<String> masterTableFileContents = Files.readAllLines(targetPath, Charset.defaultCharset());
			for (String line : masterTableFileContents) {
				if (line != null) {
					boolean isPropertiesTimestamp = line.startsWith("#"); //$NON-NLS-1$
					boolean isTreeProperty = line.startsWith(ROOT_SEQUENCE_NUMBER_KEY);
					if (isPropertiesTimestamp || isTreeProperty) {
						messageBuffer.append(System.lineSeparator());
						messageBuffer.append(line);
					}
				}
			}
		} catch (IOException e) {
			ILog log = ResourcesPlugin.getPlugin().getLog();
			String errorMessage = "Error occurred while reading master table file"; //$NON-NLS-1$
			IStatus errorStatus = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, errorMessage, e);
			log.log(errorStatus);
		}
		return messageBuffer.toString();
	}

	/**
	 * Visit the given resource (to depth infinite) and write out extra information
	 * like markers and sync info. To be called during a full save and project save.
	 *
	 * FIXME: This method is ugly. Fix it up and look at merging with #visitAndSnap
	 */
	public void visitAndSave(final IResource root) throws CoreException {
		// Ensure we have either a project or the workspace root
		Assert.isLegal(root.getType() == IResource.ROOT || root.getType() == IResource.PROJECT);
		// only write out info for accessible resources
		if (!root.isAccessible())
			return;

		// Setup variables
		final Synchronizer synchronizer = (Synchronizer) workspace.getSynchronizer();
		final MarkerManager markerManager = workspace.getMarkerManager();
		IPath markersLocation = workspace.getMetaArea().getMarkersLocationFor(root);
		IPath markersTempLocation = workspace.getMetaArea().getBackupLocationFor(markersLocation);
		IPath syncInfoLocation = workspace.getMetaArea().getSyncInfoLocationFor(root);
		IPath syncInfoTempLocation = workspace.getMetaArea().getBackupLocationFor(syncInfoLocation);
		final List<String> writtenTypes = new ArrayList<>(5);
		final List<QualifiedName> writtenPartners = new ArrayList<>(synchronizer.registry.size());
		DataOutputStream o1 = null;
		DataOutputStream o2 = null;
		String message;

		// Create the output streams
		try {
			o1 = new DataOutputStream(new SafeFileOutputStream(markersLocation.toOSString(), markersTempLocation.toOSString()));
			// we don't store the sync info for the workspace root so don't create
			// an empty file
			if (root.getType() != IResource.ROOT)
				o2 = new DataOutputStream(new SafeFileOutputStream(syncInfoLocation.toOSString(), syncInfoTempLocation.toOSString()));
		} catch (IOException e) {
			FileUtil.safeClose(o1);
			FileUtil.safeClose(o2);
			message = NLS.bind(Messages.resources_writeMeta, root.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, root.getFullPath(), message, e);
		}

		final DataOutputStream markersOutput = o1;
		final DataOutputStream syncInfoOutput = o2;
		// The following 2 piece array will hold a running total of the times
		// taken to save markers and syncInfo respectively.  This will cut down
		// on the number of statements printed out as we would get 2 statements
		// for each resource otherwise.
		final long[] saveTimes = new long[2];

		// Create the visitor
		IElementContentVisitor visitor = (tree, requestor, elementContents) -> {
			ResourceInfo info = (ResourceInfo) elementContents;
			if (info != null) {
				try {
					// save the markers
					long start = System.currentTimeMillis();
					markerManager.save(info, requestor, markersOutput, writtenTypes);
					long markerSaveTime = System.currentTimeMillis() - start;
					saveTimes[0] += markerSaveTime;
					persistMarkers += markerSaveTime;
					// save the sync info - if we have the workspace root then the output stream will be null
					if (syncInfoOutput != null) {
						start = System.currentTimeMillis();
						synchronizer.saveSyncInfo(info, requestor, syncInfoOutput, writtenPartners);
						long syncInfoSaveTime = System.currentTimeMillis() - start;
						saveTimes[1] += syncInfoSaveTime;
						persistSyncInfo += syncInfoSaveTime;
					}
				} catch (IOException e) {
					throw new WrappedRuntimeException(e);
				}
			}
			// don't continue if the current resource is the workspace root, only continue for projects
			return root.getType() != IResource.ROOT;
		};

		// Call the visitor
		try {
			try {
				new ElementTreeIterator(workspace.getElementTree(), root.getFullPath()).iterate(visitor);
			} catch (WrappedRuntimeException e) {
				throw (IOException) e.getTargetException();
			}
			if (Policy.DEBUG_SAVE_MARKERS)
				Policy.debug("Save Markers for " + root.getFullPath() + ": " + saveTimes[0] + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (Policy.DEBUG_SAVE_SYNCINFO)
				Policy.debug("Save SyncInfo for " + root.getFullPath() + ": " + saveTimes[1] + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			removeGarbage(markersOutput, markersLocation, markersTempLocation);
			// if we have the workspace root the output stream will be null and we
			// don't have to perform cleanup code
			if (syncInfoOutput != null) {
				removeGarbage(syncInfoOutput, syncInfoLocation, syncInfoTempLocation);
				syncInfoOutput.close();
			}
			markersOutput.close();
		} catch (IOException e) {
			message = NLS.bind(Messages.resources_writeMeta, root.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, root.getFullPath(), message, e);
		} finally {
			FileUtil.safeClose(markersOutput);
			FileUtil.safeClose(syncInfoOutput);
		}

		// recurse over the projects in the workspace if we were given the workspace root
		if (root.getType() == IResource.PROJECT)
			return;
		IProject[] projects = ((IWorkspaceRoot) root).getProjects(IContainer.INCLUDE_HIDDEN);
		// never use a shared ForkJoinPool.commonPool() as it may be busy with other tasks, which might deadlock:
		ForkJoinPool forkJoinPool =  new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism());
		IStatus[] stats;
		try {
			stats = forkJoinPool.submit(() -> Arrays.stream(projects).parallel().map(project -> {
				try {
					visitAndSave(project);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return null;
			}).filter(Objects::nonNull).toArray(IStatus[]::new)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new CoreException(Status.error(Messages.resources_saveProblem, e));
		} finally {
			forkJoinPool.shutdown();
		}
		if (stats.length > 0) {
			throw new CoreException(new MultiStatus(ResourcesPlugin.PI_RESOURCES, IStatus.ERROR, stats,
					Messages.resources_saveProblem, null));
		}
	}

	/**
	 * Visit the given resource (to depth infinite) and write out extra information
	 * like markers and sync info. To be called during a snapshot
	 *
	 * FIXME: This method is ugly. Fix it up and look at merging with #visitAndSnap
	 */
	public void visitAndSnap(final IResource root) throws CoreException {
		// Ensure we have either a project or the workspace root
		Assert.isLegal(root.getType() == IResource.ROOT || root.getType() == IResource.PROJECT);
		// only write out info for accessible resources
		if (!root.isAccessible())
			return;

		// Setup variables
		final Synchronizer synchronizer = (Synchronizer) workspace.getSynchronizer();
		final MarkerManager markerManager = workspace.getMarkerManager();
		IPath markersLocation = workspace.getMetaArea().getMarkersSnapshotLocationFor(root);
		IPath syncInfoLocation = workspace.getMetaArea().getSyncInfoSnapshotLocationFor(root);
		SafeChunkyOutputStream safeMarkerStream = null;
		SafeChunkyOutputStream safeSyncInfoStream = null;
		DataOutputStream o1 = null;
		DataOutputStream o2 = null;
		String message;

		// Create the output streams
		try {
			safeMarkerStream = new SafeChunkyOutputStream(markersLocation.toFile());
			o1 = new DataOutputStream(safeMarkerStream);
			// we don't store the sync info for the workspace root so don't create
			// an empty file
			if (root.getType() != IResource.ROOT) {
				safeSyncInfoStream = new SafeChunkyOutputStream(syncInfoLocation.toFile());
				o2 = new DataOutputStream(safeSyncInfoStream);
			}
		} catch (IOException e) {
			FileUtil.safeClose(o1);
			message = NLS.bind(Messages.resources_writeMeta, root.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, root.getFullPath(), message, e);
		}

		final DataOutputStream markersOutput = o1;
		final DataOutputStream syncInfoOutput = o2;
		int markerFileSize = markersOutput.size();
		int syncInfoFileSize = safeSyncInfoStream == null ? -1 : syncInfoOutput.size();
		// The following 2 piece array will hold a running total of the times
		// taken to save markers and syncInfo respectively.  This will cut down
		// on the number of statements printed out as we would get 2 statements
		// for each resource otherwise.
		final long[] snapTimes = new long[2];

		IElementContentVisitor visitor = (tree, requestor, elementContents) -> {
			ResourceInfo info = (ResourceInfo) elementContents;
			if (info != null) {
				try {
					// save the markers
					long start = System.currentTimeMillis();
					markerManager.snap(info, requestor, markersOutput);
					long markerSnapTime = System.currentTimeMillis() - start;
					snapTimes[0] += markerSnapTime;
					persistMarkers += markerSnapTime;
					// save the sync info - if we have the workspace root then the output stream will be null
					if (syncInfoOutput != null) {
						start = System.currentTimeMillis();
						synchronizer.snapSyncInfo(info, requestor, syncInfoOutput);
						long syncInfoSnapTime = System.currentTimeMillis() - start;
						snapTimes[1] += syncInfoSnapTime;
						persistSyncInfo += syncInfoSnapTime;
					}
				} catch (IOException e) {
					throw new WrappedRuntimeException(e);
				}
			}
			// don't continue if the current resource is the workspace root, only continue for projects
			return root.getType() != IResource.ROOT;
		};

		try {
			// Call the visitor
			try {
				new ElementTreeIterator(workspace.getElementTree(), root.getFullPath()).iterate(visitor);
			} catch (WrappedRuntimeException e) {
				throw (IOException) e.getTargetException();
			}
			if (Policy.DEBUG_SAVE_MARKERS)
				Policy.debug("Snap Markers for " + root.getFullPath() + ": " + snapTimes[0] + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (Policy.DEBUG_SAVE_SYNCINFO)
				Policy.debug("Snap SyncInfo for " + root.getFullPath() + ": " + snapTimes[1] + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (markerFileSize != markersOutput.size())
				safeMarkerStream.succeed();
			if (safeSyncInfoStream != null && syncInfoFileSize != syncInfoOutput.size()) {
				safeSyncInfoStream.succeed();
				syncInfoOutput.close();
			}
			markersOutput.close();
		} catch (IOException e) {
			message = NLS.bind(Messages.resources_writeMeta, root.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, root.getFullPath(), message, e);
		} finally {
			FileUtil.safeClose(markersOutput);
			FileUtil.safeClose(syncInfoOutput);
		}

		// recurse over the projects in the workspace if we were given the workspace root
		if (root.getType() == IResource.PROJECT)
			return;
		IProject[] projects = ((IWorkspaceRoot) root).getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects)
			visitAndSnap(project);
	}

	/**
	 * Writes out persistent information about all builders for which a last built
	 * tree is available. File format is:
	 * int - number of builders
	 * for each builder:
	 *    UTF - project name
	 *    UTF - fully qualified builder extension name
	 *    int - number of interesting projects for builder
	 *    For each interesting project:
	 *       UTF - interesting project name
	 */
	private void writeBuilderPersistentInfo(DataOutputStream output, List<BuilderPersistentInfo> builders) throws IOException {
		// write the number of builders we are saving
		int numBuilders = builders.size();
		output.writeInt(numBuilders);
		for (int i = 0; i < numBuilders; i++) {
			BuilderPersistentInfo info = builders.get(i);
			output.writeUTF(info.getProjectName());
			output.writeUTF(info.getBuilderName());
			// write interesting projects
			IProject[] interestingProjects = info.getInterestingProjects();
			output.writeInt(interestingProjects.length);
			for (IProject interestingProject : interestingProjects)
				output.writeUTF(interestingProject.getName());
		}
	}

	@Override
	public void writeElement(IPath path, Object element, DataOutput output) throws IOException {
		Assert.isNotNull(path);
		Assert.isNotNull(element);
		Assert.isNotNull(output);
		ResourceInfo info = (ResourceInfo) element;
		output.writeInt(info.getFlags());
		info.writeTo(output);
	}

	/**
	 * Discovers the trees which need to be saved for the passed in project's builders.
	 * In a pre-3.7 workspace, only one tree is saved per builder.
	 * Since 3.7 one tree may be persisted per build configuration per multi-config builder.
	 *
	 * We still provide one tree per builder first so the workspace can be opened in an older Eclipse.
	 * Newer eclipses will be able to load the additional per-configuration trees.
	 * @param project project to fetch builder trees for
	 * @param trees list of trees to be persisted
	 * @param builderInfos list of builder infos; one per builder
	 * @param configNames configuration names persisted for builder infos above
	 * @param additionalTrees remaining trees to be persisted for other configurations
	 * @param additionalBuilderInfos remaining builder infos for other configurations
	 * @param additionalConfigNames configuration names of the remaining per-configuration trees
	 * @throws CoreException
	 */
	private void getTreesToSave(IProject project, List<ElementTree> trees, List<BuilderPersistentInfo> builderInfos, List<String> configNames, List<ElementTree> additionalTrees, List<BuilderPersistentInfo> additionalBuilderInfos, List<String> additionalConfigNames) throws CoreException {
		if (project.isOpen()) {
			String activeConfigName = project.getActiveBuildConfig().getName();
			List<BuilderPersistentInfo> infos = workspace.getBuildManager().createBuildersPersistentInfo(project);
			if (infos != null) {
				for (BuilderPersistentInfo info : infos) {
					// Nothing to persist if there isn't a previous delta tree.
					// There used to be code which serialized the current workspace tree
					// but this will result in the next build of the builder getting an empty delta...
					if (info.getLastBuiltTree() == null)
						continue;

					// Add to the correct list of builders info and add to the configuration names
					String configName = info.getConfigName() == null ? activeConfigName : info.getConfigName();
					if (configName.equals(activeConfigName)) {
						// Serializes the active configurations's build tree
						// TODO could probably do better by serializing the 'oldest' tree
						builderInfos.add(info);
						configNames.add(configName);
						trees.add(info.getLastBuiltTree());
					} else {
						additionalBuilderInfos.add(info);
						additionalConfigNames.add(configName);
						additionalTrees.add(info.getLastBuiltTree());
					}
				}
			}
		}
	}

	/**
	 * Attempts to save plugin info, builder info and build states for all projects
	 * in the workspace.
	 *
	 * The following is written to the output stream:
	 * <ul>
	 * <li> Workspace information </li>
	 * <li> A list of plugin info </li>
	 * <li> Builder info for all the builders for each project's active build config </li>
	 * <li> Workspace trees for all plugins and builders </li>
	 * <li> And since 3.7: </li>
	 * <li> Builder info for all the builders of all the other project's buildConfigs </li>
	 * <li> The names of the buildConfigs for each of the builders </li>
	 * </ul>
	 * This format is designed to work with WorkspaceTreeReader versions 2.
	 *
	 * @see WorkspaceTreeReader_2
	 */
	protected void writeTree(Map<String, ElementTree> statesToSave, DataOutputStream output, IProgressMonitor monitor)
			throws IOException, CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
		boolean wasImmutable = false;
		try {
			// Create an array of trees to save. Ensure that the current one is in the list
			ElementTree current = workspace.getElementTree();
			wasImmutable = current.isImmutable();
			current.immutable();
			ArrayList<ElementTree> trees = new ArrayList<>(statesToSave.size() * 2); // pick a number
			subMonitor.worked(1);

			// write out the workspace fields
			writeWorkspaceFields(output, subMonitor.newChild(2));

			// save plugin info
			output.writeInt(statesToSave.size()); // write the number of plugins we are saving
			for (Map.Entry<String, ElementTree> entry : statesToSave.entrySet()) {
				String pluginId = entry.getKey();
				output.writeUTF(pluginId);
				trees.add(entry.getValue()); // tree
				updateDeltaExpiration(pluginId);
			}
			subMonitor.worked(1);

			// Get the the builder info and configuration names, and add all the associated
			// workspace trees in the correct order
			IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
			List<BuilderPersistentInfo> builderInfos = new ArrayList<>(projects.length * 2);
			List<String> configNames = new ArrayList<>(projects.length);
			List<ElementTree> additionalTrees = new ArrayList<>(projects.length * 2);
			List<BuilderPersistentInfo> additionalBuilderInfos = new ArrayList<>(projects.length * 2);
			List<String> additionalConfigNames = new ArrayList<>(projects.length);
			for (IProject project : projects)
				getTreesToSave(project, trees, builderInfos, configNames, additionalTrees, additionalBuilderInfos,
						additionalConfigNames);

			// Save the version 2 builders info
			writeBuilderPersistentInfo(output, builderInfos);

			// Builder infos of non-active configurations are persisted after the active
			// configuration's builder infos. So, their trees have to follow the same order.
			trees.addAll(additionalTrees);

			// add the current tree in the list as the last tree in the chain
			trees.add(current);

			/* save the forest! */
			ElementTreeWriter writer = new ElementTreeWriter(this);
			ElementTree[] treesToSave = trees.toArray(new ElementTree[trees.size()]);
			writer.writeDeltaChain(treesToSave, Path.ROOT, ElementTreeWriter.D_INFINITE, output,
					ResourceComparator.getSaveComparator());
			subMonitor.worked(4);

			// Since 3.7: Save the additional builders info
			writeBuilderPersistentInfo(output, additionalBuilderInfos);

			// Save the configuration names for the builders in the order they were saved
			for (String string : configNames)
				output.writeUTF(string);
			for (String string : additionalConfigNames)
				output.writeUTF(string);
		} finally {
			subMonitor.done();
			if (!wasImmutable)
				workspace.newWorkingTree();
		}
	}

	/**
	 * Attempts to save all the trees for the given project. This includes the current
	 * workspace tree and a tree for each builder that has previously built state information.
	 *
	 * The following is written to the output stream:
	 * <ul>
	 * <li> Builder info for all the builders for the project's active build configuration </li>
	 * <li> Workspace trees for all the project's builders </li>
	 * <li> Since 3.7: </li>
	 * <li> Builder info for all the builders of all the other project's buildConfigs </li>
	 * <li> Name of the project's buildConfigs </li>
	 * </ul>
	 * This format is designed to work with WorkspaceTreeReader versions 2.
	 *
	 * @throws IOException if anything went wrong during save.
	 * @see WorkspaceTreeReader_2
	 */
	protected void writeTree(Project project, DataOutputStream output, IProgressMonitor monitor)
			throws IOException, CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 10);
		boolean wasImmutable = false;
		try {
			// Create an array of trees to save and ensure that the current one is immutable
			// before we add other trees
			ElementTree current = workspace.getElementTree();
			wasImmutable = current.isImmutable();
			current.immutable();
			List<ElementTree> trees = new ArrayList<>(2);
			subMonitor.worked(1);

			// Get the the builder info and configuration names, and add all the associated
			// workspace trees in the correct order
			List<String> configNames = new ArrayList<>(5);
			List<BuilderPersistentInfo> builderInfos = new ArrayList<>(5);
			List<String> additionalConfigNames = new ArrayList<>(5);
			List<BuilderPersistentInfo> additionalBuilderInfos = new ArrayList<>(5);
			List<ElementTree> additionalTrees = new ArrayList<>(5);
			getTreesToSave(project, trees, builderInfos, configNames, additionalTrees, additionalBuilderInfos,
					additionalConfigNames);

			// Save the version 2 builders info
			writeBuilderPersistentInfo(output, builderInfos);

			// Builder infos of non-active configurations are persisted after the active
			// configuration's builder infos. So, their trees have to follow the same order.
			trees.addAll(additionalTrees);

			// Add the current tree in the list as the last tree in the chain
			trees.add(current);

			// Save the trees
			ElementTreeWriter writer = new ElementTreeWriter(this);
			ElementTree[] treesToSave = trees.toArray(new ElementTree[trees.size()]);
			writer.writeDeltaChain(treesToSave, project.getFullPath(), ElementTreeWriter.D_INFINITE, output,
					ResourceComparator.getSaveComparator());
			subMonitor.worked(5);

			// Since 3.7: Save the builders info and get the workspace trees associated with
			// those builders
			writeBuilderPersistentInfo(output, additionalBuilderInfos);

			// Save configuration names for the builders in the order they were saved
			for (String string : configNames)
				output.writeUTF(string);
			for (String string : additionalConfigNames)
				output.writeUTF(string);
		} finally {
			subMonitor.done();
			if (!wasImmutable)
				workspace.newWorkingTree();
		}
	}

	protected void writeTree(Project project, int depth) throws CoreException {
		long start = System.currentTimeMillis();
		IPath treeLocation = workspace.getMetaArea().getTreeLocationFor(project, true);
		IPath tempLocation = workspace.getMetaArea().getBackupLocationFor(treeLocation);
		try {
			SafeFileOutputStream safe = new SafeFileOutputStream(treeLocation.toOSString(), tempLocation.toOSString());
			try (
				DataOutputStream output = new DataOutputStream(safe);
			) {
				output.writeInt(ICoreConstants.WORKSPACE_TREE_VERSION_2);
				writeTree(project, output, null);
			}
		} catch (IOException e) {
			String msg = NLS.bind(Messages.resources_writeMeta, project.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, treeLocation, msg, e);
		}
		if (Policy.DEBUG_SAVE_TREE)
			Policy.debug("Save tree for " + project.getFullPath() + ": " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected void writeWorkspaceFields(DataOutputStream output, IProgressMonitor monitor) throws IOException {
		// save the next node id
		output.writeLong(workspace.nextNodeId.get());
		// save the modification stamp (no longer used)
		output.writeLong(0L);
		// save the marker id counter
		output.writeLong(workspace.nextMarkerId.get());
		// save the registered sync partners in the synchronizer
		((Synchronizer) workspace.getSynchronizer()).savePartners(output);
	}
}
