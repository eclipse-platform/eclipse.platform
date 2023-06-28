/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *	   WindRiver - Bug 192028 [Memory View] Memory view does not
 *                 display memory blocks that do not reference IDebugTarget
 *******************************************************************************/

package org.eclipse.debug.internal.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.IMemoryBlockManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;


/**
 * Implementation of IMemoryBlockManager
 * The manager is responsible to manage all memory blocks in the workbench.
 *
 * @since 3.1
 */
public class MemoryBlockManager implements IMemoryBlockManager, IDebugEventSetListener {

	private ArrayList<IMemoryBlockListener> listeners = new ArrayList<>();
	private ArrayList<IMemoryBlock> memoryBlocks = new ArrayList<>();

	private static final int ADDED = 0;
	private static final int REMOVED = 1;
	/**
	 * Notifies a memory block listener in a safe runnable to
	 * handle exceptions.
	 */
	class MemoryBlockNotifier implements ISafeRunnable {

		private IMemoryBlockListener fListener;
		private int fType;
		private IMemoryBlock[] fMemoryBlocks;

		@Override
		public void handleException(Throwable exception) {
			DebugPlugin.log(exception);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.memoryBlocksAdded(fMemoryBlocks);
					break;
				case REMOVED:
					fListener.memoryBlocksRemoved(fMemoryBlocks);
					break;
				default:
					break;
			}
		}

