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
package org.eclipse.ant.internal.ui.console;

import org.eclipse.ui.console.PatternMatchEvent;

public class EclipseJavacPatternMatcher extends AbstractJavacPatternMatcher {

    private static final String fgError= "ERROR in"; //$NON-NLS-1$
    private static final String fgWarning= "WARNING in"; //$NON-NLS-1$
    private static final String fgStartOfLineNumber= " ("; //$NON-NLS-1$
    
    /* [javac] 1. ERROR in /Users/kevinbarnes/Eclipse/runtime-workspace/org.eclipse.ant.core/src_ant/org/eclipse/ant/internal/core/ant/InternalAntRunner.java (at line 66)
     */
    public void matchFound(PatternMatchEvent event) {
        String matchedText= getMatchedText(event);
        if (matchedText == null) {
            return;
        }
        int index = matchedText.indexOf(fgError);
        String filePath;
        Integer type= fgErrorType;
        if (index == -1) {
            index = matchedText.indexOf(fgWarning);
            filePath= matchedText.substring(index + 10).trim();
            type= fgWarningType;
        } else {
            filePath= matchedText.substring(index + 8).trim();
        }

        int lineNumberStart = filePath.lastIndexOf(fgStartOfLineNumber);
        if (lineNumberStart != -1) {
        	filePath = filePath.substring(0, lineNumberStart);
        }
        
        int fileStart = matchedText.indexOf(filePath);
        int eventOffset= event.getOffset() + fileStart;
        int eventLength = filePath.length();
        
        int lineNumber = getLineNumber(lineNumberStart + eventOffset, true);
        addLink(filePath, lineNumber, eventOffset, eventLength, type);
    }
}