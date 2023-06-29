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
 *     Wind River Systems - Ted Williams - [Memory View] Memory View: Workflow Enhancements (Bug 215432)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

public abstract class AbstractMemoryViewPane implements IMemoryBlockListener, ISelectionListener, SelectionListener, IMemoryView, ISelectionChangedListener, IMemoryViewPane, IDebugContextListener, IDebugEventSetListener {

	public static final String BEGINNING_POPUP = "popUpBegin"; //$NON-NLS-1$
	protected static final StructuredSelection EMPTY = new StructuredSelection();

	protected Composite fViewPaneCanvas;
	protected StackLayout fStackLayout;
	protected ViewTabEnablementManager fViewTabEnablementManager;
	protected CTabFolder fEmptyTabFolder;
	protected Hashtable<Integer, CTabFolder> fTabFolderForDebugView = new Hashtable<>();
	protected boolean fVisible;
	protected IMemoryBlockRetrieval fKey; // store the key for current tab
											// folder
	protected ViewPaneSelectionProvider fSelectionProvider;
	protected IViewPart fParent;
	protected String fPaneId;
	private Composite fCanvas;
	protected String fLabel;

	private volatile boolean fIsDisposed = false;

	public AbstractMemoryViewPane(IViewPart parent) {
		super();
		fParent = parent;
		fSelectionProvider = new ViewPaneSelectionProvider();
	}

	/**
	 * Create the content of the view pane
	 *
	 * @param parent the parent composite
	 * @param paneId the id of the pane to create
	 * @param label the label for the new pane
	 * @return the control of the view pane
	 */
	@Override
	public Control createViewPane(Composite parent, String paneId, String label) {
		fPaneId = paneId;
		fLabel = label;
		fCanvas = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.makeColumnsEqualWidth = false;
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = SWT.BEGINNING;
		data.horizontalAlignment = SWT.BEGINNING;
		fCanvas.setLayout(layout);
		fCanvas.setLayoutData(data);

		// memory view area
		Composite memoryViewAreaParent = fCanvas;
		Composite subCanvas = new Composite(memoryViewAreaParent, SWT.NONE);
		fViewPaneCanvas = subCanvas;
		fStackLayout = new StackLayout();
		GridData memoryAreaData = new GridData();
		memoryAreaData.grabExcessHorizontalSpace = true;
		memoryAreaData.grabExcessVerticalSpace = true;
		memoryAreaData.verticalAlignment = SWT.FILL;
		memoryAreaData.horizontalAlignment = SWT.FILL;
		fViewPaneCanvas.setLayout(fStackLayout);
		fViewPaneCanvas.setLayoutData(memoryAreaData);

		fViewTabEnablementManager = new ViewTabEnablementManager();

		fEmptyTabFolder = new CTabFolder(fViewPaneCanvas, SWT.NULL);
		setTabFolder(fEmptyTabFolder);

		addListeners();

		Object context = DebugUITools.getPartDebugContext(fParent.getSite());
		if (context != null) {
			IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(context);
			if (retrieval != null) {
				createFolder(retrieval);
			}
		}

		fVisible = true;

		return fCanvas;
	}

