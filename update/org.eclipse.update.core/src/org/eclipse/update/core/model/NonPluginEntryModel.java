/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

/**
 * Non-plug-in entry model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.NonPluginEntry
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class NonPluginEntryModel extends ContentEntryModel {

	private String id = null;

	/**
	 * Creates a uninitialised non-plug-in entry model object.
	 * 
	 * @since 2.0
	 */
	public NonPluginEntryModel() {
		super();
	}

	/**
	 * Returns the entry identifier.
	 *
	 * @return entry identifier, or <code>null</code>
	 * @since 2.0
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Sets the entry identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param id entry identifier.
	 * @since 2.0
	 */
	public void setIdentifier(String id) {
		assertIsWriteable();
		this.id = id;
	}
}
