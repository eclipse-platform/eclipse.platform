/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Category provides for hierarchical grouping of elements registered in the
 * registry. One extension normally defines a category, and other reference it
 * via its ID.
 * <p>
 * A category may specify its parent category in order to achieve hierarchy.
 * </p>
 */
public class Category implements IWorkbenchAdapter, IPluginContribution,
		IAdaptable {
	/**
	 * Name of the miscellaneous category
	 */
	public final static String MISC_NAME = Messages.CATEGORY_OTHER;

	/**
	 */
	public final static String MISC_ID = "org.eclipse.ui.cheatsheets.otherCategory"; //$NON-NLS-1$

	private String id;

	private String name;

	private String[] parentPath;

	private ArrayList<Object> elements;

	private IConfigurationElement configurationElement;

	private String pluginId;

	/**
	 * Creates an instance of <code>Category</code> as a miscellaneous
	 * category.
	 */
	public Category() {
		this.id = MISC_ID;
		this.name = MISC_NAME;
		this.pluginId = MISC_ID; // TODO: remove hack for bug 55172
	}

	/**
	 * Creates an instance of <code>Category</code> with an ID and label.
	 *
	 * @param id
	 *            the unique identifier for the category
	 * @param label
	 *            the presentation label for this category
	 */
	public Category(String id, String label) {
		this.id = id;
		this.name = label;
	}

	/**
	 * Creates an instance of <code>Category</code> using the information from
	 * the specified configuration element.
	 *
	 * @param configElement
	 *            the <code>IConfigurationElement</code> containing
	 * 		the ID, label, and optional parent category path.
	 * @throws WorkbenchException if the ID or label is <code>null</code>
	 */
	public Category(IConfigurationElement configElement)
			throws WorkbenchException {
		id = configElement.getAttribute("id"); //$NON-NLS-1$

		configurationElement = configElement;
		if (id == null || getLabel() == null)
			throw new WorkbenchException("Invalid category: " + id); //$NON-NLS-1$
	}

	/**
	 * Add an element to this category.
	 *
	 * @param element
	 *            the element to add
	 */
	public void addElement(Object element) {
		if (elements == null)
			elements = new ArrayList<>(5);
		elements.add(element);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class)
			return adapter.cast(this);
		else if (adapter == IConfigurationElement.class)
			return adapter.cast(configurationElement);
		else
			return null;
	}

	@Override
	public Object[] getChildren(Object o) {
		return getElements().toArray();
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		return getLabel();
	}

	/**
	 * Return the id for this category.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Return the label for this category.
	 *
	 * @return the label
	 */
	public String getLabel() {
		return configurationElement == null ? name : configurationElement
				.getAttribute("name"); //$NON-NLS-1$
	}

	/**
	 * Return the parent path for this category.
	 *
	 * @return the parent path
	 */
	public String[] getParentPath() {
		if (parentPath != null)
			return parentPath;

		String unparsedPath = getRawParentPath();
		if (unparsedPath != null) {
			StringTokenizer stok = new StringTokenizer(unparsedPath, "/"); //$NON-NLS-1$
			parentPath = new String[stok.countTokens()];
			for (int i = 0; stok.hasMoreTokens(); i++) {
				parentPath[i] = stok.nextToken();
			}
		}

		return parentPath;
	}

	/**
	 * Return the unparsed parent path. May be <code>null</code>.
	 *
	 * @return the unparsed parent path or <code>null</code>
	 */
	public String getRawParentPath() {
		return configurationElement == null ? null : configurationElement
				.getAttribute("parentCategory"); //$NON-NLS-1$
	}

	/**
	 * Return the root path for this category.
	 *
	 * @return the root path
	 */
	public String getRootPath() {
		String[] path = getParentPath();
		if (path != null && path.length > 0)
			return path[0];

		return id;
	}

	/**
	 * Return the elements contained in this category.
	 *
	 * @return the elements
	 */
	public ArrayList<Object> getElements() {
		return elements;
	}

	/**
	 * Return whether a given object exists in this category.
	 *
	 * @param o
	 *            the object to search for
	 * @return whether the object is in this category
	 */
	public boolean hasElement(Object o) {
		if (elements == null)
			return false;
		if (elements.isEmpty())
			return false;
		return elements.contains(o);
	}

	/**
	 * Return whether this category has child elements.
	 *
	 * @return whether this category has child elements
	 */
	public boolean hasElements() {
		if (elements != null)
			return !elements.isEmpty();

		return false;
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public String getLocalId() {
		return id;
	}

	@Override
	public String getPluginId() {
		return configurationElement == null ? pluginId : configurationElement
				.getContributor().getName();
	}

	/**
	 * Clear all elements from this category.
	 *
	 * @since 3.1
	 */
	public void clear() {
		if (elements != null) {
			elements.clear();
		}
	}
}
