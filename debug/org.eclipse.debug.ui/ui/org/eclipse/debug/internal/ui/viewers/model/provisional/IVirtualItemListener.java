/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
 * Interface for listeners that need to be notified when items
 * are disposed or revealed.  It  should be implemented by the viewer.
 *
 * @see VirtualTreeModelViewer
 * @since 3.8
 */
public interface IVirtualItemListener {

	/**
	 * Called when the item has been shown in the virtual viewer's
	 * view-port.  This indicates to the viewer that it should check
	 * the item's status and request needed data.
	 *
	 * @param item The item that was revealed.
	 */
	void revealed(VirtualItem item);

	/**
	 * Called when an item is disposed.  It tells the viewer to
	 * clean up any remaining mappings and cached data of this item.
	 *
	 * @param item The itam that was disposed.
	 */
	void disposed(VirtualItem item);
}