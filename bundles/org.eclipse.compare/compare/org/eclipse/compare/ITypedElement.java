/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare;

import org.eclipse.swt.graphics.Image;

/**
 * Interface for getting the name, image, and type for an object.
 * <p>
 * These methods are typically used to present an input object in the compare UI
 * (<code>getName</code> and <code>getImage</code>)
 * and for finding a viewer for a given input type (<code>getType</code>).
 * <p>
 * Clients may implement this interface.
 */
public interface ITypedElement {

	/**
	 * Type for a folder input (value <code>"FOLDER"</code>).
	 * Folders are comparison elements that have no contents, only a name and children.
	 */
	public static final String FOLDER_TYPE= "FOLDER"; //$NON-NLS-1$

	/**
	 * Type for an element whose actual type is text  (value <code>"txt"</code>).
	 */
	public static final String TEXT_TYPE= "txt"; //$NON-NLS-1$

	/**
	 * Type for an element whose actual type could not
	 * be determined. (value <code>"???"</code>).
	 */
	public static final String UNKNOWN_TYPE= "???"; //$NON-NLS-1$

	/**
	 * Returns the name of this object.
	 * The name is used when displaying this object in the UI.
	 *
	 * @return the name of this object
	 */
	String getName();

	/**
	 * Returns an image for this object.
	 * This image is used when displaying this object in the UI.
	 *
	 * @return the image of this object or <code>null</code> if this type of input has no image
	 */
	Image getImage();

	/**
	 * Returns the type of this object. For objects with a file name
	 * this is typically the file extension. For folders its the constant
	 * <code>FOLDER_TYPE</code>.
	 * The type is used for determining a suitable viewer for this object.
	 *
	 * @return the type of this object
	 */
	String getType();
}
