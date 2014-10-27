/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Burger <m@rtin-burger.de> patch for #93810 and #93901
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.patch.IHunkFilter;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.TextUtilities;

/**
 * A Patcher 
 * - knows how to parse various patch file formats into some in-memory structure,
 * - holds onto the parsed data and the options to use when applying the patches,
 * - knows how to apply the patches to files and folders.
 */
public class Patcher implements IHunkFilter {

	static protected final String REJECT_FILE_EXTENSION= ".rej"; //$NON-NLS-1$

	static protected final String MARKER_TYPE= "org.eclipse.compare.rejectedPatchMarker"; //$NON-NLS-1$

	/**
	 * Property used to associate a patcher with a {@link PatchConfiguration}
	 */
	public static final String PROP_PATCHER = "org.eclipse.compare.patcher"; //$NON-NLS-1$
	
	public interface IFileValidator {
		boolean validateResources(IFile[] array);
	}

	// diff formats
	//	private static final int CONTEXT= 0;
	//	private static final int ED= 1;
	//	private static final int NORMAL= 2;
	//	private static final int UNIFIED= 3;
	
	private FilePatch2[] fDiffs;
	private IResource fTarget;
	// patch options
	private Set disabledElements = new HashSet();
	private Map diffResults = new HashMap();
	private final Map contentCache = new HashMap();
	private Set mergedHunks = new HashSet();

	private final PatchConfiguration configuration;
	private boolean fGenerateRejectFile = false;
	
	public Patcher() {
		configuration = new PatchConfiguration();
		configuration.setProperty(PROP_PATCHER, this);
		configuration.addHunkFilter(this);
	}
	
	/*
	 * Returns an array of Diffs after a successful call to <code>parse</code>.
	 * If <code>parse</code> hasn't been called returns <code>null</code>.
	 */
	public FilePatch2[] getDiffs() {
		if (fDiffs == null)
			return new FilePatch2[0];
		return fDiffs;
	}
	
	public IPath getPath(FilePatch2 diff) {
		return diff.getStrippedPath(getStripPrefixSegments(), isReversed());
	}

	/*
	 * Returns <code>true</code> if new value differs from old.
	 */
	public boolean setStripPrefixSegments(int strip) {
		if (strip != getConfiguration().getPrefixSegmentStripCount()) {
			getConfiguration().setPrefixSegmentStripCount(strip);
			return true;
		}
		return false;
	}
	
	int getStripPrefixSegments() {
		return getConfiguration().getPrefixSegmentStripCount();
	}
	
	/*
	 * Returns <code>true</code> if new value differs from old.
	 */
	public boolean setFuzz(int fuzz) {
		if (fuzz != getConfiguration().getFuzz()) {
			getConfiguration().setFuzz(fuzz);
			return true;
		}
		return false;
	}
	
	public int getFuzz(){
		return getConfiguration().getFuzz();
	}
		
	/*
	 * Returns <code>true</code> if new value differs from old.
	 */
	public boolean setIgnoreWhitespace(boolean ignoreWhitespace) {
		if (ignoreWhitespace != getConfiguration().isIgnoreWhitespace()) {
			getConfiguration().setIgnoreWhitespace(ignoreWhitespace);
			return true;
		}
		return false;
	}
	
	public boolean isIgnoreWhitespace() {
		return getConfiguration().isIgnoreWhitespace();
	}
	
	public boolean isGenerateRejectFile() {
		return fGenerateRejectFile;
	}

	public void setGenerateRejectFile(boolean generateRejectFile) {
		fGenerateRejectFile = generateRejectFile;
	}
	
	//---- parsing patch files

	public void parse(IStorage storage) throws IOException, CoreException {
		BufferedReader reader = Utilities.createReader(storage);
		try {
			parse(reader);
		} finally {
			try {
				reader.close();
			} catch (IOException e) { //ignored
			}
		}
	}
	
	public void parse(BufferedReader reader) throws IOException {
		PatchReader patchReader = new PatchReader() {
			protected FilePatch2 createFileDiff(IPath oldPath, long oldDate,
					IPath newPath, long newDate) {
				return new FilePatch(oldPath, oldDate, newPath, newDate);
			}
		};
		patchReader.parse(reader);
		patchParsed(patchReader);
	}

	protected void patchParsed(PatchReader patchReader) {
		fDiffs = patchReader.getDiffs();
	}
	
