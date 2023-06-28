/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.memory.IMemoryRenderingUpdater;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content Provider used by AbstractTableRendering
 *
 * @since 3.0
 */
public class TableRenderingContentProvider extends BasicDebugViewContentProvider {

	// lines currently being displayed by the table rendering
	protected Vector<TableRenderingLine> lineCache;

	// Cache to allow the content provider to comppute change information
	// Cache is taken by copying the lineCache after a suspend event
	// or change event from the the memory block.
	protected Hashtable<String, TableRenderingLine> contentCache;

	// cache in the form of MemoryByte
	// needed for reorganizing cache when the row size changes
	private MemoryByte[] fContentCacheInBytes;
	private String fContentCacheStartAddress;

	private BigInteger fBufferTopAddress;

	private TableRenderingContentInput fInput;
	private BigInteger fBufferEndAddress;

	private boolean fDynamicLoad;

	/**
	 * @param memoryBlock
	 * @param newTab
	 */
	public TableRenderingContentProvider()
	{
		lineCache = new Vector<>();
		contentCache = new Hashtable<>();
		initializeDynamicLoad();

		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	/**
	 * @param viewer
	 */
	public void setViewer(StructuredViewer viewer)
	{
		fViewer = viewer;
	}

	@Override
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		try {
			if (newInput instanceof TableRenderingContentInput)
			{
				fInput = (TableRenderingContentInput)newInput;
				if (fInput.getMemoryBlock() instanceof IMemoryBlockExtension) {
					loadContentForExtendedMemoryBlock();
				} else {
					loadContentForSimpleMemoryBlock();
				}

				// tell rendering to display table if the loading is successful
				getTableRendering(fInput).displayTable();
			}
		} catch (DebugException e) {
			getTableRendering(fInput).displayError(e);
		}
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
		super.dispose();
	}

	@Override
	public Object[] getElements(Object parent) {

		// if cache is empty, get memory
		if (lineCache.isEmpty()) {

			try {
				getMemoryFromMemoryBlock();
			} catch (DebugException e) {
				DebugUIPlugin.log(e.getStatus());
				getTableRendering(fInput).displayError(e);
				return lineCache.toArray();
			}
		}

		if (lineCache.isEmpty()) {
			return lineCache.toArray();
		}

		// check to see if the row size has changed
		TableRenderingLine line = lineCache.get(0);
		int currentRowSize = line.getByteArray().length;
		int renderingRowSize = getTableRendering(fInput).getBytesPerLine();

		if (renderingRowSize != currentRowSize)
		{
			try {
				reorganizeContentCache(renderingRowSize);
				reorganizeLines(lineCache, renderingRowSize);
			} catch (DebugException e) {
				DebugUIPlugin.log(e.getStatus());
				getTableRendering(fInput).displayError(e);
				return lineCache.toArray();
			}
		}
		return lineCache.toArray();
	}

	private void getMemoryFromMemoryBlock() throws DebugException {
		IMemoryBlock memoryBlock = fInput.getMemoryBlock();
		if (memoryBlock instanceof IMemoryBlockExtension)
		{
			loadContentForExtendedMemoryBlock();
			getTableRendering(fInput).displayTable();
		}
		else
		{
			loadContentForSimpleMemoryBlock();
			getTableRendering(fInput).displayTable();
		}
	}

	/**
	 * @throws DebugException
	 */
	public void loadContentForSimpleMemoryBlock() throws DebugException {
		// get as much memory as the memory block can handle
		fInput.setPreBuffer(0);
		fInput.setPostBuffer(0);
		long startAddress = fInput.getMemoryBlock().getStartAddress();
		BigInteger address = BigInteger.valueOf(startAddress);
		long length = fInput.getMemoryBlock().getLength();
		long numLines = length / getTableRendering(fInput).getBytesPerLine();
		getMemoryToFitTable(address, numLines, fInput.isUpdateDelta());
	}

