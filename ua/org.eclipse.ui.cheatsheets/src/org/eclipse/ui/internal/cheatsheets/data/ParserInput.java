/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.data;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The input to the parser which can be the URL of a file
 * or a string of XML
 */

public class ParserInput {
	private URL url;
	private String xml;
	private String pluginId;
	private String errorMessage;

	public ParserInput() {
		url = null;
		xml = null;
	}

	public ParserInput(String xml, String basePath) {
		this.xml = xml;
		this.url = null;
		this.errorMessage = null;
		if (basePath != null) {
			try {
				this.url = new URL(basePath);
			} catch (MalformedURLException e) {
				// leave the url null
			}
		}
	}

	public ParserInput(URL url, String pluginId, String errorMessage) {
		this.url = url;
		this.xml = null;
		this.errorMessage = errorMessage;
		this.pluginId = pluginId;
	}

	public URL getUrl() {
		return url;
	}

	public String getXml() {
		return xml;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
