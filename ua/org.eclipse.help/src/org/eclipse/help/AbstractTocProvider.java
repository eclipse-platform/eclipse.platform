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

import org.eclipse.help.internal.HelpPlugin;

/**
 * An <code>AbstractTocProvider</code> is a mechanism to provide arbitrary
 * content to the table of contents (TOC). <code>AbstractTocProvider</code>s
 * must be registered via the <code>org.eclipse.help.toc</code> extension point.
 *
 * @since 3.3
 */
public abstract class AbstractTocProvider {

	/**
	 * Returns all toc contributions for this provider. Providers
	 * are free to provide any number of contributions (zero or more).
	 *
	 * @param locale the locale for which to get contributions
	 * @return all the contributions for this provider
	 */
	public abstract ITocContribution[] getTocContributions(String locale);

	/**
	 * Notifies the platform that the content managed by this provider may
	 * have changed since the last time <code>getTocContributions()</code>
	 * was called, and needs to be updated.
	 */
	protected void contentChanged() {
		// will force a reload next time around
		HelpPlugin.getTocManager().clearCache();
	}

	/**
	 * Default priority for toc providers that do not override getPriority()
	 * @since 3.5
	 */
	public static final int DEFAULT_PRIORITY=10;

	/**
	 * Priority for toc files read from the local file system
	 * @since 3.5
	 */
	public static final int TOC_FILE_PRIORITY=20;

	/**
	 * If two toc contributions have the same id, the one with higher priority will be shown.
	 * For example, a provider with priority 1 will take precedence over a provider with priority 2.
	 * @return the priority of this provider
	 * @since 3.5
	 */
	public int getPriority()
	{
		return DEFAULT_PRIORITY;
	}
}
