/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPartConstants;

/**
 * An extension to the detail pane interface which allows implementors to
 * save contents of the details pane.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.6
 */
public interface IDetailPane3 extends IDetailPane, ISaveablePart {

	/**
	 * Adds a listener for changes to properties in this detail pane.
	 * Has no effect if an identical listener is already registered.
	 * <p>
	 * The property ids are defined in {@link IWorkbenchPartConstants}.
	 * </p>
	 *
	 * @param listener a property listener
	 */
	void addPropertyListener(IPropertyListener listener);

	/**
	 * Removes the given property listener from this workbench part.
	 * Has no effect if an identical listener is not registered.
	 *
	 * @param listener a property listener
	 */
	void removePropertyListener(IPropertyListener listener);
}
