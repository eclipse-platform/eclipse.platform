/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.core.model;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * A source locator that can be persisted and restored, to be used with a
 * specific launch configuration. The debug plug-in defines a source locator
 * extension point for persistable source locators.
 * <p>
 * A source locator extension is defined in <code>plugin.xml</code>. Following
 * is an example definition of a source locator extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.sourceLocators"&gt;
 *   &lt;sourceLocator
 *      id="com.example.ExampleIdentifier"
 *      class="com.example.ExampleSourceLocator"
 *      name="Example Source Locator"&gt;
 *   &lt;/sourceLocator&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>id</code> specifies a unique identifier for this source
 * locator.</li>
 * <li><code>class</code> specifies the fully qualified name of the Java class
 * that implements <code>IPersistableSourceLocator</code>.</li>
 * <li><code>name</code> a human readable name, describing the type of this
 * source locator.</li>
 * </ul>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see org.eclipse.debug.core.ILaunch
 * @see IStackFrame
 * @since 2.0
 */
public interface IPersistableSourceLocator extends ISourceLocator {

	/**
	 * Returns a memento that can be used to reconstruct
	 * this source locator
	 *
	 * @return a memento that can be used to reconstruct
	 *  this source locator
	 * @exception CoreException if unable to construct a memento
	 */
	String getMemento() throws CoreException;

	/**
	 * Initializes this source locator based on the given
	 * memento.
	 *
	 * @param memento a memento to initialize this source locator
	 * @exception CoreException on failure to initialize
	 */
	void initializeFromMemento(String memento) throws CoreException;

	/**
	 * Initializes this source locator to perform default
	 * source lookup for the given launch configuration.
	 *
	 * @param configuration launch configuration this source locator
	 *  will be performing source lookup for
	 * @exception CoreException on failure to initialize
	 */
	void initializeDefaults(ILaunchConfiguration configuration) throws CoreException;

}


