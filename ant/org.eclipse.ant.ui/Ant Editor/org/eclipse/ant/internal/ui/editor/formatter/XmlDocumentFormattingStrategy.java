/*******************************************************************************
 * Copyright (c) 2004, 2013 John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 * 	   IBM Corporation - bug 52076
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.formatter;

import java.util.LinkedList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

public class XmlDocumentFormattingStrategy extends ContextBasedFormattingStrategy {

	/** Documents to be formatted by this strategy */
	private final LinkedList<IDocument> fDocuments = new LinkedList<>();

	/** access to the preferences store * */
	private final FormattingPreferences prefs;

	private int indent = -1;

	public XmlDocumentFormattingStrategy() {
		this.prefs = new FormattingPreferences();
	}

	public XmlDocumentFormattingStrategy(FormattingPreferences prefs, int indent) {
		Assert.isNotNull(prefs);
		this.prefs = prefs;
		this.indent = indent;
	}

	@Override
	public void format() {

		super.format();
		final IDocument document = fDocuments.removeFirst();
		if (document != null) {
			// allow formatting of regions, not just the entire document; bug 75611
			String documentText = document.get();
			XmlDocumentFormatter formatter = new XmlDocumentFormatter();
			if (indent != -1) {
				formatter.setInitialIndent(indent);
			}
			formatter.setDefaultLineDelimiter(TextUtilities.getDefaultLineDelimiter(document));
			String formattedText = formatter.format(documentText, this.prefs);
			if (formattedText != null && !formattedText.equals(documentText)) {
				document.set(formattedText);
			}
		}
	}

	@Override
	public void formatterStarts(final IFormattingContext context) {
		super.formatterStarts(context);
		fDocuments.addLast((IDocument) context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
	}

	@Override
	public void formatterStops() {
		super.formatterStops();

		fDocuments.clear();
	}
}