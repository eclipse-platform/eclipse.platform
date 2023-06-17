/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Francis Lynch (Wind River) - [301563] Save and load tree snapshots
 *     Broadcom Corporation - ongoing development
 *     Sergey Prigogin (Google) - [437005] Out-of-date .snap file prevents Eclipse from running
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.localstore.SafeChunkyOutputStream;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

public class LocalMetaArea implements ICoreConstants {
	/* package */static final String F_BACKUP_FILE_EXTENSION = ".bak"; //$NON-NLS-1$
	/* package */static final String F_DESCRIPTION = ".workspace"; //$NON-NLS-1$

	/* package */static final String F_HISTORY_STORE = ".history"; //$NON-NLS-1$
	/* package */static final String F_MARKERS = ".markers"; //$NON-NLS-1$
	/* package */static final String F_OLD_PROJECT = ".prj"; //$NON-NLS-1$
	/* package */static final String F_PROJECT_LOCATION = ".location"; //$NON-NLS-1$
	/* package */static final String F_PROJECTS = ".projects"; //$NON-NLS-1$
	/* package */static final String F_PROPERTIES = ".properties"; //$NON-NLS-1$
	/* package */static final String F_REFRESH = ".refresh"; //$NON-NLS-1$
	/* package */static final String F_ROOT = ".root"; //$NON-NLS-1$
	/* package */static final String F_SAFE_TABLE = ".safetable"; //$NON-NLS-1$
	/* package */static final String F_SNAP = ".snap"; //$NON-NLS-1$
	/* package */static final String F_SNAP_EXTENSION = "snap"; //$NON-NLS-1$
	/* package */static final String F_SYNCINFO = ".syncinfo"; //$NON-NLS-1$
	/* package */static final String F_TREE = ".tree"; //$NON-NLS-1$
	/* package */static final String URI_PREFIX = "URI//"; //$NON-NLS-1$
	/* package */static final String F_METADATA = ".metadata"; //$NON-NLS-1$

	protected final IPath metaAreaLocation;

	/**
	 * The project location is just stored as an optimization, to avoid recomputing it.
	 */
	protected final IPath projectMetaLocation;
	private Workspace workspace;

	public LocalMetaArea(Workspace workspace) {
		this.workspace = workspace;
		metaAreaLocation = ResourcesPlugin.getPlugin().getStateLocation();
		projectMetaLocation = metaAreaLocation.append(F_PROJECTS);
	}

	/**
	 * For backwards compatibility, if there is a project at the old project
	 * description location, delete it.
	 */
	public void clearOldDescription(IProject target) {
		Workspace.clear(getOldDescriptionLocationFor(target).toFile());
	}

	/**
	 * Delete the refresh snapshot once it has been used to open a new project.
	 */
	public void clearRefresh(IProject target) {
		Workspace.clear(getRefreshLocationFor(target).toFile());
	}

	public void create(IProject target) {
		java.io.File file = locationFor(target).toFile();
		//make sure area is empty
		Workspace.clear(file);
		file.mkdirs();
	}

	/**
	 * Creates the meta area root directory.
	 */
	public synchronized void createMetaArea() throws CoreException {
		java.io.File workspaceLocation = metaAreaLocation.toFile();
		Workspace.clear(workspaceLocation);
		if (!workspaceLocation.mkdirs()) {
			String message = NLS.bind(Messages.resources_writeWorkspaceMeta, workspaceLocation);
			throw new ResourceException(IResourceStatus.FAILED_WRITE_METADATA, null, message, null);
		}
	}

	/**
	 * The project is being deleted. Delete all meta-data associated with the
	 * project.
	 */
	public void delete(IProject target) throws CoreException {
		IPath path = locationFor(target);
		if (!Workspace.clear(path.toFile()) && path.toFile().exists()) {
			String message = NLS.bind(Messages.resources_deleteMeta, target.getFullPath());
			throw new ResourceException(IResourceStatus.FAILED_DELETE_METADATA, target.getFullPath(), message, null);
		}
	}

	public IPath getBackupLocationFor(IPath file) {
		return file.removeLastSegments(1).append(file.lastSegment() + F_BACKUP_FILE_EXTENSION);
	}

	public IPath getHistoryStoreLocation() {
		return metaAreaLocation.append(F_HISTORY_STORE);
	}

	/**
	 * Returns the local file system location which contains the META data for
	 * the resources plugin (i.e., the entire workspace).
	 */
	public IPath getLocation() {
		return metaAreaLocation;
	}

