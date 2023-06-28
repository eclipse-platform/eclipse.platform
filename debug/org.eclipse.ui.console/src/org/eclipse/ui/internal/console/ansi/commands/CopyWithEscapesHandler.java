/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.console.ansi.utils.AnsiClipboardUtils;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.PageBookView;

public class CopyWithEscapesHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if (part != null) {
			if (part instanceof PageBookView) {
				final IPage currentPage = ((PageBookView) part).getCurrentPage();
				if (currentPage != null) {
					final Control control = currentPage.getControl();
					if (control instanceof StyledText) {
						AnsiClipboardUtils.textToClipboard((StyledText) control, false);
					}
				}
			}
		}
		return null;
	}
}
