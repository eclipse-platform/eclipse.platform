/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.console;

import org.eclipse.jface.text.ITypedRegion;

/**
 * Extension interface for {@link IConsoleDocumentPartitioner}.
 * <p>
 * It adds more possibilities to query read-only regions of the partitioned
 * document.
 * </p>
 *
 * @see org.eclipse.ui.console.IConsoleDocumentPartitioner
 * @since 3.9
 */
public interface IConsoleDocumentPartitionerExtension {

	/**
	 * Returns all partitions which are read-only.
	 *
	 * @return all read-only partitions. Ordered by offset and never
	 *         <code>null</code>.
	 */
	ITypedRegion[] computeReadOnlyPartitions();

	/**
	 * Returns all read-only partitions in given range.
	 *
	 * @param offset the offset of the range of interest
	 * @param length the length of the range of interest
	 * @return read-only partitions in given range. Ordered by offset and never
	 *         <code>null</code>. Returned regions may start and/or end outside
	 *         given range.
	 */
	ITypedRegion[] computeReadOnlyPartitions(int offset, int length);

	/**
	 * Returns all partitions which are writable.
	 *
	 * @return all writable partitions. Ordered by offset and never
	 *         <code>null</code>.
	 */
	ITypedRegion[] computeWritablePartitions();

	/**
	 * Returns all writable partitions in given range.
	 *
	 * @param offset the offset of the range of interest
	 * @param length the length of the range of interest
	 * @return writable partitions in given range. Ordered by offset and never
	 *         <code>null</code>. Returned regions may start and/or end outside
	 *         given range.
	 */
	ITypedRegion[] computeWritablePartitions(int offset, int length);

	/**
	 * Returns whether this partitioner's document is read-only in the specified
	 * range. Only returns <code>true</code> if the whole range is read-only.
	 *
	 * @param offset document offset
	 * @param length range length
	 * @return whether this partitioner's document is read-only in the specific
	 *         range
	 */
	boolean isReadOnly(int offset, int length);

	/**
	 * Returns whether this partitioner's document is read-only at any point in the
	 * specified range.
	 *
	 * @param offset document offset
	 * @param length range length
	 * @return returns <code>true</code> if any offset in the given range is
	 *         read-only
	 */
	boolean containsReadOnly(int offset, int length);

	/**
	 * Get this offset or the nearest offset before which is, depending on argument,
	 * writable or read-only.
	 *
	 * @param offset         the offset of interest
	 * @param searchWritable if <code>true</code> return the nearest writable
	 *                       offset. If <code>false</code> return the nearest
	 *                       read-only offset.
	 * @return the given offset if it has the requested read-only/writable state or
	 *         the nearest offset before with requested state. Returns
	 *         <code>-1</code> if there is no offset with requested state before
	 *         requested offset.
	 */
	int getPreviousOffsetByState(int offset, boolean searchWritable);

	/**
	 * Get this offset or the nearest offset after which is, depending on argument,
	 * writable or read-only.
	 *
	 * @param offset         the offset of interest
	 * @param searchWritable if <code>true</code> return the nearest writable
	 *                       offset. If <code>false</code> return the nearest
	 *                       read-only offset.
	 * @return the given offset if it has the requested read-only/writable state or
	 *         the nearest offset after with requested state. Returns the document
	 *         length if there is no offset with requested state after requested
	 *         offset.
	 */
	int getNextOffsetByState(int offset, boolean searchWritable);
}
