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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

/**
 * Context sensitive label update request for an element.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 3.3
 */
public interface ILabelUpdate extends IViewerUpdate {

	/**
	 * Returns the id's of the columns which are to be updated
	 * or <code>null</code> if none.  Note, these columns may be different
	 * than the visible columns in the view which are returned by
	 * {@link IPresentationContext#getColumns()}.
	 *
	 * @return column id's or <code>null</code>
	 */
	String[] getColumnIds();

	/**
	 * Sets the text of the label of the specified column. Cannot be <code>null</code>.
	 *
	 * @param text to set to viewer
	 * @param columnIndex column index (0 when no columns)
	 */
	void setLabel(String text, int columnIndex);

	/**
	 * Sets the font of the label.
	 *
	 * @param fontData to set to viewer
	 * @param columnIndex column index (0 when no columns)
	 */
	void setFontData(FontData fontData, int columnIndex);

	/**
	 * Sets the image of the label.
	 *
	 * @param image to set to viewer
	 * @param columnIndex column index (0 when no columns)
	 */
	void setImageDescriptor(ImageDescriptor image, int columnIndex);

	/**
	 * Sets the foreground color of the label.
	 *
	 * @param foreground to set to viewer
	 * @param columnIndex column index (0 when no columns)
	 */
	void setForeground(RGB foreground, int columnIndex);

	/**
	 * Sets the background color of the label.
	 *
	 * @param background to set to viewer
	 * @param columnIndex column index (0 when no columns)
	 */
	void setBackground(RGB background, int columnIndex);
}
