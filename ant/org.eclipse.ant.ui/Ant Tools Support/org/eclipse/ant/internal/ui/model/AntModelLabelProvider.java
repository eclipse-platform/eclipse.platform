/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.model;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


public class AntModelLabelProvider extends LabelProvider implements IColorProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(Object)
	 */
	public Image getImage(Object anElement) {
		AntElementNode node = (AntElementNode)anElement;
		return node.getImage();
	}
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
	 */
	public String getText(Object node) {
		AntElementNode element= (AntElementNode) node;
		return element.getLabel();
	}

	public Color getForeground(Object node) {
		if (node instanceof AntTargetNode && ((AntTargetNode)node).isDefaultTarget() ) {
			return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		}
		
		return null;
	}

	public Color getBackground(Object element) {
		return null;
	}
}
