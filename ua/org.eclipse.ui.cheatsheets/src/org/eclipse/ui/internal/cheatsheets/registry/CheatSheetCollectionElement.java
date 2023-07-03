/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.registry;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
/**
 * Instances of this class are a collection of CheatSheetCollectionElements,
 * thereby facilitating the definition of tree structures composed of
 * these elements. Instances also store a list of cheatsheets.
 */
public class CheatSheetCollectionElement extends WorkbenchAdapter implements IPluginContribution {
	private String pluginId;
	private String id;
	private String name;
	private CheatSheetCollectionElement parent;
	private AdaptableList cheatsheets = new AdaptableList();
	private List<CheatSheetCollectionElement> childCollections = new ArrayList<>();

	/**
	 * Creates a new <code>CheatSheetCollectionElement</code>.  Parent can be null.
	 *
	 * @param name java.lang.String
	 */
	public CheatSheetCollectionElement(String pluginId, String id, String name, CheatSheetCollectionElement parent) {
		this.name = name;
		this.pluginId = pluginId;
		this.id = id;
		this.parent = parent;
	}

	/**
	 * Adds a cheatsheet collection to this collection.
	 */
	public void add(IAdaptable a) {
		if (a instanceof CheatSheetElement) {
			cheatsheets.add(a);
		} else {
			childCollections.add((CheatSheetCollectionElement) a);
		}
	}

	/**
	 * Returns the cheatsheet collection child object corresponding to the
	 * passed path (relative to this object), or <code>null</code> if
	 * such an object could not be found.
	 *
	 * @param searchPath org.eclipse.core.runtime.IPath
	 * @return CheatSheetCollectionElement
	 */
	public CheatSheetCollectionElement findChildCollection(IPath searchPath) {
		Object[] children = getChildren();
		String searchString = searchPath.segment(0);
		for (Object element : children) {
			CheatSheetCollectionElement currentCategory = (CheatSheetCollectionElement) element;
			if (currentCategory.getLabel(null).equals(searchString)) {
				if (searchPath.segmentCount() == 1)
					return currentCategory;

				return currentCategory.findChildCollection(searchPath.removeFirstSegments(1));
			}
		}

		return null;
	}

	/**
	 * Returns this collection's associated cheatsheet object corresponding to the
	 * passed id, or <code>null</code> if such an object could not be found.
	 */
	public CheatSheetElement findCheatSheet(String searchId, boolean recursive) {
		Object[] cheatsheets = getCheatSheets();
		for (Object cheatsheet : cheatsheets) {
			CheatSheetElement currentCheatSheet = (CheatSheetElement) cheatsheet;
			if (currentCheatSheet.getID().equals(searchId))
				return currentCheatSheet;
		}
		if (!recursive)
			return null;
		for (CheatSheetCollectionElement child : childCollections) {
			CheatSheetElement result = child.findCheatSheet(searchId, true);
			if (result != null)
				return result;
		}
		return null;
	}

	/**
	 * Returns an object which is an instance of the given class
	 * associated with this object. Returns <code>null</code> if
	 * no such object can be found.
	 */
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/**
	 * Returns the unique ID of this element.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the label for this collection.
	 */
	@Override
	public String getLabel(Object o) {
		return name;
	}

	/**
	 * Returns the logical parent of the given object in its tree.
	 */
	@Override
	public Object getParent(Object o) {
		return parent;
	}

	/**
	 * Returns a path representing this collection's ancestor chain.
	 */
	public IPath getPath() {
		if (parent == null)
			return IPath.fromOSString(ICheatSheetResource.EMPTY_STRING);

		return parent.getPath().append(name);
	}

	/**
	 * Returns this collection element's associated collection of cheatsheets.
	 */
	public Object[] getCheatSheets() {
		return cheatsheets.getChildren();
	}

	/**
	 * Returns true if this element has no children and no cheatsheets.
	 */
	public boolean isEmpty() {
		return childCollections.isEmpty() && cheatsheets.size() == 0;
	}

	/**
	 * Sets this collection's unique id.
	 */
	public void setId(java.lang.String newId) {
		id = newId;
	}

	/**
	 * Sets the collection of cheatsheets associated with this collection element.
	 */
	public void setCheatSheets(AdaptableList value) {
		cheatsheets = value;
	}

	/**
	 * For debugging purposes.
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("CheatSheetCollection, "); //$NON-NLS-1$
		buf.append(childCollections.size());
		buf.append(" children, "); //$NON-NLS-1$
		buf.append(cheatsheets.size());
		buf.append(" cheatsheets"); //$NON-NLS-1$
		return buf.toString();
	}

	@Override
	public String getLocalId() {
		return getId();
	}

	@Override
	public String getPluginId() {
		return pluginId;
	}

	public Object[] getChildren() {
		return childCollections.toArray();
	}

	public void add(CheatSheetCollectionElement newElement) {
		childCollections.add(newElement);
	}
}
