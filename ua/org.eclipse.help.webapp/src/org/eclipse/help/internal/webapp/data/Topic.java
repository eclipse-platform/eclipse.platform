/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;

public class Topic {
	private String label;
	private String href;

	public Topic(String label, String href) {
		this.label = label;
		this.href = href;
	}

	public String getLabel() {
		return label;
	}

	public String getHref() {
		return UrlUtil.getHelpURL(href);
	}

}
