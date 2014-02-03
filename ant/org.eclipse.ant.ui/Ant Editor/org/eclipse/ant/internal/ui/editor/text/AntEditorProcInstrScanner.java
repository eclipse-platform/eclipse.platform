/*******************************************************************************
 * Copyright (c) 2002, 2005 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;

/**
 * The scanner to tokenize for XML processing instructions and text
 */
public class AntEditorProcInstrScanner extends AbstractAntEditorScanner {

	Token fProcInstructionToken= null;
	
    public AntEditorProcInstrScanner() {
		IRule[] rules =new IRule[2];
        fProcInstructionToken =
            new Token(createTextAttribute(IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR, 
					IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX,
					IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX));

        //Add rule for processing instructions
        rules[0]= new MultiLineRule("<?", "?>", fProcInstructionToken); //$NON-NLS-1$ //$NON-NLS-2$

        // Add generic whitespace rule.
        rules[1]= new WhitespaceRule(new AntEditorWhitespaceDetector());

        setRules(rules);
        
        setDefaultReturnToken(new Token(createTextAttribute(IAntEditorColorConstants.TEXT_COLOR, 
    							IAntEditorColorConstants.TEXT_COLOR + AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX,
								IAntEditorColorConstants.TEXT_COLOR + AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)));
    }

	private Token getTokenAffected(PropertyChangeEvent event) {
    	if (event.getProperty().startsWith(IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR)) {
    		return fProcInstructionToken;
    	}
    	return (Token)fDefaultReturnToken;
    }
    
    public void adaptToPreferenceChange(PropertyChangeEvent event) {
    	String property= event.getProperty();
    	if (property.startsWith(IAntEditorColorConstants.TEXT_COLOR) || property.startsWith(IAntEditorColorConstants.PROCESSING_INSTRUCTIONS_COLOR)) {    		
    		if (property.endsWith(AntEditorPreferenceConstants.EDITOR_BOLD_SUFFIX)) {
	    		adaptToStyleChange(event, getTokenAffected(event), SWT.BOLD);
	    	} else if (property.endsWith(AntEditorPreferenceConstants.EDITOR_ITALIC_SUFFIX)) {
	    		adaptToStyleChange(event, getTokenAffected(event), SWT.ITALIC);
	    	} else {
	    		adaptToColorChange(event, getTokenAffected(event));
	    	}
    	}
    }
}