/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.util;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;

/**
 * A FileNameMatcher associates a String with a String pattern.
 */
public class FileNameMatcher {
	
	private List matchers = new ArrayList();
	private List results = new ArrayList();
	private static final String TRUE = "true"; //$NON-NLS-1$
	
	public FileNameMatcher() {
	}
	
	public FileNameMatcher(String[] patterns) {
		register(patterns);
	}
		
	void register(String[] patterns) {
		for (String pattern : patterns) {
			register(pattern, TRUE);
		}
	}
	
	public void register(String pattern, String result) {
		
		Assert.isTrue(matchers.size() == results.size());
		
		pattern = pattern.trim();
		
		// The empty pattern matches everything, but we want to match
		// nothing with it, so we just do not register anything
		if (pattern.length() == 0) {
			return;
		}
	
		matchers.add(new StringMatcher(pattern,false,false));
		results.add(result);
		
	}
	
	public String getMatch(String name) {
		StringMatcher stringMatcher;
		
		for (int i = 0; i < matchers.size(); i++) {
			stringMatcher = (StringMatcher) matchers.get(i);
			if (stringMatcher.match(name)) {
				return (String)results.get(i);
			}
		}
		
		return null;
	}
	
	public boolean match(String name) {
		return getMatch(name) != null;
	}
}
