/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.preferences;

import java.util.Properties;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @since 3.5
 */
public class BadTestScope extends EclipsePreferences implements IScopeContext {

	public static final String SCOPE = "badtest"; //$NON-NLS-1$
	private String qualifier;
	private int segmentCount;
	private IEclipsePreferences loadLevel;

	public BadTestScope() {
		this(null, null);
	}

	private BadTestScope(EclipsePreferences parent, String key) {
		super(parent, key);
		// cache the segment count
		IPath path = new Path(absolutePath());
		segmentCount = path.segmentCount();
		if (segmentCount < 2) {
			return;
		}

		// cache the qualifier
		String scope = path.segment(0);
		if (BadTestScope.SCOPE.equals(scope)) {
			qualifier = path.segment(1);
		}

		// cache the location
		if (qualifier == null) {
			return;
		}
	}

	@Override
	protected IEclipsePreferences getLoadLevel() {
		if (loadLevel == null) {
			if (qualifier == null) {
				return null;
			}
			// Make it relative to this node rather than navigating to it from the root.
			// Walk backwards up the tree starting at this node.
			// This is important to avoid a chicken/egg thing on startup.
			IEclipsePreferences node = this;
			for (int i = 2; i < segmentCount; i++) {
				node = (IEclipsePreferences) node.parent();
			}
			loadLevel = node;
		}
		return loadLevel;
	}

	@Override
	public IPath getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return SCOPE;
	}

	@Override
	public IEclipsePreferences getNode(String qualifier1) {
		throw new RuntimeException("BadTestScope throws this on purpose.");
	}

	@Override
	protected EclipsePreferences internalCreate(EclipsePreferences nodeParent, String nodeName, Object context) {
		return new BadTestScope(nodeParent, nodeName);
	}

	void setDirty(boolean value) {
		dirty = value;
	}

	Properties toProperties() throws BackingStoreException {
		return convertToProperties(new Properties(), "");
	}
}