	/**
	 * Returns the path of the file in which to save markers for the given
	 * resource. Should only be called for the workspace root and projects.
	 */
	public IPath getMarkersLocationFor(IResource resource) {
		Assert.isNotNull(resource);
		Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
		return locationFor(resource).append(F_MARKERS);
	}

	/**
	 * Returns the path of the file in which to snapshot markers for the given
	 * resource. Should only be called for the workspace root and projects.
	 */
	public IPath getMarkersSnapshotLocationFor(IResource resource) {
		return getMarkersLocationFor(resource).addFileExtension(F_SNAP_EXTENSION);
	}

	/**
	 * The project description file is the only metadata file stored outside
	 * the metadata area. It is stored as a file directly under the project
	 * location. For backwards compatibility, we also have to check for a
	 * project file at the old location in the metadata area.
	 */
	public IPath getOldDescriptionLocationFor(IProject target) {
		return locationFor(target).append(F_OLD_PROJECT);
	}

	public IPath getPropertyStoreLocation(IResource resource) {
		int type = resource.getType();
		Assert.isTrue(type != IResource.FILE && type != IResource.FOLDER);
		return locationFor(resource).append(F_PROPERTIES);
	}

	/**
	 * Returns the path of the file in which to save the refresh snapshot for
	 * the given project.
	 */
	public IPath getRefreshLocationFor(IProject project) {
		Assert.isNotNull(project);
		return locationFor(project).append(F_REFRESH);
	}

	public IPath getSafeTableLocationFor(String pluginId) {
		IPath prefix = metaAreaLocation.append(F_SAFE_TABLE);
		// if the plugin is the resources plugin, we return the master table
		// location
		if (pluginId.equals(ResourcesPlugin.PI_RESOURCES))
			return prefix.append(pluginId); // master table
		int saveNumber = getWorkspace().getSaveManager().getSaveNumber(pluginId);
		return prefix.append(pluginId + "." + saveNumber); //$NON-NLS-1$
	}

	/**
	 * Returns the path of the snapshot file. The name of the file is composed from a sequence
	 * number corresponding to the sequence number of tree file and ".snap" extension. Should
	 * only be called for the workspace root.
	 */
	public IPath getSnapshotLocationFor(IResource resource) {
		Assert.isNotNull(resource);
		Assert.isLegal(resource.getType() == IResource.ROOT);
		IPath key = resource.getFullPath().append(F_TREE);
		String sequenceNumber = getWorkspace().getSaveManager().getMasterTable().getProperty(key.toString());
		if (sequenceNumber == null)
			sequenceNumber = "0"; //$NON-NLS-1$
		return metaAreaLocation.append(sequenceNumber + F_SNAP);
	}

	/**
	 * Returns the legacy, pre-4.4.1, path of the snapshot file. The name of the legacy snapshot
	 * file is ".snap". Should only be called for the workspace root.
	 */
	public IPath getLegacySnapshotLocationFor(IResource resource) {
		Assert.isNotNull(resource);
		Assert.isLegal(resource.getType() == IResource.ROOT);
		return metaAreaLocation.append(F_SNAP);
	}

	/**
	 * Returns the path of the file in which to save the sync information for
	 * the given resource. Should only be called for the workspace root and
	 * projects.
	 */
	public IPath getSyncInfoLocationFor(IResource resource) {
		Assert.isNotNull(resource);
		Assert.isLegal(resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT);
		return locationFor(resource).append(F_SYNCINFO);
	}

	/**
	 * Returns the path of the file in which to snapshot the sync information
	 * for the given resource. Should only be called for the workspace root and
	 * projects.
	 */
	public IPath getSyncInfoSnapshotLocationFor(IResource resource) {
		return getSyncInfoLocationFor(resource).addFileExtension(F_SNAP_EXTENSION);
	}

	/**
	 * Returns the local file system location of the tree file for the given
	 * resource. This file does not follow the same save number as its plug-in.
	 * So, the number here is called "sequence number" and not "save number" to
	 * avoid confusion.
	 */
	public IPath getTreeLocationFor(IResource target, boolean updateSequenceNumber) {
		IPath key = target.getFullPath().append(F_TREE);
		String sequenceNumber = getWorkspace().getSaveManager().getMasterTable().getProperty(key.toString());
		if (sequenceNumber == null)
			sequenceNumber = "0"; //$NON-NLS-1$
		if (updateSequenceNumber) {
			int n = Integer.parseInt(sequenceNumber) + 1;
			n = n < 0 ? 1 : n;
			sequenceNumber = Integer.toString(n);
			getWorkspace().getSaveManager().getMasterTable().setProperty(key.toString(), sequenceNumber);
		}
		return locationFor(target).append(sequenceNumber + F_TREE);
	}

