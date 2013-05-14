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
package org.eclipse.compare.contentmergeviewer;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.viewers.IContentProvider;


/** 
 * A content provider that mediates between a <code>ContentMergeViewer</code>'s model
 * and the viewer itself.
 * <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see ContentMergeViewer
 */
public interface IMergeViewerContentProvider extends IContentProvider {
	
	//---- ancestor side

	/**
	 * Returns the label for the ancestor side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the label for the ancestor side of a <code>ContentMergeViewer</code>
	 */
	String getAncestorLabel(Object input);

	/**
	 * Returns an optional image for the ancestor side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the image for the ancestor side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Image getAncestorImage(Object input);

	/**
	 * Returns the contents for the ancestor side of a <code>ContentMergeViewer</code>.
	 * The interpretation of the returned object depends on the concrete <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the content for the ancestor side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Object getAncestorContent(Object input);

	/**
	 * Returns whether the ancestor side of the given input element should be shown.
	 * @param input the merge viewer's input
	 * @return <code>true</code> if the ancestor side of the given input element should be shown
	 */
	boolean showAncestor(Object input);
	
	//---- left side

	/**
	 * Returns the label for the left side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the label for the left side of a <code>ContentMergeViewer</code>
	 */
	String getLeftLabel(Object input);

	/**
	 * Returns an optional image for the left side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the image for the left side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Image getLeftImage(Object input);

	/**
	 * Returns the contents for the left side of a <code>ContentMergeViewer</code>.
	 * The interpretation of the returned object depends on the concrete <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the content for the left side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Object getLeftContent(Object input);

	/**
	 * Returns whether the left side is editable.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return <code>true</code> if the left side of a <code>ContentMergeViewer</code> is editable	 
	 */
	boolean isLeftEditable(Object input);

	/**
	 * Saves new contents for the left side of the <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @param bytes the new contents to save for the left side
	 */
	void saveLeftContent(Object input, byte[] bytes);

	//---- right side

	/**
	 * Returns the label for the right side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the label for the right side of a <code>ContentMergeViewer</code>
	 */
	String getRightLabel(Object input);

	/**
	 * Returns an optional image for the right side of a <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the image for the right side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Image getRightImage(Object input);

	/**
	 * Returns the contents for the right side of a <code>ContentMergeViewer</code>.
	 * The interpretation of the returned object depends on the concrete <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return the content for the right side of a <code>ContentMergeViewer</code>,
	 *   or <code>null</code> if none
	 */
	Object getRightContent(Object input);

	/**
	 * Returns whether the right side is editable.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @return <code>true</code> if the right side of a <code>ContentMergeViewer</code> is editable	 
	 */
	boolean isRightEditable(Object input);

	/**
	 * Saves new contents for the right side of the <code>ContentMergeViewer</code>.
	 *
	 * @param input the input object of the <code>ContentMergeViewer</code>
	 * @param bytes the new contents to save for the right side
	 */
	void saveRightContent(Object input, byte[] bytes);
}


