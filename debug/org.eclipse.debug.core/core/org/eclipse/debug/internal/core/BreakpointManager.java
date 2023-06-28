/*******************************************************************************
 *  Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IBreakpointImportParticipant;
import org.eclipse.debug.core.model.ITriggerPoint;

/**
 * The breakpoint manager manages all registered breakpoints for the Debug plug-in. It is
 * instantiated by the Debug plug-in at startup.
 * <p>
 * <strong>Note:</strong> This manager is created while the Debug plug-in is started, but it
 * will not automatically be initialized. Client code that expects markers and breakpoints to be
 * initialized must call {@link #ensureInitialized()}.
 * </p>
 *
 * @see IBreakpointManager
 */
public class BreakpointManager implements IBreakpointManager, IResourceChangeListener {

	/**
	 * Constants for breakpoint add/remove/change updates
	 */
	private final static int ADDED = 0;
	private final static int REMOVED = 1;
	private final static int CHANGED = 2;

	/**
	 * A collection of breakpoints registered with this manager.
	 */
	private Vector<IBreakpoint> fBreakpoints;

	/**
	 * Map of breakpoint import participants.
	 * Map has the form:
	 * <pre>Map(String - marker_id, List of {@link IBreakpointImportParticipant})</pre>
	 */
	private HashMap<String, ArrayList<BreakpointImportParticipantDelegate>> fImportParticipants;

	/**
	 * A system default import participant that performs legacy comparison support
	 * when no participants are provided for a given type.
	 *
	 * @since 3.5
	 */
	private IBreakpointImportParticipant fDefaultParticipant;

	/**
	 * A collection of breakpoint markers that have received a POST_CHANGE notification
	 * that they have changed before a POST_BUILD notification of add. This allows us
	 * to tell if a marker has been created & changed since the breakpoint has been
	 * registered (see bug 138473).
	 */
	private final Set<IMarker> fPostChangMarkersChanged = new HashSet<>();

	/**
	 * A collection of breakpoint markers that have received a POST_BUILD notification
	 * of being added.
	 */
	private final Set<IMarker> fPostBuildMarkersAdded = new HashSet<>();

	/**
	 * Collection of breakpoints being added currently. Used to
	 * suppress change notification of "REGISTERED" attribute when
	 * being added.
	 */
	private final List<IBreakpoint> fSuppressChange = new ArrayList<>();

	/**
	 * A table of breakpoint extension points, keyed by
	 * marker type
	 * key: a marker type
	 * value: the breakpoint extension which corresponds to that marker type
	 */
	private final HashMap<String, IConfigurationElement> fBreakpointExtensions;

	/**
	 * Collection of markers that associates markers to breakpoints
	 * key: a marker
	 * value: the breakpoint which contains that marker
	 */
	private final HashMap<IMarker, IBreakpoint> fMarkersToBreakpoints;

	/**
	 * Collection of breakpoint listeners.
	 */
	private final ListenerList<IBreakpointListener> fBreakpointListeners = new ListenerList<>();

	/**
	 * Collection of (plural) breakpoint listeners.
	 */
	private ListenerList<IBreakpointsListener> fBreakpointsListeners= new ListenerList<>();

	/**
	 * Singleton resource delta visitor which handles marker
	 * additions, changes, and removals.
	 */
	private static BreakpointManagerVisitor fgVisitor;

	/**
	 * Collection of breakpoint manager listeners which are
	 * notified when this manager's state changes.
	 */
	private final ListenerList<IBreakpointManagerListener> fBreakpointManagerListeners = new ListenerList<>();

	/**
	 * Breakpoint which acts a the triggering point in a workspace.
	 */
	private final Set<IBreakpoint> fTriggerPointBreakpointList = new LinkedHashSet<>();

	/**
	 * Trigger points disabled by system after the first trigger point is
	 * enabled in a workspace.
	 */
	private final Set<IBreakpoint> fTriggerPointDisabledList = new LinkedHashSet<>();


	/**
	 * Listens to POST_CHANGE notifications of breakpoint markers to detect when
	 * a breakpoint is added & changed before the POST_BUILD add notification is
	 * sent.
	 */
	class PostChangeListener implements IResourceChangeListener {

