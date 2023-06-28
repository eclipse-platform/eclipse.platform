/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 * Martin Oberhuber (Wind River) - [325557] Perspective "none" is not persisted
 *******************************************************************************/
package org.eclipse.debug.internal.ui.launchConfigurations;


import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.viewers.AsynchronousSchedulingRuleFactory;
import org.eclipse.debug.internal.ui.views.ViewContextManager;
import org.eclipse.debug.internal.ui.views.ViewContextService;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.ISuspendTrigger;
import org.eclipse.debug.ui.contexts.ISuspendTriggerListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The perspective manager manages the 'perspective' settings
 * defined by launch configurations. Specifically it:
 * <ul>
 * <li>changes perspectives as launches are registered</li>
 * <li>change perspective when a thread suspends</li>
 * </ul>
 *
 * Since 3.3 the Perspectives Manager is an <code>ISaveParticipant</code>, allowing it to participate in
 * workspace persistence life-cycles.
 *
 * @see IDebugUIContants.ATTR_RUN_PERSPECTIVE
 * @see IDebugUIContants.ATTR_DEBUG_PERSPECTIVE
 * @see ISaveParticipant
 * @see ISuspendTriggerListener
 * @see ILaunchListener
 * @see org.eclipse.debug.internal.ui.preferences.LaunchPerspectivePreferencePage
 * @see DebugUIPlugin
 */
public class PerspectiveManager implements ILaunchListener, ISuspendTriggerListener, ISaveParticipant {

	/**
	 * Describes exactly one perspective context, which is composed of an <code>ILaunchCOnfigurationType</code>, and set of modes
	 * and an <code>ILaunchDelegate</code>. Perspective ids are then cached for a context based on mode set.
	 *
	 * @since 3.3
	 */
	class PerspectiveContext {

		private ILaunchConfigurationType fType = null;
		private ILaunchDelegate fDelegate = null;
		private Map<Set<String>, String> fPerspectives = null;

		/**
		 * Constructor
		 * @param type
		 * @param delegate
		 * @param modes
		 */
		public PerspectiveContext(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes) {
			fType = type;
			fDelegate = delegate;
			fPerspectives = new HashMap<>();
			fPerspectives.put(modes, null);
		}

		/**
		 * We can specially compare two cases:
		 * <ol>
		 * <li>a launch object</li>
		 * <li>an object array of the form [IlaunchConfigurationType, ILaunchDelegate, Set]</li>
		 * </ol>
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object object) {
			ILaunchDelegate delegate = null;
			ILaunchConfigurationType type = null;
			if(object instanceof ILaunch) {
				try {
					ILaunch launch = (ILaunch) object;
					type = launch.getLaunchConfiguration().getType();
					delegate = resolveLaunchDelegate(launch);
				}
				catch (CoreException e) {return false;}
			}
			else if(object instanceof PerspectiveContext) {
				PerspectiveContext context = (PerspectiveContext) object;
				type = context.getLaunchConfigurationType();
				delegate = context.getLaunchDelegate();
			}
			if(fType != null && type != null && fType.getIdentifier().equals(type.getIdentifier())) {
				if(fDelegate == null) {
					return delegate == null;
				}
				else {
					return fDelegate.equals(delegate);
				}
			}
			return super.equals(object);
		}

		@Override
		public int hashCode() {
			if (fType != null) {
				int hash = fType.getIdentifier().hashCode();
				if (fDelegate != null) {
					hash += fDelegate.hashCode();
				}
				return hash;
			}
			return super.hashCode();
		}

		public ILaunchConfigurationType getLaunchConfigurationType() {return fType;}
		public ILaunchDelegate getLaunchDelegate() {return fDelegate;}

		public Map<Set<String>, String> getPersepctiveMap() {
			return fPerspectives;
		}

		/**
		 * Creates a new mapping of the specified perspective id to the specified mode set.
		 * If a mapping for the modeset already exists it is over-written.
		 * @param modes the set of modes
		 * @param pid the id of the perspective
		 */
		public void setPerspective(Set<String> modes, String pid) {
			if(fPerspectives == null) {
				fPerspectives = new HashMap<>();
			}
			fPerspectives.put(modes, pid);
		}

