/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Dan Rubel <dan_rubel@instantiations.com> - Implementation of getLocalTimeStamp
 *     Red Hat Incorporated - get/setResourceAttribute code
 *     Oakland Software Incorporated - added getSessionProperties and getPersistentProperties
 *     Holger Oehm <holger.oehm@sap.com> - [226264] race condition in Workspace.isTreeLocked()/setTreeLocked()
 *     Martin Oberhuber (Wind River) -  [245937] ProjectDescription#setLinkLocation() detects non-change
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add resource filtering
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Sergey Prigogin (Google) - [338010] Resource.createLink() does not preserve symbolic links
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.events.LifecycleEvent;
import org.eclipse.core.internal.localstore.FileSystemResourceManager;
import org.eclipse.core.internal.properties.IPropertyManager;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.WrappedRuntimeException;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.osgi.util.NLS;

public abstract class Resource extends PlatformObject implements IResource, ICoreConstants, Cloneable, IPathRequestor {
	final IPath path;
	final Workspace workspace;

	protected Resource(IPath path, Workspace workspace) {
		this.path = path.removeTrailingSeparator();
		this.workspace = workspace;
	}

	@Override
	public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
		accept(visitor, IResource.DEPTH_INFINITE, memberFlags);
	}

	@Override
	public void accept(final IResourceProxyVisitor visitor, final int depth, final int memberFlags) throws CoreException {
		// It is invalid to call accept on a phantom when INCLUDE_PHANTOMS is not specified.
		final boolean includePhantoms = (memberFlags & IContainer.INCLUDE_PHANTOMS) != 0;
		if ((memberFlags & IContainer.DO_NOT_CHECK_EXISTENCE) == 0)
			checkAccessible(getFlags(getResourceInfo(includePhantoms, false)));

		final ResourceProxy proxy = new ResourceProxy();
		IElementContentVisitor elementVisitor = (tree, requestor, contents) -> {
			ResourceInfo info = (ResourceInfo) contents;
			if (!isMember(getFlags(info), memberFlags))
				return false;
			proxy.requestor = requestor;
			proxy.info = info;
			try {
				boolean shouldContinue = true;
				switch (depth) {
					case DEPTH_ZERO :
						shouldContinue = false;
						break;
					case DEPTH_ONE :
						shouldContinue = !path.equals(requestor.requestPath().removeLastSegments(1));
						break;
					case DEPTH_INFINITE :
						shouldContinue = true;
						break;
				}
				return visitor.visit(proxy) && shouldContinue;
			} catch (CoreException e) {
				// Throw an exception to bail out of the traversal.
				throw new WrappedRuntimeException(e);
			} finally {
				proxy.reset();
			}
		};
		try {
			new ElementTreeIterator(workspace.getElementTree(), getFullPath()).iterate(elementVisitor);
		} catch (WrappedRuntimeException e) {
			throw (CoreException) e.getTargetException();
		} finally {
			proxy.requestor = null;
			proxy.info = null;
		}
	}

	@Override
	public void accept(IResourceVisitor visitor) throws CoreException {
		accept(visitor, IResource.DEPTH_INFINITE, 0);
	}

	@Override
	public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
		accept(visitor, depth, includePhantoms ? IContainer.INCLUDE_PHANTOMS : 0);
	}

	@Override
	public void accept(final IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
		// Use the fast visitor if visiting to infinite depth.
		if (depth == IResource.DEPTH_INFINITE) {
			accept(proxy -> visitor.visit(proxy.requestResource()), memberFlags);
			return;
		}
		// It is invalid to call accept on a phantom when INCLUDE_PHANTOMS is not specified.
		final boolean includePhantoms = (memberFlags & IContainer.INCLUDE_PHANTOMS) != 0;
		ResourceInfo info = getResourceInfo(includePhantoms, false);
		int flags = getFlags(info);
		if ((memberFlags & IContainer.DO_NOT_CHECK_EXISTENCE) == 0)
			checkAccessible(flags);

		// Check that this resource matches the member flags
		// Visit this resource.
		if (!isMember(flags, memberFlags) || !visitor.visit(this) || depth == DEPTH_ZERO)
			return;
		// Get the info again because it might have been changed by the visitor.
		info = getResourceInfo(includePhantoms, false);
		if (info == null)
			return;
		// Thread safety: (cache the type to avoid changes -- we might not be inside an operation).
		int type = info.getType();
		if (type == FILE)
			return;
		// If we had a gender change we need to fix up the resource before asking for its members.
		IContainer resource = getType() != type ? (IContainer) workspace.newResource(getFullPath(), type) : (IContainer) this;
		IResource[] members = resource.members(memberFlags);
		for (IResource member : members)
			member.accept(visitor, DEPTH_ZERO, memberFlags | IContainer.DO_NOT_CHECK_EXISTENCE);
	}

	protected void assertCopyRequirements(IPath destination, int destinationType, int updateFlags) throws CoreException {
		IStatus status = checkCopyRequirements(destination, destinationType, updateFlags);
		if (!status.isOK()) {
			// This assert is ok because the error cases generated by the check method above
			// indicate assertion conditions.
			Assert.isTrue(false, status.getChildren()[0].getMessage());
		}
	}

	/**
	 * Throws an exception if the link preconditions are not met.  Returns the file info
	 * for the file being linked to, or <code>null</code> if not available.
	 */
	protected IFileInfo assertLinkRequirements(URI localLocation, int updateFlags) throws CoreException {
		boolean allowMissingLocal = (updateFlags & IResource.ALLOW_MISSING_LOCAL) != 0;
		if ((updateFlags & IResource.REPLACE) == 0)
			checkDoesNotExist(getFlags(getResourceInfo(false, false)), true);
		IStatus locationStatus = workspace.validateLinkLocationURI(this, localLocation);
		// We only tolerate an undefined path variable in the allow missing local case.
		final boolean variableUndefined = locationStatus.getCode() == IResourceStatus.VARIABLE_NOT_DEFINED_WARNING;
		if (locationStatus.getSeverity() == IStatus.ERROR || (variableUndefined && !allowMissingLocal))
			throw new ResourceException(locationStatus);
		// Check that the parent exists and is open.
		Container parent = (Container) getParent();
		parent.checkAccessible(getFlags(parent.getResourceInfo(false, false)));
		// If the variable is undefined we can't do any further checks.
		if (variableUndefined)
			return null;
		// Check if the file exists.
		URI resolved = getPathVariableManager().resolveURI(localLocation);
		IFileStore store = EFS.getStore(resolved);
		IFileInfo fileInfo = store.fetchInfo();
		boolean localExists = fileInfo.exists();
		if (!allowMissingLocal && !localExists) {
			String msg = NLS.bind(Messages.links_localDoesNotExist, store.toString());
			throw new ResourceException(IResourceStatus.NOT_FOUND_LOCAL, getFullPath(), msg, null);
		}
		// Resource type and file system type must match.
		if (localExists && ((getType() == IResource.FOLDER) != fileInfo.isDirectory())) {
			String msg = NLS.bind(Messages.links_wrongLocalType, getFullPath());
			throw new ResourceException(IResourceStatus.WRONG_TYPE_LOCAL, getFullPath(), msg, null);
		}
		return fileInfo;
	}

	protected void assertMoveRequirements(IPath destination, int destinationType, int updateFlags) throws CoreException {
		IStatus status = checkMoveRequirements(destination, destinationType, updateFlags);
		if (!status.isOK()) {
			// This assert is ok because the error cases generated by the check method
			// above indicate assertion conditions.
			Assert.isTrue(false, status.getChildren()[0].getMessage());
		}
	}

	public void checkAccessible(int flags) throws CoreException {
		checkExists(flags, true);
	}

	private ResourceInfo checkAccessibleAndLocal(int depth) throws CoreException {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		checkAccessible(flags);
		checkLocal(flags, depth);
		return info;
	}

	/**
	 * This method reports errors in two different ways. It can throw a
	 * CoreException or return a status. CoreExceptions are used according to the
	 * specification of the copy method. Programming errors, that would usually be
	 * prevented by using an "Assert" code, are reported as an IStatus. We're doing
	 * this way because we have two different methods to copy resources:
	 * IResource#copy and IWorkspace#copy. The first one gets the error and throws
	 * its message in an AssertionFailureException. The second one just throws a
	 * CoreException using the status returned by this method.
	 *
	 * @see IResource#copy(IPath, int, IProgressMonitor)
	 */
	public IStatus checkCopyRequirements(IPath destination, int destinationType, int updateFlags) throws CoreException {
		String message = Messages.resources_copyNotMet;
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INVALID_VALUE, message, null);
		if (destination == null) {
			message = Messages.resources_destNotNull;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), message);
		}
		destination = makePathAbsolute(destination);
		if (getFullPath().isPrefixOf(destination)) {
			message = NLS.bind(Messages.resources_copyDestNotSub, getFullPath());
			status.add(new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), message));
		}
		checkValidPath(destination, destinationType, false);

		ResourceInfo info;
		checkAccessibleAndLocal(DEPTH_INFINITE);

		IPath destinationParent = destination.removeLastSegments(1);
		checkValidGroupContainer(destinationParent, isLinked(), isVirtual());

		Resource dest = workspace.newResource(destination, destinationType);
		dest.checkDoesNotExist();

		// Ensure we aren't trying to copy a file to a project.
		if (getType() == IResource.FILE && destinationType == IResource.PROJECT) {
			message = Messages.resources_fileToProj;
			throw new ResourceException(IResourceStatus.INVALID_VALUE, getFullPath(), message, null);
		}

		// We can't copy into a closed project.
		if (destinationType != IResource.PROJECT) {
			Project project = (Project) dest.getProject();
			info = project.getResourceInfo(false, false);
			project.checkAccessible(getFlags(info));
			Container parent = (Container) dest.getParent();
			if (!parent.equals(project)) {
				info = parent.getResourceInfo(false, false);
				parent.checkExists(getFlags(info), true);
			}
		}
		if (isUnderLink() || dest.isUnderLink()) {
			// Make sure location is not null.  This can occur with linked resources relative to
			// undefined path variables
			URI sourceLocation = getLocationURI();
			if (sourceLocation == null) {
				message = NLS.bind(Messages.localstore_locationUndefined, getFullPath());
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, getFullPath(), message, null);
			}
			URI destLocation = dest.getLocationURI();
			if (destLocation == null && !dest.isUnderVirtual()) {
				message = NLS.bind(Messages.localstore_locationUndefined, dest.getFullPath());
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, dest.getFullPath(), message, null);
			}
			// Make sure location of source is not a prefix of the location of the destination
			// this can occur if the source and/or destination is a linked resource
			if (getStore().isParentOf(dest.getStore())) {
				message = NLS.bind(Messages.resources_copyDestNotSub, getFullPath());
				throw new ResourceException(IResourceStatus.INVALID_VALUE, getFullPath(), message, null);
			}
		}

		return status.isOK() ? Status.OK_STATUS : (IStatus) status;
	}

	/**
	 * Checks that this resource does not exist.  If the file system is not case
	 * sensitive, this method also checks for a case variant.
	 */
	protected void checkDoesNotExist() throws CoreException {
		checkDoesNotExist(getFlags(getResourceInfo(false, false)), false);
	}

	/**
	 * Checks that this resource does not exist.  If the file system is not case
	 * sensitive, this method also checks for a case variant.
	 *
	 * @exception CoreException if this resource exists
	 */
	public void checkDoesNotExist(int flags, boolean checkType) throws CoreException {
		// If this exact resource exists we are done.
		if (exists(flags, checkType)) {
			String message = NLS.bind(Messages.resources_mustNotExist, getFullPath());
			throw new ResourceException(checkType ? IResourceStatus.RESOURCE_EXISTS : IResourceStatus.PATH_OCCUPIED, getFullPath(), message, null);
		}
		if (Workspace.caseSensitive)
			return;
		// Now look for a matching case variant in the tree.
		IResource variant = findExistingResourceVariant(getFullPath());
		if (variant == null)
			return;
		String msg = NLS.bind(Messages.resources_existsDifferentCase, variant.getFullPath());
		throw new ResourceException(IResourceStatus.CASE_VARIANT_EXISTS, variant.getFullPath(), msg, null);
	}

	/**
	 * Checks that this resource exists.
	 * If checkType is true, the type of this resource and the one in the tree must match.
	 *
	 * @exception CoreException if this resource does not exist
	 */
	public void checkExists(int flags, boolean checkType) throws CoreException {
		if (!exists(flags, checkType)) {
			String message = NLS.bind(Messages.resources_mustExist, getFullPath());
			throw new ResourceException(IResourceStatus.RESOURCE_NOT_FOUND, getFullPath(), message, null);
		}
	}

	/**
	 * Checks that this resource is local to the given depth.
	 *
	 * @exception CoreException if this resource is not local
	 */
	public void checkLocal(int flags, int depth) throws CoreException {
		if (!isLocal(flags, depth)) {
			String message = NLS.bind(Messages.resources_mustBeLocal, getFullPath());
			throw new ResourceException(IResourceStatus.RESOURCE_NOT_LOCAL, getFullPath(), message, null);
		}
	}

	/**
	 * This method reports errors in two different ways. It can throw a
	 * CoreException or log a status. CoreExceptions are used according
	 * to the specification of the move method. Programming errors, that
	 * would usually be prevented by using an "Assert" code, are reported as
	 * an IStatus.
	 * We're doing this way because we have two different methods to move
	 * resources: IResource#move and IWorkspace#move. The first one gets
	 * the error and throws its message in an AssertionFailureException. The
	 * second one just throws a CoreException using the status returned
	 * by this method.
	 *
	 * @see IResource#move(IPath, int, IProgressMonitor)
	 */
	protected IStatus checkMoveRequirements(IPath destination, int destinationType, int updateFlags) throws CoreException {
		String message = Messages.resources_moveNotMet;
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.INVALID_VALUE, message, null);
		if (destination == null) {
			message = Messages.resources_destNotNull;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), message);
		}
		destination = makePathAbsolute(destination);
		if (getFullPath().isPrefixOf(destination)) {
			message = NLS.bind(Messages.resources_moveDestNotSub, getFullPath());
			status.add(new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), message));
		}
		checkValidPath(destination, destinationType, false);

		ResourceInfo info;
		checkAccessibleAndLocal(DEPTH_INFINITE);

		IPath destinationParent = destination.removeLastSegments(1);
		checkValidGroupContainer(destinationParent, isLinked(), isVirtual());

		Resource dest = workspace.newResource(destination, destinationType);

		// Check if we are only changing case.
		IResource variant = Workspace.caseSensitive ? null : findExistingResourceVariant(destination);
		if (variant == null || !this.equals(variant))
			dest.checkDoesNotExist();

		// Ensure we aren't trying to move a file to a project.
		if (getType() == IResource.FILE && dest.getType() == IResource.PROJECT) {
			message = Messages.resources_fileToProj;
			throw new ResourceException(new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), message));
		}

		// We can't move into a closed project.
		if (destinationType != IResource.PROJECT) {
			Project project = (Project) dest.getProject();
			info = project.getResourceInfo(false, false);
			project.checkAccessible(getFlags(info));
			Container parent = (Container) dest.getParent();
			if (!parent.equals(project)) {
				info = parent.getResourceInfo(false, false);
				parent.checkExists(getFlags(info), true);
			}
		}
		if (isUnderLink() || dest.isUnderLink()) {
			// Make sure location is not null.  This can occur with linked resources relative to
			// undefined path variables.
			URI sourceLocation = getLocationURI();
			if (sourceLocation == null) {
				message = NLS.bind(Messages.localstore_locationUndefined, getFullPath());
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, getFullPath(), message, null);
			}
			URI destLocation = dest.getLocationURI();
			if (destLocation == null && !dest.isUnderVirtual()) {
				message = NLS.bind(Messages.localstore_locationUndefined, dest.getFullPath());
				throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, dest.getFullPath(), message, null);
			}
			// Make sure location of source is not a prefix of the location of the destination
			// this can occur if the source and/or destination is a linked resource.
			if (getStore().isParentOf(dest.getStore())) {
				message = NLS.bind(Messages.resources_moveDestNotSub, getFullPath());
				throw new ResourceException(IResourceStatus.INVALID_VALUE, getFullPath(), message, null);
			}
		}

		return status.isOK() ? Status.OK_STATUS : (IStatus) status;
	}

	/**
	 * Checks that the supplied path is valid according to Workspace.validatePath().
	 *
	 * @exception CoreException if the path is not valid
	 */
	public void checkValidPath(IPath toValidate, int type, boolean lastSegmentOnly) throws CoreException {
		IStatus result = workspace.locationValidator.validatePath(toValidate, type, lastSegmentOnly);
		if (!result.isOK())
			throw new ResourceException(result);
	}

	/**
	 * Checks that the destination is a suitable one given that it could be a group.
	 *
	 * @exception CoreException if the path points to a group
	 */
	public void checkValidGroupContainer(IPath destination, boolean isLink, boolean isGroup) throws CoreException {
		IStatus status = getValidGroupContainer(destination, isLink, isGroup);
		if (!status.isOK())
			throw new ResourceException(status);
	}

	/**
	 * Checks that the destination is a suitable one given that it could be a group.
	 *
	 * @exception CoreException if the path points to a group
	 */
	public void checkValidGroupContainer(Container destination, boolean isLink, boolean isGroup) throws CoreException {
		if (!isLink && !isGroup) {
			String message = Messages.group_invalidParent;
			if (destination.isVirtual())
				throw new ResourceException(new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message));
		}
	}

	public IStatus getValidGroupContainer(IPath destination, boolean isLink, boolean isGroup) {
		if (!isLink && !isGroup) {
			String message = Messages.group_invalidParent;
			ResourceInfo info = workspace.getResourceInfo(destination, false, false);
			if (info != null && info.isSet(M_VIRTUAL))
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}
		return Status.OK_STATUS;
	}

	@Override
	public void clearHistory(IProgressMonitor monitor) {
		getLocalManager().getHistoryStore().remove(getFullPath(), monitor);
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		// Must allow notifications to nest in all resource rules.
		if ((this == rule) || rule.getClass().equals(WorkManager.NotifyRule.class))
			return true;
		if (rule instanceof MultiRule) {
			MultiRule multi = (MultiRule) rule;
			ISchedulingRule[] children = multi.getChildren();
			for (ISchedulingRule c : children) {
				if (!contains(c)) {
					return false;
				}
			}
			return true;
		}
		if (!(rule instanceof IResource))
			return false;
		IResource resource = (IResource) rule;
		if (!workspace.equals(resource.getWorkspace()))
			return false;
		return path.isPrefixOf(resource.getFullPath());
	}

	/**
	 * @throws CoreException in overloads
	 */
	public void convertToPhantom() throws CoreException {
		ResourceInfo info = getResourceInfo(false, true);
		if (info == null || isPhantom(getFlags(info)))
			return;
		info.clearSessionProperties();
		info.set(M_PHANTOM);
		getLocalManager().updateLocalSync(info, I_NULL_SYNC_INFO);
		info.clearModificationStamp();
		// Should already be done by the #deleteResource call but left in
		// just to be safe and for code clarity.
		info.setMarkers(null);
	}

	@Override
	public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		copy(destination, updateFlags, monitor);
	}

	@Override
	public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		String message = NLS.bind(Messages.resources_copying, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		destination = makePathAbsolute(destination);
		checkValidPath(destination, getType(), false);
		Resource destResource = workspace.newResource(destination, getType());
		final ISchedulingRule rule = workspace.getRuleFactory().copyRule(this, destResource);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			// The following assert method throws CoreExceptions as stated in the IResource.copy API
			// and assert for programming errors. See checkCopyRequirements for more information.
			assertCopyRequirements(destination, getType(), updateFlags);
			workspace.beginOperation(true);
			getLocalManager().copy(this, destResource, updateFlags, progress.split(98));
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public void copy(IProjectDescription destDesc, boolean force, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		copy(destDesc, updateFlags, monitor);
	}

	/**
	 * Used when a folder is to be copied to a project.
	 * @see IResource#copy(IProjectDescription, int, IProgressMonitor)
	 */
	@Override
	public void copy(IProjectDescription destDesc, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(destDesc);
		String message = NLS.bind(Messages.resources_copying, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(workspace.getRoot(), split);
			// The following assert method throws CoreExceptions as stated in the IResource.copy API
			// and assert for programming errors. See checkCopyRequirements for more information.
			IPath destPath = IPath.fromOSString(destDesc.getName()).makeAbsolute();
			assertCopyRequirements(destPath, getType(), updateFlags);
			Project destProject = (Project) workspace.getRoot().getProject(destPath.lastSegment());
			workspace.beginOperation(true);

			// Create and open the new project.
			destProject.create(destDesc, progress.split(5));
			destProject.open(progress.split(5));

			// Copy the children.
			IResource[] children = ((IContainer) this).members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
			SubMonitor loopProgress = SubMonitor.convert(progress.split(70), children.length);
			for (IResource element : children) {
				Resource child = (Resource) element;
				child.copy(destPath.append(child.getName()), updateFlags, loopProgress.split(1));
			}

			// Copy over the properties.
			getPropertyManager().copy(this, destProject, DEPTH_ZERO);
			progress.split(18);
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(workspace.getRoot(), true);
		}
	}

	/**
	 * Count the number of resources in the tree from this container to the
	 * specified depth. Include this resource. Include phantoms if
	 * the phantom boolean is true.
	 */
	public int countResources(int depth, boolean phantom) {
		return workspace.countResources(path, depth, phantom);
	}

	/**
	 * @see IFolder#createLink(IPath, int, IProgressMonitor)
	 * @see IFile#createLink(IPath, int, IProgressMonitor)
	 */
	public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(localLocation);
		createLink(URIUtil.toURI(localLocation), updateFlags, monitor);
	}

	/**
	 * @see IFolder#createLink(URI, int, IProgressMonitor)
	 * @see IFile#createLink(URI, int, IProgressMonitor)
	 */
	public void createLink(URI localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(localLocation);
		IResource existing = null;
		if ((updateFlags & REPLACE) != 0) {
			existing = workspace.getRoot().findMember(getFullPath());
			if (existing != null && existing.isLinked()) {
				setLinkLocation(localLocation, updateFlags, monitor);
				return;
			}
		}

		String message = NLS.bind(Messages.links_creating, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		checkValidPath(path, FOLDER, true);
		final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			IFileInfo fileInfo = assertLinkRequirements(localLocation, updateFlags);
			workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_CREATE, this));
			workspace.beginOperation(true);
			// Replace existing resource, if applicable.
			if ((updateFlags & REPLACE) != 0 && existing != null) {
				workspace.deleteResource(existing);
			}
			ResourceInfo info = workspace.createResource(this, false);
			if ((updateFlags & IResource.HIDDEN) != 0)
				info.set(M_HIDDEN);
			info.set(M_LINK);
			LinkDescription linkDescription = new LinkDescription(this, localLocation);
			if (linkDescription.isGroup())
				info.set(M_VIRTUAL);
			getLocalManager().link(this, localLocation, fileInfo);
			progress.split(5);
			// Save the location in the project description.
			Project project = (Project) getProject();
			boolean changed = project.internalGetDescription().setLinkLocation(getProjectRelativePath(), linkDescription);
			if (changed) {
				try {
					project.writeDescription(IResource.NONE);
				} catch (CoreException e) {
					// A problem happened updating the description, so delete the resource from the workspace.
					workspace.deleteResource(this);
					throw e; // Rethrow.
				}
			}
			progress.split(4);

			// Refresh to discover any new resources below this linked location.
			if (getType() != IResource.FILE) {
				// Refresh either in background or foreground.
				if ((updateFlags & IResource.BACKGROUND_REFRESH) != 0) {
					workspace.refreshManager.refresh(this);
					progress.split(90);
				} else {
					refreshLocal(DEPTH_INFINITE, progress.split(90));
				}
			} else {
				progress.split(90);
			}
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public IMarker createMarker(String type) throws CoreException {
		return createMarker(type, Collections.emptyMap());
	}

	@Override
	public IMarker createMarker(String type, Map<String, ? extends Object> attributes) throws CoreException {
		Assert.isNotNull(type);
		final ISchedulingRule rule = workspace.getRuleFactory().markerRule(this);
		try {
			workspace.prepareOperation(rule, null);
			checkAccessible(getFlags(getResourceInfo(false, false)));
			workspace.beginOperation(true);
			long id = workspace.nextMarkerId();
			MarkerManager manager = workspace.getMarkerManager();
			boolean validate = manager.isPersistentType(type);
			MarkerInfo markerInfo = new MarkerInfo(attributes, validate, type, id);
			manager.add(this, markerInfo);
			if (attributes != null && !attributes.isEmpty()) {
				if (manager.isPersistent(markerInfo)) {
					this.getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
				}
			}
			return new Marker(this, markerInfo.getId());
		} finally {
			workspace.endOperation(rule, false);
		}
	}
	@Override
	public IResourceProxy createProxy() {
		ResourceProxy result = new ResourceProxy();
		result.info = getResourceInfo(false, false);
		result.requestor = this;
		result.resource = this;
		return result;
	}

	/**
	 * @see IProject#delete(boolean, boolean, IProgressMonitor)
	 * @see IWorkspaceRoot#delete(boolean, boolean, IProgressMonitor)
	 * N.B. This is not an IResource method!
	 */
	public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		delete(updateFlags, monitor);
	}

	@Override
	public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
		delete(force ? IResource.FORCE : IResource.NONE, monitor);
	}

	@Override
	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		String message = NLS.bind(Messages.resources_deleting, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, 100).checkCanceled();
		progress.subTask(message);
		final ISchedulingRule rule = workspace.getRuleFactory().deleteRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			// If there is no resource then there is nothing to delete so just return.
			if (!exists())
				return;
			workspace.beginOperation(true);
			broadcastPreDeleteEvent();

			// When a project is being deleted, flush the build order in case there is a problem.
			if (this.getType() == IResource.PROJECT)
				workspace.flushBuildOrder();

			final IFileStore originalStore = getStore();
			boolean wasLinked = isLinked();
			message = Messages.resources_deleteProblem;
			MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_DELETE_LOCAL, message, null);
			WorkManager workManager = workspace.getWorkManager();
			ResourceTree tree = new ResourceTree(workspace.getFileSystemManager(), workManager.getLock(), status, updateFlags);
			int depth = 0;
			try {
				depth = workManager.beginUnprotected();
				unprotectedDelete(tree, updateFlags, progress.split(50));
			} finally {
				workManager.endUnprotected(depth);
			}
			if (getType() == ROOT) {
				// Need to clear out the root info.
				workspace.getMarkerManager().removeMarkers(this, IResource.DEPTH_ZERO);
				getPropertyManager().deleteProperties(this, IResource.DEPTH_ZERO);
				getResourceInfo(false, false).clearSessionProperties();
			}
			// Invalidate the tree for further use by clients.
			tree.makeInvalid();
			if (!tree.getStatus().isOK())
				throw new ResourceException(tree.getStatus());
			// Update any aliases of this resource.
			// Note that deletion of a linked resource cannot affect other resources.
			if (!wasLinked)
				workspace.getAliasManager().updateAliases(this, originalStore, IResource.DEPTH_INFINITE, progress.split(48));
			if (getType() == PROJECT) {
				// Make sure the rule factory is cleared on project deletion.
				((Rules) workspace.getRuleFactory()).setRuleFactory((IProject) this, null);
				// Make sure project deletion is remembered.
				workspace.getSaveManager().requestSnapshot();
			}
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		final ISchedulingRule rule = workspace.getRuleFactory().markerRule(this);
		try {
			workspace.prepareOperation(rule, null);
			ResourceInfo info = getResourceInfo(false, false);
			checkAccessible(getFlags(info));

			workspace.beginOperation(true);
			workspace.getMarkerManager().removeMarkers(this, type, includeSubtypes, depth);
		} finally {
			workspace.endOperation(rule, false);
		}
	}

	/**
	 * This method should be called to delete a resource from the tree because it will also
	 * delete its properties and markers.  If a status object is provided, minor exceptions are
	 * added, otherwise they are thrown.  If major exceptions occur, they are always thrown.
	 */
	public void deleteResource(boolean convertToPhantom, MultiStatus status) throws CoreException {
		// Remove markers on this resource and its descendants.
		if (exists())
			getMarkerManager().removeMarkers(this, IResource.DEPTH_INFINITE);
		// If this is a linked resource or contains linked resources,
		// remove their entries from the project description.
		List<Resource> links = findLinks();
		// Pre-delete notification to internal infrastructure
		if (links != null)
			for (Resource resource : links)
				workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_DELETE, resource));

		// Check if we deleted a preferences file.
		ProjectPreferences.deleted(this);

		// Remove all deleted linked resources from the project description.
		if (getType() != IResource.PROJECT && links != null) {
			Project project = (Project) getProject();
			ProjectDescription description = project.internalGetDescription();
			if (description != null) {
				boolean wasChanged = false;
				for (Resource resource : links)
					wasChanged |= description.setLinkLocation(resource.getProjectRelativePath(), null);
				if (wasChanged) {
					project.internalSetDescription(description, true);
					try {
						project.writeDescription(IResource.FORCE);
					} catch (CoreException e) {
						// A problem happened updating the description, update the description in memory.
						project.updateDescription();
						throw e; // Rethrow.
					}
				}
			}
		}

		// If we are synchronizing, do not delete the resource. Convert it
		// into a phantom. Actual deletion will happen when we refresh or push.
		if (convertToPhantom && getType() != PROJECT && synchronizing(getResourceInfo(true, false))) {
			convertToPhantom();
		} else {
			workspace.deleteResource(this);
		}

		List<Resource> filters = findFilters();
		if ((filters != null) && (filters.size() > 0)) {
			// Delete resource filters.
			Project project = (Project) getProject();
			ProjectDescription description = project.internalGetDescription();
			if (description != null) {
				for (Resource resource : filters)
					description.setFilters(resource.getProjectRelativePath(), null);
				project.internalSetDescription(description, true);
				project.writeDescription(IResource.FORCE);
			}
		}

		// Delete properties after the resource is deleted from the tree. See bug 84584.
		CoreException err = null;
		try {
			getPropertyManager().deleteResource(this);
		} catch (CoreException e) {
			if (status != null) {
				status.add(e.getStatus());
			} else {
				err = e;
			}
		}
		if (err != null)
			throw err;
	}

	/**
	 * Returns a list of all linked resources at or below this resource, or null if there are no links.
	 */
	private List<Resource> findLinks() {
		Project project = (Project) getProject();
		ProjectDescription description = project.internalGetDescription();
		HashMap<IPath, LinkDescription> linkMap = description.getLinks();
		if (linkMap == null)
			return null;
		List<Resource> links = null;
		IPath myPath = getProjectRelativePath();
		for (LinkDescription link : linkMap.values()) {
			IPath linkPath = link.getProjectRelativePath();
			if (myPath.isPrefixOf(linkPath)) {
				if (links == null)
					links = new ArrayList<>();
				links.add(workspace.newResource(project.getFullPath().append(linkPath), link.getType()));
			}
		}
		return links;
	}

	/**
	 * Returns a list of all filtered resources at or below this resource, or null if there are no links.
	 */
	private List<Resource> findFilters() {
		Project project = (Project) getProject();
		ProjectDescription description = project.internalGetDescription();
		List<Resource> filters = null;
		if (description != null) {
			HashMap<IPath, LinkedList<FilterDescription>> filterMap = description.getFilters();
			if (filterMap != null) {
				IPath myPath = getProjectRelativePath();
				for (IPath filterPath : filterMap.keySet()) {
					if (myPath.isPrefixOf(filterPath)) {
						if (filters == null)
							filters = new ArrayList<>();
						filters.add(workspace.newResource(project.getFullPath().append(filterPath), IResource.FOLDER));
					}
				}
			}
		}
		return filters;
	}

	@Override
	public boolean equals(Object target) {
		if (this == target)
			return true;
		if (!(target instanceof Resource))
			return false;
		Resource resource = (Resource) target;
		return getType() == resource.getType() && path.equals(resource.path) && workspace.equals(resource.workspace);
	}

	@Override
	public boolean exists() {
		ResourceInfo info = getResourceInfo(false, false);
		return exists(getFlags(info), true);
	}

	public boolean exists(int flags, boolean checkType) {
		return flags != NULL_FLAG && !(checkType && ResourceInfo.getType(flags) != getType());
	}

	/**
	 * Helper method for case insensitive file systems.  Returns
	 * an existing resource whose path differs only in case from
	 * the given path, or null if no such resource exists.
	 */
	public IResource findExistingResourceVariant(IPath target) {
		if (!workspace.tree.includesIgnoreCase(target))
			return null;
		// Ignore phantoms.
		ResourceInfo info = (ResourceInfo) workspace.tree.getElementDataIgnoreCase(target);
		if (info != null && info.isSet(M_PHANTOM))
			return null;
		// Resort to slow lookup to find exact case variant.
		IPath result = IPath.ROOT;
		int segmentCount = target.segmentCount();
		for (int i = 0; i < segmentCount; i++) {
			String[] childNames = workspace.tree.getNamesOfChildren(result);
			String name = findVariant(target.segment(i), childNames);
			if (name == null)
				return null;
			result = result.append(name);
		}
		return workspace.getRoot().findMember(result);
	}

	@Override
	public IMarker findMarker(long id) {
		return workspace.getMarkerManager().findMarker(this, id);
	}

	@Override
	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		ResourceInfo info = getResourceInfo(false, false);
		checkAccessible(getFlags(info));
		// It might happen that from this point the resource is not accessible anymore.
		// But markers have the #exists method that callers can use to check if it is still valid.
		return workspace.getMarkerManager().findMarkers(this, type, includeSubtypes, depth);
	}

	@Override
	public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
		ResourceInfo info = getResourceInfo(false, false);
		checkAccessible(getFlags(info));
		// It might happen that from this point the resource is not accessible anymore.
		// But markers have the #exists method that callers can use to check if it is still valid.
		return workspace.getMarkerManager().findMaxProblemSeverity(this, type, includeSubtypes, depth);
	}

	/**
	 * Searches for a variant of the given target in the list,
	 * that differs only in case. Returns the variant from
	 * the list if one is found, otherwise returns null.
	 */
	private String findVariant(String target, String[] list) {
		for (String element : list) {
			if (target.equalsIgnoreCase(element))
				return element;
		}
		return null;
	}

	protected void fixupAfterMoveSource() throws CoreException {
		ResourceInfo info = getResourceInfo(true, true);
		// If a linked resource is moved, we need to remove the location info from the ".project" file.
		if (isLinked() || isVirtual()) {
			Project project = (Project) getProject();
			if (project.internalGetDescription().setLinkLocation(getProjectRelativePath(), null))
				project.writeDescription(IResource.NONE);
		}

		List<Resource> filters = findFilters();
		if ((filters != null) && (filters.size() > 0)) {
			// Delete resource filters.
			Project project = (Project) getProject();
			ProjectDescription description = project.internalGetDescription();
			for (Resource resource : filters)
				description.setFilters(resource.getProjectRelativePath(), null);
			project.writeDescription(IResource.NONE);
		}

		// Check if we deleted a preferences file.
		ProjectPreferences.deleted(this);

		if (!synchronizing(info)) {
			workspace.deleteResource(this);
			return;
		}
		info.clearSessionProperties();
		info.clear(M_LOCAL_EXISTS);
		info.setLocalSyncInfo(I_NULL_SYNC_INFO);
		info.set(M_PHANTOM);
		info.clearModificationStamp();
		info.setMarkers(null);
	}

	@Override
	public String getFileExtension() {
		String name = getName();
		int index = name.lastIndexOf('.');
		if (index == -1)
			return null;
		if (index == (name.length() - 1))
			return ""; //$NON-NLS-1$
		return name.substring(index + 1);
	}

	public int getFlags(ResourceInfo info) {
		return (info == null) ? NULL_FLAG : info.getFlags();
	}

	@Override
	public IPath getFullPath() {
		return path;
	}

	public FileSystemResourceManager getLocalManager() {
		return workspace.getFileSystemManager();
	}

	@Override
	public long getLocalTimeStamp() {
		ResourceInfo info = getResourceInfo(false, false);
		return (info == null || isVirtual()) ? IResource.NULL_STAMP : info.getLocalSyncInfo();
	}

	@Override
	public IPath getLocation() {
		IProject project = getProject();
		if (project != null && !project.exists())
			return null;
		return getLocalManager().locationFor(this, false);
	}

	@Override
	public URI getLocationURI() {
		IProject project = getProject();
		if (project != null && !project.exists())
			return null;
		return getLocalManager().locationURIFor(this, false);
	}

	@Override
	public IMarker getMarker(long id) {
		return new Marker(this, id);
	}

	protected MarkerManager getMarkerManager() {
		return workspace.getMarkerManager();
	}

	@Override
	public long getModificationStamp() {
		ResourceInfo info = getResourceInfo(false, false);
		return info == null ? IResource.NULL_STAMP : info.getModificationStamp();
	}

	@Override
	public String getName() {
		return path.lastSegment();
	}

	@Override
	public IContainer getParent() {
		int segments = path.segmentCount();
		// Zero and one segments handled by subclasses.
		if (segments < 2)
			Assert.isLegal(false, path.toString());
		if (segments == 2)
			return workspace.getRoot().getProject(path.segment(0));
		return (IFolder) workspace.newResource(path.removeLastSegments(1), IResource.FOLDER);
	}

	@Override
	public String getPersistentProperty(QualifiedName key) throws CoreException {
		checkAccessibleAndLocal(DEPTH_ZERO);
		return getPropertyManager().getProperty(this, key);
	}

	@Override
	public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
		checkAccessibleAndLocal(DEPTH_ZERO);
		return getPropertyManager().getProperties(this);
	}

	@Override
	public IProject getProject() {
		return workspace.getRoot().getProject(path.segment(0));
	}

	@Override
	public IPath getProjectRelativePath() {
		return getFullPath().removeFirstSegments(ICoreConstants.PROJECT_SEGMENT_LENGTH);
	}

	public IPropertyManager getPropertyManager() {
		return workspace.getPropertyManager();
	}

	@Override
	public IPath getRawLocation() {
		if (isLinked())
			return FileUtil.toPath(((Project) getProject()).internalGetDescription().getLinkLocationURI(getProjectRelativePath()));
		return getLocation();
	}

	@Override
	public URI getRawLocationURI() {
		if (isLinked())
			return ((Project) getProject()).internalGetDescription().getLinkLocationURI(getProjectRelativePath());
		return getLocationURI();
	}

	@Override
	public ResourceAttributes getResourceAttributes() {
		if (!isAccessible() || isVirtual())
			return null;
		return getLocalManager().attributes(this);
	}

	/**
	 * Returns the resource info.  Returns null if the resource doesn't exist.
	 * If the phantom flag is true, phantom resources are considered.
	 * If the mutable flag is true, a mutable info is returned.
	 */
	public ResourceInfo getResourceInfo(boolean phantom, boolean mutable) {
		return workspace.getResourceInfo(getFullPath(), phantom, mutable);
	}

	@Override
	public Object getSessionProperty(QualifiedName key) throws CoreException {
		ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);
		return info.getSessionProperty(key);
	}

	@Override
	public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
		ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);
		return info.getSessionProperties();
	}

	public IFileStore getStore() {
		return getLocalManager().getStore(this);
	}

	@Override
	public abstract int getType();

	public String getTypeString() {
		switch (getType()) {
			case FILE :
				return "L"; //$NON-NLS-1$
			case FOLDER :
				return "F"; //$NON-NLS-1$
			case PROJECT :
				return "P"; //$NON-NLS-1$
			case ROOT :
				return "R"; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public IWorkspace getWorkspace() {
		return workspace;
	}

	@Override
	public int hashCode() {
		// The container may be null if the identified resource
		// does not exist so don't bother with it in the hash
		return getFullPath().hashCode();
	}

	/**
	 * Sets the M_LOCAL_EXISTS flag. Is internal so we don't have
	 * to begin an operation.
	 */
	protected void internalSetLocal(boolean flag, int depth) throws CoreException {
		ResourceInfo info = getResourceInfo(true, true);
		// Only make the change if it's not already in desired state.
		if (info.isSet(M_LOCAL_EXISTS) != flag) {
			if (flag && !isPhantom(getFlags(info))) {
				info.set(M_LOCAL_EXISTS);
				workspace.updateModificationStamp(info);
			} else {
				info.clear(M_LOCAL_EXISTS);
				info.clearModificationStamp();
			}
		}
		if (getType() == IResource.FILE || depth == IResource.DEPTH_ZERO)
			return;
		if (depth == IResource.DEPTH_ONE)
			depth = IResource.DEPTH_ZERO;
		IResource[] children = ((IContainer) this).members();
		for (IResource element : children)
			((Resource) element).internalSetLocal(flag, depth);
	}

	@Override
	public boolean isAccessible() {
		return exists();
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		// Must not schedule at same time as notification.
		if ((this == rule) || rule.getClass().equals(WorkManager.NotifyRule.class))
			return true;
		if (rule instanceof MultiRule) {
			MultiRule multi = (MultiRule) rule;
			ISchedulingRule[] children = multi.getChildren();
			for (ISchedulingRule element : children)
				if (isConflicting(element))
					return true;
			return false;
		}
		if (!(rule instanceof IResource))
			return false;
		IResource resource = (IResource) rule;
		if (!workspace.equals(resource.getWorkspace()))
			return false;
		IPath otherPath = resource.getFullPath();
		return path.isPrefixOf(otherPath) || otherPath.isPrefixOf(path);
	}

	@Override
	public boolean isDerived() {
		return isDerived(IResource.NONE);
	}

	@Override
	public boolean isDerived(int options) {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		if (flags != NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_DERIVED))
			return true;
		// Check ancestors if the appropriate option is set.
		if ((options & CHECK_ANCESTORS) != 0)
			return getParent().isDerived(options);
		return false;
	}

	@Override
	public boolean isHidden() {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		return flags != NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_HIDDEN);
	}

	@Override
	public boolean isHidden(int options) {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		if (flags != NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_HIDDEN))
			return true;
		// Check ancestors if the appropriate option is set.
		if ((options & CHECK_ANCESTORS) != 0)
			return getParent().isHidden(options);
		return false;
	}

	@Override
	public boolean isLinked() {
		return isLinked(NONE);
	}

	@Override
	public boolean isLinked(int options) {
		if ((options & CHECK_ANCESTORS) != 0) {
			IProject project = getProject();
			if (project == null)
				return false;
			ProjectDescription desc = ((Project) project).internalGetDescription();
			if (desc == null)
				return false;
			HashMap<IPath, LinkDescription> links = desc.getLinks();
			if (links == null)
				return false;
			IPath myPath = getProjectRelativePath();
			for (LinkDescription linkDescription : links.values()) {
				if (linkDescription.getProjectRelativePath().isPrefixOf(myPath))
					return true;
			}
			return false;
		}
		// The no ancestor checking case.
		ResourceInfo info = getResourceInfo(false, false);
		return info != null && info.isSet(M_LINK);
	}

	@Override
	public boolean isVirtual() {
		ResourceInfo info = getResourceInfo(false, false);
		return info != null && info.isSet(M_VIRTUAL);
	}

	/**
	 * Checks whether the current resource has a parent that is virtual.
	 */
	public boolean isUnderVirtual() {
		IContainer parent = getParent();
		while (parent != null) {
			if (parent.isVirtual())
				return true;
			parent = parent.getParent();
		}
		return false;
	}

	@Override
	@Deprecated
	public boolean isLocal(int depth) {
		ResourceInfo info = getResourceInfo(false, false);
		return isLocal(getFlags(info), depth);
	}

	/**
	 * Note the depth parameter is intentionally ignored because
	 * this method is over-ridden by {@link Container#isLocal(int)}.
	 * @deprecated
	 */
	@Deprecated
	public boolean isLocal(int flags, int depth) {
		return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_LOCAL_EXISTS);
	}

	/**
	 * Returns whether a resource should be included in a traversal
	 * based on the provided member flags.
	 *
	 * @param flags The resource info flags
	 * @param memberFlags The member flag mask
	 * @return Whether the resource is included
	 */
	protected boolean isMember(int flags, int memberFlags) {
		int excludeMask = 0;
		if ((memberFlags & IContainer.INCLUDE_PHANTOMS) == 0)
			excludeMask |= M_PHANTOM;
		if ((memberFlags & IContainer.INCLUDE_HIDDEN) == 0)
			excludeMask |= M_HIDDEN;
		if ((memberFlags & IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS) == 0)
			excludeMask |= M_TEAM_PRIVATE_MEMBER;
		if ((memberFlags & IContainer.EXCLUDE_DERIVED) != 0)
			excludeMask |= M_DERIVED;
		// The resource is a matching member if it matches none of the exclude flags.
		return flags != NULL_FLAG && (flags & excludeMask) == 0;
	}

	@Override
	public boolean isPhantom() {
		ResourceInfo info = getResourceInfo(true, false);
		return isPhantom(getFlags(info));
	}

	public boolean isPhantom(int flags) {
		return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_PHANTOM);
	}

	@Deprecated
	@Override
	public boolean isReadOnly() {
		final ResourceAttributes attributes = getResourceAttributes();
		return attributes != null && attributes.isReadOnly();
	}

	@Override
	public boolean isSynchronized(int depth) {
		return getLocalManager().isSynchronized(this, depth);
	}

	@Override
	public boolean isTeamPrivateMember() {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		return flags != NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_TEAM_PRIVATE_MEMBER);
	}

	@Override
	public boolean isTeamPrivateMember(int options) {
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		if (flags != NULL_FLAG && ResourceInfo.isSet(flags, ICoreConstants.M_TEAM_PRIVATE_MEMBER))
			return true;
		// Check ancestors if the appropriate option is set.
		if ((options & CHECK_ANCESTORS) != 0)
			return getParent().isTeamPrivateMember(options);
		return false;
	}

	/**
	 * Returns true if this resource is a linked resource, or a child of a linked
	 * resource, and false otherwise.
	 */
	public boolean isUnderLink() {
		int depth = path.segmentCount();
		if (depth < 2)
			return false;
		if (depth == 2)
			return isLinked();
		// Check if parent at depth two is a link.
		IPath linkParent = path.removeLastSegments(depth - 2);
		return workspace.getResourceInfo(linkParent, false, false).isSet(ICoreConstants.M_LINK);
	}

	protected IPath makePathAbsolute(IPath target) {
		if (target.isAbsolute())
			return target;
		return getParent().getFullPath().append(target);
	}

	/**
	 * @see IFile#move(IPath, boolean, boolean, IProgressMonitor)
	 * @see IFolder#move(IPath, boolean, boolean, IProgressMonitor)
	 */
	public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		move(destination, updateFlags, monitor);
	}

	@Override
	public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		move(destination, force ? IResource.FORCE : IResource.NONE, monitor);
	}

	@Override
	public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		String message = NLS.bind(Messages.resources_moving, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		destination = makePathAbsolute(destination);
		checkValidPath(destination, getType(), false);
		Resource destResource = workspace.newResource(destination, getType());
		final ISchedulingRule rule = workspace.getRuleFactory().moveRule(this, destResource);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			// The following assert method throws CoreExceptions as stated in the IResource.move API
			// and assert for programming errors. See checkMoveRequirements for more information.
			assertMoveRequirements(destination, getType(), updateFlags);
			workspace.beginOperation(true);
			broadcastPreMoveEvent(destResource, updateFlags);
			IFileStore originalStore = getStore();
			message = Messages.resources_moveProblem;
			MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IStatus.ERROR, message, null);
			WorkManager workManager = workspace.getWorkManager();
			ResourceTree tree = new ResourceTree(workspace.getFileSystemManager(), workManager.getLock(), status, updateFlags);
			boolean success = false;
			int depth = 0;
			try {
				depth = workManager.beginUnprotected();
				success = unprotectedMove(tree, destResource, updateFlags, progress.split(50));
			} finally {
				workManager.endUnprotected(depth);
			}
			// Invalidate the tree for further use by clients.
			tree.makeInvalid();
			// Update any aliases of this resource and the destination.
			if (success) {
				workspace.getAliasManager().updateAliases(this, originalStore, IResource.DEPTH_INFINITE, progress.split(24));
				workspace.getAliasManager().updateAliases(destResource, destResource.getStore(), IResource.DEPTH_INFINITE, progress.split(24));
			}
			if (!tree.getStatus().isOK())
				throw new ResourceException(tree.getStatus());
			// If this is a project, make sure the move operation is remembered.
			if (getType() == PROJECT)
				workspace.getSaveManager().requestSnapshot();
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		move(description, updateFlags, monitor);
	}

	@Override
	public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(description);
		if (getType() != IResource.PROJECT) {
			String message = NLS.bind(Messages.resources_moveNotProject, getFullPath(), description.getName());
			throw new ResourceException(IResourceStatus.INVALID_VALUE, getFullPath(), message, null);
		}
		((Project) this).move(description, updateFlags, monitor);
	}

	@Override
	public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
		boolean isRoot = getType() == ROOT;
		String message = isRoot ? Messages.resources_refreshingRoot : NLS.bind(Messages.resources_refreshing, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, 100).checkCanceled();
		progress.subTask(message);
		boolean build = false;
		final ISchedulingRule rule = workspace.getRuleFactory().refreshRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			if ((!isRoot && !getProject().isAccessible()) || (!exists() && isFiltered()))
				return;
			workspace.beginOperation(true);
			if (getType() == IResource.PROJECT || getType() == IResource.ROOT)
				workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_REFRESH, this));
			build = getLocalManager().refresh(this, depth, true, progress.split(98));
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, build);
		}
	}

	@Override
	public String requestName() {
		return getName();
	}

	@Override
	public IPath requestPath() {
		return getFullPath();
	}

	@Override
	public void revertModificationStamp(long value) throws CoreException {
		if (value < 0)
			throw new IllegalArgumentException("Illegal value: " + value); //$NON-NLS-1$
		// Fetch the info but don't bother making it mutable even though we are going to modify it.
		// It really doesn't matter as the change we are making does not show up in deltas.
		ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);
		info.setModificationStamp(value);
	}

	@Deprecated
	@Override
	public void setDerived(boolean isDerived) throws CoreException {
		// Fetch the info but don't bother making it mutable even though we are going
		// to modify it.  We don't know whether or not the tree is open and it really doesn't
		// matter as the change we are making does not show up in deltas.
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		checkAccessible(flags);
		// Ignore attempts to set derived flag on anything except files and folders
		if (info.getType() == FILE || info.getType() == FOLDER) {
			if (isDerived) {
				info.set(ICoreConstants.M_DERIVED);
			} else {
				info.clear(ICoreConstants.M_DERIVED);
			}
		}
	}

	@Override
	public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
		String message = NLS.bind(Messages.resources_settingDerivedFlag, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		final ISchedulingRule rule = workspace.getRuleFactory().derivedRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			ResourceInfo info = getResourceInfo(false, false);
			checkAccessible(getFlags(info));
			// Ignore attempts to set derived flag on anything except files and folders.
			if (info.getType() != FILE && info.getType() != FOLDER)
				return;
			workspace.beginOperation(true);
			info = getResourceInfo(false, true);
			if (isDerived) {
				info.set(ICoreConstants.M_DERIVED);
			} else {
				info.clear(ICoreConstants.M_DERIVED);
			}
			progress.split(98);
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	@Override
	public void setHidden(boolean isHidden) throws CoreException {
		// Fetch the info but don't bother making it mutable even though we are going
		// to modify it.  We don't know whether or not the tree is open and it really doesn't
		// matter as the change we are making does not show up in deltas.
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		checkAccessible(flags);
		if (isHidden) {
			info.set(ICoreConstants.M_HIDDEN);
		} else {
			info.clear(ICoreConstants.M_HIDDEN);
		}
	}

	@Deprecated
	@Override
	public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
		String message = Messages.resources_setLocal;
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(null, split);
			workspace.beginOperation(true);
			internalSetLocal(flag, depth);
			progress.split(98);
		} finally {
			progress.done();
			workspace.endOperation(null, true);
		}
	}

	@Override
	public long setLocalTimeStamp(long value) throws CoreException {
		if (value < 0)
			throw new IllegalArgumentException("Illegal value: " + value); //$NON-NLS-1$
		// Fetch the info but don't bother making it mutable even though we are going to modify it.
		// It really doesn't matter as the change we are making does not show up in deltas.
		ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);
		return getLocalManager().setLocalTimeStamp(this, info, value);
	}

	@Override
	public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
		checkAccessibleAndLocal(DEPTH_ZERO);
		getPropertyManager().setProperty(this, key, value);
	}

	@Deprecated
	@Override
	public void setReadOnly(boolean readonly) {
		ResourceAttributes attributes = getResourceAttributes();
		if (attributes == null)
			return;
		attributes.setReadOnly(readonly);
		try {
			setResourceAttributes(attributes);
		} catch (CoreException e) {
			// Failure is not an option.
		}
	}

	@Override
	public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
		checkAccessibleAndLocal(DEPTH_ZERO);
		getLocalManager().setResourceAttributes(this, attributes);
	}

	@Override
	public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
		// Fetch the info but don't bother making it mutable even though we are going to modify it.
		// We don't know whether or not the tree is open and it really doesn't matter as the change
		// we are making does not show up in deltas.
		ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);
		info.setSessionProperty(key, value);
	}

	@Override
	public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
		// Fetch the info but don't bother making it mutable even though we are going
		// to modify it.  We don't know whether or not the tree is open and it really doesn't
		// matter as the change we are making does not show up in deltas.
		ResourceInfo info = getResourceInfo(false, false);
		int flags = getFlags(info);
		checkAccessible(flags);
		// ignore attempts to set team private member flag on anything except files and folders
		if (info.getType() == FILE || info.getType() == FOLDER) {
			if (isTeamPrivate) {
				info.set(ICoreConstants.M_TEAM_PRIVATE_MEMBER);
			} else {
				info.clear(ICoreConstants.M_TEAM_PRIVATE_MEMBER);
			}
		}
	}

	/**
	 * Returns true if this resource has the potential to be
	 * (or have been) synchronized.
	 */
	public boolean synchronizing(ResourceInfo info) {
		return info != null && info.getSyncInfo(false) != null;
	}

	@Override
	public String toString() {
		return getTypeString() + getFullPath().toString();
	}

	@Override
	public void touch(IProgressMonitor monitor) throws CoreException {
		String message = NLS.bind(Messages.resources_touch, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		final ISchedulingRule rule = workspace.getRuleFactory().modifyRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			ResourceInfo info = checkAccessibleAndLocal(DEPTH_ZERO);

			workspace.beginOperation(true);
			// Fake a change by incrementing the content ID.
			info = getResourceInfo(false, true);
			info.incrementContentId();
			// Forget content-related caching flags.
			info.clear(M_CONTENT_CACHE);
			workspace.updateModificationStamp(info);
			progress.split(98);
		} catch (OperationCanceledException e) {
			workspace.getWorkManager().operationCanceled();
			throw e;
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	/**
	 * Calls the move/delete hook to perform the deletion.  Since this method calls
	 * client code, it is run "unprotected", so the workspace lock is not held.
	 */
	private void unprotectedDelete(ResourceTree tree, int updateFlags, IProgressMonitor monitor) {
		IMoveDeleteHook hook = workspace.getMoveDeleteHook();
		SubMonitor progress = SubMonitor.convert(monitor, 2).checkCanceled();
		try {
			switch (getType()) {
			case IResource.FILE:
				if (!hook.deleteFile(tree, (IFile) this, updateFlags, progress.split(1)))
					tree.standardDeleteFile((IFile) this, updateFlags, progress.split(1));
				break;
			case IResource.FOLDER:
				if (!hook.deleteFolder(tree, (IFolder) this, updateFlags, progress.split(1)))
					tree.standardDeleteFolder((IFolder) this, updateFlags, progress.split(1));
				break;
			case IResource.PROJECT:
				if (!hook.deleteProject(tree, (IProject) this, updateFlags, progress.split(1)))
					tree.standardDeleteProject((IProject) this, updateFlags, progress.split(1));
				break;
			case IResource.ROOT:
				// When the root is deleted, all its children including hidden projects have to
				// be deleted.
				IProject[] projects = ((IWorkspaceRoot) this).getProjects(IContainer.INCLUDE_HIDDEN);
				progress.setWorkRemaining(projects.length * 2);
				for (IProject project : projects) {
					if (!hook.deleteProject(tree, project, updateFlags, progress.split(1)))
						tree.standardDeleteProject(project, updateFlags, progress.split(1));
				}
				break;
			}
		} finally {
			progress.done();
		}
	}

	/**
	 * Calls the move/delete hook to perform the move.  Since this method calls
	 * client code, it is run "unprotected", so the workspace lock is not held.
	 * Returns true if resources were actually moved, and false otherwise.
	 */
	private boolean unprotectedMove(ResourceTree tree, final IResource destination, int updateFlags, IProgressMonitor monitor) throws CoreException, ResourceException {
		IMoveDeleteHook hook = workspace.getMoveDeleteHook();
		SubMonitor progress = SubMonitor.convert(monitor, 2).checkCanceled();
		try {
			switch (getType()) {
			case IResource.FILE:
				if (!hook.moveFile(tree, (IFile) this, (IFile) destination, updateFlags, progress.split(1)))
					tree.standardMoveFile((IFile) this, (IFile) destination, updateFlags, progress.split(1));
				break;
			case IResource.FOLDER:
				if (!hook.moveFolder(tree, (IFolder) this, (IFolder) destination, updateFlags, progress.split(1)))
					tree.standardMoveFolder((IFolder) this, (IFolder) destination, updateFlags, progress.split(1));
				break;
			case IResource.PROJECT:
				IProject project = (IProject) this;
				// If there is no change in name, there is nothing to do so return.
				if (getName().equals(destination.getName()))
					return false;
				IProjectDescription description = project.getDescription();
				description.setName(destination.getName());
				if (!hook.moveProject(tree, project, description, updateFlags, progress.split(1)))
					tree.standardMoveProject(project, description, updateFlags, progress.split(1));
				break;
			case IResource.ROOT:
				String msg = Messages.resources_moveRoot;
				throw new ResourceException(new ResourceStatus(IResourceStatus.INVALID_VALUE, getFullPath(), msg));
			}
		} finally {
			progress.done();
		}
		return true;
	}

	private void broadcastPreDeleteEvent() throws CoreException {
		switch (getType()) {
			case IResource.PROJECT :
				workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_DELETE, this));
				break;
			case IResource.ROOT :
				// All root children including hidden projects will be deleted so notify.
				IResource[] projects = ((Container) this).getChildren(IContainer.INCLUDE_HIDDEN);
				for (IResource project2 : projects) {
					workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_DELETE, project2));
				}
				break;
		}
	}

	private void broadcastPreMoveEvent(final IResource destination, int updateFlags) throws CoreException {
		switch (getType()) {
			case IResource.FILE :
				if (isLinked())
					workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_MOVE, this, destination, updateFlags));
				break;
			case IResource.FOLDER :
				if (isLinked())
					workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_MOVE, this, destination, updateFlags));
				if (isVirtual())
					workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_GROUP_MOVE, this, destination, updateFlags));
				break;
			case IResource.PROJECT :
				if (!getName().equals(destination.getName())) {
					// If there is a change in name, we are deleting the source project so notify.
					workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_PROJECT_MOVE, this, destination, updateFlags));
				}
				break;
		}
	}

	@Override
	public IPathVariableManager getPathVariableManager() {
		if (getProject() == null)
			return workspace.getPathVariableManager();
		return new ProjectPathVariableManager(this);
	}

	/**
	 *  Calculates whether the current resource is filtered out from the resource tree
	 *  by resource filters.  This can happen because resource filters apply to the resource,
	 *  or because resource filters apply to one of its parent.  For example, if "/foo/bar"
	 *  is filtered out, then calling isFilteredFromParent() on "/foo/bar/sub/file.txt" will
	 *  return true as well, even though there's no resource filters that apply to "file.txt" per se.
	 *
	 * @return true is the resource is filtered out from the resource tree
	 * @see IContainer#createFilter(int, FileInfoMatcherDescription, int, IProgressMonitor)
	 */
	public boolean isFiltered() {
		try {
			return isFilteredWithException(false);
		} catch (CoreException e) {
			// nothing
		}
		return false;
	}

	public boolean isFilteredWithException(boolean throwExeception) throws CoreException {
		if (isLinked() || isVirtual())
			return false;

		Project project = (Project) getProject();
		if (project == null)
			return false;
		final ProjectDescription description = project.internalGetDescription();
		if ((description == null) || (description.getFilters() == null))
			return false;

		Resource currentResource = this;
		while (currentResource != null && currentResource.getParent() != null) {
			Resource parent = (Resource) currentResource.getParent();
			IFileStore store = currentResource.getStore();
			if (store != null) {
				FileInfo fileInfo = new FileInfo(store.getName());
				fileInfo.setDirectory(currentResource.getType() == IResource.FOLDER);
				if (fileInfo != null) {
					IFileInfo[] filtered = parent.filterChildren(project, description, new IFileInfo[] {fileInfo}, throwExeception);
					if (filtered.length == 0)
						return true;
				}
			}
			currentResource = parent;
		}
		return false;
	}

	public IFileInfo[] filterChildren(IFileInfo[] list, boolean throwException) throws CoreException {
		Project project = (Project) getProject();
		if (project == null)
			return list;
		final ProjectDescription description = project.internalGetDescription();
		if (description == null)
			return list;
		return filterChildren(project, description, list, throwException);
	}

	private IFileInfo[] filterChildren(Project project, ProjectDescription description, IFileInfo[] list, boolean throwException) throws CoreException {
		IPath relativePath = getProjectRelativePath();
		LinkedList<Filter> currentIncludeFilters = new LinkedList<>();
		LinkedList<Filter> currentExcludeFilters = new LinkedList<>();
		LinkedList<FilterDescription> filters = null;

		boolean firstSegment = true;
		do {
			if (!firstSegment)
				relativePath = relativePath.removeLastSegments(1);
			filters = description.getFilter(relativePath);
			if (filters != null) {
				for (FilterDescription desc : filters) {
					if (firstSegment || desc.isInheritable()) {
						Filter filter = new Filter(project, desc);
						if (filter.isIncludeOnly()) {
							if (filter.isFirst())
								currentIncludeFilters.addFirst(filter);
							else
								currentIncludeFilters.addLast(filter);
						} else {
							if (filter.isFirst())
								currentExcludeFilters.addFirst(filter);
							else
								currentExcludeFilters.addLast(filter);
						}
					}
				}
			}
			firstSegment = false;
		} while (relativePath.segmentCount() > 0);

		if ((currentIncludeFilters.size() > 0) || (currentExcludeFilters.size() > 0)) {
			try {
				list = Filter.filter(project, currentIncludeFilters, currentExcludeFilters, (IContainer) this, list);
			} catch (CoreException e) {
				if (throwException)
					throw e;
			}
		}
		return list;
	}

	/**
	 * @see IFolder#createLink(URI, int, IProgressMonitor)
	 * @see IFile#createLink(URI, int, IProgressMonitor)
	 */
	public void setLinkLocation(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
		if (!isLinked()) {
			String message = NLS.bind(Messages.links_resourceIsNotALink, getFullPath());
			throw new ResourceException(IResourceStatus.INVALID_VALUE, getFullPath(), message, null);
		}

		String message = NLS.bind(Messages.links_setLocation, getFullPath());
		SubMonitor progress = SubMonitor.convert(monitor, message, 100).checkCanceled();
		final ISchedulingRule rule = workspace.getRuleFactory().createRule(this);
		SubMonitor split = progress.split(1);
		try {
			workspace.prepareOperation(rule, split);
			workspace.broadcastEvent(LifecycleEvent.newEvent(LifecycleEvent.PRE_LINK_CHANGE, this));
			workspace.beginOperation(true);

			ResourceInfo info = workspace.getResourceInfo(getFullPath(), true, false);
			getLocalManager().setLocation(this, info, location);

			LinkDescription linkDescription;
			linkDescription = new LinkDescription(this, location);
			Project project = (Project) getProject();
			project.internalGetDescription().setLinkLocation(getProjectRelativePath(), linkDescription);
			project.writeDescription(updateFlags);

			// Refresh either in background or foreground.
			if ((updateFlags & IResource.BACKGROUND_REFRESH) != 0) {
				workspace.refreshManager.refresh(this);
				progress.split(99);
			} else {
				refreshLocal(DEPTH_INFINITE, progress.split(98));
			}
		} finally {
			progress.done();
			workspace.endOperation(rule, true);
		}
	}

	/**
	 * @see IFolder#createLink(IPath, int, IProgressMonitor)
	 * @see IFile#createLink(IPath, int, IProgressMonitor)
	 */
	public void setLinkLocation(IPath location, int updateFlags, IProgressMonitor monitor) throws CoreException {
		if (location.isAbsolute()) {
			setLinkLocation(URIUtil.toURI(location.toPortableString()), updateFlags, monitor);
		} else {
			URI uri;
			try {
				uri = new URI(null, null, location.toPortableString(), null);
			} catch (URISyntaxException e) {
				uri = URIUtil.toURI(location.toPortableString());
			}
			setLinkLocation(uri, updateFlags, monitor);
		}
	}
}
