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
package org.eclipse.ant.internal.ui.datatransfer;

import java.text.MessageFormat;

import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModelLabelProvider;
import org.eclipse.jface.viewers.StyledString;

/**
 * Ant javac task label provider for a table
 */
public class JavacTableLabelProvider extends AntModelLabelProvider {

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof AntElementNode node) {
			AntElementNode parent = (AntElementNode) node.getParentNode();
			if (parent != null) {
				return new StyledString(MessageFormat.format(DataTransferMessages.JavacTableLabelProvider_0, parent.getLabel()));
			}
		}
		return super.getStyledText(element);
	}
}