	public IPath getWorkingLocation(IResource resource, String id) {
		return locationFor(resource).append(id);
	}

	protected Workspace getWorkspace() {
		return workspace;
	}

	public boolean hasSavedProject(IProject project) {
		//if there is a location file, then the project exists
		return getOldDescriptionLocationFor(project).toFile().exists() || locationFor(project).append(F_PROJECT_LOCATION).toFile().exists();
	}

	public boolean hasSavedWorkspace() {
		return metaAreaLocation.toFile().exists() || getBackupLocationFor(metaAreaLocation).toFile().exists();
	}

	public boolean hasSavedProjects() {
		return projectMetaLocation.toFile().exists();
	}

	/**
	 * Returns the local file system location in which the meta data for the
	 * resource with the given path is stored.
	 */
	public IPath locationFor(IPath resourcePath) {
		if (IPath.ROOT.equals(resourcePath))
			return metaAreaLocation.append(F_ROOT);
		return projectMetaLocation.append(resourcePath.segment(0));
	}

	/**
	 * Returns the local file system location in which the meta data for the
	 * given resource is stored.
	 */
	public IPath locationFor(IResource resource) {
		if (resource.getType() == IResource.ROOT)
			return metaAreaLocation.append(F_ROOT);
		return projectMetaLocation.append(resource.getProject().getName());
	}

