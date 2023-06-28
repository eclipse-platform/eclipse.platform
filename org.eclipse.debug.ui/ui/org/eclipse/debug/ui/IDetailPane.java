/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wind River - Anton Leherbauer - Fix selection provider (Bug 254442)
 *******************************************************************************/
package org.eclipse.debug.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * A detail pane is created from a detail pane factory and displays detailed information about
 * a current selection with an SWT <code>Control</code>. Use the
 * <code>org.eclipse.debug.ui.detailFactories</code> extension point to contribute a detail pane
 * factory.
 * <p>
 * Implementors should implement {@link IDetailPane2} in favor of this interface.
 * </p>
 * @see IDetailPaneFactory
 * @see IDetailPane2
 * @since 3.3
 */
public interface IDetailPane {

	/**
	 * Initializes this detail pane for the given workbench part site. This is the first method
	 * invoked on a detail pane after instantiation.  If this detail pane is being added to a
	 * non-view component such as a dialog, the passed workbench part site will be <code>null</code>.
	 *
	 * @param partSite The workbench part site that this detail pane has been created in or <code>null</code>
	 */
	void init(IWorkbenchPartSite partSite);

	/**
	 * Creates and returns the main control for this detail pane using the given composite as a
	 * parent.
	 *
	 * @param parent The parent composite that UI components should be added to
	 * @return The main control for this detail pane
	 */
	Control createControl(Composite parent);

	/**
	 * Disposes this detail pane. This is the last method invoked on a detail pane and should
	 * dispose of all UI components including the main composite returned in <code>createControl()</code>.
	 */
	void dispose();

	/**
	 * Displays details for the given selection, possible <code>null</code>.  An empty selection
	 * or <code>null</code> should clear this detail pane.
	 *
	 * @param selection The selection to be displayed, possibly empty or <code>null</code>
	 */
	void display(IStructuredSelection selection);

	/**
	 * Allows this detail pane to give focus to an appropriate control, and returns whether
	 * the detail pane accepted focus. If this detail pane does not want focus, it should
	 * return <code>false</code>, allowing the containing view to choose another target
	 * for focus.
	 *
	 * @return whether focus was taken
	 */
	boolean setFocus();

	/**
	 * Returns a unique identifier for this detail pane.
	 *
	 * @return A unique identifier for this detail pane
	 */
	String getID();

	/**
	 * The human readable name of this detail pane. This is a short description of the type
	 * of details this pane displays that appears in the context menu.
	 *
	 * @return name of this detail pane
	 */
	String getName();

	/**
	 * A brief description of this detail pane, or <code>null</code> if none
	 *
	 * @return a description of this detail pane, or <code>null</code> if none
	 */
	String getDescription();

}
