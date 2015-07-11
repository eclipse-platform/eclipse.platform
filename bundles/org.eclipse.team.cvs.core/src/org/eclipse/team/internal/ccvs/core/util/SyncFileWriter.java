/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 189304 [Sync Info] cvsignore lines should be split on whitespace
 *	   Sergey Prigogin (Google) - [464072] Refresh on Access ignored during text search
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.util;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSEntryLineTag;
import org.eclipse.team.internal.ccvs.core.syncinfo.*;

/*
 * This is a helper class that knows the format of the CVS metafiles. It
 * provides a bridge between the CVS metafile formats and location to the
 * Eclipse CVS client ResourceSyncInfo and FolderSyncInfo types.
 */
public class SyncFileWriter {

	// the famous CVS meta directory name
	public static final String CVS_DIRNAME = "CVS"; //$NON-NLS-1$

	// CVS meta files located in the CVS subdirectory
	public static final String REPOSITORY = "Repository"; //$NON-NLS-1$
	public static final String ROOT = "Root"; //$NON-NLS-1$
	public static final String STATIC = "Entries.Static";	 //$NON-NLS-1$
	public static final String TAG = "Tag";	 //$NON-NLS-1$
	public static final String ENTRIES = "Entries"; //$NON-NLS-1$
	//private static final String PERMISSIONS = "Permissions"; //$NON-NLS-1$
	public static final String ENTRIES_LOG="Entries.Log"; //$NON-NLS-1$
	public static final String NOTIFY = "Notify"; //$NON-NLS-1$
	public static final String BASE_DIRNAME = "Base"; //$NON-NLS-1$
	public static final String BASEREV = "Baserev"; //$NON-NLS-1$
	
	// the local workspace file that contains pattern for ignored resources
	public static final String IGNORE_FILE = ".cvsignore"; //$NON-NLS-1$

	// Some older CVS clients may of added a line to the entries file consisting
	// of only a 'D'. It is safe to ignore these entries.	
	private static final String FOLDER_TAG="D"; //$NON-NLS-1$
	
	// Command characters found in the Entries.log file
	private static final String ADD_TAG="A "; //$NON-NLS-1$
	private static final String REMOVE_TAG="R "; //$NON-NLS-1$	
	