	/**
	 * Reads and returns the project description for the given project. Returns
	 * null if there was no project description file on disk. Throws an
	 * exception if there was any failure to read the project.
	 */
	public ProjectDescription readOldDescription(IProject project) throws CoreException {
		IPath path = getOldDescriptionLocationFor(project);
		if (!path.toFile().exists())
			return null;
		IPath tempPath = getBackupLocationFor(path);
		ProjectDescription description = null;
		try {
			description = new ProjectDescriptionReader(project).read(path, tempPath);
		} catch (IOException e) {
			String msg = NLS.bind(Messages.resources_readMeta, project.getName());
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, project.getFullPath(), msg, e);
		}
		if (description == null) {
			String msg = NLS.bind(Messages.resources_readMeta, project.getName());
			throw new ResourceException(IResourceStatus.FAILED_READ_METADATA, project.getFullPath(), msg, null);
		}
		return description;
	}

	/**
	 * Returns the portions of the project description that are private, and
	 * adds them to the supplied project description. In particular, the
	 * project location, the project's dynamic references and build configurations
	 * are stored here.
	 * The project location will be set to <code>null</code> if the default
	 * location should be used. In the case of failure, log the exception and
	 * return silently, thus reverting to using the default location and no
	 * dynamic references. The current format of the location file is:
	 *    UTF - project location
	 *    int - number of dynamic project references
	 *    UTF - project reference 1
	 *    ... repeat for remaining references
	 * since 3.7:
	 *    int - number of build configurations
	 *      UTF - configuration name in order
	 *      ... repeated for N configurations
	 *    UTF - active build configuration name
	 *    int - number of build configurations with refs
	 *      UTF - build configuration name
	 *      int - number of referenced configuration
	 *        UTF - project name
	 *        bool - hasConfigName
	 *        UTF - configName if hasConfigName
	 *        ... repeat for number of referenced configurations
	 *      ... repeat for number of build configurations with references
	 */
	public void readPrivateDescription(IProject target, ProjectDescription description) {
		IPath locationFile = locationFor(target).append(F_PROJECT_LOCATION);
		java.io.File file = locationFile.toFile();
		if (!file.exists()) {
			locationFile = getBackupLocationFor(locationFile);
			file = locationFile.toFile();
			if (!file.exists())
				return;
		}
		try (DataInputStream dataIn = new DataInputStream(new SafeChunkyInputStream(file, 500))) {
			try {
				String location = dataIn.readUTF();
				if (location.length() > 0) {
					//location format < 3.2 was a local file system OS path
					//location format >= 3.2 is: URI_PREFIX + uri.toString()
					if (location.startsWith(URI_PREFIX))
						description.setLocationURI(URI.create(location.substring(URI_PREFIX.length())));
					else
						description.setLocationURI(URIUtil.toURI(IPath.fromOSString(location)));
				}
			} catch (Exception e) {
				//don't allow failure to read the location to propagate
				String msg = NLS.bind(Messages.resources_exReadProjectLocation, target.getName());
				Policy.log(new ResourceStatus(IStatus.ERROR, IResourceStatus.FAILED_READ_METADATA, target.getFullPath(), msg, e));
			}
			//try to read the dynamic references - will fail for old location files
			int numRefs = dataIn.readInt();
			IProject[] references = new IProject[numRefs];
			IWorkspaceRoot root = getWorkspace().getRoot();
			for (int i = 0; i < numRefs; i++)
				references[i] = root.getProject(dataIn.readUTF());
			description.setDynamicReferences(references);

			// Since 3.7 -  Build Configurations
			String[] configs = new String[dataIn.readInt()];
			for (int i = 0; i < configs.length; i++)
				configs[i] = dataIn.readUTF();
			if (configs.length > 0)
				// In the future we may decide this is better stored in the
				// .project, so only set if configs.length > 0
				description.setBuildConfigs(configs);
			// Active configuration name
			description.setActiveBuildConfig(dataIn.readUTF());
			// Build configuration references?
			int numBuildConifgsWithRefs = dataIn.readInt();
			HashMap<String, IBuildConfiguration[]> m = new HashMap<>(numBuildConifgsWithRefs);
			for (int i = 0; i < numBuildConifgsWithRefs; i++) {
				String configName = dataIn.readUTF();
				numRefs = dataIn.readInt();
				IBuildConfiguration[] refs = new IBuildConfiguration[numRefs];
				for (int j = 0; j < numRefs; j++) {
					String projName = dataIn.readUTF();
					if (dataIn.readBoolean())
						refs[j] = new BuildConfiguration(root.getProject(projName), dataIn.readUTF());
					else
						refs[j] = new BuildConfiguration(root.getProject(projName), null);
				}
				m.put(configName, refs);
			}
			description.setBuildConfigReferences(m);
		} catch (IOException e) {
			//ignore - this is an old location file or an exception occurred
			// closing the stream
		}
	}

	/**
	 * Write the private project description information, including the location
	 * and the dynamic project references.  See <tt>readPrivateDescription</tt>
	 * for details on the file format.
	 */
	public void writePrivateDescription(IProject target) throws CoreException {
		IPath location = locationFor(target).append(F_PROJECT_LOCATION);
		java.io.File file = location.toFile();
		//delete any old location file
		Workspace.clear(file);
		//don't write anything if there is no interesting private metadata
		ProjectDescription desc = ((Project) target).internalGetDescription();
		if (desc == null)
			return;
		final URI projectLocation = desc.getLocationURI();
		final IProject[] prjRefs = desc.getDynamicReferences(false);
		final String[] buildConfigs = desc.configNames;
		final Map<String, IBuildConfiguration[]> configRefs = desc.getBuildConfigReferences(false);
		if (projectLocation == null && prjRefs.length == 0 && buildConfigs.length == 0 && configRefs.isEmpty())
			return;
		//write the private metadata file
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(file); DataOutputStream dataOut = new DataOutputStream(output);) {
			if (projectLocation == null)
				dataOut.writeUTF(""); //$NON-NLS-1$
			else
				dataOut.writeUTF(URI_PREFIX + projectLocation);
			dataOut.writeInt(prjRefs.length);
			for (IProject prjRef : prjRefs)
				dataOut.writeUTF(prjRef.getName());

			// Since 3.7 - build configurations + references
			// Write out the build configurations
			dataOut.writeInt(buildConfigs.length);
			for (String buildConfig : buildConfigs) {
				dataOut.writeUTF(buildConfig);
			}
			// Write active configuration name
			dataOut.writeUTF(desc.getActiveBuildConfig());
			// Write out the configuration level references
			dataOut.writeInt(configRefs.size());
			for (Map.Entry<String, IBuildConfiguration[]> e : configRefs.entrySet()) {
				String refdName = e.getKey();
				IBuildConfiguration[] refs = e.getValue();

				dataOut.writeUTF(refdName);
				dataOut.writeInt(refs.length);
				for (IBuildConfiguration ref : refs) {
					dataOut.writeUTF(ref.getProject().getName());
					if (ref.getName() == null) {
						dataOut.writeBoolean(false);
					} else {
						dataOut.writeBoolean(true);
						dataOut.writeUTF(ref.getName());
					}
				}
			}
			output.succeed();
		} catch (IOException e) {
			String message = NLS.bind(Messages.resources_exSaveProjectLocation, target.getName());
			throw new ResourceException(IResourceStatus.INTERNAL_ERROR, null, message, e);
		}
	}
}
