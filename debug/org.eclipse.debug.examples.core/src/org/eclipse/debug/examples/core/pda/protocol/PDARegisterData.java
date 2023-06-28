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

/**
 * Object representing a register in the registers command results.
 *
 * @see PDARCommand
 */

public class PDARegisterData {

	final public String fName;
	final public boolean fWritable;
	final public PDABitFieldData[] fBitFields;

	PDARegisterData(String regString) {
		StringTokenizer st = new StringTokenizer(regString, "|"); //$NON-NLS-1$

		String regInfo = st.nextToken();
		StringTokenizer regSt = new StringTokenizer(regInfo, " "); //$NON-NLS-1$
		fName = regSt.nextToken();
		fWritable = Boolean.getBoolean(regSt.nextToken());

		List<PDABitFieldData> bitFieldsList = new ArrayList<>();
		while (st.hasMoreTokens()) {
			bitFieldsList.add(new PDABitFieldData(st.nextToken()));
		}
		fBitFields = bitFieldsList.toArray(new PDABitFieldData[bitFieldsList.size()]);
	}
}