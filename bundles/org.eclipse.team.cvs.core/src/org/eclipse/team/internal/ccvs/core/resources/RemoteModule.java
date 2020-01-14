/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;

public class RemoteModule extends RemoteFolder {
	
	private String label;
	private ICVSRemoteResource[] referencedModules;
	private LocalOption[] localOptions;
	private boolean expandable;
	
	public static RemoteModule[] getRemoteModules(ICVSRepositoryLocation repository, CVSTag tag, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(CVSMessages.RemoteModule_getRemoteModules, 100); 
		try {		
			RemoteModule[] modules;
			Session s = new Session(repository, getRemoteRootFolder(repository), false);
			s.open(Policy.subMonitorFor(monitor, 10), false /* read-only */);
			try {
				modules = Command.CHECKOUT.getRemoteModules(s, tag, Policy.subMonitorFor(monitor, 90));
			} finally {
				s.close();
			}
			return modules;
		} finally {
			monitor.done();
		}
	}
	
	private static ICVSFolder getRemoteRootFolder(ICVSRepositoryLocation repository) {
		return new RemoteFolder(null, repository, "/", null); //$NON-NLS-1$
	}

	/**
	 * Create a set of RemoteModules from the provided module definition strings returned from the server
	 * 
	 * At the moment, we are very restrictive on the types of modules we support.
	 */
	public static RemoteModule[] createRemoteModules(String[] moduleDefinitionStrings, ICVSRepositoryLocation repository, CVSTag tag) {
		
		Map<String, RemoteModule> modules = new HashMap<>();
		Map<String, String[]> referencedModulesTable = new HashMap<>();
		Map<String,String[]> moduleAliases = new HashMap<>();
		
		// First pass: Create the remote module instances based on remote mapping
		for (String moduleDefinitionString : moduleDefinitionStrings) {
			// Read the module name
			StringTokenizer tokenizer = new StringTokenizer(moduleDefinitionString);
			String moduleName = tokenizer.nextToken();
			List<LocalOption> localOptionsList;
			String next;
			try {
				// Read the options associated with the module
				localOptionsList = new ArrayList<>();
				next = tokenizer.nextToken();
				while (next.charAt(0) == '-') {
					switch (next.charAt(1)) {
						case 'a': // alias
							localOptionsList.add(Checkout.ALIAS);
							break;
						case 'l': // don't recurse
							localOptionsList.add(Command.DO_NOT_RECURSE);
							break;
						case 'd': // directory
							localOptionsList.add(Checkout.makeDirectoryNameOption(tokenizer.nextToken()));
							break;
						case 'e':
						case 'i':
						case 'o':
						case 't':
						case 'u': // Ignore any programs
							tokenizer.nextToken();
							break;
						case 's': // status
							localOptionsList.add(Checkout.makeStatusOption(tokenizer.nextToken()));
							break;
						default: // unanticipated option. Ignore it and go on
					}
					next = tokenizer.nextToken();
				}
			} catch (NoSuchElementException e) {
				// There is an invalid entry in the modules file. Log it and continue
				CVSProviderPlugin.log(IStatus.WARNING, NLS.bind(CVSMessages.RemoteModule_invalidDefinition, new String[]{moduleDefinitionString, repository.getLocation(true)}), null); 
				continue;
			}
			LocalOption[] localOptions = localOptionsList.toArray(new LocalOption[localOptionsList.size()]);
			
			if (Checkout.ALIAS.isElementOf(localOptions)) {
				
				if (localOptions.length > 1) {
					// XXX This is an error condition that needs to be reported
				}
				
				// An alias expands to one or more modules or paths
				List<String> expansions = new ArrayList<>(10);
				expansions.add(next);
				while (tokenizer.hasMoreTokens())
					expansions.add(tokenizer.nextToken());
					
				moduleAliases.put(moduleName, expansions.toArray(new String[expansions.size()]));
				modules.put(moduleName, new RemoteModule(moduleName, null, repository, null, localOptions, tag, true));

			} else {
				
				// The module definition may have a leading directory which can be followed by some files
				if (!(next.charAt(0) == '&')) {
					String directory = next;
					List<String> files = new ArrayList<>();
					while (tokenizer.hasMoreTokens() && (next.charAt(0) != '&')) {
						next = tokenizer.nextToken() ;
						if ((next.charAt(0) != '&'))
							files.add(next);
					}
					RemoteModule remoteModule = new RemoteModule(moduleName, null, repository, directory, localOptions, tag, ! files.isEmpty());
					modules.put(moduleName, remoteModule);
					if ( ! files.isEmpty()) {
						ICVSRemoteResource[] children = new ICVSRemoteResource[files.size()];
						for (int j = 0; j < children.length; j++) {
							children[j] = new RemoteFile(remoteModule, Update.STATE_NONE, files.get(j), null, null, tag);
							remoteModule.setChildren(children);
						}
					}
				} else {
					modules.put(moduleName, new RemoteModule(moduleName, null, repository, null, localOptions, tag, true));
				}
				
				// Record any referenced modules so that can be cross-referenced below
				if (next.charAt(0) == '&') {
					List<String> children = new ArrayList<>(10);
					children.add(next);
					while (tokenizer.hasMoreTokens())
						children.add(tokenizer.nextToken());
					referencedModulesTable.put(moduleName, children.toArray(new String[children.size()]));
				}
			}
		}
		
		// Second pass: Cross reference aliases to modules
		// XXX Aliases can reference other aliases which confuses the expansion!
		Iterator iter = moduleAliases.keySet().iterator();
		while (iter.hasNext()) {
			String moduleName = (String)iter.next();
			RemoteModule module = modules.get(moduleName);
			String[] expansion = moduleAliases.get(moduleName);
			List<ICVSRemoteResource> referencedFolders = new ArrayList<>();
			boolean expandable = true;
			for (String e : expansion) {
				if (e.charAt(0) == '!') {
					// XXX Unsupported for now
					expandable = false;
				} else {
					IPath path = new Path(null, e);
					if (path.segmentCount() > 1) {
						// XXX Unsupported for now
						expandable = false;
					} else {
						RemoteModule child = modules.get(e);
						if (child == null) {
							referencedFolders.add(new RemoteFolder(null, repository, path.toString(), tag));
						} else {
							// Need to check if the child is a module alias
							if (child.isAlias()) {
								// XXX Unsupported for now
								expandable = false;
							} else {
								referencedFolders.add(child);
							}
						}
					}
				}
			}
			if (expandable) {
				//TODO: Make module static??
				module.setChildren(referencedFolders.toArray(new ICVSRemoteResource[referencedFolders.size()]));
			} else {
				module.setExpandable(false);
			}
		}
		
		// Third pass: Cross reference remote modules where necessary
		iter = modules.keySet().iterator();
		while (iter.hasNext()) {
			String moduleName = (String)iter.next();
			String[] children = referencedModulesTable.get(moduleName);
			if (children != null) {
				RemoteModule module = modules.get(moduleName);
				List<RemoteModule> referencedFolders = new ArrayList<>();
				boolean expandable = true;
				for (String c : children) {
					RemoteModule child = modules.get(c.substring(1));
					if (child == null) {
						// invalid module definition
						expandable = false;
					} else if (child.isAlias()) {
						// Include alias children in-line
						expandable = false;
//						referencedFolders.addAll(Arrays.asList(child.getChildren()));
					} else {
						// XXX not expandable if child has local directory option (-d)
						if (Command.findOption(child.getLocalOptions(), "-d") != null) { //$NON-NLS-1$
							expandable = false;
						} else {
							referencedFolders.add(child);
						}
					}
				}
				if (expandable) {
					module.setReferencedModules(referencedFolders.toArray(new ICVSRemoteResource[referencedFolders.size()]));
				} else {
					module.setExpandable(false);
				}
			}
		}
						
		return modules.values().toArray(new RemoteModule[modules.size()]);
	}
		
