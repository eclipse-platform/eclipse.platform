/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.memory.AbstractMemoryRendering;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A dummy rendering for displaying an error message in a view tab.
 * @since 3.1
 */
/**
 * @author chanskw
 *
 */
public class ErrorRendering extends AbstractMemoryRendering {

	private TextViewer fTextViewer;
	private String fRenderingId;
	private Throwable fException;

	/**
	 * @param renderingId - id of rendering that the memory view has failed
	 * to create.
	 */
	public ErrorRendering(String renderingId, Throwable exception)
	{
		super("org.eclipse.debug.internal.ui.views.memory.errorrendering"); //$NON-NLS-1$
		fRenderingId = renderingId;
		fException = exception;
	}

	@Override
	public Control createControl(Composite parent) {
		fTextViewer = new TextViewer(parent, SWT.READ_ONLY);
		fTextViewer.setDocument(new Document());
		StyledText styleText = fTextViewer.getTextWidget();

		styleText.setText("\r\n\r\n" + DebugUIMessages.EmptyViewTab_Unable_to_create + "\n" + getRenderingName() + "\n\n" + DebugUIMessages.ErrorRendering_0 + fException.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$


		return fTextViewer.getControl();
	}


	/**
	 *
	 */
	private String getRenderingName() {

		if (DebugUITools.getMemoryRenderingManager().getRenderingType(fRenderingId)!= null)
		{
			String name =
				DebugUITools.getMemoryRenderingManager()
				.getRenderingType(fRenderingId)
				.getLabel();

			return name;
		}
		return "Unknown"; //$NON-NLS-1$
	}

	@Override
	public Control getControl() {
		return fTextViewer.getControl();
	}

	public void refresh() {
	}

}