		private PostChangeVisitor fVisitor = new PostChangeVisitor();

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta= event.getDelta();
			if (delta != null) {
				try {
					delta.accept(fVisitor);
				} catch (CoreException ce) {
					DebugPlugin.log(ce);
				}
			}
		}

	}

	/**
	 * Default implementation of a breakpoint import participant
	 *
	 * @since 3.5
	 */
	static class DefaultImportParticipant implements IBreakpointImportParticipant {

		@Override
		public boolean matches(Map<String, Object> attributes, IBreakpoint breakpoint) throws CoreException {
			//perform legacy comparison
			IMarker marker = breakpoint.getMarker();
			String type = (String) attributes.get("type"); //$NON-NLS-1$
			Integer line = (Integer) attributes.get(IMarker.LINE_NUMBER);
			Object localline = marker.getAttribute(IMarker.LINE_NUMBER);
			String localtype = marker.getType();
			if (type.equals(localtype)) {
				if(line != null && line.equals(localline)) {
					return true;
				}
				else if(line == null) {
					return true;
				}
			}
			return false;
		}
		@Override
		public void verify(IBreakpoint breakpoint) throws CoreException {}
	}

	/**
	 * The listener
	 */
	private PostChangeListener fPostChangeListener = new PostChangeListener();

	class PostChangeVisitor implements IResourceDeltaVisitor {

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta == null) {
				return false;
			}
			for (IMarkerDelta markerDelta : delta.getMarkerDeltas()) {
				if (markerDelta.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
					switch (markerDelta.getKind()) {
						case IResourceDelta.ADDED :
							break;
						case IResourceDelta.REMOVED :
							break;
						case IResourceDelta.CHANGED :
							IMarker marker = markerDelta.getMarker();
							synchronized (fPostChangMarkersChanged) {
								if (!fPostBuildMarkersAdded.contains(marker)) {
									fPostChangMarkersChanged.add(marker);
								}
							}
							break;
						default:
							break;
					}
				}
			}
			return true;
		}

	}

	/**
	 * Constructs a new breakpoint manager.
	 */
	public BreakpointManager() {
		fMarkersToBreakpoints = new HashMap<>(10);
		fBreakpointExtensions = new HashMap<>(15);
	}

	/**
	 * Loads all the breakpoints on the given resource.
	 *
	 * @param resource the resource which contains the breakpoints
	 * @param notify whether to notify of the breakpoint additions
	 * @throws CoreException if a problem is encountered
	 */
	private void loadBreakpoints(IResource resource, boolean notify) throws CoreException {
		initBreakpointExtensions();
		List<IBreakpoint> added = new ArrayList<>();
		for (IMarker marker : getPersistedMarkers(resource)) {
			try {
				IBreakpoint breakpoint = createBreakpoint(marker);
				synchronized (fPostChangMarkersChanged) {
					fPostBuildMarkersAdded.add(marker);
				}
				if (breakpoint.isRegistered()) {
					added.add(breakpoint);

				}
				if (breakpoint instanceof ITriggerPoint && ((ITriggerPoint) breakpoint).isTriggerPoint()) {
					addTriggerPoint(breakpoint);
				}
			} catch (DebugException e) {
				DebugPlugin.log(e);
			}
		}
		addBreakpoints(added.toArray(new IBreakpoint[added.size()]), notify);
	}

	/**
	 * Returns the persisted markers associated with the given resource.
	 *
	 * Delete any invalid breakpoint markers. This is done at startup rather
	 * than shutdown, since the changes made at shutdown are not persisted as
	 * the workspace state has already been saved. See bug 7683.
	 *
	 * Since the <code>TRANSIENT</code> marker attribute/feature has been added,
	 * we no longer have to manually delete non-persisted markers - the platform
	 * does this for us (at shutdown, transient markers are not saved). However,
	 * the code is still present to delete non-persisted markers from old
	 * workspaces.
	 * @param resource the {@link IResource} to get markers for
	 * @return the complete listing of persisted markers for the given {@link IResource}
	 * @throws CoreException if a problem is encountered
	 */
	protected IMarker[] getPersistedMarkers(IResource resource) throws CoreException {
		final List<IMarker> delete = new ArrayList<>();
		List<IMarker> persisted = new ArrayList<>();
		for (IMarker marker : resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE)) {
			// ensure the marker has a valid model identifier attribute
			// and delete the breakpoint if not
			String modelId = marker.getAttribute(IBreakpoint.ID, null);
			if (modelId == null) {
				// marker with old/invalid format - delete
				delete.add(marker);
			} else if (!marker.getAttribute(IBreakpoint.PERSISTED, true)) {
				// the breakpoint is marked as not to be persisted,
				// schedule for deletion
				delete.add(marker);
			} else {
				persisted.add(marker);
			}
		}
		// delete any markers that are not to be restored
		if (!delete.isEmpty()) {
			final IMarker[] delMarkers = delete.toArray(new IMarker[delete.size()]);
			IWorkspaceRunnable wr = pm -> {
				for (IMarker marker : delMarkers) {
					marker.delete();
				}
			};
			new BreakpointManagerJob(wr).schedule();
		}
		return persisted.toArray(new IMarker[persisted.size()]);
	}

	/**
	 * Removes this manager as a resource change listener
	 * and removes all breakpoint listeners.
	 */
	public void shutdown() {
		getWorkspace().removeResourceChangeListener(this);
		getWorkspace().removeResourceChangeListener(fPostChangeListener);
		fBreakpointListeners.clear();
		fBreakpointsListeners.clear();
		fBreakpointManagerListeners.clear();
		if(fImportParticipants != null) {
			fImportParticipants.clear();
			fImportParticipants = null;
			fDefaultParticipant = null;
		}
		if(fBreakpoints != null) {
			fBreakpoints.clear();
			fBreakpoints = null;
		}
		if(fMarkersToBreakpoints != null) {
			fMarkersToBreakpoints.clear();
		}
	}

	/**
	 * Find the defined breakpoint extensions and cache them for use in recreating
	 * breakpoints from markers.
	 */
	private void initBreakpointExtensions() {
		IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_BREAKPOINTS);
		for (IConfigurationElement element : ep.getConfigurationElements()) {
			String markerType = element.getAttribute(IConfigurationElementConstants.MARKER_TYPE);
			String className = element.getAttribute(IConfigurationElementConstants.CLASS);
			if (markerType == null) {
				DebugPlugin.log(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "Breakpoint extension " + element.getDeclaringExtension().getUniqueIdentifier() + " missing required attribute: markerType", null)); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (className == null){
				DebugPlugin.log(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "Breakpoint extension " + element.getDeclaringExtension().getUniqueIdentifier() + " missing required attribute: class", null)); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				fBreakpointExtensions.put(markerType, element);
			}
		}
	}

	/**
	 * Convenience method to get the workspace
	 * @return the default {@link IWorkspace}
	 */
	private IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * @see IBreakpointManager#getBreakpoint(IMarker)
	 */
	@Override
	public IBreakpoint getBreakpoint(IMarker marker) {
		// ensure that breakpoints are initialized
		getBreakpoints0();
		return fMarkersToBreakpoints.get(marker);
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		IBreakpoint[] temp = new IBreakpoint[0];
		Vector<IBreakpoint> breakpoints = getBreakpoints0();
		synchronized (breakpoints) {
			temp = new IBreakpoint[breakpoints.size()];
			breakpoints.copyInto(temp);
		}
		return temp;
	}

	/**
	 * Ensures that this manager is initialized.
	 * <p>
	 * This manager is created while the Debug plug-in is started, but it will not automatically
	 * be initialized. Client code that expects markers and breakpoints to be initialized must call
	 * this method.
	 * </p>
	 *
	 * @since 3.8
	 */
	public void ensureInitialized() {
		getBreakpoints0();
	}

	/**
	 * The BreakpointManager waits to load the breakpoints
	 * of the workspace until a request is made to retrieve the
	 * breakpoints.
	 * @return the underlying {@link Vector} of breakpoints
	 */
	private synchronized Vector<IBreakpoint> getBreakpoints0() {
		if (fBreakpoints == null) {
			initializeBreakpoints();
		}
		return fBreakpoints;
	}

	@Override
	public IBreakpoint[] getBreakpoints(String modelIdentifier) {
		Vector<IBreakpoint> allBreakpoints = getBreakpoints0();
		synchronized (allBreakpoints) {
			ArrayList<IBreakpoint> temp = new ArrayList<>(allBreakpoints.size());
			for (IBreakpoint breakpoint : allBreakpoints) {
				String id= breakpoint.getModelIdentifier();
				if (id != null && id.equals(modelIdentifier)) {
					temp.add(breakpoint);
				}
			}
			return temp.toArray(new IBreakpoint[temp.size()]);
		}
	}

	/**
	 * Loads the list of breakpoints from the breakpoint markers in the
	 * workspace. Start listening to resource deltas.
	 */
	private void initializeBreakpoints() {
		setBreakpoints(new Vector<>(10));
		try {
			loadBreakpoints(getWorkspace().getRoot(), false);
			getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_BUILD);
			getWorkspace().addResourceChangeListener(fPostChangeListener, IResourceChangeEvent.POST_CHANGE);
		} catch (CoreException ce) {
			DebugPlugin.log(ce);
			setBreakpoints(new Vector<>(0));
		}
	}

	/**
	 * @see IBreakpointManager#isRegistered(IBreakpoint)
	 */
	@Override
	public boolean isRegistered(IBreakpoint breakpoint) {
		return getBreakpoints0().contains(breakpoint);
	}


	/**
	 * @see IBreakpointManager#removeBreakpoint(IBreakpoint, boolean)
	 */
	@Override
	public void removeBreakpoint(IBreakpoint breakpoint, boolean delete) throws CoreException {
		removeBreakpoints(new IBreakpoint[]{breakpoint}, delete);
	}

	/**
	 * @see IBreakpointManager#removeBreakpoints(IBreakpoint[], boolean)
	 */
	@Override
	public void removeBreakpoints(IBreakpoint[] breakpoints, final boolean delete) throws CoreException {
		final List<IBreakpoint> remove = new ArrayList<>(breakpoints.length);
		List<IBreakpoint> bps = getBreakpoints0();
		for (IBreakpoint breakpoint : breakpoints) {
			if (bps.contains(breakpoint)) {
				remove.add(breakpoint);
			}
		}
		if (!remove.isEmpty()) {
			for (IBreakpoint breakpoint : remove) {
				bps.remove(breakpoint);
				fMarkersToBreakpoints.remove(breakpoint.getMarker());
				// If the breakpoint is a trigger point, remove else do nothing.
				removeTriggerPoint(breakpoint);
			}
			fireUpdate(remove, null, REMOVED);
			refreshTriggerpointDisplay();
			IWorkspaceRunnable r = monitor -> {
				for (IBreakpoint breakpoint : remove) {
					if (delete) {
						breakpoint.delete();
					} else {
						// if the breakpoint is being removed from the manager
						// because the project is closing, the breakpoint should
						// remain as registered, otherwise, the breakpoint should
						// be marked as unregistered
						IMarker marker = breakpoint.getMarker();
						if (marker.exists()) {
							IProject project = breakpoint.getMarker().getResource().getProject();
							if (project == null || project.isOpen()) {
								breakpoint.setRegistered(false);
							}
						}
					}
				}
			};
			getWorkspace().run(r, null, 0, null);
		}
	}

	/**
	 * Create a breakpoint for the given marker. The created breakpoint
	 * is of the type specified in the breakpoint extension associated
	 * with the given marker type.
	 *
	 * @param marker marker to create a breakpoint for
	 * @return a breakpoint on this marker
	 * @exception DebugException if breakpoint creation fails. Reasons for
	 *  failure include:
	 * <ol>
	 * <li>The breakpoint manager cannot determine what kind of breakpoint
	 *     to instantiate for the given marker type</li>
	 * <li>A lower level exception occurred while accessing the given marker</li>
	 * </ol>
	 */
	public IBreakpoint createBreakpoint(IMarker marker) throws DebugException {
		IBreakpoint breakpoint= fMarkersToBreakpoints.get(marker);
		if (breakpoint != null) {
			return breakpoint;
		}
		try {
			IConfigurationElement config = fBreakpointExtensions.get(marker.getType());
			if (config == null) {
				throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
 DebugException.CONFIGURATION_INVALID, MessageFormat.format(DebugCoreMessages.BreakpointManager_Missing_breakpoint_definition, new Object[] { marker.getType() }), null));
			}
			Object object = config.createExecutableExtension(IConfigurationElementConstants.CLASS);
			if (object instanceof IBreakpoint) {
				breakpoint = (IBreakpoint)object;
				breakpoint.setMarker(marker);
			} else {
				DebugPlugin.log(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "Breakpoint extension " + config.getDeclaringExtension().getUniqueIdentifier() + " missing required attribute: class", null)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return breakpoint;
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		}
	}

	/**
	 * @see IBreakpointManager#addBreakpoint(IBreakpoint)
	 */
	@Override
	public void addBreakpoint(IBreakpoint breakpoint) throws CoreException {
		addBreakpoints(new IBreakpoint[]{breakpoint});
	}

	/**
	 * @see IBreakpointManager#addBreakpoints(IBreakpoint[])
	 */
	@Override
	public void addBreakpoints(IBreakpoint[] breakpoints) throws CoreException {
		addBreakpoints(breakpoints, true);
	}

	/**
	 * Registers the given breakpoints and notifies listeners if specified.
	 *
	 * @param breakpoints the breakpoints to register
	 * @param notify whether to notify listeners of the add
	 * @throws CoreException if a problem is encountered
	 */
	private void addBreakpoints(IBreakpoint[] breakpoints, boolean notify) throws CoreException {
		List<IBreakpoint> added = new ArrayList<>(breakpoints.length);
		final List<IBreakpoint> update = new ArrayList<>();
		for (IBreakpoint breakpoint : breakpoints) {
			if (!getBreakpoints0().contains(breakpoint)) {
				verifyBreakpoint(breakpoint);
				if (breakpoint.isRegistered()) {
					// If notify == false, the breakpoints are just being added at startup
					added.add(breakpoint);
					getBreakpoints0().add(breakpoint);
					fMarkersToBreakpoints.put(breakpoint.getMarker(), breakpoint);
					if (breakpoint instanceof ITriggerPoint && ((ITriggerPoint) breakpoint).isTriggerPoint()) {
						addTriggerPoint(breakpoint);
					}

				} else {
					// need to update the 'registered' and/or 'group' attributes
					update.add(breakpoint);
				}
			}
		}
		if (notify) {
			fireUpdate(added, null, ADDED);
		}
		if (!update.isEmpty()) {
			IWorkspaceRunnable r = monitor -> {
				List<IBreakpoint> bps = getBreakpoints0();
				for (IBreakpoint breakpoint : update) {
					bps.add(breakpoint);
					breakpoint.setRegistered(true);
					fMarkersToBreakpoints.put(breakpoint.getMarker(), breakpoint);
				}
			};
			// Need to suppress change notification, since this is really
			// an add notification
			fSuppressChange.addAll(update);
			getWorkspace().run(r, null, 0, null);
			fSuppressChange.removeAll(update);
			if (notify) {
				fireUpdate(update, null, ADDED);
			}
		}
	}

	/**
	 * Returns whether change notification is to be suppressed for the given breakpoint.
	 * Used when adding breakpoints and changing the "REGISTERED" attribute.
	 *
	 * @param breakpoint the breakpoint
	 * @return boolean whether change notification is suppressed
	 */
	protected boolean isChangeSuppressed(IBreakpoint breakpoint) {
		return fSuppressChange.contains(breakpoint);
	}

	/**
	 * @see IBreakpointManager#fireBreakpointChanged(IBreakpoint)
	 */
	@Override
	public void fireBreakpointChanged(IBreakpoint breakpoint) {
		if (getBreakpoints0().contains(breakpoint)) {
			List<IBreakpoint> changed = new ArrayList<>();
			changed.add(breakpoint);
			fireUpdate(changed, null, CHANGED);
		}
	}

	/**
	 * Verifies that the breakpoint marker has the minimal required attributes,
	 * and throws a debug exception if not.
	 * @param breakpoint the {@link IBreakpoint} to verify
	 * @throws DebugException if a problem is encountered
	 */
	private void verifyBreakpoint(IBreakpoint breakpoint) throws DebugException {
		try {
			String id= breakpoint.getModelIdentifier();
			if (id == null) {
				throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
					DebugException.CONFIGURATION_INVALID, DebugCoreMessages.BreakpointManager_Missing_model_identifier, null));
			}
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		}
	}

	/**
	 * A resource has changed. Traverses the delta for breakpoint changes.
	 *
	 * @param event resource change event
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta= event.getDelta();
		if (delta != null) {
			try {
				if (fgVisitor == null) {
					fgVisitor= new BreakpointManagerVisitor();
				}
				delta.accept(fgVisitor);
				fgVisitor.update();
			} catch (CoreException ce) {
				DebugPlugin.log(ce);
			}
		}
	}

	/**
	 * Visitor for handling resource deltas
	 */
	class BreakpointManagerVisitor implements IResourceDeltaVisitor {
		/**
		 * Moved markers
		 */
		private List<IMarker> fMoved = new ArrayList<>();
		/**
		 * Removed breakpoints
		 */
		private List<IBreakpoint> fRemoved = new ArrayList<>();

		/**
		 * Added breakpoints.
		 * @since 3.7
		 */
		private List<IBreakpoint> fAdded = new ArrayList<>();

		/**
		 * Changed breakpoints and associated marker deltas
		 */
		private List<IBreakpoint> fChanged = new ArrayList<>();
		private List<IMarkerDelta> fChangedDeltas = new ArrayList<>();

		/**
		 * Resets the visitor for a delta traversal - empties
		 * collections of removed/changed breakpoints.
		 */
		protected void reset() {
			fMoved.clear();
			fRemoved.clear();
			fAdded.clear();
			fChanged.clear();
			fChangedDeltas.clear();
		}

		/**
		 * Performs updates on accumulated changes, and fires change notification after
		 * a traversal. Accumulated updates are reset.
		 */
		public void update() {
			if (!fMoved.isEmpty()) {
				// delete moved markers
				IWorkspaceRunnable wRunnable= monitor -> {
					for (IMarker marker : fMoved) {
						marker.delete();
					}
				};
				try {
					getWorkspace().run(wRunnable, null, 0, null);
				} catch (CoreException e) {
				}
			}
			if (!fRemoved.isEmpty()) {
				try {
					removeBreakpoints(fRemoved.toArray(new IBreakpoint[fRemoved.size()]), false);
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
			}
			if (!fAdded.isEmpty()) {
				try {
					IWorkspaceRunnable runnable= monitor -> {
						for (IBreakpoint breakpoint : fAdded) {
							breakpoint.getMarker().setAttribute(DebugPlugin.ATTR_BREAKPOINT_IS_DELETED, false);
							breakpoint.setRegistered(true);
						}
					};
					getWorkspace().run(runnable, null, 0, null);
					addBreakpoints(fAdded.toArray(new IBreakpoint[fAdded.size()]), true);
				} catch (CoreException e) {
					DebugPlugin.log(e);
				}
			}
			if (!fChanged.isEmpty()) {
				fireUpdate(fChanged, fChangedDeltas, CHANGED);
			}
			reset();
		}

		/**
		 * @see IResourceDeltaVisitor#visit(IResourceDelta)
		 */
		@Override
		public boolean visit(IResourceDelta delta) {
			if (delta == null) {
				return false;
			}
			if (0 != (delta.getFlags() & IResourceDelta.OPEN) && 0 == (delta.getFlags() & IResourceDelta.MOVED_FROM)) {
				handleProjectResourceOpenStateChange(delta.getResource());
				return false;
			}
			for (IMarkerDelta markerDelta : delta.getMarkerDeltas()) {
				if (markerDelta.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
					switch (markerDelta.getKind()) {
						case IResourceDelta.ADDED :
							handleAddBreakpoint(delta, markerDelta.getMarker(), markerDelta);
							break;
						case IResourceDelta.REMOVED :
							handleRemoveBreakpoint(markerDelta.getMarker());
							break;
						case IResourceDelta.CHANGED :
							handleChangeBreakpoint(markerDelta.getMarker(), markerDelta);
							break;
						default:
							break;
					}
				}
			}

			return true;
		}

		/**
		 * Wrapper for handling adds
		 * @param rDelta the {@link IResourceDelta}
		 * @param marker the new {@link IMarker}
		 * @param mDelta the accompanying {@link IMarkerDelta}
		 */
		protected void handleAddBreakpoint(IResourceDelta rDelta, IMarker marker, IMarkerDelta mDelta) {
			if (0 != (rDelta.getFlags() & IResourceDelta.MOVED_FROM)) {
				// This breakpoint has actually been moved - already removed
				// from the Breakpoint manager during the remove callback.
				// Schedule the marker associated with the new resource for deletion.
				if (getBreakpoint(marker) == null) {
					fMoved.add(marker);
				}
			} else {
				// check if the an add & change have be combined into one add notification
				synchronized (fPostChangMarkersChanged) {
					if (fPostChangMarkersChanged.contains(marker)) {
						handleChangeBreakpoint(marker, mDelta);
						fPostChangMarkersChanged.remove(marker);
					} else if (marker.getAttribute(DebugPlugin.ATTR_BREAKPOINT_IS_DELETED, false) && getBreakpoint(marker) == null) {
						try { /*
							 * There may be breakpoints with matching resource
							 * and same line number
							 */
							IBreakpoint breakpoint = findMatchingBreakpoint(marker);
							if (breakpoint != null) {
								removeBreakpoint(breakpoint, true);
							}
							fAdded.add(createBreakpoint(marker));
						} catch (CoreException e) {
							DebugPlugin.log(e);
						}
					}
					fPostBuildMarkersAdded.add(marker);
				}
			}
		}

		/**
		 * To find a breakpoint with matching marker resources and line number.
		 *
		 * @param marker the {@link IMarker} for which existing breakpoint is
		 *            retrieved
		 * @return matching breakpoint if exists else return <code>null</code>
		 */
		private IBreakpoint findMatchingBreakpoint(IMarker marker) {
			try {
				Integer line = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
				for (IBreakpoint breakpoint : getBreakpoints0()) {
					IMarker bpMarker = breakpoint.getMarker();
					if (bpMarker != null && marker.getResource().equals(bpMarker.getResource()) && bpMarker.getAttribute(IMarker.LINE_NUMBER, -1) == (line == null ? -1 : line.intValue())) {
						return breakpoint;
					}
				}
			} catch (CoreException e) {
				// ignore
			}
			return null;
		}

		/**
		 * Wrapper for handling removes
		 * @param marker the {@link IMarker}
		 */
		protected void handleRemoveBreakpoint(IMarker marker) {
			synchronized (fPostChangMarkersChanged) {
				fPostChangMarkersChanged.remove(marker);
				fPostBuildMarkersAdded.remove(marker);
			}
			IBreakpoint breakpoint= getBreakpoint(marker);
			if (breakpoint != null) {
				fRemoved.add(breakpoint);
			}
		}

		/**
		 * Wrapper for handling changes
		 * @param marker the {@link IMarker} that was changed
		 * @param delta the {@link IMarkerDelta}
		 */
		protected void handleChangeBreakpoint(IMarker marker, IMarkerDelta delta) {
			IBreakpoint breakpoint= getBreakpoint(marker);
			if (breakpoint != null && isRegistered(breakpoint) && !isChangeSuppressed(breakpoint)) {
				fChanged.add(breakpoint);
				fChangedDeltas.add(delta);
			}
		}

		/**
		 * A project has been opened or closed.  Updates the breakpoints for
		 * that project
		 * @param project the {@link IProject} that was changed
		 */
		private void handleProjectResourceOpenStateChange(final IResource project) {
			if (!project.isAccessible()) {
				//closed
				for (@SuppressWarnings("unchecked") IBreakpoint breakpoint : (Vector<IBreakpoint>) getBreakpoints0().clone()) {
					IResource markerResource= breakpoint.getMarker().getResource();
					if (project.getFullPath().isPrefixOf(markerResource.getFullPath())) {
						fRemoved.add(breakpoint);
					}
				}
				return;
			}
			try {
				loadBreakpoints(project, true);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
	}

	/**
	 * @see IBreakpointManager#addBreakpointListener(IBreakpointListener)
	 */
	@Override
	public void addBreakpointListener(IBreakpointListener listener) {
		fBreakpointListeners.add(listener);
	}

	/**
	 * @see IBreakpointManager#removeBreakpointListener(IBreakpointListener)
	 */
	@Override
	public void removeBreakpointListener(IBreakpointListener listener) {
		fBreakpointListeners.remove(listener);
	}

	/**
	 * Notifies listeners of the adds/removes/changes
	 *
	 * @param breakpoints associated breakpoints
	 * @param deltas or <code>null</code>
	 * @param update type of change
	 */
	private void fireUpdate(List<IBreakpoint> breakpoints, List<IMarkerDelta> deltas, int update) {
		if (breakpoints.isEmpty()) {
			return;
		}
		IBreakpoint[] bpArray = breakpoints.toArray(new IBreakpoint[breakpoints.size()]);
		IMarkerDelta[] deltaArray = new IMarkerDelta[bpArray.length];
		if (deltas != null) {
			deltaArray = deltas.toArray(deltaArray);
		}
		// single listeners
		getBreakpointNotifier().notify(bpArray, deltaArray, update);

		// plural listeners
		getBreakpointsNotifier().notify(bpArray, deltaArray, update);
	}

	protected void setBreakpoints(Vector<IBreakpoint> breakpoints) {
		fBreakpoints = breakpoints;
	}

	/**
	 * @see IBreakpointManager#hasBreakpoints()
	 */
	@Override
	public boolean hasBreakpoints() {
		return !getBreakpoints0().isEmpty();
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointManager#addBreakpointListener(org.eclipse.debug.core.IBreakpointsListener)
	 */
	@Override
	public void addBreakpointListener(IBreakpointsListener listener) {
		fBreakpointsListeners.add(listener);
	}

	/**
	 * @see org.eclipse.debug.core.IBreakpointManager#removeBreakpointListener(org.eclipse.debug.core.IBreakpointsListener)
	 */
	@Override
	public void removeBreakpointListener(IBreakpointsListener listener) {
		fBreakpointsListeners.remove(listener);
	}

	private BreakpointNotifier getBreakpointNotifier() {
		return new BreakpointNotifier();
	}

	/**
	 * Notifies breakpoint listener (single breakpoint) in a safe runnable to
	 * handle exceptions.
	 */
	class BreakpointNotifier implements ISafeRunnable {

		private IBreakpointListener fListener;
		private int fType;
		private IMarkerDelta fDelta;
		private IBreakpoint fBreakpoint;

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during breakpoint change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.breakpointAdded(fBreakpoint);
					break;
				case REMOVED:
					fListener.breakpointRemoved(fBreakpoint, fDelta);
					break;
				case CHANGED:
					fListener.breakpointChanged(fBreakpoint, fDelta);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies the listeners of the add/change/remove
		 *
		 * @param breakpoints the breakpoints that changed
		 * @param deltas the deltas associated with the change
		 * @param update the type of change
		 */
		public void notify(IBreakpoint[] breakpoints, IMarkerDelta[] deltas, int update) {
			fType = update;
			for (IBreakpointListener iBreakpointListener : fBreakpointListeners) {
				fListener = iBreakpointListener;
				for (int j = 0; j < breakpoints.length; j++) {
					fBreakpoint = breakpoints[j];
					fDelta = deltas[j];
					SafeRunner.run(this);
				}
			}
			fListener = null;
			fDelta = null;
			fBreakpoint = null;
		}
	}

	private BreakpointsNotifier getBreakpointsNotifier() {
		return new BreakpointsNotifier();
	}

	/**
	 * Notifies breakpoint listener (multiple breakpoints) in a safe runnable to
	 * handle exceptions.
	 */
	class BreakpointsNotifier implements ISafeRunnable {

		private IBreakpointsListener fListener;
		private int fType;
		private IMarkerDelta[] fDeltas;
		private IBreakpoint[] fNotifierBreakpoints;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during breakpoint change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.breakpointsAdded(fNotifierBreakpoints);
					break;
				case REMOVED:
					fListener.breakpointsRemoved(fNotifierBreakpoints, fDeltas);
					break;
				case CHANGED:
					fListener.breakpointsChanged(fNotifierBreakpoints, fDeltas);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies the listeners of the adds/changes/removes
		 *
		 * @param breakpoints the breakpoints that changed
		 * @param deltas the deltas associated with the changed breakpoints
		 * @param update the type of change
		 */
		public void notify(IBreakpoint[] breakpoints, IMarkerDelta[] deltas, int update) {
			fType = update;
			fNotifierBreakpoints = breakpoints;
			fDeltas = deltas;
			for (IBreakpointsListener iBreakpointsListener : fBreakpointsListeners) {
				fListener = iBreakpointsListener;
				SafeRunner.run(this);
			}
			fDeltas = null;
			fNotifierBreakpoints = null;
			fListener = null;
		}
	}

	@Override
	public boolean isEnabled() {
		return Platform.getPreferencesService().getBoolean(DebugPlugin.getUniqueIdentifier(), IInternalDebugCoreConstants.PREF_BREAKPOINT_MANAGER_ENABLED_STATE, true, null);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (isEnabled() != enabled) {
			Preferences.setBoolean(DebugPlugin.getUniqueIdentifier(), IInternalDebugCoreConstants.PREF_BREAKPOINT_MANAGER_ENABLED_STATE, enabled, null);
			touchAllBreakpoints();
			new BreakpointManagerNotifier().notify(enabled);
		}
	}

	@Override
	public void addBreakpointManagerListener(IBreakpointManagerListener listener) {
		fBreakpointManagerListeners.add(listener);
	}

	@Override
	public void removeBreakpointManagerListener(IBreakpointManagerListener listener) {
		fBreakpointManagerListeners.remove(listener);
	}

	/**
	 * Notifies breakpoint manager listeners in a safe runnable to
	 * handle exceptions.
	 */
	class BreakpointManagerNotifier implements ISafeRunnable {

		private IBreakpointManagerListener fListener;
		private boolean fManagerEnabled;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during breakpoint change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			fListener.breakpointManagerEnablementChanged(fManagerEnabled);
		}

		/**
		 * Notifies the listeners of the enabled state change
		 *
		 * @param enabled whether the manager is enabled
		 */
		public void notify(boolean enabled) {
			fManagerEnabled= enabled;
			for (IBreakpointManagerListener iBreakpointManagerListener : fBreakpointManagerListeners) {
				fListener = iBreakpointManagerListener;
				SafeRunner.run(this);
			}
			fListener = null;
		}
	}

	/**
	 * Notifies breakpoint manager listeners in a safe runnable to handle
	 * exceptions.
	 */
	class BreakpointManagerTriggerPointNotifier implements ISafeRunnable {

		private IBreakpointManagerListener fListener;
		private IBreakpoint fManagerTriggerPoint;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during breakpoint change notification.", exception); //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			fListener.breakpointManagerTriggerPointChanged(fManagerTriggerPoint);
		}

		/**
		 * Notifies the listeners of the enabled state change
		 *
		 * @param triggerBreakpoint new breakpoint as trigger point
		 */
		public void notify(IBreakpoint triggerBreakpoint) {
			fManagerTriggerPoint = triggerBreakpoint;
			for (IBreakpointManagerListener iBreakpointManagerListener : fBreakpointManagerListeners) {
				fListener = iBreakpointManagerListener;
				SafeRunner.run(this);
			}
			fListener = null;
		}
	}

	class BreakpointManagerJob extends Job {

		private final IWorkspaceRunnable fRunnable;

		public BreakpointManagerJob (IWorkspaceRunnable wRunnable) {
			super("breakpoint manager job"); //$NON-NLS-1$
			fRunnable= wRunnable;
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				getWorkspace().run(fRunnable, null, 0, null);
			} catch (CoreException ce) {
				DebugPlugin.log(ce);
			}
			return new Status(IStatus.OK, DebugPlugin.getUniqueIdentifier(), IStatus.OK, "", null); //$NON-NLS-1$
		}
	}

	@Override
	public String getTypeName(IBreakpoint breakpoint) {
		String typeName= null;
		IMarker marker = breakpoint.getMarker();
		if (marker != null) {
			try {
				IConfigurationElement element = fBreakpointExtensions.get(marker.getType());
				if (element != null) {
					typeName= element.getAttribute(IConfigurationElementConstants.NAME);
				}
			}
			catch (CoreException e) {}
		}
		return typeName;
	}

	@Override
	public IBreakpointImportParticipant[] getImportParticipants(String markertype) throws CoreException {
		initializeImportParticipants();
		ArrayList<BreakpointImportParticipantDelegate> list = fImportParticipants.get(markertype);
		if(list == null) {
			return new IBreakpointImportParticipant[] {fDefaultParticipant};
		}
		IBreakpointImportParticipant[] participants = new IBreakpointImportParticipant[list.size()];
		BreakpointImportParticipantDelegate delegate = null;
		for(int i = 0; i < list.size(); i++) {
			delegate = list.get(i);
			participants[i] = delegate.getDelegate();
		}
		if(participants.length == 0) {
			return new IBreakpointImportParticipant[] {fDefaultParticipant};
		}
		return participants;
	}

	/**
	 * Initializes the cache of breakpoint import participants. Does no work if the cache
	 * has already been initialized
	 */
	private synchronized void initializeImportParticipants() {
		if(fImportParticipants == null) {
			fImportParticipants = new HashMap<>();
			fDefaultParticipant = new DefaultImportParticipant();
			IExtensionPoint ep = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), DebugPlugin.EXTENSION_POINT_BREAKPOINT_IMPORT_PARTICIPANTS);
			String type = null;
			ArrayList<BreakpointImportParticipantDelegate> list = null;
			for (IConfigurationElement element : ep.getConfigurationElements()) {
				type = element.getAttribute(IConfigurationElementConstants.TYPE);
				if(type != null) {
					list = fImportParticipants.get(type);
					if(list == null) {
						list = new ArrayList<>();
						fImportParticipants.put(type, list);
					}
					list.add(new BreakpointImportParticipantDelegate(element));
				}
			}
		}
	}

	@Override
	public IBreakpoint[] getTriggerPoints() {
		return fTriggerPointBreakpointList.toArray(new IBreakpoint[0]);
	}

	@Override
	public void addTriggerPoint(IBreakpoint triggerPoint) throws CoreException {
		if (triggerPoint == null) {
			return;
		}
		fTriggerPointBreakpointList.add(triggerPoint);
		new BreakpointManagerTriggerPointNotifier().notify(triggerPoint);
	}

	@Override
	public void removeTriggerPoint(IBreakpoint breakpoint) throws CoreException {
		if (breakpoint != null) {
			fTriggerPointBreakpointList.remove(breakpoint);
		}
	}

	@Override
	public void removeAllTriggerPoints() throws CoreException {
		IBreakpoint[] triggerPointBreakpointList = getTriggerPoints();
		for (IBreakpoint iBreakpoint : triggerPointBreakpointList) {
			if (iBreakpoint instanceof ITriggerPoint) {
				((ITriggerPoint) iBreakpoint).setTriggerPoint(false);
			}
		}
		refreshTriggerpointDisplay();
	}

	@Override
	public boolean hasActiveTriggerPoints() {
		if (fTriggerPointBreakpointList.isEmpty()) {
			return false;
		}
		for (IBreakpoint iBreakpoint : fTriggerPointBreakpointList) {
			try {
				if (iBreakpoint.isEnabled()) {
					return true;
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		return false;
	}

	@Override
	public void enableTriggerPoints(IBreakpoint[] triggerPoints, boolean enable) {
		IBreakpoint[] triggerPointArray = triggerPoints;
		if (triggerPoints == null) {
			if (enable) {
				synchronized (fTriggerPointDisabledList) {
					triggerPointArray = fTriggerPointDisabledList.toArray(new IBreakpoint[0]);
				}
			} else {
				triggerPointArray = getTriggerPoints();
			}
		}
		List<IBreakpoint> toDisable = new ArrayList<>();
		for (IBreakpoint iBreakpoint : triggerPointArray) {
			try {
				IMarker m = iBreakpoint.getMarker();
				if (m != null && m.exists()) {
					if (!enable && iBreakpoint.isEnabled()) {
						toDisable.add(iBreakpoint);
					}
					iBreakpoint.setEnabled(enable);
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		synchronized (fTriggerPointDisabledList) {
			fTriggerPointDisabledList.clear();
			if (!enable) {
				fTriggerPointDisabledList.addAll(toDisable);
			}
		}
	}

	@Override
	public void refreshTriggerpointDisplay() {
		touchAllBreakpoints();
	}

	/*
	 * Touch and refresh display of all breakpoints
	 */
	private void touchAllBreakpoints() {
		IWorkspaceRunnable runnable = monitor -> {
			for (IBreakpoint breakpoint : getBreakpoints()) {
				// Touch the marker (but don't actually change anything) so
				// that the icon in
				// the editor ruler will be updated (editors listen to
				// marker changes).
				try {
					breakpoint.getMarker().setAttribute(IBreakpoint.ENABLED, breakpoint.isEnabled());
				} catch (CoreException e) {
					// don't care if marker was already deleted
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(runnable, null, IWorkspace.AVOID_UPDATE, null);
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
	}
}

