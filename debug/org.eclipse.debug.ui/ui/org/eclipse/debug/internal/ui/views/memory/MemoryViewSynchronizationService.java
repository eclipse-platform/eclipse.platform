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
 *     Justin Kong (IBM) -  Bug 258890 -  [Memory View] MemoryViewSynchronizationService not implementing addPropertyChangeListener() correctly
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingSynchronizationService;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Synchronization service for the memory view.
 *
 * @since 3.1
 */
public class MemoryViewSynchronizationService implements IMemoryRenderingSynchronizationService, IMemoryBlockListener, IPropertyChangeListener {

	private static final int ENABLED = 0;
	private static final int ENABLING = 1;
	private static final int DISABLED = 2;

	private Hashtable<IMemoryBlock, SynchronizeInfo> fSynchronizeInfo;
	private int fEnableState = ENABLED;
	private Hashtable<IPropertyChangeListener, PropertyListener> fPropertyListeners;

	private IMemoryRendering fLastChangedRendering;
	private IMemoryRendering fSyncServiceProvider;

	private static final boolean DEBUG_SYNC_SERVICE = false;

	public MemoryViewSynchronizationService() {
		fSynchronizeInfo = new Hashtable<>();
		fPropertyListeners = new Hashtable<>();
		MemoryViewUtil.getMemoryBlockManager().addListener(this);
	}

	/**
	 * Wrapper for ISynchronizedMemoryBlockView Holds a list of property filters
	 * for the view.
	 */
	static class PropertyListener {
		IPropertyChangeListener fListener;
		String[] fFilters;

		public PropertyListener(IPropertyChangeListener listener, String[] properties) {
			fListener = listener;

			if (properties != null) {
				fFilters = properties;
			}
		}

		/**
		 * If the property matches one of the filters, the property is valid and
		 * the view should be notified about its change.
		 *
		 * @param property the property
		 * @return if the property is specified in the filter
		 */
		public boolean isValidProperty(String property) {
			if (fFilters == null) {
				return true;
			}
			for (String filter : fFilters) {
				if (filter.equals(property)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Set property filters, indicating what property change events the
		 * listener is interested in.
		 *
		 * @param filters the property filters or <code>null</code>
		 */
		public void setPropertyFilters(String[] filters) {
			fFilters = filters;
		}

		/**
		 * @return Returns the fListener.
		 */
		public IPropertyChangeListener getListener() {
			return fListener;
		}
	}

	public Object getSynchronizedProperty(IMemoryBlock memoryBlock, String propertyId) {
		SynchronizeInfo info = fSynchronizeInfo.get(memoryBlock);

		if (info != null) {
			Object value = info.getProperty(propertyId);
			return value;
		}

		return null;
	}

	@Override
	public void memoryBlocksAdded(IMemoryBlock[] memoryBlocks) {
		// do nothing when a memory block is added
		// create a synchronize info object when there is a fView
		// tab registered to be synchronized.

	}

	@Override
	public void memoryBlocksRemoved(IMemoryBlock[] memoryBlocks) {

		// Sync info can be null if the service is already shut down
		if (fSynchronizeInfo == null) {
			return;
		}

		for (IMemoryBlock memory : memoryBlocks) {
			if (fLastChangedRendering != null && fLastChangedRendering.getMemoryBlock() == memory) {
				fLastChangedRendering = null;
			}

			if (fSyncServiceProvider != null && fSyncServiceProvider.getMemoryBlock() == memory) {
				fSyncServiceProvider = null;
			}

			// delete the info object and remove it from fSynchronizeInfo
			// when the memory block is deleted
			SynchronizeInfo info = fSynchronizeInfo.get(memory);

			if (info != null) {
				info.delete();
				fSynchronizeInfo.remove(memory);
			}
		}
	}

	/**
	 * Clean up when the plug-in is shutdown
	 */
	public void shutdown() {
		if (fSynchronizeInfo != null) {
			Enumeration<SynchronizeInfo> enumeration = fSynchronizeInfo.elements();

			// clean up all synchronize info objects
			while (enumeration.hasMoreElements()) {
				SynchronizeInfo info = enumeration.nextElement();
				info.delete();
			}

			fSynchronizeInfo.clear();
			fSynchronizeInfo = null;
		}
		MemoryViewUtil.getMemoryBlockManager().removeListener(this);
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener, String[] properties) {
		fPropertyListeners.put(listener, new PropertyListener(listener, properties));
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fPropertyListeners.containsKey(listener)) {
			fPropertyListeners.remove(listener);
		}
	}

	/**
	 * Fire property change events
	 *
	 * @param evt the event to fire
	 */
	public void firePropertyChanged(final PropertyChangeEvent evt) {
		// do not fire property changed event if the synchronization
		// service is disabled
		if (fEnableState == DISABLED) {
			return;
		}

		// Make sure the synchronizer does not swallow any events
		// Values of the properties are updated in the syncrhonizer immediately.
		// Change events are queued up on the UI Thread.
		Display.getDefault().syncExec(() -> {
			if (fSynchronizeInfo == null) {
				return;
			}

			IMemoryRendering rendering = (IMemoryRendering) evt.getSource();
			String propertyId = evt.getProperty();

			SynchronizeInfo info = fSynchronizeInfo.get(rendering.getMemoryBlock());
			if (info != null) {
				Object value = info.getProperty(propertyId);
				if (value != null) {
					Enumeration<PropertyListener> enumeration = fPropertyListeners.elements();

					while (enumeration.hasMoreElements()) {
						PropertyListener listener = enumeration.nextElement();

						IPropertyChangeListener origListener = listener.getListener();

						// if it's a valid property - valid means that it's
						// listed in the property filters
						if (listener.isValidProperty(propertyId)) {
							PropertyChangeNotifier notifier = new PropertyChangeNotifier(origListener, evt);
							SafeRunner.run(notifier);
						}
					}
				}
			}
		});
	}

	@Override
	public Object getProperty(IMemoryBlock block, String property) {

		// When the synchronization service is disabled
		// return null for all queries to properties
		// This is to ensure that renderings are not synchronized
		// to new synchronization properties when the sync service is
		// disabled.
		if (!isEnabled()) {
			return null;
		}

		SynchronizeInfo info = fSynchronizeInfo.get(block);

		if (info != null) {
			return info.getProperty(property);
		}

		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event == null || !(event.getSource() instanceof IMemoryRendering)) {
			return;
		}

		// Do not handle any property changed event as the
		// sync service is being enabled.
		// Otherwise, current sync info provider may overwrite
		// sync info unexpectedly. We want to sync with the rendering
		// that is last changed.
		if (fEnableState == ENABLING) {
			return;
		}

		IMemoryRendering rendering = ((IMemoryRendering) event.getSource());
		IMemoryBlock memoryBlock = rendering.getMemoryBlock();
		String propertyId = event.getProperty();
		Object value = event.getNewValue();

		if (DEBUG_SYNC_SERVICE) {
			DebugUIPlugin.trace("SYNC SERVICE RECEIVED CHANGED EVENT:"); //$NON-NLS-1$
			DebugUIPlugin.trace("Source:  " + rendering); //$NON-NLS-1$
			DebugUIPlugin.trace("Property:  " + propertyId); //$NON-NLS-1$
			DebugUIPlugin.trace("Value:  " + value); //$NON-NLS-1$

			if (value instanceof BigInteger) {
				DebugUIPlugin.trace("Value in hex:  " + ((BigInteger) value).toString(16)); //$NON-NLS-1$
			}
		}

		if (memoryBlock == null) {
			return;
		}

		if (propertyId == null) {
			return;
		}

		// find the synchronize info object for the memory block
		SynchronizeInfo info = fSynchronizeInfo.get(memoryBlock);

		// if info is not available, need to create one to hold the property
		if (info == null) {
			info = new SynchronizeInfo(memoryBlock);
			fSynchronizeInfo.put(memoryBlock, info);
		}

		// get the value of the property
		Object oldValue = info.getProperty(propertyId);

		if (oldValue == null) {
			// if the value has never been added to the info object
			// set the property and fire a change event
			info.setProperty(propertyId, value);
			fLastChangedRendering = rendering;
			firePropertyChanged(event);
			return;
		} else if (!oldValue.equals(value)) {
			// if the value has changed
			// set the property and fire a change event
			info.setProperty(propertyId, value);
			fLastChangedRendering = rendering;
			firePropertyChanged(event);
		}
	}