	public void countLines() {
		FilePatch2[] fileDiffs = getDiffs();
		for (int i = 0; i < fileDiffs.length; i++) {
			int addedLines = 0;
			int removedLines = 0;
			FilePatch2 fileDiff = fileDiffs[i];
			for (int j = 0; j < fileDiff.getHunkCount(); j++) {
				IHunk hunk = fileDiff.getHunks()[j];
				String[] lines = ((Hunk) hunk).getLines();
				for (int k = 0; k < lines.length; k++) {
					char c = lines[k].charAt(0);
					switch (c) {
					case '+':
						addedLines++;
						continue;
					case '-':
						removedLines++;
						continue;
					}
				}
			}
			fileDiff.setAddedLines(addedLines);
			fileDiff.setRemovedLines(removedLines);
		}
	}
	
	//---- applying a patch file

	public void applyAll(IProgressMonitor pm, IFileValidator validator) throws CoreException {
		
		int i;
		
		IFile singleFile= null;	// file to be patched
		IContainer container= null;
		if (fTarget instanceof IContainer)
			container= (IContainer) fTarget;
		else if (fTarget instanceof IFile) {
			singleFile= (IFile) fTarget;
			container= singleFile.getParent();
		} else {
			Assert.isTrue(false);
		}
		
		// get all files to be modified in order to call validateEdit
		List list= new ArrayList();
		if (singleFile != null)
			list.add(singleFile);
		else {
			for (i= 0; i < fDiffs.length; i++) {
				FilePatch2 diff= fDiffs[i];
				if (isEnabled(diff)) {
					switch (diff.getDiffType(isReversed())) {
					case FilePatch2.CHANGE:
						list.add(createPath(container, getPath(diff)));
						break;
					}
				}
			}
		}
		if (! validator.validateResources((IFile[])list.toArray(new IFile[list.size()]))) {
			return;
		}
		
		final int WORK_UNIT= 10;
		if (pm != null) {
			String message= Messages.Patcher_0;	
			pm.beginTask(message, fDiffs.length*WORK_UNIT);
		}
		
		for (i= 0; i < fDiffs.length; i++) {
			
			int workTicks= WORK_UNIT;
			
			FilePatch2 diff= fDiffs[i];
			if (isEnabled(diff)) {
				
				IPath path= getPath(diff);
				if (pm != null)
					pm.subTask(path.toString());
			
				IFile file= singleFile != null
								? singleFile
								: createPath(container, path);
					
				List failed= new ArrayList();
				
				int type= diff.getDiffType(isReversed());
				switch (type) {
				case FilePatch2.ADDITION:
					// patch it and collect rejected hunks
					List result= apply(diff, file, true, failed);
					if (result != null)
						store(LineReader.createString(isPreserveLineDelimeters(), result), file, new SubProgressMonitor(pm, workTicks));
					workTicks-= WORK_UNIT;
					break;
				case FilePatch2.DELETION:
					file.delete(true, true, new SubProgressMonitor(pm, workTicks));
					workTicks-= WORK_UNIT;
					break;
				case FilePatch2.CHANGE:
					// patch it and collect rejected hunks
					result= apply(diff, file, false, failed);
					if (result != null)
						store(LineReader.createString(isPreserveLineDelimeters(), result), file, new SubProgressMonitor(pm, workTicks));
					workTicks-= WORK_UNIT;
					break;
				}

				if (isGenerateRejectFile() && failed.size() > 0) {
					IPath pp = getRejectFilePath(path);
					file= createPath(container, pp);
					if (file != null) {
						store(getRejected(failed), file, pm);
						try {
							IMarker marker= file.createMarker(MARKER_TYPE);
							marker.setAttribute(IMarker.MESSAGE, Messages.Patcher_1);	
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						} catch (CoreException ex) {
							// NeedWork
						}
					}
				}
			}
			
			if (pm != null) {
				if (pm.isCanceled())
					break;
				if (workTicks > 0)
					pm.worked(workTicks);
			}
		}
	}

	private IPath getRejectFilePath(IPath path) {
		IPath pp= null;
		if (path.segmentCount() > 1) {
			pp= path.removeLastSegments(1);
			pp= pp.append(path.lastSegment() + REJECT_FILE_EXTENSION);
		} else
			pp= new Path(path.lastSegment() + REJECT_FILE_EXTENSION);
		return pp;
	}
	
