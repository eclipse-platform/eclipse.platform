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

package org.eclipse.ui.internal.cheatsheets.data;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.parser.IStatusContainer;
import org.w3c.dom.Node;

/**
 * This class checks for incompatible children of items and subitems
 */

public class IncompatibleSiblingChecker {

	private IStatusContainer statusContainer;
	private Node parentNode;
	private String existingChild;
	private boolean errorReported = false;

	public IncompatibleSiblingChecker(IStatusContainer statusContainer, Node parentNode) {
		this.statusContainer = statusContainer;
		this.parentNode = parentNode;
	}

	/**
	 * Check to see that adding this new element does not create an error based on
	 * other elements. The rules are the only one action, command or perform-when can
	 * be declared per item or subitem and none of these can coexist with a subitem,
	 * conditional-subitem or repeated-subitem
	 * @param elementKind
	 */
	public void checkElement(String elementKind) {
		if (isExecutable(elementKind)) {
			if (existingChild == null) {
				existingChild = elementKind;
			} else {
				reportIncompatible(elementKind);
			}
		} else if (isSubitem(elementKind)) {
			if (isExecutable(existingChild)) {
				reportIncompatible(elementKind);
			}
		}
	}

	private boolean isSubitem(String elementKind) {
		return IParserTags.SUBITEM.equals(elementKind)
		|| IParserTags.CONDITIONALSUBITEM.equals(elementKind)
		|| IParserTags.REPEATEDSUBITM.equals(elementKind);
	}

	private boolean isExecutable(String elementKind) {
		return IParserTags.ACTION.equals(elementKind)
		|| IParserTags.COMMAND.equals(elementKind)
		|| IParserTags.PERFORMWHEN.equals(elementKind);
	}

	private void reportIncompatible(String elementKind) {
		if (errorReported) {
			return; // One message is enough
		}
		errorReported = true;
		String message;
		if (elementKind.equals(existingChild)) {
			message = NLS.bind(Messages.ERROR_PARSING_DUPLICATE_CHILD, (new Object[] {parentNode.getNodeName(), elementKind}));
		} else {
			message = NLS.bind(Messages.ERROR_PARSING_INCOMPATIBLE_CHILDREN, (new Object[] {parentNode.getNodeName(), existingChild, elementKind}));
		}
		statusContainer.addStatus(IStatus.ERROR, message, null);
	}
}
