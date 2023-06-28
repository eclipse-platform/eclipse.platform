/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.internal.ui.codemining;

import java.util.function.Consumer;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineEndCodeMining;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class DebugValueCodeMining extends LineEndCodeMining {

	private IVariable variable;

	protected DebugValueCodeMining(IDocument document, int line, IVariable variable, ICodeMiningProvider provider)
			throws BadLocationException {
		super(document, line, provider);
		this.variable = variable;
		setLabel(DebugUIPlugin.getModelPresentation().getText(variable));
	}

	@Override
	public Consumer<MouseEvent> getAction() {
		return e -> openVariableInVariablesView(variable);
	}

	private static void openVariableInVariablesView(IVariable variable) {

		VariablesView view;
		try {
			view = (VariablesView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(IDebugUIConstants.ID_VARIABLE_VIEW);
			view.getViewer().setSelection(new StructuredSelection(variable));
		} catch (PartInitException e) {
			DebugUIPlugin.log(e);
		}
	}

	@Override
	public boolean isResolved() {
		return true;
	}

}
