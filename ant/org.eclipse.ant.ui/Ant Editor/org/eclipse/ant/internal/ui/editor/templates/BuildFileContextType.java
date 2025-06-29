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

import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;

/**
 * A very simple context type.
 */
public class BuildFileContextType extends TemplateContextType {

	/** This context's id */
	public static final String BUILDFILE_CONTEXT_TYPE = "org.eclipse.ant.ui.templateContextType.buildFile"; //$NON-NLS-1$

	/**
	 * Creates a new XML context type.
	 */
	public BuildFileContextType() {
		addGlobalResolvers();
	}

	private void addGlobalResolvers() {
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.WordSelection());
		addResolver(new GlobalTemplateVariables.LineSelection());
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
	}
}
