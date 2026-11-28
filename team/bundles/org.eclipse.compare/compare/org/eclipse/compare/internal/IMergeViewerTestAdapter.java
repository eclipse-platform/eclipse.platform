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
import org.eclipse.swt.events.PaintEvent;

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

	/**
	 * Returns the vertical position used to draw comparison highlights for a line.
	 *
	 * @param viewer the source viewer
	 * @param line the zero-based line index
	 * @return the vertical position in pixels
	 * @throws UnsupportedOperationException if this adapter does not support geometry access
	 */
	default int getLineY(MergeSourceViewer viewer, int line) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the height of the given line range.
	 *
	 * @param viewer the source viewer
	 * @param fromLine the zero-based start line, inclusive
	 * @param toLine the zero-based end line, exclusive
	 * @return the height in pixels
	 * @throws UnsupportedOperationException if this adapter does not support geometry access
	 */
	default int getHeightBetweenLines(MergeSourceViewer viewer, int fromLine, int toLine) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Paints comparison highlights for the given source viewer.
	 *
	 * @param event the paint event containing the graphics context
	 * @param viewer the source viewer
	 * @throws UnsupportedOperationException if this adapter does not support painting access
	 */
	default void paint(PaintEvent event, MergeSourceViewer viewer) {
		throw new UnsupportedOperationException();
	}
}
