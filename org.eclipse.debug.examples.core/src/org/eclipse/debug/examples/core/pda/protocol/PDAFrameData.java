/*******************************************************************************
 * Copyright (c) 2008, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IPath;

/**
 * Object representing a frame in the stack command results.
 *
 * @see PDAStackCommand
 */

public class PDAFrameData {

	final public IPath fFilePath;
	final public int fPC;
	final public String fFunction;
	final public String[] fVariables;

	PDAFrameData(String frameString) {
		StringTokenizer st = new StringTokenizer(frameString, "|"); //$NON-NLS-1$

		fFilePath = IPath.fromOSString(st.nextToken());
		fPC = Integer.parseInt(st.nextToken());
		fFunction = st.nextToken();

		List<String> variablesList = new ArrayList<>();
		while (st.hasMoreTokens()) {
			variablesList.add(st.nextToken());
		}
		fVariables = variablesList.toArray(new String[variablesList.size()]);
	}
}