	public RemoteModule(String label, RemoteFolder parent, ICVSRepositoryLocation repository, String repositoryRelativePath, LocalOption[] localOptions, CVSTag tag, boolean isStatic) {
		super(parent, 
			label, 
			repository, 
			repositoryRelativePath == null ? FolderSyncInfo.VIRTUAL_DIRECTORY : repositoryRelativePath, 
			tag, 
			isStatic);
		this.localOptions = localOptions;
		this.label = label;
		this.expandable = true;
	}
	
	public LocalOption[] getLocalOptions() {
		return localOptions;
	}
	/* 
	 * Override of inherited getMembers in order to combine the physical members with any referenced modules
	 */
	public ICVSRemoteResource[] getMembers(CVSTag tagName, IProgressMonitor monitor) throws CVSException {
		
		if ( ! expandable) return new ICVSRemoteResource[0];
		
		ICVSRemoteResource[] physicalChildren;
		if ( folderInfo.getIsStatic()) {
			physicalChildren = getChildren();
		} else {
			physicalChildren = super.getMembers(tagName, monitor);
		}
		ICVSRemoteResource[] allChildren;
		if (referencedModules != null && referencedModules.length > 0) {
			if (physicalChildren == null) {
				allChildren = referencedModules;
			} else {
				// Combine two sets of children
				allChildren = new ICVSRemoteResource[physicalChildren.length + referencedModules.length];
				System.arraycopy(physicalChildren, 0, allChildren, 0, physicalChildren.length);
				System.arraycopy(referencedModules, 0, allChildren, physicalChildren.length, referencedModules.length);
			}
		} else if (physicalChildren != null) {
			allChildren = physicalChildren;
		} else {
			allChildren = new ICVSRemoteResource[0];
		}
		return allChildren;
	}
	
