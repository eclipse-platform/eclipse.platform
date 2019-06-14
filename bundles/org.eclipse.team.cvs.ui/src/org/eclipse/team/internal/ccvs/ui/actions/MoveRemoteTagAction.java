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

import org.eclipse.jface.window.Window;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.IHelpContextIds;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.tags.TagSelectionDialog;

public class MoveRemoteTagAction extends TagInRepositoryAction {

	@Override
	protected ITagOperation configureOperation() {
		// Allow the user to select a tag
		ITagOperation operation = createTagOperation();
		TagSelectionDialog dialog = new TagSelectionDialog(getShell(), operation.getTagSource(), 
			CVSUIMessages.MoveTagAction_title, 
			CVSUIMessages.MoveTagAction_message, 
			TagSelectionDialog.INCLUDE_BRANCHES | TagSelectionDialog.INCLUDE_VERSIONS, 
			isFolderSelected(), /* show recurse */
			IHelpContextIds.TAG_REMOTE_WITH_EXISTING_DIALOG);
		dialog.setBlockOnOpen(true);
		if (dialog.open() == Window.CANCEL) {
			return null;
		}
		CVSTag tag = dialog.getResult();
		if (tag == null) return null;
		operation.setTag(tag);
		operation.moveTag();
		boolean recursive = dialog.getRecursive();
		if (!recursive)  {
			operation.doNotRecurse();
		}
		return operation;
	}

	private boolean isFolderSelected() {
		ICVSResource[] resources = getSelectedCVSResources();
		for (ICVSResource resource : resources) {
			if (resource.isFolder()) 
				return true;
		}
		return false;
	}
}