		/**
		 * Notify listeners of added/removed memory block events
		 *
		 * @param memBlocks blocks that have changed
		 * @param update type of change
		 */
		public void notify(IMemoryBlock[] memBlocks, int update) {
			if (listeners != null) {
				fType = update;
				Object[] copiedListeners= listeners.toArray(new IMemoryBlockListener[listeners.size()]);
				for (Object copiedListener : copiedListeners) {
					fListener = (IMemoryBlockListener) copiedListener;
					fMemoryBlocks = memBlocks;
					SafeRunner.run(this);
				}
			}
			fListener = null;
			fMemoryBlocks = null;
		}
	}

	/**
	 * Returns the <code>MemoryBlockNotifier</code>
	 * @return the <code>MemoryBlockNotifier</code>
	 *
	 * TODO consider using only one of these, and sync where needed,
	 * this way we are not creating a new every single time.
	 */
	private MemoryBlockNotifier getMemoryBlockNotifier() {
		return new MemoryBlockNotifier();
	}

	@Override
	public void addMemoryBlocks(IMemoryBlock[] mem) {
		if (memoryBlocks == null) {
			return;
		}
		if (mem == null) {
			DebugPlugin.logMessage("Null argument passed into IMemoryBlockManager.addMemoryBlock", null); //$NON-NLS-1$
			return;
		}

		if(mem.length > 0) {
			ArrayList<IMemoryBlock> newMemoryBlocks = new ArrayList<>();
			for (IMemoryBlock m : mem) {
				// do not allow duplicates
				if (!memoryBlocks.contains(m)) {
					newMemoryBlocks.add(m);
					memoryBlocks.add(m);
					// add listener for the first memory block added
					if (memoryBlocks.size() == 1) {
						DebugPlugin.getDefault().addDebugEventListener(this);
					}
				}
			}
			notifyListeners(newMemoryBlocks.toArray(new IMemoryBlock[newMemoryBlocks.size()]), ADDED);
		}
	}

	@Override
	public void removeMemoryBlocks(IMemoryBlock[] memBlocks) {
		if (memoryBlocks == null) {
			return;
		}
		if (memBlocks == null){
			DebugPlugin.logMessage("Null argument passed into IMemoryBlockManager.removeMemoryBlock", null); //$NON-NLS-1$
			return;
		}

		if(memBlocks.length > 0) {
			for (IMemoryBlock memBlock : memBlocks) {
				memoryBlocks.remove(memBlock);
				// remove listener after the last memory block has been removed
				if (memoryBlocks.isEmpty()) {
					DebugPlugin.getDefault().removeDebugEventListener(this);
				}
				if (memBlock instanceof IMemoryBlockExtension) {
					try {
						((IMemoryBlockExtension) memBlock).dispose();
					}catch (DebugException e) {
						DebugPlugin.log(e);
					}
				}
			}
			notifyListeners(memBlocks, REMOVED);
		}
	}

	@Override
	public void addListener(IMemoryBlockListener listener) {

		if(listeners == null) {
			return;
		}
		if(listener == null) {
			DebugPlugin.logMessage("Null argument passed into IMemoryBlockManager.addListener", null); //$NON-NLS-1$
			return;
		}
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(IMemoryBlockListener listener) {

		if(listeners == null) {
			return;
		}
		if(listener == null) {
			DebugPlugin.logMessage("Null argument passed into IMemoryBlockManager.removeListener", null); //$NON-NLS-1$
			return;
		}
		if (listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}

	@Override
	public IMemoryBlock[] getMemoryBlocks() {
		return memoryBlocks.toArray(new IMemoryBlock[memoryBlocks.size()]);
	}

	@Override
	public IMemoryBlock[] getMemoryBlocks(IDebugTarget debugTarget) {
		ArrayList<IMemoryBlock> memoryBlocksList = new ArrayList<>();
		for (IMemoryBlock block : memoryBlocks) {
			if (block.getDebugTarget() == debugTarget) {
				memoryBlocksList.add(block);
			}
		}
		return memoryBlocksList.toArray(new IMemoryBlock[memoryBlocksList.size()]);
	}

	@Override
	public IMemoryBlock[] getMemoryBlocks(IMemoryBlockRetrieval retrieve) {
		ArrayList<IMemoryBlock> memoryBlocksList = new ArrayList<>();
		for (IMemoryBlock block : memoryBlocks) {
			if (block instanceof IMemoryBlockExtension) {
				if (((IMemoryBlockExtension) block).getMemoryBlockRetrieval() == retrieve) {
					memoryBlocksList.add(block);
				}
			}
			else {
				IMemoryBlockRetrieval mbRetrieval = block.getAdapter(IMemoryBlockRetrieval.class);
				// standard memory block always uses the debug target as the memory block retrieval
				if (mbRetrieval == null) {
					mbRetrieval = block.getDebugTarget();
				}
				if (mbRetrieval == retrieve) {
					memoryBlocksList.add(block);
				}
			}
		}
		return memoryBlocksList.toArray(new IMemoryBlock[memoryBlocksList.size()]);
	}

	/**
	 * Notifies the listeners about the given memory blocks and the event to be sent
	 * @param memBlocks the array of memory blocks
	 * @param event the event to notify to the blocks
	 */
	private void notifyListeners(IMemoryBlock[] memBlocks, int event) {
		getMemoryBlockNotifier().notify(memBlocks, event);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			handleDebugEvent(event);
		}
	}

	/**
	 * Handles a debug event
	 * @param event the {@link DebugEvent}
	 */
	public void handleDebugEvent(DebugEvent event) {
		Object obj = event.getSource();
		IDebugTarget dt = null;

		if (event.getKind() == DebugEvent.TERMINATE) {
			// a terminate event could happen from an IThread or IDebugTarget
			// only handle a debug event from the debug target
			if (obj instanceof IDebugTarget) {
				dt = ((IDebugTarget)obj);

				// getMemoryBlocks will return an empty array if it is null
				IMemoryBlock[] deletedMemoryBlocks = getMemoryBlocks(dt);
				removeMemoryBlocks(deletedMemoryBlocks);
			}
		}
	}

	/**
	 * Clean up when the plugin is shut down
	 */
	public void shutdown() {
		if (listeners != null) {
			listeners.clear();
			listeners = null;
		}

		if (memoryBlocks != null) {
			memoryBlocks.clear();
			memoryBlocks = null;
		}
	}

}