	// key for saving the mod stamp for each written meta file
	public static final QualifiedName MODSTAMP_KEY = new QualifiedName("org.eclipse.team.cvs.core", "meta-file-modtime"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Reads the CVS/Entries, CVS/Entries.log and CVS/Permissions files from the
	 * specified folder and returns ResourceSyncInfo instances for the data stored therein.
	 * If the folder does not have a CVS subdirectory then <code>null</code> is returned.
	 */
	public static byte[][] readAllResourceSync(IContainer parent) throws CVSException {
        IFolder cvsSubDir = getCVSSubdirectory(parent);
        
        if (!folderExists(cvsSubDir)){
        	return null;
        }
        
		if (Policy.DEBUG_METAFILE_CHANGES) {
			System.out.println("Reading Entries file for " + parent.getFullPath()); //$NON-NLS-1$
		}

		// process Entries file contents
		String[] entries = readLines(cvsSubDir.getFile(ENTRIES));
		if (entries == null) return null;
		Map infos = new TreeMap();
		for (int i = 0; i < entries.length; i++) {
			String line = entries[i];
			if(!FOLDER_TAG.equals(line) && !"".equals(line)) { //$NON-NLS-1$
				try {
					ResourceSyncInfo info = new ResourceSyncInfo(line, null);
					infos.put(info.getName(), info);
				} catch (CVSException e) {
					// There was a problem parsing the entry line.
					// Log the problem and skip the entry
					CVSProviderPlugin.log(new CVSStatus(IStatus.ERROR, NLS.bind(CVSMessages.SyncFileWriter_0, new String[] { parent.getFullPath().toString() }), e)); 
				}			
			}
		}
		
		// process Entries.log file contents
		String[] entriesLog = readLines(cvsSubDir.getFile(ENTRIES_LOG));
		if (entriesLog != null) {
			for (int i = 0; i < entriesLog.length; i++) {
				String line = entriesLog[i];
				if (line.startsWith(ADD_TAG)) {
					line = line.substring(ADD_TAG.length());
					ResourceSyncInfo info = new ResourceSyncInfo(line, null);
					infos.put(info.getName(), info);
				} else if (line.startsWith(REMOVE_TAG)) {
					line = line.substring(REMOVE_TAG.length());
					ResourceSyncInfo info = new ResourceSyncInfo(line, null);
					infos.remove(info.getName());
				}
			}
		}
		
		//return (ResourceSyncInfo[])infos.values().toArray(new ResourceSyncInfo[infos.size()]);
		byte[][] result = new byte[infos.size()][];
		int i = 0;
		for (Iterator iter = infos.values().iterator(); iter.hasNext();) {
			ResourceSyncInfo info = (ResourceSyncInfo) iter.next();
			result[i++] = info.getBytes();
		}
		return result;
	}
	
	private static boolean folderExists(IFolder cvsSubDir) throws CVSException {
	    try {
	    	URI uri = cvsSubDir.getLocationURI();
	    	if (uri != null){
				IFileStore store = EFS.getStore(uri);
				if (store != null){
					return store.fetchInfo().exists();
				}
	    	}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} 
		return false;
	}

	public static void writeAllResourceSync(IContainer parent, byte[][] infos) throws CVSException {
		try {
			if (Policy.DEBUG_METAFILE_CHANGES) {
				System.out.println("Writing Entries file for folder " + parent.getFullPath()); //$NON-NLS-1$
			}
			IFolder cvsSubDir = createCVSSubdirectory(parent);

			// format file contents
			String[] entries = new String[infos.length];
			for (int i = 0; i < infos.length; i++) {
				byte[] info = infos[i];
				entries[i] = new String(info);
			}

			// write Entries
			writeLines(cvsSubDir.getFile(ENTRIES), entries);

			// delete Entries.log
			cvsSubDir.getFile(ENTRIES_LOG).delete(IResource.NONE, null);
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	/**
	 * Reads the CVS/Root, CVS/Repository, CVS/Tag, and CVS/Entries.static files from
	 * the specified folder and returns a FolderSyncInfo instance for the data stored therein.
	 * If the folder does not have a CVS subdirectory then <code>null</code> is returned.
	 */
	public static FolderSyncInfo readFolderSync(IContainer folder) throws CVSException {
		IFolder cvsSubDir = getCVSSubdirectory(folder);
		
        if (!folderExists(cvsSubDir)){
        	return null;
        }

        if (Policy.DEBUG_METAFILE_CHANGES) {
			System.out.println("Reading Root/Repository files for " + folder.getFullPath()); //$NON-NLS-1$
		}
		
		// check to make sure the the cvs folder is hidden
		if (!cvsSubDir.isTeamPrivateMember() && cvsSubDir.exists()) {
			try {
				cvsSubDir.setTeamPrivateMember(true);
			} catch (CoreException e) {
				CVSProviderPlugin.log(e);
			}
		}
				
		// read CVS/Root
		String root = readFirstLine(cvsSubDir.getFile(ROOT));
		if (root == null) return null;
		
		// read CVS/Repository
		String repository = readFirstLine(cvsSubDir.getFile(REPOSITORY));
		if (repository == null) return null;
		
		// read CVS/Tag
		String tag = readFirstLine(cvsSubDir.getFile(TAG));
		if (Policy.DEBUG_METAFILE_CHANGES && tag != null) {
			System.out.println("Reading Tag file for " + folder.getFullPath()); //$NON-NLS-1$
		}
		CVSTag cvsTag = (tag != null) ? new CVSEntryLineTag(tag) : null;

		// read Entries.Static
		String staticDir = readFirstLine(cvsSubDir.getFile(STATIC));
		if (Policy.DEBUG_METAFILE_CHANGES && staticDir != null) {
			System.out.println("Reading Static file for " + folder.getFullPath()); //$NON-NLS-1$
		}
		boolean isStatic = (staticDir != null);
		
		// return folder sync
		return new FolderSyncInfo(repository, root, cvsTag, isStatic);		
	}
	
	/**
	 * Writes the CVS/Root, CVS/Repository, CVS/Tag, and CVS/Entries.static files to the
	 * specified folder using the data contained in the specified FolderSyncInfo instance.
	 */
	public static void writeFolderSync(IContainer folder, FolderSyncInfo info) throws CVSException {
		try {
			if (Policy.DEBUG_METAFILE_CHANGES) {
				System.out.println("Writing Root/Respository files for " + folder.getFullPath()); //$NON-NLS-1$
			}
			IFolder cvsSubDir = createCVSSubdirectory(folder);
	
			// write CVS/Root
			writeLines(cvsSubDir.getFile(ROOT), new String[] {info.getRoot()});
			
			// write CVS/Repository
			writeLines(cvsSubDir.getFile(REPOSITORY), new String[] {info.getRepository()});
			
			// write CVS/Tag
			IFile tagFile = cvsSubDir.getFile(TAG);
			if (info.getTag() != null) {
				if (Policy.DEBUG_METAFILE_CHANGES) {
					System.out.println("Writing Tag file for " + folder.getFullPath()); //$NON-NLS-1$
				}
				writeLines(tagFile, new String[] {info.getTag().toEntryLineFormat(false)});
			} else {
				if(tagFile.exists()) {
					if (Policy.DEBUG_METAFILE_CHANGES) {
						System.out.println("Deleting Tag file for " + folder.getFullPath()); //$NON-NLS-1$
					}
					tagFile.delete(IResource.NONE, null);
				}
			}
			
			// write CVS/Entries.Static
			IFile staticFile = cvsSubDir.getFile(STATIC);
			if(info.getIsStatic()) {
				// the existence of the file is all that matters
				if (Policy.DEBUG_METAFILE_CHANGES) {
					System.out.println("Writing Static file for " + folder.getFullPath()); //$NON-NLS-1$
				}
				writeLines(staticFile, new String[] {""}); //$NON-NLS-1$
			} else {
				if(staticFile.exists()) {
					if (Policy.DEBUG_METAFILE_CHANGES) {
						System.out.println("Deleting Static file for " + folder.getFullPath()); //$NON-NLS-1$
					}
					staticFile.delete(IResource.NONE, null);
				}
			}
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Returns all .cvsignore entries for the specified folder.
	 */
	public static String[] readCVSIgnoreEntries(IContainer folder) throws CVSException {
		IFile ignoreFile = folder.getFile(new Path(IGNORE_FILE));
		if (ignoreFile != null) {
			String[] lines = readLines(ignoreFile);
			if (lines == null)
				return null;
			// Split each line on spaces and tabs.
			ArrayList/*<String>*/ entries = new ArrayList/*<String>*/();
			for (int ln = 0; ln < lines.length; ln++) {
				String line = lines[ln];
				int pos = 0;
				while (pos < line.length()) {
					if (line.charAt(pos) == ' ' || line.charAt(pos) == '\t')
						pos++;
					else {
						int start = pos;
						while (pos < line.length() && line.charAt(pos) != ' ' && line.charAt(pos) != '\t')
							pos++;
						entries.add(line.substring(start, pos));
					}
				}
			}
			return (String[]) entries.toArray(new String[entries.size()]);
		}
		return null;
	}
	
	/**
	 * Writes all entries to the specified folder's .cvsignore file, overwriting any
	 * previous edition of the file.
	 */
	public static void writeCVSIgnoreEntries(IContainer folder, String[] patterns) throws CVSException {
		IFile ignoreFile = folder.getFile(new Path(IGNORE_FILE));
		writeLines(ignoreFile, patterns);
	}	

	/**
	 * Delete folder sync is equivalent to removing the CVS subdir.
	 */
	public static void deleteFolderSync(IContainer folder) throws CVSException {		
		try {
			if (Policy.DEBUG_METAFILE_CHANGES) {
				System.out.println("Deleting CVS directory from " + folder.getFullPath()); //$NON-NLS-1$
			}
			getCVSSubdirectory(folder).delete(IResource.NONE, null);
		} catch(CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	/**
	 * Reads the CVS/Notify file from the specified folder and returns NotifyInfo instances 
	 * for the data stored therein. If the folder does not have a CVS subdirectory then <code>null</code> is returned.
	 */
	public static NotifyInfo[] readAllNotifyInfo(IContainer parent) throws CVSException {
		IFolder cvsSubDir = getCVSSubdirectory(parent);

        if (!folderExists(cvsSubDir)){
        	return null;
        }
        
		// process Notify file contents
		String[] entries = readLines(cvsSubDir.getFile(NOTIFY));
		if (entries == null) return null;
		Map infos = new TreeMap();
		for (int i = 0; i < entries.length; i++) {
			String line = entries[i];
			if(!"".equals(line)) { //$NON-NLS-1$
				try {
                    NotifyInfo info = new NotifyInfo(parent, line);
                    infos.put(info.getName(), info);
                } catch (CVSException e) {
                    // We couldn't parse the notify info
                    // Log it and ignore
                    CVSProviderPlugin.log(e);
                }			
			}
		}
		
		return (NotifyInfo[])infos.values().toArray(new NotifyInfo[infos.size()]);
	}
	
	/**
	 * Writes the CVS/Notify file to the specified folder using the data contained in the 
	 * specified NotifyInfo instances. A CVS subdirectory must already exist (an exception 
	 * is thrown if it doesn't).
	 */
	public static void writeAllNotifyInfo(IContainer parent, NotifyInfo[] infos) throws CVSException {
		// get the CVS directory
		IFolder cvsSubDir = getCVSSubdirectory(parent);
		// write lines will throw an exception if the CVS directory does not exist
		
		if (infos.length == 0) {
			// if there are no notify entries, delete the notify file
			try {
				IFile notifyFile = cvsSubDir.getFile(NOTIFY);
				if(notifyFile.exists()) {
					notifyFile.delete(IResource.NONE, null);
				}
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		} else {
			// format file contents
			String[] entries = new String[infos.length];
			for (int i = 0; i < infos.length; i++) {
				NotifyInfo info = infos[i];
				entries[i] = info.getNotifyLine();
			}
	
			// write Notify entries
			writeLines(cvsSubDir.getFile(NOTIFY), entries);
		}
	}

	/**
	 * Reads the CVS/Baserev file from the specified folder and returns
	 * BaserevInfo instances for the data stored therein. If the folder does not
	 * have a CVS subdirectory then <code>null</code> is returned.
	 */
	public static BaserevInfo[] readAllBaserevInfo(IContainer parent) throws CVSException {
		IFolder cvsSubDir = getCVSSubdirectory(parent);
        
        if (!folderExists(cvsSubDir)){
        	return null;
        }
        
		// process Notify file contents
		String[] entries = readLines(cvsSubDir.getFile(BASEREV));
		if (entries == null) return null;
		Map infos = new TreeMap();
		for (int i = 0; i < entries.length; i++) {
			String line = entries[i];
			if(!"".equals(line)) { //$NON-NLS-1$
				BaserevInfo info = new BaserevInfo(line);
				infos.put(info.getName(), info);
			}
		}

		return (BaserevInfo[])infos.values().toArray(new BaserevInfo[infos.size()]);
	}

	/**
	 * Writes the CVS/Baserev file to the specified folder using the data
	 * contained in the specified BaserevInfo instances. A CVS subdirectory must
	 * already exist (an exception is thrown if it doesn't).
	 */
	public static void writeAllBaserevInfo(IContainer parent, BaserevInfo[] infos) throws CVSException {
		// get the CVS directory
		IFolder cvsSubDir = getCVSSubdirectory(parent);
		// write lines will throw an exception if the CVS directory does not exist

		// format file contents
		String[] entries = new String[infos.length];
		for (int i = 0; i < infos.length; i++) {
			BaserevInfo info = infos[i];
			entries[i] = info.getEntryLine();
		}

		// write Notify entries
		writeLines(cvsSubDir.getFile(BASEREV), entries);
	}
				
	/**
	 * Returns the CVS subdirectory for this folder.
	 */
	private static IFolder getCVSSubdirectory(IContainer folder) {
		return folder.getFolder(new Path(CVS_DIRNAME));
	}
	
	/**
	 * Creates and makes team-private and returns a CVS subdirectory in this folder.
	 */
	private static IFolder createCVSSubdirectory(IContainer folder) throws CVSException {
		try {
			final IFolder cvsSubDir = getCVSSubdirectory(folder);
			if (! cvsSubDir.exists()) {
				// important to have both the folder creation and setting of team-private in the
				// same runnable so that the team-private flag is set before other delta listeners 
				// sees the CVS folder creation.
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						// Re-check existence in case this method was called without a resource rule
						if (! cvsSubDir.exists()) {
							if (existsInFileSystem(cvsSubDir)) {
								cvsSubDir.refreshLocal(IResource.DEPTH_INFINITE, null);
								cvsSubDir.setTeamPrivateMember(true);
							} else {
								cvsSubDir.create(IResource.TEAM_PRIVATE, true /*make local*/, null);
							}
						} else {
							if (!cvsSubDir.isTeamPrivateMember()) {
								cvsSubDir.setTeamPrivateMember(true);
							}
						}
					} 
				}, folder, 0, null);
			}
			return cvsSubDir;
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}

	protected static boolean existsInFileSystem(IFolder cvsSubDir) {
		URI uri = cvsSubDir.getLocationURI();
		if (uri != null) {
			try {
				IFileStore store = EFS.getStore(uri);
				if (store != null) {
					return store.fetchInfo().exists();
				}
			} catch (CoreException e) {
				CVSProviderPlugin.log(e);
			}
		}
		return false;
	}

	/*
	 * Reads the first line of the specified file.
	 * Returns null if the file does not exist, or the empty string if it is blank.
	 */
	private static String readFirstLine(IFile file) throws CVSException {
		try {
			InputStream in = getInputStream(file);
			if (in != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in), 512);
				try {
					String line = reader.readLine();
					if (line == null) return ""; //$NON-NLS-1$
					return line;
				} finally {
					reader.close();
				}
            }
            return null;
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		} catch (CoreException e) {
			// If the IFile doesn't exist or the underlying File doesn't exist,
			// just return null to indicate the absence of the file
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND
					|| e.getStatus().getCode() == IResourceStatus.FAILED_READ_LOCAL)
				return null;
			throw CVSException.wrapException(e);
		}
	}

	private static InputStream getInputStream(IFile file) throws CoreException, FileNotFoundException {
		if (file.exists()) {
		    return file.getContents(true);
		}
		
		URI uri = file.getLocationURI();
		if (uri != null) {
			IFileStore store = EFS.getStore(uri);
			if (store != null) {
				return store.openInputStream(EFS.NONE, null);
			}
		}
		
		IPath location = file.getLocation();
		if (location != null) {
			File ioFile = location.toFile();
			if (ioFile != null && ioFile.exists()) {
				return new FileInputStream(ioFile);
			}
		}

		return null;
	}
	
	/*
	 * Reads all lines of the specified file.
	 * Returns null if the file does not exist.
	 */
	private static String[] readLines(IFile file) throws CVSException {
		try {
			InputStream in = getInputStream(file);
			if (in != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in), 512);
				List fileContentStore = new ArrayList();
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						fileContentStore.add(line);
					}
					return (String[]) fileContentStore.toArray(new String[fileContentStore.size()]);
				} finally {
					reader.close();
				}
			}
			return null;
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		} catch (CoreException e) {
			// If the IFile doesn't exist or the underlying File doesn't exist,
			// just return null to indicate the absence of the file
			switch (e.getStatus().getCode()) {
			case IResourceStatus.RESOURCE_NOT_FOUND:
			case IResourceStatus.NOT_FOUND_LOCAL:
			case IResourceStatus.FAILED_READ_LOCAL:
				return null;
			default:
				throw CVSException.wrapException(e);
			}
		}
	}
	
	/*
	 * Writes all lines to the specified file, using linefeed terminators for
	 * compatibility with other CVS clients.
	 */
	private static void writeLines(final IFile file, final String[] contents) throws CVSException {
		try {
			// The creation of sync files has to be in a runnable in order for the resulting delta
			// to include the MODSTAMP value. If not in a runnable then create/setContents
			// will trigger a delta and the SyncFileWriter change listener won't know that the delta
			// was a result of our own creation.
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					try {
						ByteArrayOutputStream os = new ByteArrayOutputStream();
						writeLinesToStreamAndClose(os, contents);
						if(!file.exists()) {
							file.create(new ByteArrayInputStream(os.toByteArray()), IResource.FORCE /*don't keep history but do force*/, null);
						} else {
							file.setContents(new ByteArrayInputStream(os.toByteArray()), IResource.FORCE /*don't keep history but do force*/, null);
						}			
						file.setSessionProperty(MODSTAMP_KEY, new Long(file.getModificationStamp()));
					} catch(CVSException e) {
						throw new CoreException(e.getStatus());
					}
				}
			}, ResourcesPlugin.getWorkspace().getRuleFactory().createRule(file), 0, null);
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	private static void writeLinesToStreamAndClose(OutputStream os, String[] contents) throws CVSException {
		byte[] lineEnd = getLineDelimiter();
		try {
			try {
				for (int i = 0; i < contents.length; i++) {
					os.write(contents[i].getBytes());
					os.write(lineEnd);
				}
			} finally {
				os.close();
			}
		} catch (IOException e) {
			throw CVSException.wrapException(e);
		}
	}
	
	/**
	 * Method writeFileToBaseDirectory.
	 * 
	 * @param file
	 * @param info
	 */
	public static void writeFileToBaseDirectory(IFile file, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			IFolder baseFolder = getBaseDirectory(file);
			if (!baseFolder.exists()) {
				baseFolder.create(false /* force */, true /* local */, Policy.subMonitorFor(monitor, 10));
			}
			IFile target = baseFolder.getFile(new Path(null, file.getName()));
			if (target.exists()) {
				// XXX Should ensure that we haven't already copied it
				// XXX write the revision to the CVS/Baserev file
				setReadOnly(target, false);
				target.delete(true, Policy.subMonitorFor(monitor, 10));
			}
			// Copy the file so the timestamp is maintained
			file.copy(target.getFullPath(), true /* force */, Policy.subMonitorFor(monitor, 80));
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}
	/**
	 * Method restoreFileFromBaseDirectory.
	 * @param file
	 * @param info
	 * @param monitor
	 */
	public static void restoreFileFromBaseDirectory(IFile file, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			IFolder baseFolder = getBaseDirectory(file);
			IFile source = baseFolder.getFile(new Path(null, file.getName()));
			if (!source.exists()) {
				IStatus status = new CVSStatus(IStatus.ERROR, CVSStatus.ERROR, NLS.bind(CVSMessages.SyncFileWriter_baseNotAvailable, new String[] { file.getFullPath().toString() }), file);
				throw new CVSException(status); 
			}
			if (file.exists()) {
				file.delete(false /* force */, true /* keep history */, Policy.subMonitorFor(monitor, 10));
			}
			// Make the source writtable to avoid problems on some file systems (bug 109308)
			setReadOnly(source, false);
			// Copy the file so the timestamp is maintained
			source.move(file.getFullPath(), false /* force */, true /* keep history */,Policy.subMonitorFor(monitor, 100));
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}

	private static void setReadOnly(IFile source, boolean readOnly) {
		ResourceAttributes attrs = source.getResourceAttributes();
		if (attrs != null && attrs.isReadOnly() != readOnly) {
			attrs.setReadOnly(readOnly);
			try {
		        source.setResourceAttributes(attrs);
		    } catch (CoreException e) {
		    	// Just log the failure since the move may succeed anyway
		        CVSProviderPlugin.log(e);
		    }
		}
	}
	
	/**
	 * Method deleteFileFromBaseDirectory.
	 * @param file
	 * @param monitor
	 */
	public static void deleteFileFromBaseDirectory(IFile file, IProgressMonitor monitor) throws CVSException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, 100);
		try {
			IFolder baseFolder = getBaseDirectory(file);
			IFile source = baseFolder.getFile(new Path(null, file.getName()));
			if (source.exists()) {
				setReadOnly(source, false);
				source.delete(false, false, Policy.subMonitorFor(monitor, 100));
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		} finally {
			monitor.done();
		}
	}

	private static IFolder getBaseDirectory(IFile file) {
		IContainer cvsFolder = getCVSSubdirectory(file.getParent());
		IFolder baseFolder = cvsFolder.getFolder(new Path(BASE_DIRNAME));
		return baseFolder;
	}
	
	/**
	 * Return a handle to the CVS/Template file for the given folder
	 * @param folder
	 * @return IFile
	 * @throws CVSException
	 */
	public static IFile getTemplateFile(IContainer folder) throws CVSException {
		IFolder cvsFolder = createCVSSubdirectory(folder);
		return cvsFolder.getFile("Template"); //$NON-NLS-1$
	}
	
	/**
	 * Method isEdited.
	 * @param resource
	 * @return boolean
	 */
	public static boolean isEdited(IFile file) {
		IFolder baseFolder = getBaseDirectory(file);
		IFile baseFile = baseFolder.getFile(file.getName());
		return baseFile.exists();
	}
	
	private static byte[] getLineDelimiter() {
		if (CVSProviderPlugin.getPlugin().isUsePlatformLineend()) {
			String property = System.getProperty("line.separator"); //$NON-NLS-1$
			if (property != null) return property.getBytes();
		}
		return new byte[] { 0x0A }; 
	}

}
