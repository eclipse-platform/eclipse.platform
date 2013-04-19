/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * These are elements created to display synchronization state to the user. These elements are found in
 * the generated diff tree viewer created by a {@link SubscriberParticipant}. Since it implements
 * {@link ITypedElement} and {@link ICompareInput} they can be used as input to compare components.
  * <p>
 * Clients typically use this interface as is, but may implement it if required.
 * </p>
 * @since 3.0
 */
public interface ISynchronizeModelElement extends IDiffContainer, ITypedElement, ICompareInput {

	/**
	 * Property constant indicating that the element is currently being worked on by an operation.
	 */
	public static final String BUSY_PROPERTY = TeamUIPlugin.ID + ".busy"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating that the element has children that are conflicting.
	 */
	public static final String PROPAGATED_CONFLICT_PROPERTY = TeamUIPlugin.ID + ".conflict"; //$NON-NLS-1$
	
	/**
	 * Property constant identifying that this element or one of its children has an error marker.
	 */
	public static final String PROPAGATED_ERROR_MARKER_PROPERTY = TeamUIPlugin.ID + ".error"; //$NON-NLS-1$
	
	/**
	 * Property constant indicating that this element or one of its children has a warning marker.
	 */
	public static final String PROPAGATED_WARNING_MARKER_PROPERTY = TeamUIPlugin.ID + ".warning"; //$NON-NLS-1$

	/**
	 * Adds a listener for changes to properties of this synchronize element. Has no effect if an identical 
	 * listener is already registered.
	 * 
	 * @param listener the listener to register
	 */
	public abstract void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the given property change listener from this model element. Has no effect if
	 * the listener is not registered.
	 * 
	 * @param listener the listener to remove
	 */
	public abstract void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Assigns the given property to this element and all it's parents.
	 * 
	 * @param propertyName the property name to set
	 * @param value the value of the property
	 */
	public void setPropertyToRoot(String propertyName, boolean value);
	
	/**
	 * Assigns the given property to this element.
	 * 
	 * @param propertyName the property name
	 * @param value the value of the property.
	 */
	public void setProperty(String propertyName, boolean value);
	
	/**
	 * Return whether this element has the given property assigned.
	 * 
	 * @param propertyName the property to test for
	 * @return <code>true</code> if the property is set and <code>false</code>
	 * otherwise.
	 */
	public abstract boolean getProperty(String propertyName);

	/**
	 * The image descriptor describing the given element.
	 * 
	 * @param element the model element for which to return an image.
	 * @return the image descriptor for the given element.
	 */
	public abstract ImageDescriptor getImageDescriptor(Object element);

	/**
	 * Returns the resource this element is showing synchronization information for or <code>null</code>
	 * if the element does not have an associated local resource.
	 * 
	 * @return the resource this element is showing synchronization information for or <code>null</code>
	 * if the element does not have an associated local resource.
	 */
	public abstract IResource getResource();
}
