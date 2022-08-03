/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.compare.contentmergeviewer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;


/**
 * Defines a subrange in a document.
 * <p>
 * It is used by text viewers that can work on a subrange of a document. For example,
 * a text viewer for Java compilation units might use this to restrict the view
 * to a single method.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see TextMergeViewer
 * @see org.eclipse.compare.structuremergeviewer.DocumentRangeNode
 */
public interface IDocumentRange {

	/**
	 * The position category typically used for an <code>IDocumentRange</code> position
	 * (value <code>"DocumentRangeCategory"</code>).
	 * @since 2.0
	 */
	public static final String RANGE_CATEGORY= "DocumentRangeCategory";	//$NON-NLS-1$

	/**
	 * Returns the underlying document.
	 *
	 * @return the underlying document
	 */
	IDocument getDocument();

	/**
	 * Returns a position that specifies a subrange in the underlying document,
	 * or <code>null</code> if this document range spans the whole underlying document.
	 *
	 * @return a position that specifies a subrange in the underlying document, or <code>null</code>
	 */
	Position getRange();
}
