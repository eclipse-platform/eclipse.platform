/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.jface.text.IDocument;

/**
 * An interface that provides access to the internals of a merge viewer for the purposes of testing.
 * NOTE: This interface is not to be used for any other purpose.
 */
public interface IMergeViewerTestAdapter {

	/**
	 * Return the document for the given leg
	 * @param leg the leg (or side)
	 * @return the document for that leg of the comparison
	 */
	public IDocument getDocument(char leg);

	/**
	 * Returns the number of changes in merge viewer
	 * 
	 * @return the number of changes
	 */
	public int getChangesCount();
}
