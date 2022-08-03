/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.PessimisticResourceRuleFactory;
import org.eclipse.team.internal.core.RepositoryProviderManager;
import org.eclipse.team.internal.core.TeamHookDispatcher;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A concrete subclass of <code>RepositoryProvider</code> is created for each
 * project that is associated with a repository provider. The lifecycle of these
 * instances is is similar to that of the platform's 'nature' mechanism.
 * <p>
 * To create a repository provider and have it registered with the platform, a
 * client must minimally:
 * </p>
 * <ol>
 * <li>extend <code>RepositoryProvider</code>
 * <li>define a repository extension in <code>plugin.xml</code>. Here is an
 * example extension point definition:
 *
 * <code>
 *	<br>&lt;extension point="org.eclipse.team.core.repository"&gt;
 *  <br>&nbsp;&lt;repository
 *  <br>&nbsp;&nbsp;class="org.eclipse.myprovider.MyRepositoryProvider"
 *  <br>&nbsp;&nbsp;id="org.eclipse.myprovider.myProviderID"&gt;
 *  <br>&nbsp;&lt;/repository&gt;
 *	<br>&lt;/extension&gt;
 *  </code>
 * </ol>
 * <p>
 * Once a repository provider is registered with Team, then you can associate a
 * repository provider with a project by invoking
 * <code>RepositoryProvider.map()</code>.
 * </p>
 *
 * @see RepositoryProvider#map(IProject, String)
 *
 * @since 2.0
 */
public abstract class RepositoryProvider implements IProjectNature, IAdaptable {

	private final static String TEAM_SETID = "org.eclipse.team.repository-provider"; //$NON-NLS-1$

	private final static List<String> AllProviderTypeIds = initializeAllProviderTypes();

	// the project instance that this nature is assigned to
	private IProject project;

	// lock to ensure that map/unmap and getProvider support concurrency
	private static final ILock mappingLock = Job.getJobManager().newLock();

	// Session property used to identify projects that are not mapped
	private static final Object NOT_MAPPED = new Object();

