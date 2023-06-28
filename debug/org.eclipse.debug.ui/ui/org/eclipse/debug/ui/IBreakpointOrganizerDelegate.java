/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * A breakpoint organizer is used to categorize breakpoints and provides change
 * notification when categorization has changed. Categories are represented as
 * arbitrary adaptable objects. For example, projects could be used to
 * categorize breakpoints. Images and labels for categories are generated via
 * workbench adapters.
 * <p>
 * Organizers may optionally support breakpoint recategorization.
 * </p>
 * <p>
 * Following is example plug-in XML for contributing a breakpoint organizer.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.breakpointOrganizers"&gt;
 * 	&lt;breakpointOrganizer
 * 		class="com.example.BreakpointOrganizer"
 *      id="com.example.BreakpointOrganizer"
 *      label="Example Organizer"
 *      icon="icons/full/obj16/example_org.png"/&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>class</code> Fully qualified name of a Java class that implements
 * {@link IBreakpointOrganizerDelegate}.</li>
 * <li><code>id</code> Unique identifier for this breakpoint organizer.</li>
 * <li><code>label</code> Label for this organizer which is suitable for
 * presentation to the user.</li>
 * <li><code>icon</code> Optional path to an icon which can be shown for this
 * organizer</li>
 * </ul>
 * <p>
 * Clients contributing a breakpoint organizer are intended to implement this
 * interface.
 * </p>
 *
 * @since 3.1
 */
public interface IBreakpointOrganizerDelegate {

	/**
	 * Change event id when a category's breakpoints have changed.
	 * The <code>oldValue</code> of the <code>PropertyChangeEvent</code> will be the
	 * category that has changed, and the source of the event will the the
	 * breakpoint organizer. Breakpoints in the category will be
	 * recategorized when this event is fired.
	 *
	 * @see IPropertyChangeListener
	 */
	String P_CATEGORY_CHANGED = DebugUIPlugin.getUniqueIdentifier() + ".P_CATEGORY_CHANGED"; //$NON-NLS-1$

	/**
	 * Returns objects representing the categories of the specified
	 * breakpoint or <code>null</code> if this organizer cannot classify
	 * the breakpoint. Categories must return <code>true</code> when sent
	 * the message <code>equals(Object)</code> with an equivalent category
	 * as an argument.
	 *
	 * @param breakpoint breakpoint to classify
	 * @return categories of the given breakpoint or <code>null</code>
	 */
	IAdaptable[] getCategories(IBreakpoint breakpoint);

	/**
	 * Adds the specified listener. Has no effect if an identical listener is
	 * already registered.
	 *
	 * @param listener listener to add
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the specified listener. Has no effect if an identical listener
	 * is not already registered.
	 *
	 * @param listener listener to remove
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Adds the specified breakpoint to the given category. Only called
	 * if <code>canAdd(...)</code> returns <code>true</code> for the given
	 * breakpoint and category.
	 *
	 * @param breakpoint breakpoint to recategorize
	 * @param category the breakpoint's new category
	 */
	void addBreakpoint(IBreakpoint breakpoint, IAdaptable category);

	/**
	 * Removes the specified breakpoint from the given category. Only
	 * called if <code>canRemove(...)</code> returns <code>true</code> for
	 * the given breakpoint and category.
	 *
	 * @param breakpoint breakpoint to recategorize
	 * @param category the category the breakpoint is remove from
	 */
	void removeBreakpoint(IBreakpoint breakpoint, IAdaptable category);

	/**
	 * Returns whether the given breakpoint can be categorized in the
	 * specified category.
	 *
	 * @param breakpoint breakpoint to recatogorize
	 * @param category the category to add the breakpoint to
	 * @return whether the given breakpoint can be categorized in the
	 * specified category
	 */
	boolean canAdd(IBreakpoint breakpoint, IAdaptable category);

	/**
	 * Returns whether the given breakpoint can be removed from the given
	 * category.
	 *
	 * @param breakpoint breakpoint to recategorize
	 * @param category the category to remove the breakpoint from
	 * @return whether the given breakpoint can be removed from the given
	 * category
	 */
	boolean canRemove(IBreakpoint breakpoint, IAdaptable category);

	/**
	 * Returns all categories managed by this organizer, or <code>null</code>.
	 * When <code>null</code> is returned, the breakpoints view only displays
	 * categories that contain breakpoints. When a collection of categories
	 * is returned the breakpoints will display all of the categories, some of
	 * which may be empty.
	 *
	 * @return all categories managed by this organizer, or <code>null</code>
	 */
	IAdaptable[] getCategories();

	/**
	 * Disposes this breakpoint organizer.
	 */
	void dispose();

}
