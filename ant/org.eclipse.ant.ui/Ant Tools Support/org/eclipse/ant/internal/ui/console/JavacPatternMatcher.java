/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.console;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ui.console.PatternMatchEvent;

public class JavacPatternMatcher extends AbstractJavacPatternMatcher {

    /*
     *     [javac] /Users/kevinbarnes/Eclipse/runtime-workspace/Foo/src/CarriageReturn.java:4: ';' expected
     */
    public void matchFound(PatternMatchEvent event) {
        String matchedText= getMatchedText(event);
        if (matchedText == null) {
            return;
        }

        int numEnd= matchedText.lastIndexOf(':');
        while (numEnd > 1 && !Character.isDigit(matchedText.charAt(numEnd - 1))) {
            numEnd= matchedText.lastIndexOf(':', numEnd - 1);
        }
        int numStart= matchedText.lastIndexOf(':', numEnd - 1);
        
        int index = matchedText.indexOf("]"); //$NON-NLS-1$
        
        String filePath;
        if (numStart == -1) {
        	//file path from listfiles
        	filePath= matchedText.substring(index + 1);
        	filePath = filePath.trim();
        	int fileStart = matchedText.indexOf(filePath);
        	int eventOffset= event.getOffset() + fileStart;
            int eventLength = filePath.length();
        	addLink(filePath, -1, eventOffset, eventLength, null);
        } else {
        	filePath= matchedText.substring(index + 1, numStart);
        	filePath = filePath.trim();

        	int fileStart = matchedText.indexOf(filePath);
        	int eventOffset= event.getOffset() + fileStart;
        	int eventLength = filePath.length();

        	String lineNumberString = matchedText.substring(numStart + 1, numEnd);
        	int lineNumber= -1; 
        	try {
        		lineNumber= Integer.parseInt(lineNumberString);
        	} catch (NumberFormatException e) {
        		AntUIPlugin.log(e);
        	}

        	Integer type= fgErrorType;
        	if (-1 != matchedText.indexOf("warning", numEnd)) { //$NON-NLS-1$
        		type= fgWarningType;
        	}
        	addLink(filePath, lineNumber, eventOffset, eventLength, type);
        }
    }
}