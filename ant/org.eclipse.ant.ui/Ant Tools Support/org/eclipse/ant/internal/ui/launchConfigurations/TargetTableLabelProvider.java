/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.internal.ui.model.AntImageDescriptor;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Ant target label provider
 */
public class TargetTableLabelProvider extends LabelProvider implements ITableLabelProvider, IColorProvider {

	public TargetTableLabelProvider() {
		super();
	}
	
	/* (non-Javadoc)
	 * Method declared on ILabelProvider.
	 */
	public String getText(Object model) {
		TargetInfo target = (TargetInfo) model;
		StringBuffer result = new StringBuffer(target.getName());
		if (target.isDefault()) {
			result.append(" ("); //$NON-NLS-1$;
			result.append(AntLaunchConfigurationMessages.getString("AntTargetLabelProvider.default_target_1")); //$NON-NLS-1$
			result.append(")"); //$NON-NLS-1$;
		}
		return result.toString();
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		TargetInfo target = (TargetInfo)element;
		ImageDescriptor base = null;
		int flags = 0;
		if (target.isDefault()) {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_DEFAULT_TARGET);
		} else if (target.getDescription() == null) {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET_INTERNAL);
		} else {
			base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET);
		}
		return AntUIImages.getImage(new AntImageDescriptor(base, flags));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getImage(element);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0){
			return getText(element);
		}
		String desc = ((TargetInfo)element).getDescription();
		if (desc == null) {
			return ""; //$NON-NLS-1$
		}
		return desc;
	}

	public Color getForeground(Object element) {
		if (!(element instanceof TargetInfo)) {
			return null;
		}
		TargetInfo info = (TargetInfo) element;
		if (info.isDefault()) {
			return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		}
		return null;
	}

	public Color getBackground(Object element) {
		return null;
	}

}
