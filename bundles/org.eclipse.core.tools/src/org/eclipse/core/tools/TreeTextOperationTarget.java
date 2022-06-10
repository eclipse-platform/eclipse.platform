/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.swt.widgets.Tree;

public class TreeTextOperationTarget implements ITextOperationTarget {

	private Tree tree;

	public TreeTextOperationTarget(Tree tree) {
		this.tree = tree;
	}

	@Override
	public boolean canDoOperation(int operation) {
		return true;
	}

	@Override
	public void doOperation(int operation) {
		//TODO: add support to other operations
		switch (operation) {
			case ITextOperationTarget.SELECT_ALL :
				tree.selectAll();
		}
	}
}
