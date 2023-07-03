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
 *     Wind River - Bug 206407 AbstractMemoryRendering connect()s the wrong "this"
 *******************************************************************************/
package org.eclipse.debug.ui.memory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.views.memory.PropertyChangeNotifier;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Abstract implementation of a memory rendering.
 * <p>
 * To contribute an action to a rendering, an <code>objectContribution</code> can
 * be used on a rendering implementation class itself using a
 * <code>popupMenus</code> extension. Additionally, the context menu created
 * by <code>createPopupMenu()</code> is registered with an identifier of this
 * rendering's container identifier. Actions may also be contributed to the
 * container's context menu specifically by using a  <code>viewerContribution</code>
 * on a <code>popupMenus</code> extension that has a <code>targetID</code> referring
 * to this rendering container's identifier.
 * </p>
 * <p>
 * Clients implementing memory renderings must subclass this class.
 * </p>
 * @since 3.1
 */
public abstract class AbstractMemoryRendering extends PlatformObject implements IMemoryRendering{

	private IMemoryBlock fMemoryBlock;
	private IMemoryRenderingContainer fContainer;
	private ListenerList<IPropertyChangeListener> fPropertyListeners;
	private boolean fVisible = true;
	private MenuManager fPopupMenuMgr;
	private String fRenderingId;

	private class ConnectionJob extends Job
	{
		Runnable fRunnable;
		ConnectionJob(Runnable runnable)
		{
			super("Connect/Disconnect MemoryBlock"); //$NON-NLS-1$
			fRunnable = runnable;
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			fRunnable.run();
			return Status.OK_STATUS;
		}
	}

	/**
	 * Client may provide a label decorator adapter from its memory block
	 * to decorate the label of a rendering.
	 * @since 3.2
	 */
	private ILabelDecorator fLabelDecorator;
	private IMenuListener fMenuListener;

	/**
	 * Constructs a new rendering of the given type.
	 *
	 * @param renderingId memory rendering type identifier
	 */
	public AbstractMemoryRendering(String renderingId)
	{
		fRenderingId = renderingId;
	}

	@Override
	public void init(IMemoryRenderingContainer container, IMemoryBlock block) {
		fContainer = container;
		fMemoryBlock = block;

		fLabelDecorator = fMemoryBlock.getAdapter(ILabelDecorator.class);
	}

	@Override
	public void dispose()
	{
		// disconnect from memory block when rendering is disposed
		if (fMemoryBlock instanceof IMemoryBlockExtension)
		{
			Runnable runnable = () -> ((IMemoryBlockExtension) fMemoryBlock).disconnect(AbstractMemoryRendering.this);
			new ConnectionJob(runnable).schedule();
		}

		if (fPopupMenuMgr != null)
		{
			fPopupMenuMgr.removeMenuListener(fMenuListener);
			fPopupMenuMgr.removeAll();
			fPopupMenuMgr.dispose();
			fPopupMenuMgr = null;
		}

		if (fPropertyListeners != null) {
			fPropertyListeners = null;
		}
	}

	@Override
	public void activated() {
		if (fContainer.getMemoryRenderingSite().getSynchronizationService() != null) {
			fContainer.getMemoryRenderingSite().getSynchronizationService().setSynchronizationProvider(this);
		}
	}

	@Override
	public void deactivated() {
		// do nothing when deactivated
		// we do not want to set the sync provider from rendering site
		// to null because Linux GTK unexpectedly fires deactivated event.
		// If we reset the sync provider upon a deactivated event, it screws
		// up synchronization on Linux GTK.
	}

	@Override
	public void becomesVisible() {
		fVisible = true;

		if (fMemoryBlock instanceof IMemoryBlockExtension)
		{
			Runnable runnable = () -> ((IMemoryBlockExtension) fMemoryBlock).connect(AbstractMemoryRendering.this);
			new ConnectionJob(runnable).schedule();
		}
	}

	@Override
	public void becomesHidden() {
		fVisible = false;
		if (fMemoryBlock instanceof IMemoryBlockExtension)
		{
			Runnable runnable = () -> ((IMemoryBlockExtension) fMemoryBlock).disconnect(AbstractMemoryRendering.this);
			new ConnectionJob(runnable).schedule();
		}
	}

	@Override
	public IMemoryBlock getMemoryBlock() {
		return fMemoryBlock;
	}

