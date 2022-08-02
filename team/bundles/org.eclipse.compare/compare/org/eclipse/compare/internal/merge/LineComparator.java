/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal.merge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.compare.rangedifferencer.IRangeComparator;

/**
 * This implementation of IRangeComparator breaks an input stream into lines.
 */
class LineComparator implements IRangeComparator {

	private String[] fLines;

	public LineComparator(InputStream is, String encoding) throws IOException {

		BufferedReader br = new BufferedReader(new InputStreamReader(is, encoding));
		String line;
		ArrayList<String> ar = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			ar.add(line);
		}
		// It is the responsibility of the caller to close the stream
		fLines = ar.toArray(new String[ar.size()]);
	}

	String getLine(int ix) {
		return fLines[ix];
	}

	@Override
	public int getRangeCount() {
		return fLines.length;
	}

	@Override
	public boolean rangesEqual(int thisIndex, IRangeComparator other,
			int otherIndex) {
		String s1 = fLines[thisIndex];
		String s2 = ((LineComparator) other).fLines[otherIndex];
		return s1.equals(s2);
	}

	@Override
	public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
		return false;
	}
}
