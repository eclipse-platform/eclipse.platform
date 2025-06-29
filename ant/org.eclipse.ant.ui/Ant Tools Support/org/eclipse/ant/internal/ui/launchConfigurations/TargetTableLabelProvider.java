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
package org.eclipse.ant.internal.ui.launchConfigurations;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModelLabelProvider;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Ant target label provider for a table
 */
public class TargetTableLabelProvider extends AntModelLabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return getImage(element);
		}
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (columnIndex == 0) {
			AntElementNode node = (AntElementNode) element;
			return node.getLabel();
		}
		String desc = ((AntTargetNode) element).getTarget().getDescription();
		if (desc == null) {
			return IAntCoreConstants.EMPTY_STRING;
		}
		return desc;
	}
}
