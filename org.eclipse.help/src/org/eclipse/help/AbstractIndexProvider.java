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
 * An <code>AbstractIndexProvider</code> is a mechanism to provide arbitrary
 * content to the keyword index. <code>AbstractIndexProvider</code>s must be
 * registered via the <code>org.eclipse.help.index</code> extension point.
 *
 * @since 3.3
 */
public abstract class AbstractIndexProvider {

	/**
	 * Returns all index contributions for this provider. Providers
	 * are free to provide any number of contributions (zero or more).
	 *
	 * @param locale the locale for which to get contributions
	 * @return all the index contributions for this provider
	 */
	public abstract IIndexContribution[] getIndexContributions(String locale);

	/**
	 * Notifies the platform that the content managed by this provider may
	 * have changed since the last time <code>getIndexContributions()</code>
	 * was called, and needs to be updated.
	 */
	protected void contentChanged() {
		// will force a reload next time around
		HelpPlugin.getIndexManager().clearCache();
	}
}
