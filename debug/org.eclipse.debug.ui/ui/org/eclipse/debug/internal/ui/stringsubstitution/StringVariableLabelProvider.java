/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.stringsubstitution;

import org.eclipse.core.variables.IStringVariable;
import org.eclipse.jface.viewers.LabelProvider;


public class StringVariableLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof IStringVariable) {
				IStringVariable variable = (IStringVariable)element;
				return variable.getName();
			}
			return super.getText(element);
		}

}
