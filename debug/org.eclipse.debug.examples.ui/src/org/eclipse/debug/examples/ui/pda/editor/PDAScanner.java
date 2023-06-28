/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.editor;

import org.eclipse.debug.examples.ui.pda.DebugUIPlugin;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;


/**
 * PDA editor keyword scanner.
 */
public class PDAScanner extends BufferedRuleBasedScanner {

	/**
	 * PDA keywods
	 */
	public static final String[] fgKeywords = new String[] {
 "add", "branch_not_zero", "call", "dec", "dup", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	"halt", "output", "pop", "push", "return", "var" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	};

	/**
	 * Detects potential keywords
	 */
	static class PDAWordDetector implements IWordDetector {

		@Override
		public boolean isWordStart(char c) {
			return Character.isLetter(c);
		}

		@Override
		public boolean isWordPart(char c) {
			return Character.isLetter(c) || c == '_';
		}
	}

	/**
	 * Detects PDA branch labels
	 */
	class PDALabelDetector extends PDAWordDetector {

		@Override
		public boolean isWordStart(char c) {
			return c == ':';
		}

		@Override
		public boolean isWordPart(char c) {
			return super.isWordPart(c) || Character.isDigit(c);
		}
	}

	/**
	 * Constructs a scanner that identifies PDA keywords.
	 */
	public PDAScanner() {
		// keywords
		Token token = new Token(new TextAttribute(DebugUIPlugin.getDefault().getColor(DebugUIPlugin.KEYWORD)));
		WordRule keywords = new WordRule(new PDAWordDetector());
		for (int i = 0; i < fgKeywords.length; i++) {
			String keyword = fgKeywords[i];
			keywords.addWord(keyword, token);
		}
		// labels
		token = new Token(new TextAttribute(DebugUIPlugin.getDefault().getColor(DebugUIPlugin.LABEL)));
		WordRule labels = new WordRule(new PDALabelDetector(), token);
		setRules(new IRule[]{keywords, labels});
	}
}
