/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.contextlaunching;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener;
import org.eclipse.debug.internal.ui.ILaunchLabelChangedListener;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.eclipse.ui.internal.WorkbenchWindow;

/**
 * This manager is used to calculate the labels for the current resource or for the current
 * state of the launch history, depending on the enabled status of contextual launching. More specifically
 * if contextual launching is enabled the calculated labels are for the current resource, otherwise
 * the calculated labels are for the current state of the launch history.
 *
 * Any actions interested in being notified of launch label updates need to register with this manager, and implement
 * the <code>ILaunchLabelChangedListener</code> interface.
 *
 * @see ILaunchLabelChangedListener
 * @see org.eclipse.debug.ui.actions.AbstractLaunchHistoryAction
 *
 * @since 3.3
 */
public class LaunchingResourceManager implements IPropertyChangeListener, IWindowListener, ISelectionListener, ILaunchHistoryChangedListener, ILaunchesListener2 {

	/**
	 *The set of label update listeners
	 */
	private ListenerList<ILaunchLabelChangedListener> fLabelListeners = new ListenerList<>();

	/**
	 * The map of ToolBars that have mouse tracker listeners associated with them:
	 * stored as Map<IWorkbenchWindow, ToolBar>
	 */
	private HashMap<IWorkbenchWindow, ToolBar> fToolbars = new HashMap<>();

	/**
	 * the map of current labels
	 */
	private HashMap<ILaunchGroup, String> fCurrentLabels = new HashMap<>();

	/**
	 * The selection has changed and we need to update the labels
	 */
	private boolean fUpdateLabel = true;

	/**
	 * Set of windows that have been opened and that we have registered selection listeners with
	 */
	private HashSet<IWorkbenchWindow> fWindows = new HashSet<>();

	/**
	 * Cache of IResource -> ILaunchConfiguration[] used during a tooltip update job.
	 * The cache is cleared after each tooltip update job is complete.
	 */
	private HashMap<IResource, ILaunchConfiguration[]> fConfigCache = new HashMap<>();

	/**
	 * Cache of IResource -> LaunchShortcutExtension used during a tooltip update job.
	 * The cache is cleared after each tooltip update job is complete.
	 */
	private HashMap<IResource, List<LaunchShortcutExtension>> fExtCache = new HashMap<>();

	/**
	 * Constant denoting the empty string;
	 */
	private static final String EMPTY_STRING = IInternalDebugCoreConstants.EMPTY_STRING;