	/**
	 * Instantiate a new RepositoryProvider with concrete class by given providerID
	 * and associate it with project.
	 *
	 * @param project the project to be mapped
	 * @param id the ID of the provider to be mapped to the project
	 * @throws TeamException if
	 * <ul>
	 * <li>There is no provider by that ID.</li>
	 * <li>The project is already associated with a repository provider and that provider
	 * prevented its unmapping.</li>
	 * </ul>
	 * @see RepositoryProvider#unmap(IProject)
	 */
	public static void map(IProject project, String id) throws TeamException {
		ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(project);
		try {
			// Obtain a scheduling rule on the project before obtaining the
			// mappingLock. This is required because a caller of getProvider
			// may hold a scheduling rule before getProvider is invoked but
			// getProvider itself does not (and can not) obtain a scheduling rule.
			// Thus, the locking order is always scheduling rule followed by
			// mappingLock.
			Job.getJobManager().beginRule(rule, null);
			try {
				mappingLock.acquire();
				RepositoryProvider existingProvider = null;

				if(project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY) != null)
					existingProvider = getProvider(project);	// get the real one, not the nature one

				//if we already have a provider, and its the same ID, we're ok
				//if the ID's differ, unmap the existing.
				if(existingProvider != null) {
					if(existingProvider.getID().equals(id))
						return;	//nothing to do
					else
						unmap(project);
				}

				// Create the provider as a session property before adding the persistent
				// property to ensure that the provider can be instantiated
				RepositoryProvider provider = mapNewProvider(project, id);

				//mark it with the persistent ID for filtering
				try {
					project.setPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY, id);
				} catch (CoreException outer) {
					// couldn't set the persistent property so clear the session property
					try {
						project.setSessionProperty(TeamPlugin.PROVIDER_PROP_KEY, null);
					} catch (CoreException inner) {
						// something is seriously wrong
						TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.RepositoryProvider_couldNotClearAfterError, new String[] { project.getName(), id }), inner);
					}
					throw outer;
				}

				provider.configure();

				//adding the nature would've caused project description delta, so trigger one
				project.touch(null);

				// Set the rule factory for the provider after the touch
				// so the touch does not fail due to incompatible modify rules
				TeamHookDispatcher.setProviderRuleFactory(project, provider.getRuleFactory());

				// Notify any listeners
				RepositoryProviderManager.getInstance().providerMapped(provider);
			} finally {
				mappingLock.release();
			}
		} catch (CoreException e) {
			throw TeamPlugin.wrapException(e);
		} finally {
			Job.getJobManager().endRule(rule);
		}
	}

	/*
	 * Instantiate the provider denoted by ID and store it in the session property.
	 * Return the new provider instance. If a TeamException is thrown, it is
	 * guaranteed that the session property will not be set.
	 *
	 * @param project
	 * @param id
	 * @return RepositoryProvider
	 * @throws TeamException we can't instantiate the provider, or if the set
	 * session property fails from core
	 */
	private static RepositoryProvider mapNewProvider(final IProject project, final String id) throws TeamException {
		final RepositoryProvider provider = newProvider(id); 	// instantiate via extension point

		if(provider == null)
			throw new TeamException(NLS.bind(Messages.RepositoryProvider_couldNotInstantiateProvider, new String[] { project.getName(), id }));

		// validate that either the provider supports linked resources or the project has no linked resources
		if (!provider.canHandleLinkedResourceURI()) {
			try {
				project.accept(proxy -> {
					if (proxy.isLinked()) {
						if (!provider.canHandleLinkedResources() ||
								proxy.requestFullPath().segmentCount() > 2 ||
								!EFS.SCHEME_FILE.equals(proxy.requestResource().getLocationURI().getScheme()))
							throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.LINKING_NOT_ALLOWED, NLS.bind(Messages.RepositoryProvider_linkedURIsExist, new String[] { project.getName(), id }), null));
					}
					return true;
				}, IResource.NONE);
			} catch (CoreException e) {
				if (e instanceof TeamException) {
					TeamException te = (TeamException) e;
					throw te;
				}
				throw new TeamException(e);
			}
		}
		if (!provider.canHandleLinkedResources()) {
			try {
				IResource[] members = project.members();
				for (IResource resource : members) {
					if (resource.isLinked()) {
						throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.LINKING_NOT_ALLOWED, NLS.bind(Messages.RepositoryProvider_linkedResourcesExist, new String[] { project.getName(), id }), null));
					}
				}
			} catch (CoreException e) {
				throw TeamPlugin.wrapException(e);
			}
		}

		//store provider instance as session property
		try {
			project.setSessionProperty(TeamPlugin.PROVIDER_PROP_KEY, provider);
			provider.setProject(project);
		} catch (CoreException e) {
			throw TeamPlugin.wrapException(e);
		}
		return provider;
	}

	private static RepositoryProvider mapExistingProvider(IProject project, String id) throws TeamException {
		try {
			// Obtain the mapping lock before creating the instance so we can make sure
			// that a disconnect is not happening at the same time
			mappingLock.acquire();
			try {
				// Ensure that the persistent property is still set
				// (i.e. an unmap may have come in since we checked it last
				String currentId = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);
				if (currentId == null) {
					// The provider has been unmapped
					return null;
				}
				if (!currentId.equals(id)) {
					// A provider has been disconnected and another connected
					// Since mapping creates the session property, we
					// can just return it
					return lookupProviderProp(project);
				}
			} catch (CoreException e) {
				throw TeamPlugin.wrapException(e);
			}
			return mapNewProvider(project, id);
		} finally {
			mappingLock.release();
		}
	}
	/**
	 * Disassociates project with the repository provider its currently mapped to.
	 * @param project project to unmap
	 * @throws TeamException The project isn't associated with any repository provider.
	 */
	public static void unmap(IProject project) throws TeamException {
		ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory().modifyRule(project);
		try{
			// See the map(IProject, String) method for a description of lock ordering
			Job.getJobManager().beginRule(rule, null);
			try {
				mappingLock.acquire();
				String id = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);

				//If you tried to remove a non-existant nature it would fail, so we need to as well with the persistent prop
				if(id == null) {
					throw new TeamException(NLS.bind(Messages.RepositoryProvider_No_Provider_Registered, new String[] { project.getName() }));
				}

				//This will instantiate one if it didn't already exist,
				//which is ok since we need to call deconfigure() on it for proper lifecycle
				RepositoryProvider provider = getProvider(project);
				if (provider == null) {
					// There is a persistent property but the provider cannot be obtained.
					// The reason could be that the provider's plugin is no longer available.
					// Better log it just in case this is unexpected.
					TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.RepositoryProvider_couldNotInstantiateProvider, new String[] { project.getName(), id }), null);
				}

				if (provider != null) provider.deconfigure();

				project.setSessionProperty(TeamPlugin.PROVIDER_PROP_KEY, null);
				project.setPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY, null);

				if (provider != null) provider.deconfigured();

				//removing the nature would've caused project description delta, so trigger one
				project.touch(null);

				// Change the rule factory after the touch in order to
				// avoid rule incompatibility
				TeamHookDispatcher.setProviderRuleFactory(project, null);

				// Notify any listeners
				RepositoryProviderManager.getInstance().providerUnmapped(project);
			} finally {
				mappingLock.release();
			}
		} catch (CoreException e) {
			throw TeamPlugin.wrapException(e);
		} finally {
			Job.getJobManager().endRule(rule);
		}
	}

	/*
	 * Return the provider mapped to project, or null if none;
	 */
	private static RepositoryProvider lookupProviderProp(IProject project) throws CoreException {
		Object provider = project.getSessionProperty(TeamPlugin.PROVIDER_PROP_KEY);
		if (provider instanceof RepositoryProvider) {
			return (RepositoryProvider) provider;
		}
		return null;
	}


	/**
	 * Default constructor required for the resources plugin to instantiate this class from
	 * the nature extension definition.
	 */
	public RepositoryProvider() {
	}

	/**
	 * Configures the provider for the given project. This method is called after <code>setProject</code>.
	 * If an exception is generated during configuration
	 * of the project, the provider will not be assigned to the project.
	 *
	 * @throws CoreException if the configuration fails.
	 */
	abstract public void configureProject() throws CoreException;

	/**
	 * Configures the nature for the given project. This is called by <code>RepositoryProvider.map()</code>
	 * the first time a provider is mapped to a project. It is not intended to be called by clients.
	 *
	 * @throws CoreException if this method fails. If the configuration fails the provider will not be
	 * associated with the project.
	 *
	 * @see RepositoryProvider#configureProject()
	 */
	@Override
	final public void configure() throws CoreException {
		try {
			configureProject();
		} catch(CoreException e) {
			try {
				RepositoryProvider.unmap(getProject());
			} catch(TeamException e2) {
				throw new CoreException(new Status(IStatus.ERROR, TeamPlugin.ID, 0, Messages.RepositoryProvider_Error_removing_nature_from_project___1 + getID(), e2));
			}
			throw e;
		}
	}

	/**
	 * Method deconfigured is invoked after a provider has been unmaped. The
	 * project will no longer have the provider associated with it when this
	 * method is invoked. It is a last chance for the provider to clean up.
	 */
	protected void deconfigured() {
	}

	/**
	 * Answer the id of this provider instance. The id should be the repository provider's
	 * id as defined in the provider plugin's plugin.xml.
	 *
	 * @return the nature id of this provider
	 */
	abstract public String getID();

	/**
	 * Returns an <code>IFileModificationValidator</code> for pre-checking operations
	 * that modify the contents of files.
	 * Returns <code>null</code> if the provider does not wish to participate in
	 * file modification validation.
	 * @return an <code>IFileModificationValidator</code> for pre-checking operations
	 * that modify the contents of files
	 *
	 * @see org.eclipse.core.resources.IFileModificationValidator
	 * @deprecated use {@link #getFileModificationValidator2()}
	 */
	@Deprecated
	public IFileModificationValidator getFileModificationValidator() {
		return null;
	}

	/**
	 * Returns a {@link FileModificationValidator} for pre-checking operations
	 * that modify the contents of files. Returns <code>null</code> if the
	 * provider does not wish to participate in file modification validation. By
	 * default, this method wraps the old validator returned from
	 * {@link #getFileModificationValidator()}. Subclasses that which to remain
	 * backwards compatible while providing this new API should override
	 * {@link #getFileModificationValidator2()} to return a subclass of
	 * {@link FileModificationValidator} and should return the same
	 * validator from {@link #getFileModificationValidator()}.
	 * <p>
	 * This method is not intended to be called by clients. Clients should
	 * use the {@link IWorkspace#validateEdit(IFile[], Object)} method instead.
	 *
	 * @return an <code>FileModificationValidator</code> for pre-checking
	 *         operations that modify the contents of files
	 *
	 * @see FileModificationValidator
	 * @see IWorkspace#validateEdit(IFile[], Object)
	 * @since 3.3
	 */
	public FileModificationValidator getFileModificationValidator2() {
		final IFileModificationValidator fileModificationValidator = getFileModificationValidator();
		if (fileModificationValidator == null)
			return null;
		return new FileModificationValidator() {
			@Override
			public IStatus validateSave(IFile file) {
				return fileModificationValidator.validateSave(file);
			}
			@Override
			public IStatus validateEdit(IFile[] files,
					FileModificationValidationContext context) {
				// Extract the shell from the context in order to invoke the old API
				Object shell;
				if (context == null)
					shell = null;
				else
					shell = context.getShell();
				return fileModificationValidator.validateEdit(files, shell);
			}
		};
	}

	/**
	 * Returns an <code>IFileHistoryProvider</code> which can be used to access
	 * file histories. By default, returns <code>null</code>. Subclasses may override.
	 * @return an <code>IFileHistoryProvider</code> which can be used to access
	 * file histories.
	 * @since 3.2
	 */
	public IFileHistoryProvider getFileHistoryProvider(){
		return null;
	}

	/**
	 * Returns an <code>IMoveDeleteHook</code> for handling moves and deletes
	 * that occur within projects managed by the provider. This allows providers
	 * to control how moves and deletes occur and includes the ability to prevent them.
	 * <p>
	 * Returning <code>null</code> signals that the default move and delete behavior is desired.
	 * @return an <code>IMoveDeleteHook</code> for handling moves and deletes
	 * that occur within projects managed by the provider
	 *
	 * @see org.eclipse.core.resources.team.IMoveDeleteHook
	 */
	public IMoveDeleteHook getMoveDeleteHook() {
		return null;
	}

	/**
	 * Returns a brief description of this provider. The exact details of the
	 * representation are unspecified and subject to change, but the following
	 * may be regarded as typical:
	 *
	 * "SampleProject:org.eclipse.team.cvs.provider"
	 *
	 * @return a string description of this provider
	 */
	@Override
	public String toString() {
		return NLS.bind(Messages.RepositoryProvider_toString, new String[] { getProject().getName(), getID() });
	}

	/**
	 * Returns all known (registered) RepositoryProvider ids.
	 *
	 * @return an array of registered repository provider ids.
	 */
	final public static String[] getAllProviderTypeIds() {
		IProjectNatureDescriptor[] desc = ResourcesPlugin.getWorkspace().getNatureDescriptors();
		Set<String> teamSet = new HashSet<>();

		teamSet.addAll(AllProviderTypeIds);	// add in all the ones we know via extension point
		//fall back to old method of nature ID to find any for backwards compatibility
		for (IProjectNatureDescriptor d : desc) {
			String[] setIds = d.getNatureSetIds();
			for (String setId : setIds) {
				if (setId.equals(TEAM_SETID)) {
					teamSet.add(d.getNatureId());
				}
			}
		}
		return teamSet.toArray(new String[teamSet.size()]);
	}

	/**
	 * Returns the provider for a given IProject or <code>null</code> if a provider is not associated with
	 * the project or if the project is closed or does not exist. This method should be called if the caller
	 * is looking for <b>any</b> repository provider. Otherwise call <code>getProvider(project, id)</code>
	 * to look for a specific repository provider type.
	 * @param project the project to query for a provider
	 * @return the repository provider associated with the project
	 */
	final public static RepositoryProvider getProvider(IProject project) {
		try {
			if (project.isAccessible()) {

				//-----------------------------
				//First, look for the session property
				RepositoryProvider provider = lookupProviderProp(project);
				if(provider != null)
					return provider;
				// Do a quick check to see it the project is known to be unshared.
				// This is done to avoid accessing the persistent property store
				if (isMarkedAsUnshared(project))
					return null;

				// -----------------------------
				//Next, check if it has the ID as a persistent property, if yes then instantiate provider
				String id = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);
				if(id != null)
					return mapExistingProvider(project, id);

				//Couldn't find using new method, fall back to lookup using natures for backwards compatibility
				//-----------------------------
				IProjectDescription projectDesc = project.getDescription();
				String[] natureIds = projectDesc.getNatureIds();
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				// for every nature id on this project, find it's natures sets and check if it is
				// in the team set.
				for (String natureId : natureIds) {
					IProjectNatureDescriptor desc = workspace.getNatureDescriptor(natureId);
					// The descriptor can be null if the nature doesn't exist
					if (desc != null) {
						String[] setIds = desc.getNatureSetIds();
						for (String setId : setIds) {
							if (setId.equals(TEAM_SETID)) {
								return getProvider(project, natureId);
							}
						}
					}
				}
				markAsUnshared(project);
			}
		} catch(CoreException e) {
			if (!isAcceptableException(e)) {
				TeamPlugin.log(e);
			}
			markAsUnshared(project);
		}
		return null;
	}

	/*
	 * Return whether the given exception is acceptable during a getProvider().
	 * If the exception is acceptable, it is assumed that there is no provider
	 * on the project.
	 */
	private static boolean isAcceptableException(CoreException e) {
		return e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND;
	}

	/**
	 * Returns a provider of type with the given id if associated with the given project
	 * or <code>null</code> if the project is not associated with a provider of that type
	 * or the nature id is that of a non-team repository provider nature.
	 *
	 * @param project the project to query for a provider
	 * @param id the repository provider id
	 * @return the repository provider
	 */
	final public static RepositoryProvider getProvider(IProject project, String id) {
		try {
			if (project.isAccessible()) {
				// Look for an existing provider first to avoid accessing persistent properties
				RepositoryProvider provider = lookupProviderProp(project);  //throws core, we will reuse the catching already here
				if(provider != null) {
					if (provider.getID().equals(id)) {
						return provider;
					} else {
						return null;
					}
				}
				// Do a quick check to see it the project is known to be unshared.
				// This is done to avoid accessing the persistent property store
				if (isMarkedAsUnshared(project))
					return null;

				// There isn't one so check the persistent property
				String existingID = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY);
				if(id.equals(existingID)) {
					// The ids are equal so instantiate and return
					RepositoryProvider newProvider = mapExistingProvider(project, id);
					if (newProvider!= null && newProvider.getID().equals(id)) {
						return newProvider;
					} else {
						// The id changed before we could create the desired provider
						return null;
					}
				}

				//couldn't find using new method, fall back to lookup using natures for backwards compatibility
				//-----------------------------

				// if the nature id given is not in the team set then return
				// null.
				IProjectNatureDescriptor desc = ResourcesPlugin.getWorkspace().getNatureDescriptor(id);
				if(desc == null) //for backwards compatibility, may not have any nature by that ID
					return null;

				String[] setIds = desc.getNatureSetIds();
				for (String setId : setIds) {
					if (setId.equals(TEAM_SETID)) {
						return (RepositoryProvider)project.getNature(id);
					}
				}
				markAsUnshared(project);
			}
		} catch(CoreException e) {
			if (!isAcceptableException(e)) {
				TeamPlugin.log(e);
			}
			markAsUnshared(project);
		}
		return null;
	}

	/**
	 * Returns whether the given project is shared or not. This is a lightweight
	 * method in that it will not instantiate a provider instance (as
	 * <code>getProvider</code> would) if one is not already instantiated.
	 *
	 * Note that IProject.touch() generates a project description delta.  This, in combination
	 * with isShared() can be used to be notified of sharing/unsharing of projects.
	 *
	 * @param project the project being tested.
	 * @return boolean
	 *
	 * @see #getProvider(IProject)
	 *
	 * @since 2.1
	 */
	public static boolean isShared(IProject project) {
		if (!project.isAccessible()) return false;
		try {
			if (lookupProviderProp(project) != null) return true;
			// Do a quick check to see it the project is known to be unshared.
			// This is done to avoid accessing the persistent property store
			if (isMarkedAsUnshared(project))
				return false;
			boolean shared = project.getPersistentProperty(TeamPlugin.PROVIDER_PROP_KEY) != null;
			if (!shared)
				markAsUnshared(project);
			return shared;
		} catch (CoreException e) {
			TeamPlugin.log(e);
			return false;
		}
	}

	private static boolean isMarkedAsUnshared(IProject project) {
		try {
			return project.getSessionProperty(TeamPlugin.PROVIDER_PROP_KEY) == NOT_MAPPED;
		} catch (CoreException e) {
			return false;
		}
	}

	private static void markAsUnshared(IProject project) {
		try {
			project.setSessionProperty(TeamPlugin.PROVIDER_PROP_KEY, NOT_MAPPED);
		} catch (CoreException e) {
			// Just ignore the error as this is just an optimization
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	private static List<String> initializeAllProviderTypes() {
		List<String> allIDs = new ArrayList<>();

		TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin != null) {
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.REPOSITORY_EXTENSION);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for (IExtension e : extensions) {
					IConfigurationElement[] configElements = e.getConfigurationElements();
					for (IConfigurationElement configElement : configElements) {
						String extensionId = configElement.getAttribute("id"); //$NON-NLS-1$
						allIDs.add(extensionId);
					}
				}
			}
		}
		return allIDs;
	}

	private static RepositoryProvider newProvider(String id) {
		TeamPlugin plugin = TeamPlugin.getPlugin();
		if (plugin != null) {
			IExtensionPoint extension = Platform.getExtensionRegistry().getExtensionPoint(TeamPlugin.ID, TeamPlugin.REPOSITORY_EXTENSION);
			if (extension != null) {
				IExtension[] extensions =  extension.getExtensions();
				for (IExtension ext : extensions) {
					IConfigurationElement[] configElements = ext.getConfigurationElements();
					for (IConfigurationElement configElement : configElements) {
						String extensionId = configElement.getAttribute("id"); //$NON-NLS-1$
						if (extensionId != null && extensionId.equals(id)) {
							try {
								return (RepositoryProvider) configElement.createExecutableExtension("class"); //$NON-NLS-1$
							} catch (CoreException e) {
								TeamPlugin.log(e);
							} catch (ClassCastException e) {
								String className = configElement.getAttribute("class"); //$NON-NLS-1$
								TeamPlugin.log(IStatus.ERROR, NLS.bind(Messages.RepositoryProvider_invalidClass, new String[] { id, className }), e);
							}
							return null;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Method validateCreateLink is invoked by the Platform Core TeamHook when a
	 * linked resource is about to be added to the provider's project. It should
	 * not be called by other clients and it should not need to be overridden by
	 * subclasses (although it is possible to do so in special cases).
	 * Subclasses can indicate that they support linked resources by overriding
	 * the <code>canHandleLinkedResources()</code> method.
	 *
	 * @param resource see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @param updateFlags see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @param location see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @return IStatus see <code>org.eclipse.core.resources.team.TeamHook</code>
	 *
	 * @see RepositoryProvider#canHandleLinkedResources()
	 *
	 * @deprecated see {@link #validateCreateLink(IResource, int, URI) } instead
	 * @since 2.1
	 */
	@Deprecated
	public IStatus validateCreateLink(IResource resource, int updateFlags, IPath location) {
		if (canHandleLinkedResources()) {
			return Team.OK_STATUS;
		} else {
			return new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.LINKING_NOT_ALLOWED, NLS.bind(Messages.RepositoryProvider_linkedResourcesNotSupported, new String[] { getProject().getName(), getID() }), null);
		}
	}

	/**
	 * Method validateCreateLink is invoked by the Platform Core TeamHook when a
	 * linked resource is about to be added to the provider's project. It should
	 * not be called by other clients and it should not need to be overridden by
	 * subclasses (although it is possible to do so in special cases).
	 * Subclasses can indicate that they support linked resources by overriding
	 * the <code>canHandleLinkedResourcesAtArbitraryDepth()</code> method.
	 *
	 * @param resource see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @param updateFlags see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @param location see <code>org.eclipse.core.resources.team.TeamHook</code>
	 * @return IStatus see <code>org.eclipse.core.resources.team.TeamHook</code>
	 *
	 * @see RepositoryProvider#canHandleLinkedResourceURI()
	 *
	 * @since 3.2
	 */
	public IStatus validateCreateLink(IResource resource, int updateFlags, URI location) {
		if (resource.getProjectRelativePath().segmentCount() == 1 && EFS.SCHEME_FILE.equals(location.getScheme())) {
			// This is compatible with the old style link so invoke the old
			// validateLink
			return validateCreateLink(resource, updateFlags, URIUtil.toPath(location));
		}
		if (canHandleLinkedResourceURI()) {
			return Team.OK_STATUS;
		} else {
			return new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.LINKING_NOT_ALLOWED, NLS.bind(Messages.RepositoryProvider_linkedURIsNotSupported, new String[] { getProject().getName(), getID() }), null);
		}
	}

	/**
	 * Method canHandleLinkedResources should be overridden by subclasses who
	 * support linked resources. At a minimum, supporting linked resources
	 * requires changes to the move/delete hook
	 * {@link org.eclipse.core.resources.team.IMoveDeleteHook}. This method is
	 * called after the RepositoryProvider is instantiated but before
	 * <code>setProject()</code> is invoked so it will not have access to any
	 * state determined from the <code>setProject()</code> method.
	 * @return boolean
	 *
	 * @see org.eclipse.core.resources.team.IMoveDeleteHook
	 *
	 * @since 2.1
	 *
	 * @deprecated see {@link #canHandleLinkedResourceURI() }
	 */
	@Deprecated
	public boolean canHandleLinkedResources() {
		return canHandleLinkedResourceURI();
	}

	/**
	 * Return whether this repository provider can handle linked resources that
	 * are located via a URI (i.e. may not be on the local file system) or occur
	 * at an arbitrary depth in the project. This should be overridden by
	 * subclasses who support linked resources at arbitrary depth and/or in
	 * non-local file systems. This is not enabled by default since linked
	 * resources previously only occurred at the root of a project but now can
	 * occur anywhere within a project. This method is called after the
	 * RepositoryProvider is instantiated but before <code>setProject()</code>
	 * is invoked so it will not have access to any state determined from the
	 * <code>setProject()</code> method.
	 *
	 * @return whether this repository provider can handle linked resources that
	 *         are located via a URI or occur at an arbitrary depth in the
	 *         project
	 *
	 * @see #validateCreateLink(IResource, int, URI)
	 *
	 * @since 3.2
	 */
	public boolean canHandleLinkedResourceURI() {
		return false;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/**
	 * Return the resource rule factory for this provider. This factory
	 * will be used to determine the scheduling rules that are to be obtained
	 * when performing various resource operations (e.g. move, copy, delete, etc.)
	 * on the resources in the project the provider is mapped to.
	 * <p>
	 * By default, the factory returned by this method is pessimistic and
	 * obtains the workspace lock for all operations that could result in a
	 * callback to the provider (either through the <code>IMoveDeleteHook</code>
	 * or <code>IFileModificationValidator</code>). This is done to ensure that
	 * older providers are not broken. However, providers should override this
	 * method and provide a subclass of {@link org.eclipse.core.resources.team.ResourceRuleFactory}
	 * that provides rules of a more optimistic granularity (e.g. project
	 * or lower).
	 * @return the rule factory for this provider
	 * @since 3.0
	 * @see org.eclipse.core.resources.team.ResourceRuleFactory
	 */
	public IResourceRuleFactory getRuleFactory() {
		return new PessimisticResourceRuleFactory();
	}

	/**
	 * Return a {@link Subscriber} that describes the synchronization state
	 * of the resources contained in the project associated with this
	 * provider. The subscriber is obtained from the {@link RepositoryProviderType}
	 * associated with a provider and is thus shared for all providers of the
	 * same type.
	 * @return a subscriber that provides resource synchronization state or <code>null</code>
	 * @since 3.2
	 */
	public final Subscriber getSubscriber() {
		RepositoryProviderType type = RepositoryProviderType.getProviderType(getID());
		if (type != null)
			return type.getSubscriber();
		return null;
	}
}
