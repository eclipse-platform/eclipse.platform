/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

/**
 * @since 3.1
 */
public class TableRenderingCellModifier implements ICellModifier {

	private boolean editActionInvoked = false;

	private final AbstractTableRendering fRendering;

	public TableRenderingCellModifier(AbstractTableRendering rendering) {
		fRendering = rendering;
	}

	@Override
	public boolean canModify(Object element, String property) {
		boolean canModify = true;
		try {
			if (!(element instanceof TableRenderingLine)) {
				return false;
			}

			if (!editActionInvoked) {
				return false;
			}

			if (fRendering == null) {
				return false;
			}

			if (!fRendering.getMemoryBlock().supportsValueModification()) {
				return false;
			}

			TableRenderingLine line = (TableRenderingLine) element;
			if (TableRenderingLine.P_ADDRESS.equals(property)) {
				return false;
			}

			// property is stored as number of addressable unit away from the
			// line address
			// to calculate offset to the memory line array, offset =
			// numberofAddressableUnit * addressableSize
			int addressableSize = getAddressableSize();

			int offset = Integer.parseInt(property, 16) * addressableSize;
			int end = offset + fRendering.getBytesPerColumn();

			for (int i = offset; i < end; i++) {
				MemoryByte oneByte = line.getByte(i);

				if (!oneByte.isWritable()) {
					canModify = false;
				}
			}
			return canModify;
		} catch (NumberFormatException e) {
			canModify = false;
			return canModify;
		}
	}

	private int getAddressableSize() {
		int addressableSize = fRendering.getAddressableSize();
		if (addressableSize < 1) {
			addressableSize = 1;
		}
		return addressableSize;
	}

	@Override
	public Object getValue(Object element, String property) {
		// give back the value of the column

		if (!(element instanceof TableRenderingLine)) {
			return null;
		}

		TableRenderingLine line = (TableRenderingLine) element;
		try {
			if (TableRenderingLine.P_ADDRESS.equals(property)) {
				return line.getAddress();
			}

			int offset = Integer.parseInt(property, 16) * getAddressableSize();
			int end = offset + fRendering.getBytesPerColumn();

			// Ask for label provider
			MemoryByte[] memory = line.getBytes(offset, end);

			if (line.isAvailable(offset, end)) {
				// ask the renderer for a string representation of the bytes
				offset = Integer.parseInt(property, 16);

				BigInteger address = new BigInteger(((TableRenderingLine) element).getAddress(), 16);
				address = address.add(BigInteger.valueOf(offset));

				return fRendering.getString(fRendering.getRenderingId(), address, memory);
			}
			// if the range is not available, just return padded string
			return getPaddedString(offset, end);
		} catch (NumberFormatException e) {
			return "00"; //$NON-NLS-1$
		}
	}

	@Override
	public void modify(Object element, String property, Object value) {
		TableRenderingLine line = null;
		if (!(element instanceof TableRenderingLine)) {
			line = (TableRenderingLine) ((TableItem) element).getData();
		} else {
			line = (TableRenderingLine) element;
		}

		// validate data
		if (!(value instanceof String)) {
			return;
		}

		try {
			// calculate offset to update
			IMemoryBlock memoryBlk = fRendering.getMemoryBlock();

			int lineOffset = Integer.parseInt(property, 16);

			// this offset is number of addressable unit from the line address
			BigInteger offset = getOffset(memoryBlk, line.getAddress(), lineOffset);

			byte[] bytes = null;

			String oldValue = (String) getValue(line, property);

			if (!oldValue.equals(value)) {

				// property is number of addressable unit from line address
				// to calculate proper offset in the memoryViewLine's array
				// offset = numberOfAddressableUnit * addressableSize
				int offsetToLine = Integer.parseInt(property, 16) * getAddressableSize();
				int end = offsetToLine + fRendering.getBytesPerColumn();

				MemoryByte[] oldArray = line.getBytes(offsetToLine, end);

				BigInteger address = new BigInteger(line.getAddress(), 16);
				address = address.add(BigInteger.valueOf(offsetToLine));

				bytes = fRendering.getBytes(fRendering.getRenderingId(), address, oldArray, (String) value);

				if (bytes == null) {
					return;
				}

				if (bytes.length == 0) {
					return;
				}

				if (bytes.length <= oldArray.length) {
					boolean changed = false;
					// check that the bytes returned has actually changed
					for (int i = 0; i < bytes.length; i++) {
						if (bytes[i] != oldArray[i].getValue()) {
							changed = true;
							break;
						}
					}
					if (!changed) {
						return;
					}
				}
			} else {
				// return if value has not changed
				return;
			}

			if (memoryBlk instanceof IMemoryBlockExtension) {
				((IMemoryBlockExtension) memoryBlk).setValue(offset, bytes);
			} else {
				memoryBlk.setValue(offset.longValue(), bytes);
			}
		} catch (DebugException e) {
			MemoryViewUtil.openError(DebugUIMessages.MemoryViewCellModifier_failure_title, DebugUIMessages.MemoryViewCellModifier_failed, e);
		} catch (NumberFormatException e) {
			MemoryViewUtil.openError(DebugUIMessages.MemoryViewCellModifier_failure_title, DebugUIMessages.MemoryViewCellModifier_failed + "\n" + DebugUIMessages.MemoryViewCellModifier_data_is_invalid, null); //$NON-NLS-1$
		}

	}

	private BigInteger getOffset(IMemoryBlock memory, String lineAddress, int lineOffset) throws DebugException {

		BigInteger lineAddr = new BigInteger(lineAddress, 16);
		BigInteger memoryAddr;

		if (memory instanceof IMemoryBlockExtension) {
			memoryAddr = ((IMemoryBlockExtension) memory).getBigBaseAddress();
		} else {
			memoryAddr = BigInteger.valueOf(memory.getStartAddress());
		}

		if (memoryAddr == null) {
			memoryAddr = BigInteger.ZERO;
		}

		return lineAddr.subtract(memoryAddr).add(BigInteger.valueOf(lineOffset));
	}

	/**
	 * @param editActionInvoked
	 *            The editActionInvoked to set.
	 */
	public void setEditActionInvoked(boolean editActionInvoked) {
		this.editActionInvoked = editActionInvoked;
	}

	/**
	 * @return padded string
	 */
	public String getPaddedString(int start, int end) {
		StringBuilder buf = new StringBuilder();
		String paddedStr = DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);
		for (int i = start; i < end; i++) {
			buf.append(paddedStr);
		}
		return buf.toString();
	}

}
