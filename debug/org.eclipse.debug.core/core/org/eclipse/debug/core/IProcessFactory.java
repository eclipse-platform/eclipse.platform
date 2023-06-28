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
package org.eclipse.debug.core;

import java.util.Map;

import org.eclipse.debug.core.model.IProcess;

/**
 * A process factory is used to override default process (<code>IProcess</code>)
 * creation by the debug plug-in, and can be contributed via plug-in XML. When a
 * new process is created via <code>DebugPlugin.newProcess(..)</code>, the
 * launch configuration associated with the specified launch is consulted for a
 * process factory attribute (<code>DebugPlugin.ATTR_PROCESS_FACTORY_ID</code>).
 * If present, the associated process factory is consulted to create a process
 * for the launch. If not present a default process implementation is created
 * and returned by the debug plug-in.
 * <p>
 * Following is example plug-in XML that contributes a process factory.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.processFactories"&gt;
 *   &lt;processFactory
 *           id="com.example.ExampleIdentifier"
 *           class="com.example.ExampleProcessFactory"&gt;
 *   &lt;/processFactory&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li>id - a unique identifier for this extension point</li>
 * <li>class - the fully qualified name of a class the implements
 * <code>IProcessFactory</code></li>
 * </ul>
 * <p>
 * Clients contributing a process factory are intended to implement this
 * interface.
 * </p>
 *
 * @since 3.0
 */

public interface IProcessFactory {

	/**
	 * Creates and returns a new process representing the given
	 * <code>java.lang.Process</code>. A streams proxy is created
	 * for the I/O streams in the system process. The process
	 * is added to the given launch, and the process is initialized
	 * with the given attribute map.
	 *
	 * @param launch the launch the process is contained in
	 * @param process the system process to wrap
	 * @param label the label assigned to the process
	 * @param attributes initial values for the attribute map
	 * @return the process
	 * @see IProcess
	 */
	IProcess newProcess(ILaunch launch, Process process, String label, Map<String, String> attributes);
}
