/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui;

import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

public class AntDoubleClickStrategy implements ITextDoubleClickStrategy {

	@Override
	public void doubleClicked(ITextViewer textViewer) {
		int offset = textViewer.getSelectedRange().x;

		if (offset < 0) {
			return;
		}
		IRegion region = XMLTextHover.getRegion(textViewer, offset);
		if (region != null) {
			textViewer.setSelectedRange(region.getOffset(), region.getLength());
		}
	}
}