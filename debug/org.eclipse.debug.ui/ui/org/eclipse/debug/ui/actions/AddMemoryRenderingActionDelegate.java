/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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

package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * A cascade menu to add a memory rendering to the memory view. This action delegate can be
 * contributed to a an editor, view or object via standard workbench extension points.
 * The action works on the {@link IAddMemoryRenderingsTarget} adapter provided
 * by the active debug context, creating a context menu to add applicable renderings
 * to the memory view.
 * <p>
 * Clients may reference/contribute this class as an action delegate
 * in plug-in XML.
 * </p>
 * @since 3.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class AddMemoryRenderingActionDelegate extends Action implements IViewActionDelegate, IEditorActionDelegate, IObjectActionDelegate, IActionDelegate2{

	private IAction fAction;
	private IWorkbenchPart fPart;
	private ISelection fCurrentSelection;
	private IAddMemoryRenderingsTarget fActionDelegate;
	private IMenuCreator fMenuCreator;
	private IAdaptable fDebugContext;
	private IWorkbenchWindow fWindow;
	private DebugContextListener fDebugContextListener = new DebugContextListener();

	private class AddMemoryRenderingAction extends Action
	{
		private IMemoryRenderingType fRenderingType;			// type of rendering to add
		AddMemoryRenderingAction(IMemoryRenderingType renderingType)
		{
			super(renderingType.getLabel());
			fRenderingType = renderingType;
		}

		@Override
		public void runWithEvent(Event event) {
			if (fActionDelegate != null)
			{
				try {
					fActionDelegate.addMemoryRenderings(fPart, fCurrentSelection, new IMemoryRenderingType[]{fRenderingType});
				} catch (CoreException e) {
					DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.AddMemoryRenderingActionDelegate_0, ActionMessages.AddMemoryRenderingActionDelegate_1, e);
				}
			}
		}
	}

	private class AddMemoryRenderingMenuCreator implements IMenuCreator
	{

		@Override
		public void dispose() {

		}

		@Override
		public Menu getMenu(Control parent) {
			return null;
		}

		@Override
		public Menu getMenu(Menu parent) {
			Menu menu = new Menu(parent);
			menu.addMenuListener(new MenuAdapter() {
				@Override
				public void menuShown(MenuEvent e) {
					Menu m = (Menu)e.widget;
					MenuItem[] items = m.getItems();
					for (MenuItem item : items) {
						item.dispose();
					}
					fillMenu(m);
				}
			});
			return menu;
		}

		private void fillMenu(Menu parent)
		{
			if (fActionDelegate != null)
			{
				IMemoryRenderingType[] types = fActionDelegate.getMemoryRenderingTypes(fPart, fCurrentSelection);

				for (IMemoryRenderingType type : types) {
					AddMemoryRenderingAction action = new AddMemoryRenderingAction(type);
					ActionContributionItem item = new ActionContributionItem(action);
					item.fill(parent, -1);
				}
			}
		}
	}

	private class DebugContextListener implements IDebugContextListener
	{

		private void contextActivated(ISelection selection) {
			setupActionDelegate(selection);

			if(fAction != null) {
				updateAction(fAction, fCurrentSelection);
			}
		}

		@Override
		public void debugContextChanged(DebugContextEvent event) {
			contextActivated(event.getContext());
		}


	}

	private void setupActionDelegate(ISelection context)
	{
		IAdaptable debugContext = null;
		if (context instanceof IStructuredSelection)
		{
			if (((IStructuredSelection)context).getFirstElement() instanceof IAdaptable) {
				debugContext = (IAdaptable)((IStructuredSelection)context).getFirstElement();
			}
		}

		if (debugContext == null) {
			fActionDelegate = null;
		}

		if (debugContext == fDebugContext) {
			return;
		}

		fDebugContext = debugContext;

		if (fDebugContext == null) {
			return;
		}

		IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(fDebugContext);

		if (retrieval == null) {
			return;
		}

		IAddMemoryRenderingsTarget target = null;
		if (fCurrentSelection instanceof IStructuredSelection)
		{
			// get target from current selection
			IStructuredSelection strucSel = (IStructuredSelection)fCurrentSelection;
			Object obj = strucSel.getFirstElement();
			target = getAddMemoryRenderingTarget(obj);
		}
		if (target == null)
		{
			// get the target from Debug View
			target = getAddMemoryRenderingTarget(fDebugContext);
		}
		if (target == null)
		{
			// try to get target from memory block retrieval
			target = getAddMemoryRenderingTarget(retrieval);
		}

		fActionDelegate = target;
	}

	@Override
	public void init(IViewPart view) {
		bindPart(view);
	}

	@Override
	public void run(IAction action) {
		// do nothing
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fCurrentSelection = selection;

		if(action != null) {
			bindAction(action);
			updateAction(action, selection);
		}
	}

	/**
	 * @param action - the action to bind with the menu and to update enablement, must not be null
	 * @param selection the current selection
	 */
	private void updateAction(IAction action, ISelection selection)
	{
		if (fActionDelegate != null)
		{
			action.setEnabled(fActionDelegate.canAddMemoryRenderings(fPart, selection));
			bindAction(action);
		}
		else
		{
			action.setEnabled(false);
		}
	}

	/**
	 * @param action - the action to bind with the menu, must not be null
	 */
	private void bindAction(IAction action) {
		if (action != fAction) {
			if (fMenuCreator == null) {
				fMenuCreator = new AddMemoryRenderingMenuCreator();
			}
			action.setMenuCreator(fMenuCreator);
			fAction= action;
		}
	}

	private IAddMemoryRenderingsTarget getAddMemoryRenderingTarget(Object elmt) {
		IAddMemoryRenderingsTarget target = null;

		if (elmt instanceof IAddMemoryRenderingsTarget)
		{
			target = (IAddMemoryRenderingsTarget)elmt;
		}
		else if (elmt instanceof IAdaptable)
		{
			target = ((IAdaptable)elmt).getAdapter(IAddMemoryRenderingsTarget.class);
		}
		return target;
	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		bindPart(targetEditor);

		if(action != null) {
			bindAction(action);
			updateAction(action, fCurrentSelection);
		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		bindPart(targetPart);

		if(action != null) {
			bindAction(action);
			updateAction(action, fCurrentSelection);
		}
	}

	@Override
	public void init(IAction action) {
		if (action != null) {
			bindAction(action);

			action.setText(ActionMessages.AddMemoryRenderingActionDelegate_2);
			action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_MONITOR_EXPRESSION));
			action.setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_ADD));
			action.setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_MONITOR_EXPRESSION));
		}
	}

	@Override
	public void dispose() {
		// remove as debug context listener
		fAction = null;
		fPart = null;
		fCurrentSelection = null;
		fActionDelegate = null;

		// remove debug context listener
		bindPart(null);
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		// do nothing
	}

	private void bindPart(IWorkbenchPart part)
	{
		IWorkbenchWindow window = null;
		if (part != null)
		{
			window = part.getSite().getWorkbenchWindow();
		}
		if (window != fWindow)
		{

			if (fWindow != null)
			{
				DebugUITools.getDebugContextManager().getContextService(fWindow).removeDebugContextListener(fDebugContextListener);
			}

			if (window != null)
			{
				DebugUITools.getDebugContextManager().getContextService(window).addDebugContextListener(fDebugContextListener);
			}
			fWindow = window;
		}

		if (part != fPart) {
			fPart = part;
		}

		if (fWindow != null) {
			setupActionDelegate(DebugUITools.getDebugContextManager().getContextService(fWindow).getActiveContext());
		}
	}

}

