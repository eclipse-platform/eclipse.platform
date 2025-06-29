/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.preferences;

import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.internal.core.AntObject;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Label provider for type elements
 */
public class AntObjectLabelProvider extends LabelProvider implements ITableLabelProvider {

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex != 0) {
			return null;
		}
		if (element instanceof Property prop) {
			if (prop.isDefault() && prop.isEclipseRuntimeRequired()) {
				return AntUIImages.getImage(IAntUIConstants.IMG_ANT_ECLIPSE_RUNTIME_OBJECT);
			}
			return getPropertyImage();
		} else if (element instanceof AntObject object) {
			if (object.isDefault() && object.isEclipseRuntimeRequired()) {
				return AntUIImages.getImage(IAntUIConstants.IMG_ANT_ECLIPSE_RUNTIME_OBJECT);
			}
			if (element instanceof Task) {
				return getTaskImage();
			}
			return getTypeImage();
		}
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof Property) {
			return getPropertyText((Property) element, columnIndex);
		} else if (element instanceof AntObject object) {
			switch (columnIndex) {
				case 0:
					return object.toString();
				case 1:
					return object.getClassName();
				case 2:
					return object.getLibraryEntry().getLabel();
				case 3:
					return object.getPluginLabel();
				default:
					break;
			}
		}

		return element.toString();
	}

	public String getPropertyText(Property property, int columnIndex) {
		switch (columnIndex) {
			case 0:
				return property.getName();
			case 1:
				return property.getValue(false);
			case 2:
				if (property.isDefault()) {
					return property.getPluginLabel();
				}
				break;
			default:
				break;
		}
		return IAntCoreConstants.EMPTY_STRING;
	}

	public static Image getTypeImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_ANT_TYPE);
	}

	public static Image getTaskImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJS_TASK_TSK);
	}

	public static Image getPropertyImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_PROPERTY);
	}
}