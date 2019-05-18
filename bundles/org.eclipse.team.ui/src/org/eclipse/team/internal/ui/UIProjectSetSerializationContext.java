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
 *     Dan Rubel - initial API and implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/

package org.eclipse.team.internal.ui;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ui.dialogs.IPromptCondition;
import org.eclipse.team.internal.ui.dialogs.PromptingDialog;

/**
 * The UI based context in which project serialization occurs.
 * The class may be subclasses to represent different UI based serialization contexts.
 * It is recommended that all UI based serialization contexts
 * use this class directly or indirectly as their superclass.
 *
 * @since 3.0
 */
public class UIProjectSetSerializationContext extends ProjectSetSerializationContext {
	/**
	 * The parent shell for this UI context
	 */
	private final Shell shell;

	/**
	 * Constructs a new instance.
	 *
	 * @param shell The parent shell for this UI context
	 * @param filename a filename or {@code null}
	 */
	public UIProjectSetSerializationContext(Shell shell, String filename) {
		super(filename);
		Assert.isNotNull(shell);
		this.shell = shell;
	}

	/**
	 * Answer the shell associated with this UI context.
	 *
	 * @return the shell (not <code>null</code>)
	 */
	@Override
	public Object getShell() {
		return shell;
	}

	/**
	 * Given an array of projects that currently exist in the workspace
	 * prompt the user to determine which of those projects should be overwritten.
	 * <p>
	 * This default implementation prompts the user
	 * to determine which projects should be overwritten.
	 * Subclasses may override this as appropriate.
	 *
	 * @param projects
	 * 		an array of projects currently existing in the workspace
	 * 		that are desired to be overwritten.
	 * 		(not <code>null</code>, contains no <code>null</code>s)
	 * @return
	 * 		an array of zero or more projects that should be overwritten
	 * 		or <code>null</code> if the operation is to be canceled
	 *
	 * @see org.eclipse.team.core.ProjectSetSerializationContext#confirmOverwrite(org.eclipse.core.resources.IProject[])
	 */
	@Override
	public IProject[] confirmOverwrite(final IProject[] projects) throws TeamException {
		IPromptCondition prompt = new IPromptCondition() {
			List resources = Arrays.asList(projects);
			@Override
			public boolean needsPrompt(IResource resource) {
				if (resource instanceof IProject) {
					IProject project = (IProject) resource;
					return (project.exists() || getTargetFile(project).exists()) && resources.contains(resource);
				}
				return false;
			}
			@Override
			public String promptMessage(IResource resource) {
				if (resource.exists())
					return NLS.bind(TeamUIMessages.UIProjectSetSerializationContext_0, new String[] { resource.getName() });
				return NLS.bind(TeamUIMessages.UIProjectSetSerializationContext_2, new String[] { resource.getName(), getTargetFile((IProject)resource).getAbsolutePath() });
			}
			public File getTargetFile(IProject project) {
				return new File(project.getParent().getLocation().toFile(), project.getName());
			}
		};
		PromptingDialog dialog =
			new PromptingDialog(
				(Shell) getShell(),
				projects,
				prompt,
				TeamUIMessages.UIProjectSetSerializationContext_1);
		IResource[] resourcesToOverwrite;
		try {
			resourcesToOverwrite = dialog.promptForMultiple();
		} catch (InterruptedException e) {
			// Return null indicating that the user canceled the operation
			return null;
		}
		IProject[] projectsToOverwrite = new IProject[resourcesToOverwrite.length];
		System.arraycopy(resourcesToOverwrite, 0, projectsToOverwrite, 0, resourcesToOverwrite.length);
		return projectsToOverwrite;
	}

}