	public void setEnabled(boolean enabled) {
		if (enabled && fEnableState == ENABLED) {
			return;
		}

		if (!enabled && fEnableState == DISABLED) {
			return;
		}

		try {
			if (enabled) {
				fEnableState = ENABLING;
				// get sync info from the sync service provider
				if (fLastChangedRendering != null) {
					IMemoryBlock memBlock = fLastChangedRendering.getMemoryBlock();
					SynchronizeInfo info = fSynchronizeInfo.get(memBlock);
					String[] ids = info.getPropertyIds();

					// stop handling property changed event while the
					// synchronization service is being enabled
					// this is to get around problem when the last changed
					// rendering is not currently the sync info provider
					for (String id : ids) {
						PropertyChangeEvent evt = new PropertyChangeEvent(fLastChangedRendering, id, null, info.getProperty(id));
						firePropertyChanged(evt);
					}
				}
			}
		} finally {
			if (enabled) {
				fEnableState = ENABLED;
			} else {
				fEnableState = DISABLED;
			}
		}
	}

	public boolean isEnabled() {
		return fEnableState == ENABLED;
	}

	@Override
	public void setSynchronizationProvider(IMemoryRendering rendering) {

		if (DEBUG_SYNC_SERVICE) {
			DebugUIPlugin.trace("SYNCHRONIZATION PROVIDER: " + rendering); //$NON-NLS-1$
		}
		if (fSyncServiceProvider != null) {
			fSyncServiceProvider.removePropertyChangeListener(this);
		}
		if (rendering != null) {
			rendering.addPropertyChangeListener(this);
		}
		fSyncServiceProvider = rendering;
	}

	@Override
	public IMemoryRendering getSynchronizationProvider() {
		return fSyncServiceProvider;
	}
}
