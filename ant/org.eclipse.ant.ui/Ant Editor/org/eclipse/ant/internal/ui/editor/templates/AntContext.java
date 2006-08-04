/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.templates;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.formatter.FormattingPreferences;
import org.eclipse.ant.internal.ui.editor.formatter.XmlFormatter;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;

public class AntContext extends DocumentTemplateContext {
	
	private IAntModel fAntModel;
	
	public AntContext(TemplateContextType type, IDocument document, IAntModel model, Position position) {
		super(type, document, position);
		fAntModel= model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.TemplateContext#evaluate(org.eclipse.jface.text.templates.Template)
	 */
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {

		TemplateBuffer templateBuffer= super.evaluate(template);
		
		if (templateBuffer == null) {
			return null;
		}
		//TODO Not enabled see bug 55356
		if (false && AntUIPlugin.getDefault().getPreferenceStore().getBoolean(AntEditorPreferenceConstants.TEMPLATES_USE_CODEFORMATTER)) {
			FormattingPreferences prefs = new FormattingPreferences();
			XmlFormatter.format(templateBuffer, this, prefs);
		}
		return templateBuffer;
	}
	
	/**
	 * @return Returns the AntModel.
	 */
	public IAntModel getAntModel() {
		return fAntModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.DocumentTemplateContext#getEnd()
	 */
	public int getEnd() {
		int replacementOffset = getCompletionOffset();
		int replacementLength = getCompletionLength();
		if (replacementOffset > 0 && getDocument().get().charAt(replacementOffset - 1) == '<' && getDocument().getLength() > 1) {
			replacementLength++;
		}
		return replacementLength + replacementOffset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.templates.DocumentTemplateContext#getStart()
	 */
	public int getStart() {
		int replacementOffset= getCompletionOffset();
		if (replacementOffset > 0 && getDocument().get().charAt(replacementOffset - 1) == '<') {
			replacementOffset--;
		}
		return replacementOffset;
	}
}
