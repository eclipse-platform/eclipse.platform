/*******************************************************************************
 *  Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.runtime.QualifiedName;

public class RootInfo extends ResourceInfo {
	/** The property store for this resource */
	protected Object propertyStore = null;

	/**
	 * Returns the property store associated with this info.  The return value may be null.
	 */
	@Override
	public Object getPropertyStore() {
		return propertyStore;
	}

	/**
	 * Override parent's behaviour and do nothing. Sync information
	 * cannot be stored on the workspace root so we don't need to
	 * update this counter which is used for deltas.
	 */
	@Override
	public void incrementSyncInfoGenerationCount() {
		// do nothing
	}

	/**
	 * Sets the property store associated with this info.  The value may be null.
	 */
	@Override
	public void setPropertyStore(Object value) {
		propertyStore = value;
	}

	/**
	 * Overrides parent's behaviour since sync information is not
	 * stored on the workspace root.
	 */
	@Override
	public void setSyncInfo(QualifiedName id, byte[] value) {
		// do nothing
	}
}
