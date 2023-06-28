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

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Defines the possible set of columns presented in a view for a model.
 * A column presentation is customizable per presentation context (view) and
 * view input, and is created by an <code>IColumnPresentationFactory</code>.
 *
 * @since 3.2
 */
public interface IColumnPresentation {

	/**
	 * Initializes this column presentation to be used in the
	 * given context.
	 *
	 * @param context Presentation context.
	 */
	void init(IPresentationContext context);

	/**
	 * Disposes this column presentation
	 */
	void dispose();

	/**
	 * Returns an array of all columns supported by this adapter as
	 * column identifiers.
	 *
	 * @return column identifiers
	 */
	String[] getAvailableColumns();

	/**
	 * Returns an ordered array of columns that should be displayed initially for
	 * this presentation as column IDs.
	 *
	 * @return initial columns to display
	 */
	String[] getInitialColumns();

	/**
	 * Returns the column header for the column with the given identifier.
	 *
	 * @param id a column identifier included in <code>getColumns()</code>
	 * @return column header
	 */
	String getHeader(String id);

	/**
	 * Returns the image for the column with the given identifier, or <code>null</code>
	 *
	 * @param id column id
	 * @return image descriptor or <code>null</code>
	 */
	ImageDescriptor getImageDescriptor(String id);

	/**
	 * Returns an identifier for this columns presentation.
	 * The identifier should be unique per kind of column presentation
	 * (for example, the column presentation for Java stack frames
	 * in the variables view). Allows visible column information to be
	 * persisted by the platform.
	 *
	 * @return identifier
	 */
	String getId();

	/**
	 * Returns whether this column presentation is optional. When a column presentation
	 * is optional, the user may toggle columns on/off.
	 *
	 * @return whether this column presentation is optional
	 */
	boolean isOptional();

}
