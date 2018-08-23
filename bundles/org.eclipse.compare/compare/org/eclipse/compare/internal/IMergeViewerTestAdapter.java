/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
