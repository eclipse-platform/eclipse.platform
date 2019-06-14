/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.tags.TagAsVersionDialog;

/**
 * TagAction tags the selected resources with a version tag specified by the user.
 */
public abstract class TagAction extends WorkspaceTraversalAction {
	
	// remember if the execute action was cancelled
	private boolean wasCancelled = false;

	@Override
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		setWasCancelled(false);
		
		// Prompt for the tag name
		final ITagOperation[] result = new ITagOperation[1];
		getShell().getDisplay().syncExec(() -> {
			result[0] = configureOperation();
			if (result[0] == null) {
				return;
			}
		});
		
		if (result[0] == null)  {
			setWasCancelled(true);
			return;
		}
		
		result[0].run();
	}
	
	protected boolean performPrompting(ITagOperation operation)  {
		return true;
	}
	
	/**
	 * Prompts the user for a tag name.
	 * Note: This method is designed to be overridden by test cases.
	 * @return the operation, or null to cancel
	 */
	protected ITagOperation configureOperation() {
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		ITagOperation operation = createTagOperation();
		if (operation.isEmpty()) {
			return null;
		}
		if (!performPrompting(operation)) {
			return null;
		}
		TagAsVersionDialog dialog = new TagAsVersionDialog(getShell(),
											CVSUIMessages.TagAction_tagResources, 
											operation);
		if (dialog.open() != Window.OK) return null;

		// The user has indicated they want to force a move.  Make sure they really do.		
		if (dialog.shouldMoveTag() && store.getBoolean(ICVSUIConstants.PREF_CONFIRM_MOVE_TAG))  {
			MessageDialogWithToggle confirmDialog = MessageDialogWithToggle.openYesNoQuestion(getShell(), 
				CVSUIMessages.TagAction_moveTagConfirmTitle,  
				NLS.bind(CVSUIMessages.TagAction_moveTagConfirmMessage, new String[] { dialog.getTagName() }), 
				null,
				false,
				null,
				null);
			
			if (confirmDialog.getReturnCode() == IDialogConstants.YES_ID)  {
				store.setValue(ICVSUIConstants.PREF_CONFIRM_MOVE_TAG, !confirmDialog.getToggleState());
			} else  {
				return null;
			}
		}
		
		// The user is a cowboy and wants to do it.
		return dialog.getOperation();
	}
	
	protected abstract ITagOperation createTagOperation();

	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.TagAction_tagErrorTitle; 
	}
	
	@Override
	protected String getWarningTitle() {
		return CVSUIMessages.TagAction_tagWarningTitle; 
	}
	
	@Override
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	public boolean wasCancelled() {
		return wasCancelled;
	}

	public void setWasCancelled(boolean b) {
		wasCancelled = b;
	}

	public static void broadcastTagChange(final ICVSResource[] resources, final CVSTag tag) throws InvocationTargetException, InterruptedException {
		final RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
		manager.run(new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				try {
					for (ICVSResource resource : resources) {
						// Cache the new tag creation even if the tag may have had warnings.
						manager.addTags(getRootParent(resource), new CVSTag[] {tag});
					}
				} catch (CVSException e) {
					CVSUIPlugin.log(e);
				}
			}
			private ICVSResource getRootParent(ICVSResource resource) throws CVSException {
				if (!resource.isManaged()) return resource;
				ICVSFolder parent = resource.getParent();
				if (parent == null) return resource;
				// Special check for a parent which is the repository itself
				if (parent.getName().length() == 0) return resource;
				return getRootParent(parent);
			}
		}, new NullProgressMonitor());
	}
}

