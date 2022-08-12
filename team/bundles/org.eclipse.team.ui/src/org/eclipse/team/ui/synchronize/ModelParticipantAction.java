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
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.ITwoWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

/**
 * Model provider actions for use with a {@link ModelSynchronizeParticipant}.
 *
 * @since 3.2
 */
public abstract class ModelParticipantAction extends BaseSelectionListenerAction {

	private final ISynchronizePageConfiguration configuration;

	/**
	 * Create the model participant action.
	 * @param text the label of the action or <code>null</code>
	 * @param configuration the configuration for the page that is surfacing the action
	 */
	public ModelParticipantAction(String text, ISynchronizePageConfiguration configuration) {
		super(text);
		this.configuration = configuration;
		initialize(configuration);
	}

	private void initialize(ISynchronizePageConfiguration configuration) {
		configuration.getSite().getSelectionProvider().addSelectionChangedListener(this);
		configuration.getPage().getViewer().getControl().addDisposeListener(e -> getConfiguration().getSite().getSelectionProvider().removeSelectionChangedListener(ModelParticipantAction.this));
	}

	/**
	 * Return the page configuration.
	 * @return the page configuration
	 */
	protected ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Set the selection of this action to the given selection
	 *
	 * @param selection the selection
	 */
	public void selectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			super.selectionChanged((IStructuredSelection)selection);
		} else {
			super.selectionChanged(StructuredSelection.EMPTY);
		}

	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		return isEnabledForSelection(selection);
	}

	/**
	 * Return whether the action is enabled for the given selection
	 * @param selection the selection
	 * @return whether the action is enabled for the given selection
	 */
	protected abstract boolean isEnabledForSelection(IStructuredSelection selection);

	/**
	 * Return the synchronization context associated with this action.
	 * @return the synchronization context associated with this action
	 */
	protected ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	/**
	 * Return whether the given node is visible in the page based
	 * on the mode in the configuration.
	 * @param node a diff node
	 * @return whether the given node is visible in the page
	 */
	protected boolean isVisible(IDiff node) {
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration.getComparisonType() == ISynchronizePageConfiguration.THREE_WAY
				&& node instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) node;
			int mode = configuration.getMode();
			switch (mode) {
			case ISynchronizePageConfiguration.INCOMING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.OUTGOING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				if (twd.getDirection() == IThreeWayDiff.CONFLICTING) {
					return true;
				}
				break;
			case ISynchronizePageConfiguration.BOTH_MODE:
				return true;
			}
		} else if (configuration.getComparisonType() == ISynchronizePageConfiguration.TWO_WAY
				&& node instanceof ITwoWayDiff) {
			return true;
		}
		return false;
	}

	/**
	 * Check to see if the target saveable differs from the currently
	 * active saveable. If it does, prompt to save changes in the
	 * active saveable if it is dirty.
	 * @throws InterruptedException if operation is interrupted
	 * @throws InvocationTargetException if an error occurs
	 */
	protected void handleTargetSaveableChange() throws InvocationTargetException, InterruptedException {
		final SaveableComparison targetSaveable = getTargetSaveable();
		final SaveableComparison  activeSaveable = getActiveSaveable();
		if (activeSaveable != null && activeSaveable.isDirty()) {
			PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
				try {
					handleTargetSaveableChange(configuration.getSite().getShell(), targetSaveable, activeSaveable, true,
							monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		}
		setActiveSaveable(targetSaveable);
	}

	/**
	 * Convenience method that prompts if the currently active saveable is dirty
	 * and either saves or reverts the saveable depending on the users input.
	 * @param shell a parent shell
	 * @param targetSaveable the new saveable
	 * @param activeSaveable the current saveable
	 * @param allowCancel whether canceling the action is an option
	 * @param monitor a progress monitor
	 * @throws CoreException if an error occurs
	 * @throws InterruptedException if operation is interrupted
	 */
	public static void handleTargetSaveableChange(Shell shell, SaveableComparison targetSaveable, SaveableComparison activeSaveable, boolean allowCancel, IProgressMonitor monitor) throws CoreException, InterruptedException {
		if (activeSaveable != null && targetSaveable != activeSaveable) {
			if (activeSaveable.isDirty()) {
				if (promptToSaveChanges(shell, activeSaveable, allowCancel)) {
					activeSaveable.doSave(monitor);
				} else {
					activeSaveable.doRevert(monitor);
				}
			}
		}
	}

	/**
	 * Convenience method that prompts to save changes in the given dirty model.
	 * @param shell a shell
	 * @param saveable a dirty saveable model
	 * @param allowCancel whether canceling the action is an option
	 * @return whether the user choose to save (<code>true</code>) or revert (<code>false</code>() the model
	 * @throws InterruptedException thrown if the user choose to cancel
	 */
	public static boolean promptToSaveChanges(final Shell shell, final SaveableComparison saveable, final boolean allowCancel) throws InterruptedException {
		final int[] result = new int[] { 0 };
		Runnable runnable = () -> {
			String[] options;
			if (allowCancel) {
				options = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
			} else {
				options = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
			}
			MessageDialog dialog = new MessageDialog(
					shell,
					TeamUIMessages.ModelParticipantAction_0, null,
					NLS.bind(TeamUIMessages.ModelParticipantAction_1, saveable.getName()),
					MessageDialog.QUESTION,
					options,
					result[0]);
			result[0] = dialog.open();
		};
		shell.getDisplay().syncExec(runnable);
		if (result[0] == 2)
			throw new InterruptedException();
		return result[0] == 0;
	}

	/**
	 * Return the currently active saveable. By default,
	 * the active saveable is obtained from the synchronization
	 * page configuration.
	 * @return the currently active saveable (or <code>null</code> if
	 * no buffer is active).
	 */
	protected SaveableComparison getActiveSaveable() {
		return ((ModelSynchronizeParticipant)configuration.getParticipant()).getActiveSaveable();
	}

	/**
	 * Set the active saveable. By default to active saveable is stored with the
	 * synchronize page configuration.
	 * @param saveable the saveable that is now active (or <code>null</code> if
	 * no saveable is active).
	 */
	protected void setActiveSaveable(SaveableComparison saveable) {
		((ModelSynchronizeParticipant)configuration.getParticipant()).setActiveSaveable(saveable);
	}

	/**
	 * Return the saveable that is the target of this operation.
	 * By default, <code>null</code> is returned.
	 * @return the saveable that is the target of this operation
	 */
	protected SaveableComparison getTargetSaveable() {
		return null;
	}

	/**
	 * Method called when the action is about to be shown in a context menu.
	 * This method recalculates the enablement for the current
	 * selection and uses that to set the enablement.
	 */
	public void updateEnablement() {
		setEnabled(isEnabledForSelection(getStructuredSelection()));
	}

}
