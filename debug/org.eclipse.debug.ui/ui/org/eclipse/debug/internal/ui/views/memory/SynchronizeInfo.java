/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.debug.core.model.IMemoryBlock;

/**
 * Stores synchronization information for a memory block Each object of
 * synchronization information contains a memory block, a list of views to be
 * synchronized and a list of properties to be syncrhonized. The views are
 * responsible for defining properties to be synchronized and notifying the
 * synchronizer of properties changes. This is only for keeping track of values
 * of synchronized properties and firing events when properties are changed.
 *
 * Memory block serves as a key for synchronization. Views displaying the same
 * memory block can be synchronized. Views displaying different memory block
 * cannot be synchronized.
 *
 * @since 3.0
 */
public class SynchronizeInfo {
	private IMemoryBlock fBlock; // memory block blocked by the views
	private Hashtable<String, Object> fProperties; // list of properties to be
													// synchronized

	/**
	 * Create a new synchronization info object for the memory block
	 *
	 * @param block
	 */
	public SynchronizeInfo(IMemoryBlock block) {
		fBlock = block;
		fProperties = new Hashtable<>();
	}

	/**
	 * Set a property and its value to the info object
	 *
	 * @param propertyId
	 * @param value
	 */
	public void setProperty(String propertyId, Object value) {
		if (propertyId == null)
			return;

		if (value == null)
			return;

		fProperties.put(propertyId, value);
	}

	/**
	 * Returns the value of the property from the info object
	 *
	 * @param propertyId
	 * @return value of the property
	 */
	public Object getProperty(String propertyId) {
		if (propertyId == null)
			return null;

		Object value = fProperties.get(propertyId);

		return value;
	}

	/**
	 * @return all the property ids stored in this sync info object
	 */
	public String[] getPropertyIds() {
		if (fProperties == null)
			return new String[0];

		Enumeration<String> enumeration = fProperties.keys();
		ArrayList<String> ids = new ArrayList<>();

		while (enumeration.hasMoreElements()) {
			ids.add(enumeration.nextElement());
		}

		return ids.toArray(new String[ids.size()]);
	}

	/**
	 * Clean up the synchronization info object
	 */
	public void delete() {

		if (fProperties != null) {
			fProperties.clear();
			fProperties = null;
		}

		if (fBlock != null) {
			fBlock = null;
		}
	}
}
