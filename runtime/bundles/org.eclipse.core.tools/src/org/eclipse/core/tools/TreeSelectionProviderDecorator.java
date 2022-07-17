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

import java.util.*;
import org.eclipse.jface.viewers.*;

/**
 * This class provides a decorator for ISelectionProviders that use
 * TreeContentProviderNode as the basis for their data model.
 * The only affected method is <code>getSelection()</code>, which will return a
 * selection object that provides selected elements in the same order they
 * appear in the tree.
 *
 * <p>This class is an workaround to the SWT's <code>Tree.getSelection()</code>
 * method, which returns an array of selected elements without preserving the
 * order they appear in the tree widget.</p>
 */
public class TreeSelectionProviderDecorator implements ISelectionProvider {

	/** The decorated selection provider. */
	private ISelectionProvider selectionProvider;

	/**
	 * Constructs a <code>TreeSelectionProviderDecorator</code> having the given
	 * selection provider as its decorated object.
	 *
	 * @param selectionProvider the selection provider to be decorated
	 */
	public TreeSelectionProviderDecorator(ISelectionProvider selectionProvider) {
		this.selectionProvider = selectionProvider;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.addSelectionChangedListener(listener);
	}

	/**
	 * Returns the current selection for this provider. If the selection is a
	 * structured selection made of <code>TreeContentProviderNode</code>
	 * elements, this method will return a structured selection where the order of
	 * elements is the same order the elements appear in the tree (only for tree
	 * elements that are instances of <code>TreeContentProviderNode</code>).
	 *
	 * @return the current selection, ordered in the same sequence they appear in
	 * the tree
	 */
	@Override
	public ISelection getSelection() {
		// gets the original selection object
		ISelection selection = selectionProvider.getSelection();

		// in these cases the original selection will be returned
		if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return selection;

		// constructs a list with the selected elements
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		List<Object> list = structuredSelection.toList();
		final List<Object> selectedElements = new ArrayList<>(list);

		// tries to find a TreeContentProviderNode between the selected elements
		TreeContentProviderNode anyNode = findNodeElement(selectedElements);

		// if there is no TreeContentProviderNodes, there is nothing to do
		if (anyNode == null)
			return selection;

		// otherwise, we will move the elements to a new list in the same order
		// we find them in the tree.
		final List<Object> orderedElements = new LinkedList<>();

		// uses a visitor to traverse the whole tree
		// when a visited node is the selected list, it is moved to the ordered list
		anyNode.getRoot().accept(node -> {
			int elementIndex = selectedElements.indexOf(node);

			if (selectedElements.contains(node))
				orderedElements.add(selectedElements.remove(elementIndex));

			return true;
		});

		// any remaining elements in the list (probably they are not tree nodes)
		// are copied to the end of the ordered list
		orderedElements.addAll(selectedElements);
		return new StructuredSelection(orderedElements);
	}

	/**
	 * Returns the first element in the list that is instance of
	 * <code>TreeContentProviderNode</code>.
	 *
	 * @return the first element that is a tree node or null, if none is found.
	 */
	private TreeContentProviderNode findNodeElement(List<Object> elements) {
		for (Iterator<?> iter = elements.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof TreeContentProviderNode)
				return (TreeContentProviderNode) element;
		}

		return null;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		selectionProvider.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		selectionProvider.setSelection(selection);
	}

}
