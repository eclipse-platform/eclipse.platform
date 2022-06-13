/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferenceModifyListener extends org.eclipse.core.runtime.preferences.PreferenceModifyListener {
	@Override
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		Preferences root = node.node("/"); //$NON-NLS-1$
		try {
			if (root.nodeExists(InstanceScope.SCOPE)) {
				Preferences instance = root.node(InstanceScope.SCOPE);
				if (instance.nodeExists(ContentTypeManager.CONTENT_TYPE_PREF_NODE))
					ContentTypeManager.getInstance().invalidate();
			}
		} catch (BackingStoreException e) {
			// do nothing
		}
		return node;
	}
}
