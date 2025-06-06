/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;

public class AntTemplateProposal extends TemplateProposal {

	public AntTemplateProposal(Template template, TemplateContext context, IRegion region, Image image, int relevance) {
		super(template, context, region, image, relevance);
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		try {
			int replaceOffset = getReplaceOffset();
			if (offset >= replaceOffset) {
				String content = document.get(replaceOffset, offset - replaceOffset);
				if (content.length() == 0) {
					return true;
				}
				if (content.charAt(0) == '<') {
					content = content.substring(1);
				}
				return getTemplate().getName().toLowerCase().startsWith(content.toLowerCase());
			}
		}
		catch (BadLocationException e) {
			// concurrent modification - ignore
		}
		return false;
	}
}
