/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *     Sebastian Davids <sdavids@gmx.de> - bug 74959
 *     Maik Schreiber - bug 102461
 *     William Mitsuda (wmitsuda@gmail.com) - Bug 153879 [Wizards] configurable size of cvs commit comment history
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.repo;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkingSet;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is repsible for maintaining the UI's list of known repositories,
 * and a list of known tags within each of those repositories.
 * 
 * It also provides a number of useful methods for assisting in repository operations.
 */
public class RepositoryManager {
	// old state file
	private static final String STATE_FILE = ".repositoryManagerState"; //$NON-NLS-1$
	private static final int STATE_FILE_VERSION_1 = -1;
	// new state file
	private static final String REPOSITORIES_VIEW_FILE = "repositoriesView.xml"; //$NON-NLS-1$
	private static final String COMMENT_HIST_FILE = "commitCommentHistory.xml"; //$NON-NLS-1$
	private static final String COMMENT_TEMPLATES_FILE = "commentTemplates.xml"; //$NON-NLS-1$
	static final String ELEMENT_COMMIT_COMMENT = "CommitComment"; //$NON-NLS-1$
	static final String ELEMENT_COMMIT_HISTORY = "CommitComments"; //$NON-NLS-1$
	static final String ELEMENT_COMMENT_TEMPLATES = "CommitCommentTemplates"; //$NON-NLS-1$

	private Map<String, RepositoryRoot> repositoryRoots = new HashMap<>();
	
	List<IRepositoryListener> listeners = new ArrayList<>();

	// The previously remembered comment
	static String[] previousComments = new String[0];
	static String[] commentTemplates = new String[0];
	
	public static boolean notifyRepoView = true;
	
	// Cache of changed repository roots
	private int notificationLevel = 0;
	private Map<String, ICVSRepositoryLocation> changedRepositories = new HashMap<>();
	
	public static final int DEFAULT_MAX_COMMENTS = 10;
	
	private int maxComments = DEFAULT_MAX_COMMENTS;
	
	public void setMaxComments(int maxComments) {
		if (maxComments > 0) {
			this.maxComments = maxComments;
			if (maxComments < previousComments.length) {
				String[] newComments = new String[maxComments];
				System.arraycopy(previousComments, 0, newComments, 0, maxComments);
				previousComments = newComments;
			}
		}
	}
	
	/**
	 * Answer an array of all known remote roots.
	 */
	public ICVSRepositoryLocation[] getKnownRepositoryLocations() {
		return KnownRepositories.getInstance().getRepositories();
	}
	
	/**
	 * Method getRepositoryRoots.
	 * @param iCVSRepositoryLocations
	 * @return RepositoryRoot[]
	 */
	private RepositoryRoot[] getRepositoryRoots(ICVSRepositoryLocation[] locations) {
		List<RepositoryRoot> roots = new ArrayList<>();
		for (ICVSRepositoryLocation location : locations) {
			RepositoryRoot root = getRepositoryRootFor(location);
			if (root != null)
				roots.add(root);
		}
		return roots.toArray(new RepositoryRoot[roots.size()]);
	}
	
	public RepositoryRoot[] getKnownRepositoryRoots() {
		return getRepositoryRoots(getKnownRepositoryLocations());
	}
	
	/**
	 * Get the list of known branch tags for a given remote root.
	 */
	public CVSTag[] getKnownTags(ICVSFolder project, int tagType) {
		try {
			CVSTag[] tags = getKnownTags(project);
			Set<CVSTag> result = new HashSet<>();
			for (CVSTag tag : tags) {
				if (tag.getType() == tagType)
					result.add(tag);
			}

			return result.toArray(new CVSTag[result.size()]);
		} catch(CVSException e) {
			CVSUIPlugin.log(e);
			return new CVSTag[0];
		}
	}
	
	/**
	 * Get the list of known version tags for a given project.
	 */
	public CVSTag[] getKnownTags(ICVSRepositoryLocation location, int tagType) {
		Set<CVSTag> result = new HashSet<>();
		RepositoryRoot root = repositoryRoots.get(location.getLocation(false));
		if (root != null) {
			CVSTag[] tags = root.getAllKnownTags();
			for (CVSTag tag : tags) {
				if (tag.getType() == tagType)
					result.add(tag);
			}
		}
		return result.toArray(new CVSTag[0]);
	}
	
