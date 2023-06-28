/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingBindingsListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Dialog allowing user to add a memory rendering
 */
public class AddMemoryRenderingDialog extends SelectionDialog {

	private IMemoryBlock[] fMemoryBlocks;
	private Combo memoryBlock;
	private ListViewer fViewer;
	private IMemoryBlock fSelectedMemoryBlock;
	private Button addNew;

	private ISelectionChangedListener fSelectionChangedListener;
	private SelectionListener fSelectionListener;
	private SelectionAdapter fAddNewSelectionAdapter;
	private IMemoryRenderingSite fSite;

	private IMemoryBlockListener fMemoryBlockListener = new IMemoryBlockListener() {

		@Override
		public void memoryBlocksAdded(final IMemoryBlock[] memory) {
			if (memory.length > 0) {
				IMemoryBlock currentBlock = getMemoryBlockToSelect(memory[0]);
				if (currentBlock == null) {
					addNew();
				} else {
					populateDialog(currentBlock);
				}
			}
		}

		@Override
		public void memoryBlocksRemoved(IMemoryBlock[] memory) {
		}
	};

	private IMemoryRenderingBindingsListener fBindingListener = () -> {
		UIJob job = new UIJob("refresh") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				fViewer.refresh();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	};

	static class MemoryRenderingLabelProvider implements ILabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IMemoryRenderingType) {
				String label = ((IMemoryRenderingType) element).getLabel();
				return label;
			}
			return element.toString();
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

	}

	static class MemoryRenderingContentProvider implements IStructuredContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			IMemoryRenderingType[] renderings = DebugUITools.getMemoryRenderingManager().getRenderingTypes((IMemoryBlock) inputElement);
			return renderings;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	@Override
	public boolean close() {

		fViewer.removeSelectionChangedListener(fSelectionChangedListener);
		memoryBlock.removeSelectionListener(fSelectionListener);
		addNew.removeSelectionListener(fAddNewSelectionAdapter);
		DebugPlugin.getDefault().getMemoryBlockManager().removeListener(fMemoryBlockListener);
		DebugUITools.getMemoryRenderingManager().removeListener(fBindingListener);

		return super.close();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	@Override
	public Object[] getResult() {

		Object[] results = super.getResult();

		if (results != null) {
			Object[] renderings = ((IStructuredSelection) results[0]).toArray();
			return renderings;
		}
		return new Object[0];
	}

	@Override
	protected void cancelPressed() {

		setResult(null);

		super.cancelPressed();
	}

	@Override
	protected void okPressed() {

		ISelection select = fViewer.getSelection();
		setSelectionResult(new Object[] { select });

		super.okPressed();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, DebugUIPlugin.getUniqueIdentifier() + ".AddMemoryRenderingDialog_context"); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.numColumns = 3;
		compositeLayout.makeColumnsEqualWidth = true;
		composite.setLayout(compositeLayout);

		GridData comositeSpec = new GridData();
		comositeSpec.grabExcessVerticalSpace = true;
		comositeSpec.grabExcessHorizontalSpace = true;
		comositeSpec.horizontalAlignment = GridData.FILL;
		comositeSpec.verticalAlignment = GridData.CENTER;
		composite.setLayoutData(comositeSpec);

		Label textLabel = new Label(composite, SWT.NONE);
		textLabel.setText(DebugUIMessages.AddMemoryRenderingDialog_Memory_Monitor);
		GridData textLayout = new GridData();
		textLayout.verticalAlignment = GridData.CENTER;
		textLayout.horizontalAlignment = GridData.BEGINNING;
		textLabel.setLayoutData(textLayout);

		memoryBlock = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		GridData spec = new GridData(GridData.FILL_HORIZONTAL);
		spec.grabExcessVerticalSpace = false;
		spec.grabExcessHorizontalSpace = false;
		spec.horizontalAlignment = GridData.FILL;
		spec.verticalAlignment = GridData.FILL;
		spec.horizontalSpan = 4;
		memoryBlock.setLayoutData(spec);

		Label filler = new Label(composite, SWT.NONE);
		filler.setText(" "); //$NON-NLS-1$
		GridData fillerData = new GridData(GridData.FILL_HORIZONTAL);
		fillerData.horizontalSpan = 2;
		filler.setLayoutData(fillerData);

		addNew = new Button(composite, SWT.NONE);
		addNew.setText(DebugUIMessages.AddMemoryRenderingDialog_Add_New);
		GridData specButton = new GridData();
		specButton.horizontalAlignment = GridData.END;
		specButton.verticalAlignment = GridData.CENTER;
		addNew.setLayoutData(specButton);

		fAddNewSelectionAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				RetargetAddMemoryBlockAction action = new RetargetAddMemoryBlockAction(fSite, false);
				action.run();
				action.dispose();
			}
		};

		addNew.addSelectionListener(fAddNewSelectionAdapter);

		fSelectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				int idx = memoryBlock.getSelectionIndex();

				// avoid null pointer exception
				if (fMemoryBlocks == null) {
					return;
				}

				fSelectedMemoryBlock = fMemoryBlocks[idx];

				fViewer.setInput(fSelectedMemoryBlock);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		memoryBlock.addSelectionListener(fSelectionListener);

		Label renderingLabel = new Label(composite, SWT.NONE);
		renderingLabel.setText(DebugUIMessages.AddMemoryRenderingDialog_Memory_renderings);
		GridData renderingLayout = new GridData();
		renderingLayout.horizontalAlignment = GridData.BEGINNING;
		renderingLayout.verticalAlignment = GridData.CENTER;
		renderingLayout.horizontalSpan = 3;
		renderingLabel.setLayoutData(renderingLayout);

		fViewer = new ListViewer(composite);
		fViewer.setContentProvider(new MemoryRenderingContentProvider());
		fViewer.setLabelProvider(new MemoryRenderingLabelProvider());

		GridData listLayout = new GridData(GridData.FILL_BOTH);
		listLayout.horizontalSpan = 3;
		listLayout.heightHint = 140;
		fViewer.getControl().setLayoutData(listLayout);

		fViewer.addDoubleClickListener(event -> okPressed());

		IMemoryBlock currentBlock = getMemoryBlockToSelect(null);
		if (currentBlock == null) {
			addNew();
		} else {
			populateDialog(currentBlock);
		}

		fSelectionChangedListener = event -> {
			ISelection selection = fViewer.getSelection();

			if (selection.isEmpty()) {
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			} else {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		};

		fViewer.addSelectionChangedListener(fSelectionChangedListener);

		DebugPlugin.getDefault().getMemoryBlockManager().addListener(fMemoryBlockListener);
		DebugUITools.getMemoryRenderingManager().addListener(fBindingListener);

		return composite;
	}

	public AddMemoryRenderingDialog(Shell parent, IMemoryRenderingSite site) {
		super(parent);
		super.setTitle(DebugUIMessages.AddMemoryRenderingDialog_Add_memory_rendering);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fSite = site;
	}

	private void doPopulateDialog(Combo combo, ListViewer viewer, String[] labels, int selectionIdx, IMemoryBlock currentBlock) {
		// clean up
		combo.removeAll();

		for (String label : labels) {
			combo.add(label);
		}

		combo.select(selectionIdx);
		fSelectedMemoryBlock = currentBlock;

		viewer.setInput(currentBlock);
	}

	private IMemoryBlock getMemoryBlockToSelect(IMemoryBlock lastAdded) {
		IMemoryBlock currentBlock = null;

		if (lastAdded != null) {
			currentBlock = lastAdded;
		} else {
			// take Memory View's selection if possible
			ISelectionProvider selectionProvider = fSite.getSite().getSelectionProvider();
			ISelection selection = null;

			if (selectionProvider != null) {
				selection = selectionProvider.getSelection();
			} else {
				// otherwise, take selection from selection service
				selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection(IDebugUIConstants.ID_MEMORY_VIEW);
			}

			IMemoryBlock element = getMemoryBlock(selection);

			if (element == null) {
				IAdaptable context = DebugUITools.getPartDebugContext(fSite.getSite());

				if (context != null) {
					IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(context);

					if (retrieval == null) {
						return currentBlock;
					}

					IMemoryBlock[] blocks = new IMemoryBlock[0];

					if (retrieval != null) {
						blocks = MemoryViewUtil.getMemoryBlockManager().getMemoryBlocks(retrieval);
					}

					if (blocks.length > 0) {
						currentBlock = blocks[0];
					}
				}
			} else {
				currentBlock = element;
			}
		}
		return currentBlock;
	}

	private String[] getLabels(IMemoryBlock[] memoryBlocks) {
		String[] labels = new String[memoryBlocks.length];
		for (int i = 0; i < memoryBlocks.length; i++) {
			String text = IInternalDebugCoreConstants.EMPTY_STRING;
			if (memoryBlocks[i] instanceof IMemoryBlockExtension) {
				try {
					text = ((IMemoryBlockExtension) memoryBlocks[i]).getExpression();

					if (text == null) {
						text = DebugUIMessages.AddMemoryRenderingDialog_Unknown;
					}

					if (((IMemoryBlockExtension) memoryBlocks[i]).getBigBaseAddress() != null) {
						text += " : 0x"; //$NON-NLS-1$
						text += ((IMemoryBlockExtension) memoryBlocks[i]).getBigBaseAddress().toString(16);
					}
				} catch (DebugException e) {
					long address = memoryBlocks[i].getStartAddress();
					text = Long.toHexString(address);
				}
			} else {
				long address = memoryBlocks[i].getStartAddress();
				text = Long.toHexString(address);
			}

			// ask decorator to decorate to ensure consistent label
			ILabelDecorator decorator = fMemoryBlocks[i].getAdapter(ILabelDecorator.class);
			if (decorator != null) {
				text = decorator.decorateText(text, fMemoryBlocks[i]);
			}

			labels[i] = text;
		}
		return labels;
	}

	private IMemoryBlock getMemoryBlock(ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			return null;
		}

		// only single selection of debug element is allowed for this action
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

	public IMemoryBlock getMemoryBlock() {
		return fSelectedMemoryBlock;
	}

	/**
	 * @param currentBlock the current memory block context
	 */
	private void populateDialog(IMemoryBlock currentBlock) {
		final IMemoryBlock selectMB = currentBlock;
		Job job = new Job("Populate dialog") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IMemoryBlockRetrieval mbRetrieval = MemoryViewUtil.getMemoryBlockRetrieval(selectMB);

				if (mbRetrieval != null) {
					fMemoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(mbRetrieval);
					int selectionIdx = 0;
					for (int i = 0; i < fMemoryBlocks.length; i++) {
						if (fMemoryBlocks[i] == selectMB) {
							selectionIdx = i;
							break;
						}
					}

					final String[] labels = getLabels(fMemoryBlocks);
					final int idx = selectionIdx;
					final IMemoryBlock selectedBlk = selectMB;
					WorkbenchJob wbJob = new WorkbenchJob("populate dialog") { //$NON-NLS-1$

						@Override
						public IStatus runInUIThread(IProgressMonitor wbMonitor) {
							doPopulateDialog(memoryBlock, fViewer, labels, idx, selectedBlk);
							return Status.OK_STATUS;
						}
					};
					wbJob.setSystem(true);
					wbJob.schedule();
				} else {
					DebugUIPlugin.logErrorMessage("Unable to obtain memory block retrieval."); //$NON-NLS-1$
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/**
	 *
	 */
	private void addNew() {
		WorkbenchJob job = new WorkbenchJob("populate dialog") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				memoryBlock.add(DebugUIMessages.AddMemoryRenderingDialog_Add_New);
				memoryBlock.select(0);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}
}
