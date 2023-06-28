/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;


/**
 * @since 3.2
 *
 */
public class TableRemoveRequestMonitor extends TableAddRequestMonitor {

	/**
	 * @param parent
	 * @param elements
	 * @param model
	 */
	TableRemoveRequestMonitor(ModelNode parent, Object[] elements, AsynchronousModel model) {
		super(parent, elements, model);
	}

	@Override
	protected void performUpdate() {
		((AsynchronousTableModel)getModel()).removed(fElements);
	}
}
