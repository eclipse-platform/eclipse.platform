/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     WindRiver - Bug 192028 [Memory View] Memory view does not
 *                 display memory blocks that do not reference IDebugTarget
 *******************************************************************************/

package org.eclipse.debug.internal.ui.elements.adapters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.internal.ui.memory.provisional.MemoryViewPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AsynchronousContentAdapter;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.internal.ui.views.memory.renderings.MemorySegment;
import org.eclipse.debug.internal.ui.views.memory.renderings.TableRenderingContentDescriptor;
import org.eclipse.debug.ui.memory.IMemoryRendering;

public class MemoryBlockContentAdapter extends AsynchronousContentAdapter {

	@Override
	protected Object[] getChildren(Object parent, IPresentationContext context) throws CoreException {

		if (!(parent instanceof IMemoryBlock)) {
			return new Object[0];
		}

		if (!(context instanceof MemoryViewPresentationContext)) {
			return new Object[0];
		}

		MemoryViewPresentationContext memoryViewContext = (MemoryViewPresentationContext) context;
		IMemoryRendering rendering = memoryViewContext.getRendering();

		if (!(rendering instanceof AbstractAsyncTableRendering)) {
			return new Object[0];
		}

		try {
			return getMemoryFromMemoryBlock(memoryViewContext);
		} catch (DebugException e) {
			throw e;
		}

	}

