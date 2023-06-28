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
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.internal.ui.memory.provisional.MemoryViewPresentationContext;
import org.eclipse.debug.internal.ui.viewers.TableUpdatePolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelChangedListener;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.ui.progress.UIJob;

/**
 * This update policy updates immediately after a model changed event.  The update policy will
 * only update if the rendering is visible.  Cache from the content manager is cleared
 * when the memory block has changed when the rendering is not visible
 *
 */
public class AsyncTableRenderingUpdatePolicy extends TableUpdatePolicy
{
	@Override
	public void modelChanged(IModelDelta node, IModelProxy proxy) {

		// clear current cache as it becomes invalid when the memory block is changed
		AbstractVirtualContentTableModel model = getTableViewer().getVirtualContentModel();

		if (model != null)
		{
			IContentChangeComputer computer = null;
			if (model instanceof IContentChangeComputer)
				computer = (IContentChangeComputer)model;

			clearCache(computer);

			if (!containsEvent())
			{
				return;
			}

			if (node.getElement() instanceof IMemoryBlock && (node.getFlags() & IModelDelta.CONTENT) != 0)
			{
				if (computer != null && getTableViewer() != null)
				{
					// only cache if the rendering is not currently displaying error
					if (!getTableViewer().getRendering().isDisplayingError())
					{
						// cache visible elelements
						computer.cache(model.getElements());
					}
				}

				// update policy figured out what's changed in the memory block
				// and will tell rendering to update accordinly.
				// Updating the rendering indirectly update the table viewer
				notifyRendering(node, proxy);
				handleMemoryBlockChanged((IMemoryBlock)node.getElement(), node);
				return;

			}
			else if (node.getElement() instanceof IMemoryBlock && (node.getFlags() & IModelDelta.STATE) != 0)
			{
				// override handling of state change event
				// let the super class deals with the rest of the changes
				handleMemoryBlockChanged((IMemoryBlock)node.getElement(), node);
				return;
			}
		}

		super.modelChanged(node, proxy);
	}

	/**
	 * @param computer the change computer to clear the cache for
	 */
	protected void clearCache(IContentChangeComputer computer) {
		if (computer != null)
			computer.clearCache();
	}

	private void notifyRendering(IModelDelta node, IModelProxy proxy) {
		if (getTableViewer() != null)
		{
			IModelChangedListener listener = getTableViewer().getRendering().getAdapter(IModelChangedListener.class);
			if (listener != null)
				listener.modelChanged(node, proxy);
		}
	}

	protected void handleMemoryBlockChanged(IMemoryBlock mb, IModelDelta delta)
	{
		try {
			if (getViewer().getPresentationContext() instanceof MemoryViewPresentationContext)
			{
				MemoryViewPresentationContext context = (MemoryViewPresentationContext)getViewer().getPresentationContext();
				final AbstractAsyncTableRendering rendering = getTableRendering(context);
				if (rendering != null)
				{
					if ((delta.getFlags() & IModelDelta.CONTENT) != 0)
					{
						TableRenderingContentDescriptor descriptor = rendering.getAdapter(TableRenderingContentDescriptor.class);

						if (descriptor != null)
						{
							final BigInteger address = getMemoryBlockBaseAddress(mb);
							if (!descriptor.isMemoryBlockBaseAddressInitialized() || !address.equals(descriptor.getContentBaseAddress()))
							{
								descriptor.updateContentBaseAddress();
								UIJob job = new UIJob("go to address"){ //$NON-NLS-1$

									@Override
									public IStatus runInUIThread(IProgressMonitor monitor) {
										try {
											rendering.goToAddress(address);
										} catch (DebugException e) {
											if (getTableViewer() != null)
												getTableViewer().handlePresentationFailure(null, e.getStatus());
										}
										return Status.OK_STATUS;
									}};
								job.setSystem(true);
								job.schedule();
							}
							else
							{
								rendering.refresh();
							}
						}
					}
					else
					{
						rendering.updateLabels();
					}
				}
			}
		} catch (DebugException e) {
			if (getTableViewer() != null)
				getTableViewer().handlePresentationFailure(null, e.getStatus());
		}
	}

	private BigInteger getMemoryBlockBaseAddress(IMemoryBlock mb) throws DebugException
	{
		if (mb instanceof IMemoryBlockExtension)
			return ((IMemoryBlockExtension)mb).getBigBaseAddress();
		else
			return BigInteger.valueOf(mb.getStartAddress());
	}

	private AsyncTableRenderingViewer getTableViewer()
	{
		if (getViewer() instanceof AsyncTableRenderingViewer)
			return (AsyncTableRenderingViewer)getViewer();
		return null;
	}

	private boolean containsEvent()
	{
		if (getViewer().getPresentationContext() instanceof MemoryViewPresentationContext)
		{
			MemoryViewPresentationContext context = (MemoryViewPresentationContext) getViewer().getPresentationContext();
			if (context.getRendering() instanceof AbstractAsyncTableRendering)
			{
				AbstractAsyncTableRendering rendering = (AbstractAsyncTableRendering)context.getRendering();
				if (!rendering.isVisible())
					return false;
			}
		}
		return true;
	}

	protected AbstractAsyncTableRendering getTableRendering(MemoryViewPresentationContext context)
	{
		IMemoryRendering memRendering = context.getRendering();
		if (memRendering != null && memRendering instanceof AbstractAsyncTableRendering)
		{
			return (AbstractAsyncTableRendering)memRendering;
		}
		return null;
	}
}