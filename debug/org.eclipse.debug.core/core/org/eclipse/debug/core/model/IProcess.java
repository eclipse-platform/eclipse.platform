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
package org.eclipse.debug.core.model;


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;

/**
 * A process represents a program running in normal (non-debug) mode. Processes
 * support setting and getting of client defined attributes. This way, clients
 * can annotate a process with any extra information important to them. For
 * example, classpath annotations, or command line arguments used to launch the
 * process may be important to a client.
 * <p>
 * Clients may implement this interface, however, the debug plug-in provides an
 * implementation of this interface for a <code>java.lang.Process</code>.
 * </p>
 *
 * If implementing custom launches it is important to fire event whenever the
 * process state changes in the following way:
 *
 * <pre>
 * DebugPlugin manager = DebugPlugin.getDefault();
 * if (manager != null) {
 * 	manager.fireDebugEventSet(new DebugEvent[] {
 * 			new DebugEvent(this, DebugEvent.CREATE) });
 * }
 * </pre>
 *
 * otherwise the UI will probably show faulty state for your process and launch.
 *
 * @see org.eclipse.debug.core.DebugPlugin#newProcess(ILaunch, Process, String)
 */
public interface IProcess extends IAdaptable, ITerminate {

	/**
	 * Attribute key for a common, optional, process property. The value of this
	 * attribute is the command line a process was launched with.
	 *
	 * @since 2.1
	 */
	String ATTR_CMDLINE= DebugPlugin.getUniqueIdentifier() + ".ATTR_CMDLINE"; //$NON-NLS-1$

	/**
	 * Attribute key for a common, optional, process property. The value of this
	 * attribute is an identifier for the type of this process. Process types
	 * are client defined - whoever creates a process may define its type. For
	 * example, a process type could be "java", "javadoc", or "ant".
	 *
	 * @since 2.1
	 */
	String ATTR_PROCESS_TYPE = DebugPlugin.getUniqueIdentifier() + ".ATTR_PROCESS_TYPE"; //$NON-NLS-1$

	/**
	 * Attribute key for a common, optional, process property. The value of this
	 * attribute specifies an alternate dynamic label for a process, displayed by
	 * the console.
	 *
	 * @since 3.0
	 */
	String ATTR_PROCESS_LABEL = DebugPlugin.getUniqueIdentifier() + ".ATTR_PROCESS_LABEL"; //$NON-NLS-1$

	/**
	 * Attribute key for a common, optional, process property. The value of this
	 * attribute specifies process id, displayed in the console description or
	 * in the debug view.
	 *
	 * @since 3.19
	 */
	String ATTR_PROCESS_ID = DebugPlugin.getUniqueIdentifier() + ".ATTR_PROCESS_ID"; //$NON-NLS-1$

	/**
	 * Returns a human-readable label for this process.
	 *
	 * @return a label for this process
	 */
	String getLabel();
	/**
	 * Returns the launch this element originated from.
	 *
	 * @return the launch this process is contained in
	 */
	ILaunch getLaunch();
	/**
	 * Returns a proxy to the standard input, output, and error streams
	 * for this process, or <code>null</code> if not supported.
	 *
	 * @return a streams proxy, or <code>null</code> if not supported
	 */
	IStreamsProxy getStreamsProxy();

	/**
	 * Sets the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	void setAttribute(String key, String value);

	/**
	 * Returns the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @return value the String attribute value, or <code>null</code> if undefined
	 */
	String getAttribute(String key);

	/**
	 * Returns the exit value of this process. Conventionally, 0 indicates
	 * normal termination.
	 *
	 * @return the exit value of this process
	 * @exception DebugException if this process has not yet terminated
	 */
	int getExitValue() throws DebugException;
}
