/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.merge;

import java.io.*;
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
        ArrayList ar = new ArrayList();
        while ((line = br.readLine()) != null) {
            ar.add(line);
        }
        // It is the responsibility of the caller to close the stream
        fLines = (String[]) ar.toArray(new String[ar.size()]);
    }

    String getLine(int ix) {
        return fLines[ix];
    }

    /* (non-Javadoc)
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#getRangeCount()
     */
    public int getRangeCount() {
        return fLines.length;
    }

    /* (non-Javadoc)
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#rangesEqual(int, org.eclipse.compare.rangedifferencer.IRangeComparator, int)
     */
    public boolean rangesEqual(int thisIndex, IRangeComparator other,
            int otherIndex) {
        String s1 = fLines[thisIndex];
        String s2 = ((LineComparator) other).fLines[otherIndex];
        return s1.equals(s2);
    }

    /* (non-Javadoc)
     * @see org.eclipse.compare.rangedifferencer.IRangeComparator#skipRangeComparison(int, int, org.eclipse.compare.rangedifferencer.IRangeComparator)
     */
    public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
        return false;
    }
}