	private void setReferencedModules(ICVSRemoteResource[] referencedModules) {
		this.referencedModules = referencedModules;
	}
	
	public boolean isAlias() {
		return Checkout.ALIAS.isElementOf(localOptions);
	}
	
	public boolean isExpandable() {
		return expandable;
	}
	
	private void setExpandable(boolean expandable) {
		this.expandable = expandable;
	}
	
	public ICVSRemoteResource forTag(ICVSRemoteFolder parent, CVSTag tagName) {
		RemoteModule r = new RemoteModule(label, (RemoteFolder)parent, getRepository(), folderInfo.getRepository(), localOptions, tagName, folderInfo.getIsStatic());
		r.setExpandable(expandable);
		if (folderInfo.getIsStatic()) {
			ICVSRemoteResource[] children = getChildren();
			if (children != null) {
				List<ICVSRemoteResource> taggedChildren = new ArrayList<>(children.length);
				for (ICVSRemoteResource resource : children) {
					taggedChildren.add(((RemoteResource)resource).forTag(r, tagName));
				}
				r.setChildren(taggedChildren.toArray(new ICVSRemoteResource[taggedChildren.size()]));
			}
		}
		if (referencedModules != null) {
			List<ICVSRemoteResource> taggedModules = new ArrayList<>(referencedModules.length);
			for (ICVSRemoteResource referencedModule : referencedModules) {
				RemoteModule module = (RemoteModule) referencedModule;
				taggedModules.add(module.forTag(r, tagName));
			}
			r.setReferencedModules(taggedModules.toArray(new ICVSRemoteResource[taggedModules.size()]));
		}
		return r;
	}
	
	public boolean isDefinedModule() {
		return true;
	}
	public boolean equals(Object arg0) {
		if (arg0 instanceof RemoteModule) {
			RemoteModule module = (RemoteModule) arg0;
			return (getName().equals(module.getName()) && super.equals(module));
		}
		return false;
	}
	
	public int hashCode() {
		return super.hashCode() | getName().hashCode();
	}

	public ICVSResource getChild(String path) throws CVSException {
		if (path.equals(Session.CURRENT_LOCAL_FOLDER) || path.length() == 0)
			return this;
		// If the path is one segment and it's a referenced module, return the module
		// Note: the overriden method will extract the first segment from a multi segment
		// path and re-invoke this method so we only need to check for one segment here
		// and use the inherited method in the other cases
		if (referencedModules != null) {
			if (!path.contains(Session.SERVER_SEPARATOR)) {
				for (ICVSRemoteResource referencedModule : referencedModules) {
					if (referencedModule.getName().equals(path)) {
						return referencedModule;
					}
				}
			}
		}
		return super.getChild(path);
	}

}
