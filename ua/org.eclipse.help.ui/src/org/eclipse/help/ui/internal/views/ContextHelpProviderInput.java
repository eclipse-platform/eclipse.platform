/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;

public class ContextHelpProviderInput {
	private IContext context;
	private IContextProvider provider;
	private Control control;
	private IWorkbenchPart part;
	private boolean explicitRequest;
	public ContextHelpProviderInput(IContextProvider provider, IContext context, Control control, IWorkbenchPart part, boolean explicitRequest) {
		this.provider = provider;
		this.context = context;
		this.control =control;
		this.part = part;
		this.explicitRequest = explicitRequest;
	}

	public IContextProvider getProvider() {
		return provider;
	}
	public IContext getContext() {
		return context;
	}
	public Control getControl() {
		return control;
	}
	public IWorkbenchPart getPart() {
		return part;
	}

	public boolean isExplicitRequest() {
		return explicitRequest;
	}
}
