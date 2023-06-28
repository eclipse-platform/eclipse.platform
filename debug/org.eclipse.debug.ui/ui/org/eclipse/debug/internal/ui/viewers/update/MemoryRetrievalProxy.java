/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *
 *******************************************************************************/

package org.eclipse.debug.internal.ui.viewers.update;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.UIJob;

public class MemoryRetrievalProxy extends AbstractModelProxy implements IMemoryBlockListener {
	private IMemoryBlockRetrieval fRetrieval;

	public MemoryRetrievalProxy(IMemoryBlockRetrieval retrieval) {
		fRetrieval = retrieval;

	}

	@Override
	public void memoryBlocksAdded(IMemoryBlock[] memory) {
		IMemoryBlock[] allMB = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		int lastIndex = allMB.length - memory.length;
		ModelDelta delta = new ModelDelta(fRetrieval, 0, IModelDelta.NO_CHANGE, allMB.length);

		for (int i = 0; i < memory.length; i++) {
			IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(memory[i]);

			if (retrieval != null) {
				if (retrieval == fRetrieval) {
					// select the newly added memory block
					delta.addNode(memory[i], lastIndex + i, IModelDelta.ADDED | IModelDelta.SELECT, 0);
				}
			}
		}

		fireModelChanged(delta);
	}

	@Override
	public void memoryBlocksRemoved(final IMemoryBlock[] memory) {

		UIJob job = new UIJob("memory blocks removed") { //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				ModelDelta delta = new ModelDelta(fRetrieval, IModelDelta.NO_CHANGE);

				// find a memory block to select
				for (IMemoryBlock block : memory) {
					IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(block);
					if (retrieval != null) {
						if (retrieval == fRetrieval) {
							// do not change selection if the memory block
							// removed is not
							// currently selected
							// #getCurrentSelection must be run on the UI thread
							if (isMemoryBlockSelected(getCurrentSelection(), block)) {
								addSelectDeltaNode(delta);
							}
							delta.addNode(block, IModelDelta.REMOVED);
						}
					}
				}

				fireModelChanged(delta);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	@Override
	public void init(IPresentationContext context) {
		super.init(context);
		DebugPlugin.getDefault().getMemoryBlockManager().addListener(this);
	}

	@Override
	public void installed(Viewer viewer) {
		super.installed(viewer);

		// Set the initial selection when the proxy is installed
		// Otherwise, rendering pane cannot be populated.
		setInitialSelection();
	}

	/**
	 * Set the initial memory block selection when the proxy is installed. This
	 * is done to ensure that when the memory view is opened, there is an
	 * initial selection. Otherwise, the Rendering Pane will show up as blank.
	 *
	 * @since 3.4
	 */
	protected void setInitialSelection() {
		IMemoryBlock[] allMB = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		if (allMB.length > 0) {
			ModelDelta delta = new ModelDelta(fRetrieval, 0, IModelDelta.NO_CHANGE, allMB.length);
			delta.addNode(allMB[0], 0, IModelDelta.SELECT, 0);
			fireModelChanged(delta);
		}
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getMemoryBlockManager().removeListener(this);
	}

	private void addSelectDeltaNode(ModelDelta delta) {
		IMemoryBlock[] memoryBlocks = DebugPlugin.getDefault().getMemoryBlockManager().getMemoryBlocks(fRetrieval);
		if (memoryBlocks != null && memoryBlocks.length > 0) {
			delta.addNode(memoryBlocks[0], IModelDelta.SELECT);
		}
	}

	private IStructuredSelection getCurrentSelection() {
		Viewer viewer = getViewer();
		if (viewer instanceof StructuredViewer) {
			StructuredViewer sv = (StructuredViewer) viewer;
			ISelection selection = sv.getSelection();
			if (selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}
		}
		return StructuredSelection.EMPTY;
	}

	private boolean isMemoryBlockSelected(IStructuredSelection selection, IMemoryBlock memoryBlock) {
		if (!selection.isEmpty()) {
			Iterator<?> iter = selection.iterator();
			while (iter.hasNext()) {
				Object sel = iter.next();
				if (sel == memoryBlock) {
					return true;
				}

				if (sel instanceof IMemoryRendering) {
					if (((IMemoryRendering) sel).getMemoryBlock() == memoryBlock) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
