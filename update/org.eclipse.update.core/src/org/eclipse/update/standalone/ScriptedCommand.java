/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.standalone;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.operations.*;

/**
 * Parent class for all the update manager standalone commands.
 * Subclasses will provide specific operations and the implementation of the run() method.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public abstract class ScriptedCommand implements IOperationListener {

	private IInstallConfiguration config;
	protected boolean verifyOnly;

	/**
	 * Constructor
	 *
	 */
	public ScriptedCommand() {
		this(null);
	}

	/**
	 * Constructor.
	 * 
	 * @param verifyOnly if true, the command is not executed, but will only attempt to run the command. 
	 * This is mostly used when wanted to know if the command would fail.
	 */
	public ScriptedCommand(String verifyOnly) {
		this.verifyOnly = "true".equals(verifyOnly); //$NON-NLS-1$
	}

	/**
	 * @return  true if the command should only be run in simulation mode,
	 * to verify if it can execute.
	 */
	protected final boolean isVerifyOnly() {
		return verifyOnly;
	}

	/**
	 * Convenience method that executes the command with a null progress monitor.
	 */
	public final boolean run() {
		return run(new NullProgressMonitor());
	}
	
	/**
	 * Executes the command. Subclasses are responsible for implementing this method.
	 * If the command was constructed with verifyOnly=true, the command should not execute, but only verify it can execute.
	 * @param monitor progress monitor during command execution.
	 */
	public abstract boolean run(IProgressMonitor monitor);

	/**
	 * Applies the changes made to the current configuration.
	 */
	public void applyChangesNow() {
		OperationsManager.applyChangesNow();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#afterExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean afterExecute(IOperation operation, Object data) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.update.operations.IOperationListener#beforeExecute(org.eclipse.update.operations.IOperation)
	 */
	public boolean beforeExecute(IOperation operation, Object data) {
		return true;
	}

	/**
	 * @return the installation configuration affected by the command
	 */
	public final IInstallConfiguration getConfiguration() {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			config = localSite.getCurrentConfiguration();
		} catch (CoreException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
		}
		return config;
	}

}
