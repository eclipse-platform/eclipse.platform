/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

public class RepositoryProviderBic extends RepositoryProvider {

	final public static String NATURE_ID = "org.eclipse.team.tests.core.bic-provider";

	private IMoveDeleteHook mdh;
	private IFileModificationValidator mv;

	/*
	 * @see RepositoryProvider#configureProject()
	 */
	public void configureProject() throws CoreException {
	}

	/*
	 * @see RepositoryProvider#getID()
	 */
	public String getID() {
		return NATURE_ID;
	}
	/*
	 * @see IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
	}

	/*
	 * @see RepositoryProvider#getFileModificationValidator()
	 */
	public IFileModificationValidator getFileModificationValidator() {
		return mv;
	}

	/*
	 * @see RepositoryProvider#getMoveDeleteHook()
	 */
	public IMoveDeleteHook getMoveDeleteHook() {
		return mdh;
	}

	public void setModificationValidator(IFileModificationValidator mv) {
		this.mv = mv;
	}

	public void setMoveDeleteHook(IMoveDeleteHook mdh) {
		this.mdh = mdh;
	}
}
