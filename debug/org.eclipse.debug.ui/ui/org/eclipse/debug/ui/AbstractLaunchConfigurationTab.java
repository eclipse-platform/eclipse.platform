/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Mohamed Hussein (Mentor Graphics) - Added s/getWarningMessage (Bug 386673)
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.ui;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IPrototypeAttributesLabelProvider;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Common function for launch configuration tabs.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @see ILaunchConfigurationTab
 * @since 2.0
 */
public abstract class AbstractLaunchConfigurationTab implements ILaunchConfigurationTab2, IPrototypeAttributesLabelProvider {

	/**
	 * The control for this page, or <code>null</code>
	 */
	private Control fControl;

	/**
	 * The launch configuration dialog this tab is
	 * contained in.
	 */
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;

	/**
	 * Current error message, or <code>null</code>
	 */
	private String fErrorMessage;

	/**
	 * Current warning message, or <code>null</code>
	 */
	private String fWarningMessage;

	/**
	 * Current message, or <code>null</code>
	 */
	private String fMessage;

	/**
	 * Whether this tab needs to apply changes. This attribute is initialized to
	 * <code>true</code> to be backwards compatible. If clients want to take advantage
	 * of such a feature, they should set the flag to false, and check it before
	 * applying changes to the launch configuration working copy.
	 *
	 * @since 2.1
	 */
	private boolean fDirty = true;

	/**
	 * Job to update the tab after a delay. Used to delay updates while
	 * the user is typing.
	 */
	private Job fRereshJob;

	/**
	 * The set help context id
	 *
	 * @since 3.7
	 */
	private String fHelpContextId = null;

	/**
	 * Attributes labels for prototype tab.
	 *
	 * @since 3.13
	 */
	private Map<String, String> fAttributesLabelsForPrototype;

	/**
	 * Default constructor.
	 *
	 * @since 3.13
	 */
	public AbstractLaunchConfigurationTab() {
		fAttributesLabelsForPrototype = new HashMap<>();
		initializeAttributes();
	}

	/**
	 * Initialize attributes labels.
	 *
	 * @since 3.13
	 */
	protected void initializeAttributes() {
	}

	@Override
	public String getAttributeLabel(String attribute) {
		String label = fAttributesLabelsForPrototype.get(attribute);
		if (label != null) {
			return label;
		}
		return null;
	}

