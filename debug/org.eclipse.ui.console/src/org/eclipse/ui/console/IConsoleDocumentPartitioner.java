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

package org.eclipse.ui.console;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.swt.custom.StyleRange;

/**
 * A document partitioner for a text console.
 * <p>
 * In addition to regular partitioner duties, a console document partitioner
 * dictates which regions in its document are read-only and provides style
 * ranges.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * In order to provided backward compatibility for clients of
 * <code>IConsoleDocumentPartitioner</code>, extension interfaces are used to
 * provide a means of evolution. The following extension interfaces exist:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.ui.console.IConsoleDocumentPartitionerExtension} since
 * version 3.9 adding more possibilities to query read-only regions.</li>
 * </ul>
 *
 * @see org.eclipse.ui.console.TextConsole
 * @see org.eclipse.ui.console.IConsoleDocumentPartitionerExtension
 * @since 3.1
 */
public interface IConsoleDocumentPartitioner extends IDocumentPartitioner {

	/**
	 * Returns whether this partitioner's document is read-only at the specified
	 * offset. The user is not allowed to type in read-only locations.
	 *
	 * @param offset document offset
	 * @return whether this partitioner's document is read-only at the specified
	 * offset
	 */
	boolean isReadOnly(int offset);

	/**
	 * Returns style ranges for the specified region of this partitioner's document
	 * to use when rendering, or <code>null</code> if none.
	 *
	 * @param offset beginning offset for which style ranges are requested
	 * @param length the length of text for which style ranges are requested
	 * @return style ranges for the specified region of this partitioner's document
	 * to use when rendering, or <code>null</code> if none
	 */
	StyleRange[] getStyleRanges(int offset, int length);
}
