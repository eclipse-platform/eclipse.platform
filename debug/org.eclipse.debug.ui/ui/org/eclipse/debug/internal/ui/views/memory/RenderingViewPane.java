/*******************************************************************************
 * Copyright (c) 2004, 2020 IBM Corporation and others.
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
 *     ARM - Bug 192028 [Memory View] Memory view does not
 *                 display memory blocks that do not reference IDebugTarget
 *     WindRiver - Bug 216509 [Memory View] typo, s/isMeomryBlockRemoved/isMemoryBlockRemoved
 *     Wind River Systems - Ted Williams - [Memory View] Memory View: Workflow Enhancements (Bug 215432)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.memory.renderings.CreateRendering;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.debug.ui.memory.IMemoryRenderingSynchronizationService;
import org.eclipse.debug.ui.memory.IResettableMemoryRendering;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

/**
 * Represents a rendering view pane in the Memory View. This hosts the memory
 * view tabs in the view.
 *
 * @since 3.1
 *
 */
public class RenderingViewPane extends AbstractMemoryViewPane implements IMemoryRenderingContainer {

	public static final String RENDERING_VIEW_PANE_ID = DebugUIPlugin.getUniqueIdentifier() + ".MemoryView.RenderingViewPane"; //$NON-NLS-1$

	private Hashtable<IMemoryBlock, CTabFolder> fTabFolderForMemoryBlock = new Hashtable<>();
	private Hashtable<CTabFolder, IMemoryBlock> fMemoryBlockFromTabFolder = new Hashtable<>();

	private ViewPaneRenderingMgr fRenderingMgr;

	private IMemoryRenderingSite fRenderingSite;
	private Set<IMemoryRendering> fAddedRenderings = new HashSet<>();
	private Set<IMemoryBlock> fAddedMemoryBlocks = new HashSet<>();

	private boolean fCanAddRendering = true;
	private boolean fCanRemoveRendering = true;

	/**
	 * @param parent is the view hosting this view pane
	 * @param paneId is the identifier assigned by the Memory View
	 *
	 *            Pane id is assigned with the following format. Rendering view
	 *            pane created has its id assigned to
	 *            org.eclipse.debug.ui.MemoryView.RenderingViewPane.#. # is a
	 *            number indicating the order of which the rendering view pane
	 *            is created. First rendering view pane created will have its id
	 *            assigned to
	 *            org.eclipse.debug.ui.MemoryView.RenderingViewPane.1. Second
	 *            rendering view pane created will have its id assigned to
	 *            org.eclipse.debug.ui.MemoryView.RenderingViewPane.2. and so
	 *            on. View pane are created from left to right by the Memory
	 *            View.
	 *
	 */
	public RenderingViewPane(IViewPart parent) {
		super(parent);

		if (parent instanceof IMemoryRenderingSite) {
			fRenderingSite = (IMemoryRenderingSite) parent;
		} else {
			DebugUIPlugin.logErrorMessage("Parent for the rendering view pane is invalid."); //$NON-NLS-1$
		}
	}

	@Override
	public void memoryBlocksAdded(final IMemoryBlock[] memoryBlocks) {
		Display.getDefault().asyncExec(() -> {

			if (isDisposed()) {
				return;
			}

			// check condition before doing anything
			if (memoryBlocks == null || memoryBlocks.length <= 0) {
				return;
			}

			for (IMemoryBlock memory : memoryBlocks) {
				if (!fTabFolderForMemoryBlock.containsKey(memory)) {
					createFolderForMemoryBlock(memory);
				}
				fAddedMemoryBlocks.add(memory);
				updateToolBarActionsEnablement();
			}
		});
	}

