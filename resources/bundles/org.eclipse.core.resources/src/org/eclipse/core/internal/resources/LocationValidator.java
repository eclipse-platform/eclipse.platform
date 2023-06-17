/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Project Path Variable Support
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * This class implements the various path, URI, and name validation methods
 * in the workspace API
 */
public class LocationValidator {
	private final Workspace workspace;

	public LocationValidator(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Returns a string representation of a URI suitable for displaying to an end user.
	 */
	private String toString(URI uri) {
		try {
			return EFS.getStore(uri).toString();
		} catch (CoreException e) {
			//there is no store defined, so the best we can do is the URI toString.
			return uri.toString();
		}
	}

	/**
	 * Check that the location is absolute
	 */
	private IStatus validateAbsolute(URI location, boolean error) {
		if (!location.isAbsolute()) {
			String message;
			String schemeSpecificPart = location.getSchemeSpecificPart();
			if (schemeSpecificPart == null || schemeSpecificPart.isEmpty()) {
				message = Messages.links_noPath;
			} else {
				IPath pathPart = IPath.fromOSString(schemeSpecificPart);
				if (pathPart.segmentCount() > 0)
					message = NLS.bind(Messages.pathvar_undefined, location.toString(), pathPart.segment(0));
				else
					message = Messages.links_noPath;
			}
			int code = error ? IResourceStatus.VARIABLE_NOT_DEFINED : IResourceStatus.VARIABLE_NOT_DEFINED_WARNING;
			return new ResourceStatus(code, null, message);
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see IWorkspace#validateLinkLocation(IResource, IPath)
	 */
	public IStatus validateLinkLocation(IResource resource, IPath unresolvedLocation) {
		IPath location = resource.getPathVariableManager().resolvePath(unresolvedLocation);
		if (location.isEmpty())
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), Messages.links_noPath);
		//check that the location is absolute
		if (!location.isAbsolute()) {
			//we know there is at least one segment, because of previous isEmpty check
			String message = NLS.bind(Messages.pathvar_undefined, location.toOSString(), location.segment(0));
			return new ResourceStatus(IResourceStatus.VARIABLE_NOT_DEFINED_WARNING, resource.getFullPath(), message);
		}
		//if the location doesn't have a device, see if the OS will assign one
		if (location.getDevice() == null)
			location = IPath.fromOSString(location.toFile().getAbsolutePath());
		return validateLinkLocationURI(resource, URIUtil.toURI(location));
	}

	public IStatus validateLinkLocationURI(IResource resource, URI unresolvedLocation) {
		String schemeSpecificPart = unresolvedLocation.getSchemeSpecificPart();
		if (schemeSpecificPart == null || schemeSpecificPart.isEmpty()) {
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), Messages.links_noPath);
		}
		String message;
		//check if resource linking is disabled
		if (ResourcesPlugin.getPlugin().getPluginPreferences().getBoolean(ResourcesPlugin.PREF_DISABLE_LINKING)) {
			message = NLS.bind(Messages.links_workspaceVeto, resource.getName());
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), message);
		}
		//check that the resource is the right type
		int type = resource.getType();
		if (type != IResource.FOLDER && type != IResource.FILE) {
			message = NLS.bind(Messages.links_notFileFolder, resource.getName());
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), message);
		}
		IContainer parent = resource.getParent();
		if (!parent.isAccessible()) {
			message = NLS.bind(Messages.links_parentNotAccessible, resource.getFullPath());
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), message);
		}
		URI location = resource.getPathVariableManager().resolveURI(unresolvedLocation);
		//check nature veto
		String[] natureIds = ((Project) resource.getProject()).internalGetDescription().getNatureIds();

		IStatus result = workspace.getNatureManager().validateLinkCreation(natureIds);
		if (!result.isOK())
			return result;
		//check team provider veto
		if (resource.getType() == IResource.FILE)
			result = workspace.getTeamHook().validateCreateLink((IFile) resource, IResource.NONE, location);
		else
			result = workspace.getTeamHook().validateCreateLink((IFolder) resource, IResource.NONE, location);
		if (!result.isOK())
			return result;
		//check the standard path name restrictions
		result = validateSegments(location);
		if (!result.isOK())
			return result;
		//check if the location is based on an undefined variable
		result = validateAbsolute(location, false);
		if (!result.isOK())
			return result;
		// test if the given location overlaps the platform metadata location
		URI testLocation = workspace.getMetaArea().getLocation().toFile().toURI();
		if (FileUtil.isOverlapping(location, testLocation)) {
			message = NLS.bind(Messages.links_invalidLocation, toString(location));
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), message);
		}
		//test if the given path overlaps the location of the given project
		testLocation = resource.getProject().getLocationURI();
		if (testLocation != null && FileUtil.isPrefixOf(location, testLocation)) {
			message = NLS.bind(Messages.links_locationOverlapsProject, toString(location));
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, resource.getFullPath(), message);
		}
		//warnings (all errors must be checked before all warnings)

		// Iterate over each known project and ensure that the location does not
		// conflict with any project locations or linked resource locations
		for (IProject project : workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN)) {
			// since we are iterating over the project in the workspace, we
			// know that they have been created before and must have a description
			IProjectDescription desc = ((Project) project).internalGetDescription();
			testLocation = desc.getLocationURI();
			if (testLocation != null && FileUtil.isOverlapping(location, testLocation)) {
				message = NLS.bind(Messages.links_overlappingResource, toString(location));
				return new ResourceStatus(IResourceStatus.OVERLAPPING_LOCATION, resource.getFullPath(), message);
			}
			//iterate over linked resources and check for overlap
			if (!project.isOpen())
				continue;
			IResource[] children = null;
			try {
				children = project.members();
			} catch (CoreException e) {
				//ignore projects that cannot be accessed
			}
			if (children == null)
				continue;
			for (IResource child : children) {
				if (child.isLinked()) {
					testLocation = child.getLocationURI();
					if (testLocation != null && FileUtil.isOverlapping(location, testLocation)) {
						message = NLS.bind(Messages.links_overlappingResource, toString(location));
						return new ResourceStatus(IResourceStatus.OVERLAPPING_LOCATION, resource.getFullPath(), message);
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see IWorkspace#validateName(String, int)
	 */
	public IStatus validateName(String segment, int type) {
		String message;

		/* segment must not be null */
		if (segment == null) {
			message = Messages.resources_nameNull;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		// cannot be an empty string
		if (segment.length() == 0) {
			message = Messages.resources_nameEmpty;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		/* test invalid characters */
		char[] chars = OS.INVALID_RESOURCE_CHARACTERS;
		for (char c : chars)
			if (segment.indexOf(c) != -1) {
				message = NLS.bind(Messages.resources_invalidCharInName, String.valueOf(c), segment);
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}

		/* test invalid OS names */
		if (!OS.isNameValid(segment)) {
			message = NLS.bind(Messages.resources_invalidName, segment);
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}
		return Status.OK_STATUS;
	}

	/**
	 * Validates that the given workspace path is valid for the given type.  If
	 * <code>lastSegmentOnly</code> is true, it is assumed that all segments except
	 * the last one have previously been validated.  This is an optimization for validating
	 * a leaf resource when it is known that the parent exists (and thus its parent path
	 * must already be valid).
	 */
	public IStatus validatePath(IPath path, int type, boolean lastSegmentOnly) {
		String message;

		/* path must not be null */
		if (path == null) {
			message = Messages.resources_pathNull;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		/* path must not have a device separator */
		if (path.getDevice() != null) {
			message = NLS.bind(Messages.resources_invalidCharInPath, String.valueOf(IPath.DEVICE_SEPARATOR), path);
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		/* path must not be the root path */
		if (path.isRoot()) {
			message = Messages.resources_invalidRoot;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		/* path must be absolute */
		if (!path.isAbsolute()) {
			message = NLS.bind(Messages.resources_mustBeAbsolute, path);
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		/* validate segments */
		int numberOfSegments = path.segmentCount();
		if ((type & IResource.PROJECT) != 0) {
			if (numberOfSegments == ICoreConstants.PROJECT_SEGMENT_LENGTH) {
				return validateName(path.segment(0), IResource.PROJECT);
			} else if (type == IResource.PROJECT) {
				message = NLS.bind(Messages.resources_projectPath, path);
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}
		}
		if ((type & (IResource.FILE | IResource.FOLDER)) != 0) {
			if (numberOfSegments < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH) {
				message = NLS.bind(Messages.resources_resourcePath, path);
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}
			int fileFolderType = type &= ~IResource.PROJECT;
			int segmentCount = path.segmentCount();
			if (lastSegmentOnly)
				return validateName(path.segment(segmentCount - 1), fileFolderType);
			IStatus status = validateName(path.segment(0), IResource.PROJECT);
			if (!status.isOK())
				return status;
			// ignore first segment (the project)
			for (int i = 1; i < segmentCount; i++) {
				status = validateName(path.segment(i), fileFolderType);
				if (!status.isOK())
					return status;
			}
			return Status.OK_STATUS;
		}
		message = NLS.bind(Messages.resources_invalidPath, path);
		return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
	}

	/* (non-Javadoc)
	 * @see IWorkspace#validatePath(String, int)
	 */
	public IStatus validatePath(String path, int type) {
		/* path must not be null */
		if (path == null) {
			String message = Messages.resources_pathNull;
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}
		return validatePath(IPath.fromOSString(path), type, false);
	}

	public IStatus validateProjectLocation(IProject context, IPath unresolvedLocation) {
		if (unresolvedLocation == null)
			return validateProjectLocationURI(context, null);
		IPath location;
		if (context != null)
			location = context.getPathVariableManager().resolvePath(unresolvedLocation);
		else
			location = workspace.getPathVariableManager().resolvePath(unresolvedLocation);
		//check that the location is absolute
		if (!location.isAbsolute()) {
			String message;
			if (location.segmentCount() > 0)
				message = NLS.bind(Messages.pathvar_undefined, location.toString(), location.segment(0));
			else
				message = Messages.links_noPath;
			return new ResourceStatus(IResourceStatus.VARIABLE_NOT_DEFINED, null, message);
		}
		return validateProjectLocationURI(context, URIUtil.toURI(location));
	}

	/* (non-Javadoc)
	 * @see IWorkspace#validateProjectLocationURI(IProject, URI)
	 */
	public IStatus validateProjectLocationURI(IProject context, URI unresolvedLocation) {
		if (context == null && unresolvedLocation == null)
			throw new IllegalArgumentException("Either a project or a location must be provided"); //$NON-NLS-1$

		// Checks if the new location overlaps the workspace metadata location
		boolean isMetadataLocation = false;

		if (unresolvedLocation != null) {
			if (URIUtil.equals(unresolvedLocation, URIUtil.toURI(Platform.getLocation().addTrailingSeparator().append(LocalMetaArea.F_METADATA)))) {
				isMetadataLocation = true;
			}
		} else if (context != null && context.getName().equals(LocalMetaArea.F_METADATA)) {
			isMetadataLocation = true;
		}

		String message;
		if (isMetadataLocation) {
			message = NLS.bind(Messages.resources_invalidPath, toString(URIUtil.toURI(Platform.getLocation().addTrailingSeparator().append(LocalMetaArea.F_METADATA))));
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}

		// the default is ok for all other projects
		if (unresolvedLocation == null)
			return Status.OK_STATUS;
		URI location;
		if (context != null)
			location = context.getPathVariableManager().resolveURI(unresolvedLocation);
		else
			location = workspace.getPathVariableManager().resolveURI(unresolvedLocation);
		//check the standard path name restrictions
		IStatus result = validateSegments(location);
		if (!result.isOK())
			return result;
		result = validateAbsolute(location, true);
		if (!result.isOK())
			return result;
		//check that the URI has a legal scheme
		try {
			EFS.getFileSystem(location.getScheme());
		} catch (CoreException e) {
			return e.getStatus();
		}
		//overlaps with default location can only occur with file URIs
		if (location.getScheme().equals(EFS.SCHEME_FILE)) {
			IPath locationPath = URIUtil.toPath(location);
			// test if the given location overlaps the default default location
			IPath defaultDefaultLocation = workspace.getRoot().getLocation();
			if (FileUtil.isPrefixOf(locationPath, defaultDefaultLocation)) {
				message = NLS.bind(Messages.resources_overlapWorkspace, toString(location), defaultDefaultLocation.toOSString());
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}
			// Test if the given location is the default location for any potential project except
			// the one being created.
			IPath parentPath = locationPath.removeLastSegments(1);
			if (FileUtil.isPrefixOf(parentPath, defaultDefaultLocation) && FileUtil.isPrefixOf(defaultDefaultLocation, parentPath) && (context == null || !locationPath.equals(defaultDefaultLocation.append(context.getName())))) {
				message = NLS.bind(Messages.resources_overlapProject, toString(location), locationPath.lastSegment());
				return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
			}
		}

		// Iterate over each known project and ensure that the location does not
		// conflict with any of their already defined locations.
		IProject[] projects = workspace.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects) {
			URI testLocation = project.getLocationURI();
			if (context != null && project.equals(context)) {
				//tolerate locations being the same if this is the project being tested
				if (URIUtil.equals(testLocation, location))
					continue;
				//a project cannot be moved inside of its current location
				if (!FileUtil.isPrefixOf(testLocation, location))
					continue;
			} else if (!URIUtil.equals(testLocation, location)) {
				// a project cannot have the same location as another existing project
				continue;
			}
			//in all other cases there is illegal overlap
			message = NLS.bind(Messages.resources_overlapProject, toString(location), project.getName());
			return new ResourceStatus(IResourceStatus.INVALID_VALUE, null, message);
		}
		//if this project exists and has linked resources, the project location cannot overlap
		//the locations of any linked resources in that project
		if (context != null && context.exists() && context.isOpen()) {
			IResource[] children = null;
			try {
				children = context.members();
			} catch (CoreException e) {
				//ignore projects that cannot be accessed
			}
			if (children != null) {
				for (IResource child : children) {
					if (child.isLinked()) {
						URI testLocation = child.getLocationURI();
						if (testLocation != null && FileUtil.isPrefixOf(testLocation, location)) {
							message = NLS.bind(Messages.links_locationOverlapsLink, toString(location));
							return new ResourceStatus(IResourceStatus.OVERLAPPING_LOCATION, context.getFullPath(), message);
						}
					}
				}
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * Validates the standard path name restrictions on the segments of the provided URI.
	 * @param location The URI to validate
	 * @return A status indicating if the segments of the provided URI are valid
	 */
	private IStatus validateSegments(URI location) {
		if (EFS.SCHEME_FILE.equals(location.getScheme())) {
			IPath pathPart = IPath.fromOSString(location.getSchemeSpecificPart());
			int segmentCount = pathPart.segmentCount();
			for (int i = 0; i < segmentCount; i++) {
				IStatus result = validateName(pathPart.segment(i), IResource.PROJECT);
				if (!result.isOK())
					return result;
			}
		}
		return Status.OK_STATUS;
	}
}