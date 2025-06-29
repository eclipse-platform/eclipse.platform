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
package org.eclipse.ant.internal.ui.preferences;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.core.IAntClasspathEntry;

public abstract class AbstractClasspathEntry implements IClasspathEntry {

	protected List<IAntClasspathEntry> fChildEntries = new ArrayList<>();
	protected IClasspathEntry fParent = null;

	@Override
	public IAntClasspathEntry[] getEntries() {
		return fChildEntries.toArray(new IAntClasspathEntry[fChildEntries.size()]);
	}

	@Override
	public boolean hasEntries() {
		return !fChildEntries.isEmpty();
	}

	@Override
	public IClasspathEntry getParent() {
		return fParent;
	}

	/**
	 * @param parent
	 *            The parent to set.
	 */
	public void setParent(IClasspathEntry parent) {
		fParent = parent;
	}

	@Override
	public URL getEntryURL() {
		return null;
	}

	@Override
	public String getLabel() {
		return toString();
	}

	@Override
	public boolean isEclipseRuntimeRequired() {
		return false;
	}
}
