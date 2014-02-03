/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected List fChildEntries = new ArrayList();
	protected IClasspathEntry fParent = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IClasspathEntry#getEntries()
	 */
	public IAntClasspathEntry[] getEntries() {
		return (IAntClasspathEntry[])fChildEntries.toArray(new IAntClasspathEntry[fChildEntries.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IClasspathEntry#hasEntries()
	 */
	public boolean hasEntries() {
		return !fChildEntries.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.preferences.IClasspathEntry#getParent()
	 */
	public IClasspathEntry getParent() {
		return fParent;
	}
	
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(IClasspathEntry parent) {
		fParent = parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.core.IAntClasspathEntry#getEntryURL()
	 */
	public URL getEntryURL() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.core.IAntClasspathEntry#getLabel()
	 */
	public String getLabel() {
		return toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ant.core.IAntClasspathEntry#isEclipseRuntimeRequired()
	 */
	public boolean isEclipseRuntimeRequired() {
		return false;
	}
}
