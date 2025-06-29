/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;

public class ClasspathEntry extends AbstractClasspathEntry {

	private URL fUrl = null;
	private String fVariableString = null;
	private IAntClasspathEntry fEntry = null;

	public ClasspathEntry(Object o, IClasspathEntry parent) {
		fParent = parent;
		if (o instanceof URL) {
			fUrl = (URL) o;
		} else if (o instanceof String) {
			fVariableString = (String) o;
		} else if (o instanceof IAntClasspathEntry) {
			fEntry = (IAntClasspathEntry) o;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IAntClasspathEntry other) {
			return other.getLabel().equals(getLabel());
		}
		return false;

	}

	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}

	@Override
	public String toString() {
		if (fEntry != null) {
			return fEntry.getLabel();
		}
		if (getURL() != null) {
			return getURL().getFile();
		}

		return getVariableString();
	}

	protected URL getURL() {
		return fUrl;
	}

	protected String getVariableString() {
		return fVariableString;
	}

	@Override
	public String getLabel() {
		if (fEntry == null) {
			return toString();
		}
		return fEntry.getLabel();
	}

	@Override
	public URL getEntryURL() {
		if (fEntry != null) {
			return fEntry.getEntryURL();
		}
		if (fUrl != null) {
			return fUrl;
		}

		try {
			String expanded = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(fVariableString);
			return new URL(IAntCoreConstants.FILE_PROTOCOL + expanded);
		}
		catch (CoreException e) {
			AntUIPlugin.log(e);
		}
		catch (MalformedURLException e) {
			AntUIPlugin.log(e);
		}
		return null;
	}

	@Override
	public boolean isEclipseRuntimeRequired() {
		if (fEntry == null) {
			return super.isEclipseRuntimeRequired();
		}
		return fEntry.isEclipseRuntimeRequired();
	}
}