	/**
	 * Provides a mouse tracker listener for the launching main toolbar
	 */
	private MouseTrackAdapter fMouseListener = new MouseTrackAdapter() {
		@Override
		public void mouseEnter(MouseEvent e) {
			if(fUpdateLabel) {
				fUpdateLabel = false;
				fCurrentLabels.clear();
				Job job = new Job("Compute launch button tooltip") { //$NON-NLS-1$
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						computeLabels();
						fConfigCache.clear();
						fExtCache.clear();
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
			}
		}
	};

	/**
	 * Returns if context launching is enabled
	 * @return if context launching is enabled
	 */
	public static boolean isContextLaunchEnabled() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH);
	}

	/**
	 * Returns if context launching is enabled or not. Context launching is enabled iff:
	 * <ul>
	 * <li>The preference is turned on</li>
	 * <li>the launch group id is not <code>org.eclipse.ui.externaltools.launchGroup</code></li>
	 * </ul>
	 * @param launchgroupid the id of the {@link ILaunchGroup}
	 * @return <code>true</code> if context launching is enabled <code>false</code> otherwise
	 */
	public static boolean isContextLaunchEnabled(String launchgroupid) {
		return isContextLaunchEnabled() && !"org.eclipse.ui.externaltools.launchGroup".equals(launchgroupid); //$NON-NLS-1$
	}

	/**
	 * Allows an <code>AbstractLaunchHistoryAction</code> to register with this manager to be notified
	 * of a context (<code>IResource</code>) change and have its updateToolTip(..) method called back to.
	 * <br><br>
	 * Obeys the contract of listener addition as outlined in {@link ListenerList#add(Object)}
	 * @param listener the {@link ILaunchLabelChangedListener} to add
	 */
	public void addLaunchLabelUpdateListener(ILaunchLabelChangedListener listener) {
		fLabelListeners.add(listener);
	}

	/**
	 * Removes the specified <code>AbstractLaunchHistoryAction</code> from the listing of registered
	 * listeners
	 * <br><br>
	 * Obeys the contract of listener removal as outlined in {@link ListenerList#remove(Object)}
	 * @param listener the {@link ILaunchLabelChangedListener} to remove
	 */
	public void removeLaunchLabelChangedListener(ILaunchLabelChangedListener listener) {
		fLabelListeners.remove(listener);
	}

	/**
	 * Returns the current resource label to be displayed.
	 *
	 * @param group the launch group to get the label for
	 * @return the current resource label;
	 */
	public String getLaunchLabel(ILaunchGroup group) {
		return fCurrentLabels.get(group);
	}

	/**
	 * Returns if the parent project should be checked automatically
	 * @return true if the parent project should checked automatically, false otherwise
	 */
	protected boolean shouldCheckParent() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT);
	}

	/**
	 * Returns if the the last launch configuration should be launched if the selected resource is not launchable and context launching is enabled
	 * @return true if the last launched should be launched, false otherwise
	 */
	protected boolean shouldLaunchLast() {
		return DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_LAUNCH_LAST_IF_NOT_LAUNCHABLE);
	}

	/**
	 * Computes the current listing of labels for the given <code>IResource</code> context change or the
	 * current launch history changed event
	 */
	protected void computeLabels() {
		ILaunchGroup group = null;
		ILaunchConfiguration config = null;
		String label = null;

		SelectedResourceManager srm = SelectedResourceManager.getDefault();
		IStructuredSelection selection = srm.getCurrentSelection();
		List<LaunchShortcutExtension> shortcuts = null;
		IResource resource = srm.getSelectedResource();
		for (ILaunchLabelChangedListener iLaunchLabelChangedListener : fLabelListeners) {
			group = iLaunchLabelChangedListener.getLaunchGroup();
			if(group != null) {
				if(isContextLaunchEnabled(group.getIdentifier())) {
					shortcuts = getShortcutsForSelection(selection, group.getMode());
					if(resource == null) {
						resource = getLaunchableResource(shortcuts, selection);
					}
					label = getLabel(selection, resource, shortcuts, group);
				}
				else {
					config = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getFilteredLastLaunch(group.getIdentifier());
					if(config != null) {
						label = appendLaunched(config);
					}
				}
				fCurrentLabels.put(group, label);
				label = null;
			}
		}
		notifyLabelChanged();
	}

	/**
	 * Notifies all registered listeners that the known labels have changed
	 */
	protected void notifyLabelChanged() {
		for (ILaunchLabelChangedListener iLaunchLabelChangedListener : fLabelListeners) {
			iLaunchLabelChangedListener.labelChanged();
		}
	}

	/**
	 * Appends the text '(already running)' to the tooltip label if there is a launch currently
	 * running (not terminated) with the same backing launch configuration as the one specified
	 * @param config the {@link ILaunchConfiguration} to check for running state
	 * @return the appended string for the tooltip label or the configuration name (default)
	 */
	private String appendLaunched(ILaunchConfiguration config) {
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		boolean launched = false;
		ILaunchConfiguration tmp = null;
		for (ILaunch launch : launches) {
			tmp = launch.getLaunchConfiguration();
			if (tmp != null) {
				if (!launch.isTerminated() && tmp.equals(config)) {
					launched = true;
					break;
				}
			}
		}
		if(launched) {
			return MessageFormat.format(ContextMessages.LaunchingResourceManager_0, new Object[] { config.getName() });
		}
		return config.getName();
	}

	/**
	 * Returns the label for the last launched configuration or and empty string if there was no last launch.
	 * @param group the {@link ILaunchGroup} to get the label for
	 * @return the name of the last launched configuration, altered with '(running)' if needed, or the empty
	 * string if there is no last launch.
	 */
	protected String getlastLaunchedLabel(ILaunchGroup group) {
		ILaunchConfiguration config = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getFilteredLastLaunch(group.getIdentifier());
		if(config != null) {
			return appendLaunched(config);
		}
		return EMPTY_STRING;
	}

	/**
	 * Returns the label for the specified resource or the empty string, never <code>null</code>
	 *
	 * @param selection the current {@link IStructuredSelection}
	 * @param resource the backing {@link IResource} for the selection
	 * @param shortcuts the list of {@link ILaunchShortcut}s to consider
	 * @param group the {@link ILaunchGroup} to launch using
	 * @return the label for the resource or the empty string, never <code>null</code>
	 */
	protected String getLabel(IStructuredSelection selection, IResource resource, List<LaunchShortcutExtension> shortcuts, ILaunchGroup group) {
		List<LaunchShortcutExtension> sc = pruneShortcuts(shortcuts, resource, group.getMode());
		LaunchConfigurationManager lcm = DebugUIPlugin.getDefault().getLaunchConfigurationManager();
		//see if the context is a shared configuration
		ILaunchConfiguration config = lcm.isSharedConfig(resource);
		if(config != null) {
			return appendLaunched(config);
		}
		List<ILaunchConfiguration> configs = getParticipatingLaunchConfigurations(selection, resource, sc, group.getMode());
		int csize = configs.size();
		if(csize == 1) {
			return appendLaunched(configs.get(0));
		}
		else if(csize > 1) {
			config = lcm.getMRUConfiguration(configs, group, resource);
			if(config != null) {
				return appendLaunched(config);
			}
			else {
				return ContextMessages.ContextRunner_14;
			}
		}
		else {
			List<LaunchShortcutExtension> exts = fExtCache.get(resource);
			if(exts == null && resource != null) {
				fExtCache.put(resource, sc);
			}
			int esize = sc.size();
			if(esize == 0) {
				if(resource != null && shouldCheckParent()) {
					IProject project = resource.getProject();
					if(project != null && !project.equals(resource)) {
						return getLabel(selection, project, sc, group);
					}
				}
				else if(shouldLaunchLast() || resource == null) {
					return getlastLaunchedLabel(group);
				}
				else {
					return ContextMessages.ContextRunner_15;
				}
			}
			if(esize == 1) {
				if(resource != null) {
					return resource.getName();
				}
				else {
					return MessageFormat.format(ContextMessages.LaunchingResourceManager_1, new Object[] { sc.get(0).getLabel() });
				}
			}
			else {
				return ContextMessages.ContextRunner_14;
			}
		}
	}

	/**
	 * Prunes the original listing of shortcuts
	 * @param shortcuts the original listing of <code>LaunchShortcutExtension</code>s
	 * @param resource the derived resource
	 * @param mode the mode we are wanting to launch in
	 * @return the list of {@link ILaunchShortcut}s to consider
	 *
	 * @since 3.4
	 */
	protected List<LaunchShortcutExtension> pruneShortcuts(List<LaunchShortcutExtension> shortcuts, IResource resource, String mode) {
		List<LaunchShortcutExtension> list = new ArrayList<>(shortcuts);
		if(resource == null) {
			LaunchShortcutExtension ext = null;
			for (ListIterator<LaunchShortcutExtension> iter = list.listIterator(); iter.hasNext();) {
				ext = iter.next();
				if(!ext.isParticipant()) {
					iter.remove();
				}
			}
		}
		else {
			list = getShortcutsForSelection(new StructuredSelection(resource), mode);
		}
		return list;
	}

	/**
	 * Computes the current resources context, given all of the launch shortcut participants
	 * and the current selection
	 * @param shortcuts the list of {@link ILaunchShortcut} to ask for mapped resources
	 * @param selection the current workbench {@link IStructuredSelection}
	 * @return The set of resources who care about this launch
	 *
	 * @since 3.4
	 */
	public IResource getLaunchableResource(List<LaunchShortcutExtension> shortcuts, IStructuredSelection selection) {
		if(selection != null && !selection.isEmpty()) {
			ArrayList<IResource> resources = new ArrayList<>();
			IResource resource = null;
			Object o = selection.getFirstElement();
			for (LaunchShortcutExtension ext : shortcuts) {
				if(o instanceof IEditorPart) {
					resource = ext.getLaunchableResource((IEditorPart) o);
				}
				else {
					resource = ext.getLaunchableResource(selection);
				}
				if(resource != null && !resources.contains(resource)) {
					resources.add(resource);
					resource = null;
				}
			}
			if(resources.size() > 0) {
				return resources.get(0);
			}
		}
		return null;
	}

	/**
	 * Returns the launch shortcuts that apply to the current <code>IStructuredSelection</code>
	 * @param selection the current selection
	 * @param mode the mode
	 * @return the list of shortcuts that apply to the given selection and mode or an empty listing, never <code>null</code>
	 *
	 * @since 3.4
	 */
	public List<LaunchShortcutExtension> getShortcutsForSelection(IStructuredSelection selection, String mode) {
		ArrayList<LaunchShortcutExtension> list = new ArrayList<>();
		List<LaunchShortcutExtension> sc = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchShortcuts();
		List<IEditorInput> ctxt = new ArrayList<>();
		// work around to bug in Structured Selection that returns actual underlying array in selection
		// @see bug 211646
		ctxt.addAll(selection.toList());
		Object o = selection.getFirstElement();
		if(o instanceof IEditorPart) {
			ctxt.set(0, ((IEditorPart)o).getEditorInput());
		}
		IEvaluationContext context = DebugUIPlugin.createEvaluationContext(ctxt);
		context.addVariable("selection", ctxt); //$NON-NLS-1$
		for (LaunchShortcutExtension ext : sc) {
			try {
				if(ext.evalEnablementExpression(context, ext.getContextualLaunchEnablementExpression()) &&
						ext.getModes().contains(mode) && !WorkbenchActivityHelper.filterItem(ext)) {
					if(!list.contains(ext)) {
						list.add(ext);
					}
				}
			}
			catch(CoreException ce) {}
		}
		return list;
	}

	/**
	 * Returns a listing of all launch configurations that want to participate in the contextual
	 * launch of the specified resource or specified selection
	 * @param resource the underlying resource
	 * @param selection the current selection in the workbench
	 * @param shortcuts the listing of shortcut extensions that apply to the current context
	 * @param mode the mode
	 * @return a listing of all launch configurations wanting to participate in the current launching
	 *
	 * @since 3.4
	 */
	public List<ILaunchConfiguration> getParticipatingLaunchConfigurations(IStructuredSelection selection, IResource resource, List<LaunchShortcutExtension> shortcuts, String mode) {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		int voteDefault = 0;
		if(selection != null) {
			Object o = selection.getFirstElement();
			LaunchShortcutExtension ext = null;
			ILaunchConfiguration[] cfgs = null;
			for (LaunchShortcutExtension shortcut : shortcuts) {
				ext = shortcut;
				if(o instanceof IEditorPart) {
					cfgs = ext.getLaunchConfigurations((IEditorPart)o);
				}
				else {
					 cfgs = ext.getLaunchConfigurations(selection);
				}
				if (cfgs == null) {
					Set<String> types = ext.getAssociatedConfigurationTypes();
					addAllToList(configs, DebugUIPlugin.getDefault().getLaunchConfigurationManager().getApplicableLaunchConfigurations(types.toArray(new String[types.size()]), resource));
					voteDefault++;
				} else {
					if(cfgs.length > 0) {
						Collections.addAll(configs, cfgs);
					}
				}
			}
		}
		if (voteDefault == shortcuts.size()) {
			// consider default configurations if no configurations were contributed
			addAllToList(configs, DebugUIPlugin.getDefault().getLaunchConfigurationManager().getApplicableLaunchConfigurations(null, resource));
		}
		Iterator<ILaunchConfiguration> iterator = configs.iterator();
		while (iterator.hasNext()) {
			ILaunchConfiguration config = iterator.next();
			try {
				Set<String> modes = config.getModes();
				modes.add(mode);
				if (!config.getType().supportsModeCombination(modes)) {
					iterator.remove();
				}
			}
			catch (CoreException e) {}
		}
		return configs;
	}

	/**
	 * Adds all of the items in the given object array to the given collection.
	 * Does nothing if either the collection or array is <code>null</code>.
	 * @param list the {@link List} to append to
	 * @param values the array of {@link Object}s to add to the list
	 */
	private void addAllToList(List<ILaunchConfiguration> list, ILaunchConfiguration[] configs) {
		if (list == null || configs == null) {
			return;
		}
		for (ILaunchConfiguration config : configs) {
			if (!list.contains(config)) {
				list.add(config);
			}
		}
	}

	/**
	 * Starts up the manager
	 */
	public void startup() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench != null) {
			workbench.addWindowListener(this);
			// initialize for already open windows
			IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();
			for (IWorkbenchWindow workbenchWindow : workbenchWindows) {
				if (workbenchWindow.getSelectionService() != null) {
					windowOpened(workbenchWindow);
				}
			}
		}
		DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		DebugUIPlugin.getDefault().getLaunchConfigurationManager().addLaunchHistoryListener(this);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	/**
	 * Shutdown and clean up the manager
	 */
	public void shutdown() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench != null) {
			workbench.removeWindowListener(this);
		}
		DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		DebugUIPlugin.getDefault().getLaunchConfigurationManager().removeLaunchHistoryListener(this);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
		for (IWorkbenchWindow window : fWindows) {
			window.getSelectionService().removeSelectionListener(this);
		}
		// set fUpdateLabel to false so that mouse track listener will do nothing if called
		// before the asynchronous execution disposes them
		fUpdateLabel = false;
		for (Entry<IWorkbenchWindow, ToolBar> entry : fToolbars.entrySet()) {
			final ToolBar bar = entry.getValue();
			if(bar != null && !bar.isDisposed()) {
				final MouseTrackAdapter listener = fMouseListener;
				DebugUIPlugin.getStandardDisplay().asyncExec(() -> bar.removeMouseTrackListener(listener));

			}
		}
		fWindows.clear();
		fToolbars.clear();
		fLabelListeners.clear();
		fCurrentLabels.clear();
	}

	/**
	 * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowActivated(IWorkbenchWindow window) {
		if(!fToolbars.containsKey(window)) {
			addMouseListener(window);
		}
	}

	/**
	 * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowClosed(IWorkbenchWindow window) {
		ToolBar bar = fToolbars.remove(window);
		if(bar != null && !bar.isDisposed()) {
			bar.removeMouseTrackListener(fMouseListener);
		}
		if(fWindows.remove(window)) {
			window.getSelectionService().removeSelectionListener(this);
		}
	}

	/**
	 * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowDeactivated(IWorkbenchWindow window) {}

	/**
	 * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowOpened(IWorkbenchWindow window) {
		if(fWindows.add(window)) {
			window.getSelectionService().addSelectionListener(this);
		}
	}

	/**
	 * Adds a mouse listener to the launch toolbar
	 *
	 * @param window the {@link IWorkbenchWindow} to work with
	 */
	private void addMouseListener(IWorkbenchWindow window) {
		ICoolBarManager cmgr = ((WorkbenchWindow)window).getCoolBarManager2();
		if(cmgr != null) {
			IContributionItem item = cmgr.find("org.eclipse.debug.ui.launchActionSet"); //$NON-NLS-1$
			if(item instanceof ToolBarContributionItem) {
				ToolBarManager tmgr = (ToolBarManager) ((ToolBarContributionItem)item).getToolBarManager();
				ToolBar bar = tmgr.getControl();
				if(bar != null && !bar.isDisposed()) {
					bar.addMouseTrackListener(fMouseListener);
					fToolbars.put(window, bar);
				}
			}
		}
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if(event.getProperty().equals(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH) ||
				event.getProperty().equals(IInternalDebugUIConstants.PREF_LAUNCH_LAST_IF_NOT_LAUNCHABLE)) {
			if(isContextLaunchEnabled()) {
				windowActivated(DebugUIPlugin.getActiveWorkbenchWindow());
			}
			fUpdateLabel = true;
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(isContextLaunchEnabled()) {
			fUpdateLabel = true;
		}
	}

	@Override
	public void launchHistoryChanged() {
		//this always must be set to true, because as the history is loaded these events are fired, and we need to
		//update on workspace load.
		fUpdateLabel = true;
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		fUpdateLabel = true;
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		fUpdateLabel = true;
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		//we want to ensure that even if a launch is removed from the debug view
		//when it is not terminated we update the label just in case.
		//bug 195232
		for (ILaunch launch : launches) {
			if (!launch.isTerminated()) {
				fUpdateLabel = true;
				return;
			}
		}
	}
}
