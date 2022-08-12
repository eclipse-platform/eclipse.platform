/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.core.internal.properties;

import java.util.Map;
import org.eclipse.core.internal.resources.IManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public interface IPropertyManager extends IManager {
	/**
	 * Closes the property store for a resource
	 *
	 * @param target The resource to close the property store for
	 */
	void closePropertyStore(IResource target) throws CoreException;

	/**
	 * Copy all the properties of one resource to another. Both resources
	 * must have a property store available.
	 */
	void copy(IResource source, IResource destination, int depth) throws CoreException;

	/**
	 * Deletes all properties for the given resource and its children.
	 * <p>
	 * The subtree under the given resource is traversed to the supplied depth.
	 * </p>
	 *
	 * @param target the resource(tree) to delete properties from
	 * @param depth  the max depth to delete properties from the resource(tree)
	 */
	void deleteProperties(IResource target, int depth) throws CoreException;

	/**
	 * The resource is being deleted so permanently erase its properties.
	 */
	void deleteResource(IResource target) throws CoreException;

	/**
	 * Returns the value of the identified property on the given resource as
	 * maintained by this store.
	 * <p>
	 * The qualifier part of the property name must be the unique identifier
	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
	 * </p>
	 */
	String getProperty(IResource target, QualifiedName name) throws CoreException;

	/**
	 * Sets the value of the identified property on the given resource.
	 * <p>
	 * The qualifier part of the property name must be the unique identifier
	 * of the declaring plug-in (e.g. <code>"com.example.plugin"</code>).
	 * </p>
	 */
	void setProperty(IResource target, QualifiedName name, String value) throws CoreException;

	/**
	 * Returns a map {@literal (<propertyKey: QualifiedName -> value: String>)}
	 * containing all properties defined for the given resource. In case no
	 * properties can be found, returns an empty map.
	 */
	Map<QualifiedName, String> getProperties(IResource resource) throws CoreException;
}
