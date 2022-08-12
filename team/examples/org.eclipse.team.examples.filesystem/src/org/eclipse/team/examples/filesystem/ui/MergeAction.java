/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.ui.synchronize.ModelMergeOperation;

/**
 * This merge action is contributed as a a popupmenu objectContribution in
 * the plugin.xml. You can change the value return from {@link #isUseSyncFramework()}
 * to try out different dialogs.
 *
 * @since 3.2
 */
public class MergeAction extends FileSystemAction {

	@Override
	protected void execute(IAction action) {
		try {
			ModelMergeOperation operation;
			if (isUseSyncFramework()) {
				operation = new SyncDialogModelMergeOperation(getTargetPart(),
						FileSystemOperation.createScopeManager("Merging", getSelectedMappings()));
			} else {
				operation = new NonSyncModelMergeOperation(getTargetPart(),
						FileSystemOperation.createScopeManager("Merging", getSelectedMappings()));
			}
			operation.run();
		} catch (InvocationTargetException e) {
			handle(e, null, "Errors occurred while merging");
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	private boolean isUseSyncFramework() {
		return true;
	}
}
