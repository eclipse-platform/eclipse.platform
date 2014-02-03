/*******************************************************************************
 * Copyright (c) 2002, 2012 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug 31796, bug 24108, bug 47139
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;

/**
 * The scanner to tokenize for strings and tags
 */
public class AntEditorTagScanner extends AbstractAntEditorScanner {

	private Token fStringToken;
	
    public AntEditorTagScanner() {
    	fStringToken= new Token(
    			createTextAttribute(IAntEditorColorConstants.STRING_COLOR, 
    					IAntEditorColorConstants.STRING_COLOR + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX,
						IAntEditorColorConstants.STRING_COLOR + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX));
                    
		IRule[] rules= new IRule[3];

        // Add rule for single and double quotes
        rules[0]= new MultiLineRule("\"", "\"", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
        rules[1]= new SingleLineRule("'", "'", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$

        // Add generic whitespace rule.
        rules[2]= new WhitespaceRule(new AntEditorWhitespaceDetector());

        setRules(rules);
        
        setDefaultReturnToken(
        		new Token(createTextAttribute(IAntEditorColorConstants.TAG_COLOR, 
    					IAntEditorColorConstants.TAG_COLOR + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX,
						IAntEditorColorConstants.TAG_COLOR + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)));
    }
    
    public void adaptToPreferenceChange(PropertyChangeEvent event) {
    	String property= event.getProperty();
    	if (property.startsWith(IAntEditorColorConstants.TAG_COLOR) || property.startsWith(IAntEditorColorConstants.STRING_COLOR)) {
    		if (property.endsWith(AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX)) {
    			adaptToStyleChange(event, getTokenAffected(event), SWT.BOLD);
    		} else if (property.endsWith(AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)) {
    			adaptToStyleChange(event, getTokenAffected(event), SWT.ITALIC);
    		} else {
    			adaptToColorChange(event, getTokenAffected(event));
    		}
    	}
    }
    
    private Token getTokenAffected(PropertyChangeEvent event) {
    	String property= event.getProperty();
    	if (property.startsWith(IAntEditorColorConstants.STRING_COLOR)) {
    		return fStringToken;
    	}// else if (property.startsWith(IAntEditorColorConstants.TAG_COLOR)) {
    		return (Token)fDefaultReturnToken;
    	//}
    }
}
