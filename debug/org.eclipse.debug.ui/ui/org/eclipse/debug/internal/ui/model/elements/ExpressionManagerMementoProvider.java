/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * Memento provider for expression manager.
 *
 * @since 3.4
 */
public class ExpressionManagerMementoProvider extends DebugElementMementoProvider {

	private static final String EXP_MGR = "EXP_MGR"; //$NON-NLS-1$

	@Override
	protected String getElementName(Object element, IPresentationContext context) throws CoreException {
		if (element instanceof IExpressionManager) {
			return EXP_MGR;
		}
		return null;
	}

}
