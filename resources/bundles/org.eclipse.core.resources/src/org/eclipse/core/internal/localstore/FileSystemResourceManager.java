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
 *     Martin Oberhuber (Wind River) - [210664] descriptionChanged(): ignore LF style
 *     Martin Oberhuber (Wind River) - [233939] findFilesForLocation() with symlinks
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Sergey Prigogin (Google) - [338010] Resource.createLink() does not preserve symbolic links
 *                              - [462440] IFile#getContents methods should specify the status codes for its exceptions
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Karsten Thoms <karsten.thoms@itemis.de> - Bug 521500
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileTree;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.refresh.RefreshManager;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.IManager;
import org.eclipse.core.internal.resources.LinkDescription;
import org.eclipse.core.internal.resources.ModelObjectWriter;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.internal.resources.ProjectDescriptionReader;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.resources.WorkspaceDescription;
import org.eclipse.core.internal.utils.BitMask;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;

/**
 * Manages the synchronization between the workspace's view and the file system.
 */
public class FileSystemResourceManager implements ICoreConstants, IManager {

	/**
	 * The history store is initialized lazily - always use the accessor method
	 */
	protected IHistoryStore _historyStore;
	protected Workspace workspace;

	private volatile boolean lightweightAutoRefreshEnabled;

	private final IPreferenceChangeListener lightweightAutoRefreshPrefListener = event -> {
		if (ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH.equals(event.getKey())) {
			lightweightAutoRefreshEnabled = Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES,
					ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false, null);
		}
	};

	public FileSystemResourceManager(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Returns the workspace paths of all resources that may correspond to
	 * the given file system location.  Returns an empty ArrayList if there are no
	 * such paths.  This method does not consider whether resources actually
	 * exist at the given locations.
	 * <p>
	 * The workspace paths of {@link IResource#HIDDEN} project and resources
	 * located in {@link IResource#HIDDEN} projects won't be added to the result.
	 * </p>
	 */
	protected ArrayList<IPath> allPathsForLocation(URI inputLocation) {
		URI canonicalLocation = FileUtil.canonicalURI(inputLocation);
		// First, try the canonical version of the inputLocation.
		// If the inputLocation is different from the canonical version, it will be tried second
		ArrayList<IPath> results = allPathsForLocationNonCanonical(canonicalLocation);
		if (results.isEmpty() && canonicalLocation != inputLocation) {
			results = allPathsForLocationNonCanonical(inputLocation);
		}
		return results;
	}

	private ArrayList<IPath> allPathsForLocationNonCanonical(URI inputLocation) {
		URI location = inputLocation;
		final boolean isFileLocation = EFS.SCHEME_FILE.equals(inputLocation.getScheme());
		final IWorkspaceRoot root = getWorkspace().getRoot();
		final ArrayList<IPath> results = new ArrayList<>();
		if (URIUtil.equals(location, locationURIFor(root, true))) {
			//there can only be one resource at the workspace root's location
			results.add(IPath.ROOT);
			return results;
		}
		for (IProject project : root.getProjects(IContainer.INCLUDE_HIDDEN)) {
			if (!project.exists())
				continue;
			//check the project location
			URI testLocation = locationURIFor(project, true);
			if (testLocation == null)
				continue;
			boolean usingAnotherScheme = !inputLocation.getScheme().equals(testLocation.getScheme());
			// if we are looking for file: locations try to get a file: location for this project
			if (isFileLocation && !EFS.SCHEME_FILE.equals(testLocation.getScheme()))
				testLocation = getFileURI(testLocation);
			if (testLocation == null)
				continue;
			URI relative = testLocation.relativize(location);
			if (!relative.isAbsolute() && !relative.equals(testLocation)) {
				IPath suffix = IPath.fromOSString(relative.getPath());
				results.add(project.getFullPath().append(suffix));
			}
			if (usingAnotherScheme) {
				// if a different scheme is used, we can't use the AliasManager, since the manager
				// map is stored using the EFS scheme, and not necessarily the SCHEME_FILE
				ProjectDescription description = ((Project) project).internalGetDescription();
				if (description == null)
					continue;
				HashMap<IPath, LinkDescription> links = description.getLinks();
				if (links == null)
					continue;
				for (LinkDescription link : links.values()) {
					IResource resource = project.findMember(link.getProjectRelativePath());
					IPathVariableManager pathMan = resource == null ? project.getPathVariableManager() : resource.getPathVariableManager();
					testLocation = pathMan.resolveURI(link.getLocationURI());
					// if we are looking for file: locations try to get a file: location for this link
					if (isFileLocation && !EFS.SCHEME_FILE.equals(testLocation.getScheme()))
						testLocation = getFileURI(testLocation);
					if (testLocation == null)
						continue;
					relative = testLocation.relativize(location);
					if (!relative.isAbsolute() && !relative.equals(testLocation)) {
						IPath suffix = IPath.fromOSString(relative.getPath());
						results.add(project.getFullPath().append(link.getProjectRelativePath()).append(suffix));
					}
				}
			}
		}
		try {
			findLinkedResourcesPaths(inputLocation, results);
		} catch (CoreException e) {
			Policy.log(e);
		}
		return results;
	}

	/**
	 * Asynchronously auto-refresh the requested resource if {@link ResourcesPlugin#PREF_LIGHTWEIGHT_AUTO_REFRESH} is enabled.
	 */
	private void asyncRefresh(IResource target) {
		if (lightweightAutoRefreshEnabled) {
			RefreshManager refreshManager = workspace.getRefreshManager();
			// refreshManager can be null during shutdown
			if (refreshManager != null) {
				refreshManager.refresh(target);
			}
		}
	}

	private void findLinkedResourcesPaths(URI inputLocation, final ArrayList<IPath> results) throws CoreException {
		IPath suffix = null;
		IFileStore fileStore = EFS.getStore(inputLocation);
		while (fileStore != null) {
			IResource[] resources = workspace.getAliasManager().findResources(fileStore);
			for (IResource resource : resources) {
				if (resource.isLinked()) {
					IPath path = resource.getFullPath();
					if (suffix != null)
						path = path.append(suffix);
					if (!results.contains(path))
						results.add(path);
				}
			}
			if (suffix == null)
				suffix = IPath.fromPortableString(fileStore.getName());
			else
				suffix = IPath.fromPortableString(fileStore.getName()).append(suffix);
			fileStore = fileStore.getParent();
		}
	}

	/**
	 * Tries to obtain a file URI for the given URI. Returns <code>null</code> if the file system associated
	 * to the URI scheme does not map to the local file system.
	 * @param locationURI the URI to convert
	 * @return a file URI or <code>null</code>
	 */
	private URI getFileURI(URI locationURI) {
		try {
			IFileStore testLocationStore = EFS.getStore(locationURI);
			java.io.File storeAsFile = testLocationStore.toLocalFile(EFS.NONE, null);
			if (storeAsFile != null)
				return URIUtil.toURI(storeAsFile.getAbsolutePath());
		} catch (CoreException e) {
			// we don't know such file system or some other failure, just return null
		}
		return null;
	}

	/**
	 * Returns all resources that correspond to the given file system location,
	 * including resources under linked resources. Returns an empty array if
	 * there are no corresponding resources.
	 * <p>
	 * If the {@link IContainer#INCLUDE_TEAM_PRIVATE_MEMBERS} flag is specified
	 * in the member flags, team private members will be included along with the
	 * others. If the {@link IContainer#INCLUDE_TEAM_PRIVATE_MEMBERS} flag is
	 * not specified (recommended), the result will omit any team private member
	 * resources.
	 * </p>
	 * <p>
	 * If the {@link IContainer#INCLUDE_HIDDEN} flag is specified in the member
	 * flags, hidden members will be included along with the others. If the
	 * {@link IContainer#INCLUDE_HIDDEN} flag is not specified (recommended),
	 * the result will omit any hidden member resources.
	 * </p>
	 * <p>
	 * The result will also omit resources that are explicitly excluded
	 * from the workspace according to existing resource filters.
	 * </p>
	 *
	 * @param location
	 *        the file system location
	 * @param files
	 *        resources that may exist below the project level can be either
	 *        files or folders. If this parameter is true, files will be
	 *        returned, otherwise containers will be returned.
	 * @param memberFlags
	 *        bit-wise or of member flag constants (
	 *        {@link IContainer#INCLUDE_TEAM_PRIVATE_MEMBERS} and
	 *        {@link IContainer#INCLUDE_HIDDEN}) indicating which members are of
	 *        interest
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public IResource[] allResourcesFor(URI location, boolean files, int memberFlags) {
		ArrayList result = allPathsForLocation(location);
		int count = 0;
		for (int i = 0, imax = result.size(); i < imax; i++) {
			//replace the path in the list with the appropriate resource type
			IResource resource = resourceFor((IPath) result.get(i), files);

			if (resource == null || ((Resource) resource).isFiltered() || (((memberFlags & IContainer.INCLUDE_HIDDEN) == 0) && resource.isHidden(IResource.CHECK_ANCESTORS)) || (((memberFlags & IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS) == 0) && resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS)))
				resource = null;

			result.set(i, resource);
			//count actual resources - some paths won't have a corresponding resource
			if (resource != null)
				count++;
		}
		//convert to array and remove null elements
		IResource[] toReturn = files ? (IResource[]) new IFile[count] : (IResource[]) new IContainer[count];
		count = 0;
		for (Object element : result) {
			IResource resource = (IResource) element;
			if (resource != null)
				toReturn[count++] = resource;
		}
		return toReturn;
	}

	/* (non-javadoc)
	 * @see IResource.getResourceAttributes
	 */
	public ResourceAttributes attributes(IResource resource) {
		IFileStore store = getStore(resource);
		IFileInfo fileInfo = store.fetchInfo();
		if (!fileInfo.exists())
			return null;
		return FileUtil.fileInfoToAttributes(fileInfo);
	}

	/**
	 * Returns a container for the given file system location or null if there
	 * is no mapping for this path. If the path has only one segment, then an
	 * <code>IProject</code> is returned.  Otherwise, the returned object
	 * is a <code>IFolder</code>.  This method does NOT check the existence
	 * of a folder in the given location. Location cannot be null.
	 * <p>
	 * The result will also omit resources that are explicitly excluded
	 * from the workspace according to existing resource filters. If all resources
	 * are omitted, the result may be null.
	 * </p>
	 * <p>
	 * Returns a folder whose path has a minimal number of segments.
	 * I.e. a folder in a nested project is preferred over a folder in an enclosing project.
	 * </p>
	 */
	public IContainer containerForLocation(IPath location) {
		return (IContainer) resourceForLocation(location, false);
	}

	/**
	 * Returns a resource corresponding to the given location.  The
	 * "files" parameter is used for paths of two or more segments.  If true,
	 * a file is returned, otherwise a folder is returned.  Returns null if files is true
	 * and the path is not of sufficient length. Also returns null if the resource is
	 * filtered out by resource filters.
	 * <p>
	 * Returns a resource whose path has a minimal number of segments.
	 * I.e. a resource in a nested project is preferred over a resource in an enclosing project.
	 * </p>
	 */
	private IResource resourceForLocation(IPath location, boolean files) {
		if (workspace.getRoot().getLocation().equals(location)) {
			if (!files)
				return resourceFor(IPath.ROOT, false);
			return null;
		}
		int resultProjectPathSegments = 0;
		IResource result = null;
		IProject[] projects = getWorkspace().getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
		for (IProject project : projects) {
			IPath projectLocation = project.getLocation();
			if (projectLocation != null && projectLocation.isPrefixOf(location)) {
				int segmentsToRemove = projectLocation.segmentCount();
				if (segmentsToRemove > resultProjectPathSegments) {
					IPath path = project.getFullPath().append(location.removeFirstSegments(segmentsToRemove));
					IResource resource = resourceFor(path, files);
					if (resource != null && !((Resource) resource).isFiltered()) {
						resultProjectPathSegments = segmentsToRemove;
						result = resource;
					}
				}
			}
		}
		return result;
	}

	public void copy(IResource target, IResource destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		String title = NLS.bind(Messages.localstore_copying, target.getFullPath());

		SubMonitor subMonitor = SubMonitor.convert(monitor, title, 100);
		IFileStore destinationStore = getStore(destination);
		if (destinationStore.fetchInfo().exists()) {
			String message = NLS.bind(Messages.localstore_resourceExists, destination.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, destination.getFullPath(), message, null);
		}
		getHistoryStore().copyHistory(target, destination, false);
		CopyVisitor visitor = new CopyVisitor(target, destination, updateFlags, subMonitor.split(100));
		UnifiedTree tree = new UnifiedTree(target);
		tree.accept(visitor, IResource.DEPTH_INFINITE);
		IStatus status = visitor.getStatus();
		if (!status.isOK()) {
			throw new ResourceException(status);
		}
	}

	public void delete(IResource target, int flags, IProgressMonitor monitor) throws CoreException {

		Resource resource = (Resource) target;
		final int deleteWork = resource.countResources(IResource.DEPTH_INFINITE, false) * 2;
		boolean force = (flags & IResource.FORCE) != 0;
		int refreshWork = 0;
		if (!force) {
			refreshWork = Math.min(deleteWork, 100);
		}
		String title = NLS.bind(Messages.localstore_deleting, resource.getFullPath());

		SubMonitor subMonitor = SubMonitor.convert(monitor, title, deleteWork + refreshWork);
		MultiStatus status = new MultiStatus(ResourcesPlugin.PI_RESOURCES, IResourceStatus.FAILED_DELETE_LOCAL, Messages.localstore_deleteProblem, null);
		List<Resource> skipList = null;
		UnifiedTree tree = new UnifiedTree(target);
		if (!force) {
			CollectSyncStatusVisitor refreshVisitor = new CollectSyncStatusVisitor(Messages.localstore_deleteProblem, subMonitor.split(refreshWork));
			refreshVisitor.setIgnoreLocalDeletions(true);
			tree.accept(refreshVisitor, IResource.DEPTH_INFINITE);
			status.merge(refreshVisitor.getSyncStatus());
			skipList = refreshVisitor.getAffectedResources();
		}
		DeleteVisitor deleteVisitor = new DeleteVisitor(skipList, flags, subMonitor.split(deleteWork), deleteWork);
		tree.accept(deleteVisitor, IResource.DEPTH_INFINITE);
		status.merge(deleteVisitor.getStatus());
		if (!status.isOK()) {
			throw new ResourceException(status);
		}

	}

	/**
	 * Returns true if the description on disk is different from the given byte array,
	 * and false otherwise.
	 * Since org.eclipse.core.resources 3.4.1 differences in line endings (CR, LF, CRLF)
	 * are not considered.
	 */
	private boolean descriptionChanged(IFile descriptionFile, byte[] newContents) {
		//buffer size: twice the description length, but maximum 8KB
		int bufsize = newContents.length > 4096 ? 8192 : newContents.length * 2;
		try (
			InputStream oldStream = new BufferedInputStream(descriptionFile.getContents(true), bufsize);
		) {
			InputStream newStream = new ByteArrayInputStream(newContents);
			//compare streams char by char, ignoring line endings
			int newChar = newStream.read();
			int oldChar = oldStream.read();
			while (newChar >= 0 && oldChar >= 0) {
				if (newChar == oldChar) {
					//streams are the same
					newChar = newStream.read();
					oldChar = oldStream.read();
				} else if ((newChar == '\r' || newChar == '\n') && (oldChar == '\r' || oldChar == '\n')) {
					//got a difference, but both sides are newlines: read over newlines
					while (newChar == '\r' || newChar == '\n')
						newChar = newStream.read();
					while (oldChar == '\r' || oldChar == '\n')
						oldChar = oldStream.read();
				} else {
					//streams are different
					return true;
				}
			}
			//test for excess data in one stream
			if (newChar >= 0 || oldChar >= 0)
				return true;
			return false;
		} catch (Exception e) {
			Policy.log(e);
			//if we failed to compare, just write the new contents
		}
		return true;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public int doGetEncoding(IFileStore store) throws CoreException {
		try (
			InputStream input = store.openInputStream(EFS.NONE, null);
		) {
			int first = input.read();
			int second = input.read();
			if (first == -1 || second == -1)
				return IFile.ENCODING_UNKNOWN;
			first &= 0xFF;//converts unsigned byte to int
			second &= 0xFF;
			//look for the UTF-16 Byte Order Mark (BOM)
			if (first == 0xFE && second == 0xFF)
				return IFile.ENCODING_UTF_16BE;
			if (first == 0xFF && second == 0xFE)
				return IFile.ENCODING_UTF_16LE;
			int third = (input.read() & 0xFF);
			if (third == -1)
				return IFile.ENCODING_UNKNOWN;
			//look for the UTF-8 BOM
			if (first == 0xEF && second == 0xBB && third == 0xBF)
				return IFile.ENCODING_UTF_8;
			return IFile.ENCODING_UNKNOWN;
		} catch (IOException e) {
			String message = NLS.bind(Messages.localstore_couldNotRead, store.toString());
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, null, message, e);
		}
	}

	/**
	 * Optimized sync check for files.  Returns true if the file exists and is in sync, and false
	 * otherwise.  The intent is to let the default implementation handle the complex
	 * cases like gender change, case variants, etc.
	 */
	public boolean fastIsSynchronized(File target) {
		ResourceInfo info = target.getResourceInfo(false, false);
		if (target.exists(target.getFlags(info), true)) {
			IFileInfo fileInfo = getStore(target).fetchInfo();
			if (!fileInfo.isDirectory() && info.getLocalSyncInfo() == fileInfo.getLastModified())
				return true;
		}
		return false;
	}

	public boolean fastIsSynchronized(Folder target) {
		ResourceInfo info = target.getResourceInfo(false, false);
		if (target.exists(target.getFlags(info), true)) {
			IFileInfo fileInfo = getStore(target).fetchInfo();
			if (!fileInfo.exists() && info.getLocalSyncInfo() == fileInfo.getLastModified())
				return true;
		}
		return false;
	}

	/**
	 * Returns an IFile for the given file system location or null if there
	 * is no mapping for this path. This method does NOT check the existence
	 * of a file in the given location. Location cannot be null.
	 * <p>
	 * The result will also omit resources that are explicitly excluded
	 * from the workspace according to existing resource filters. If all resources
	 * are omitted, the result may be null.
	 * </p>
	 * <p>
	 * Returns a file whose path has a minimal number of segments.
	 * I.e. a file in a nested project is preferred over a file in an enclosing project.
	 * </p>
	 */
	public IFile fileForLocation(IPath location) {
		return (IFile) resourceForLocation(location, true);
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public int getEncoding(File target) throws CoreException {
		// thread safety: (the location can be null if the project for this file does not exist)
		IFileStore store = getStore(target);
		if (!store.fetchInfo().exists()) {
			String message = NLS.bind(Messages.localstore_fileNotFound, store.toString());
			throw new ResourceException(IResourceStatus.FAILED_READ_LOCAL, target.getFullPath(), message, null);
		}
		return doGetEncoding(store);
	}

	public IHistoryStore getHistoryStore() {
		if (_historyStore == null) {
			IPath location = getWorkspace().getMetaArea().getHistoryStoreLocation();
			location.toFile().mkdirs();
			IFileStore store = EFS.getLocalFileSystem().getStore(location);
			_historyStore = new HistoryStore2(getWorkspace(), store, 256);
		}
		return _historyStore;
	}

	/**
	 * Returns the real name of the resource on disk. Returns null if no local
	 * file exists by that name.  This is useful when dealing with
	 * case insensitive file systems.
	 */
	public String getLocalName(IFileStore target) {
		return target.fetchInfo().getName();
	}

	protected IPath getProjectDefaultLocation(IProject project) {
		return workspace.getRoot().getLocation().append(project.getFullPath());
	}

	/**
	 * Never returns null.
	 *
	 * @param target the resource to get a store for
	 * @return The file store for this resource
	 */
	public IFileStore getStore(IResource target) {
		try {
			return getStoreRoot(target).createStore(target.getFullPath(), target);
		} catch (CoreException e) {
			//callers aren't expecting failure here, so return null file system
			return EFS.getNullFileSystem().getStore(target.getFullPath());
		}
	}

	/**
	 * Returns the file store root for the provided resource. Never returns null.
	 */
	private FileStoreRoot getStoreRoot(IResource target) {
		ResourceInfo info = workspace.getResourceInfo(target.getFullPath(), true, false);
		FileStoreRoot root;
		if (info != null) {
			root = info.getFileStoreRoot();
			if (root != null && root.isValid())
				return root;
			if (info.isSet(ICoreConstants.M_VIRTUAL)) {
				ProjectDescription description = ((Project) target.getProject()).internalGetDescription();
				if (description != null) {
					setLocation(target, info, description.getGroupLocationURI(target.getProjectRelativePath()));
					return info.getFileStoreRoot();
				}
				return info.getFileStoreRoot();
			}
			if (info.isSet(ICoreConstants.M_LINK)) {
				ProjectDescription description = ((Project) target.getProject()).internalGetDescription();
				if (description != null) {
					final URI linkLocation = description.getLinkLocationURI(target.getProjectRelativePath());
					//if we can't determine the link location, fall through to parent resource
					if (linkLocation != null) {
						setLocation(target, info, linkLocation);
						return info.getFileStoreRoot();
					}
				}
			}
		}
		final IContainer parent = target.getParent();
		if (parent == null) {
			//this is the root, so we know where this must be located
			//initialize root location
			info = workspace.getResourceInfo(IPath.ROOT, false, true);
			final IWorkspaceRoot rootResource = workspace.getRoot();
			setLocation(rootResource, info, URIUtil.toURI(rootResource.getLocation()));
			return info.getFileStoreRoot();
		}
		root = getStoreRoot(parent);
		if (info != null)
			info.setFileStoreRoot(root);
		return root;
	}

	protected Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Returns whether the project has any local content on disk.
	 */
	public boolean hasSavedContent(IProject project) {
		return getStore(project).fetchInfo().exists();
	}

	/**
	 * Returns whether the project has a project description file on disk.
	 */
	public boolean hasSavedDescription(IProject project) {
		IResource dotProjectResource = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		return dotProjectResource.exists() ? //
			getStore(dotProjectResource).fetchInfo().exists() : //
			getStore(project).getChild(IProjectDescription.DESCRIPTION_FILE_NAME).fetchInfo().exists();
	}

	/**
	 * Initializes the file store for a resource.
	 *
	 * @param target The resource to initialize the file store for.
	 * @param location the File system location of this resource on disk
	 * @return The file store for the provided resource
	 */
	private IFileStore initializeStore(IResource target, URI location, boolean locationAlreadyNormalized)
			throws CoreException {
		ResourceInfo info = ((Resource) target).getResourceInfo(false, true);
		if (locationAlreadyNormalized) {
			setNormalizedLocation(target, info, location);
		} else {
			setLocation(target, info, location);
		}
		FileStoreRoot root = getStoreRoot(target);
		return root.createStore(target.getFullPath(), target);
	}

	/**
	 * The target must exist in the workspace.  This method must only ever
	 * be called from Project.writeDescription(), because that method ensures
	 * that the description isn't then immediately discovered as a new change.
	 * @return true if a new description was written, and false if it wasn't written
	 * because it was unchanged
	 */
	public boolean internalWrite(IProject target, IProjectDescription description, int updateFlags, boolean hasPublicChanges, boolean hasPrivateChanges) throws CoreException {
		//write the project's private description to the metadata area
		if (hasPrivateChanges)
			getWorkspace().getMetaArea().writePrivateDescription(target);
		//can't do anything if there's no description
		if (!hasPublicChanges || (description == null))
			return false;

		//write the model to a byte array
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IFile descriptionFile = target.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		try {
			new ModelObjectWriter().write(description, out, descriptionFile.getLineSeparator(true));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.resources_writeMeta, target.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, target.getFullPath(), msg, e);
		}
		byte[] newContents = out.toByteArray();

		//write the contents to the IFile that represents the description
		if (!descriptionFile.exists())
			workspace.createResource(descriptionFile, false);
		else {
			//if the description has not changed, don't write anything
			if (!descriptionChanged(descriptionFile, newContents))
				return false;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(newContents);
		IFileStore descriptionFileStore = ((Resource) descriptionFile).getStore();
		IFileInfo fileInfo = descriptionFileStore.fetchInfo();

		if (fileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
			IStatus result = getWorkspace().validateEdit(new IFile[] {descriptionFile}, null);
			if (!result.isOK())
				throw new ResourceException(result);
			// re-read the file info in case the file attributes were modified
			fileInfo = descriptionFileStore.fetchInfo();
		}

		//write the project description file (don't use API because scheduling rule might not match)
		write(descriptionFile, in, fileInfo, IResource.FORCE, false, SubMonitor.convert(null));
		workspace.getAliasManager().updateAliases(descriptionFile, getStore(descriptionFile), IResource.DEPTH_ZERO, SubMonitor.convert(null));

		//update the timestamp on the project as well so we know when it has
		//been changed from the outside
		long lastModified = ((Resource) descriptionFile).getResourceInfo(false, false).getLocalSyncInfo();
		ResourceInfo info = ((Resource) target).getResourceInfo(false, true);
		updateLocalSync(info, lastModified);

		//for backwards compatibility, ensure the old .prj file is deleted
		getWorkspace().getMetaArea().clearOldDescription(target);
		return true;
	}

	/**
	 * Returns true if the given project's description is synchronized with
	 * the project description file on disk, and false otherwise.
	 */
	public boolean isDescriptionSynchronized(IProject target) {
		//sync info is stored on the description file, and on project info.
		//when the file is changed by someone else, the project info modification
		//stamp will be out of date
		IFile descriptionFile = target.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ResourceInfo projectInfo = ((Resource) target).getResourceInfo(false, false);
		if (projectInfo == null)
			return false;
		return projectInfo.getLocalSyncInfo() == getStore(descriptionFile).fetchInfo().getLastModified();
	}

	/**
	 * Returns true if the given resource is synchronized with the file system
	 * to the given depth.  Returns false otherwise.
	 *
	 * Any discovered out-of-sync resources are scheduled to be brought
	 * back in sync, if {@link ResourcesPlugin#PREF_LIGHTWEIGHT_AUTO_REFRESH} is
	 * enabled.
	 *
	 * @see IResource#isSynchronized(int)
	 */
	public boolean isSynchronized(IResource target, int depth) {
		switch (target.getType()) {
			case IResource.ROOT :
				if (depth == IResource.DEPTH_ZERO)
					return true;
				//check sync on child projects.
				depth = depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : depth;
				IProject[] projects = ((IWorkspaceRoot) target).getProjects(IContainer.INCLUDE_HIDDEN);
				for (IProject project : projects) {
					if (!isSynchronized(project, depth)) {
						return false;
					}
				}
				return true;
			case IResource.PROJECT :
				if (!target.isAccessible())
					return true;
				break;
			case IResource.FOLDER :
				if (fastIsSynchronized((Folder) target))
					return true;
				break;
			case IResource.FILE :
				if (fastIsSynchronized((File) target))
					return true;
				break;
		}
		IsSynchronizedVisitor visitor = new IsSynchronizedVisitor(SubMonitor.convert(null));
		UnifiedTree tree = new UnifiedTree(target);
		try {
			tree.accept(visitor, depth);
		} catch (CoreException e) {
			Policy.log(e);
			return false;
		} catch (IsSynchronizedVisitor.ResourceChangedException e) {
			// Ask refresh manager to bring out-of-sync resource back into sync when convenient
			asyncRefresh(e.target);
			//visitor throws an exception if out of sync
			return false;
		}
		return true;
	}

	/**
	 * Check whether the preference {@link ResourcesPlugin#PREF_LIGHTWEIGHT_AUTO_REFRESH} is
	 * enabled.  When this preference is true the Resources plugin automatically refreshes
	 * resources which are known to be out-of-sync, and may install lightweight filesystem
	 * notification hooks.
	 * @return whether this FSRM is automatically refreshing discovered out-of-sync resources
	 */
	public boolean isLightweightAutoRefreshEnabled() {
		return lightweightAutoRefreshEnabled;
	}

	public void link(Resource target, URI location, IFileInfo fileInfo) throws CoreException {
		initializeStore(target, location, false);
		ResourceInfo info = target.getResourceInfo(false, true);
		long lastModified = fileInfo == null ? 0 : fileInfo.getLastModified();
		if (lastModified == 0)
			info.clearModificationStamp();
		updateLocalSync(info, lastModified);
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved. No canonicalization is
	 * applied to the returned path.
	 *
	 * @param target the resource to get the location for
	 */
	public IPath locationFor(IResource target) {
		return locationFor(target, false);
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved.
	 *
	 * @param target the resource to get the location for
	 * @param canonical if {@code true}, the prefix of the returned path corresponding
	 *     to the resource's file store root will be canonicalized
	 */
	public IPath locationFor(IResource target, boolean canonical) {
		return getStoreRoot(target).localLocation(target.getFullPath(), target, false);
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved. No canonicalization is
	 * applied to the returned URI.
	 *
	 * @param target the resource to get the location URI for
	 */
	public URI locationURIFor(IResource target) {
		return locationURIFor(target, false);
	}

	/**
	 * Returns the resolved, absolute file system location of the given resource.
	 * Returns null if the location could not be resolved.
	 *
	 * @param target the resource to get the location URI for
	 * @param canonical if {@code true}, the prefix of the path of the returned URI
	 *     corresponding to resource's file store root will be canonicalized
	 */
	public URI locationURIFor(IResource target, boolean canonical) {
		return getStoreRoot(target).computeURI(target.getFullPath(), canonical);
	}

	public void move(IResource source, IFileStore destination, int flags, IProgressMonitor monitor) throws CoreException {
		//TODO figure out correct semantics for case where destination exists on disk
		getStore(source).move(destination, EFS.NONE, monitor);
	}

	public InputStream read(IFile target, boolean force, IProgressMonitor monitor) throws CoreException {
		IFileStore store = getStore(target);
		if (lightweightAutoRefreshEnabled || !force) {
			final IFileInfo fileInfo = store.fetchInfo();
			if (!fileInfo.exists()) {
				asyncRefresh(target);
				String message = NLS.bind(Messages.localstore_fileNotFound, store.toString());
				throw new ResourceException(IResourceStatus.RESOURCE_NOT_FOUND, target.getFullPath(), message, null);
			}
			ResourceInfo info = ((Resource) target).getResourceInfo(true, false);
			int flags = ((Resource) target).getFlags(info);
			((Resource) target).checkExists(flags, true);
			if (fileInfo.getLastModified() != info.getLocalSyncInfo()) {
				asyncRefresh(target);
				if (!force) {
					String message = NLS.bind(Messages.localstore_resourceIsOutOfSync, target.getFullPath());
					throw new ResourceException(IResourceStatus.OUT_OF_SYNC_LOCAL, target.getFullPath(), message, null);
				}
			}
		}
		try {
			return store.openInputStream(EFS.NONE, monitor);
		} catch (CoreException e) {
			if (e.getStatus().getCode() == EFS.ERROR_NOT_EXISTS) {
				String message = NLS.bind(Messages.localstore_fileNotFound, store.toString());
				throw new ResourceException(IResourceStatus.RESOURCE_NOT_FOUND, target.getFullPath(), message, e);
			}
			throw e;
		}
	}

	/**
	 * Reads and returns the project description for the given project.
	 * Never returns null.
	 * @param target the project whose description should be read.
	 * @param creation true if this project is just being created, in which
	 * case the private project information (including the location) needs to be read
	 * from disk as well.
	 * @exception CoreException if there was any failure to read the project
	 * description, or if the description was missing.
	 */
	public ProjectDescription read(IProject target, boolean creation) throws CoreException {

		//read the project location if this project is being created
		URI projectLocation = null;
		ProjectDescription privateDescription = null;
		if (creation) {
			privateDescription = new ProjectDescription();
			getWorkspace().getMetaArea().readPrivateDescription(target, privateDescription);
			projectLocation = privateDescription.getLocationURI();
		} else {
			IProjectDescription description = ((Project) target).internalGetDescription();
			if (description != null && description.getLocationURI() != null) {
				projectLocation = description.getLocationURI();
			}
		}
		final boolean isDefaultLocation = projectLocation == null;
		if (isDefaultLocation) {
			projectLocation = URIUtil.toURI(getProjectDefaultLocation(target));
		}
		IFile descFile = target.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		IFileStore projectStore = initializeStore(target, projectLocation, true);
		IFileStore descriptionStore = descFile.exists() ? getStore(descFile) : projectStore.getChild(IProjectDescription.DESCRIPTION_FILE_NAME);
		ProjectDescription description = null;
		//hold onto any exceptions until after sync info is updated, then throw it
		ResourceException error = null;
		try (
			InputStream in = new BufferedInputStream(descriptionStore.openInputStream(EFS.NONE, SubMonitor.convert(null)));
		) {
			// IFileStore#openInputStream may cancel the monitor, thus the monitor state is checked
			description = new ProjectDescriptionReader(target).read(new InputSource(in));
		} catch (OperationCanceledException e) {
			String msg = NLS.bind(Messages.resources_missingProjectMeta, target.getName());
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, e);
		} catch (CoreException e) {
			//try the legacy location in the meta area
			description = getWorkspace().getMetaArea().readOldDescription(target);
			if (description != null)
				return description;
			if (!descriptionStore.fetchInfo().exists()) {
				String msg = NLS.bind(Messages.resources_missingProjectMeta, target.getName());
				throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, null);
			}
			String msg = NLS.bind(Messages.resources_readProjectMeta, target.getName());
			error = new ResourceException(IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, e);
		} catch (IOException ex) {
			// ignore
		}
		if (error == null && description == null) {
			String msg = NLS.bind(Messages.resources_readProjectMeta, target.getName());
			error = new ResourceException(IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, null);
		}
		if (description != null) {
			if (!isDefaultLocation)
				description.setLocationURI(projectLocation);
			if (creation && privateDescription != null)
				// Bring dynamic state back to life
				description.updateDynamicState(privateDescription);
		}
		long lastModified = descriptionStore.fetchInfo().getLastModified();
		IFile descriptionFile = target.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		//don't get a mutable copy because we might be in restore which isn't an operation
		//it doesn't matter anyway because local sync info is not included in deltas
		ResourceInfo info = ((Resource) descriptionFile).getResourceInfo(false, false);
		if (info == null) {
			//create a new resource on the sly -- don't want to start an operation
			info = getWorkspace().createResource(descriptionFile, false);
			updateLocalSync(info, lastModified);
		}
		//if the project description has changed between sessions, let it remain
		//out of sync -- that way link changes will be reconciled on next refresh
		if (!creation)
			updateLocalSync(info, lastModified);

		//update the timestamp on the project as well so we know when it has
		//been changed from the outside
		info = ((Resource) target).getResourceInfo(false, true);
		updateLocalSync(info, lastModified);

		if (error != null)
			throw error;
		return description;
	}

	public boolean refresh(IResource target, int depth, boolean updateAliases, IProgressMonitor monitor) throws CoreException {
		switch (target.getType()) {
			case IResource.ROOT :
				return refreshRoot((IWorkspaceRoot) target, depth, updateAliases, monitor);
			case IResource.PROJECT :
				if (!target.isAccessible())
					return false;
				//fall through
			case IResource.FOLDER :
			case IResource.FILE :
				return refreshResource(target, depth, updateAliases, monitor);
		}
		return false;
	}

	protected boolean refreshResource(IResource target, int depth, boolean updateAliases, IProgressMonitor monitor) throws CoreException {
		String title = NLS.bind(Messages.localstore_refreshing, target.getFullPath());
		SubMonitor subMonitor = SubMonitor.convert(monitor, title, 100);
		IFileTree fileTree = null;
		// If there can be more than one resource to refresh, try to get the whole tree in one shot, if the file system supports it.
		if (depth != IResource.DEPTH_ZERO) {
			IFileStore fileStore = ((Resource) target).getStore();
			fileTree = fileStore.getFileSystem().fetchFileTree(fileStore, subMonitor.newChild(2));
		}
		UnifiedTree tree = fileTree == null ? new UnifiedTree(target) : new UnifiedTree(target, fileTree);
		SubMonitor refreshMonitor = subMonitor.newChild(98);
		RefreshLocalVisitor visitor = updateAliases ? new RefreshLocalAliasVisitor(refreshMonitor) : new RefreshLocalVisitor(refreshMonitor);
		tree.accept(visitor, depth);
		IStatus result = visitor.getErrorStatus();
		if (!result.isOK())
			throw new ResourceException(result);
		return visitor.resourcesChanged();
	}

	/**
	 * Synchronizes the entire workspace with the local file system.
	 * The current implementation does this by synchronizing each of the
	 * projects currently in the workspace.  A better implementation may
	 * be possible.
	 */
	protected boolean refreshRoot(IWorkspaceRoot target, int depth, boolean updateAliases, IProgressMonitor monitor) throws CoreException {
		IProject[] projects = target.getProjects(IContainer.INCLUDE_HIDDEN);
		String title = Messages.localstore_refreshingRoot;
		SubMonitor subMonitor = SubMonitor.convert(monitor, title, projects.length);
		// if doing depth zero, there is nothing to do (can't refresh the root).
		// Note that we still need to do the beginTask, done pair.
		if (depth == IResource.DEPTH_ZERO)
			return false;
		boolean changed = false;
		// drop the depth by one level since processing the root counts as one level.
		depth = depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : depth;
		for (IProject project : projects) {
			changed |= refresh(project, depth, updateAliases, subMonitor.newChild(1));
		}
		return changed;
	}

	/**
	 * Returns the resource corresponding to the given workspace path.  The
	 * "files" parameter is used for paths of two or more segments.  If true,
	 * a file is returned, otherwise a folder is returned.  Returns null if files is true
	 * and the path is not of sufficient length.
	 */
	protected IResource resourceFor(IPath path, boolean files) {
		int numSegments = path.segmentCount();
		if (files && numSegments < ICoreConstants.MINIMUM_FILE_SEGMENT_LENGTH)
			return null;
		IWorkspaceRoot root = getWorkspace().getRoot();
		if (path.isRoot())
			return root;
		if (numSegments == 1)
			return root.getProject(path.segment(0));
		return files ? (IResource) root.getFile(path) : (IResource) root.getFolder(path);
	}

	/* (non-javadoc)
	 * @see IResouce.setLocalTimeStamp
	 */
	public long setLocalTimeStamp(IResource target, ResourceInfo info, long value) throws CoreException {
		IFileStore store = getStore(target);
		IFileInfo fileInfo = store.fetchInfo();
		fileInfo.setLastModified(value);
		store.putInfo(fileInfo, EFS.SET_LAST_MODIFIED, null);
		//actual value may be different depending on file system granularity
		fileInfo = store.fetchInfo();
		long actualValue = fileInfo.getLastModified();
		updateLocalSync(info, actualValue);
		return actualValue;
	}

	/**
	 * The storage location for a resource has changed; update the location.
	 *
	 * @param target   the changed resource
	 * @param info     the resource info to update
	 * @param location the new storage location
	 */
	public void setLocation(IResource target, ResourceInfo info, URI location) {
		// Normalize case as it exists on the file system.
		setNormalizedLocation(target, info, FileUtil.realURI(location));
	}

	public void setNormalizedLocation(IResource target, ResourceInfo info, URI location) {
		FileStoreRoot oldRoot = info.getFileStoreRoot();
		if (location != null) {
			info.setFileStoreRoot(new FileStoreRoot(location, target.getFullPath()));
		} else {
			//project is in default location so clear the store root
			info.setFileStoreRoot(null);
		}
		if (oldRoot != null)
			oldRoot.setValid(false);
	}

	/* (non-javadoc)
	 * @see IResource.setResourceAttributes
	 */
	public void setResourceAttributes(IResource resource, ResourceAttributes attributes) throws CoreException {
		IFileStore store = getStore(resource);
		//when the executable bit is changed on a folder a refresh is required
		boolean refresh = false;
		if (resource instanceof IContainer && ((store.getFileSystem().attributes() & EFS.ATTRIBUTE_EXECUTABLE) != 0))
			refresh = store.fetchInfo().getAttribute(EFS.ATTRIBUTE_EXECUTABLE) != attributes.isExecutable();
		store.putInfo(FileUtil.attributesToFileInfo(attributes), EFS.SET_ATTRIBUTES, null);
		//must refresh in the background because we are not inside an operation
		if (refresh)
			workspace.getRefreshManager().refresh(resource);
	}

	@Override
	public void shutdown(IProgressMonitor monitor) throws CoreException {
		if (_historyStore != null)
			_historyStore.shutdown(monitor);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES)
				.removePreferenceChangeListener(lightweightAutoRefreshPrefListener);
	}

	@Override
	public void startup(IProgressMonitor monitor) {
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES)
				.addPreferenceChangeListener(lightweightAutoRefreshPrefListener);
		lightweightAutoRefreshEnabled = Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES,
				ResourcesPlugin.PREF_LIGHTWEIGHT_AUTO_REFRESH, false, null);
	}

	/**
	 * The ResourceInfo must be mutable.
	 */
	public void updateLocalSync(ResourceInfo info, long localSyncInfo) {
		info.setLocalSyncInfo(localSyncInfo);
		if (localSyncInfo == I_NULL_SYNC_INFO)
			info.clear(M_LOCAL_EXISTS);
		else
			info.set(M_LOCAL_EXISTS);
	}

	/**
	 * The target must exist in the workspace and must remain existing throughout
	 * the execution of this method. The {@code content} {@link InputStream} is
	 * closed even if the method fails. If the {@link IResource#FORCE} flag is not
	 * set in {@code updateFlags}, we only write the file if it does not exist or if
	 * it is already local and the timestamp has <b>not</b> changed since last
	 * synchronization, otherwise a {@link CoreException} is thrown.
	 *
	 * @param target      the file to write to
	 * @param content     a stream with the contents to write to {@code target}
	 * @param fileInfo    the info object for the {@code target} file
	 * @param updateFlags update flags as defined in {@link IResource}
	 * @param append      whether the {@code content} stream shall be appended to
	 *                    the existing contents of {@code target}
	 * @param monitor     the progress monitor to report to
	 *
	 * @throws CoreException in any of the following cases:
	 *                       <ul>
	 *                       <li>the given {@code target} does not exist or was
	 *                       removed from the workspace concurrently
	 *                       <li>writing the stream to {@code target} fails
	 *                       <li>the {@link IResource#FORCE} flag is set in
	 *                       {@code updateFlags}, {@code append} is {@code true},
	 *                       and the file is not local or does not exist</li>
	 *                       <li>the {@link IResource#FORCE} flag is not set in
	 *                       {@code updateFlags} and
	 *                       <ul>
	 *                       <li>{@code target} is local and has been modified since
	 *                       last synchronization</li>
	 *                       <li>{@code target} is not local but exists or
	 *                       {@code append} is {code true}</li>
	 *                       </ul>
	 *                       </ul>
	 *
	 * @see IResource#FORCE
	 * @see IResource#KEEP_HISTORY
	 */
	public void write(IFile target, InputStream content, IFileInfo fileInfo, int updateFlags, boolean append, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 4);
		try (content) {
			Resource targetResource = (Resource) target;
			IFileStore store = getStore(target);
			if (fileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
				String message = NLS.bind(Messages.localstore_couldNotWriteReadOnly, target.getFullPath());
				throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, target.getFullPath(), message, null);
			}
			long lastModified = fileInfo.getLastModified();
			ResourceInfo immutableTargetResourceInfo = targetResource.getResourceInfo(true, false);
			if (immutableTargetResourceInfo == null) {
				// If the resource info is null, the resource does not exist in the workspace.
				// This violates the method contract, so throw an according exception.
				targetResource.checkExists(targetResource.getFlags(immutableTargetResourceInfo), true);
			}
			if (BitMask.isSet(updateFlags, IResource.FORCE)) {
				if (append && !target.isLocal(IResource.DEPTH_ZERO) && !fileInfo.exists()) {
					// force=true, local=false, existsInFileSystem=false
					String message = NLS.bind(Messages.resources_mustBeLocal, target.getFullPath());
					throw new ResourceException(IResourceStatus.RESOURCE_NOT_LOCAL, target.getFullPath(), message, null);
				}
			} else {
				if (target.isLocal(IResource.DEPTH_ZERO)) {
					// test if timestamp is the same since last synchronization
					if (lastModified != immutableTargetResourceInfo.getLocalSyncInfo()) {
						asyncRefresh(target);
						String message = NLS.bind(Messages.localstore_resourceIsOutOfSync, target.getFullPath());
						throw new ResourceException(IResourceStatus.OUT_OF_SYNC_LOCAL, target.getFullPath(), message, null);
					}
					if (!fileInfo.exists()) {
						asyncRefresh(target);
						String message = NLS.bind(Messages.localstore_resourceDoesNotExist, target.getFullPath());
						throw new ResourceException(IResourceStatus.NOT_FOUND_LOCAL, target.getFullPath(), message, null);
					}
				} else {
					if (fileInfo.exists()) {
						String message = NLS.bind(Messages.localstore_resourceExists, target.getFullPath());
						throw new ResourceException(IResourceStatus.EXISTS_LOCAL, target.getFullPath(), message, null);
					}
					if (append) {
						String message = NLS.bind(Messages.resources_mustBeLocal, target.getFullPath());
						throw new ResourceException(IResourceStatus.RESOURCE_NOT_LOCAL, target.getFullPath(), message, null);
					}
				}
			}
			// add entry to History Store.
			if (BitMask.isSet(updateFlags, IResource.KEEP_HISTORY) && fileInfo.exists()
					&& FileSystemResourceManager.storeHistory(target))
				//never move to the history store, because then the file is missing if write fails
				getHistoryStore().addState(target.getFullPath(), store, fileInfo, false);
			if (!fileInfo.exists()) {
				IFileStore parent = store.getParent();
				IFileInfo parentInfo = parent.fetchInfo();
				if (!parentInfo.exists()) {
					parent.mkdir(EFS.NONE, null);
				}
			}

			// On Windows an attempt to open an output stream on a hidden file results in FileNotFoundException.
			// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=194216
			boolean restoreHiddenAttribute = false;
			if (fileInfo.exists() && fileInfo.getAttribute(EFS.ATTRIBUTE_HIDDEN) && Platform.getOS().equals(Platform.OS_WIN32)) {
				fileInfo.setAttribute(EFS.ATTRIBUTE_HIDDEN, false);
				store.putInfo(fileInfo, EFS.SET_ATTRIBUTES, subMonitor.split(1));
				restoreHiddenAttribute = true;
			} else {
				subMonitor.split(1);
			}
			int options = append ? EFS.APPEND : EFS.NONE;
			try (OutputStream out = store.openOutputStream(options, subMonitor.split(1))) {
				if (restoreHiddenAttribute) {
					fileInfo.setAttribute(EFS.ATTRIBUTE_HIDDEN, true);
					store.putInfo(fileInfo, EFS.SET_ATTRIBUTES, subMonitor.split(1));
				} else {
					subMonitor.split(1);
				}
				FileUtil.transferStreams(content, out, store.toString(), subMonitor.split(1));
			} catch (IOException e) {
				String msg = NLS.bind(Messages.localstore_couldNotWrite, store.toString());
				throw new ResourceException(IResourceStatus.FAILED_WRITE_LOCAL, IPath.fromOSString(store.toString()), msg, e);
			}
			// get the new last modified time and stash in the info
			lastModified = store.fetchInfo().getLastModified();
			ResourceInfo mutableTargetResourceInfo = targetResource.getResourceInfo(false, true);
			if (mutableTargetResourceInfo == null) {
				// If the resource info is null, the resource must have been concurrently
				// removed from the workspace. This violates the method contract, so throw an
				// according exception.
				targetResource.checkExists(targetResource.getFlags(mutableTargetResourceInfo), true);
			}
			updateLocalSync(mutableTargetResourceInfo, lastModified);
			mutableTargetResourceInfo.incrementContentId();
			mutableTargetResourceInfo.clear(M_CONTENT_CACHE);
			workspace.updateModificationStamp(mutableTargetResourceInfo);
		} catch (IOException streamCloseIgnored) {
			// ignore;
		}
	}

	/**
	 * If force is false, this method fails if there is already a resource in
	 * target's location.
	 */
	public void write(IFolder target, boolean force, IProgressMonitor monitor) throws CoreException {
		IFileStore store = getStore(target);
		if (!force) {
			IFileInfo fileInfo = store.fetchInfo();
			if (fileInfo.isDirectory()) {
				String message = NLS.bind(Messages.localstore_resourceExists, target.getFullPath());
				throw new ResourceException(IResourceStatus.EXISTS_LOCAL, target.getFullPath(), message, null);
			}
			if (fileInfo.exists()) {
				String message = NLS.bind(Messages.localstore_fileExists, target.getFullPath());
				throw new ResourceException(IResourceStatus.OUT_OF_SYNC_LOCAL, target.getFullPath(), message, null);
			}
		}
		store.mkdir(EFS.NONE, monitor);
		ResourceInfo info = ((Resource) target).getResourceInfo(false, true);
		updateLocalSync(info, store.fetchInfo().getLastModified());
	}

	/**
	 * Write the .project file without modifying the resource tree.  This is called
	 * during save when it is discovered that the .project file is missing.  The tree
	 * cannot be modified during save.
	 */
	public void writeSilently(IProject target) throws CoreException {
		IPath location = locationFor(target, false);
		//if the project location cannot be resolved, we don't know if a description file exists or not
		if (location == null)
			return;
		IFileStore projectStore = getStore(target);
		projectStore.mkdir(EFS.NONE, null);
		//can't do anything if there's no description
		IProjectDescription desc = ((Project) target).internalGetDescription();
		if (desc == null)
			return;
		//write the project's private description to the meta-data area
		getWorkspace().getMetaArea().writePrivateDescription(target);

		//write the file that represents the project description
		IFileStore fileStore = projectStore.getChild(IProjectDescription.DESCRIPTION_FILE_NAME);
		try (
			OutputStream out = fileStore.openOutputStream(EFS.NONE, null)
		) {
			IFile file = target.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
			new ModelObjectWriter().write(desc, out, file.getLineSeparator(true));
		} catch (IOException e) {
			String msg = NLS.bind(Messages.resources_writeMeta, target.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, target.getFullPath(), msg, e);
		}
		//for backwards compatibility, ensure the old .prj file is deleted
		getWorkspace().getMetaArea().clearOldDescription(target);
	}

	public static boolean storeHistory(IResource file) {
		WorkspaceDescription description = ((Workspace) file.getWorkspace()).internalGetDescription();
		return description.isKeepDerivedState() || !file.isDerived();
	}

}
