/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - added a breadcrumb mode to Debug view (Bug 252677)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Context in which an asynchronous request has been made.
 * <p>
 * Clients may implement and extend this interface to provide
 * special contexts. Implementations must subclass {@link PresentationContext}.
 * </p>
 * @since 3.2
 */
public interface IPresentationContext {

	/**
	 * Property name used for property change notification when the columns
	 * in a presentation context change.
	 */
	String PROPERTY_COLUMNS = "PROPERTY_COLUMNS"; //$NON-NLS-1$

	/**
	 * Property indicating whether the presentation context is disposed.
	 * It is set to <code>Boolean.TRUE</code> after the presentation context
	 * is disposed. This property may be <code>null</code>, which indicates
	 * that context is not yet disposed.
	 *
	 * @since 3.6
	 */
	String PROPERTY_DISPOSED = "PROPERTY_DISPOSED"; //$NON-NLS-1$

	/**
	 * Returns identifiers of the visible columns in the order
	 * labels should be provided, or <code>null</code> if columns
	 * are not being displayed. Label providers use this
	 * information.
	 *
	 * @return visible column identifiers or <code>null</code>
	 * @see IColumnPresentation
	 */
	String[] getColumns();

	/**
	 * Registers the given listener for property change notification.
	 *
	 * @param listener property listener
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Unregisters the given listener from property change notification.
	 *
	 * @param listener property listener.
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Returns the id of this presentation context. Usually this is the id of
	 * the associated part. However, when no part is associated with this context,
	 * the id may exist on its own. Allows for a context that is not tied to a part.
	 *
	 * @return id
	 * @since 3.3
	 */
	String getId();

	/**
	 * Sets the specified property and notifies listeners of changes.
	 *
	 * @param property property name
	 * @param value property value
	 */
	void setProperty(String property, Object value);

	/**
	 * Returns the property with the specified name or <code>null</code>
	 * if none.
	 *
	 * @param property property name
	 * @return property value or <code>null</code>
	 */
	Object getProperty(String property);

	/**
	 * Disposes this presentation context. Called by the framework
	 * when the associated viewer is disposed.
	 */
	void dispose();

	/**
	 * Returns all keys of properties currently set in this context,
	 * possibly an empty collection
	 *
	 * @return keys of all current properties
	 * @since 3.4
	 */
	String[] getProperties();

	/**
	 * Returns the part that this presentation context is associated with.
	 * May return <code>null</code> if the presentation is not associated
	 * with a part.
	 *
	 * @return IWorkbenchPart or <code>null</code>
	 * @since 3.6
	 */
	IWorkbenchPart getPart();

	/**
	 * Returns the window that this presentation context is associated with.
	 * May return <code>null</code> if the presentation is not associated
	 * with a window.
	 *
	 * @return IWorkbenchWindow or <code>null</code>
	 * @since 3.6
	 */
	IWorkbenchWindow getWindow();

}
