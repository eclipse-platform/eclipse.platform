/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.templates;

import java.io.IOException;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;


public class AntTemplateAccess {
	/** Key to store custom templates. */
	private static final String CUSTOM_TEMPLATES_KEY= "org.eclipse.ant.ui.customtemplates"; //$NON-NLS-1$
	
	/** The shared instance. */
	private static AntTemplateAccess fgInstance;
	
	/** The template store. */
	private TemplateStore fStore;
	
	/** The context type registry. */
	private ContributionContextTypeRegistry fRegistry;
	
	private AntTemplateAccess() {
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static AntTemplateAccess getDefault() {
		if (fgInstance == null) {
			fgInstance= new AntTemplateAccess();
		}
		return fgInstance;
	}

	/**
	 * Returns this plug-in's template store.
	 * 
	 * @return the template store of this plug-in instance
	 */
	public TemplateStore getTemplateStore() {
		if (fStore == null) {
			fStore= new ContributionTemplateStore(getContextTypeRegistry(),AntUIPlugin.getDefault().getPreferenceStore(), CUSTOM_TEMPLATES_KEY);
			try {
				fStore.load();
			} catch (IOException e) {
				AntUIPlugin.log(e);
			}
		}
		return fStore;
	}

	/**
	 * Returns this plug-in's context type registry.
	 * 
	 * @return the context type registry for this plug-in instance
	 */
	public ContextTypeRegistry getContextTypeRegistry() {
		if (fRegistry == null) {
			// create and configure the contexts available in the template editor
			fRegistry= new ContributionContextTypeRegistry();
			fRegistry.addContextType(BuildFileContextType.BUILDFILE_CONTEXT_TYPE);
			fRegistry.addContextType(TargetContextType.TARGET_CONTEXT_TYPE);
			fRegistry.addContextType(TaskContextType.TASK_CONTEXT_TYPE);
		}
		return fRegistry;
	}
}
