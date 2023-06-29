/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A presentation context tied to a part.
 *
 * @since 3.3
 * @deprecated getPart() is now supported by IPresentationContext itself.
 */
@Deprecated
public class PartPresentationContext extends PresentationContext {

	/**
	 * Constructs a part presentation context.
	 *
	 * @param part part
	 */
	public PartPresentationContext(IWorkbenchPart part) {
		super(part);
	}

	@Override
	public IWorkbenchPart getPart() {
		return super.getPart();
	}

}
