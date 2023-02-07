/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.w3c.dom.Node;

/**
 * Interface which encapsulates the parsing logic specific to different
 * task kinds.
 */

public interface ITaskParseStrategy {

	/**
	 * Called before parsing a new set of children
	 */
	public void init();

	/**
	 * Parse a child node of this task
	 * @param childNode The child node
	 * @param parentNode The task node
	 * @param parentTask The task object
	 * @param status Used to add error messages.
	 * @return True if this element is a valid child
	 */
	public boolean parseElementNode(Node childNode, Node parentNode, AbstractTask parentTask, IStatusContainer status);

	/**
	 * Called afer all the children have been parsed to check for missing children
	 */
	public void parsingComplete(AbstractTask parentTask, IStatusContainer status);


}