	@Override
	public void memoryBlocksRemoved(final IMemoryBlock[] memoryBlocks) {
		Display.getDefault().asyncExec(() -> {
			for (IMemoryBlock mbRemoved : memoryBlocks) {
				if (fTabFolderForMemoryBlock == null) {
					return;
				}
				// get all renderings from this memory block and remove them
				// from the view
				IMemoryRendering[] renderings = fRenderingMgr.getRenderingsFromMemoryBlock(mbRemoved);
				for (IMemoryRendering rendering : renderings) {
					removeMemoryRendering(rendering);
				}
				// remove a the tab folder if the memory block is removed
				CTabFolder tabFolder = fTabFolderForMemoryBlock.get(mbRemoved);
				if (tabFolder == null) {
					continue;
				}
				fTabFolderForMemoryBlock.remove(mbRemoved);
				fMemoryBlockFromTabFolder.remove(tabFolder);
				IMemoryBlockRetrieval retrieve = MemoryViewUtil.getMemoryBlockRetrieval(mbRemoved);
				if (retrieve != null) {
					if (fTabFolderForDebugView.contains(tabFolder)) {
						fTabFolderForDebugView.remove(MemoryViewUtil.getHashCode(retrieve));
					}
				}
				if (!tabFolder.isDisposed()) {
					// dispose all view tabs belonging to the tab folder
					CTabItem[] items = tabFolder.getItems();
					for (CTabItem item : items) {
						disposeTab(item);
					}
					// dispose the tab folder
					tabFolder.dispose();
					// if this is the top control
					if (tabFolder == fStackLayout.topControl) {

						// if memory view is visible and have a selection
						// follow memory view's selection

						ISelection selection = DebugUIPlugin.getActiveWorkbenchWindow().getSelectionService()
								.getSelection(IDebugUIConstants.ID_MEMORY_VIEW);
						IMemoryBlock mbToSelect = getMemoryBlock(selection);

						if (mbToSelect != null) {
							// memory view may not have got the event and is
							// still displaying
							// the deleted memory block
							if (mbToSelect != mbRemoved) {
								handleMemoryBlockSelection(null, mbToSelect);
							} else if ((MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieve).length > 0)) {
								mbToSelect = MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieve)[0];
								handleMemoryBlockSelection(null, mbToSelect);
							} else {
								emptyFolder();
							}
						} else if (MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieve).length > 0) { // get
																													// to
																													// the
																													// next
																													// folder
							mbToSelect = MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieve)[0];
							handleMemoryBlockSelection(null, mbToSelect);
						} else {
							emptyFolder();

						}
					}

					// if not the top control
					// no need to do anything
				}
				fAddedMemoryBlocks.remove(mbRemoved);
				updateToolBarActionsEnablement();
			}
		});

	}

	@Override
	public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
		if (isDisposed()) {
			return;
		}

		// do not schedule job if any of these conditions are true
		if (part == RenderingViewPane.this) {
			return;
		}

		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		if (selection == AbstractMemoryViewPane.EMPTY) {
			return;
		}

		UIJob job = new UIJob("RenderingViewPane selectionChanged") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {

					if (isDisposed()) {
						return Status.OK_STATUS;
					}

					if (selection.isEmpty()) {
						// if the event comes from Memory View
						// pick empty tab folder as the memory view is no longer
						// displaying anything
						if (part.getSite().getId().equals(IDebugUIConstants.ID_MEMORY_VIEW)) {
							if (part == getMemoryRenderingSite().getSite().getPart()) {
								IMemoryViewTab lastViewTab = getTopMemoryTab();

								if (lastViewTab != null) {
									lastViewTab.setEnabled(false);
								}

								emptyFolder();
							}
						}

						// do not do anything if there is no selection
						// In the case when a debug adpater fires a debug event
						// incorrectly, Launch View sets
						// selection to nothing. If the view tab is disabled, it
						// erases all the "delta" information
						// in the content. This may not be desirable as it will
						// cause memory to show up as
						// unchanged when it's actually changed. Do not disable
						// the view tab until there is a
						// valid selection.

						return Status.OK_STATUS;
					}

					// back up current view tab
					IMemoryViewTab lastViewTab = getTopMemoryTab();

					if (!(selection instanceof IStructuredSelection)) {
						return Status.OK_STATUS;
					}

					Object elem = ((IStructuredSelection) selection).getFirstElement();

					if (elem instanceof IMemoryBlock) {
						// if the selection event comes from this view
						if (part == getMemoryRenderingSite()) {
							// find the folder associated with the given
							// IMemoryBlockRetrieval
							IMemoryBlock memBlock = (IMemoryBlock) elem;

							// should never get here... added code for safety
							if (fTabFolderForMemoryBlock == null) {
								if (lastViewTab != null) {
									lastViewTab.setEnabled(false);
								}

								emptyFolder();
								return Status.OK_STATUS;
							}

							handleMemoryBlockSelection(lastViewTab, memBlock);
						}
					}
				} catch (SWTException se) {
					DebugUIPlugin.log(se);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	public void handleMemoryBlockSelection(final IMemoryViewTab lastViewTab, final IMemoryBlock memBlock) {
		// Do not check if the debug target of mb is removed
		// We should not get into this method if the debug target of the memory
		// block is terminated
		// Memory Block Manager gets the terminate event and would have removed
		// all memory blocks
		// associated with the debug target
		// Therefore, we will never try to set a selection to a memory block
		// whose target is terminated

		// check current memory block
		CTabFolder currentFolder = (CTabFolder) fStackLayout.topControl;
		if (currentFolder != null && !currentFolder.isDisposed()) {
			IMemoryBlock currentBlk = fMemoryBlockFromTabFolder.get(currentFolder);
			if (currentBlk != null) {
				if (currentBlk == memBlock) {
					return;
				}
			}
		}

		if (getTopMemoryTab() != null) {
			if (getTopMemoryTab().getRendering().getMemoryBlock() == memBlock) {
				return;
			}
		}

		// if we've got a tabfolder to go with the IMemoryBlock, display
		// it
		if (fTabFolderForMemoryBlock.containsKey(memBlock)) {
			if (fStackLayout.topControl != fTabFolderForMemoryBlock.get(memBlock)) {
				setTabFolder(fTabFolderForMemoryBlock.get(memBlock));
				fViewPaneCanvas.layout();
			}
		} else { // otherwise, add a new one
			CTabFolder folder = createTabFolder(fViewPaneCanvas);

			fTabFolderForMemoryBlock.put(memBlock, folder);
			fMemoryBlockFromTabFolder.put(folder, memBlock);
			setTabFolder(fTabFolderForMemoryBlock.get(memBlock));
			fViewPaneCanvas.layout();
			fAddedMemoryBlocks.add(memBlock);

			newCreateRenderingForFolder(memBlock, folder);
		}

		// restore view tabs
		IMemoryRendering[] renderings = fRenderingMgr.getRenderingsFromMemoryBlock(memBlock);
		CTabFolder toDisplay = (CTabFolder) fStackLayout.topControl;

		// if only CreateRendering is present, restore renderings
		if (isRestoreViewTabs(toDisplay)) {
			restoreViewTabs(renderings);
		}

		// disable last view tab as it becomes hidden
		IMemoryViewTab newViewTab = getTopMemoryTab();

		if (lastViewTab != null && lastViewTab != newViewTab) {
			lastViewTab.setEnabled(false);
		}

		if (newViewTab != null) {
			// if new view tab is not already enabled, enable it
			if (!newViewTab.isEnabled()) {
				// if the view tab is visible, enable it
				if (fVisible) {
					newViewTab.setEnabled(fVisible);
				}
			}
		}

		IMemoryViewTab viewTab = getTopMemoryTab();
		if (viewTab != null) {
			setRenderingSelection(viewTab.getRendering());
		}

		// set toolbar actions enabled/disabled
		updateToolBarActionsEnablement();
	}

	private boolean isRestoreViewTabs(CTabFolder folder) {
		if (canAddRendering()) {
			return (folder.getItemCount() == 1 && getTopMemoryTab().getRendering() instanceof CreateRendering);
		} else {
			return (folder.getItemCount() == 0);
		}
	}

	private int getIndexOfCreateRenderingTab(CTabFolder folder) {
		for (int i = 0; i < folder.getItemCount(); i++) {
			if (folder.getItem(i).getData() instanceof MemoryViewTab && ((MemoryViewTab) folder.getItem(i).getData()).getRendering() instanceof CreateRendering) {
				return i;
			}
		}

		return -1;
	}

	public void memoryBlockRenderingAdded(final IMemoryRendering rendering) {

		Display.getDefault().asyncExec(() -> {

			if (isDisposed()) {
				return;
			}

			if (fAddedRenderings.contains(rendering)) {
				return;
			}

			IMemoryBlock memoryblk = rendering.getMemoryBlock();

			CTabFolder tabFolder = fTabFolderForMemoryBlock.get(memoryblk);

			if (tabFolder == null) {
				tabFolder = createFolderForMemoryBlock(memoryblk);
			}

			if (tabFolder == fStackLayout.topControl) {
				// disable current view tab
				if (getTopMemoryTab() != null) {
					deactivateRendering(getTopMemoryTab());
					getTopMemoryTab().setEnabled(false);
				}
			}
			fAddedRenderings.add(rendering);

			int index = getIndexOfCreateRenderingTab(tabFolder);
			if (index < 0) {
				index = 0;
			}
			CTabItem tab = createTab(tabFolder, index);

			MemoryViewTab viewTab = new MemoryViewTab(tab, rendering, getInstance());
			tabFolder.setSelection(tabFolder.indexOf(tab));

			if (tabFolder == fStackLayout.topControl) {
				setRenderingSelection(viewTab.getRendering());

				// disable top view tab if the view pane is not visible
				IMemoryViewTab top = getTopMemoryTab();
				if (top != null) {
					top.setEnabled(fVisible);
				}
			} else {
				deactivateRendering(viewTab);
				viewTab.setEnabled(false);
			}

			updateToolBarActionsEnablement();
		});
	}

	public void memoryBlockRenderingRemoved(final IMemoryRendering rendering) {
		final IMemoryBlock memory = rendering.getMemoryBlock();

		// need to run the following code on the UI Thread to avoid invalid
		// thread access exception
		Display.getDefault().asyncExec(() -> {
			if (!fAddedRenderings.contains(rendering)) {
				return;
			}

			fAddedRenderings.remove(rendering);

			CTabFolder tabFolder = (CTabFolder) fStackLayout.topControl;

			if (tabFolder.isDisposed()) {
				return;
			}

			CTabItem[] tabs = tabFolder.getItems();
			boolean foundTab = false;
			for (CTabItem tab : tabs) {
				IMemoryViewTab viewTab1 = (IMemoryViewTab) tab.getData();
				if (tab.isDisposed()) {
					continue;
				}
				if (viewTab1.getRendering().getMemoryBlock() == memory) {
					if (viewTab1.getRendering() == rendering) {
						foundTab = true;
						disposeTab(tab);
						break;
					}
				}
			}

			// if a tab is not found in the current top control
			// this deletion is a result of a debug target termination
			// find memory from other folder and dispose the view tab
			if (!foundTab) {
				Enumeration<CTabFolder> enumeration = fTabFolderForMemoryBlock.elements();
				while (enumeration.hasMoreElements()) {
					CTabFolder otherTabFolder = enumeration.nextElement();
					tabs = otherTabFolder.getItems();
					IMemoryViewTab viewTab2 = null;
					for (CTabItem tab : tabs) {
						viewTab2 = (IMemoryViewTab) tab.getData();
						if (viewTab2.getRendering().getMemoryBlock() == memory) {
							if (viewTab2.getRendering() == rendering) {
								foundTab = true;
								disposeTab(tab);
								break;
							}
						}
					}
				}
			}
			IMemoryViewTab top = getTopMemoryTab();

			// update selection
			if (top != null) {
				setRenderingSelection(top.getRendering());
			}

			updateToolBarActionsEnablement();
		});

	}

	/**
	 * @param viewTab
	 */
	protected void setRenderingSelection(IMemoryRendering rendering) {

		if (rendering != null) {
			fSelectionProvider.setSelection(new StructuredSelection(rendering));
		}
	}

	private void restoreViewTabs(IMemoryRendering[] renderings) {
		for (IMemoryRendering rendering : renderings) {
			memoryBlockRenderingAdded(rendering);
		}
	}

	private void handleDebugElementSelection(final IMemoryViewTab lastViewTab, final IAdaptable element) {
		// get current memory block retrieval and debug target
		IMemoryBlockRetrieval currentRetrieve = null;

		// get tab folder
		CTabFolder tabFolder = (CTabFolder) fStackLayout.topControl;

		// get memory block
		IMemoryBlock currentBlock = fMemoryBlockFromTabFolder.get(tabFolder);

		if (currentBlock != null) {
			currentRetrieve = MemoryViewUtil.getMemoryBlockRetrieval(currentBlock);

			// backup current retrieve and tab folder
			if (currentRetrieve != null && tabFolder != null) {
				fTabFolderForDebugView.put(MemoryViewUtil.getHashCode(currentRetrieve), tabFolder);
			}
		}

		// find the folder associated with the given IMemoryBlockRetrieval
		IMemoryBlockRetrieval retrieve = MemoryViewUtil.getMemoryBlockRetrieval(element);

		// if debug target has changed
		// switch to that tab folder
		if (retrieve != null && retrieve != currentRetrieve) {
			Integer key = MemoryViewUtil.getHashCode(retrieve);
			CTabFolder folder = fTabFolderForDebugView.get(key);

			if (folder != null) {
				setTabFolder(folder);
				fTabFolderForDebugView.put(key, folder);
				fViewPaneCanvas.layout();
			} else {
				// find out if there is any memory block for this debug target
				// and set up tab folder for the memory blocks
				IMemoryBlock blocks[] = MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieve);

				if (blocks.length > 0) {
					handleMemoryBlockSelection(null, blocks[0]);
				} else {
					emptyFolder();
					fTabFolderForDebugView.put(key, fEmptyTabFolder);
					fViewPaneCanvas.layout();
				}
			}
		}

		// disable last view tab as it becomes hidden
		IMemoryViewTab newViewTab = getTopMemoryTab();

		if (lastViewTab != null && lastViewTab != newViewTab) {
			lastViewTab.setEnabled(false);
		}

		if (newViewTab != null) {
			// if new view tab is not already enabled, enable it
			if (!newViewTab.isEnabled()) {
				// if the view tab is visible, enable it
				if (fVisible) {
					newViewTab.setEnabled(fVisible);
				}
			}

			// should only change selection if the new view tab is different
			if (lastViewTab != newViewTab) {
				setRenderingSelection(newViewTab.getRendering());
			}
		}
		// set toolbar actions enabled/disabled
		updateToolBarActionsEnablement();
	}

	@Override
	protected void addListeners() {
		super.addListeners();

		// must directly listen for selection events from parent's selection
		// provider
		// to ensure that we get the selection event from the tree viewer pane
		// even
		// if the view does not have focuse
		fParent.getSite().getSelectionProvider().addSelectionChangedListener(this);
	}

	@Override
	protected void removeListeners() {
		super.removeListeners();
		fParent.getSite().getSelectionProvider().removeSelectionChangedListener(this);
	}

	@Override
	public void widgetSelected(SelectionEvent e) {

		if (getTopMemoryTab() == null) {
			return;
		}

		IMemoryRendering rendering = getTopMemoryTab().getRendering();

		if (rendering != null) {
			fSelectionProvider.setSelection(new StructuredSelection(rendering));
		}

	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}

	@Override
	public Object getCurrentSelection() {
		if (getTopMemoryTab() != null) {
			if (getTopMemoryTab().getRendering() != null) {
				return getTopMemoryTab().getRendering();
			}
		}
		return null;
	}

	@Override
	public IMemoryViewTab[] getAllViewTabs() {

		// otherwise, find the view tab to display
		CTabFolder folder = (CTabFolder) fStackLayout.topControl;
		CTabItem[] items = folder.getItems();

		IMemoryViewTab[] viewTabs = new IMemoryViewTab[folder.getItemCount()];

		for (int i = 0; i < items.length; i++) {
			viewTabs[i] = (IMemoryViewTab) items[i].getData();
		}

		return viewTabs;
	}

	@Override
	public void moveToTop(IMemoryViewTab viewTab) {

		IMemoryViewTab lastViewTab = getTopMemoryTab();

		if (viewTab == lastViewTab) {
			return;
		}

		// otherwise, find the view tab to display
		CTabFolder folder = (CTabFolder) fStackLayout.topControl;
		CTabItem[] items = folder.getItems();

		for (int i = 0; i < items.length; i++) {
			IMemoryViewTab tab = (IMemoryViewTab) items[i].getData();
			if (viewTab == tab) {

				boolean isEnabled = lastViewTab.isEnabled();

				// switch to that viewTab
				lastViewTab.setEnabled(false);
				folder.setSelection(i);

				setRenderingSelection(tab.getRendering());

				getTopMemoryTab().setEnabled(isEnabled && fVisible);
				break;
			}
		}
	}

	private CTabFolder createTabFolder(Composite parent) {
		CTabFolder folder = new CTabFolder(parent, SWT.NO_REDRAW_RESIZE | SWT.NO_TRIM | SWT.FLAT);

		ColorRegistry reg = JFaceResources.getColorRegistry();
		Color c1 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_START"), //$NON-NLS-1$
		c2 = reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_BG_END"); //$NON-NLS-1$
		folder.setSelectionBackground(new Color[] { c1, c2 }, new int[] { 100 }, true);
		folder.setSelectionForeground(reg.get("org.eclipse.ui.workbench.ACTIVE_TAB_TEXT_COLOR")); //$NON-NLS-1$
		folder.setBorderVisible(true);
		folder.setFont(fViewPaneCanvas.getFont());

		folder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				if (event.item.getData() instanceof MemoryViewTab) {
					RenderingViewPane.this.removeMemoryRendering(((MemoryViewTab) event.item.getData()).getRendering());
				}
				event.doit = false;
			}
		});
		return folder;
	}

	@Override
	public void restoreViewPane() {

		// get current selection from memory view

		ISelection selection = null;
		if (fParent.getSite().getSelectionProvider() != null) {
			selection = fParent.getSite().getSelectionProvider().getSelection();
		}

		IMemoryBlock memoryBlock = null;
		if (selection != null) {
			memoryBlock = getMemoryBlock(selection);
		}

		if (memoryBlock == null) {
			// get selection from this view
			selection = fSelectionProvider.getSelection();

			if (MemoryViewUtil.isValidSelection(selection)) {
				Object elem = ((IStructuredSelection) selection).getFirstElement();

				if (!(elem instanceof IMemoryBlock)) {
					return;
				}

				memoryBlock = (IMemoryBlock) elem;
			}
		}

		if (memoryBlock == null) {
			// get a memory block from current debug context
			IAdaptable context = DebugUITools.getPartDebugContext(fParent.getSite());
			if (context != null) {
				IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(context);

				if (retrieval != null) {
					IMemoryBlock[] blocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(retrieval);
					if (blocks.length > 0) {
						memoryBlock = blocks[0];
					}
				}
			}
		}

		if (memoryBlock != null) {
			if (!fTabFolderForMemoryBlock.containsKey(memoryBlock)) {
				// create tab folder if a tab folder does not already exist
				// for the memory block
				CTabFolder folder = createTabFolder(fViewPaneCanvas);

				fTabFolderForMemoryBlock.put(memoryBlock, folder);
				fMemoryBlockFromTabFolder.put(folder, memoryBlock);
				setTabFolder(fTabFolderForMemoryBlock.get(memoryBlock));
				IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(memoryBlock);
				if (retrieval != null) {
					fTabFolderForDebugView.put(MemoryViewUtil.getHashCode(retrieval), fTabFolderForMemoryBlock.get(memoryBlock));
				} else {
					DebugUIPlugin.logErrorMessage("Memory block retrieval for memory block is null."); //$NON-NLS-1$
				}

				fViewPaneCanvas.layout();
				fAddedMemoryBlocks.add(memoryBlock);

				// every time we create a folder, we have to create a
				// CreateRendering
				newCreateRenderingForFolder(memoryBlock, folder);
			}

			if (fTabFolderForMemoryBlock.containsKey(memoryBlock)) {
				CTabFolder toDisplay = fTabFolderForMemoryBlock.get(memoryBlock);

				if (toDisplay != null) {
					setTabFolder(toDisplay);
					IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(memoryBlock);

					if (retrieval != null) {
						fTabFolderForDebugView.put(MemoryViewUtil.getHashCode(retrieval), toDisplay);
					} else {
						DebugUIPlugin.logErrorMessage("Memory block retrieval is null for memory block."); //$NON-NLS-1$
					}

					fViewPaneCanvas.layout();

					// restore view tabs
					IMemoryRendering[] renderings = fRenderingMgr.getRenderingsFromMemoryBlock(memoryBlock);

					// if only CreateRendering is present, restore renderings
					if (isRestoreViewTabs(toDisplay)) {
						restoreViewTabs(renderings);
					}
				}
			}

			// disable current storag block

			IMemoryViewTab top = getTopMemoryTab();

			if (top != null) {
				top.setEnabled(fVisible);
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();

		fTabFolderForMemoryBlock.clear();
		fTabFolderForMemoryBlock = null;

		fMemoryBlockFromTabFolder.clear();
		fMemoryBlockFromTabFolder = null;

		fRenderingMgr.dispose();
		fRenderingMgr = null;

		fAddedMemoryBlocks.clear();
		fAddedRenderings.clear();
	}

	public Control createViewPane(Composite parent, String paneId, String label, boolean canAddRendering, boolean canRemoveRendering) {
		return doCreateViewPane(parent, paneId, label, canAddRendering, canRemoveRendering);
	}

	@Override
	public Control createViewPane(Composite parent, String paneId, String label) {
		return doCreateViewPane(parent, paneId, label, true, true);
	}

	/**
	 * @param parent
	 * @param paneId
	 * @param label
	 * @param canAddRendering
	 * @param canRemoveRendering
	 * @return
	 */
	private Control doCreateViewPane(Composite parent, String paneId, String label, boolean canAddRendering, boolean canRemoveRendering) {
		Control control = super.createViewPane(parent, paneId, label);
		fCanAddRendering = canAddRendering;
		fCanRemoveRendering = canRemoveRendering;
		fRenderingMgr = new ViewPaneRenderingMgr(this);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IDebugUIConstants.PLUGIN_ID + ".MemoryRenderingView_context"); //$NON-NLS-1$
		return control;
	}

	@Override
	public IAction[] getActions() {
		return new IAction[0];
	}

	// enable/disable toolbar action
	protected void updateToolBarActionsEnablement() {
	}

	@Override
	protected void emptyFolder() {
		super.emptyFolder();
		updateToolBarActionsEnablement();
		fSelectionProvider.setSelection(AbstractMemoryViewPane.EMPTY);
	}

	@Override
	public void addMemoryRendering(IMemoryRendering rendering) {

		if (rendering == null) {
			return;
		}

		memoryBlockRenderingAdded(rendering);
		fRenderingMgr.addMemoryBlockRendering(rendering);

	}

	@Override
	public void removeMemoryRendering(IMemoryRendering rendering) {

		if (rendering == null) {
			return;
		}

		memoryBlockRenderingRemoved(rendering);

		if (fRenderingMgr != null) {
			fRenderingMgr.removeMemoryBlockRendering(rendering);
		}

	}

	private RenderingViewPane getInstance() {
		return this;
	}

	private IMemoryBlock getMemoryBlock(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}

		// only single selection of PICLDebugElements is allowed for this action
		if (selection.isEmpty() || ((IStructuredSelection) selection).size() > 1) {
			return null;
		}

		Object elem = ((IStructuredSelection) selection).getFirstElement();

		if (elem instanceof IMemoryBlock) {
			return (IMemoryBlock) elem;
		} else if (elem instanceof IMemoryRendering) {
			return ((IMemoryRendering) elem).getMemoryBlock();
		} else {
			return null;
		}
	}

	private void deactivateRendering(IMemoryViewTab viewTab) {
		if (viewTab == null) {
			return;
		}

		if (!viewTab.isDisposed()) {
			viewTab.getRendering().deactivated();
		}
	}

	@Override
	public IMemoryRenderingSite getMemoryRenderingSite() {
		return fRenderingSite;
	}

	@Override
	public String getId() {
		return getPaneId();
	}

	@Override
	public IMemoryRendering[] getRenderings() {
		return fRenderingMgr.getRenderings();
	}

	@Override
	public IMemoryRendering getActiveRendering() {
		if (getTopMemoryTab() == null) {
			return null;
		}
		return getTopMemoryTab().getRendering();
	}

	/**
	 * Reset the memory renderings within this view pane.
	 *
	 * @param memoryBlock - reset renderings associated with the given memory
	 *            block
	 * @param resetVisible - reset what's currently visible if the parameter is
	 *            true. Otherwise, the view pane will reset all renderings
	 *            associated with the given memory block.
	 */
	public void resetRenderings(IMemoryBlock memoryBlock, boolean resetVisible) {
		// if we only reset what's visible and the view pane is not visible
		// do nothing.
		if (resetVisible && !isVisible()) {
			return;
		}

		if (resetVisible) {
			IMemoryRendering rendering = getActiveRendering();
			if (rendering != null) {
				if (rendering.getMemoryBlock() == memoryBlock) {
					if (rendering instanceof IResettableMemoryRendering) {
						IResettableMemoryRendering resettableRendering = (IResettableMemoryRendering) rendering;
						try {
							resettableRendering.resetRendering();
						} catch (DebugException e) {
							// do not pop up error message
							// error message is annoying where there are
							// multiple rendering
							// panes and renderings to reset
						}
					}
				}
			}
		} else {
			// get all renderings associated with the given memory block
			IMemoryRendering[] renderings = fRenderingMgr.getRenderingsFromMemoryBlock(memoryBlock);

			// back up current synchronization provider
			IMemoryRendering originalProvider = null;
			IMemoryRenderingSynchronizationService service = getMemoryRenderingSite().getSynchronizationService();
			if (service != null) {
				originalProvider = service.getSynchronizationProvider();
			}

			for (IMemoryRendering rendering : renderings) {
				if (rendering instanceof IResettableMemoryRendering) {
					try {
						// This is done to allow user to select multiple memory
						// monitors and
						// reset their renderings.
						// In this case, a hidden rendering will not be the sync
						// provider to the sync
						// service. When the reset happens, the top visible
						// address and selected
						// address is not updated in the sync service. When the
						// rendering later
						// becomes visible, the rendering gets the sync info
						// from the sync service
						// and will try to sync up with old information, giving
						// user the impression
						// that the rendering was never reset. By forcing the
						// rendering that we
						// are trying to reset as the synchronization provider,
						// we ensure that
						// the rendering is able to update its sync info even
						// though the rendering
						// is currently hidden.
						if (service != null) {
							service.setSynchronizationProvider(rendering);
						}
						((IResettableMemoryRendering) rendering).resetRendering();
					}catch (DebugException e) {
						// do not pop up error message
						// error message is annoying where there are multiple
						// rendering
						// panes and renderings to reset
					}
				}
			}

			// restore synchronization provider
			if (service != null) {
				service.setSynchronizationProvider(originalProvider);
			}
		}
	}

	public void showCreateRenderingTab() {
		IMemoryRendering activeRendering = RenderingViewPane.this.getActiveRendering();
		if (activeRendering == null) {
			return;
		}

		IMemoryBlock memoryblk = activeRendering.getMemoryBlock();

		final CTabFolder tabFolder = fTabFolderForMemoryBlock.get(memoryblk);
		if (tabFolder != null) {
			Display.getDefault().asyncExec(() -> {
				int index = getIndexOfCreateRenderingTab(tabFolder);
				if (index >= 0) {
					tabFolder.setSelection(index);
				}
			});
		}
	}

	public void contextActivated(final ISelection selection) {

		UIJob job = new UIJob("contextActivated") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (isDisposed()) {
					return Status.OK_STATUS;
				}

				IMemoryViewTab lastViewTab = getTopMemoryTab();

				if (MemoryViewUtil.isValidSelection(selection)) {
					if (!(selection instanceof IStructuredSelection)) {
						return Status.OK_STATUS;
					}

					Object elem = ((IStructuredSelection) selection).getFirstElement();

					if (elem instanceof IAdaptable) {
						handleDebugElementSelection(lastViewTab, (IAdaptable) elem);
					}
				} else {
					if (lastViewTab != null) {
						lastViewTab.setEnabled(false);
					}

					if (fStackLayout.topControl != fEmptyTabFolder) {
						emptyFolder();
					}

				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/**
	 * @param memory
	 */
	private CTabFolder createFolderForMemoryBlock(IMemoryBlock memory) {
		CTabFolder folder = createTabFolder(fViewPaneCanvas);

		fTabFolderForMemoryBlock.put(memory, folder);
		fMemoryBlockFromTabFolder.put(folder, memory);

		IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(memory);
		if (retrieval != null) {
			fTabFolderForDebugView.put(MemoryViewUtil.getHashCode(retrieval), folder);
		} else {
			DebugUIPlugin.logErrorMessage("Memory block retrieval for memory block is null"); //$NON-NLS-1$
		}

		newCreateRenderingForFolder(memory, folder);

		return folder;
	}

	private void newCreateRenderingForFolder(IMemoryBlock memory, CTabFolder folder) {

		if (!canAddRendering()) {
			return;
		}

		CTabItem newItem = new CTabItem(folder, SWT.NONE);
		CreateRendering rendering = new CreateRendering(getInstance());
		rendering.init(getInstance(), memory);
		new MemoryViewTab(newItem, rendering, getInstance());
		folder.setSelection(0);
	}

	@Override
	public void debugContextChanged(DebugContextEvent event) {
		if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
			contextActivated(event.getContext());
		}
	}

	/**
	 * @return whether this container allows user to add rendering manually
	 * @since 3.4
	 */
	public boolean canAddRendering() {
		return fCanAddRendering;
	}

	/**
	 * @return whether this container allows user to remove rendering manually
	 * @since 3.4
	 */
	public boolean canRemoveRendering() {
		return fCanRemoveRendering;
	}

	/**
	 * @param tabFolder
	 * @param index
	 * @return
	 */
	private CTabItem createTab(CTabFolder tabFolder, int index) {
		int swtStyle = SWT.CLOSE;
		if (!canRemoveRendering()) {
			swtStyle = SWT.NONE;
		}
		CTabItem tab = new CTabItem(tabFolder, swtStyle, index);
		return tab;
	}
}
