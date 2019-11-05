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
package org.eclipse.team.internal.ccvs.core.client.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;

/**
 * This class extracts matches server lines to expected patterns and extracts
 * required information from the line.
 */
public class ServerMessageLineMatcher {

	protected static final Pattern VARIABLE_MATCHING_PATTERN = Pattern.compile("(\\((\\w*):.*:\\2\\))"); //$NON-NLS-1$
	
	Pattern pattern;
	String[] variables;

	public ServerMessageLineMatcher(String template, String[] expectedVariables) throws CVSException {
		// Extract the variable names from the template
		Matcher matcher = VARIABLE_MATCHING_PATTERN.matcher(template);
		List<String> variables = new ArrayList<>();
		while (matcher.find()) {
			if (matcher.groupCount() != 2) {
				IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.ServerMessageLineMatcher_5, new String[] { template }));
				throw new CVSException(status); 
			}
			variables.add(matcher.group(2));
		}
		ensureMatches(template, variables, expectedVariables);
		this.variables = variables.toArray(new String[variables.size()]);

		// Modify the template so it can be used to match message lines from the server
		// (i.e. remove the variable markup)
		for (Iterator iter = variables.iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				template = template.replaceAll(element + ":", ""); //$NON-NLS-1$ //$NON-NLS-2$
				template = template.replaceAll(":" + element, ""); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Ensure that the number of groups in the pattern match the number of variables
		int count = 0;
		int start = -1;
		while ((start = template.indexOf('(', start + 1)) != -1) {
			count++;
		}
		if (count != variables.size()) {
			IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.ServerMessageLineMatcher_6, new String[] { template }));
			throw new CVSException(status); 
		}

		// Create the pattern fir matching lines from the server
		this.pattern = Pattern.compile(template);
	}

	/*
	 * Throw an exception if the found variables do not match the expected variables
	 */
	private void ensureMatches(String template, List variables, String[] expectedVariables) throws CVSException {
		for (String expected : expectedVariables) {
			if (!variables.contains(expected)) {
				IStatus status = new CVSStatus(IStatus.ERROR,NLS.bind(CVSMessages.ServerMessageLineMatcher_7, new String[] { expected, template }));
				throw new CVSException(status); 
			}
		}
	}

	public Map processServerMessage(String line) {
		Matcher matcher = pattern.matcher(line);
		if (!matcher.find()) return null;
		Assert.isTrue(matcher.groupCount() == variables.length);
		Map<String,String> result = new HashMap<>();
		for (int i = 1; i <= matcher.groupCount(); i++) {
			result.put(variables[i - 1], matcher.group(i));
		}
		return result;
	}

}