	/**
	 * Returns the dialog this tab is contained in, or <code>null</code> if not
	 * yet set.
	 *
	 * @return launch configuration dialog, or <code>null</code>
	 */
	protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
		return fLaunchConfigurationDialog;
	}

	/**
	 * Updates the buttons and message in this page's launch
	 * configuration dialog.
	 */
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			//order is important here due to the call to
			//refresh the tab viewer in updateButtons()
			//which ensures that the messages are up to date
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
		}
	}

	/**
	 * @see ILaunchConfigurationTab#getControl()
	 */
	@Override
	public Control getControl() {
		return fControl;
	}

	/**
	 * Sets the control to be displayed in this tab.
	 *
	 * @param control the control for this tab
	 */
	protected void setControl(Control control) {
		fControl = control;
	}

	/**
	 * @see ILaunchConfigurationTab#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/**
	 * @see ILaunchConfigurationTab2#getWarningMessage()
	 * @since 3.9
	 */
	@Override
	public String getWarningMessage() {
		return fWarningMessage;
	}

	/**
	 * @see ILaunchConfigurationTab#getMessage()
	 */
	@Override
	public String getMessage() {
		return fMessage;
	}

	/**
	 * By default, do nothing.
	 *
	 * @see ILaunchConfigurationTab#launched(ILaunch)
	 * @deprecated As of R3.0, this method is no longer called by the launch
	 *  framework. Since tabs do not exist when launching is performed elsewhere
	 *  than the launch dialog, this method cannot be relied upon for launching
	 *  functionality.
	 */
	@Deprecated
	@Override
	public void launched(ILaunch launch) {
	}

	/**
	 * @see ILaunchConfigurationTab#setLaunchConfigurationDialog(ILaunchConfigurationDialog)
	 */
	@Override
	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		fLaunchConfigurationDialog = dialog;
	}

	/**
	 * Sets this page's error message, possibly <code>null</code>.
	 *
	 * @param errorMessage the error message or <code>null</code>
	 */
	protected void setErrorMessage(String errorMessage) {
		fErrorMessage = errorMessage;
	}

	/**
	 * Sets this page's warning message, possibly <code>null</code>.
	 *
	 * @param warningMessage the warning message or <code>null</code>
	 * @since 3.9
	 */
	protected void setWarningMessage(String warningMessage) {
		fWarningMessage = warningMessage;
	}

	/**
	 * Sets this page's message, possibly <code>null</code>.
	 *
	 * @param message the message or <code>null</code>
	 */
	protected void setMessage(String message) {
		fMessage = message;
	}

	/**
	 * Convenience method to return the launch manager.
	 *
	 * @return the launch manager
	 */
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * By default, do nothing.
	 *
	 * @see ILaunchConfigurationTab#dispose()
	 */
	@Override
	public void dispose() {
	}

	/**
	 * Returns the shell this tab is contained in, or <code>null</code>.
	 *
	 * @return the shell this tab is contained in, or <code>null</code>
	 */
	protected Shell getShell() {
		Control control = getControl();
		if (control != null) {
			return control.getShell();
		}
		return null;
	}

	/**
	 * Creates and returns a new push button with the given
	 * label and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 * @param image image of <code>null</code>
	 *
	 * @return a new push button
	 */
	protected Button createPushButton(Composite parent, String label, Image image) {
		return SWTFactory.createPushButton(parent, label, image);
	}

	/**
	 * Creates and returns a new radio button with the given
	 * label and/or image.
	 *
	 * @param parent parent control
	 * @param label button label or <code>null</code>
	 *
	 * @return a new radio button
	 */
	protected Button createRadioButton(Composite parent, String label) {
		return SWTFactory.createRadioButton(parent, label);
	}

	/**
	 * Creates and returns a new check button with the given
	 * label.
	 *
	 * @param parent the parent composite
	 * @param label the button label
	 * @return a new check button
	 * @since 3.0
	 */
	protected Button createCheckButton(Composite parent, String label) {
		return SWTFactory.createCheckButton(parent, label, null, false, 1);
	}

	/**
	 * @see ILaunchConfigurationTab#canSave()
	 */
	@Override
	public boolean canSave() {
		return true;
	}

	/**
	 * @see ILaunchConfigurationTab#isValid(ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return true;
	}

	/**
	 * Creates vertical space in the parent <code>Composite</code>
	 * @param comp the parent to add the vertical space to
	 * @param colSpan the number of line of vertical space to add
	 */
	protected void createVerticalSpacer(Composite comp, int colSpan) {
		SWTFactory.createVerticalSpacer(comp, colSpan);
	}

	/**
	 * Create a horizontal separator.
	 *
	 * @param comp parent widget
	 * @param colSpan number of columns to span
	 * @since 3.0
	 */
	protected void createSeparator(Composite comp, int colSpan) {
		Label label = new Label(comp, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		label.setLayoutData(gd);
	}

	/**
	 * @see ILaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return null;
	}

	/**
	 * Returns this tab's unique identifier or <code>null</code> if none.
	 * By default, <code>null</code> is returned. Subclasses should override
	 * as necessary.
	 * <p>
	 * Tab identifiers allow contributed tabs to be ordered relative to one
	 * another.
	 * </p>
	 * @return tab id or <code>null</code>
	 * @since 3.3
	 */
	public String getId() {
		return null;
	}

	/**
	 * Convenience method to set a boolean attribute of on a launch
	 * configuration. If the value being set is the default, the attribute's
	 * value is set to <code>null</code>.
	 *
	 * @param attribute attribute identifier
	 * @param configuration the configuration on which to set the attribute
	 * @param value the value of the attribute
	 * @param defaultValue the default value of the attribute
	 * @since 2.1
	 */
	protected void setAttribute(String attribute, ILaunchConfigurationWorkingCopy configuration, boolean value, boolean defaultValue) {
		if (value == defaultValue) {
			configuration.setAttribute(attribute, (String)null);
		} else {
			configuration.setAttribute(attribute, value);
		}
	}



	/**
	 * Returns if this tab has pending changes that need to be saved.
	 *
	 * It is up to clients to set/reset and consult this attribute as required.
	 * By default, a tab is initialized to dirty for backwards compatibility.
	 *
	 * @return whether this tab is dirty
	 * @since 2.1
	 */
	protected boolean isDirty() {
		return fDirty;
	}

	/**
	 * Sets the dirty state of the tab. Setting this flag allows clients to
	 * explicitly say whether this tab has pending changes or not.
	 *
	 * It is up to clients to set/reset and consult this attribute as required.
	 * By default, a tab is initialized to dirty for backwards compatibility.
	 *
	 * @param dirty what to set the dirty flag to
	 * @since 2.1
	 */
	protected void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	/**
	 * This method was added to the <code>ILaunchConfigurationTab</code> interface
	 * in the 3.0 release to allow tabs to distinguish between a tab being activated
	 * and a tab group be initialized for the first time, from a selected launch
	 * configuration. To maintain backwards compatible behavior, the default
	 * implementation provided, calls this tab's <code>initializeFrom</code> method.
	 * Tabs should override this method as required.
	 * <p>
	 * The launch tab framework was originally designed to take care of inter tab
	 * communication by applying attributes from the active tab to the launch configuration
	 * being edited, when a tab is exited, and by initializing a tab when activated.
	 * The addition of the methods <code>activated</code> and <code>deactivated</code>
	 * allow tabs to determine the appropriate course of action.
	 * </p>
	 *
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 * @since 3.0
	 */
	@Override
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		initializeFrom(workingCopy);
	}

	/**
	 * This method was added to the <code>ILaunchConfigurationTab</code> interface
	 * in the 3.0 release to allow tabs to distinguish between a tab being deactivated
	 * and saving its attributes to a launch configuration. To maintain backwards
	 * compatible behavior, the default implementation provided, calls this tab's
	 * <code>performApply</code> method. Tabs should override this method as required.
	 * <p>
	 * The launch tab framework was originally designed to take care of inter tab
	 * communication by applying attributes from the active tab to the launch configuration
	 * being edited, when a tab is exited, and by initializing a tab when activated.
	 * The addition of the methods <code>activated</code> and <code>deactivated</code>
	 * allow tabs to determine the appropriate course of action.
	 * </p>
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 * @since 3.0
	 */
	@Override
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		performApply(workingCopy);
	}

	/**
	 * Returns the job to update the launch configuration dialog.
	 *
	 * @return update job
	 */
	private Job getUpdateJob() {
		if (fRereshJob == null) {
			fRereshJob = createUpdateJob();
			fRereshJob.setSystem(true);
		}
		return fRereshJob;
	}

	/**
	 * Schedules the update job to run for this tab based on this tab's delay.
	 *
	 * @since 3.6
	 */
	protected void scheduleUpdateJob() {
		Job job = getUpdateJob();
		job.cancel(); // cancel existing job
		job.schedule(getUpdateJobDelay());
	}

	/**
	 * Creates and returns a job used to update the launch configuration dialog
	 * for this tab. Subclasses may override.
	 *
	 * @return job to update the launch dialog for this tab
	 * @since 3.6
	 */
	protected Job createUpdateJob() {
		return  new WorkbenchJob(getControl().getDisplay(), "Update LCD") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (!getControl().isDisposed()) {
					updateLaunchConfigurationDialog();
				}
				return Status.OK_STATUS;
			}
			@Override
			public boolean shouldRun() {
				return !getControl().isDisposed();
			}
		};
	}

	/**
	 * Return the time delay that should be used when scheduling the
	 * update job. Subclasses may override.
	 *
	 * @return a time delay in milliseconds before the job should run
	 * @since 3.6
	 */
	protected long getUpdateJobDelay() {
		return 200;
	}

	/**
	 * Sets the help context id for this tab.
	 * <p>
	 * Not all tabs honor this setting, but if this method is called prior
	 * to {@link #createControl(Composite)}, a tab implementation may use this
	 * to set the context help associated with this tab.
	 * </p>
	 * @param id help context id
	 * @since 3.7
	 */
	public void setHelpContextId(String id) {
		fHelpContextId = id;
	}

	/**
	 * Returns the help context id for this tab or <code>null</code>.
	 *
	 * @return the help context for this tab or <code>null</code> if unknown.
	 * @since 3.7
	 */
	public String getHelpContextId() {
		return fHelpContextId;
	}

	/**
	 * Get the attributes labels for prototype tab.
	 *
	 * @return the attributes labels for prototype tab.
	 * @since 3.13
	 */
	protected Map<String, String> getAttributesLabelsForPrototype() {
		return fAttributesLabelsForPrototype;
	}
}

