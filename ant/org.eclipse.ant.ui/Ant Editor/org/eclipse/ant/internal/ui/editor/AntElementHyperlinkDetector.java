/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor;

import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public class AntElementHyperlinkDetector extends AbstractHyperlinkDetector {

    private AntEditor fEditor;
    
    public AntElementHyperlinkDetector() {    
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		 if (region == null) {
			return null;
        }
		fEditor = (AntEditor) getAdapter(AntEditor.class);
        region= XMLTextHover.getRegion(textViewer, region.getOffset());
        Object linkTarget= fEditor.findTarget(region);
		if (linkTarget == null) {
			return null;
		}
        return new IHyperlink[] {new AntElementHyperlink(fEditor, region, linkTarget)};
	}
}
