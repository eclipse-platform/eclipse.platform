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
 * Context sensitive update request for whether an element has children.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.3
 */
public interface IHasChildrenUpdate extends IViewerUpdate {

	/**
	 * Sets whether there are children for this update.
	 *
	 * @param hasChildren whether there are children
	 */
	void setHasChilren(boolean hasChildren);
}
