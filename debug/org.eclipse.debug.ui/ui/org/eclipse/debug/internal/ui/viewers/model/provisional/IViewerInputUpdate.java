/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * A request to provide a viewer input for a given element and presentation context.
 * Allows a view to translate the active debug context into an appropriate viewer
 * input element.
 * <p>
 * Clients requesting a viewer input update may implement this interface.
 * </p>
 * @since 3.4
 *
 */
public interface IViewerInputUpdate extends IViewerUpdate {

	/**
	 * Sets the input to use for this request's presentation context, or <code>null</code>
	 * if none (empty viewer). The source used to derive the viewer input is available
	 * from this request's <code>getElement()</code> method.
	 *
	 * @param element viewer input for this request's presentation context, possibly <code>null</code>
	 */
	void setInputElement(Object element);

	/**
	 * Returns the computed viewer input or <code>null</code> if none. The return value of this method
	 * only contains valid data if this request is complete (i.e. <code>done()</code> has been called).
	 *
	 * @return viewer input or <code>null</code>
	 */
	Object getInputElement();
}
