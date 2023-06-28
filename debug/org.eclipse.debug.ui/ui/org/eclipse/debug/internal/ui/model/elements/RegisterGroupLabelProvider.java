/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;

/**
 * @since 3.3
 */
public class RegisterGroupLabelProvider extends DebugElementLabelProvider {

	@Override
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		if (columnId == null || IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnId)) {
			return super.getImageDescriptor(elementPath, presentationContext, columnId);
		}
		return null;
	}

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext context, String columnId) throws CoreException {
		if (columnId == null || IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnId)) {
			return super.getLabel(elementPath, context, columnId);
		} else {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}
	}

}
