/*******************************************************************************
 * Copyright (c) 2002, 2013 GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;

/**
 * The scanner to tokenize for XML processing instructions and text
 */
public class AntEditorProcInstrScanner extends AbstractAntEditorScanner {

	Token fProcInstructionToken = null;

	public AntEditorProcInstrScanner() {
		IRule[] rules = new IRule[2];
		fProcInstructionToken = new Token(createTextAttribute(SyntaxThemeConstants.DIRECTIVE_COLOR));

		// Add rule for processing instructions
		rules[0] = new MultiLineRule("<?", "?>", fProcInstructionToken); //$NON-NLS-1$ //$NON-NLS-2$

		// Add generic whitespace rule.
		rules[1] = new WhitespaceRule(new AntEditorWhitespaceDetector());

		setRules(rules);

		// unstyled text follows the editor foreground colour
		setDefaultReturnToken(new Token(createTextAttribute(null)));
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (event.getProperty().endsWith(SyntaxThemeConstants.DIRECTIVE_COLOR)) {
			adaptToColorChange(fProcInstructionToken, SyntaxThemeConstants.DIRECTIVE_COLOR);
		}
	}
}
