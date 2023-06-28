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
 * Request monitor used to collect the number of children for an element in a viewer.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.3
 */
public interface IChildrenCountUpdate extends IViewerUpdate {

	/**
	 * Sets the number of children for this update.
	 *
	 * @param numChildren number of children
	 */
	void setChildCount(int numChildren);
}
