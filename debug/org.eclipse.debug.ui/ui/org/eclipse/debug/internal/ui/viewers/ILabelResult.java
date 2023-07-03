/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.Image;


/**
 * Results of collecting an asynchronous label in a tree.
 *
 * @since 3.2
 */
public interface ILabelResult {

	/**
	 * Returns the labels for the element. One for each column.
	 *
	 * @return
	 */
	String[] getLabels();

	/**
	 * Returns the images for the element.
	 *
	 * @return
	 */
	Image[] getImages();

	/**
	 * Returns the element the label is for.
	 *
	 * @return
	 */
	Object getElement();

	/**
	 * Returns the path to the element in the tree.
	 *
	 * @return
	 */
	TreePath getTreePath();

	/**
	 * Returns this element's depth in the tree.
	 *
	 * @return
	 */
	int getDepth();
}
