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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.part.IDropActionDelegate;

public class CVSResourceDropAdapter implements IDropActionDelegate {

	public boolean run(Object source, Object target) {

		if (source != null && target instanceof IHistoryView) {
			CVSResourceTransfer transfer = CVSResourceTransfer.getInstance();
			Object file = transfer.fromByteArray((byte[]) source);
			((IHistoryView) target).showHistoryFor(file);

		}
		return false;
	}

}
