/*****************************************************************
 * Copyright (c) 2009, 2011 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 286310)
 *     IBM Corporation - ongoing enhancements
 *****************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.viewers.TreePath;


/**
 * Optional extension to a model proxy for models that use a check box tree viewer. Provides
 * notification for check state changes in the tree.
 *
 * @since 3.6
 * @see IModelProxy
 */
public interface ICheckboxModelProxy {

	/**
	 * Notifies the receiver that the given element has had its
	 * checked state modified in the viewer.
	 * <p>
	 * This method is called in the UI thread. Clients that execute long running operations or
	 * communicate with a potentially unreliable or blocking model should run those operations
	 * asynchronously.
	 * </p>
	 *
	 * @param context Presentation context in which the element was updated.
	 * @param viewerInput The root element of the viewer where the check
	 * selection took place.
	 * @param path Path of the element that had its checked state changed
	 * @param checked The new checked state of the element
	 * @return false if the check state should not change
	 */
	boolean setChecked(IPresentationContext context, Object viewerInput, TreePath path, boolean checked);

}
