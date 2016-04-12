/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.ui.synchronize.ModelOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A model operation that builds the scope of the operation's
 * manager. Clients who want to build the scope can do the following:
 * <p>
 *
 * @see ISynchronizationScopeManager
 * @since 3.2
 */
public class BuildScopeOperation extends ModelOperation {

	public BuildScopeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part, manager);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.operations.ModelOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void execute(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// Do nothing since we only want to build the scope
	}

}
