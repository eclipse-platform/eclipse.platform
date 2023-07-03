/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help;

/**
 * An <code>AbstractContextProvider</code> is a mechanism to provide arbitrary
 * context-sensitive help for any part of the UI. <code>AbstractContextProvider
 * </code>s must be registered via the <code>org.eclipse.help.contexts</code>
 * extension point.
 *
 * @since 3.3
 */
public abstract class AbstractContextProvider {

	/**
	 * Returns the context-sensitive help content for the UI element with the
	 * given context help ID, and for the given locale.
	 *
	 * @param id the unique context help ID, e.g. "org.my.plugin.my_context_id"
	 * @return the context help, or <code>null</code> if not available
	 */
	public abstract IContext getContext(String id, String locale);

	/**
	 * Returns an array of <code>String</code>s containing the ids of the
	 * UI plug-ins for which this provider should be used. This is equivalent to
	 * the <code>plugin</code> attribute of the <code>contexts</code> element
	 * of the <code>org.eclipse.help.contexts</code> extension point, except you
	 * can specify any number of plug-ins.
	 *
	 * @return the UI plug-ins for which this provider should be used
	 */
	public abstract String[] getPlugins();
}
