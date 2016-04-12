/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.*;

/**
 * An Action that wraps IActionDelegates so they can be used programmatically
 * in toolbars, etc.
 */
public class ActionDelegateWrapper extends Action implements ISelectionChangedListener {

	private IActionDelegate delegate;

	public ActionDelegateWrapper(IActionDelegate delegate, ISynchronizePageConfiguration configuration) {
		this.delegate = delegate;
		IWorkbenchPart part = configuration.getSite().getPart();
		if(part != null) {
			if (delegate instanceof IObjectActionDelegate) {
				((IObjectActionDelegate)delegate).setActivePart(this, part);
			}
			if (part instanceof IViewPart
					&& delegate instanceof IViewActionDelegate) {
				((IViewActionDelegate)delegate).init((IViewPart)part);
			}
			if (part instanceof IEditorPart
					&& delegate instanceof IEditorActionDelegate) {
				((IEditorActionDelegate)delegate).setActiveEditor(this, (IEditorPart)part);
			}
		}
		initialize(configuration);
	}

	public ActionDelegateWrapper(IActionDelegate delegate, ISynchronizePageConfiguration configuration, String id) {
		this(delegate, configuration);
		setId(id);
		setActionDefinitionId(id);
	}

	/**
	 * Method invoked from the constructor when a configuration is provided.
	 * The default implementation registers the action as a selection change
	 * listener. Subclass may override.
	 * @param configuration the synchronize page configuration
	 */
	protected void initialize(final ISynchronizePageConfiguration configuration) {
		configuration.getSite().getSelectionProvider().addSelectionChangedListener(this);
		configuration.getPage().getViewer().getControl().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				configuration.getSite().getSelectionProvider().removeSelectionChangedListener(ActionDelegateWrapper.this);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		getDelegate().selectionChanged(this, event.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		getDelegate().run(this);
	}

	/**
	 * Return the delegate associated with this action.
	 * @return the delegate associated with this action
	 */
	public IActionDelegate getDelegate() {
		return delegate;
	}

}