		/**
		 * Returns the perspective id associated with the given mode set
		 * @param modes the set of mode
		 * @return the perspective id associated with the given mode set, or
		 * <code>null</code>, if there isn't one
		 */
		public String getPerspective(Set<String> modes) {
			if(fPerspectives != null) {
				return fPerspectives.get(modes);
			}
			return null;
		}
	}

	/**
	 * Use a customized UI job so that nested jobs with a scheduling rule are
	 * not prevented from running.
	 * See bug 377593
	 */
	private abstract class MyUIJob extends Job {
		public MyUIJob(String name) {
			super(name);
			setSystem(true);
			setPriority(Job.INTERACTIVE);
			setRule(AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(this));
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			Display asyncDisplay = DebugUIPlugin.getStandardDisplay();
			if (asyncDisplay == null || asyncDisplay.isDisposed()) {
				return Status.CANCEL_STATUS;
			}
			asyncDisplay.asyncExec(() -> {
				IStatus result = null;
				Throwable throwable = null;
				try {
					if (monitor.isCanceled()) {
						result = Status.CANCEL_STATUS;
					} else {
						result = runInUIThread(monitor);
					}

				} catch (Throwable t) {
					throwable = t;
				} finally {
					if (result == null) {
						result = new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.ERROR,
								LaunchConfigurationsMessages.PerspectiveManager_Error_1, throwable);
					}
					done(result);
				}
			});
			return Job.ASYNC_FINISH;
		}

