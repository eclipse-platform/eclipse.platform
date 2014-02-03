/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191783, NullPointerException in FeatureDownloader
 *******************************************************************************/

package org.eclipse.update.internal.core;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UpdateSession {
	
	private boolean enabled = false;
	private Set visitedURLs = Collections.synchronizedSet(new HashSet());
	
	UpdateSession() {
	}
	
	public boolean isVisited(URL url) {
		if (!enabled)
			return false;
		return visitedURLs.contains(url.toExternalForm());
	}

	public void markVisited(URL url) {
		if (!enabled)
			return ;
		visitedURLs.add(url.toExternalForm());
	}
	
	/*
	 * Session will not start caching URLs prior to calling this
	 * method. If you want to use update session facility make sure
	 * you call this method first
	 */
	public void reset() {
		this.enabled = true;
		visitedURLs.clear();
	}

}
