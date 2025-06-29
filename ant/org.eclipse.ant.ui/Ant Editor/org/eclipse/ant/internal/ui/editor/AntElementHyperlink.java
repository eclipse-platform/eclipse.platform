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
package org.eclipse.ant.internal.ui.editor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class AntElementHyperlink implements IHyperlink {

	private IRegion fRegion = null;
	private Object fLinkTarget = null;
	private AntEditor fEditor = null;

	public AntElementHyperlink(AntEditor editor, IRegion region, Object linkTarget) {

		fRegion = region;
		fLinkTarget = linkTarget;
		fEditor = editor;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	@Override
	public String getTypeLabel() {
		return null;
	}

	@Override
	public String getHyperlinkText() {
		return null;
	}

	@Override
	public void open() {
		fEditor.openTarget(fLinkTarget);
	}
}
