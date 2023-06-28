/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.memory.MemoryRenderingElement;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.TableItem;

/**
 * @since 3.1
 *
 */
// TODO:  if we want "true" flexible hierarchy, also need to allow clients
// to plug cell modifier in case the element is not MemorySegment
public class AsyncTableRenderingCellModifier implements ICellModifier {

	private AbstractAsyncTableRendering fRendering;
	private boolean fMBSupportsValueModification = false;

	private ICellModifier fCustomModifier;

	public AsyncTableRenderingCellModifier(AbstractAsyncTableRendering rendering, ICellModifier customModifier) {
		fRendering = rendering;
		fCustomModifier = customModifier;

		Job job = new Job("AsyncTableRenderingCellModifier"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				fMBSupportsValueModification = fRendering.getMemoryBlock().supportsValueModification();
				return Status.OK_STATUS;
			}};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public boolean canModify(Object element, String property) {
		boolean canModify = true;
		try {
			if (!(element instanceof MemorySegment)) {
				return false;
			}

			if (!isValueModificationSupported()) {
				return false;
			}

			MemorySegment line = (MemorySegment) element;
			if (TableRenderingLine.P_ADDRESS.equals(property)) {
				return false;
			}

			// property is stored as number of addressable unit away from the
			// line address
			// to calculate offset to the memory line array, offset =
			// numberofAddressableUnit * addressableSize
			int addressableSize = getAddressableSize();

			int offset = Integer.parseInt(property, 16) * addressableSize;

			MemoryByte[] bytes = line.getBytes(offset, fRendering.getBytesPerColumn());

			if (fCustomModifier != null)
			{
				BigInteger address = line.getAddress().add(BigInteger.valueOf(offset));
				MemoryRenderingElement mElement = new MemoryRenderingElement(fRendering, address, bytes);
				return fCustomModifier.canModify(mElement, null);
			}

			for (MemoryByte b : bytes) {
				if (!b.isWritable()) {
					canModify = false;
				}
			}
			return canModify;
		} catch (NumberFormatException e) {
			canModify = false;
			return canModify;
		}
	}

	/**
	 * @return the rendering addressable size
	 */
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

		if (!(element instanceof MemorySegment)) {
			return null;
		}

		MemorySegment line = (MemorySegment) element;
		try {
			if (TableRenderingLine.P_ADDRESS.equals(property)) {
				return line.getAddress();
			}

			int offsetToLineBuffer = Integer.parseInt(property, 16) * getAddressableSize();
			MemoryByte[] memory = line.getBytes(offsetToLineBuffer, fRendering.getBytesPerColumn());

			int offsetFromLineAddress = Integer.parseInt(property, 16);
			BigInteger address = line.getAddress().add(BigInteger.valueOf(offsetFromLineAddress));

			if (fCustomModifier != null)
			{
				MemoryRenderingElement mElement = new MemoryRenderingElement(fRendering, address, memory);
				return fCustomModifier.getValue(mElement, null);
			}

			// ask the rendering for a string representation of the bytes
			return fRendering.getString(fRendering.getRenderingId(), address, memory);

		} catch (NumberFormatException e) {
			return "00"; //$NON-NLS-1$
		}
	}

	@Override
	public void modify(Object element, final String property, final Object value) {

		MemorySegment segment = null;
		if (element instanceof TableItem) {
			Object data = ((TableItem)element).getData();
			if (data != null && data instanceof MemorySegment) {
				segment = (MemorySegment)data;
			}

		} else if (element instanceof MemorySegment){
			segment = (MemorySegment) element;
		}

		if (segment == null) {
			return;
		}

		final MemorySegment line = segment;

		Job job = new Job("Set Values"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// calculate offset to update
					final IMemoryBlock memoryBlk = fRendering.getMemoryBlock();

					// number of addressable units from the line's start address
					int offsetFromLineAddress = Integer.parseInt(property, 16);

					// this offset is number of addressable unit from memory block's base address
					final BigInteger offsetFromMBBase = getOffset(memoryBlk, line.getAddress(), offsetFromLineAddress);

					// property is number of addressable unit from line address
					// to calculate proper offset in the memoryViewLine's array
					// offset = numberOfAddressableUnit * addressableSize
					int offsetToLineBuffer = Integer.parseInt(property, 16) * getAddressableSize();

					MemoryByte[] oldArray = line.getBytes(offsetToLineBuffer, fRendering.getBytesPerColumn());

					// address is line address + addressable unit into the line
					BigInteger address = line.getAddress();
					address = address.add(BigInteger.valueOf(offsetFromLineAddress));

					if (fCustomModifier != null) {
						MemoryRenderingElement mElement = new MemoryRenderingElement(fRendering, address, oldArray);
						fCustomModifier.modify(mElement, null, value);
						return Status.OK_STATUS;
					}

					if (!(value instanceof String))
					{
						DebugUIPlugin.logErrorMessage("Cell modifier cannot handle non-string values."); //$NON-NLS-1$
						return Status.OK_STATUS;
					}

					byte[] bytes = null;
					String oldValue = (String) getValue(line, property);
					if (!oldValue.equals(value)) {
						bytes = fRendering.getBytes(fRendering.getRenderingId(), address, oldArray, (String) value);

						if (bytes == null) {
							return Status.OK_STATUS;
						}

						if (bytes.length == 0) {
							return Status.OK_STATUS;
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
								return Status.OK_STATUS;
							}
						}
					} else {
						// return if value has not changed
						return Status.OK_STATUS;
					}

					final byte[] newByteValues = bytes;

					if (memoryBlk instanceof IMemoryBlockExtension) {
						((IMemoryBlockExtension) memoryBlk).setValue(offsetFromMBBase, newByteValues);
					} else {
						memoryBlk.setValue(offsetFromMBBase.longValue(), newByteValues);
					}
				} catch (DebugException e) {
					MemoryViewUtil.openError(DebugUIMessages.MemoryViewCellModifier_failure_title, DebugUIMessages.MemoryViewCellModifier_failed, e);
				} catch (NumberFormatException e) {
					MemoryViewUtil.openError(DebugUIMessages.MemoryViewCellModifier_failure_title, DebugUIMessages.MemoryViewCellModifier_failed + "\n" + DebugUIMessages.MemoryViewCellModifier_data_is_invalid, null); //$NON-NLS-1$
				}
				return Status.OK_STATUS;
			}};

		job.setSystem(true);
		job.schedule();
	}

	private BigInteger getOffset(IMemoryBlock memory, BigInteger lineAddress, int lineOffset) throws DebugException {

		BigInteger memoryAddr;

		if (memory instanceof IMemoryBlockExtension) {
			memoryAddr = ((IMemoryBlockExtension) memory).getBigBaseAddress();
		} else {
			memoryAddr = BigInteger.valueOf(memory.getStartAddress());
		}

		if (memoryAddr == null) {
			memoryAddr = BigInteger.ZERO;
		}

		return lineAddress.subtract(memoryAddr).add(BigInteger.valueOf(lineOffset));
	}

	private boolean isValueModificationSupported()
	{
		return fMBSupportsValueModification;
	}

}
