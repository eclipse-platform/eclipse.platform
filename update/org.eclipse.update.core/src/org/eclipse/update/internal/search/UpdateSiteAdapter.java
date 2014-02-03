/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.search;

import java.net.*;

import org.eclipse.update.search.*;

/**
 */
public class UpdateSiteAdapter implements IUpdateSiteAdapter {
	private String label;
	private URL url;
	
	public UpdateSiteAdapter(String label, URL url) {
		this.label = label;
		this.url = url;
	}
	public URL getURL() {
		return url;
	}
	public String getLabel() {
		return label;
	}
/*
	public ISite getSite(IProgressMonitor monitor) {
		try {
			return SiteManager.getSite(getURL(), monitor);
		} catch (CoreException e) {
			return null;
		}
	}
*/
	public String toString(){
		return "" + getURL(); //$NON-NLS-1$
	}
}