		public abstract IStatus runInUIThread(IProgressMonitor monitor);

	}

	/**
	 * A listing of <code>PerspectiveContext</code>s
	 *
	 * @since 3.3
	 */
	private Set<PerspectiveContext> fPerspectiveContexts = null;

	/**
	 * id for the 'delegate' attribute
	 *
	 * @since 3.3
	 */
	public static final String ATTR_DELEGATE_ID = "delegate"; //$NON-NLS-1$

	/**
	 * Flag used to indicate that the user is already being prompted to
	 * switch perspectives. This flag allows us to not open multiple
	 * prompts at the same time.
	 */
	private boolean fPrompting;

	/**
	 * Maps each launch to its perspective context activation. These
	 * are disabled when a launch terminates.
	 */
	private Map<ILaunch, IContextActivation[]> fLaunchToContextActivations = new HashMap<>();

	/**
	 * Called by the debug ui plug-in on startup.
	 * The perspective manager starts listening for
	 * launches to be registered.
	 */
	public void startup() {
		initPerspectives();
		DebugUIPlugin.getDefault().addSaveParticipant(this);
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	/**
	 * Called by the debug ui plug-in on shutdown.
	 * The perspective manager de-registers as a
	 * launch listener.
	 */
	public void shutdown() {
		DebugUIPlugin.getDefault().removeSaveParticipant(this);
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	/**
	 * If there are no launches, remove the Suspend Trigger Listener
	 *
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	@Override
	public synchronized void launchRemoved(final ILaunch launch) {
		ISuspendTrigger trigger = launch.getAdapter(ISuspendTrigger.class);
		if (trigger != null) {
			trigger.removeSuspendTriggerListener(this);
		}
		Runnable r = () -> {
			IContextActivation[] activations = fLaunchToContextActivations.remove(launch);
			if (activations != null) {
				for (IContextActivation activation : activations) {
					activation.getContextService().deactivateContext(activation);
				}
			}
		};
		async(r);
	}

	/**
	 * Do nothing.
	 *
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	@Override
	public void launchChanged(ILaunch launch) {}

	/**
	 * Switch to the perspective specified by the
	 * launch configuration.
	 *
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	@Override
	public synchronized void launchAdded(ILaunch launch) {
		ISuspendTrigger trigger = launch.getAdapter(ISuspendTrigger.class);
		if (trigger != null) {
			trigger.addSuspendTriggerListener(this);
		}
		String perspectiveId = null;
		// check event filters
		try {
			perspectiveId = getPerspectiveId(launch);
		}
		catch (CoreException e) {
			String name = DebugUIPlugin.getModelPresentation().getText(launch);
			switchFailed(e, name);
		}
		// don't switch if a private config
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (configuration != null) {
			if (!LaunchConfigurationManager.isVisible(configuration)) {
				perspectiveId = null;
			}
		}
		final String id = perspectiveId;
		// switch
		Job switchJob = new MyUIJob("Perspective Switch Job") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbenchWindow window = getWindowForPerspective(id);
				if (id != null && window != null && shouldSwitchPerspective(window, id, IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE)) {
					switchToPerspective(window, id);
				}
				return Status.OK_STATUS;
			}
		};
		switchJob.setSystem(true);
		switchJob.setPriority(Job.INTERACTIVE);
		//switchJob.setRule(AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(this));
		switchJob.schedule();
	}


	/**
	 * Switches to the specified perspective
	 *
	 * @param id perspective identifier
	 */
	protected void switchToPerspective(IWorkbenchWindow window, String id) {
		try {
			// don't loose the focus dialog if there is one
			Shell dialog = getModalDialogOpen(window.getShell());
			window.getWorkbench().showPerspective(id, window);
			if (dialog != null) {
				dialog.setFocus();
			}
		} catch (WorkbenchException e) {
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(),
			LaunchConfigurationsMessages.PerspectiveManager_Error_1,
 MessageFormat.format(LaunchConfigurationsMessages.PerspectiveManager_Unable_to_switch_to_perspective___0__2, new Object[] { id }),
			e);
		}
	}

	/**
	 * Utility method to submit an asynchronous runnable to the UI
	 */
	protected void async(Runnable r) {
		Display d = DebugUIPlugin.getStandardDisplay();
		if (d != null && !d.isDisposed()) {
			d.asyncExec(r);
		}
	}

	/**
	 * Utility method to submit a synchronous runnable to the UI
	 */
	protected void sync(Runnable r) {
		Display d = DebugUIPlugin.getStandardDisplay();
		if (d != null && !d.isDisposed()) {
			d.syncExec(r);
		}
	}

	/**
	 * Reports failure to switch perspectives to the user
	 *
	 * @param status exception status describing failure
	 * @param launchName the name of the launch that the
	 *  failure is associated with
	 */
	protected void switchFailed(final Throwable t, final String launchName) {
		sync(() -> DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(),
				LaunchConfigurationsMessages.PerspectiveManager_Error_1,
 MessageFormat.format(LaunchConfigurationsMessages.PerspectiveManager_Unable_to_switch_perpsectives_as_specified_by_launch___0__4, new Object[] { launchName }),
				t));
	}

	/**
	 * A breakpoint has been hit. Carry out perspective switching
	 * as appropriate for the given debug event.
	 *
	 * @param event the suspend event
	 */
	private void handleBreakpointHit(final ILaunch launch) {

		String perspectiveId = null;
		try {
			perspectiveId = getPerspectiveId(launch);
		}
		catch (CoreException e) {DebugUIPlugin.log(e);}
		// if no perspective specified, always switch to debug
		// perspective

		// this has to be done in an async, such that the workbench
		// window can be accessed
		final String targetId = perspectiveId;
		Job switchJob = new MyUIJob("Perspective Switch Job") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbenchWindow window = null;
				if (targetId != null) {
					// get the window to open the perspective in
					window = getWindowForPerspective(targetId);
					if (window == null) {
						return Status.OK_STATUS;
					}

					// switch the perspective if user preference is set
					if (shouldSwitchPerspective(window, targetId, IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND)) {
						switchToPerspective(window, targetId);
						window = getWindowForPerspective(targetId);
						if (window == null) {
							return Status.OK_STATUS;
						}
					}

					// make sure the shell is active
					Shell shell= window.getShell();
					if (shell != null) {
						if (DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH)) {
							Shell dialog = getModalDialogOpen(shell);
							if (shell.getMinimized()) {
								shell.setMinimized(false);
								if (dialog != null) {
									dialog.setFocus();
								}
							}
							// If a model dialog is open on the shell, don't activate it
							if (dialog == null) {
								shell.forceActive();
							}
						}
					}

					// Activate a context for the launch
					Object ca = fLaunchToContextActivations.get(launch);
					if (ca == null) {
						ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
						if (launchConfiguration != null) {
							try {
								String type = launchConfiguration.getType().getIdentifier();
								ViewContextService service = ViewContextManager.getDefault().getService(window);
								if (service != null) {
									IContextService contextServce = PlatformUI.getWorkbench().getAdapter(IContextService.class);
									String[] ids = service.getEnabledPerspectives();
									IContextActivation[] activations = new IContextActivation[ids.length];
									for (int i = 0; i < ids.length; i++) {
										// Include the word '.internal.' so the context is filtered from the key binding pref page (Bug 144019) also see ViewContextService.contextActivated()
										Context context = contextServce.getContext(type + ".internal." + ids[i]); //$NON-NLS-1$
										if (!context.isDefined()) {
											context.define(context.getId(), null, null);
										}
										IContextActivation activation = contextServce.activateContext(context.getId());
										activations[i] = activation;
									}
									fLaunchToContextActivations.put(launch, activations);
								}
							} catch (CoreException e) {
								DebugUIPlugin.log(e);
							}
						}
					}

				}
				if (window != null && DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW)) {
					ViewContextService service = ViewContextManager.getDefault().getService(window);
					service.showViewQuiet(IDebugUIConstants.ID_DEBUG_VIEW);
				}
				return Status.OK_STATUS;
			}
		};

		switchJob.setSystem(true);
		switchJob.setPriority(Job.INTERACTIVE);
		switchJob.setRule(AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(this));
		switchJob.schedule();
	}

	/**
	 * Returns a modal dialog currently open on the given shell or <code>null</code> if none.
	 *
	 * @param shell shell to check
	 * @return a modal dialog currently open on the given shell or <code>null</code> if none
	 */
	private Shell getModalDialogOpen(Shell shell) {
		Shell[] shells = shell.getShells();
		for (Shell dialog : shells) {
			if ((dialog.getStyle() & (SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL)) > 0) {
				return dialog;
			}
		}
		return null;
	}

	/**
	 * Returns the workbench window in which the given perspective
	 * should be shown. First, check the current window to see if it
	 * is already showing the perspective. Then check any other windows.
	 *
	 * @param perspectiveId the perspective identifier
	 * @return which window the given perspective should be shown in
	 *  or <code>null</code> if there are no windows available
	 */
	private IWorkbenchWindow getWindowForPerspective(String perspectiveId) {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (isWindowShowingPerspective(window, perspectiveId)) {
			return window;
		}
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow w : windows) {
			window = w;
			if (isWindowShowingPerspective(window, perspectiveId)) {
				return window;
			}
		}
		window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			return window;
		}
		if (windows.length > 0) {
			return windows[0];
		}
		return null;
	}

	/**
	 * Returns if the specified window is showing the perspective denoted by the specified id
	 * @param window the window to query
	 * @param perspectiveId the perspective to ask about
	 * @return true if the specified window is showing the perspective, false otherwise
	 */
	private boolean isWindowShowingPerspective(IWorkbenchWindow window, String perspectiveId) {
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IPerspectiveDescriptor perspectiveDescriptor = page.getPerspective();
				if (perspectiveDescriptor != null && perspectiveDescriptor.getId().equals(perspectiveId)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns whether or not the user wishes to switch to the specified
	 * perspective when a launch occurs.
	 *
	 * @param perspectiveName the name of the perspective that will be presented
	 *  to the user for confirmation if they've asked to be prompted about
	 *  perspective switching
	 * @param message a message to be presented to the user. This message is expected to
	 *  contain a slot for the perspective name to be inserted ("{0}").
	 * @param preferenceKey the preference key of the perspective switching preference
	 * @return whether or not the user wishes to switch to the specified perspective
	 *  automatically
	 */
	private boolean shouldSwitchPerspective(IWorkbenchWindow window, String perspectiveId, String preferenceKey) {
		if (isCurrentPerspective(window, perspectiveId)) {
			return false;
		}
		String perspectiveName = getPerspectiveLabel(perspectiveId);
		if (perspectiveName == null) {
			return false;
		}
		String perspectiveDesc = getPerspectiveDescription(perspectiveId);
		Object[] args;
		if (perspectiveDesc != null) {
			args = new String[] { perspectiveName , perspectiveDesc };
		}
		else {
			args = new String[] { perspectiveName };
		}
		String switchPerspective = DebugUIPlugin.getDefault().getPreferenceStore().getString(preferenceKey);
		if (MessageDialogWithToggle.ALWAYS.equals(switchPerspective)) {
			return true;
		} else if (MessageDialogWithToggle.NEVER.equals(switchPerspective)) {
			return false;
		}

		Shell shell= window.getShell();
		if (shell == null || fPrompting) {
			return false;
		}
		fPrompting= true;
		// Activate the shell if necessary so the prompt is visible
		Shell modal = getModalDialogOpen(shell);
		if (shell.getMinimized()) {
			shell.setMinimized(false);
			if (modal != null) {
				modal.setFocus();
			}
		}
		if (DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH)) {
			if (modal == null) {
				shell.forceActive();
			}
		}
		String message = IInternalDebugCoreConstants.EMPTY_STRING;
		if(IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND.equals(preferenceKey)) {
			if (getPerspectiveDescription(perspectiveId) != null) {
				message = LaunchConfigurationsMessages.PerspectiveManager_suspend_description;
			}
			else {
				message = LaunchConfigurationsMessages.PerspectiveManager_13;
			}
		}
		else if(IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE.equals(preferenceKey)) {
			if (getPerspectiveDescription(perspectiveId) != null) {
				message = LaunchConfigurationsMessages.PerspectiveManager_launch_description;
			}
			else {
				message = LaunchConfigurationsMessages.PerspectiveManager_15;
			}
		}
		LinkedHashMap<String, Integer> buttonLabelToId = new LinkedHashMap<>();
		buttonLabelToId.put(LaunchConfigurationsMessages.PerspectiveManager_switch, IDialogConstants.YES_ID);
		buttonLabelToId.put(IDialogConstants.NO_LABEL, IDialogConstants.NO_ID);
		MessageDialogWithToggle dialog = MessageDialogWithToggle.open(MessageDialog.QUESTION, shell, LaunchConfigurationsMessages.PerspectiveManager_12, MessageFormat.format(message, args), null, false, DebugUIPlugin.getDefault().getPreferenceStore(), preferenceKey, SWT.NONE, buttonLabelToId);
		boolean answer = (dialog.getReturnCode() == IDialogConstants.YES_ID);
		synchronized (this) {
			fPrompting= false;
			notifyAll();
		}
		if (isCurrentPerspective(window, perspectiveId)) {
			answer = false;
		}
		return answer;
	}

	/**
	 * Returns whether the given perspective identifier matches the
	 * identifier of the current perspective.
	 *
	 * @param perspectiveId the identifier
	 * @return whether the given perspective identifier matches the
	 *  identifier of the current perspective
	 */
	protected boolean isCurrentPerspective(IWorkbenchWindow window, String perspectiveId) {
		boolean isCurrent= false;
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IPerspectiveDescriptor perspectiveDescriptor = page.getPerspective();
				if (perspectiveDescriptor != null) {
					isCurrent= perspectiveId.equals(perspectiveDescriptor.getId());
				}
			}
		}
		return isCurrent;
	}

	/**
	 * Returns the label of the perspective with the given identifier or
	 * <code>null</code> if no such perspective exists.
	 *
	 * @param perspectiveId the identifier
	 * @return the label of the perspective with the given identifier or
	 *  <code>null</code> if no such perspective exists
	 */
	protected String getPerspectiveLabel(String perspectiveId) {
		IPerspectiveDescriptor newPerspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
		if (newPerspective == null) {
			return null;
		}
		return newPerspective.getLabel();
	}


	/**
	 * Returns the label of the perspective with the given identifier or
	 * <code>null</code> if no such perspective exists.
	 *
	 * @param perspectiveId the identifier
	 * @return the label of the perspective with the given identifier or
	 *  <code>null</code> if no such perspective exists
	 */
	protected String getPerspectiveDescription(String perspectiveId) {
		IPerspectiveDescriptor newPerspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
		if (newPerspective == null) {
			return null;
		}
		return newPerspective.getDescription();
	}

	/**
	 * Returns the perspective associated with the
	 * given launch, or <code>null</code> if none.
	 *
	 * @param launch a launch
	 * @return the perspective associated with the launch,
	 * 	or <code>null</code>
	 * @exception CoreException if unable to retrieve a required
	 *  launch configuration attribute
	 */
	protected String getPerspectiveId(ILaunch launch) throws CoreException {
		if (launch == null) {
			return null;
		}
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (config == null) {
			return null;
		}
		Set<String> modes = launch.getLaunchConfiguration().getModes();
		modes.add(launch.getLaunchMode());
		String perspectiveId = getLaunchPerspective(config.getType(), modes, resolveLaunchDelegate(launch));
		if (perspectiveId != null && perspectiveId.equals(IDebugUIConstants.PERSPECTIVE_NONE)) {
			perspectiveId = null;
		}
		return perspectiveId;
	}

	/**
	 * Returns the id of the perspective associated with the given type and set of modes. Passing <code>null</code> for
	 * the launch delegate results in the default perspective id being returned (if there is one).
	 * @param type the type we are launching
	 * @param modes the set of modes the type was launched with
	 * @param delegate the delegate performing the launch for this type and modeset
	 * @return the id of the perspective for the given launch configuration type, modeset and launch delegate
	 *
	 * @since 3.3
	 */
	public String getLaunchPerspective(ILaunchConfigurationType type, Set<String> modes, ILaunchDelegate delegate) {
		String id = null;
		PerspectiveContext context = findContext(new PerspectiveContext(type, delegate, modes));
		if(context == null || (context != null && !context.getPersepctiveMap().containsKey(modes))) {
			//try with a null delegate, denoting the perspective for the type
			context = findContext(new PerspectiveContext(type, null, modes));
			if(context == null || (context != null && !context.getPersepctiveMap().containsKey(modes))) {
				//last resort, try the default perspective
				return getDefaultLaunchPerspective(type, delegate, modes);
			}
		}
		if(context != null) {
			id = context.getPerspective(modes);
		}
		return id;
	}

	/**
	 * Returns the perspective to switch to when a configuration of the given type
	 * is launched in the given mode, or <code>null</code> if no switch should take
	 * place.
	 * <p>
	 * This method is equivalent to calling <code>getLaunchPerspective(ILaunchConfigurationType type, Set modes, ILaunchDelegate delegate)</code>,
	 * with the 'mode' parameter comprising a single element set and passing <code>null</code> as the launch delegate.
	 * </p>
	 * @param type launch configuration type
	 * @param mode launch mode identifier
	 * @return perspective identifier or <code>null</code>
	 * @since 3.0
	 */
	public String getLaunchPerspective(ILaunchConfigurationType type, String mode) {
		HashSet<String> modes = new HashSet<>();
		modes.add(mode);
		return getLaunchPerspective(type, modes, null);
	}

	/**
	 * Sets the perspective to switch to when a configuration of the given type
	 * is launched in the given mode. <code>PERSPECTIVE_NONE</code> indicates no
	 * perspective switch should take place. <code>PERSPECTIVE_DEFAULT</code> indicates
	 * a default perspective switch should take place, as defined by the associated
	 * launch tab group extension.
	 * <p>
	 * Calling this method is equivalent to calling <code>setLaunchPerspective(ILaunchConfigurationType type, Set modes, ILaunchDelegate delegate, String perspectiveid)</code>,
	 * with the parameter 'mode' used in the set modes, and null passed as the delegate
	 * </p>
	 * @param type launch configuration type
	 * @param mode launch mode identifier
	 * @param perspective identifier, <code>PERSPECTIVE_NONE</code>, or
	 *   <code>PERSPECTIVE_DEFAULT</code>
	 * @since 3.0
	 */
	public void setLaunchPerspective(ILaunchConfigurationType type, String mode, String perspective) {
		HashSet<String> modes = new HashSet<>();
		modes.add(mode);
		setLaunchPerspective(type, modes, null, perspective);
	}

	/**
	 * Sets the perspective that should be switched to when a configuration of the given type is launched with the
	 * specified modes set by the given launch delegate.
	 * <p>
	 * Passing <code>null</code> as a launch delegate will set the default perspective switch for that type and modeset, where
	 * <code>PERSPECTIVE_NONE</code> indicates no perspective switch should take place.
	 * </p>
	 * @param type the type to set a perspective context for
	 * @param modes the set of modes
	 * @param delegate the delegate, or <code>null</code> if the default perspective should be used
	 *
	 * @since 3.3
	 */
	public void setLaunchPerspective(ILaunchConfigurationType type, Set<String> modes, ILaunchDelegate delegate, String perspectiveid) {
		PerspectiveContext context = new PerspectiveContext(type, delegate, modes);
		String id = null;
		if(!IDebugUIConstants.PERSPECTIVE_NONE.equals(perspectiveid)) {
			if(IDebugUIConstants.PERSPECTIVE_DEFAULT.equals(perspectiveid)) {
				id = getDefaultLaunchPerspective(type, delegate, modes);
			}
			else {
				id = perspectiveid;
			}
		}
		PerspectiveContext item = findContext(context);
		if(item != null) {
			item.setPerspective(modes, id);
		}
		else {
			context.setPerspective(modes, id);
			item = context;
		}
		fPerspectiveContexts.add(item);
	}

	/**
	 * Searches the listing of perspective contexts to see if the specified one already exists
	 * @param context the context to compare
	 * @return the matching <code>PerspectiveContext</code> or <code>null</code> if none
	 *
	 * @since 3.3
	 */
	private PerspectiveContext findContext(PerspectiveContext context) {
		for (PerspectiveContext c : fPerspectiveContexts) {
			if (context.equals(c)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Generates XML for the user specified perspective settings.
	 *
	 * @return XML
	 * @exception IOException if unable to generate the XML
	 * @exception TransformerException if unable to generate the XML
	 * @exception ParserConfigurationException if unable to generate the XML
	 */
	private String generatePerspectiveXML() throws ParserConfigurationException, CoreException {
		Document doc = DebugUIPlugin.getDocument();
		Element root = doc.createElement(IConfigurationElementConstants.LAUNCH_PERSPECTIVES);
		doc.appendChild(root);
		Map<Set<String>, String> modesets = null;
		Element element = null;
		String id = null;
		ILaunchConfigurationType type = null;
		ILaunchDelegate delegate = null;
		for (PerspectiveContext context : fPerspectiveContexts) {
			modesets = context.getPersepctiveMap();
			type = context.getLaunchConfigurationType();
			delegate = context.getLaunchDelegate();
			for (Set<String> modes : modesets.keySet()) {
				id = context.getPerspective(modes);
				String defaultId = getDefaultLaunchPerspective(type, delegate, modes);
				if(id == null && defaultId != null) {
					//bug 325557: Override of a default perspective
					id = IDebugUIConstants.PERSPECTIVE_NONE;
				}
				if(id != null && !id.equals(defaultId)) {
					element = doc.createElement(IConfigurationElementConstants.LAUNCH_PERSPECTIVE);
					element.setAttribute(IConfigurationElementConstants.MODE, createModesetString(modes));
					if(delegate != null) {
						element.setAttribute(ATTR_DELEGATE_ID, delegate.getId());
					}
					element.setAttribute(IConfigurationElementConstants.CONFIGURATION_TYPES, type.getIdentifier());
					element.setAttribute(IConfigurationElementConstants.PERSPECTIVE, id);
					root.appendChild(element);
				}
			}

		}
		return DebugPlugin.serializeDocument(doc);
	}

	/**
	 * Returns the default perspective to switch to when a configuration of the given type is launched by the specified
	 * launch delegate in the given mode set, or <code>null</code> if none
	 * @param type the type
	 * @param delegate the associated delegate, or <code>null</code> to specify that the default perspective id for that given type and mode set should be returned
	 * @param modes the set of modes this applies to
	 * @return the default perspective id for the given type, delegate and mode set combination, or <code>null</code> if none
	 *
	 * @since 3.3
	 */
	public String getDefaultLaunchPerspective(ILaunchConfigurationType type, ILaunchDelegate delegate, Set<String> modes) {
		String id = null;
		if(delegate != null) {
			id = delegate.getPerspectiveId(modes);
		}
		if(id == null) {
			LaunchConfigurationTabGroupExtension extension = LaunchConfigurationPresentationManager.getDefault().getExtension(type.getIdentifier(), modes);
			if (extension != null) {
				id = extension.getPerspective(modes);
				if (id == null) {
					if (modes.contains(ILaunchManager.DEBUG_MODE)) {
						id = IDebugUIConstants.ID_DEBUG_PERSPECTIVE;
					}
				}
			}
		}
		return id;
	}

	/**
	 * Resolves the <code>ILaunchDelegate</code> from the given <code>ILaunch</code>
	 * @param launch the launch
	 * @return
	 * @throws CoreException
	 */
	private ILaunchDelegate resolveLaunchDelegate(ILaunch launch) throws CoreException {
		Set<String> modes = launch.getLaunchConfiguration().getModes();
		modes.add(launch.getLaunchMode());
		ILaunchConfigurationType type = launch.getLaunchConfiguration().getType();
		ILaunchDelegate[] delegates = LaunchConfigurationManager.filterLaunchDelegates(type, modes);
		ILaunchDelegate delegate = null;
		if(delegates.length == 1) {
			delegate = delegates[0];
		}
		else if(delegates.length > 1) {
			delegate = launch.getLaunchConfiguration().getPreferredDelegate(modes);
			if(delegate == null) {
				delegate = type.getPreferredDelegate(modes);
			}
		}
		return delegate;
	}

	/**
	 * Initialize the preference set with settings from user preferences
	 */
	private void initPerspectives() {
		if(fPerspectiveContexts == null) {
			fPerspectiveContexts = new HashSet<>();
			String xml = DebugUIPlugin.getDefault().getPreferenceStore().getString(IInternalDebugUIConstants.PREF_LAUNCH_PERSPECTIVES);
			if (xml != null && xml.length() > 0) {
				try {
					Element root = DebugPlugin.parseDocument(xml);
					NodeList list = root.getChildNodes();
					LaunchManager lm = (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
					ILaunchConfigurationType lctype = null;
					ILaunchDelegate ldelegate = null;
					Set<String> modes = null;
					Node node = null;
					Element element = null;
					for (int i = 0; i < list.getLength(); ++i) {
						node = list.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							element = (Element) node;
							String nodeName = element.getNodeName();
							if (nodeName.equalsIgnoreCase(IConfigurationElementConstants.LAUNCH_PERSPECTIVE)) {
								String type = element.getAttribute(IConfigurationElementConstants.CONFIGURATION_TYPES);
								String mode = element.getAttribute(IConfigurationElementConstants.MODE);
								String perspective = element.getAttribute(IConfigurationElementConstants.PERSPECTIVE);
								String delegate = element.getAttribute(ATTR_DELEGATE_ID);
								lctype = lm.getLaunchConfigurationType(type);
								ldelegate = lm.getLaunchDelegate(delegate);
								modes = parseModes(mode);
								if(lctype != null && !modes.isEmpty() && !IInternalDebugCoreConstants.EMPTY_STRING.equals(perspective)) {
									setLaunchPerspective(lctype, modes, ldelegate, perspective);
								}
							}
						}
					}
				}
				catch (CoreException e) {DebugUIPlugin.log(e);}
			}
		}
	}

	/**
	 * Parses a string argument into a set of modes
	 * @param modes the string to parse
	 * @return a set of modes parsed from the specified string of the empty set, never null
	 *
	 * @since 3.3
	 */
	private Set<String> parseModes(String modes) {
		HashSet<String> modeset = new HashSet<>();
		String[] ms = modes.split(","); //$NON-NLS-1$
		for (String m : ms) {
			modeset.add(m.trim());
		}
		return modeset;
	}

	/**
	 * Creates a standard comma separated list of the modes from the specified
	 * set
	 *
	 * @param modes the set to write to string
	 * @return the mode set as a string
	 */
	private String createModesetString(Set<String> modes) {
		String str = IInternalDebugCoreConstants.EMPTY_STRING;
		if(modes != null) {
			for (Iterator<String> iter = modes.iterator(); iter.hasNext();) {
				str += iter.next();
				if(iter.hasNext()) {
					str += ","; //$NON-NLS-1$
				}
			}
		}
		return str;
	}

	/**
	 * Schedules the given job after perspective switching is complete, or
	 * immediately if a perspective switch is not in progress.
	 *
	 * @param job job to run after perspective switching
	 */
	public void schedulePostSwitch(Job job) {
		job.setRule(AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(this));
		job.schedule();
	}

	@Override
	public void suspended(ILaunch launch, Object context) {
		handleBreakpointHit(launch);
	}

	@Override
	public void doneSaving(ISaveContext context) {}

	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {}

	@Override
	public void rollback(ISaveContext context) {}

	@Override
	public void saving(ISaveContext context) throws CoreException {
		try {
			DebugUIPlugin.getDefault().getPreferenceStore().putValue(IInternalDebugUIConstants.PREF_LAUNCH_PERSPECTIVES, generatePerspectiveXML());
		}   catch (ParserConfigurationException e) {
			throw new CoreException(DebugUIPlugin.newErrorStatus("Exception occurred while generating launch perspectives preference XML", e)); //$NON-NLS-1$
		}
	}
}
