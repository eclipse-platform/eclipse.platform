/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.ui.memory.AbstractMemoryRendering;
import org.eclipse.debug.ui.memory.IRepositionableMemoryRendering;

/**
 * Internal base class to allow AbstractTableRendering and AbstractAsyncTableRendering
 * to share actions and dialogs.  This abstract class is not to be published.
 *
 * @since 3.2
 */
public abstract class AbstractBaseTableRendering extends AbstractMemoryRendering implements IRepositionableMemoryRendering{

	public AbstractBaseTableRendering(String renderingId) {
		super(renderingId);
	}

	/**
	 * Format view tab based on the bytes per line and column.
	 *
	 * @param bytesPerLine - number of bytes per line, possible values: (1 / 2 / 4 / 8 / 16) * addressableSize
	 * @param columnSize - number of bytes per column, possible values: (1 / 2 / 4 / 8 / 16) * addressableSize
	 * @return true if format is successful, false, otherwise
	 */
	abstract public boolean format(int bytesPerLine, int columnSize);

	/**
	 * Returns the addressible size of this rendering's memory block in bytes.
	 *
	 * @return the addressible size of this rendering's memory block in bytes
	 */
	abstract public int getAddressableSize();

	/**
	 * Resize column to the preferred size.
	 */
	abstract public void resizeColumnsToPreferredSize();

	/**
	 * Returns the number of addressable units per row.
	 *
	 * @return number of addressable units per row
	 */
	abstract public int getAddressableUnitPerLine();

	/**
	 * Returns the number of addressable units per column.
	 *
	 * @return number of addressable units per column
	 */
	abstract public int getAddressableUnitPerColumn();

	/**
	 * Returns the number of bytes displayed in a single column cell.
	 *
	 * @return the number of bytes displayed in a single column cell
	 */
	abstract public int getBytesPerColumn();

	/**
	 * Returns the number of bytes displayed in a row.
	 *
	 * @return the number of bytes displayed in a row
	 */
	abstract public int getBytesPerLine();

	/**
	 * Updates labels of this rendering.
	 */
	abstract public void updateLabels();

	/**
	 * @return the label of this rendering
	 */
	@Override
	abstract public String getLabel();

	/**
	 * Refresh the table viewer with the current top visible address.
	 * Update labels in the memory rendering.
	 */
	abstract public void refresh();

	/**
	 * Moves the cursor to the specified address.
	 * Will load more memory if the address is not currently visible.
	 *
	 * @param address address to position cursor at
	 * @throws DebugException if an exception occurrs
	 */
	@Override
	abstract public void goToAddress(BigInteger address) throws DebugException;

	/**
	 * Returns the currently selected address in this rendering.
	 *
	 * @return the currently selected address in this rendering
	 */
	@Override
	abstract public BigInteger getSelectedAddress();

	/**
	 * Returns the currently selected content in this rendering as a String.
	 *
	 * @return the currently selected content in this rendering
	 */
	abstract public String getSelectedAsString();

	/**
	 * Returns the currently selected content in this rendering as MemoryByte.
	 *
	 * @return the currently selected content in array of MemoryByte.
	 * Returns an empty array if the selected address is out of buffered range.
	 */
	@Override
	abstract public MemoryByte[] getSelectedAsBytes();

	/**
	 * Returns the number of characters a byte will convert to
	 * or -1 if unknown.
	 *
	 * @return the number of characters a byte will convert to
	 *  or -1 if unknown
	 */
	abstract public int getNumCharsPerByte();

	/*
	 * @see org.eclipse.debug.ui.memory.IResettableMemoryRendering#resetRendering()
	 */
	abstract public void resetRendering() throws DebugException;

	/**
	 * Returns text for the given memory bytes at the specified address for the specified
	 * rendering type. This is called by the label provider for.
	 * Subclasses must override.
	 *
	 * @param renderingTypeId rendering type identifier
	 * @param address address where the bytes belong to
	 * @param data the bytes
	 * @return a string to represent the memory. Cannot not return <code>null</code>.
	 * 	Returns a string to pad the cell if the memory cannot be converted
	 *  successfully.
	 */
	abstract public String getString(String renderingTypeId, BigInteger address,
			MemoryByte[] data);

	/**
	 * Returns bytes for the given text corresponding to bytes at the given
	 * address for the specified rendering type. This is called by the cell modifier
	 * when modifying bytes in a memory block.
	 * Subclasses must convert the string value to an array of bytes.  The bytes will
	 * be passed to the debug adapter for memory block modification.
	 * Returns <code>null</code> if the bytes cannot be formatted properly.
	 *
	 * @param renderingTypeId rendering type identifier
	 * @param address address the bytes begin at
	 * @param currentValues current values of the data in bytes format
	 * @param newValue the string to be converted to bytes
	 * @return the bytes converted from a string
	 */
	abstract public byte[] getBytes(String renderingTypeId, BigInteger address,
			MemoryByte[] currentValues, String newValue);

}
