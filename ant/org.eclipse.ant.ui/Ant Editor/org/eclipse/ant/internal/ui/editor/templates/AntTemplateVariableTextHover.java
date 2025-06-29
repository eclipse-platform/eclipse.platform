/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor.templates;

import java.util.Iterator;

import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class AntTemplateVariableTextHover implements ITextHover {

	public AntTemplateVariableTextHover() {
	}

	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion subject) {
		try {
			IDocument doc = textViewer.getDocument();
			int offset = subject.getOffset();
			if (offset >= 2 && "${".equals(doc.get(offset - 2, 2))) { //$NON-NLS-1$
				String varName = doc.get(offset, subject.getLength());
				TemplateContextType contextType = AntTemplateAccess.getDefault().getContextTypeRegistry().getContextType(TaskContextType.TASK_CONTEXT_TYPE);
				if (contextType != null) {
					Iterator<TemplateVariableResolver> iter = contextType.resolvers();
					while (iter.hasNext()) {
						TemplateVariableResolver var = iter.next();
						if (varName.equals(var.getType())) {
							return var.getDescription();
						}
					}
				}
			}
		}
		catch (BadLocationException e) {
			// do nothing
		}
		return null;
	}

	@Override
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (textViewer != null) {
			return XMLTextHover.getRegion(textViewer, offset);
		}
		return null;
	}
}