	@Override
	public String getRenderingId()
	{
		return fRenderingId;
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {

		if (fPropertyListeners == null) {
			fPropertyListeners = new ListenerList<>();
		}

		fPropertyListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {

		if (fPropertyListeners == null) {
			return;
		}
		fPropertyListeners.remove(listener);
	}

	@Override
	public Image getImage() {
		return decorateImage(null);
	}

	/**
	 * Decorates and returns this rendering's image.
	 *
	 * @param image base image
	 * @return decorated image
	 * @since 3.2
	 */
	protected Image decorateImage(Image image) {
		if (fLabelDecorator != null) {
			return fLabelDecorator.decorateImage(image, this);
		}
		return image;
	}

	@Override
	public  String getLabel()
	{
		if (fMemoryBlock == null) {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}

		StringBuilder label = new StringBuilder(IInternalDebugCoreConstants.EMPTY_STRING);

		if (fMemoryBlock instanceof IMemoryBlockExtension)
		{
			String expression = ((IMemoryBlockExtension)fMemoryBlock).getExpression();

			if (expression == null) {
				expression = IInternalDebugCoreConstants.EMPTY_STRING;
			}

			label.append(expression);

			if (expression.startsWith("&")) //$NON-NLS-1$
			 {
				label.insert(0, "&"); //$NON-NLS-1$
			}

			// show full address if the rendering is visible
			// hide address if the rendering is invisible
			try {
				if (fVisible && ((IMemoryBlockExtension)fMemoryBlock).getBigBaseAddress() != null)
				{
					label.append(" : 0x"); //$NON-NLS-1$
					label.append(((IMemoryBlockExtension)fMemoryBlock).getBigBaseAddress().toString(16).toUpperCase());
				}
			} catch (DebugException e) {
				// do nothing... the label will not show memory block's address
			}
		}
		else
		{
			long address = fMemoryBlock.getStartAddress();
			label.append(Long.toHexString(address).toUpperCase());
		}

		IMemoryRenderingType type = DebugUITools.getMemoryRenderingManager().getRenderingType(getRenderingId());

		if (type != null)
		{
			String preName = type.getLabel();

			if (preName != null)
			{
				label.append(" <"); //$NON-NLS-1$
				label.append(preName);
				label.append(">");  //$NON-NLS-1$
			}
		}

		return decorateLabel(label.toString());
	}

	/**
	 * Decorates and returns this rendering's label.
	 *
	 * @param label base label
	 * @return decorated label
	 * @since 3.2
	 */
	protected String decorateLabel(String label) {
		if (fLabelDecorator != null) {
			return fLabelDecorator.decorateText(label, this);
		}
		return label;
	}

	/**
	 * Helper method for creating a pop up menu in the rendering for a control.
	 * Call this method when a context menu is required for a control
	 * in a rendering.
	 * <p>
	 * To contribute an action to a rendering, an <code>objectContribution</code> can
	 * be used on a rendering implementation class itself using a
	 * <code>popupMenus</code> extension. Additionally, the context menu created
	 * by this method is registered with an identifier of this rendering's container.
	 * Actions may also be contributed to the context menu specifically by using a
	 * <code>viewerContribution</code> on a <code>popupMenus</code> extension
	 * that has a <code>targetID</code> referring to this rendering container's identifier.
	 * </p>
	 * <p>
	 * Clients are expected to become a menu listener for their pop  up
	 * menu if they require to fill the context menu for the rendering.
	 * </p>
	 * @param control - control to create the pop up menu for
	 */
	protected void createPopupMenu(Control control)
	{
		if (fPopupMenuMgr == null)
		{
			fPopupMenuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
			fPopupMenuMgr.setRemoveAllWhenShown(true);
			IMemoryRenderingSite site = fContainer.getMemoryRenderingSite();
			String menuId = fContainer.getId();

			ISelectionProvider selProvider = site.getSite().getSelectionProvider();

			fMenuListener = manager -> manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			fPopupMenuMgr.addMenuListener(fMenuListener);

			site.getSite().registerContextMenu(menuId, fPopupMenuMgr, selProvider);
		}

		Menu popupMenu = fPopupMenuMgr.createContextMenu(control);
		control.setMenu(popupMenu);
	}

	/**
	 * Returns the pop up menu manager for this rendering, or <code>null</code>
	 * if none.
	 *
	 * @return the pop up menu manager for this rendering, or <code>null</code>
	 */
	protected MenuManager getPopupMenuManager()
	{
		return fPopupMenuMgr;
	}

	/**
	 * Fires the given event to all registered listeners.
	 *
	 * @param event the event to fire
	 */
	protected void firePropertyChangedEvent(PropertyChangeEvent event)
	{
		if (fPropertyListeners == null) {
			return;
		}

		for (IPropertyChangeListener iPropertyChangeListener : fPropertyListeners) {
			PropertyChangeNotifier notifier = new PropertyChangeNotifier(iPropertyChangeListener, event);
			SafeRunner.run(notifier);
		}
	}

	/**
	 * Returns the container hosting this memory rendering.
	 *
	 * @return the container hosting this memory rendering
	 */
	public IMemoryRenderingContainer getMemoryRenderingContainer()
	{
		return fContainer;
	}

	/**
	 * Returns whether this rendering is currently visible.
	 *
	 * @return whether this rendering is currently visible
	 */
	public boolean isVisible() {
		return fVisible;
	}
}
