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
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.core.resources.IResource;

/**
 * Input to a confirm prompt
 *
 * @see PromptingDialog
 */
public interface IPromptCondition {
	/**
	 * Answers <code>true</code> if a prompt is required for this resource and
	 * false otherwise.
	 */
	public boolean needsPrompt(IResource resource);

	/**
	 * Answers the message to include in the prompt.
	 */
	public String promptMessage(IResource resource);
}