	/**
	 * Method getKnownTags.
	 * @param repository
	 * @param set
	 * @param i
	 * @param monitor
	 * @return CVSTag[]
	 */
	public CVSTag[] getKnownTags(ICVSRepositoryLocation repository, IWorkingSet set, int tagType, IProgressMonitor monitor) throws CVSException {
		if (set == null) {
			return getKnownTags(repository, tagType);
		}
		ICVSRemoteResource[] folders = getFoldersForTag(repository, CVSTag.DEFAULT, monitor);
		folders = filterResources(set, folders);
		Set<CVSTag> tags = new HashSet<>();
		for (ICVSRemoteResource f : folders) {
			ICVSRemoteFolder folder = (ICVSRemoteFolder) f;
			tags.addAll(Arrays.asList(getKnownTags(folder, tagType)));
		}
		return tags.toArray(new CVSTag[tags.size()]);
	}
	
	public CVSTag[] getKnownTags(ICVSFolder project) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = RepositoryRoot.getRemotePathFor(project);
		return root.getAllKnownTags(remotePath);
	}
	
	/*
	 * XXX I hope this methos is not needed in this form
	 */
	public Map getKnownProjectsAndVersions(ICVSRepositoryLocation location) {
		Map<String, Set<CVSTag>> knownTags = new HashMap<>();
		RepositoryRoot root = getRepositoryRootFor(location);
		String[] paths = root.getKnownRemotePaths();
		for (String path : paths) {
			Set<CVSTag> result = new HashSet<>();
			result.addAll(Arrays.asList(root.getAllKnownTags(path)));
			knownTags.put(path, result);
		}
		return knownTags;
	}
	
	public ICVSRemoteResource[] getFoldersForTag(ICVSRepositoryLocation location, CVSTag tag, IProgressMonitor monitor) throws CVSException {		
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(NLS.bind(CVSUIMessages.RepositoryManager_fetchingRemoteFolders, new String[] { tag.getName() }), 100); 
			if (tag.getType() == CVSTag.HEAD) {
				ICVSRemoteResource[] resources = location.members(tag, false, Policy.subMonitorFor(monitor, 60));
				RepositoryRoot root = getRepositoryRootFor(location);
				ICVSRemoteResource[] modules = root.getDefinedModules(tag, Policy.subMonitorFor(monitor, 40));
				ICVSRemoteResource[] result = new ICVSRemoteResource[resources.length + modules.length];
				System.arraycopy(resources, 0, result, 0, resources.length);
				System.arraycopy(modules, 0, result, resources.length, modules.length);
				return result;
			}
			if (tag.getType() == CVSTag.DATE) {
				ICVSRemoteResource[] resources = location.members(tag, false, Policy.subMonitorFor(monitor, 60));
				RepositoryRoot root = getRepositoryRootFor(location);
				ICVSRemoteResource[] modules = root.getDefinedModules(tag, Policy.subMonitorFor(monitor, 40));
				ICVSRemoteResource[] result = new ICVSRemoteResource[resources.length + modules.length];
				System.arraycopy(resources, 0, result, 0, resources.length);
				System.arraycopy(modules, 0, result, resources.length, modules.length);
				return result;
			}
			Set<ICVSRemoteFolder> result = new HashSet<>();
			// Get the tags for the location
			RepositoryRoot root = getRepositoryRootFor(location);
			String[] paths = root.getRemoteChildrenForTag(null, tag);
			for (String path : paths) {
				ICVSRemoteFolder remote = root.getRemoteFolder(path, tag,
						Policy.subMonitorFor(monitor, 100));
				result.add(remote);
			}
			return result.toArray(new ICVSRemoteResource[result.size()]);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Returns a list of child resources for given folder that are known to
	 * contain given tag. If the return list is empty than given tag exists
	 * directly in given folder and its children should be retrieved directly
	 * from the repository.
	 *
	 * NOTE: Resources are cached only for tags of type CVSTag.Branch and
	 * CVSTag.Version. Other types of tags will always return empty list.
	 *
	 * @param location
	 *            CVS repository location
	 * @param parentFolder
	 *            folder to check tags for
	 * @param tag
	 * @param monitor
	 * @return a list of remote resources that are known to contain given tag or
	 *         empty list if resources should be retrieved from the repository
	 * @throws CVSException
	 */
	public ICVSRemoteResource[] getCachedChildrenForTag(
			ICVSRepositoryLocation location, ICVSRemoteFolder parentFolder,
			CVSTag tag, IProgressMonitor monitor) throws CVSException {
		if (tag == null || tag.getType() == CVSTag.HEAD
				|| tag.getType() == CVSTag.DATE) {
			// folders are kept in cache only for tags and versions
			return new ICVSRemoteResource[0];
		}
		monitor = Policy.monitorFor(monitor);
		Set<ICVSRemoteFolder> result = new HashSet<>();
		RepositoryRoot root = getRepositoryRootFor(location);
		// if remote folder is null return the subfolders of repository root
		String[] paths = root.getRemoteChildrenForTag(
				parentFolder == null ? null : RepositoryRoot
						.getRemotePathFor(parentFolder), tag);
		monitor.beginTask(NLS
				.bind(CVSUIMessages.RemoteFolderElement_fetchingRemoteChildren,
						new String[] { NLS.bind(
								CVSUIMessages.RemoteFolderElement_nameAndTag,
								new String[] { parentFolder.getName(),
										tag.getName() }) }), 10 * paths.length);
		try {
			for (String path : paths) {
				ICVSRemoteFolder remote = root.getRemoteFolder(path, tag,
						Policy.subMonitorFor(monitor, 10));
				result.add(remote);
			}
			return result
					.toArray(new ICVSRemoteResource[result.size()]);
		} finally {
			monitor.done();
		}
	}
		
	/*
	 * Fetches tags from auto-refresh files if they exist. Then fetches tags from the user defined auto-refresh file
	 * list. The fetched tags are cached in the CVS ui plugin's tag cache.
	 */
	public CVSTag[] refreshDefinedTags(ICVSFolder folder, boolean recurse, boolean notify, IProgressMonitor monitor) throws TeamException {
		RepositoryRoot root = getRepositoryRootFor(folder);
		CVSTag[] tags = root.refreshDefinedTags(folder, recurse, monitor);
		if (tags.length > 0 && notify)
			broadcastRepositoryChange(root);
		return tags;
	}
	
	/**
	 * A repository root has been added. Notify any listeners.
	 */
	public void rootAdded(ICVSRepositoryLocation root) {
		for (IRepositoryListener listener : listeners) {
			listener.repositoryAdded(root);
		}
	}
	
	/**
	 * A repository root has been removed.
	 * Remove the tags defined for this root and notify any listeners
	 */
	public void rootRemoved(ICVSRepositoryLocation root) {
		RepositoryRoot repoRoot = repositoryRoots.remove(root.getLocation(false));
		if (repoRoot != null)
			broadcastRepositoryChange(repoRoot);
	}
	
	/**
	 * Accept tags for any CVS resource. However, for the time being,
	 * the given version tags are added to the list of known tags for the 
	 * remote ancestor of the resource that is a direct child of the remote root
	 */
	public void addTags(ICVSResource resource, CVSTag[] tags) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(resource);
		// XXX could be a file or folder
		String remotePath = RepositoryRoot.getRemotePathFor(resource);
		root.addTags(remotePath, tags);
		broadcastRepositoryChange(root);
	}
	public void addDateTag(ICVSRepositoryLocation location, CVSTag tag) {
		if(tag == null) return;
		RepositoryRoot root = getRepositoryRootFor(location);
		root.addDateTag(tag);
		broadcastRepositoryChange(root);
	}
	public CVSTag[] getDateTags(ICVSRepositoryLocation location) {
		RepositoryRoot root = getRepositoryRootFor(location);
		return root.getDateTags();
	}
	public void removeDateTag(ICVSRepositoryLocation location, CVSTag tag){
		RepositoryRoot root = getRepositoryRootFor(location);
		root.removeDateTag(tag);
		broadcastRepositoryChange(root);
	}
	public void setAutoRefreshFiles(ICVSFolder project, String[] filePaths) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = RepositoryRoot.getRemotePathFor(project);
		root.setAutoRefreshFiles(remotePath, filePaths);
	}
	
	public String[] getAutoRefreshFiles(ICVSFolder project) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = RepositoryRoot.getRemotePathFor(project);
		return root.getAutoRefreshFiles(remotePath);
	}
	
	/**
	 * Remove the given tags from the list of known tags for the
	 * given remote root.
	 */
	public void removeTags(ICVSFolder project, CVSTag[] tags) throws CVSException {
		RepositoryRoot root = getRepositoryRootFor(project);
		String remotePath = RepositoryRoot.getRemotePathFor(project);
		root.removeTags(remotePath, tags);
		broadcastRepositoryChange(root);
	}
	
	public void startup() {
		loadState();
		loadCommentHistory();
		loadCommentTemplates();
		CVSProviderPlugin.getPlugin().addRepositoryListener(new ICVSListener() {
			@Override
			public void repositoryAdded(ICVSRepositoryLocation root) {
				rootAdded(root);
			}
			@Override
			public void repositoryRemoved(ICVSRepositoryLocation root) {
				rootRemoved(root);
			}
		});
		
		IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		store.addPropertyChangeListener(event -> {
			if (event.getProperty().equals(ICVSUIConstants.PREF_COMMIT_COMMENTS_MAX_HISTORY)) {
				Object newValue = event.getNewValue();
				if (newValue instanceof String) {
					try {
						setMaxComments(Integer.parseInt((String) newValue));
					} catch (NumberFormatException e) {
						// fail silently
					}
				}
			}
		});
		setMaxComments(store.getInt(ICVSUIConstants.PREF_COMMIT_COMMENTS_MAX_HISTORY));
	}
	
	public void shutdown() throws TeamException {
		saveState();
		saveCommentHistory();
		saveCommentTemplates();
	}
	
	private void loadState() {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(REPOSITORIES_VIEW_FILE);
		File file = pluginStateLocation.toFile();
		if (file.exists()) {
			try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
				readState(is);
			} catch (IOException e) {
				CVSUIPlugin.log(IStatus.ERROR, CVSUIMessages.RepositoryManager_ioException, e); 
			} catch (TeamException e) {
				CVSUIPlugin.log(e);
			}
		} else {
			IPath oldPluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(STATE_FILE);
			file = oldPluginStateLocation.toFile();
			if (file.exists()) {
				try {
					try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
						readOldState(dis);
					}
					saveState();
					file.delete();
				} catch (IOException e) {
					CVSUIPlugin.log(IStatus.ERROR, CVSUIMessages.RepositoryManager_ioException, e); 
				} catch (TeamException e) {
					CVSUIPlugin.log(e);
				}
			} 
		}
	}
	private void loadCommentHistory() {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(COMMENT_HIST_FILE);
		File file = pluginStateLocation.toFile();
		if (!file.exists()) return;
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
			readCommentHistory(is);
		} catch (IOException e) {
			CVSUIPlugin.log(IStatus.ERROR, CVSUIMessages.RepositoryManager_ioException, e); 
		} catch (TeamException e) {
			CVSUIPlugin.log(e);
		}
	}
	private void loadCommentTemplates() {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation().append(COMMENT_TEMPLATES_FILE);
		File file = pluginStateLocation.toFile();
		if (!file.exists()) return;
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
			readCommentTemplates(is);
		} catch (IOException e) {
			CVSUIPlugin.log(IStatus.ERROR, CVSUIMessages.RepositoryManager_ioException, e);
		} catch (TeamException e) {
			CVSUIPlugin.log(e);
		}
	}
	
	protected void saveState() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File stateFile = pluginStateLocation.append(REPOSITORIES_VIEW_FILE).toFile();
		try {
			try (XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
				writeState(writer);
			}
			if (stateFile.exists()) {
				stateFile.delete();
			}
			boolean renamed = tempFile.renameTo(stateFile);
			if (!renamed) {
				throw new TeamException(new Status(IStatus.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, NLS.bind(CVSUIMessages.RepositoryManager_rename, new String[] { tempFile.getAbsolutePath() }), null)); 
			}
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, NLS.bind(CVSUIMessages.RepositoryManager_save, new String[] { stateFile.getAbsolutePath() }), e)); 
		}
	}
	private void writeState(XMLWriter writer) {
		writer.startTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG, null, true);
		// Write the repositories
		Collection repos = Arrays.asList(getKnownRepositoryLocations());
		Iterator it = repos.iterator();
		while (it.hasNext()) {
			CVSRepositoryLocation location = (CVSRepositoryLocation)it.next();
			RepositoryRoot root = getRepositoryRootFor(location);
			root.writeState(writer);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORIES_VIEW_TAG);
	}
	
	private void readState(InputStream stream) throws IOException, TeamException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(new InputSource(stream), new RepositoriesViewContentHandler(this));
		} catch (SAXException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSUIMessages.RepositoryManager_parsingProblem, new String[] { REPOSITORIES_VIEW_FILE }), ex);
			throw new CVSException(status); 
		} catch (ParserConfigurationException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSUIMessages.RepositoryManager_parsingProblem, new String[] { REPOSITORIES_VIEW_FILE }), ex);
			throw new CVSException(status); 
		}
	}
	
	private void readCommentHistory(InputStream stream) throws IOException, TeamException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(new InputSource(stream), new CommentHistoryContentHandler());
		} catch (SAXException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSUIMessages.RepositoryManager_parsingProblem, new String[] { COMMENT_HIST_FILE }), ex);
			throw new CVSException(status); 
		} catch (ParserConfigurationException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSUIMessages.RepositoryManager_parsingProblem, new String[] { COMMENT_HIST_FILE }), ex);
			throw new CVSException(status);  
		}
	}
	
	private void readOldState(DataInputStream dis) throws IOException, TeamException {
		int repoSize = dis.readInt();
		boolean version1 = false;
		if (repoSize == STATE_FILE_VERSION_1) {
			version1 = true;
			repoSize = dis.readInt();
		}
		for (int i = 0; i < repoSize; i++) {
			ICVSRepositoryLocation root = KnownRepositories.getInstance().getRepository(dis.readUTF());
			RepositoryRoot repoRoot = getRepositoryRootFor(root);
			
			// read branch tags associated with this root
			int tagsSize = dis.readInt();
			CVSTag[] branchTags = new CVSTag[tagsSize];
			for (int j = 0; j < tagsSize; j++) {
				String tagName = dis.readUTF();
				int tagType = dis.readInt();
				branchTags[j] = new CVSTag(tagName, tagType);
			}
			// Ignore the branch tags since they are handled differently now
			// addBranchTags(root, branchTags);
			
			// read the number of projects for this root that have version tags
			int projSize = dis.readInt();
			if (projSize > 0) {
				for (int j = 0; j < projSize; j++) {
					String name = dis.readUTF();
					Set<CVSTag> tagSet = new HashSet<>();
					int numTags = dis.readInt();
					for (int k = 0; k < numTags; k++) {
						tagSet.add(new CVSTag(dis.readUTF(), CVSTag.VERSION));
					}
					CVSTag[] tags = tagSet.toArray(new CVSTag[tagSet.size()]);
					repoRoot.addTags(name, tags);
				}
			}
			// read the auto refresh filenames for this project
			if (version1) {
				try {
					projSize = dis.readInt();
					if (projSize > 0) {
						for (int j = 0; j < projSize; j++) {
							String name = dis.readUTF();
							Set<String> filenames = new HashSet<>();
							int numFilenames = dis.readInt();
							for (int k = 0; k < numFilenames; k++) {
								filenames.add(name + "/" + dis.readUTF()); //$NON-NLS-1$
							}
							repoRoot.setAutoRefreshFiles(name, filenames.toArray(new String[filenames.size()]));
						}
					}
				} catch (EOFException e) {
					// auto refresh files are not persisted, continue and save them next time.
				}
			}
			broadcastRepositoryChange(repoRoot);
		}
	}
	
	protected void saveCommentHistory() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(COMMENT_HIST_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File histFile = pluginStateLocation.append(COMMENT_HIST_FILE).toFile();
		try {
			try (XMLWriter writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
				writeCommentHistory(writer);
			}
			if (histFile.exists()) {
				histFile.delete();
			}
			boolean renamed = tempFile.renameTo(histFile);
			if (!renamed) {
				throw new TeamException(new Status(IStatus.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, NLS.bind(CVSUIMessages.RepositoryManager_rename, new String[] { tempFile.getAbsolutePath() }), null)); 
			}
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, CVSUIPlugin.ID, TeamException.UNABLE, NLS.bind(CVSUIMessages.RepositoryManager_save, new String[] { histFile.getAbsolutePath() }), e)); 
		}
	}
	private void writeCommentHistory(XMLWriter writer) {
		writer.startTag(ELEMENT_COMMIT_HISTORY, null, false);
		for (int i = 0; i < previousComments.length && i < maxComments; i++)
			writer.printSimpleTag(ELEMENT_COMMIT_COMMENT, previousComments[i]);
		writer.endTag(ELEMENT_COMMIT_HISTORY);
	}
		
	public void addRepositoryListener(IRepositoryListener listener) {
		listeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Return the entered comment or null if canceled.
	 * @param proposedComment
	 */
	public String promptForComment(final Shell shell, IResource[] resourcesToCommit, String proposedComment) {
		final int[] result = new int[1];
		final ReleaseCommentDialog dialog = new ReleaseCommentDialog(shell, resourcesToCommit, proposedComment, IResource.DEPTH_INFINITE); 
		shell.getDisplay().syncExec(() -> {
			result[0] = dialog.open();
			if (result[0] != Window.OK)
				return;
		});
		if (result[0] != Window.OK) return null;
		return dialog.getComment();
	}
	
	/**
	 * Prompt to add all or some of the provided resources to version control.
	 * The value null is returned if the dialog is cancelled.
	 * 
	 * @param shell
	 * @param unadded
	 * @return IResource[]
	 */
	public IResource[] promptForResourcesToBeAdded(Shell shell, IResource[] unadded) {
		if (unadded == null) return new IResource[0];
		if (unadded.length == 0) return unadded;
		final IResource[][] result = new IResource[1][0];
		result[0] = null;
		final AddToVersionControlDialog dialog = new AddToVersionControlDialog(shell, unadded);
		shell.getDisplay().syncExec(() -> {
			int code = dialog.open();
			if (code == IDialogConstants.YES_ID) {
				result[0] = dialog.getResourcesToAdd();
			} else if (code == IDialogConstants.NO_ID) {
				// allow the commit to continue.
				result[0] = new IResource[0];
			}
		});
		return result[0];
	}
	
	public ICVSRepositoryLocation getRepositoryLocationFor(ICVSResource resource) {
		try {
			return internalGetRepositoryLocationFor(resource);
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
			return null;
		}
	}

	private ICVSRepositoryLocation internalGetRepositoryLocationFor(ICVSResource resource) throws CVSException {
		ICVSFolder folder;
		if (resource.isFolder()) {
			folder = (ICVSFolder)resource;
		} else {
			folder = resource.getParent();
		}
		if (folder.isCVSFolder()) {
			ICVSRepositoryLocation location = KnownRepositories.getInstance().getRepository(folder.getFolderSyncInfo().getRoot());
			return location;
		}
		// XXX This is asking for trouble
		return null;
	}
		
	private RepositoryRoot getRepositoryRootFor(ICVSResource resource) throws CVSException {
		ICVSRepositoryLocation location = internalGetRepositoryLocationFor(resource);
		if (location == null) return null;
		return getRepositoryRootFor(location);
	}
	
	public RepositoryRoot getRepositoryRootFor(ICVSRepositoryLocation location) {
		RepositoryRoot root = repositoryRoots.get(location.getLocation(false));
		if (root == null) {
			root = new RepositoryRoot(location);
			add(root);
		}
		return root;
	}
	
	/**
	 * Add the given repository root to the receiver. The provided instance of RepositoryRoot
	 * is used to provide extra information about the repository location
	 * 
	 * @param currentRepositoryRoot
	 */
	public void add(RepositoryRoot root) {
		repositoryRoots.put(root.getRoot().getLocation(false), root);
		broadcastRepositoryChange(root);
	}
	
	private void broadcastRepositoryChange(RepositoryRoot root) {
		if (notificationLevel == 0) {
			broadcastRepositoriesChanged(new ICVSRepositoryLocation[] {root.getRoot()});
		} else {
			changedRepositories.put(root.getRoot().getLocation(false), root.getRoot());
		}
	}
	
	private void broadcastRepositoriesChanged(ICVSRepositoryLocation[] roots) {
		if (roots.length == 0) return;
		for (IRepositoryListener listener : listeners) {
			listener.repositoriesChanged(roots);
		}
	}
	
	/**
	 * Run the given runnable, waiting until the end to perform a refresh
	 * 
	 * @param runnable
	 * @param monitor
	 */
	public void run(IRunnableWithProgress runnable, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			notificationLevel++;
			runnable.run(monitor);
		} finally {
			notificationLevel = Math.max(0, notificationLevel - 1);
			if (notificationLevel == 0) {
				try {
					Collection<ICVSRepositoryLocation> roots = changedRepositories.values();
					broadcastRepositoriesChanged(roots.toArray(new ICVSRepositoryLocation[roots.size()]));
				} finally {
					changedRepositories.clear();
				}
			}
		}
	}
	
	/**
	 * Method isDisplayingProjectVersions.
	 * @param repository
	 * @return boolean
	 */
	public boolean isDisplayingProjectVersions(ICVSRepositoryLocation repository) {
		return true;
	}
	
	/**
	 * Method filterResources filters the given resources using the given
	 * working set.
	 *
	 * @param current
	 * @param resources
	 * @return ICVSRemoteResource[]
	 */
	public ICVSRemoteResource[] filterResources(IWorkingSet workingSet, ICVSRemoteResource[] resources) {
		if (workingSet == null) return resources;
		// get the projects associated with the working set
		IAdaptable[] adaptables = workingSet.getElements();
		Set<IProject> projects = new HashSet<>();
		for (IAdaptable adaptable : adaptables) {
			Object adapted = adaptable.getAdapter(IResource.class);
			if (adapted != null) {
				// Can this code be generalized?
				IProject project = ((IResource)adapted).getProject();
				projects.add(project);
			}
		}
		List<ICVSRemoteResource> result = new ArrayList<>();
		for (ICVSRemoteResource resource : resources) {
			for (Iterator iter = projects.iterator(); iter.hasNext();) {
				IProject project = (IProject) iter.next();
				if (project.getName().equals(resource.getName())) {
					result.add(resource);
					break;
				}
			}
		}
		return result.toArray(new ICVSRemoteResource[result.size()]);
	}
	
	/**
	 * Method setLabel.
	 * @param location
	 * @param label
	 */
	public void setLabel(CVSRepositoryLocation location, String label) {
		RepositoryRoot root = getRepositoryRootFor(location);
		String oldLabel = root.getName();
		if (oldLabel == null) {
			if (label == null) return;
			root.setName(label);
		} else if (label == null) {
			root.setName(label);
		} else if (label.equals(oldLabel)) {
			return;
		} else {
			root.setName(label);
		}
		broadcastRepositoryChange(root);	
	}
	
	/**
	 * Replace the old repository location with the new one assuming that they
	 * are the same location with different authentication informations
	 * @param location
	 * @param newLocation
	 */
	public void replaceRepositoryLocation(
			final ICVSRepositoryLocation oldLocation,
			final CVSRepositoryLocation newLocation) {
		
		try {
			run(monitor -> {
				RepositoryRoot root = getRepositoryRootFor(oldLocation);
				// Disposing of the old location will result in the deletion of the
				// cached root through a listener callback
				KnownRepositories.getInstance().disposeRepository(oldLocation);

				// Get the new location from the CVS plugin to ensure we use the
				// instance that will be returned by future calls to getRepository()
				boolean isNew = !KnownRepositories.getInstance().isKnownRepository(newLocation.getLocation());
				root.setRepositoryLocation(
						KnownRepositories.getInstance().addRepository(newLocation, isNew /* broadcast */));
				add(root);
			}, Policy.monitorFor(null));
		} catch (InvocationTargetException e) {
			CVSException.wrapException(e);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Purge any cahced information.
	 */
	public void purgeCache() {
		for (RepositoryRoot root : repositoryRoots.values()) {
			root.clearCache();
		}
	}

	/**
	 * Answer the list of comments that were previously used when committing.
	 * @return String[]
	 */
	public String[] getPreviousComments() {
		return previousComments;
	}

	/**
	 * Method addComment.
	 * @param string
	 */
	public void addComment(String comment) {
		// Make comment first element if it's already there
		int index = getCommentIndex(comment);
		if (index != -1) {
			makeFirstElement(index);
			return;
		}
		if (containsCommentTemplate(comment))
			return;
		
		// Insert the comment as the first element
		String[] newComments = new String[Math.min(previousComments.length + 1, maxComments)];
		newComments[0] = comment;
		for (int i = 1; i < newComments.length; i++) {
			newComments[i] = previousComments[i-1];
		}
		previousComments = newComments;
	}

	private int getCommentIndex(String comment) {
		for (int i = 0; i < previousComments.length; i++) {
			if (previousComments[i].equals(comment)) {
				return i;
			}
		}
		return -1;
	}
	
	private void makeFirstElement(int index) {
		String[] newComments = new String[previousComments.length];
		newComments[0] = previousComments[index];
		System.arraycopy(previousComments, 0, newComments, 1, index);
		int maxIndex = previousComments.length - 1;
		if (index != maxIndex) {
			int nextIndex = (index + 1);
			System.arraycopy(previousComments, nextIndex, newComments,
					nextIndex, (maxIndex - index));
		}
		previousComments = newComments;
	}
	
	private void readCommentTemplates(InputStream stream) throws IOException, TeamException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			parser.parse(new InputSource(stream),
					new CommentTemplatesContentHandler());
		} catch (SAXException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(
					CVSUIMessages.RepositoryManager_parsingProblem,
					new String[] { COMMENT_TEMPLATES_FILE }), ex);
			throw new CVSException(status);
		} catch (ParserConfigurationException ex) {
			IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(
					CVSUIMessages.RepositoryManager_parsingProblem,
					new String[] { COMMENT_TEMPLATES_FILE }), ex);
			throw new CVSException(status);
		}
	}
	
	protected void saveCommentTemplates() throws TeamException {
		IPath pluginStateLocation = CVSUIPlugin.getPlugin().getStateLocation();
		File tempFile = pluginStateLocation.append(
				COMMENT_TEMPLATES_FILE + ".tmp").toFile(); //$NON-NLS-1$
		File histFile = pluginStateLocation.append(COMMENT_TEMPLATES_FILE)
				.toFile();
		try {
			try (XMLWriter writer = new XMLWriter(new BufferedOutputStream(
					new FileOutputStream(tempFile)))) {
				writeCommentTemplates(writer);
			}
			if (histFile.exists()) {
				histFile.delete();
			}
			boolean renamed = tempFile.renameTo(histFile);
			if (!renamed) {
				throw new TeamException(new Status(IStatus.ERROR,
						CVSUIPlugin.ID, TeamException.UNABLE, NLS.bind(
								CVSUIMessages.RepositoryManager_rename,
								new String[] { tempFile.getAbsolutePath() }),
						null));
			}
		} catch (IOException e) {
			throw new TeamException(new Status(IStatus.ERROR, CVSUIPlugin.ID,
					TeamException.UNABLE, NLS.bind(
							CVSUIMessages.RepositoryManager_save,
							new String[] { histFile.getAbsolutePath() }), e));
		}
	}
	
	private void writeCommentTemplates(XMLWriter writer) {
		writer.startTag(ELEMENT_COMMENT_TEMPLATES, null, false);
		for (String commentTemplate : commentTemplates) {
			writer.printSimpleTag(ELEMENT_COMMIT_COMMENT, commentTemplate);
		}
		writer.endTag(ELEMENT_COMMENT_TEMPLATES);
	}
	
	private boolean containsCommentTemplate(String comment) {
		for (String commentTemplate : commentTemplates) {
			if (commentTemplate.equals(comment)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get list of comment templates.
	 */
	public String[] getCommentTemplates() {
		return commentTemplates;
	}
	
	public void replaceAndSaveCommentTemplates(String[] templates)
			throws TeamException {
		commentTemplates = templates;
		saveCommentTemplates();
	}
}