	/**
	 * @throws DebugException
	 */
	public void loadContentForExtendedMemoryBlock() throws DebugException {

		// do not load if number of lines needed is < 0
		if (fInput.getNumLines() <= 0) {
			return;
		}

		// calculate top buffered address
		BigInteger loadAddress = fInput.getLoadAddress();
		if (loadAddress == null) {
			loadAddress = BigInteger.ZERO;
		}

		BigInteger mbStart = fInput.getStartAddress();
		BigInteger mbEnd = fInput.getEndAddress();

		// check that the load address is within range
		if (loadAddress.compareTo(mbStart) < 0 || loadAddress.compareTo(mbEnd) > 0)
		{
			// default load address to memory block base address
			loadAddress = ((IMemoryBlockExtension)getMemoryBlock()).getBigBaseAddress();
			fInput.setLoadAddress(loadAddress);
		}

		// if address is still out of range, throw an exception
		if (loadAddress.compareTo(mbStart) < 0 || loadAddress.compareTo(mbEnd) > 0)
		{
			throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_0 + loadAddress.toString(16), null));
		}

		int addressableUnitsPerLine = getTableRendering(fInput).getAddressableUnitPerLine();
		BigInteger bufferStart = loadAddress.subtract(BigInteger.valueOf(fInput.getPreBuffer()*addressableUnitsPerLine));
		BigInteger bufferEnd = loadAddress.add(BigInteger.valueOf(fInput.getPostBuffer()*addressableUnitsPerLine));
		bufferEnd = bufferEnd.add(BigInteger.valueOf(fInput.getNumLines()*addressableUnitsPerLine));

		if (isDynamicLoad())
		{
			if (bufferStart.compareTo(mbStart) < 0) {
				bufferStart = mbStart;
			}

			if (bufferEnd.compareTo(mbEnd) > 0)
			{
				bufferEnd = mbEnd;

				int numLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue();
				if (numLines < fInput.getNumLines())
				{
					// re-calculate buffer start since we may not have enough lines to popoulate the view
					bufferStart = bufferEnd.subtract(BigInteger.valueOf(fInput.getNumLines()*addressableUnitsPerLine));
					bufferStart = bufferStart.subtract(BigInteger.valueOf(fInput.getPreBuffer()*addressableUnitsPerLine));
				}

				// if after adjusting buffer start, it goes before the memory block start
				// address, adjust it back
				if (bufferStart.compareTo(mbStart) < 0) {
					bufferStart = mbStart;
				}
			}

			// buffer end must be greater than buffer start
			if (bufferEnd.compareTo(bufferStart) <= 0) {
				throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_1, null));
			}

			int numLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue()+1;
			// get stoarage to fit the memory view tab size
			getMemoryToFitTable(bufferStart, numLines, fInput.isUpdateDelta());
		}
		else
		{
			if (bufferStart.compareTo(mbStart) < 0) {
				bufferStart = mbStart;
			}

			if (bufferEnd.compareTo(mbEnd) > 0)
			{
				bufferStart = mbEnd.subtract(BigInteger.valueOf((fInput.getNumLines()-1)*addressableUnitsPerLine));
				bufferEnd = mbEnd;

				// after adjusting buffer start, check if it's smaller than memory block's start address
				if (bufferStart.compareTo(mbStart) < 0) {
					bufferStart = mbStart;
				}
			}

			// buffer end must be greater than buffer start
			if (bufferEnd.compareTo(bufferStart) <= 0) {
				throw new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.TableRenderingContentProvider_2, null));
			}

			int numLines = fInput.getNumLines();
			int bufferNumLines = bufferEnd.subtract(bufferStart).divide(BigInteger.valueOf(addressableUnitsPerLine)).intValue()+1;

			if (bufferNumLines < numLines) {
				numLines = bufferNumLines;
			}

			// get stoarage to fit the memory view tab size
			getMemoryToFitTable(bufferStart, numLines, fInput.isUpdateDelta());
		}
	}

	/**
	 * @return the memroy block
	 */
	public IMemoryBlock getMemoryBlock() {
		return fInput.getMemoryBlock();
	}

	/**
	 * Get memory to fit table
	 * @param startingAddress
	 * @param numberOfLines
	 * @param updateDelta
	 * @throws DebugException
	 */
	public void getMemoryToFitTable(BigInteger startingAddress, long numberOfLines, boolean updateDelta) throws DebugException
	{
		// do not ask for memory from memory block if the debug target
		// is already terminated
		IDebugTarget target = fInput.getMemoryBlock().getDebugTarget();

		if (target.isDisconnected() || target.isTerminated()) {
			return;
		}

		DebugException dbgEvt = null;

		// calculate address size
		String adjustedAddress = startingAddress.toString(16);

		int addressSize;
		try {
			addressSize = getAddressSize(startingAddress);
		} catch (DebugException e1) {
			dbgEvt = e1;
			addressSize = 4;
		}

		int addressLength = addressSize * IInternalDebugUIConstants.CHAR_PER_BYTE;

		// align to the closest boundary based on addressable size per line
		if ( getMemoryBlock() instanceof IMemoryBlockExtension)
		{
			startingAddress = MemoryViewUtil.alignToBoundary(startingAddress, getTableRendering(fInput).getAddressableUnitPerLine());
		}

		IMemoryBlockExtension extMemoryBlock = null;
		MemoryByte[] memoryBuffer = null;

		String paddedString = DebugUIPlugin.getDefault().getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);

		long reqNumBytes = 0;
		try
		{
			if (fInput.getMemoryBlock() instanceof IMemoryBlockExtension)
			{
				reqNumBytes = getTableRendering(fInput).getBytesPerLine() * numberOfLines;
				// get memory from memory block
				extMemoryBlock = (IMemoryBlockExtension) fInput.getMemoryBlock();

				long reqNumberOfUnits = getTableRendering(fInput).getAddressableUnitPerLine() * numberOfLines;

				memoryBuffer =	extMemoryBlock.getBytesFromAddress(startingAddress,	reqNumberOfUnits);

				if(memoryBuffer == null)
				{
					DebugException e = new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.MemoryViewContentProvider_Unable_to_retrieve_content, null));
					throw e;
				}
			}
			else
			{
				// get memory from memory block
				byte[] memory = fInput.getMemoryBlock().getBytes();

				if (memory == null)
				{
					DebugException e = new DebugException(DebugUIPlugin.newErrorStatus(DebugUIMessages.MemoryViewContentProvider_Unable_to_retrieve_content, null));
					throw e;
				}

				int prefillNumBytes = 0;

				// number of bytes need to prefill
				if (!startingAddress.toString(16).endsWith("0")) //$NON-NLS-1$
				{
					adjustedAddress = startingAddress.toString(16).substring(0, adjustedAddress.length() - 1);
					adjustedAddress += "0"; //$NON-NLS-1$
					BigInteger adjustedStart = new BigInteger(adjustedAddress, 16);
					prefillNumBytes = startingAddress.subtract(adjustedStart).intValue();
					startingAddress = adjustedStart;
				}
				reqNumBytes = fInput.getMemoryBlock().getLength() + prefillNumBytes;

				// figure out number of dummy bytes to append
				while (reqNumBytes % getTableRendering(fInput).getBytesPerLine() != 0)
				{
					reqNumBytes ++;
				}

				numberOfLines = reqNumBytes / getTableRendering(fInput).getBytesPerLine();

				// create memory byte for IMemoryBlock
				memoryBuffer = new MemoryByte[(int)reqNumBytes];

				// prefill buffer to ensure double-word alignment
				for (int i=0; i<prefillNumBytes; i++)
				{
					MemoryByte tmp = new MemoryByte();
					tmp.setValue((byte)0);
					tmp.setWritable(false);
					tmp.setReadable(false);
					tmp.setEndianessKnown(false);
					memoryBuffer[i] = tmp;
				}

				// fill buffer with memory returned by debug adapter
				int j = prefillNumBytes; 							// counter for memoryBuffer
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
				for (int i=j; i<memoryBuffer.length; i++)
				{
					MemoryByte tmp = new MemoryByte();
					tmp.setValue((byte)0);
					tmp.setWritable(false);
					tmp.setReadable(false);
					tmp.setEndianessKnown(false);
					memoryBuffer[i] = tmp;
				}
			}
		}
		catch (DebugException e)
		{
			memoryBuffer = makeDummyContent(numberOfLines);

			// finish creating the content provider before throwing an event
			dbgEvt = e;
		}
		catch (Throwable e)
		{
			// catch all errors from this process just to be safe
			memoryBuffer = makeDummyContent(numberOfLines);

			// finish creating the content provider before throwing an event
			dbgEvt = new DebugException(DebugUIPlugin.newErrorStatus(e.getMessage(), e));
		}

		// if debug adapter did not return enough memory, create dummy memory
		if (memoryBuffer.length < reqNumBytes)
		{
			ArrayList<MemoryByte> newBuffer = new ArrayList<>();

			Collections.addAll(newBuffer, memoryBuffer);

			for (int i=memoryBuffer.length; i<reqNumBytes; i++)
			{
				MemoryByte mb = new MemoryByte();
				mb.setReadable(false);
				mb.setWritable(false);
				mb.setEndianessKnown(false);
				newBuffer.add(mb);
			}

			memoryBuffer = newBuffer.toArray(new MemoryByte[newBuffer.size()]);

		}

		// clear line cache
		if (!lineCache.isEmpty())
		{
			lineCache.clear();
		}
		String address = startingAddress.toString(16);
		// save address of the top of buffer
		fBufferTopAddress = startingAddress;

		boolean manageDelta = true;

		// If change information is not managed by the memory block
		// The view tab will manage it and calculate delta information
		// for its content cache.
		if (fInput.getMemoryBlock() instanceof IMemoryBlockExtension)
		{
			manageDelta = !((IMemoryBlockExtension)fInput.getMemoryBlock()).supportsChangeManagement();
		}

		// put memory information into MemoryViewLine
		organizeLines(numberOfLines, updateDelta, addressLength, memoryBuffer, paddedString, address, manageDelta);

		if (dbgEvt != null){
			throw dbgEvt;
		}
	}

	private void organizeLines(long numberOfLines, boolean updateDelta, int addressLength, MemoryByte[] memoryBuffer, String paddedString, String address, boolean manageDelta)
	{
		for (int i = 0; i < numberOfLines; i++)
		{   //chop the raw memory up
			String tmpAddress = address.toUpperCase();
			if (tmpAddress.length() < addressLength)
			{
				while  (tmpAddress.length() < addressLength)
				{
					tmpAddress = "0" + tmpAddress; //$NON-NLS-1$
				}
			}
			int bytesPerLine = getTableRendering(fInput).getBytesPerLine();
			MemoryByte[] memory = new MemoryByte[bytesPerLine];
			boolean isMonitored = true;

			// counter for memory, starts from 0 to number of bytes per line
			int k = 0;
			// j is the counter for memArray, memory returned by debug adapter
			for (int j = i * bytesPerLine;
				j < i * bytesPerLine + bytesPerLine;
				j++)
			{

				byte changeFlag = memoryBuffer[j].getFlags();
				if (manageDelta)
				{
					// turn off both change and known bits to make sure that
					// the change bits returned by debug adapters do not take
					// any effect

					changeFlag |= MemoryByte.HISTORY_KNOWN;
					changeFlag ^= MemoryByte.HISTORY_KNOWN;

					changeFlag |= MemoryByte.CHANGED;
					changeFlag ^= MemoryByte.CHANGED;
				}

				MemoryByte newByteObj = new MemoryByte(memoryBuffer[j].getValue(), changeFlag);
				memory[k] =  newByteObj;
				k++;


				if (!manageDelta)
				{
					// If the byte is marked as unknown, the line is not monitored
					if (!memoryBuffer[j].isHistoryKnown())
					{
						isMonitored = false;
					}
				}
			}

			TableRenderingLine newLine = new TableRenderingLine(tmpAddress, memory, lineCache.size(), paddedString);

			TableRenderingLine oldLine = contentCache.get(newLine.getAddress());

			if (manageDelta)
			{
				if (oldLine != null) {
					newLine.isMonitored = true;
				} else {
					newLine.isMonitored = false;
				}
			}
			else
			{
				// check the byte for information
				newLine.isMonitored = isMonitored;
			}

			// calculate delta info for the memory view line
			if (manageDelta && !getTableRendering(fInput).isDisplayingError())
			{
				if (updateDelta)
				{
					if (oldLine != null)
					{
						newLine.markDeltas(oldLine);
					}
				}
				else
				{
					if (oldLine != null)
					{
						// deltas can only be reused if the line has not been changed
						// otherwise, force a refresh
						if (newLine.isLineChanged(oldLine))
						{
							newLine.markDeltas(oldLine);
						}
						else
						{
							newLine.copyDeltas(oldLine);
						}
					}
				}
			}
			else if (manageDelta && getTableRendering(fInput).isDisplayingError())
			{
				// show as unmonitored if the view tab is previoulsy displaying error
				newLine.isMonitored = false;
			}
			lineCache.add(newLine);


			// increment row address
			BigInteger bigInt = new BigInteger(address, 16);
			fBufferEndAddress = bigInt;
			int addressableUnit = getTableRendering(fInput).getBytesPerLine()/getTableRendering(fInput).getAddressableSize();
			address = bigInt.add(BigInteger.valueOf(addressableUnit)).toString(16);
		}
	}

	/**
	 * @param numberOfLines
	 * @return an array of dummy MemoryByte
	 */
	private MemoryByte[] makeDummyContent(long numberOfLines) {
		MemoryByte[] memoryBuffer;
		// make up dummy memory, needed for recovery in case the debug adapter
		// is capable of retrieving memory again

		int numBytes = (int)(getTableRendering(fInput).getBytesPerLine() * numberOfLines);
		memoryBuffer = new MemoryByte[numBytes];

		for (int i=0; i<memoryBuffer.length; i++){
			memoryBuffer[i] = new MemoryByte();
			memoryBuffer[i].setValue((byte)0);
			memoryBuffer[i].setWritable(false);
			memoryBuffer[i].setReadable(false);
			memoryBuffer[i].setEndianessKnown(false);
		}
		return memoryBuffer;
	}

	@Override
	protected void doHandleDebugEvent(DebugEvent event) {

		if (getTableRendering(fInput).isVisible())
		{
			// only do this if it's visible
			// still need to clear content cache if the rendering
			// is not visible
			if (isUpdateManagedByMB()) {
				return;
			}
		}

		// do nothing if the debug event did not come from a debug element comes from non-debug element
		if (!(event.getSource() instanceof IDebugElement)) {
			return;
		}

		// do not try to recover if the content input has not been created
		if (fInput == null) {
			return;
		}

		IDebugElement src = (IDebugElement)event.getSource();

		// if a debug event happens from the memory block
		// invoke contentChanged to get content of the memory block updated
		if (event.getKind() == DebugEvent.CHANGE && event.getSource() == fInput.getMemoryBlock())
		{
			if (event.getDetail() == DebugEvent.STATE){
				getTableRendering(fInput).updateLabels();
			}
			else
			{
				updateContent();
			}
		}

		// if the suspend evnet happens from the debug target that the
		// memory block belongs to
		if (event.getKind() == DebugEvent.SUSPEND && src.getDebugTarget() == fInput.getMemoryBlock().getDebugTarget())
		{
			updateContent();
		}

	}

	/**
	 * Update content of the view tab if the content of the memory block has changed
	 * or if its base address has changed
	 * Update will not be performed if the memory block has not been changed or
	 * if the rendering is not visible
	 */
	public void updateContent()
	{
		IDebugTarget dt = fInput.getMemoryBlock().getDebugTarget();

		// no need to update if debug target is disconnected or terminated
		if (dt.isDisconnected() || dt.isTerminated())
		{
			return;
		}

		takeContentSnapshot();

		//do not handle event if the rendering is not visible
		if (!getTableRendering(fInput).isVisible()) {
			return;
		}

		getTableRendering(fInput).refresh();

	}

	/**
	 *  Take a snapshot on the content, marking the lines as monitored
	 */
	public void takeContentSnapshot()
	{
		// cache content before getting new ones
		TableRenderingLine[] lines =lineCache.toArray(new TableRenderingLine[lineCache.size()]);
		fContentCacheInBytes = convertLinesToBytes(lines);
		fContentCacheStartAddress = lines[0].getAddress();

		if (contentCache != null)
		{
			contentCache.clear();
		}

		//do not handle event if the rendering is not visible
		if (!getTableRendering(fInput).isVisible()) {
			return;
		}

		// use existing lines as cache is the rendering is not currently displaying
		// error.  Otherwise, leave contentCache empty as we do not have updated
		// content.
		if (!getTableRendering(fInput).isDisplayingError())
		{
			for (TableRenderingLine line : lines) {
				contentCache.put(line.getAddress(), line);
				line.isMonitored = true;
			}
		}

		// reset all the deltas currently stored in contentCache
		// This will ensure that changes will be recomputed when user scrolls
		// up or down the memory view.
		resetDeltas();
	}

	/**
	 * @return buffer's top address
	 */
	public BigInteger getBufferTopAddress()
	{
		return fBufferTopAddress;
	}

	public BigInteger getBufferEndAddress()
	{
		return fBufferEndAddress;
	}

	/**
	 * Calculate address size of the given address
	 * @param address
	 * @return size of address from the debuggee
	 */
	public int getAddressSize(BigInteger address) throws DebugException
	{
		// calculate address size
		 String adjustedAddress = address.toString(16);

		 int addressSize = 0;
		 if (fInput.getMemoryBlock() instanceof IMemoryBlockExtension)
		 {
			 addressSize = ((IMemoryBlockExtension)fInput.getMemoryBlock()).getAddressSize();
		 }

		 // handle IMemoryBlock and invalid address size returned by IMemoryBlockExtension
		 if (addressSize <= 0)
		 {
			 if (adjustedAddress.length() > 8)
			 {
				 addressSize = 8;
			 }
			 else
			 {
				 addressSize = 4;
			 }
		 }

		 return addressSize;
	}

	/**
	 * @return base address of memory block
	 */
	public BigInteger getContentBaseAddress()
	{
		return fInput.getContentBaseAddress();
	}

	/**
	 * Clear all delta information in the lines
	 */
	public void resetDeltas()
	{
		Enumeration<TableRenderingLine> enumeration = contentCache.elements();

		while (enumeration.hasMoreElements())
		{
			TableRenderingLine line = enumeration.nextElement();
			line.unmarkDeltas();
		}
	}

	/**
	 * Check if address is out of buffered range
	 * @param address
	 * @return true if address is out of bufferred range, false otherwise
	 */
	public boolean isAddressOutOfRange(BigInteger address)
	{
		if (lineCache != null && !lineCache.isEmpty())
		{
			TableRenderingLine first = lineCache.firstElement();
			TableRenderingLine last = lineCache.lastElement();

			if (first == null ||last == null) {
				return true;
			}

			BigInteger startAddress = new BigInteger(first.getAddress(), 16);
			BigInteger lastAddress = new BigInteger(last.getAddress(), 16);
			int addressableUnit = getTableRendering(fInput).getAddressableUnitPerLine();
			lastAddress = lastAddress.add(BigInteger.valueOf(addressableUnit)).subtract(BigInteger.valueOf(1));

			if (startAddress.compareTo(address) <= 0 &&
				lastAddress.compareTo(address) >= 0)
			{
				return false;
			}
			return true;
		}
		return true;
	}

	public void clearContentCache()
	{
		fContentCacheInBytes = new MemoryByte[0];
		fContentCacheStartAddress = null;
		contentCache.clear();
	}

	/**
	 * @return if the memory block would manage its own update.
	 */
	private boolean isUpdateManagedByMB()
	{
		IMemoryBlock memoryBlock = getMemoryBlock();

		IMemoryRenderingUpdater managedMB = null;
		if (memoryBlock instanceof IMemoryRenderingUpdater)
		{
			managedMB = (IMemoryRenderingUpdater)memoryBlock;
		}

		if (managedMB == null) {
			managedMB = memoryBlock.getAdapter(IMemoryRenderingUpdater.class);
		}

		// do not handle event if if the memory block wants to do its
		// own update
		if (managedMB != null && managedMB.supportsManagedUpdate(getTableRendering(fInput))) {
			return true;
		}

		return false;
	}

	public boolean isDynamicLoad()
	{
		return fDynamicLoad;
	}

	private void initializeDynamicLoad()
	{
		fDynamicLoad = DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM);
	}

	public void setDynamicLoad(boolean dynamicLoad)
	{
		fDynamicLoad = dynamicLoad;
	}

	private void reorganizeLines(Vector<TableRenderingLine> lines, int numBytesPerLine) throws DebugException
	{
		if (lines == null || lines.isEmpty()) {
			return;
		}

		Object[] objs = lines.toArray();

		if (objs.length > 0)
		{
			TableRenderingLine[] renderingLines = lines.toArray(new TableRenderingLine[lines.size()]);
			MemoryByte[] buffer = convertLinesToBytes(renderingLines);
			BigInteger lineAddress = new BigInteger(renderingLines[0].getAddress(), 16);
			int numberOfLines = buffer.length / numBytesPerLine;
			boolean updateDelta = false;
			int addressLength = getAddressSize(lineAddress) * IInternalDebugUIConstants.CHAR_PER_BYTE;
			MemoryByte[] memoryBuffer = buffer;
			String address =renderingLines[0].getAddress();
			String paddedString = DebugUITools.getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);

			// set to false to preserve information delta information
			boolean manageDelta = true;

			// If change information is not managed by the memory block
			// The view tab will manage it and calculate delta information
			// for its content cache.
			if (fInput.getMemoryBlock() instanceof IMemoryBlockExtension)
			{
				manageDelta = !((IMemoryBlockExtension)fInput.getMemoryBlock()).supportsChangeManagement();
			}
			lineCache.clear();

			organizeLines(numberOfLines, updateDelta, addressLength, memoryBuffer, paddedString, address, manageDelta);
		}
	}

	private void reorganizeContentCache(int bytesPerLine)
	{
		// if content cache is empty, do nothing
		if (contentCache == null || contentCache.isEmpty()
			|| fContentCacheInBytes.length == 0 || fContentCacheStartAddress == null) {
			return;
		}

		MemoryByte[] bytes = fContentCacheInBytes;
		TableRenderingLine[] convertedLines = convertBytesToLines(bytes, bytesPerLine, new BigInteger(fContentCacheStartAddress, 16));

		contentCache.clear();
		for (TableRenderingLine convertedLine : convertedLines) {
			contentCache.put(convertedLine.getAddress(), convertedLine);
		}
	}

	private MemoryByte[] convertLinesToBytes(TableRenderingLine[] lines)
	{
		// convert the lines back to a buffer of MemoryByte
		TableRenderingLine temp = lines[0];
		int lineLength = temp.getLength();

		MemoryByte[] buffer = new MemoryByte[lines.length * lineLength];
		for (int i=0; i<lines.length; i++)
		{
			TableRenderingLine line = lines[i];
			MemoryByte[] bytes = line.getBytes();
			System.arraycopy(bytes, 0, buffer, i*lineLength, lineLength);
		}
		return buffer;
	}

	private TableRenderingLine[] convertBytesToLines(MemoryByte[] bytes, int bytesPerLine, BigInteger startAddress)
	{
		int numOfLines = bytes.length / bytesPerLine;
		String address = startAddress.toString(16);
		int addressLength;
		try {
			addressLength = getAddressSize(startAddress) * IInternalDebugUIConstants.CHAR_PER_BYTE;
		} catch (DebugException e) {
			DebugUIPlugin.log(e);
			addressLength = 4 * IInternalDebugUIConstants.CHAR_PER_BYTE;
		}
		ArrayList<TableRenderingLine> lines = new ArrayList<>();
		String paddedString = DebugUITools.getPreferenceStore().getString(IDebugUIConstants.PREF_PADDED_STR);

		for (int i=0; i<numOfLines; i++)
		{
			MemoryByte[] temp = new MemoryByte[bytesPerLine];
			System.arraycopy(bytes, i*bytesPerLine, temp, 0, bytesPerLine);

			String tmpAddress = address.toUpperCase();
			if (tmpAddress.length() < addressLength)
			{
				while (tmpAddress.length() < addressLength)
				{
					tmpAddress = "0" + tmpAddress; //$NON-NLS-1$
				}
			}

			TableRenderingLine newLine = new TableRenderingLine(tmpAddress, temp, lines.size(), paddedString);
			lines.add(newLine);

			// increment row address
			BigInteger bigInt = new BigInteger(address, 16);
			fBufferEndAddress = bigInt;
			int addressableUnit = getTableRendering(fInput).getBytesPerLine()/getTableRendering(fInput).getAddressableSize();
			address = bigInt.add(BigInteger.valueOf(addressableUnit)).toString(16);
		}

		return lines.toArray(new TableRenderingLine[lines.size()]);
	}

	private AbstractTableRendering getTableRendering(TableRenderingContentInput input)
	{
		return input.getAdapter(AbstractTableRendering.class);
	}
}
