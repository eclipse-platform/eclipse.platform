/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.DelegatingModelPresentation;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.LazyModelPresentation;
import org.eclipse.debug.internal.ui.TerminateToggleValue;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.ToggleBreakpointsTargetManager;
import org.eclipse.debug.internal.ui.contextlaunching.LaunchingResourceManager;
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationPropertiesDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTabGroupViewer;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupExtension;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.internal.ui.memory.MemoryRenderingManager;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupFacility;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIUtils;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.debug.internal.ui.views.breakpoints.DeleteBreakpointMarkersOperation;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetManager;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextManager;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.debug.ui.sourcelookup.ISourceLookupResult;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;


/**
 * This class provides utilities for clients of the debug UI.
 * <p>
 * Images retrieved from this facility should not be disposed.
 * The images will be disposed when this plug-in is shutdown.
 * </p>
 * <p>
 * Note: all methods in this class are expected to be called
 * on the Display thread unless otherwise noted.
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DebugUITools {

	/**
	 * The undo context for breakpoints.
	 *
	 * @since 3.7
	 */
	private static ObjectUndoContext fgBreakpointsUndoContext;

	/**
	 * Returns the shared image managed under the given key, or <code>null</code>
	 * if none.
	 * <p>
	 * Note that clients <b>MUST NOT</b> dispose the image returned by this method.
	 * </p>
	 * <p>
	 * See <code>IDebugUIConstants</code> for available images.
	 * </p>
	 *
	 * @param key the image key
	 * @return the image, or <code>null</code> if none
	 * @see IDebugUIConstants
	 */
	public static Image getImage(String key) {
		return DebugPluginImages.getImage(key);
	}

	/**
	 * Returns the shared image descriptor managed under the given key, or
	 * <code>null</code> if none.
	 * <p>
	 * See <code>IDebugUIConstants</code> for available image descriptors.
	 * </p>
	 *
	 * @param key the image descriptor key
	 * @return the image descriptor, or <code>null</code> if none
	 * @see IDebugUIConstants
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return DebugPluginImages.getImageDescriptor(key);
	}

	/**
	 * Returns the default image descriptor for the given element.
	 *
	 * @param element the element
	 * @return the image descriptor or <code>null</code> if none
	 */
	public static ImageDescriptor getDefaultImageDescriptor(Object element) {
		String imageKey= getDefaultImageKey(element);
		if (imageKey == null) {
			return null;
		}
		return DebugPluginImages.getImageDescriptor(imageKey);
	}

	private static String getDefaultImageKey(Object element) {
		return ((DefaultLabelProvider)DebugUIPlugin.getDefaultLabelProvider()).getImageKey(element);
	}

	/**
	 * Returns the preference store for the debug UI plug-in.
	 *
	 * @return preference store
	 */
	public static IPreferenceStore getPreferenceStore() {
		return DebugUIPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Returns a new debug model presentation that delegates to
	 * appropriate debug models.
	 * <p>
	 * It is the client's responsibility dispose the presentation.
	 * </p>
	 *
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 * @return a debug model presentation
	 * @since 2.0
	 */
	public static IDebugModelPresentation newDebugModelPresentation() {
		return new DelegatingModelPresentation();
	}

	/**
	 * Returns a new debug model presentation for specified
	 * debug model, or <code>null</code> if a presentation does
	 * not exist.
	 * <p>
	 * It is the client's responsibility dispose the presentation.
	 * </p>
	 *
	 * @param identifier debug model identifier
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 * @return a debug model presentation, or <code>null</code>
	 * @since 2.0
	 */
	public static IDebugModelPresentation newDebugModelPresentation(String identifier) {
		IExtensionPoint point= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.ID_DEBUG_MODEL_PRESENTATION);
		if (point != null) {
			IExtension[] extensions= point.getExtensions();
			for (IExtension extension : extensions) {
				IConfigurationElement[] configElements= extension.getConfigurationElements();
				for (IConfigurationElement elt : configElements) {
					String id= elt.getAttribute("id"); //$NON-NLS-1$
					if (id != null && id.equals(identifier)) {
						return new LazyModelPresentation(elt);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the element of the currently selected context in the
	 * active workbench window. Returns <code>null</code> if there is no
	 * current debug context.
	 * <p>
	 * This method used to return <code>null</code> when called from a non-UI thread,
	 * but since 3.1, this methods also works when called from a non-UI thread.
	 * </p>
	 * @return the currently selected debug context, or <code>null</code>
	 * @since 2.0
	 */
	public static IAdaptable getDebugContext() {
		IWorkbenchWindow activeWindow = SelectedResourceManager.getDefault().getActiveWindow();
		if (activeWindow != null) {
			ISelection activeContext = DebugUITools.getDebugContextManager().getContextService(activeWindow).getActiveContext();
			return getDebugContextElementForSelection(activeContext);
		}
		return null;
	}

	/**
	 * Returns the currently selected context in the given part or part's
	 * workbench window. Returns <code>null</code> if there is no current
	 * debug context.
	 * @param part workbench part where the active context is to be evaluated
	 * @return the currently selected debug context in the given workbench part,
	 * or <code>null</code>
	 * @since 3.8
	 * @see IDebugContextService#getActiveContext(String)
	 * @see IDebugContextService#getActiveContext(String, String)
	 */
	public static ISelection getDebugContextForPart(IWorkbenchPart part) {
		IWorkbenchPartSite site = part.getSite();
		IWorkbenchWindow partWindow = site.getWorkbenchWindow();
		if (partWindow != null) {
			IDebugContextService contextService = DebugUITools.getDebugContextManager().getContextService(partWindow);
			if (site instanceof IViewSite) {
				return contextService.getActiveContext(site.getId(), ((IViewSite)site).getSecondaryId());
			} else {
				return contextService.getActiveContext(site.getId());
			}
		}
		return null;
	}

	/**
	 * Return the undo context that should be used for operations involving breakpoints.
	 *
	 * @return the undo context for breakpoints
	 * @since 3.7
	 */
	public static synchronized IUndoContext getBreakpointsUndoContext() {
		if (fgBreakpointsUndoContext == null) {
			fgBreakpointsUndoContext= new ObjectUndoContext(new Object(), "Breakpoints Context"); //$NON-NLS-1$
			fgBreakpointsUndoContext.addMatch(WorkspaceUndoUtil.getWorkspaceUndoContext());
		}
		return fgBreakpointsUndoContext;
	}

	/**
	 * Deletes the given breakpoints using the operation history, which allows to undo the deletion.
	 *
	 * @param breakpoints the breakpoints to delete
	 * @param shell the shell used for potential user interactions, or <code>null</code> if unknown
	 * @param progressMonitor the progress monitor
	 * @throws CoreException if the deletion fails
	 * @since 3.7
	 */
	public static void deleteBreakpoints(IBreakpoint[] breakpoints, final Shell shell, IProgressMonitor progressMonitor) throws CoreException {
		if (breakpoints.length == 0) {
			// Note: this is not only a small performance optimization but also the fix for
			// bug 344352. When removing no breakpoints the DeleteMarkersOperation will show
			// an error dialog about missing markers.
			return;
		}

		IMarker[] markers= new IMarker[breakpoints.length];
		int markerCount;
		for (markerCount= 0; markerCount < breakpoints.length; markerCount++) {
			if (!breakpoints[markerCount].isRegistered()) {
				break;
			}
			markers[markerCount]= breakpoints[markerCount].getMarker();
			if (markers[markerCount] == null) {
				break;
			}
		}

		// We only offer undo support if all breakpoints are registered and have associated markers
		boolean allowUndo= markerCount == breakpoints.length;

		DebugPlugin.getDefault().getBreakpointManager().removeBreakpoints(breakpoints, !allowUndo);

		if (allowUndo) {
			IAdaptable context= null;
			if (shell != null) {
				context= new IAdaptable() {
					@SuppressWarnings("unchecked")
					@Override
					public <T> T getAdapter(Class<T> adapter) {
						if (adapter == Shell.class) {
							return (T) shell;
						}
						return null;
					}
				};
			}

			String operationName= markers.length == 1 ? ActionMessages.DeleteBreakpointOperationName : ActionMessages.DeleteBreakpointsOperationName;
			IUndoableOperation deleteMarkerOperation = new DeleteBreakpointMarkersOperation(markers, operationName);
			deleteMarkerOperation.removeContext(WorkspaceUndoUtil.getWorkspaceUndoContext());
			deleteMarkerOperation.addContext(DebugUITools.getBreakpointsUndoContext());
			IOperationHistory operationHistory= PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
			try {
				operationHistory.execute(deleteMarkerOperation, progressMonitor, context);
			} catch (ExecutionException e) {
				throw new CoreException(DebugUIPlugin.newErrorStatus("Exception while deleting breakpoint markers", e)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Returns the currently active context for the given workbench part. Returns
	 * <code>null</code> if there is no current debug context.
	 *
	 * @param site the part's site where to look up the active context
	 * @return the currently active debug context in the given part, or
	 *         <code>null</code>
	 * @since 3.7
	 */
	public static IAdaptable getPartDebugContext(IWorkbenchPartSite site) {
		IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(site.getWorkbenchWindow());
		String id = null;
		String secondaryId = null;
		id = site.getId();
		if (site instanceof IViewSite) {
			secondaryId = ((IViewSite)site).getSecondaryId();
		}
		ISelection activeContext = service.getActiveContext(id, secondaryId);
		return getDebugContextElementForSelection(activeContext);
	}

	/**
	 * Adds the given debug context listener as a listener to the debug context changed events, in
	 * the context of the given workbench part.
	 * <p>
	 * This method is a utility method which ultimately calls
	 * {@link IDebugContextService#addDebugContextListener(IDebugContextListener, String, String)}
	 * using the part id parameters extracted from the given part parameter.
	 * </p>
	 *
	 * @param site the part's site to get the part ID and part secondary ID from
	 * @param listener Debug context listener to add
	 *
	 * @see IDebugContextService#addDebugContextListener(IDebugContextListener, String, String)
	 * @see IDebugContextManager#addDebugContextListener(IDebugContextListener)
	 * @since 3.7
	 */
	public static void addPartDebugContextListener(IWorkbenchPartSite site, IDebugContextListener listener) {
		IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(site.getWorkbenchWindow());
		String id = site.getId();
		String secondaryId = null;
		if (site instanceof IViewSite) {
			secondaryId = ((IViewSite)site).getSecondaryId();
		}
		service.addDebugContextListener(listener, id, secondaryId);
	}

	/**
	 * Removes the given debug context listener as a listener to the debug context changed events,
	 * in the context of the given workbench part.
	 * <p>
	 * This method is a utility method which ultimately calls
	 * {@link IDebugContextService#removeDebugContextListener(IDebugContextListener, String, String)}
	 * using the part id parameters extracted from the given part parameter.
	 * </p>
	 *
	 * @param site the part's site to get the part ID and part secondary ID from
	 * @param listener Debug context listener to remove
	 *
	 * @see IDebugContextService#removeDebugContextListener(IDebugContextListener, String, String)
	 * @see IDebugContextManager#removeDebugContextListener(IDebugContextListener)
	 * @since 3.7
	 */
	public static void removePartDebugContextListener(IWorkbenchPartSite site, IDebugContextListener listener) {
		IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(site.getWorkbenchWindow());
		String id = site.getId();
		String secondaryId = null;
		if (site instanceof IViewSite) {
			secondaryId = ((IViewSite)site).getSecondaryId();
		}
		service.removeDebugContextListener(listener, id, secondaryId);
	}

	/**
	 * Extracts the first element from the given selection and casts it to IAdaptable.
	 *
	 * @param activeContext the selection
	 * @return an adaptable
	 */
	private static IAdaptable getDebugContextElementForSelection(ISelection activeContext) {
		if (activeContext instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) activeContext;
			if (!selection.isEmpty()) {
				Object firstElement = selection.getFirstElement();
				if (firstElement instanceof IAdaptable) {
					return (IAdaptable) firstElement;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the currently selected resource in the active workbench window,
	 * or <code>null</code> if none. If an editor is active, the resource adapter
	 * associated with the editor is returned, if any.
	 *
	 * @return selected resource or <code>null</code>
	 * @since 3.0
	 */
	public static IResource getSelectedResource() {
		return SelectedResourceManager.getDefault().getSelectedResource();
	}

	/**
	 * Returns the process associated with the current debug context.
	 * If there is no debug context currently, the most recently
	 * launched process is returned. If there is no current process
	 * <code>null</code> is returned.
	 *
	 * @return the current process, or <code>null</code>
	 * @since 2.0
	 */
	public static IProcess getCurrentProcess() {
		IAdaptable context = getDebugContext();
		if (context == null) {
			ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
			if (launches.length > 0) {
				context = launches[launches.length - 1];
			}
		}

		if (context instanceof IDebugElement) {
			return ((IDebugElement)context).getDebugTarget().getProcess();
		}

		if (context instanceof IProcess) {
			return (IProcess)context;
		}

		if (context instanceof ILaunch) {
			ILaunch launch= (ILaunch)context;
			IDebugTarget target= launch.getDebugTarget();
			if (target != null) {
				IProcess process = target.getProcess();
				if (process != null) {
					return process;
				}
			}
			IProcess[] ps = launch.getProcesses();
			if (ps.length > 0) {
				return ps[ps.length - 1];
			}
		}

		if (context != null) {
			return context.getAdapter(IProcess.class);
		}

		return null;
	}

	/**
	 * Open the launch configuration dialog with the specified initial selection.
	 * The selection may be <code>null</code>, or contain any mix of
	 * <code>ILaunchConfiguration</code> or <code>ILaunchConfigurationType</code>
	 * elements.
	 * <p>
	 * Before opening a new dialog, this method checks if there is an existing open
	 * launch configuration dialog.  If there is, this dialog is used with the
	 * specified selection.  If there is no existing dialog, a new one is created.
	 * </p>
	 * <p>
	 * Note that if an existing dialog is reused, the <code>mode</code> argument is ignored
	 * and the existing dialog keeps its original mode.
	 * </p>
	 *
	 * @param shell the parent shell for the launch configuration dialog
	 * @param selection the initial selection for the dialog
	 * @param mode the mode (run or debug) in which to open the launch configuration dialog.
	 *  This should be one of the constants defined in <code>ILaunchManager</code>.
	 * @return the return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see ILaunchGroup
	 * @since 2.0
	 * @deprecated use openLaunchConfigurationDialogOnGroup(Shell, IStructuredSelection, String)
	 *  to specify the launch group that the dialog should be opened on. This method will open
	 *  on the launch group with the specified mode and a <code>null</code> category
	 */
	@Deprecated
	public static int openLaunchConfigurationDialog(Shell shell, IStructuredSelection selection, String mode) {
		ILaunchGroup[] groups = getLaunchGroups();
		for (ILaunchGroup group : groups) {
			if (group.getMode().equals(mode) && group.getCategory() == null) {
				return openLaunchConfigurationDialogOnGroup(shell, selection, group.getIdentifier());
			}
		}
		return Window.CANCEL;
	}

	/**
	 * Open the launch configuration dialog with the specified initial selection.
	 * The selection may be <code>null</code>, or contain any mix of
	 * <code>ILaunchConfiguration</code> or <code>ILaunchConfigurationType</code>
	 * elements.
	 * <p>
	 * Before opening a new dialog, this method checks if there is an existing open
	 * launch configuration dialog.  If there is, this dialog is used with the
	 * specified selection.  If there is no existing dialog, a new one is created.
	 * </p>
	 * <p>
	 * Note that if an existing dialog is reused, the <code>mode</code> argument is ignored
	 * and the existing dialog keeps its original mode.
	 * </p>
	 *
	 * @param shell the parent shell for the launch configuration dialog
	 * @param selection the initial selection for the dialog
	 * @param groupIdentifier the identifier of the launch group to display (corresponds to
	 * the identifier of a launch group extension)
	 * @return The return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see ILaunchGroup
	 * @since 2.1
	 */
	public static int openLaunchConfigurationDialogOnGroup(Shell shell, IStructuredSelection selection, String groupIdentifier) {
		return openLaunchConfigurationDialogOnGroup(shell, selection, groupIdentifier, null);
	}

	/**
	 * Open the launch configuration dialog with the specified initial selection.
	 * The selection may be <code>null</code>, or contain any mix of
	 * <code>ILaunchConfiguration</code> or <code>ILaunchConfigurationType</code>
	 * elements.
	 * <p>
	 * Before opening a new dialog, this method checks if there is an existing open
	 * launch configuration dialog.  If there is, this dialog is used with the
	 * specified selection.  If there is no existing dialog, a new one is created.
	 * </p>
	 * <p>
	 * Note that if an existing dialog is reused, the <code>mode</code> argument is ignored
	 * and the existing dialog keeps its original mode.
	 * </p>
	 * <p>
	 * If a status is specified, a status handler is consulted to handle the
	 * status. The status handler is passed the instance of the launch
	 * configuration dialog that is opened. This gives the status handler an
	 * opportunity to perform error handling/initialization as required.
	 * </p>
	 * @param shell the parent shell for the launch configuration dialog
	 * @param selection the initial selection for the dialog
	 * @param groupIdentifier the identifier of the launch group to display (corresponds to
	 * the identifier of a launch group extension)
	 * @param status the status to display in the dialog, or <code>null</code>
	 * if none
	 * @return the return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see org.eclipse.debug.core.IStatusHandler
	 * @see ILaunchGroup
	 * @since 2.1
	 */
	public static int openLaunchConfigurationDialogOnGroup(final Shell shell, final IStructuredSelection selection, final String groupIdentifier, final IStatus status) {
		final int[] result = new int[1];
		Runnable r = () -> {
			LaunchConfigurationsDialog dialog = (LaunchConfigurationsDialog) LaunchConfigurationsDialog
					.getCurrentlyVisibleLaunchConfigurationDialog();
			if (dialog != null) {
				dialog.setInitialSelection(selection);
				dialog.doInitialTreeSelection();
				if (status != null) {
					dialog.handleStatus(status);
				}
				result[0] = Window.OK;
			} else {
				LaunchGroupExtension ext = DebugUIPlugin.getDefault().getLaunchConfigurationManager()
						.getLaunchGroup(groupIdentifier);
				if (ext != null) {
					dialog = new LaunchConfigurationsDialog(shell, ext);
					dialog.setOpenMode(LaunchConfigurationsDialog.LAUNCH_CONFIGURATION_DIALOG_OPEN_ON_SELECTION);
					dialog.setInitialSelection(selection);
					dialog.setInitialStatus(status);
					result[0] = dialog.open();
				} else {
					result[0] = Window.CANCEL;
				}
			}
		};
		BusyIndicator.showWhile(DebugUIPlugin.getStandardDisplay(), r);
		return result[0];
	}

	/**
	 * Open the launch configuration properties dialog on the specified launch
	 * configuration.
	 *
	 * @param shell the parent shell for the launch configuration dialog
	 * @param configuration the configuration to display
	 * @param groupIdentifier group identifier of the launch group the launch configuration
	 * belongs to
	 * @return the return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see ILaunchGroup
	 * @since 2.1
	 */
	public static int openLaunchConfigurationPropertiesDialog(Shell shell, ILaunchConfiguration configuration, String groupIdentifier) {
		return openLaunchConfigurationPropertiesDialog(shell, configuration, groupIdentifier, null);
	}

	/**
	 * Open the launch configuration properties dialog on the specified launch
	 * configuration.
	 *
	 * @param shell the parent shell for the launch configuration dialog
	 * @param configuration the configuration to display
	 * @param groupIdentifier group identifier of the launch group the launch configuration
	 * belongs to
	 * @param status the status to display, or <code>null</code> if none
	 * @return the return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see ILaunchGroup
	 * @since 3.0
	 */
	public static int openLaunchConfigurationPropertiesDialog(Shell shell, ILaunchConfiguration configuration, String groupIdentifier, IStatus status) {
		LaunchGroupExtension group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(groupIdentifier);
		if (group != null) {
			LaunchConfigurationPropertiesDialog dialog = new LaunchConfigurationPropertiesDialog(shell, configuration, group);
			dialog.setInitialStatus(status);
			return dialog.open();
		}

		return Window.CANCEL;
	}

	/**
	 * Open the launch configuration dialog on the specified launch
	 * configuration. The dialog displays the tabs for a single configuration
	 * only (a tree of launch configuration is not displayed), and provides a
	 * launch (run or debug) button.
	 * <p>
	 * If a status is specified, a status handler is consulted to handle the
	 * status. The status handler is passed the instance of the launch
	 * configuration dialog that is opened. This gives the status handler an
	 * opportunity to perform error handling/initialization as required.
	 * </p>
	 * @param shell the parent shell for the launch configuration dialog
	 * @param configuration the configuration to display
	 * @param groupIdentifier group identifier of the launch group the launch configuration
	 * belongs to
	 * @param status the status to display, or <code>null</code> if none
	 * @return the return code from opening the launch configuration dialog -
	 *  one  of <code>Window.OK</code> or <code>Window.CANCEL</code>. <code>Window.CANCEL</code>
	 *  is returned if an invalid launch group identifier is provided.
	 * @see ILaunchGroup
	 * @since 2.1
	 */
	public static int openLaunchConfigurationDialog(Shell shell, ILaunchConfiguration configuration, String groupIdentifier, IStatus status) {
		LaunchGroupExtension group = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(groupIdentifier);
		if (group != null) {
			LaunchConfigurationDialog dialog = new LaunchConfigurationDialog(shell, configuration, group);
			dialog.setInitialStatus(status);
			return dialog.open();
		}

		return Window.CANCEL;
	}

	/**
	 * Saves all dirty editors and builds the workspace according to current
	 * preference settings, and returns whether a launch should proceed.
	 * <p>
	 * The following preferences affect whether dirty editors are saved, and/or if
	 * the user is prompted to save dirty editors:
	 * </p>
	 * <ul>
	 * <li>PREF_NEVER_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * <li>PREF_PROMPT_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * <li>PREF_AUTOSAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * </ul>
	 * The following preference affects whether a build is performed before
	 * launching (if required):
	 * <ul>
	 * <li>PREF_BUILD_BEFORE_LAUNCH</li>
	 * </ul>
	 *
	 * @return whether a launch should proceed
	 * @since 2.0
	 * @deprecated Saving has been moved to the launch delegate
	 *             <code>LaunchConfigurationDelegate</code> to allow for scoped
	 *             saving of resources that are only involved in the current launch,
	 *             no longer the entire workspace
	 */
	@Deprecated
	public static boolean saveAndBuildBeforeLaunch() {
		return DebugUIPlugin.saveAndBuild();
	}

	/**
	 * Saves all dirty editors according to current preference settings, and returns
	 * whether a launch should proceed.
	 * <p>
	 * The following preferences affect whether dirty editors are saved, and/or if
	 * the user is prompted to save dirty editors:
	 * </p>
	 * <ul>
	 * <li>PREF_NEVER_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * <li>PREF_PROMPT_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * <li>PREF_AUTOSAVE_DIRTY_EDITORS_BEFORE_LAUNCH</li>
	 * </ul>
	 *
	 *
	 * @return whether a launch should proceed
	 * @since 2.1
	 * @deprecated Saving has been moved to the launch delegate
	 *             <code>LaunchConfigurationDelegate</code> to allow for scoped
	 *             saving of resources that are only involved in the current launch,
	 *             no longer the entire workspace
	 */
	@Deprecated
	public static boolean saveBeforeLaunch() {
		return DebugUIPlugin.preLaunchSave();
	}

	/**
	 * Saves and builds the workspace according to current preference settings,
	 * and launches the given launch configuration in the specified mode. It
	 * terminates the current launch for the same configuration if it was
	 * specified via Preferences or toggled by Shift.
	 * <p>
	 * This method must be called in the UI thread.
	 * </p>
	 *
	 * @param configuration the configuration to launch
	 * @param mode launch mode - run or debug
	 * @since 2.1
	 */
	public static void launch(final ILaunchConfiguration configuration, final String mode) {
		launch(configuration, mode, DebugUITools.findToggleLaunchForConfig(configuration, mode));
	}

	private static HashMap<Object, Object> fgLaunchToggleTerminateMap = new HashMap<>();

	/**
	 * Stores the toggle data for launch in a Map to be used while launching to
	 * decide if previous launch for same configuration can be terminated.
	 *
	 * @param data the editor or selected tree node
	 * @param isShift is Shift pressed (use <code>false</code> if no support for
	 *            Shift)
	 * @since 3.12
	 */
	public static void storeLaunchToggleTerminate(Object data, Object isShift) {
		synchronized (fgLaunchToggleTerminateMap) {
			fgLaunchToggleTerminateMap.put(data, isShift);
		}
	}

	/**
	 * Stores the toggle data for launch in a Map to be used while launching to
	 * decide if previous launch for same configuration can be terminated.
	 *
	 * @param data the editor or selected tree node
	 * @since 3.12
	 */
	public static void removeLaunchToggleTerminate(Object data) {
		synchronized (fgLaunchToggleTerminateMap) {
			if (fgLaunchToggleTerminateMap.containsKey(data)) {
				fgLaunchToggleTerminateMap.remove(data);
			}
		}
	}

	/**
	 * @since 3.12
	 */
	private static boolean isShiftTerminateLaunch(Object data) {
		Object value;
		synchronized (fgLaunchToggleTerminateMap) {
			value = fgLaunchToggleTerminateMap.get(data);
		}
		if (value instanceof TerminateToggleValue) {
			return ((TerminateToggleValue) value).isShift();
		} else if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		return Boolean.FALSE;
	}

	/**
	 * @since 3.12
	 */

	private static Object getToggleTerminateValue(Object data) {
		Object value;
		synchronized (fgLaunchToggleTerminateMap) {
			value = fgLaunchToggleTerminateMap.get(data);
		}
		return value;
	}

	/**
	 * @since 3.12
	 */
	private static boolean findToggleLaunchForConfig(ILaunchConfiguration configuration, String mode) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunch[] launches = launchManager.getLaunches();
		for (ILaunch iLaunch : launches) {
			if (configuration.contentsEqual(iLaunch.getLaunchConfiguration())) {
				try {
					IResource[] configResource = iLaunch.getLaunchConfiguration().getMappedResources();
					if (configResource != null && configResource.length == 1) {
						for (Object key : fgLaunchToggleTerminateMap.keySet()) {
							if (key instanceof IEditorPart) {
								IEditorInput input = ((IEditorPart) key).getEditorInput();
								if (input.getAdapter(IResource.class).equals(configResource[0])) {
									return isShiftTerminateLaunch(key);
								}
							} else if (key instanceof TreeSelection) {
								TreeSelection selection = (TreeSelection) key;
								TreePath[] treePath = selection.getPaths();
								if (treePath != null && treePath.length == 1) {
									Object lastSegmentObj = treePath[0].getLastSegment();
									IResource selectedResource = Adapters.adapt(lastSegmentObj, IResource.class);
									if (selectedResource!= null && selectedResource.equals(configResource[0])) {
										return isShiftTerminateLaunch(key);
									}
								}
							}
						}
					} else {
						for (Object key : fgLaunchToggleTerminateMap.keySet()) {
							if (key instanceof IStructuredSelection) {
								Object toggleValue = getToggleTerminateValue(key);
								if (toggleValue instanceof TerminateToggleValue) {
									LaunchingResourceManager lrm = DebugUIPlugin.getDefault().getLaunchingResourceManager();
									ArrayList<LaunchShortcutExtension> shortcuts = new ArrayList<>();
									LaunchShortcutExtension shortcut = ((TerminateToggleValue) toggleValue).getShortcut();
									shortcuts.add(shortcut);
									IResource resource = SelectedResourceManager.getDefault().getSelectedResource();
									if (resource == null) {
										resource = lrm.getLaunchableResource(shortcuts, (IStructuredSelection) key);
									}
									List<ILaunchConfiguration> configs = lrm.getParticipatingLaunchConfigurations((IStructuredSelection) key, resource, shortcuts, mode);
									if (configs.contains(configuration)) {
										return ((TerminateToggleValue) toggleValue).isShift();
									}
								}
							}
						}
					}

				} catch (CoreException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
		return false;

	}

	/**
	 * Saves and builds the workspace according to current preference settings,
	 * and launches the given launch configuration in the specified mode.
	 * <p>
	 * This method must be called in the UI thread.
	 * </p>
	 *
	 * @param configuration the configuration to launch
	 * @param mode launch mode - run or debug
	 * @since 3.12
	 */
	public static void reLaunch(final ILaunchConfiguration configuration, final String mode) {
		boolean launchInBackground = true;
		try {
			launchInBackground = configuration.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		if (launchInBackground) {
			DebugUIPlugin.launchInBackground(configuration, mode);
		} else {
			DebugUIPlugin.launchInForeground(configuration, mode);
		}

	}


	/**
	 * Saves and builds the workspace according to current preference settings,
	 * and launches the given launch configuration in the specified mode. It
	 * terminates the current launch for the same configuration if it was
	 * specified via Preferences or toggled by Shift
	 * <p>
	 * This method must be called in the UI thread.
	 * </p>
	 *
	 * @param configuration the configuration to launch
	 * @param mode launch mode - run or debug
	 * @param isShift is Shift pressed (use <code>false</code> if no support for
	 *            Shift)
	 * @since 3.12
	 */
	public static void launch(final ILaunchConfiguration configuration, final String mode, boolean isShift) {
		if (DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION) != isShift) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunch[] launches = launchManager.getLaunches();
			for (ILaunch iLaunch : launches) {
				if (configuration.contentsEqual(iLaunch.getLaunchConfiguration())) {
					synchronized (fgLaunchList) {
						fgLaunchList.add(iLaunch);
					}
				}
			}
		}
		if (!fgLaunchList.isEmpty()) {
			Thread t = new Thread(fgLaunchTerminate);
			t.start();
			try {
				t.join();
			} catch (InterruptedException e1) {
				DebugUIPlugin.log(e1);
				return;
			}
		}
		boolean launchInBackground = true;
		try {
			launchInBackground = configuration.getAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
		if (launchInBackground) {
			DebugUIPlugin.launchInBackground(configuration, mode);
		} else {
			DebugUIPlugin.launchInForeground(configuration, mode);
		}
	}


	private static Set<ILaunch> fgLaunchList = new LinkedHashSet<>();
	private static Runnable fgLaunchTerminate = () -> {
		String launchConfigName = null;
		Set<ILaunch> launchList;
		try {
			synchronized (fgLaunchList) {
				launchList = new LinkedHashSet<>(fgLaunchList);
				fgLaunchList.clear();
			}
			for (ILaunch iLaunch : launchList) {
				launchConfigName = iLaunch.getLaunchConfiguration().getName();
				iLaunch.terminate();
			}

		} catch (DebugException e) {
			DebugUIPlugin.log(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(),
					NLS.bind(ActionMessages.TerminateAndLaunchFailure, launchConfigName), e));
		}
	};


	/**
	 * Builds the workspace according to current preference settings, and launches
	 * the given configuration in the specified mode, returning the resulting launch
	 * object.
	 * <p>
	 * The following preference affects whether a build is performed before
	 * launching (if required):
	 * </p>
	 * <ul>
	 * <li>PREF_BUILD_BEFORE_LAUNCH</li>
	 * </ul>
	 *
	 *
	 * @param configuration the configuration to launch
	 * @param mode          the mode to launch in
	 * @param monitor       progress monitor
	 * @return the resulting launch object
	 * @throws CoreException if building or launching fails
	 * @since 2.1
	 */
	public static ILaunch buildAndLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return DebugUIPlugin.buildAndLaunch(configuration, mode, monitor);
	}

	/**
	 * Returns the perspective to switch to when a configuration of the given type
	 * is launched in the given mode, or <code>null</code> if no switch should take
	 * place.
	 *
	 * In 3.3 this method is equivalent to calling <code>getLaunchPerspective(ILaunchConfigurationType type, Set modes, ILaunchDelegate delegate)</code>,
	 * with the 'mode' parameter comprising a single element set and passing <code>null</code> as the launch delegate.
	 *
	 * @param type launch configuration type
	 * @param mode launch mode identifier
	 * @return perspective identifier or <code>null</code>
	 * @since 3.0
	 */
	public static String getLaunchPerspective(ILaunchConfigurationType type, String mode) {
		return DebugUIPlugin.getDefault().getPerspectiveManager().getLaunchPerspective(type, mode);
	}

	/**
	 * Returns the perspective id to switch to when a configuration of the given type launched with the specified delegate
	 * is launched in the given mode set, or <code>null</code> if no switch should occurr.
	 * @param type the configuration type
	 * @param delegate the launch delegate
	 * @param modes the set of modes
	 * @return the perspective id or <code>null</code> if no switch should occur
	 *
	 * @since 3.3
	 */
	public static String getLaunchPerspective(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes) {
		return DebugUIPlugin.getDefault().getPerspectiveManager().getLaunchPerspective(type, modes, delegate);
	}

	/**
	 * Sets the perspective to switch to when a configuration of the given type
	 * is launched in the given mode. <code>PERSPECTIVE_NONE</code> indicates no
	 * perspective switch should take place. <code>PERSPECTIVE_DEFAULT</code> indicates
	 * a default perspective switch should take place, as defined by the associated
	 * launch tab group extension.
	 *
	 * In 3.3 this method is equivalent to calling <code>setLaunchPerspective(ILaunchConfigurationType type, Set modes, ILaunchDelegate delegate, String perspectiveid)</code>,
	 * with the parameter 'mode' used in the set modes, and null passed as the delegate
	 *
	 * @param type launch configuration type
	 * @param mode launch mode identifier
	 * @param perspective identifier, <code>PERSPECTIVE_NONE</code>, or
	 *   <code>PERSPECTIVE_DEFAULT</code>
	 * @since 3.0
	 */
	public static void setLaunchPerspective(ILaunchConfigurationType type, String mode, String perspective) {
		DebugUIPlugin.getDefault().getPerspectiveManager().setLaunchPerspective(type, mode, perspective);
	}

	/**
	 * Sets the perspective to switch to when a configuration of the specified type and launched using the
	 * specified launch delegate is launched in the specified modeset. <code>PERSPECTIVE_NONE</code> indicates no
	 * perspective switch should take place.
	 *
	 * Passing <code>null</code> for the launch delegate is quivalent to using the default perspective for the specified
	 * type.
	 * @param type the configuration type
	 * @param delegate the launch delegate
	 * @param modes the set of modes
	 * @param perspectiveid identifier or <code>PERSPECTIVE_NONE</code>
	 *
	 * @since 3.3
	 */
	public static void setLaunchPerspective(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes, String perspectiveid) {
		DebugUIPlugin.getDefault().getPerspectiveManager().setLaunchPerspective(type, modes, delegate, perspectiveid);
	}

	/**
	 * Returns whether the given launch configuration is private. Generally,
	 * private launch configurations should not be displayed to the user. The
	 * private status of a launch configuration is determined by the
	 * <code>IDebugUIConstants.ATTR_PRIVATE</code> attribute.
	 *
	 * @param configuration launch configuration
	 * @return whether the given launch configuration is private
	 * @since 3.0
	 */
	public static boolean isPrivate(ILaunchConfiguration configuration) {
		return !LaunchConfigurationManager.isVisible(configuration);
	}

	/**
	 * Sets whether step filters should be applied to step commands. This
	 * setting is a global option applied to all registered debug targets.
	 * <p>
	 * Since 3.3, this is equivalent to calling <code>DebugPlugin.setUseStepFilters(boolean)</code>.
	 * </p>
	 * @param useStepFilters whether step filters should be applied to step
	 *  commands
	 * @since 3.0
	 * @see org.eclipse.debug.core.model.IStepFilters
	 */
	public static void setUseStepFilters(boolean useStepFilters) {
		DebugPlugin.setUseStepFilters(useStepFilters);
	}

	/**
	 * Returns whether step filters are applied to step commands.
	 * <p>
	 * Since 3.3, this is equivalent to calling <code>DebugPlugin.isUseStepFilters()</code>.
	 * </p>
	 * @return whether step filters are applied to step commands
	 * @since 3.0
	 * @see org.eclipse.debug.core.model.IStepFilters
	 */
	public static boolean isUseStepFilters() {
		return DebugPlugin.isUseStepFilters();
	}

	/**
	 * Returns the console associated with the given process, or
	 * <code>null</code> if none.
	 *
	 * @param process a process
	 * @return console associated with the given process, or
	 * <code>null</code> if none
	 * @since 3.0
	 */
	public static IConsole getConsole(IProcess process) {
		return DebugUIPlugin.getDefault().getProcessConsoleManager().getConsole(process);
	}

	/**
	 * Returns the console associated with the given debug element, or
	 * <code>null</code> if none.
	 *
	 * @param element a debug model element
	 * @return console associated with the given element, or
	 * <code>null</code> if none
	 * @since 3.0
	 */
	public static IConsole getConsole(IDebugElement element) {
		IProcess process = element.getDebugTarget().getProcess();
		if (process != null) {
			return getConsole(process);
		}
		return null;
	}

	/**
	 * Returns all registered launch group extensions.
	 *
	 * @return all registered launch group extensions
	 * @since 3.0
	 */
	public static ILaunchGroup[] getLaunchGroups() {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroups();
	}

	/**
	 * Returns the last configuration that was launched for specified launch group or
	 * <code>null</code>, if there is not one. This method does not provide any form of
	 * filtering on the returned launch configurations.
	 *
	 * @param groupId the unique identifier of a launch group
	 * @return the last launched configuration for the specified group or <code>null</code>.
	 * @see DebugUITools#getLaunchGroups()
	 * @since 3.3
	 */
	public static ILaunchConfiguration getLastLaunch(String groupId) {
		return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLastLaunch(groupId);
	}

	/**
	 * Returns the launch group that the given launch configuration belongs to, for the specified
	 * mode, or <code>null</code> if none.
	 *
	 * @param configuration the launch configuration
	 * @param mode the mode
	 * @return the launch group the given launch configuration belongs to, for the specified mode,
	 *         or <code>null</code> if none
	 * @since 3.0
	 */
	public static ILaunchGroup getLaunchGroup(ILaunchConfiguration configuration, String mode) {
		try {
			return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(configuration.getType(), mode);
		}
		catch(CoreException ce) {
			return null;
		}
	}

	/**
	 * Performs source lookup on the given artifact and returns the result.
	 * Optionally, a source locator may be specified.
	 *
	 * @param artifact object for which source is to be resolved
	 * @param locator the source locator to use, or <code>null</code>. When <code>null</code>
	 *   a source locator is determined from the artifact, if possible. If the artifact
	 *   is a debug element, the source locator from its associated launch is used.
	 * @return a source lookup result
	 * @since 3.1
	 */
	public static ISourceLookupResult lookupSource(Object artifact, ISourceLocator locator) {
		return SourceLookupFacility.getDefault().lookup(artifact, locator, false);
	}

	/**
	 * Displays the given source lookup result in an editor in the given workbench
	 * page. Has no effect if the result has an unknown editor id or editor input.
	 * The editor is opened, positioned, and annotated.
	 * <p>
	 * Honors user preference for editors re-use.
	 * </p>
	 * @param result source lookup result to display
	 * @param page the page to display the result in
	 * @since 3.1
	 */
	public static void displaySource(ISourceLookupResult result, IWorkbenchPage page) {
		SourceLookupFacility.getDefault().display(result, page);
	}

	/**
	 * Returns the memory rendering manager.
	 *
	 * @return the memory rendering manager
	 * @since 3.1
	 */
	public static IMemoryRenderingManager getMemoryRenderingManager() {
		return MemoryRenderingManager.getDefault();
	}

	/**
	 * Returns the image associated with the specified type of source container
	 * or <code>null</code> if none.
	 *
	 * @param id unique identifier for a source container type
	 * @return image associated with the specified type of source container
	 *    or <code>null</code> if none
	 * @since 3.2
	 * @see org.eclipse.debug.core.sourcelookup.ISourceContainerType
	 */
	public static Image getSourceContainerImage(String id){
		return SourceLookupUIUtils.getSourceContainerImage(id);
	}

	/**
	 * Returns a new source container browser for the specified type of source container
	 * or <code>null</code> if a browser has not been registered.
	 *
	 * @param id unique identifier for a source container type
	 * @return source container browser or <code>null</code> if none
	 * @since 3.2
	 * @see org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser
	 */
	public static ISourceContainerBrowser getSourceContainerBrowser(String id) {
		return SourceLookupUIUtils.getSourceContainerBrowser(id);
	}

	/**
	 * Returns the color associated with the specified preference identifier or
	 * <code>null</code> if none.
	 *
	 * @param id preference identifier of the color
	 * @return the color associated with the specified preference identifier
	 * 	or <code>null</code> if none
	 * @since 3.2
	 * @see IDebugUIConstants
	 */
	public static Color getPreferenceColor(String id) {
		return DebugUIPlugin.getPreferenceColor(id);
	}

	/**
	 * Returns the debug context manager.
	 *
	 * @return debug context manager
	 * @since 3.3
	 */
	public static IDebugContextManager getDebugContextManager() {
		return DebugContextManager.getDefault();
	}

	/**
	 * Return the debug context for the given executionEvent or <code>null</code> if none.
	 *
	 * @param event The execution event that contains the application context
	 * @return the current debug context, or <code>null</code>.
	 *
	 * @since 3.5
	 */
	public static ISelection getDebugContextForEvent(ExecutionEvent event) {
		Object o = HandlerUtil.getVariable(event, IConfigurationElementConstants.DEBUG_CONTEXT);
		if (o instanceof ISelection) {
			return (ISelection) o;
		}
		return null;
	}

	/**
	 * Return the debug context for the given executionEvent.
	 *
	 * @param event The execution event that contains the application context
	 * @return the debug context. Will not return <code>null</code>.
	 * @throws ExecutionException If the current selection variable is not found.
	 *
	 * @since 3.5
	 */
	public static ISelection getDebugContextForEventChecked(ExecutionEvent event)
			throws ExecutionException {
		Object o = HandlerUtil.getVariableChecked(event, IConfigurationElementConstants.DEBUG_CONTEXT);
		if (!(o instanceof ISelection)) {
			throw new ExecutionException("Incorrect type for " //$NON-NLS-1$
				+ IConfigurationElementConstants.DEBUG_CONTEXT
				+ " found while executing " //$NON-NLS-1$
				+ event.getCommand().getId()
				+ ", expected " + ISelection.class.getName() //$NON-NLS-1$
				+ " found " + o.getClass().getName()); //$NON-NLS-1$
		}
		return (ISelection) o;
	}

	/**
	 * Returns the global instance of toggle breakpoints target manager.
	 *
	 * @return toggle breakpoints target manager
	 *
	 * @since 3.8
	 */
	public static IToggleBreakpointsTargetManager getToggleBreakpointsTargetManager() {
		return ToggleBreakpointsTargetManager.getDefault();
	}

	/**
	 * Returns the ILaunchConfiguration corresponding to
	 * ILaunchConfigurationDialog
	 *
	 * @param dialog The input launch configuration dialog
	 * @return {@link ILaunchConfiguration} Corresponding launch configuration
	 * @since 3.13
	 */
	public static ILaunchConfiguration getLaunchConfiguration(ILaunchConfigurationDialog dialog) {
		if (dialog instanceof LaunchConfigurationsDialog) {
			LaunchConfigurationTabGroupViewer tabViewer = ((LaunchConfigurationsDialog) dialog).getTabViewer();
			if (tabViewer != null) {
				Object input = tabViewer.getInput();
				if (input instanceof ILaunchConfiguration) {
					ILaunchConfiguration configuration = (ILaunchConfiguration) input;
					return configuration;
				}
			}
		}
		return null;
	}

}
