/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;

public class AntInformationProvider implements IInformationProvider {

	XMLTextHover fTextHover;

	public AntInformationProvider(XMLTextHover hover) {
		fTextHover = hover;
	}

	@Override
	public IRegion getSubject(ITextViewer textViewer, int offset) {
		return fTextHover.getHoverRegion(textViewer, offset);
	}

	@Override
	public String getInformation(ITextViewer textViewer, IRegion subject) {
		return fTextHover.getHoverInfo(textViewer, subject);
	}
}