	protected void addListeners() {
		MemoryViewUtil.getMemoryBlockManager().addListener(this);
		fParent.getViewSite().getPage().addSelectionListener(this);
		DebugUITools.addPartDebugContextListener(fParent.getSite(), this);
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	protected void removeListeners() {
		MemoryViewUtil.getMemoryBlockManager().removeListener(this);
		fParent.getViewSite().getPage().removeSelectionListener(this);
		DebugUITools.removePartDebugContextListener(fParent.getSite(), this);
		if (fStackLayout.topControl != null) {
			CTabFolder old = (CTabFolder) fStackLayout.topControl;

			if (!old.isDisposed()) {
				old.removeSelectionListener(this);
				old.removeSelectionListener(fViewTabEnablementManager);
			}
		}
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	protected void setTabFolder(CTabFolder folder) {
		if (fStackLayout.topControl != null) {
			CTabFolder old = (CTabFolder) fStackLayout.topControl;

			if (!old.isDisposed()) {
				old.removeSelectionListener(this);
				old.removeSelectionListener(fViewTabEnablementManager);
			}
		}

		fStackLayout.topControl = folder;

		if (folder.getItemCount() > 0) {
			CTabItem selectedItem = folder.getSelection();

			if (selectedItem != null) {
				Object selected = getCurrentSelection();
				if (selected != null) {
					fSelectionProvider.setSelection(new StructuredSelection(selected));
				} else {
					fSelectionProvider.setSelection(AbstractMemoryViewPane.EMPTY);
				}
			}
		} else {
			fSelectionProvider.setSelection(AbstractMemoryViewPane.EMPTY);
		}

		folder.addSelectionListener(this);
		folder.addSelectionListener(fViewTabEnablementManager);
	}

	private void createFolder(IMemoryBlockRetrieval memRetrieval) {
		// if we've got a tabfolder to go with the IMemoryBlockRetrieval,
		// display it
		Integer key = MemoryViewUtil.getHashCode(memRetrieval);
		if (fTabFolderForDebugView.containsKey(key)) {
			if (fStackLayout.topControl != fTabFolderForDebugView.get(key)) {
				setTabFolder(fTabFolderForDebugView.get(key));
				fViewPaneCanvas.layout();
			}
		} else { // otherwise, add a new one
			fTabFolderForDebugView.put(key, new CTabFolder(fViewPaneCanvas, SWT.NULL));
			setTabFolder(fTabFolderForDebugView.get(key));
			fViewPaneCanvas.layout();
		}
	}

	@Override
	public IMemoryViewTab getTopMemoryTab() {

		if (fStackLayout.topControl instanceof CTabFolder) {
			CTabFolder folder = (CTabFolder) fStackLayout.topControl;
			if (!folder.isDisposed()) {
				int index = folder.getSelectionIndex();
				if (index >= 0) {
					CTabItem tab = folder.getItem(index);
					return (IMemoryViewTab) tab.getData();
				}
			}
		}
		return null;
	}

	protected void disposeTab(CTabItem tabItem) {
		if (tabItem == null) {
			return;
		}

		// dispose the tab item in case the view tab has not
		// cleaned up the tab item
		if (!tabItem.isDisposed()) {
			tabItem.dispose();
		}
	}

	protected void emptyFolder() {
		setTabFolder(fEmptyTabFolder);
		if (!fViewPaneCanvas.isDisposed()) {
			fViewPaneCanvas.layout();
		}
	}

	@Override
	public void addSelectionListener(ISelectionChangedListener listener) {
		if (fSelectionProvider == null) {
			fSelectionProvider = new ViewPaneSelectionProvider();
		}

		fSelectionProvider.addSelectionChangedListener(listener);
	}

	@Override
	public void removeSelctionListener(ISelectionChangedListener listener) {
		if (fSelectionProvider == null) {
			return;
		}

		fSelectionProvider.removeSelectionChangedListener(listener);
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return fSelectionProvider;
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			Object source = event.getSource();
			if (event.getKind() == DebugEvent.TERMINATE && source instanceof IMemoryBlockRetrieval) {
				if (isDisposed()) {
					return;
				}

				// When a memory block retrieval terminates, it and its
				// tab folders should be removed from our map.
				final IMemoryBlockRetrieval ret = (IMemoryBlockRetrieval) source;
				if (ret != null) {
					Display.getDefault().asyncExec(() -> {
						if (isDisposed()) {
							return;
						}

						Integer key = MemoryViewUtil.getHashCode(ret);
						Object folder = fTabFolderForDebugView.get(key);

						if (folder != null && folder != fEmptyTabFolder) {
							// remove the tab folder , and all contained tab
							// items
							disposeOfFolder((CTabFolder) folder);
							fTabFolderForDebugView.remove(key);
						}
					});
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (isDisposed()) {
			return;
		}
		fIsDisposed = true;

		removeListeners();

		// dispose empty folders
		fEmptyTabFolder.dispose();

		// dispose all other folders
		try {
			Enumeration<CTabFolder> enumeration = fTabFolderForDebugView.elements();

			while (enumeration.hasMoreElements()) {
				CTabFolder tabFolder = enumeration.nextElement();
				disposeOfFolder(tabFolder);
			}

			// Clear the table as all CTabFolder's have been dipose()d
			fTabFolderForDebugView.clear();
		} catch (Exception e) {

			DebugUIPlugin.logErrorMessage("Exception occurred when the Memory View is disposed."); //$NON-NLS-1$
		}
	}

	/**
	 * Helper method to dispose of a tab folder, and of any tab items it
	 * contains. Must be called from the UI thread.
	 *
	 * @param tabFolder the {@link CTabFolder} to dispose
	 * */
	private void disposeOfFolder(CTabFolder tabFolder) {
		if (!tabFolder.isDisposed()) {

			// if tab folder is not empty, dipose view tabs
			CTabItem[] tabs = tabFolder.getItems();

			for (CTabItem tab : tabs) {
				disposeTab(tab);
			}

			tabFolder.dispose();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		fVisible = visible;

		IMemoryViewTab currentTab = getTopMemoryTab();
		if (currentTab != null) {
			currentTab.setEnabled(visible);
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		selectionChanged(fParent, selection);

		fSelectionProvider.setSelection(selection);
	}

	/**
	 * @return the unique identifier of the view pane
	 */
	public String getPaneId() {
		return fPaneId;
	}

	@Override
	public Control getControl() {
		return fCanvas;
	}

	@Override
	public boolean isVisible() {
		return fVisible;
	}

	public String getLabel() {
		return fLabel;
	}

	protected boolean isDisposed() {
		return fIsDisposed;
	}

	@Override
	abstract public void memoryBlocksAdded(IMemoryBlock[] memoryBlocks);

	@Override
	abstract public void memoryBlocksRemoved(final IMemoryBlock[] memoryBlocks);

	@Override
	abstract public void selectionChanged(IWorkbenchPart part, ISelection selection);

	/**
	 * @return current selection from the view pane
	 */
	abstract public Object getCurrentSelection();

	/**
	 * restore the view pane based on current selection from the debug view and the
	 * memory blocks and renderings currently exist
	 */
	@Override
	abstract public void restoreViewPane();

	/**
	 * @return actions to be contributed to the view pane's toolbar
	 */
	@Override
	abstract public IAction[] getActions();

}
