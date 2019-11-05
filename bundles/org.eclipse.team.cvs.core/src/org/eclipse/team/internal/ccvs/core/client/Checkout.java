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
package org.eclipse.team.internal.ccvs.core.client;

 
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.listeners.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.RemoteModule;

public class Checkout extends Command {
	/*** Local options: specific to checkout ***/
	public static final LocalOption DO_NOT_SHORTEN = new LocalOption("-N"); //$NON-NLS-1$
	public static final LocalOption FETCH_MODULE_ALIASES = new LocalOption("-c"); //$NON-NLS-1$
	public static LocalOption makeDirectoryNameOption(String moduleName) {
		return new LocalOption("-d", moduleName); //$NON-NLS-1$
	}

	/*** Default command output listener ***/
	private static final ICommandOutputListener DEFAULT_OUTPUT_LISTENER = new UpdateListener(null);
	
	/** Command options found in the CVSROOT/modules file */
	public static LocalOption ALIAS = new LocalOption("-a"); //$NON-NLS-1$
	public static LocalOption makeStatusOption(String status) {
		return new LocalOption("-s", status); //$NON-NLS-1$
	}
	
	protected Checkout() { }	
	protected String getRequestId() {
		return "co"; //$NON-NLS-1$
	}
	
	protected ICommandOutputListener getDefaultCommandOutputListener() {
		return DEFAULT_OUTPUT_LISTENER;
	}
	
	protected ICVSResource[] computeWorkResources(Session session, LocalOption[] localOptions,
		String[] arguments) throws CVSException {
			
		// We shouldn't have any arguments if we're fetching the module definitions
		if (arguments.length < 1 && ! FETCH_MODULE_ALIASES.isElementOf(localOptions)) throw new IllegalArgumentException();
		
		// We can determine the local directories using either the -d option or the module expansions
		Option dOption = findOption(localOptions, "-d");  //$NON-NLS-1$
		if (dOption != null) {
			// Should we append the expansions to the -d argument?
			return new ICVSResource[] {session.getLocalRoot().getFolder(dOption.argument)};
		}
		String[] modules = session.getModuleExpansions();
		ICVSResource[] resources = new ICVSResource[modules.length];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = session.getLocalRoot().getFolder(modules[i]);
		}
		return resources;
	}
	
	/**
	 * Start the Checkout command:
	 *    Send the module that is going to be checked-out to the server 
	 *    by reading the name of the resource given
	 *    (This has to change to we give it the name of the modul and the
	 *    Checkout creates everything for us)
	 */
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			
		
		// We need a folder to put the project(s) we checkout into
		Assert.isTrue(session.getLocalRoot().isFolder());
		
		// Send the information about the local workspace resources to the server
		List<ICVSResource> resourcesToSend = new ArrayList<>(resources.length);
		for (ICVSResource resource : resources) {
			if (resource.exists() && resource.isFolder() && ((ICVSFolder)resource).isCVSFolder()) {
				resourcesToSend.add(resource);
			}
		}
		if ( ! resourcesToSend.isEmpty()) {
			resources = resourcesToSend.toArray(new ICVSResource[resourcesToSend.size()]);
			new FileStructureVisitor(session, localOptions, true, true).visit(session, resources, monitor);
		} else {
			monitor.beginTask(null, 100);
			monitor.done();
		}
		return resources;
	}

	protected void sendLocalWorkingDirectory(Session session) throws CVSException {
		session.sendConstructedRootDirectory();
	}

	/**
	 * On sucessful finish, prune empty directories if 
	 * the -P option was specified (or is implied by -D or -r)
	 */
	protected IStatus commandFinished(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor,
		IStatus status) throws CVSException {
		// If we didn't succeed, don't do any post processing
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			return status;
		}
	
		// If we are retrieving the modules file, ignore other options
		if (FETCH_MODULE_ALIASES.isElementOf(localOptions)) return status;

		// If we are pruning (-P) or getting a sticky copy (-D or -r), then prune empty directories
		if (PRUNE_EMPTY_DIRECTORIES.isElementOf(localOptions) ||
			(findOption(localOptions, "-D") != null) || //$NON-NLS-1$
			(findOption(localOptions, "-r") != null)) { //$NON-NLS-1$			

			// Prune empty directories
			new PruneFolderVisitor().visit(session, resources);
		}
		
		return status;
	}
	
	/**
	 * Override execute to perform a expand-modules before the checkout
	 */
	protected IStatus doExecute(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, String[] arguments, ICommandOutputListener listener,
		IProgressMonitor monitor) throws CVSException {
		monitor.beginTask(null, 100);

		if ( ! FETCH_MODULE_ALIASES.isElementOf(localOptions)) {
			// Execute the expand-modules command. 
			// This will put the expansions in the session for later retrieval
			IStatus status = Request.EXPAND_MODULES.execute(session, arguments, Policy.subMonitorFor(monitor, 10));
			if (status.getCode() == CVSStatus.SERVER_ERROR)
				return status;
			
			// If -d is not included in the local options, then send -N (do not shorten directory paths)
			// to the server (as is done by other cvs clients)
			if (findOption(localOptions, "-d") == null) { //$NON-NLS-1$
				if ( ! DO_NOT_SHORTEN.isElementOf(localOptions)) {
					LocalOption[] newLocalOptions = new LocalOption[localOptions.length + 1];
					newLocalOptions[0] = DO_NOT_SHORTEN;
					System.arraycopy(localOptions, 0, newLocalOptions, 1, localOptions.length);
					localOptions = newLocalOptions;
				}
			}
		}
		
		return super.doExecute(session, globalOptions, localOptions, arguments, listener, Policy.subMonitorFor(monitor, 90));
	}
	
	/**
	 * Perform a checkout to get the module expansions defined in the CVSROOT/modules file
	 */
	public RemoteModule[] getRemoteModules(Session session, CVSTag tag, IProgressMonitor monitor)
		throws CVSException {
		
		ModuleDefinitionsListener moduleDefinitionListener = new ModuleDefinitionsListener();
		
		IStatus status = super.execute(session, NO_GLOBAL_OPTIONS, new LocalOption[] {FETCH_MODULE_ALIASES}, NO_ARGUMENTS, 
			moduleDefinitionListener, monitor);
			
		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			throw new CVSServerException(status);
		}
		
		return RemoteModule.createRemoteModules(moduleDefinitionListener.getModuleExpansions(), session.getCVSRepositoryLocation(), tag);
	}
	
	protected String getDisplayText() {
		return "checkout"; //$NON-NLS-1$
	}
}
