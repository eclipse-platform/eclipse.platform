/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.help.internal.criteria;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.help.internal.util.ResourceLocator;

public class CriteriaDefinitionFile {

	private String pluginId;
	private String file;
	private String locale;

	public CriteriaDefinitionFile(String pluginId, String file, String locale) {
		this.pluginId = pluginId;
		this.file = file;
		this.locale = locale;
	}

	public String getFile() {
		return file;
	}

	public String getLocale() {
		return locale;
	}

	public String getPluginId() {
		return pluginId;
	}

	public InputStream getInputStream() throws IOException {
		if (pluginId != null)
			return ResourceLocator.openFromPlugin(pluginId, file, locale);
		else
			return new FileInputStream(file);
	}
}
