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

/**
 * Common interface for objects with a modification date. The modification date
 * can be used in the UI to give the user a general idea of how old an object is.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IModificationDate {
	
	/**
	 * Returns the modification time of this object.
	 * <p>
	 * Note that this value should only be used to give the user a general idea of how
	 * old the object is.
	 *
	 * @return the time of last modification, in milliseconds since January 1, 1970, 00:00:00 GMT
	 */
	long getModificationDate();
}
