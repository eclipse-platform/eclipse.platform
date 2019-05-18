/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;

/**
 * Action that tags the local workspace with a version tag.
 */
public class TagLocalAction extends TagAction {
	
	@Override
	protected boolean performPrompting(ITagOperation operation)  {
		if (operation instanceof TagOperation) {
			final TagOperation tagOperation = (TagOperation) operation;
			try {
				if (hasOutgoingChanges(tagOperation)) {
					final boolean[] keepGoing = new boolean[] { true };
					Display.getDefault().syncExec(() -> {
						OutgoingChangesDialog dialog = new OutgoingChangesDialog(getShell(),
								tagOperation.getScopeManager(), CVSUIMessages.TagLocalAction_2,
								CVSUIMessages.TagLocalAction_0, ""); //$NON-NLS-1$
						dialog.setHelpContextId(IHelpContextIds.TAG_OUTGOING_CHANGES_DIALOG);
						int result = dialog.open();
						keepGoing[0] = result == Window.OK;
					});
					return keepGoing[0];
				}
				return true;
			} catch (InterruptedException e) {
				// Ignore
			} catch (InvocationTargetException e) {
				handle(e);
			}
		}
		return false;
	}

	@Override
	protected ITagOperation createTagOperation() {
		return new TagOperation(getTargetPart(), getCVSResourceMappings());
	}
	
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_TAGASVERSION;
	}
}
