/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupFacility;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.ui.IWorkbenchPage;

/**
 * @since 3.2
 */
public class StackFrameSourceDisplayAdapter implements ISourceDisplay {

	@Override
	public synchronized void displaySource(Object context, IWorkbenchPage page, boolean force) {
		SourceLookupFacility.getDefault().displaySource(context, page, force);
	}
}
