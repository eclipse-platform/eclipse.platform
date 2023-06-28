/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Common function for debug views. Provides access to the underlying viewer and
 * debug model presentation being used by a viewer. This allows clients to do
 * such things as add and remove filters to a viewer, and configure a debug
 * model presentation.
 * <p>
 * Clients may implement this interface. Generally, clients should subclass
 * <code>AbstractDebugView</code> when creating a new debug view.
 * </p>
 * @see org.eclipse.core.runtime.IAdaptable
 * @see org.eclipse.debug.ui.IDebugModelPresentation
 * @see org.eclipse.debug.ui.AbstractDebugView
 * @since 2.0
 */

public interface IDebugView extends IViewPart {

	/**
	 * Action id for a view's copy action. Any view
	 * with a copy action that should be invoked when
	 * CTRL+C is pressed should store their
	 * copy action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String COPY_ACTION = ActionFactory.COPY.getId();

	/**
	 * Action id for a view's cut action. Any view
	 * with a cut action that should be invoked when
	 * CTRL+X is pressed should store their
	 * cut action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String CUT_ACTION = ActionFactory.CUT.getId();

	/**
	 * Action id for a view's double-click action. Any view
	 * with an action that should be invoked when
	 * the mouse is double-clicked should store their
	 * double-click action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String DOUBLE_CLICK_ACTION = "Double_Click_ActionId";	 //$NON-NLS-1$

	/**
	 * Action id for a view's find action. Any view
	 * with a find action that should be invoked when
	 * CTRL+F is pressed should store their
	 * find action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String FIND_ACTION = ActionFactory.FIND.getId();

	/**
	 * Action id for a view's paste action. Any view
	 * with a paste action that should be invoked when
	 * CTRL+V is pressed should store their
	 * paste action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String PASTE_ACTION = ActionFactory.PASTE.getId();

	/**
	 * Action id for a view's remove action. Any view
	 * with a remove action that should be invoked when
	 * the delete key is pressed should store their
	 * remove action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String REMOVE_ACTION = "Remove_ActionId"; //$NON-NLS-1$

	/**
	 * Action id for a view's select all action. Any view
	 * with a select all action that should be invoked when
	 * CTRL+A is pressed should store their
	 * select all action with this key.
	 *
	 * @see #setAction(String, IAction)
	 */
	String SELECT_ALL_ACTION = ActionFactory.SELECT_ALL.getId();

	/**
	 * Returns the viewer contained in this debug view.
	 *
	 * @return viewer
	 */
	Viewer getViewer();

	/**
	 * Returns the debug model presentation for this view specified
	 * by the debug model identifier.
	 *
	 * @param id the debug model identifier that corresponds to the <code>id</code>
	 *     attribute of a debug model presentation extension
	 * @return the debug model presentation, or <code>null</code> if no
	 *     presentation is registered for the specified id
	 */
	IDebugModelPresentation getPresentation(String id);

	/**
	 * Installs the given action under the given action id.
	 *
	 * If the action has an id that maps to one of the global
	 * action ids defined by this interface, the action is registered
	 * as a global action handler.
	 *
	 * If the action is an instance of <code>IUpdate</code> it is added/remove
	 * from the collection of updateables associated with this view.
	 *
	 * @param actionID the action id
	 * @param action the action, or <code>null</code> to clear it
	 * @see #getAction
	 */
	void setAction(String actionID, IAction action);

	/**
	 * Adds the given IUpdate to this view's collection of updatable
	 * objects.  Allows the view to periodically update these registered
	 * objects.
	 * Has no effect if an identical IUpdate is already registered.
	 *
	 * @param updatable The IUpdate instance to be added
	 */
	void add(IUpdate updatable);

	/**
	 * Removes the given IUpdate from this view's collection of updatable
	 * objects.
	 * Has no effect if an identical IUpdate was not already registered.
	 *
	 * @param updatable The IUpdate instance to be removed
	 */
	void remove(IUpdate updatable);

	/**
	 * Returns the action installed under the given action id.
	 *
	 * @param actionID the action id
	 * @return the action, or <code>null</code> if none
	 * @see #setAction
	 */
	IAction getAction(String actionID);

	/**
	 * Returns the context menu manager for this view.
	 *
	 * @return the context menu manager for this view, or <code>null</code> if none
	 * @deprecated See AbstractDebugView#getContextMenuManagers()
	 */
	@Deprecated IMenuManager getContextMenuManager();
}
