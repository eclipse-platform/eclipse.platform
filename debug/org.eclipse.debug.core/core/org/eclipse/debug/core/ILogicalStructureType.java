/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.core;

import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate2;

/**
 * Provides a value representing the logical structure of a raw implementation
 * value from a debug model. Logical structures are useful for navigating
 * complex data structures. Logical structure types are contributed via
 * extensions in plug-in XML, and provide a delegate for performing logical
 * value computation. Logical structure types can be retrieved from the
 * <code>DebugPlugin</code>.
 * <p>
 * Following is example plug-in XML to define a logical structure type.
 * </p>
 *
 * <pre>
 * &lt;extension point=&quot;org.eclipse.debug.core.logicalStructureTypes&quot;&gt;
 *  &lt;logicalStructureType
 *   id=&quot;com.example.ExampleLogicalStructure&quot;
 *   class=&quot;com.example.ExampleLogicalStructureDelegate&quot;
 *   modelIdentifier=&quot;com.example.debug.model&quot;
 *   description=&quot;Ordered Collection&quot;&gt;
 *  &lt;/logicalStructureType&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * The attributes are specified as follows:
 * </p>
 * <ul>
 * <li>id - unique identifier for this logical structure type</li>
 * <li>class - fully qualified name of class that implements
 * <code>ILogicalStructureTypeDelegate</code></li>
 * <li>modelIdentifier - identifier of the debug model this logical structure
 * type is associated with</li>
 * <li>description - description of the logical structure provided</li>
 * </ul>
 * <p>
 * Clients contributing logicalStructureType extensions are not intended to
 * implement this interface. Rather, they provide an
 * <code>ILogicalStructureTypeDelegate</code> that optionally implements
 * <code>ILogicalStructureTypeDelegate2</code> to provide dynamic descriptions
 * of logical structures. Since 3.1, clients contributing
 * logicalStructureProviders extensions may implement this interface to return a
 * collection of logical structure types applicable to a value.
 * </p>
 *
 * @since 3.0
 * @see org.eclipse.debug.core.model.ILogicalStructureTypeDelegate
 * @see org.eclipse.debug.core.ILogicalStructureProvider
 */
public interface ILogicalStructureType extends ILogicalStructureTypeDelegate, ILogicalStructureTypeDelegate2 {

	/**
	 * Returns a simple description of the logical structure provided by this
	 * structure type.
	 * <p>
	 * Since 3.1, this method can return <code>null</code> if this logical structure
	 * type's delegate implements <code>ILogicalStructureTypeDelegate2</code>.
	 * </p>
	 *
	 * @return a simple description of the logical structure provided by this
	 * structure type, possibly <code>null</code>
	 */
	String getDescription();

	/**
	 * Returns this logical structure type's unique identifier, as defined
	 * in plug-in XML.
	 *
	 * @return this logical structure type's unique identifier
	 */
	String getId();

}