	List apply(FilePatch2 diff, IFile file, boolean create, List failedHunks) {
		FileDiffResult result = getDiffResult(diff);
		List lines = LineReader.load(file, create);
		result.patch(lines, null);
		failedHunks.addAll(result.getFailedHunks());
		if (hasCachedContents(diff)) {
			// Used the cached contents since they would have been provided by the user
			return getCachedLines(diff);
		} else if (!result.hasMatches()) {
			// Return null if there were no matches
			return null;
		}
		return result.getLines();
	}
	
	/*
	 * Converts the string into bytes and stores them in the given file.
	 */
	protected void store(String contents, IFile file, IProgressMonitor pm) throws CoreException {

		if (!file.exists()) {
			if (FileBuffers.getTextFileBufferManager().isTextFileLocation(file.getFullPath(), true)) {
				// For new text files use the line delimiter as defined in the workspace
				String expectedLD= getLineDelimiterPreference(file);
				if (expectedLD != null) {
					String patchLD= TextUtilities.determineLineDelimiter(contents, expectedLD);
					if (!expectedLD.equals(patchLD))
						contents= contents.replaceAll(patchLD, expectedLD);
				}
			}
		}

		byte[] bytes;
		try {
			bytes= contents.getBytes(Utilities.getCharset(file));
		} catch (UnsupportedEncodingException x) {
			// uses default encoding
			bytes= contents.getBytes();
		}
		
		store(bytes,file, pm);
	}

	private static String getLineDelimiterPreference(IFile file) {
		IScopeContext[] scopeContext;
		if (file != null && file.getProject() != null) {
			// project preference
			scopeContext= new IScopeContext[] { new ProjectScope(file.getProject()) };
			String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
			if (lineDelimiter != null)
				return lineDelimiter;
		}
		// workspace preference
		scopeContext= new IScopeContext[] { InstanceScope.INSTANCE };
		return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
	}

	protected void store(byte[] bytes, IFile file, IProgressMonitor pm) throws CoreException {
		InputStream is= new ByteArrayInputStream(bytes);
		try {
			if (file.exists()) {
				file.setContents(is, false, true, pm);
			} else {
				file.create(is, false, pm);
			}
		} finally {
			if (is != null)
				try {
					is.close();
				} catch(IOException ex) {
					// silently ignored
				}
		}
	}

	public boolean isPreserveLineDelimeters() {
		return true;
	}

	public static String getRejected(List failedHunks) {
		if (failedHunks.size() <= 0)
			return null;
		
		String lineSeparator= System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer sb= new StringBuffer();
		Iterator iter= failedHunks.iterator();
		while (iter.hasNext()) {
			Hunk hunk= (Hunk) iter.next();
			sb.append(hunk.getRejectedDescription());
			sb.append(lineSeparator);
			sb.append(hunk.getContent());
		}
		return sb.toString();
	}
	
	/*
	 * Ensures that a file with the given path exists in
	 * the given container. Folder are created as necessary.
	 */
	protected IFile createPath(IContainer container, IPath path) throws CoreException {
		if (path.segmentCount() > 1) {
			IContainer childContainer;
			if (container instanceof IWorkspaceRoot) {
				IProject project = ((IWorkspaceRoot)container).getProject(path.segment(0));
				if (!project.exists())
					project.create(null);
				if (!project.isOpen())
					project.open(null);
				childContainer = project;
			} else {
				IFolder f= container.getFolder(path.uptoSegment(1));
				if (!f.exists())
					f.create(false, true, null);
				childContainer = f;
			}
			return createPath(childContainer, path.removeFirstSegments(1));
		}
		// a leaf
		return container.getFile(path);
	}

	public IResource getTarget() {
		return fTarget;
	}

	public void setTarget(IResource target) {
		fTarget= target;
	}
	

	public IFile getTargetFile(FilePatch2 diff) {
		IPath path = diff.getStrippedPath(getStripPrefixSegments(), isReversed());
		return existsInTarget(path);
	}
	
	/**
	 * Iterates through all of the resources contained in the Patch Wizard target
	 * and looks to for a match to the passed in file 
	 * @param path
	 * @return IFile which matches the passed in path or null if none found
	 */
	public IFile existsInTarget(IPath path) {
		if (fTarget instanceof IFile) { // special case
			IFile file= (IFile) fTarget;
			if (matches(file.getFullPath(), path))
				return file;
		} else if (fTarget instanceof IContainer) {
			IContainer c= (IContainer) fTarget;
			if (c.exists(path))
				return c.getFile(path);
		}
		return null;
	}

