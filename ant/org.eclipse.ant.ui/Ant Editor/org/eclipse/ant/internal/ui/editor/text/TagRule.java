/*******************************************************************************
 * Copyright (c) 2002, 2006 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug 24108, bug 111740
 *     John-Mason P. Shackelford - bug 51215
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

/*
 * This file originates from an internal package of Eclipse's 
 * Manifest Editor. It has been copied by GEBIT to here in order to
 * permanently use those features. It has been renamed and edited by GEBIT 
 * after copying.
 */

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;

public class TagRule extends MultiLineRule {

    public TagRule(IToken token) {
        super("<", ">", token); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
        int c = scanner.read();
        if (sequence[0] == '<') {
            if (c == '?') {
                // processing instruction - abort
                scanner.unread();
                return false;
            }
            if (c == '!') {
                scanner.unread();
                // comment - abort
                return false;
            }
        } else if (sequence[0] == '>') {
            scanner.unread();
        }

        return super.sequenceDetected(scanner, sequence, eofAllowed);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.rules.PatternRule#endSequenceDetected(org.eclipse.jface.text.rules.ICharacterScanner)
     */
    protected boolean endSequenceDetected(ICharacterScanner scanner) {
        int c;
        while ((c = scanner.read()) != ICharacterScanner.EOF) {
            if (c == fEscapeCharacter) {
                // Skip the escaped character.
                scanner.read();
            } else if (c == '>') {
                endOfTagDetected(scanner);
                return true;
            } 
        }
        
        scanner.unread();
        return false;
    }

    private void endOfTagDetected(ICharacterScanner scanner) {
        int c;
        int scanAhead = 0;
        int endOfTagOffset = 0;
        while ((c = scanner.read()) != ICharacterScanner.EOF && c != '<') {
            scanAhead++;
            if (c == '>') {
            	endOfTagOffset = scanAhead;
            }
        }

        if (c == '<') {
            int rewind = (scanAhead - endOfTagOffset) + 1;
            for (int i = 0; i < rewind; i++) {
                scanner.unread();
            }
        }
    }    
}
