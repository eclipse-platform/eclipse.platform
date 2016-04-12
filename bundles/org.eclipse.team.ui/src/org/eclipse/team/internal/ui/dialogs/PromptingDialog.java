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
package org.eclipse.team.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;

/**
 * A confirmation dialog helper that will either show a 'yes/no/yes to all/cancel'
 * dialog to confirm an action performed on several resources or if only one
 * resource is specified 'ok/cancel' will be shown.
 */
public class PromptingDialog extends MultipleYesNoPrompter {
	private IPromptCondition condition;
	private IResource[] resources;
	/**
	 * Prompt for the given resources using the specific condition. The prompt dialog will
	 * have the title specified.
	 * @param shell
	 * @param resources
	 * @param condition
	 * @param title
	 */
	public PromptingDialog(Shell shell, IResource[] resources, IPromptCondition condition, String title) {
		this(shell, resources, condition, title, false /* all or nothing */);
	}

	public PromptingDialog(final Shell shell, IResource[] resources, IPromptCondition condition, String title, boolean allOrNothing) {
		super(new IShellProvider() {
			@Override
			public Shell getShell() {
				return shell;
			}
		}, title, resources.length > 1, allOrNothing);
		this.resources = resources;
		this.condition = condition;
	}
	/**
	 * Call to calculate and show prompt. If no resources satisfy the prompt
	 * condition a dialog won't be shown. The resources for which the user
	 * confirmed the action are returned.
	 * @return the resources
	 *
	 * @throws InterruptedException
	 *             if the user choose to cancel on the prompt dialog
	 */
	public IResource[] promptForMultiple() throws InterruptedException {
		List targetResources = new ArrayList();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (!condition.needsPrompt(resource) || shouldInclude(condition.promptMessage(resource))) {
				targetResources.add(resource);
			}
		}
		return (IResource[]) targetResources.toArray(new IResource[targetResources.size()]);
	}
}
