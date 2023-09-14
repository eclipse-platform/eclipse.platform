/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.model.IProblem;
import org.eclipse.jface.text.source.Annotation;

/**
 * Annotation representing an <code>IProblem</code>.
 */
public class XMLProblemAnnotation extends Annotation {

	public static final String ERROR_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.error"; //$NON-NLS-1$
	public static final String WARNING_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.warning"; //$NON-NLS-1$
	public static final String INFO_ANNOTATION_TYPE = "org.eclipse.ui.workbench.texteditor.info"; //$NON-NLS-1$

	private final IProblem fProblem;

	public XMLProblemAnnotation(IProblem problem) {

		fProblem = problem;

		if (fProblem.isError()) {
			setType(ERROR_ANNOTATION_TYPE);
		} else if (fProblem.isWarning()) {
			setType(WARNING_ANNOTATION_TYPE);
		} else {
			setType(INFO_ANNOTATION_TYPE);
		}

		setText(fProblem.getMessage());
	}
}
