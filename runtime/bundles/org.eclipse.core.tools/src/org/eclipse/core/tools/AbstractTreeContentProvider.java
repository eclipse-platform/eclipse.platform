/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * An abstract base class for hierarchical content providers. Uses
 * <code>TreeContentProviderNode</code> objects to keep a hierarchical
 * data model. Subclasses must provide an implementation for the
 * <code>#rebuild(Object)</code> operation in order to define how the data model
 * will be built upon a given input provided by the viewer.
 */

public abstract class AbstractTreeContentProvider implements ITreeContentProvider {

	/**
	 * Flag for omitting the root or not when providing the contents.
	 */
	private boolean omitRoot;

	/**
	 * The root node.
	 */
	private TreeContentProviderNode rootNode;

	/**
	 * Constructs a AbstractTreeContentProvider.
	 *
	 * @param omitRoot if true, the root node will be omitted when providing
	 * contents.
	 */
	protected AbstractTreeContentProvider(boolean omitRoot) {
		this.omitRoot = omitRoot;
	}

	/**
	 * Constructs a AbstractTreeContentProvider that will omit the root node when
	 * providing contents.
	 *
	 * @see #AbstractTreeContentProvider(boolean)
	 */
	protected AbstractTreeContentProvider() {
		this(true);
	}

	/**
	 * Returns the child elements of the given parent element.
	 *
	 * @return an array containing <code>parentElement</code>'s children.
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(
	 * java.lang.Object)
	 * @see org.eclipse.core.tools.TreeContentProviderNode#getChildren()
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (!(parentElement instanceof TreeContentProviderNode))
			return null;

		TreeContentProviderNode treeNode = (TreeContentProviderNode) parentElement;
		return treeNode.getChildren();
	}

	/**
	 * Returns the parent for the given element, or <code>null</code>
	 * indicating that the parent can't be computed.
	 *
	 * @return <code>element</code>'s parent node or null, if it is a root node
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(
	 * java.lang.Object)
	 * @see org.eclipse.core.tools.TreeContentProviderNode#getParent()
	 */
	@Override
	public Object getParent(Object element) {
		if (!(element instanceof TreeContentProviderNode))
			return null;

		TreeContentProviderNode treeNode = (TreeContentProviderNode) element;
		return treeNode.getParent();
	}

	/**
	 * Returns whether the given element has children.
	 *
	 * @return true, if <code>element</code> has children, false otherwise
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(
	 * java.lang.Object)
	 * @see org.eclipse.core.tools.TreeContentProviderNode#hasChildren()
	 */
	@Override
	public boolean hasChildren(Object element) {
		return element instanceof TreeContentProviderNode && ((TreeContentProviderNode) element).hasChildren();
	}

	/**
	 * Returns the elements to display in the viewer
	 * when its input is set to the given element.
	 *
	 * @return this content provider root element's children
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(
	 * java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (rootNode == null)
			return new Object[0];

		return omitRoot ? rootNode.getChildren() : new Object[] {rootNode};
	}

	/**
	 * Disposes of this content provider.
	 * This is called by the viewer when it is disposed.
	 *
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		rootNode = null;
	}

	/**
	 * Helper method that creates a root node given a node name and value.
	 *
	 * @param name the name of the node
	 * @param value the value of the node. May be null.
	 * @return the tree node created
	 * @see TreeContentProviderNode#TreeContentProviderNode(String, Object)
	 */
	protected TreeContentProviderNode createNode(String name, Object value) {
		return new TreeContentProviderNode(name, value);
	}

	/**
	 * Helper method that creates a root node given a node name and no value.
	 *
	 * @param name the name of the node
	 * @return the tree node created
	 * @see TreeContentProviderNode#TreeContentProviderNode(String)
	 */
	protected TreeContentProviderNode createNode(String name) {
		return new TreeContentProviderNode(name);
	}

	/**
	 * Notifies this content provider that the given viewer's input
	 * has been switched to a different element.
	 * Rebuilds this content provider's state from a given resource.
	 *
	 * @param viewer the viewer
	 * @param oldInput ignored
	 * @param input the new input. If null, clears this content provider. If not,
	 * is passed in a call to <code>rebuild(Object)</code>.
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(
	 * org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 * @see #rebuild(Viewer, Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, final Object input) {
		if (input == null) {
			rootNode = createNode("root"); //$NON-NLS-1$
			return;
		}

		if (!acceptInput(input))
			return;

		rootNode = createNode("root"); //$NON-NLS-1$
		rebuild(viewer, input);
	}

	/**
	 * Reconstructs this content provider data model upon the provided input object.
	 *
	 * @param input the new input object - must not be null
	 * @param viewer the corresponding viewer
	 */
	protected abstract void rebuild(Viewer viewer, Object input);

	/**
	 * Returns true if the provided input is accepted by this content provider.
	 *
	 * @param input an input object
	 * @return boolean true if the provided object is accepted, false otherwise
	 */
	protected abstract boolean acceptInput(Object input);

	/**
	 * Returns the rootNode.
	 *
	 * @return this content provider root node
	 */
	protected TreeContentProviderNode getRootNode() {
		return rootNode;
	}

}
