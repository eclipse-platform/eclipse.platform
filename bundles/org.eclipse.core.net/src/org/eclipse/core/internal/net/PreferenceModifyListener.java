/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferenceModifyListener extends
		org.eclipse.core.runtime.preferences.PreferenceModifyListener {

	public PreferenceModifyListener() {
		// Nothing to do
	}
	
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		// the node does not need to be the root of the hierarchy
		Preferences root = node.node("/"); //$NON-NLS-1$
		try {
			// we must not create empty preference nodes, so first check if the node exists
			if (root.nodeExists(InstanceScope.SCOPE)) {
				Preferences instance = root.node(InstanceScope.SCOPE);
				// we must not create empty preference nodes, so first check if the node exists
				if (instance.nodeExists(Activator.ID)) {
					((ProxyManager) ProxyManager.getProxyManager())
							.migrateInstanceScopePreferences(
									instance.node(Activator.ID),
									root.node(ConfigurationScope.SCOPE).node(
											Activator.ID), false);
				}
			}
		} catch (BackingStoreException e) {
			Activator.logError("Could not access instance preferences", e); //$NON-NLS-1$
		}
		return super.preApply(node);
	}
}
