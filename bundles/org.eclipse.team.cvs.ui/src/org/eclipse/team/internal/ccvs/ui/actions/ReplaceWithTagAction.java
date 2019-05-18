/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;

/**
 * Action for replace with tag.
 */
public abstract class ReplaceWithTagAction extends WorkspaceTraversalAction {
	
	@Override
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		final ReplaceOperation replaceOperation= createReplaceOperation();
		if (hasOutgoingChanges(replaceOperation)) {
			final boolean[] keepGoing = new boolean[] { true };
			Display.getDefault().syncExec(() -> {
				OutgoingChangesDialog dialog = new OutgoingChangesDialog(getShell(), replaceOperation.getScopeManager(),
						CVSUIMessages.ReplaceWithTagAction_2, CVSUIMessages.ReplaceWithTagAction_0,
						CVSUIMessages.ReplaceWithTagAction_1);
				dialog.setHelpContextId(IHelpContextIds.REPLACE_OUTGOING_CHANGES_DIALOG);
				int result = dialog.open();
				keepGoing[0] = result == Window.OK;
			});
			if (!keepGoing[0])
				return;
		}

		// Setup the tag holder
		final CVSTag[] tag= new CVSTag[] { null };

		// Show a busy cursor while display the tag selection dialog
		run((IRunnableWithProgress) monitor -> {
			monitor = Policy.monitorFor(monitor);
			tag[0] = getTag(replaceOperation);

			// finish, when tag can't be obtained
			if (tag[0] == null)
				return;

			// For non-projects determine if the tag being loaded is the same as the
			// resource's parent
			// If it's not, warn the user that they will have strange sync behavior
			try {
				if (!CVSAction.checkForMixingTags(getShell(), replaceOperation.getScope().getRoots(), tag[0])) {
					tag[0] = null;
					return;
				}
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);			 
		
		if (tag[0] == null) return;
		
		// Perform the replace in the background
		replaceOperation.setTag(tag[0]);
		replaceOperation.run();
	}

	/**
	 * Creates and returns a new <code>ReplaceOperation</code>.
	 * 
	 * @return the created replace operation
	 */
	protected ReplaceOperation createReplaceOperation() {
		return new ReplaceOperation(getTargetPart(), getCVSResourceMappings(), null);
	}

	@Override
	protected String getErrorTitle() {
		return CVSUIMessages.ReplaceWithTagAction_replace; 
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	/**
	 * This function should obtain a tag which user wants to load.
	 * @param replaceOperation 
	 *    replaceOperation is an operation for which the tag is obtained
	 * @return tag
	 * 		marks the resources revision the user wants to load
	 */
	abstract protected CVSTag getTag(final ReplaceOperation replaceOperation);
	
}