	@Override
	protected boolean hasChildren(Object element, IPresentationContext context) throws CoreException {

		if (context instanceof MemoryViewPresentationContext) {
			if (((MemoryViewPresentationContext) context).getRendering() != null) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean supportsPartId(String id) {
		return true;
	}

	private Object[] getMemoryFromMemoryBlock(MemoryViewPresentationContext context) throws DebugException {
		IMemoryBlock memoryBlock = context.getRendering().getMemoryBlock();
		if (memoryBlock instanceof IMemoryBlockExtension) {
			return loadContentForExtendedMemoryBlock(context);
		}

		return loadContentForSimpleMemoryBlock(context);

	}

	/**
	 * @throws DebugException
	 */
	public Object[] loadContentForSimpleMemoryBlock(MemoryViewPresentationContext context) throws DebugException {
		AbstractAsyncTableRendering rendering = getTableRendering(context);
		if (rendering != null) {
			IMemoryBlock memoryBlock = rendering.getMemoryBlock();
			long startAddress = memoryBlock.getStartAddress();
			BigInteger address = BigInteger.valueOf(startAddress);
			long length = memoryBlock.getLength();
			long numLines = length / rendering.getBytesPerLine();
			return getMemoryToFitTable(address, numLines, context);
		}
		return EMPTY;
	}

	/**
	 * @throws DebugException
	 */
	public Object[] loadContentForExtendedMemoryBlock(MemoryViewPresentationContext context) throws DebugException {

		AbstractAsyncTableRendering rendering = getTableRendering(context);
		if (rendering != null) {
			TableRenderingContentDescriptor descriptor = rendering.getAdapter(TableRenderingContentDescriptor.class);

			if (descriptor == null) {
				return new Object[0];
			}

			if (descriptor.getNumLines() <= 0) {
				return new Object[0];
			}

			// calculate top buffered address
			BigInteger loadAddress = descriptor.getLoadAddress();
			if (loadAddress == null) {
				loadAddress = BigInteger.ZERO;
			}

			BigInteger mbStart = descriptor.getStartAddress();
			BigInteger mbEnd = descriptor.getEndAddress();

			// check that the load address is within range
			if (loadAddress.compareTo(mbStart) < 0 || loadAddress.compareTo(mbEnd) > 0) {
				// default load address to memory block base address
				loadAddress = ((IMemoryBlockExtension) descriptor.getMemoryBlock()).getBigBaseAddress();
				descriptor.setLoadAddress(loadAddress);
			}

			// if address is still out of range, throw an exception
			if (loadAddress.compareTo(mbStart) < 0 || loadAddress.compareTo(mbEnd) > 0) {
				throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_0 + loadAddress.toString(16), null));
			}

			int addressableUnitsPerLine = rendering.getAddressableUnitPerLine();
			BigInteger bufferStart = loadAddress.subtract(BigInteger.valueOf(descriptor.getPreBuffer() * addressableUnitsPerLine));
			BigInteger bufferEnd = loadAddress.add(BigInteger.valueOf(descriptor.getPostBuffer() * addressableUnitsPerLine));
			bufferEnd = bufferEnd.add(BigInteger.valueOf(descriptor.getNumLines() * addressableUnitsPerLine));

			// TODO: should rely on input to tell us what to load
			// instead of having the content adapter override the setting
			if (descriptor.isDynamicLoad()) {
				if (bufferStart.compareTo(mbStart) < 0) {
					bufferStart = mbStart;
				}

				if (bufferEnd.compareTo(mbEnd) > 0) {
					bufferEnd = mbEnd;

					int numLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue();
					if (numLines < descriptor.getNumLines()) {
						// re-calculate buffer start since we may not have
						// enough lines to popoulate the view
						bufferStart = bufferEnd.subtract(BigInteger.valueOf(descriptor.getNumLines() * addressableUnitsPerLine));
						bufferStart = bufferStart.subtract(BigInteger.valueOf(descriptor.getPreBuffer() * addressableUnitsPerLine));

						// if after adjusting buffer start, it goes before the
						// memory block start
						// address, adjust it back
						if (bufferStart.compareTo(mbStart) < 0) {
							bufferStart = mbStart;
						}
					}
				}

				// buffer end must be greater than buffer start
				if (bufferEnd.compareTo(bufferStart) <= 0) {
					throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_1, null));
				}

				int numLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue() + 1;
				// get stoarage to fit the memory view tab size
				return getMemoryToFitTable(bufferStart, numLines, context);
			} else {
				if (bufferStart.compareTo(mbStart) < 0) {
					bufferStart = mbStart;
				}

				if (bufferEnd.compareTo(mbEnd) > 0) {
					bufferStart = mbEnd.subtract(BigInteger.valueOf((descriptor.getNumLines() - 1) * addressableUnitsPerLine));
					bufferEnd = mbEnd;

					// after adjusting buffer start, check if it's smaller than
					// memory block's start address
					if (bufferStart.compareTo(mbStart) < 0) {
						bufferStart = mbStart;
					}
				}

				// buffer end must be greater than buffer start
				if (bufferEnd.compareTo(bufferStart) <= 0) {
					throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_2, null));
				}

				int numLines = descriptor.getNumLines();
				int bufferNumLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue() + 1;

				if (bufferNumLines < numLines) {
					numLines = bufferNumLines;
				}

				// get stoarage to fit the memory view tab size
				return getMemoryToFitTable(bufferStart, numLines, context);
			}
		}
		return EMPTY;
	}

	/**
	 * Get memory to fit table
	 *
	 * @param startingAddress
	 * @param numberOfLines
	 * @param updateDelta
	 * @throws DebugException
	 */
	public Object[] getMemoryToFitTable(BigInteger startAddress, long numberOfLines, MemoryViewPresentationContext context) throws DebugException {
		AbstractAsyncTableRendering tableRendering = getTableRendering(context);
		if (tableRendering == null) {
			DebugException e = new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.MemoryViewContentProvider_Unable_to_retrieve_content, null));
			throw e;
		}

		TableRenderingContentDescriptor descriptor = tableRendering.getAdapter(TableRenderingContentDescriptor.class);
		if (descriptor == null) {
			return new Object[0];
		}

		// do not ask for memory from memory block if the debug target
		// is already terminated
		IDebugTarget target = descriptor.getMemoryBlock().getDebugTarget();

		// check for null target to not calculate and retrieve memory for
		// standard debug model
		if (target != null && (target.isDisconnected() || target.isTerminated())) {
			return new Object[0];
		}

		DebugException dbgEvt = null;

		String adjustedAddress = startAddress.toString(16);

		// align to the closest boundary based on addressable size per line
		if (descriptor.isAlignAddressToBoundary() && descriptor.getMemoryBlock() instanceof IMemoryBlockExtension) {
			startAddress = MemoryViewUtil.alignToBoundary(startAddress, tableRendering.getAddressableUnitPerLine());
		}

		IMemoryBlockExtension extMemoryBlock = null;
		MemoryByte[] memoryBuffer = null;

		long reqNumBytes = 0;
		try {

			if (descriptor.getMemoryBlock() instanceof IMemoryBlockExtension) {
				reqNumBytes = tableRendering.getBytesPerLine() * numberOfLines;
				// get memory from memory block
				extMemoryBlock = (IMemoryBlockExtension) descriptor.getMemoryBlock();

				long reqNumberOfUnits = tableRendering.getAddressableUnitPerLine() * numberOfLines;

				memoryBuffer = extMemoryBlock.getBytesFromAddress(startAddress, reqNumberOfUnits);

				if (memoryBuffer == null) {
					DebugException e = new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.MemoryViewContentProvider_Unable_to_retrieve_content, null));
					throw e;
				}
			} else {
				// get memory from memory block
				byte[] memory = descriptor.getMemoryBlock().getBytes();

				if (memory == null) {
					DebugException e = new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.MemoryViewContentProvider_Unable_to_retrieve_content, null));
					throw e;
				}

				int prefillNumBytes = 0;

				// number of bytes need to prefill
				if (!startAddress.toString(16).endsWith("0")) //$NON-NLS-1$
				{
					adjustedAddress = startAddress.toString(16).substring(0, adjustedAddress.length() - 1);
					adjustedAddress += "0"; //$NON-NLS-1$
					BigInteger adjustedStart = new BigInteger(adjustedAddress, 16);
					prefillNumBytes = startAddress.subtract(adjustedStart).intValue();
					startAddress = adjustedStart;
				}
				reqNumBytes = descriptor.getMemoryBlock().getLength() + prefillNumBytes;

				// figure out number of dummy bytes to append
				while (reqNumBytes % tableRendering.getBytesPerLine() != 0) {
					reqNumBytes++;
				}

				numberOfLines = reqNumBytes / tableRendering.getBytesPerLine();

				// create memory byte for IMemoryBlock
				memoryBuffer = new MemoryByte[(int) reqNumBytes];

				// prefill buffer to ensure double-word alignment
				for (int i = 0; i < prefillNumBytes; i++) {
					MemoryByte tmp = new MemoryByte();
					tmp.setValue((byte) 0);
					tmp.setWritable(false);
					tmp.setReadable(false);
					tmp.setEndianessKnown(false);
					memoryBuffer[i] = tmp;
				}

				// fill buffer with memory returned by debug adapter
				int j = prefillNumBytes; // counter for memoryBuffer
				for (byte element : memory) {
					MemoryByte tmp = new MemoryByte();
					tmp.setValue(element);
					tmp.setReadable(true);
					tmp.setWritable(true);
					tmp.setEndianessKnown(false);
					memoryBuffer[j] = tmp;
					j++;
				}

				// append to buffer to fill up the entire line
				for (int i = j; i < memoryBuffer.length; i++) {
					MemoryByte tmp = new MemoryByte();
					tmp.setValue((byte) 0);
					tmp.setWritable(false);
					tmp.setReadable(false);
					tmp.setEndianessKnown(false);
					memoryBuffer[i] = tmp;
				}
			}
		} catch (DebugException e) {
			memoryBuffer = makeDummyContent(numberOfLines, tableRendering.getBytesPerLine());

			// finish creating the content provider before throwing an event
			dbgEvt = e;
		} catch (Throwable e) {
			// catch all errors from this process just to be safe
			memoryBuffer = makeDummyContent(numberOfLines, tableRendering.getBytesPerLine());

			// finish creating the content provider before throwing an event
			dbgEvt = new DebugException(DebugUIPlugin.newErrorStatus(e.getMessage(), e));
		}

		// if debug adapter did not return enough memory, create dummy memory
		if (memoryBuffer.length < reqNumBytes) {
			ArrayList<MemoryByte> newBuffer = new ArrayList<>();

			Collections.addAll(newBuffer, memoryBuffer);

			for (int i = memoryBuffer.length; i < reqNumBytes; i++) {
				MemoryByte mb = new MemoryByte();
				mb.setReadable(false);
				mb.setWritable(false);
				mb.setEndianessKnown(false);
				newBuffer.add(mb);
			}

			memoryBuffer = newBuffer.toArray(new MemoryByte[newBuffer.size()]);

		}

		boolean manageDelta = true;

		// If change information is not managed by the memory block
		// The view tab will manage it and calculate delta information
		// for its content cache.
		if (descriptor.getMemoryBlock() instanceof IMemoryBlockExtension) {
			manageDelta = !((IMemoryBlockExtension) descriptor.getMemoryBlock()).supportsChangeManagement();
		}

		if (dbgEvt != null) {
			throw dbgEvt;
		}

		// put memory information into MemoryViewLine
		return organizeLines(numberOfLines, memoryBuffer, startAddress, manageDelta, context);

	}

	private Object[] organizeLines(long numberOfLines, MemoryByte[] memoryBuffer, BigInteger address, boolean manageDelta, MemoryViewPresentationContext context) {
		ArrayList<MemorySegment> lineCache = new ArrayList<>();
		IMemoryRendering rendering = context.getRendering();
		if (!(rendering instanceof AbstractAsyncTableRendering)) {
			return lineCache.toArray();
		}

		AbstractAsyncTableRendering tableRendering = (AbstractAsyncTableRendering) rendering;
		int addressableUnit = tableRendering.getBytesPerLine() / tableRendering.getAddressableSize();

		for (int i = 0; i < numberOfLines; i++) {
			int bytesPerLine = tableRendering.getBytesPerLine();
			MemoryByte[] memory = new MemoryByte[bytesPerLine];

			// counter for memory, starts from 0 to number of bytes per line
			int k = 0;
			// j is the counter for memArray, memory returned by debug adapter
			for (int j = i * bytesPerLine; j < i * bytesPerLine + bytesPerLine; j++) {

				byte changeFlag = memoryBuffer[j].getFlags();
				if (manageDelta) {
					// turn off both change and known bits to make sure that
					// the change bits returned by debug adapters do not take
					// any effect

					changeFlag |= MemoryByte.HISTORY_KNOWN;
					changeFlag ^= MemoryByte.HISTORY_KNOWN;

					changeFlag |= MemoryByte.CHANGED;
					changeFlag ^= MemoryByte.CHANGED;
				}

				MemoryByte newByteObj = new MemoryByte(memoryBuffer[j].getValue(), changeFlag);
				memory[k] = newByteObj;
				k++;
			}

			MemorySegment newLine = new MemorySegment(address, memory, addressableUnit);
			lineCache.add(newLine);
			address = address.add(BigInteger.valueOf(addressableUnit));
		}
		return lineCache.toArray();
	}

	/**
	 * @param numberOfLines
	 * @return an array of dummy MemoryByte
	 */
	private MemoryByte[] makeDummyContent(long numberOfLines, int bytesPerLine) {
		MemoryByte[] memoryBuffer;
		// make up dummy memory, needed for recovery in case the debug adapter
		// is capable of retrieving memory again

		int numBytes = (int) (bytesPerLine * numberOfLines);
		memoryBuffer = new MemoryByte[numBytes];

		for (int i = 0; i < memoryBuffer.length; i++) {
			memoryBuffer[i] = new MemoryByte();
			memoryBuffer[i].setValue((byte) 0);
			memoryBuffer[i].setWritable(false);
			memoryBuffer[i].setReadable(false);
			memoryBuffer[i].setEndianessKnown(false);
		}
		return memoryBuffer;
	}

	protected AbstractAsyncTableRendering getTableRendering(MemoryViewPresentationContext context) {
		IMemoryRendering memRendering = context.getRendering();
		if (memRendering != null && memRendering instanceof AbstractAsyncTableRendering) {
			return (AbstractAsyncTableRendering) memRendering;
		}
		return null;
	}
}
