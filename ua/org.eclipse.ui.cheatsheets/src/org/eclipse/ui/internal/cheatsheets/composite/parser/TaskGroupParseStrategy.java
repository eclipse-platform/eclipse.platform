/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.parser;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;
import org.w3c.dom.Node;

public class TaskGroupParseStrategy implements ITaskParseStrategy {


	public TaskGroupParseStrategy() {
	}

	@Override
	public void init() {
	}

	@Override
	public boolean parseElementNode(Node childNode, Node parentNode,
			AbstractTask parentTask, IStatusContainer status)
	{
		// Task children are handled by CompositeCheatSheetParser
		return false;
	}

	@Override
	public void parsingComplete(AbstractTask parentTask, IStatusContainer status) {
		String kind = parentTask.getKind();
		if (ITaskGroup.SEQUENCE.equals(kind)) {
			// Create dependencies between the children
			ICompositeCheatSheetTask[] children  = parentTask.getSubtasks();
			AbstractTask previous = null;
			AbstractTask next = null;
			for (ICompositeCheatSheetTask element : children) {
				previous = next;
				next = (AbstractTask) element;
				if (previous != null) {
					next.addRequiredTask(previous);
				}
			}
			checkForChildren(parentTask, status);
		} else if (ITaskGroup.SET.equals(kind)) {
			checkForChildren(parentTask, status);
		} else if (ITaskGroup.CHOICE.equals(kind)) {
			checkForChildren(parentTask, status);
		} else {
			String message = NLS.bind(
					Messages.ERROR_PARSING_TASK_INVALID_KIND,
					(new Object[] { parentTask.getKind(), ICompositeCheatsheetTags.TASK_GROUP, parentTask.getName()}));
			status.addStatus(IStatus.ERROR, message, null);
		}
	}

	private void checkForChildren(AbstractTask parentTask, IStatusContainer status) {
		if (parentTask.getSubtasks().length < 1) {
			String message = NLS.bind(
					Messages.ERROR_PARSING_CHILDLESS_TASK_GROUP,
					(new Object[] { parentTask.getName()}));
			status.addStatus(IStatus.ERROR, message, null);
		}
	}


}