	/**
	 * Returns true if path completely matches the end of fullpath
	 * @param fullpath 
	 * @param path 
	 * @return true if path matches, false otherwise
	 */
	private boolean matches(IPath fullpath, IPath path) {
		for (IPath p= fullpath; path.segmentCount()<=p.segmentCount(); p= p.removeFirstSegments(1)) {
			if (p.equals(path))
				return true;
		}
		return false;
	}

	public int calculatePrefixSegmentCount() {
		//Update prefix count - go through all of the diffs and find the smallest
		//path segment contained in all diffs.
		int length= 99;
		if (fDiffs!=null) {
			for (int i= 0; i<fDiffs.length; i++) {
				FilePatch2 diff= fDiffs[i];
				length= Math.min(length, diff.segmentCount());
			}
			if (ResourcesPlugin.getWorkspace().getRoot().equals(fTarget))
				length--;
		}
		return length;
	}
	
	public void addDiff(FilePatch2 newDiff){
		FilePatch2[] temp = new FilePatch2[fDiffs.length + 1];
		System.arraycopy(fDiffs,0, temp, 0, fDiffs.length);
		temp[fDiffs.length] = newDiff;
		fDiffs = temp;
	}
	
	public void removeDiff(FilePatch2 diffToRemove){
		FilePatch2[] temp = new FilePatch2[fDiffs.length - 1];
		int counter = 0;
		for (int i = 0; i < fDiffs.length; i++) {
			if (fDiffs[i] != diffToRemove){
				temp[counter++] = fDiffs[i];
			}
		}
		fDiffs = temp;
	}
	
	public void setEnabled(Object element, boolean enabled) {
		if (element instanceof DiffProject) 
			setEnabledProject((DiffProject) element, enabled);
		if (element instanceof FilePatch2) 
			setEnabledFile((FilePatch2)element, enabled);
		if (element instanceof Hunk) 
			setEnabledHunk((Hunk) element, enabled);
	}
	
	private void setEnabledProject(DiffProject projectDiff, boolean enabled) {
		FilePatch2[] diffFiles = projectDiff.getFileDiffs();
		for (int i = 0; i < diffFiles.length; i++) {
			setEnabledFile(diffFiles[i], enabled);
		}
	}
	
	private void setEnabledFile(FilePatch2 fileDiff, boolean enabled) {
		IHunk[] hunks = fileDiff.getHunks();
		for (int i = 0; i < hunks.length; i++) {
			setEnabledHunk((Hunk) hunks[i], enabled);
		}
	}

	private void setEnabledHunk(Hunk hunk, boolean enabled) {
		if (enabled) {
			disabledElements.remove(hunk);
			FilePatch2 file = hunk.getParent();
			disabledElements.remove(file);
			DiffProject project = file.getProject();
			if (project != null)
				disabledElements.remove(project);
		} else {
			disabledElements.add(hunk);
			FilePatch2 file = hunk.getParent();
			if (disabledElements.containsAll(Arrays.asList(file.getHunks()))) {
				disabledElements.add(file);
				DiffProject project = file.getProject();
				if (project != null
						&& disabledElements.containsAll(Arrays.asList(project
								.getFileDiffs())))
					disabledElements.add(project);
			}
		}
	}

	public boolean isEnabled(Object element) {
		if (disabledElements.contains(element)) 
			return false;
		Object parent = getElementParent(element);
		if (parent == null)
			return true;
		return isEnabled(parent);
	}

	protected Object getElementParent(Object element) {
		if (element instanceof Hunk) {
			Hunk hunk = (Hunk) element;
			return hunk.getParent();
		}
		return null;
	}
	
