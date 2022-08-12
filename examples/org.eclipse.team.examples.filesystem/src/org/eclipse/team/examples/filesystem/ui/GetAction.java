/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.examples.filesystem.Policy;

/**
 * Action for getting the contents of the selected resources
 */
public class GetAction extends FileSystemAction {

	@Override
	protected void execute(IAction action) {
		try {
			GetOperation operation = new GetOperation(getTargetPart(),
								FileSystemOperation.createScopeManager(Policy.bind("GetAction.working"), getSelectedMappings())); //$NON-NLS-1$
			operation.setOverwriteOutgoing(isOverwriteOutgoing());
			operation.run();
		} catch (InvocationTargetException e) {
			handle(e, null, Policy.bind("GetAction.problemMessage")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	/**
	 * Indicate whether the action should overwrite outgoing changes.
	 * By default, the get action does not override local modifications.
	 * @return whether the action should overwrite outgoing changes.
	 */
	protected boolean isOverwriteOutgoing() {
		return false;
	}
}
