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
package org.eclipse.ant.internal.ui.views;

import org.eclipse.ant.internal.ui.model.AntImageDescriptor;
import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.views.elements.ProjectNode;
import org.eclipse.ant.internal.ui.views.elements.TargetNode;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * A label provider that provides labels for elements displayed in the
 * <code>AntView</code>
 */
public class AntViewLabelProvider implements ILabelProvider, IColorProvider {

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof ProjectNode) {
			ProjectNode project= (ProjectNode) element;
			int flags = 0;
			if (project.isErrorNode()) {
				flags = flags | AntImageDescriptor.HAS_ERRORS;
			}
			CompositeImageDescriptor descriptor = new AntImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_PROJECT), flags);
			return AntUIImages.getImage(descriptor);
		} else if (element instanceof TargetNode) {
			TargetNode target= (TargetNode) element;
			int flags = 0;
			ImageDescriptor base = null;
			if (target.equals(target.getProject().getDefaultTarget())){
				base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_DEFAULT_TARGET);
			} else if (target.getDescription() == null) {
				base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET_INTERNAL);
			} else {
				base = AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ANT_TARGET);
			}			
			if (target.isErrorNode()) {
				flags = flags | AntImageDescriptor.HAS_ERRORS;
			}
			return AntUIImages.getImage(new AntImageDescriptor(base, flags));
		}
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof TargetNode) {
			TargetNode node= (TargetNode)element;
			StringBuffer name= new StringBuffer(node.getName());
			if (node.getName().equals(node.getProject().getDefaultTargetName())) {
				name.append(AntViewMessages.getString("TargetNode.default")); //$NON-NLS-1$
			} 
			return name.toString();
		} else if (element instanceof ProjectNode) {
			ProjectNode project= (ProjectNode) element;
			StringBuffer buffer= new StringBuffer(project.getName());
			String defaultTarget= project.getDefaultTargetName();
			if (defaultTarget != null) {
				buffer.append(" [").append(defaultTarget).append(']'); //$NON-NLS-1$
			}
			return buffer.toString();
		} else {
			return element.toString();
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	public Color getForeground(Object element) {
		if (element instanceof TargetNode) {
			TargetNode target= (TargetNode)element;
			if (target == target.getProject().getDefaultTarget()) {
				return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	public Color getBackground(Object element) {
		return null;
	}
}