	/**
	 * Calculate the fuzz factor that will allow the most hunks to be matched.
	 * @param monitor a progress monitor
	 * @return the fuzz factor or <code>-1</code> if no hunks could be matched
	 */
	public int guessFuzzFactor(IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.Patcher_2, IProgressMonitor.UNKNOWN);
			FilePatch2[] diffs= getDiffs();
			if (diffs==null||diffs.length<=0)
				return -1;
			int fuzz= -1;
			for (int i= 0; i<diffs.length; i++) {
				FilePatch2 d= diffs[i];
				IFile file= getTargetFile(d);
				if (file != null && file.exists()) {
					List lines= LineReader.load(file, false);
					FileDiffResult result = getDiffResult(d);
					int f = result.calculateFuzz(lines, monitor);
					if (f > fuzz)
						fuzz = f;
				}
			}
			return fuzz;
		} finally {
			monitor.done();
		}
	}
	
	public void refresh() {
		diffResults.clear();
		refresh(getDiffs());
	}
	
	public void refresh(FilePatch2[] diffs) {
		for (int i = 0; i < diffs.length; i++) {
			FilePatch2 diff = diffs[i];
			FileDiffResult result = getDiffResult(diff);
			((WorkspaceFileDiffResult)result).refresh();
		}
	}
	
	public FileDiffResult getDiffResult(FilePatch2 diff) {
		FileDiffResult result = (FileDiffResult)diffResults.get(diff);
		if (result == null) {
			result = new WorkspaceFileDiffResult(diff, getConfiguration());
			diffResults.put(diff, result);
		}
		return result;
	}

	public PatchConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the project that contains this diff or <code>null</code>
	 * if the patch is not a workspace patch.
	 * @param diff the diff
	 * @return the project that contains the diff
	 */
	public DiffProject getProject(FilePatch2 diff) {
		return diff.getProject();
	}

	/*
	 * Returns <code>true</code> if new value differs from old.
	 */
	public boolean setReversed(boolean reverse) {
		if (getConfiguration().isReversed() != reverse) {
			getConfiguration().setReversed(reverse);
			refresh();
			return true;
		}
		return false;
	}
	
	public boolean isReversed() {
		return getConfiguration().isReversed();
	}
	
	/**
	 * Cache the contents for the given file diff. These contents
	 * will be used for the diff when the patch is applied. When the
	 * patch is applied, it is assumed that the provided contents 
	 * already have all relevant hunks applied.
	 * @param diff the file diff
	 * @param contents the contents for the file diff
	 */
	public void cacheContents(FilePatch2 diff, byte[] contents) {
		contentCache.put(diff, contents);
	}
	
	/**
	 * Return whether contents have been cached for the 
	 * given file diff.
	 * @param diff the file diff
	 * @return whether contents have been cached for the file diff
	 * @see #cacheContents(FilePatch2, byte[])
	 */
	public boolean hasCachedContents(FilePatch2 diff) {
		return contentCache.containsKey(diff);
	}

	/**
	 * Return the content lines that are cached for the given 
	 * file diff.
	 * @param diff the file diff
	 * @return the content lines that are cached for the file diff
	 */
	public List getCachedLines(FilePatch2 diff) {
		byte[] contents = (byte[])contentCache.get(diff);
		if (contents != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents)));
			return LineReader.readLines(reader);
		}
		return null;
	}

	/**
	 * Return the contents that are cached for the given diff or
	 * <code>null</code> if there is no contents cached.
	 * @param diff the diff
	 * @return the contents that are cached for the given diff or
	 * <code>null</code>
	 */
	public byte[] getCachedContents(FilePatch2 diff) {
		return (byte[])contentCache.get(diff);
	}
	
	/**
	 * Return whether the patcher has any cached contents.
	 * @return whether the patcher has any cached contents
	 */
	public boolean hasCachedContents() {
		return !contentCache.isEmpty();
	}

	/**
	 * Clear any cached contents.
	 */
	public void clearCachedContents() {
		contentCache.clear();
		mergedHunks.clear();
	}
	
	public void setProperty(String key, Object value) {
		getConfiguration().setProperty(key, value);
	}
	
	public Object getProperty(String key) {
		return getConfiguration().getProperty(key);
	}

	public boolean isManuallyMerged(Hunk hunk) {
		return mergedHunks.contains(hunk);
	}

	public void setManuallyMerged(Hunk hunk, boolean merged) {
		if (merged)
			mergedHunks.add(hunk);
		else 
			mergedHunks.remove(hunk);
	}

	public IProject getTargetProject(FilePatch2 diff) {
		DiffProject dp = getProject(diff);
		if (dp != null)
			return Utilities.getProject(dp);
		IResource tr = getTarget();
		if (tr instanceof IWorkspaceRoot) {
			IWorkspaceRoot root = (IWorkspaceRoot) tr;
			return root.getProject(diff.getPath(isReversed()).segment(0));
		}
		return tr.getProject();
	}

	public static Patcher getPatcher(PatchConfiguration configuration) {
		return (Patcher)configuration.getProperty(PROP_PATCHER);
	}
	
	public boolean hasRejects() {
		for (Iterator iterator = diffResults.values().iterator(); iterator.hasNext();) {
			FileDiffResult result = (FileDiffResult) iterator.next();
			if (result.hasRejects())
				return true;
		}
		return false;
	}

	public boolean select(IHunk hunk) {
		return isEnabled(hunk);
	}
}
