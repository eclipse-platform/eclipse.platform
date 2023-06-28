/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;


/**
 * Context sensitive children update request for a parent and subrange of its
 * children.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.3
 */
public interface IChildrenUpdate extends IViewerUpdate {

	/**
	 * Returns the offset at which children have been requested for. This is
	 * the index of the first child being requested.
	 *
	 * @return offset at which children have been requested for
	 */
	int getOffset();

	/**
	 * Returns the number of children requested.
	 *
	 * @return number of children requested
	 */
	int getLength();

	/**
	 * Sets the child for this request's parent at the given offset.
	 *
	 * @param child child
	 * @param offset child offset
	 */
	void setChild(Object child, int offset);
}
