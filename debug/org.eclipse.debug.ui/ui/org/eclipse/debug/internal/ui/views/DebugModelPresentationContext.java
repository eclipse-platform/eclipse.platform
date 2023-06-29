/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views;

import org.eclipse.debug.internal.ui.viewers.model.provisional.PresentationContext;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A presentation context that has a debug model presentation.
 *
 * @since 3.3
 */
public class DebugModelPresentationContext extends PresentationContext {

	private IDebugModelPresentation fPresentation;

	/**
	 * Constructs a presentation context for the given id using the
	 * specified model presentation.
	 *
	 * @param id context id
	 * @param part workbench view
	 * @param presentation debug model presentation
	 */
	public DebugModelPresentationContext(String id, IWorkbenchPart part, IDebugModelPresentation presentation) {
		super(id, part);
		fPresentation = presentation;
	}

	public IDebugModelPresentation getModelPresentation() {
		return fPresentation;
	}

}
