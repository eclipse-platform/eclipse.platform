/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor.templates;

import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.source.ISourceViewer;

public class AntTemplateViewerConfiguration extends AntSourceViewerConfiguration {

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new AntTemplateVariableTextHover();
	}
}
