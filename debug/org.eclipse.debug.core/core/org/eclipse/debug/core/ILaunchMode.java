/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

/**
 * A launch mode. The debug platform contributes launch modes for run, debug,
 * and profile. Clients may contribute additional launch modes in plug-in XML
 * via the <code>launchModes</code> extension point.
 * <p>
 * Following is an example launch mode contribution for profiling. A launch mode
 * has an unique identifier specified by the <code>mode</code> attribute and a
 * human readable label specified by the <code>label</code> attribute.
 * </p>
 *
 * <pre>
 *  &lt;extension point=&quot;org.eclipse.debug.core.launchModes&quot;&gt;
 *   &lt;launchMode
 *    mode=&quot;profile&quot;
 *    label=&quot;Profile&quot;&gt;
 *   &lt;/launchMode&gt;
 *  &lt;/extension&gt;
 * </pre>
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILaunchMode {

	/**
	 * Returns the unique identifier for this launch mode.
	 *
	 * @return the unique identifier for this launch mode
	 */
	String getIdentifier();

	/**
	 * Returns a human readable label for this launch mode.
	 *
	 * @return a human readable label for this launch mode
	 */
	String getLabel();

	/**
	 * Returns a human readable label for this launch mode when used in a
	 * cascade menu. For example, "Run As". Allows the label to be
	 * properly externalized.
	 * <p>
	 * A new attribute has been added the the launch mode extension in 3.2
	 * to specify this label. When unspecified a default label is generated
	 * by concatenation, for backwards compatibility.
	 * </p>
	 * @return human readable label for this launch mode when used in a
	 * cascade menu
	 * @since 3.2
	 */
	String getLaunchAsLabel();
}
