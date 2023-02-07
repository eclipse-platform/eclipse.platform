/*******************************************************************************
 * Copyright (c) 2005, 2015 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.help.internal.util.ResourceLocator;

public class IndexFile {

	private String pluginId;
	private String file;
	private String locale;

	public IndexFile(String pluginId, String file, String locale) {
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
