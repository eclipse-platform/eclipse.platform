/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.AsynchronousModel;
import org.eclipse.debug.internal.ui.viewers.AsynchronousTableViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IStatusMonitor;
import org.eclipse.debug.internal.ui.viewers.provisional.ILabelRequestMonitor;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.progress.UIJob;

abstract public class AsyncVirtualContentTableViewer extends AsynchronousTableViewer {

	private Object fPendingTopIndexKey;
	private ArrayList<Object> fTopIndexQueue = new ArrayList<>();

	private boolean fPendingResizeColumns;
	private ListenerList<IVirtualContentListener> fVirtualContentListeners;
	private SelectionListener fScrollSelectionListener;
	private ListenerList<IPresentationErrorListener> fPresentationErrorListeners;
	private Object fTopIndexKey;

	public AsyncVirtualContentTableViewer(Composite parent, int style) {
		super(parent, style);
		fVirtualContentListeners = new ListenerList<>();
		fPresentationErrorListeners = new ListenerList<>();
		initScrollBarListener();
	}

	private void initScrollBarListener() {
		ScrollBar scroll = getTable().getVerticalBar();
		fScrollSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleScrollBarSelection();
			}
		};
		scroll.addSelectionListener(fScrollSelectionListener);
	}

	public void setTopIndex(Object key) {
		fPendingTopIndexKey = key;
		attemptSetTopIndex();
	}

	protected Object getPendingSetTopIndexKey() {
		return fPendingTopIndexKey;
	}

	@Override
	protected void handlePresentationFailure(IStatusMonitor monitor, IStatus status) {
		notifyPresentationError(monitor, status);
	}

	public void disposeColumns() {
		// clean up old columns
		TableColumn[] oldColumns = getTable().getColumns();

		for (TableColumn oldColumn : oldColumns) {
			oldColumn.dispose();
		}
	}

	public void disposeCellEditors() {
		// clean up old cell editors
		CellEditor[] oldCellEditors = getCellEditors();

		if (oldCellEditors != null) {
			for (CellEditor oldCellEditor : oldCellEditors) {
				oldCellEditor.dispose();
			}
		}
	}

	/**
	 * Resize column to the preferred size.
	 */
	public void resizeColumnsToPreferredSize() {
		fPendingResizeColumns = true;
		fPendingResizeColumns = attemptResizeColumnsToPreferredSize();
	}

	private boolean attemptResizeColumnsToPreferredSize() {
		if (fPendingResizeColumns) {
			if (!hasPendingUpdates()) {
				UIJob job = new UIJob("packcolumns") { //$NON-NLS-1$

					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						Table table = getTable();

						if (!table.isDisposed()) {
							// if table size is zero, the rendering has not been
							// made visible
							// cannot pack until the rendering is visible
							if (table.getSize().x > 0) {
								TableColumn[] columns = table.getColumns();
								for (int i = 0; i < columns.length - 1; i++) {
									columns[i].pack();
								}
							} else {
								fPendingResizeColumns = true;
							}
						}
						return Status.OK_STATUS;
					}
				};
				job.setSystem(true);
				job.schedule();
				return false;
			}
		}
		return fPendingResizeColumns;
	}

	/**
	 * Attempts to update any pending setTopIndex
	 */
	protected synchronized void attemptSetTopIndex() {
		if (fPendingTopIndexKey != null) {
			Object remaining = doAttemptSetTopIndex(fPendingTopIndexKey);
			if (remaining == null) {
				fPendingTopIndexKey = remaining;
			}
		}
	}

	private synchronized Object doAttemptSetTopIndex(final Object topIndexKey) {
		final int i = getVirtualContentModel().indexOfKey(topIndexKey);
		if (i >= 0) {
			UIJob job = new UIJob("set top index") { //$NON-NLS-1$

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (getTable().isDisposed()) {
						fTopIndexQueue.clear();
						return Status.OK_STATUS;
					}

					int idx = getVirtualContentModel().indexOfKey(topIndexKey);
					if (idx >= 0) {
						if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
							DebugUIPlugin.trace("actual set top index: " + ((BigInteger) topIndexKey).toString(16)); //$NON-NLS-1$
						}
						fPendingTopIndexKey = null;
						setTopIndexKey(topIndexKey);
						getTable().setTopIndex(idx);
						tableTopIndexSetComplete();

						if (getTable().getTopIndex() != idx) {
							if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
								DebugUIPlugin.trace(">>> FAILED set top index : " + ((BigInteger) topIndexKey).toString(16)); //$NON-NLS-1$
							}

							// only retry if we have pending updates
							if (hasPendingUpdates()) {
								if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
									DebugUIPlugin.trace(">>> Retry top index: " + ((BigInteger) topIndexKey).toString(16)); //$NON-NLS-1$
								}

								fPendingTopIndexKey = topIndexKey;
							}
						}
					} else {
						if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
							DebugUIPlugin.trace("cannot find key, put it back to the queue: " + topIndexKey); //$NON-NLS-1$
						}
						fPendingTopIndexKey = topIndexKey;
					}

					// remove the top index key from queue when it is processed
					removeKeyFromQueue(topIndexKey);

					return Status.OK_STATUS;
				}
			};

			// set top index does not happen immediately, keep track of
			// all pending set top index
			addKeyToQueue(topIndexKey);

			job.setSystem(true);
			job.schedule();
			return topIndexKey;
		}
		return topIndexKey;
	}

	protected void tableTopIndexSetComplete() {

	}

	public void addVirtualContentListener(IVirtualContentListener listener) {
		fVirtualContentListeners.add(listener);
	}

	public void removeVirtualContentListener(IVirtualContentListener listener) {
		fVirtualContentListeners.remove(listener);
	}

	protected void notifyListenersAtBufferStart() {
		int topIdx = getTable().getTopIndex();
		for (IVirtualContentListener iVirtualContentListener : fVirtualContentListeners) {
			final IVirtualContentListener listener = iVirtualContentListener;
			if (topIdx < listener.getThreshold(IVirtualContentListener.BUFFER_START)) {
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.handledAtBufferStart();
					}

					@Override
					public void handleException(Throwable exception) {
						DebugUIPlugin.log(exception);
					}
				});
			}
		}
	}

	protected void notifyListenersAtBufferEnd() {
		int topIdx = getTable().getTopIndex();
		int bottomIdx = topIdx + getNumberOfVisibleLines();
		int elementsCnt = getVirtualContentModel().getElements().length;
		int numLinesLeft = elementsCnt - bottomIdx;

		for (IVirtualContentListener iVirtualContentListener : fVirtualContentListeners) {
			final IVirtualContentListener listener = iVirtualContentListener;
			if (numLinesLeft <= listener.getThreshold(IVirtualContentListener.BUFFER_END)) {
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.handleAtBufferEnd();
					}

					@Override
					public void handleException(Throwable exception) {
						DebugUIPlugin.log(exception);
					}
				});
			}
		}
	}

	protected void handleScrollBarSelection() {
		// ignore event if there is pending set top index in the queue
		if (!fTopIndexQueue.isEmpty()) {
			return;
		}
		topIndexChanged();
	}

	public void topIndexChanged() {
		if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
			MemorySegment a = (MemorySegment) getTable().getItem(getTable().getTopIndex()).getData();
			DebugUIPlugin.trace(Thread.currentThread().getName() + " " + this + " handle scroll bar moved:  top index: " + a.getAddress().toString(16)); //$NON-NLS-1$ //$NON-NLS-2$
		}

		setTopIndexKey(getVirtualContentModel().getKey(getTable().getTopIndex()));

		notifyListenersAtBufferStart();
		notifyListenersAtBufferEnd();
	}

	protected void setTopIndexKey(Object key) {
		fTopIndexKey = key;
	}

	protected Object getTopIndexKey() {
		return fTopIndexKey;
	}

	@Override
	protected synchronized void preservingSelection(Runnable updateCode) {
		Object oldTopIndexKey = null;
		if (fPendingTopIndexKey == null) {
			// preserve selection
			oldTopIndexKey = getTopIndexKey();
		} else {
			oldTopIndexKey = fPendingTopIndexKey;
		}

		try {

			// perform the update
			updateCode.run();
		} finally {
			if (oldTopIndexKey != null) {
				setTopIndex(oldTopIndexKey);
			}
		}

	}

	public void addPresentationErrorListener(IPresentationErrorListener errorListener) {
		fPresentationErrorListeners.add(errorListener);
	}

	public void removePresentationErrorListener(IPresentationErrorListener errorListener) {
		fPresentationErrorListeners.remove(errorListener);
	}

	private void notifyPresentationError(final IStatusMonitor monitor, final IStatus status) {
		for (IPresentationErrorListener iPresentationErrorListener : fPresentationErrorListeners) {
			final IPresentationErrorListener listener = iPresentationErrorListener;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.handlePresentationFailure(monitor, status);
					}

					@Override
					public void handleException(Throwable exception) {
						DebugUIPlugin.log(exception);
					}
				});
		}
	}

	@Override
	protected AsynchronousModel createModel() {
		return createVirtualContentTableModel();
	}

	abstract protected AbstractVirtualContentTableModel createVirtualContentTableModel();

	private void addKeyToQueue(Object topIndexKey) {
		synchronized (fTopIndexQueue) {
			if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
				DebugUIPlugin.trace(" >>> add to top index queue: " + ((BigInteger) topIndexKey).toString(16)); //$NON-NLS-1$
			}
			fTopIndexQueue.add(topIndexKey);
		}
	}

	private void removeKeyFromQueue(Object topIndexKey) {
		synchronized (fTopIndexQueue) {
			if (DebugUIPlugin.DEBUG_DYNAMIC_LOADING) {
				DebugUIPlugin.trace(" >>> remove frome top index queue: " + ((BigInteger) topIndexKey).toString(16)); //$NON-NLS-1$
			}
			fTopIndexQueue.remove(topIndexKey);
		}
	}

	public AbstractVirtualContentTableModel getVirtualContentModel() {
		if (getModel() instanceof AbstractVirtualContentTableModel) {
			return (AbstractVirtualContentTableModel) getModel();
		}
		return null;
	}

	private int getNumberOfVisibleLines() {
		Table table = getTable();
		int height = table.getSize().y;

		// when table is not yet created, height is zero
		if (height == 0) {
			// make use of the table viewer to estimate table size
			height = table.getParent().getSize().y;
		}

		// height of border
		int border = table.getHeaderHeight();

		// height of scroll bar
		int scroll = table.getHorizontalBar().getSize().y;

		// height of table is table's area minus border and scroll bar height
		height = height - border - scroll;

		// calculate number of visible lines
		int lineHeight = getMinTableItemHeight(table);

		int numberOfLines = height / lineHeight;

		if (numberOfLines <= 0) {
			return 20;
		}

		return numberOfLines;
	}

	private int getMinTableItemHeight(Table table) {

		// Hack to get around Linux GTK problem.
		// On Linux GTK, table items have variable item height as
		// carriage returns are actually shown in a cell. Some rows will be
		// taller than others. When calculating number of visible lines, we
		// need to find the smallest table item height. Otherwise, the rendering
		// underestimates the number of visible lines. As a result the rendering
		// will not be able to get more memory as needed.
		if (MemoryViewUtil.isLinuxGTK()) {
			// check each of the items and find the minimum
			TableItem[] items = table.getItems();
			int minHeight = table.getItemHeight();
			for (TableItem item : items) {
				if (item.getData() != null) {
					minHeight = Math.min(item.getBounds(0).height, minHeight);
				}
			}

			return minHeight;

		}
		return table.getItemHeight();
	}

	@Override
	protected void updateComplete(IStatusMonitor monitor) {
		super.updateComplete(monitor);
		attemptSetTopIndex();
		if (monitor instanceof ILabelRequestMonitor) {
			fPendingResizeColumns = attemptResizeColumnsToPreferredSize();
		}
	}

	protected boolean hasPendingSetTopIndex() {
		return !fTopIndexQueue.isEmpty();
	}

}
