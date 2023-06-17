/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.compare.internal.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.internal.core.Messages;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.core.patch.PatchReader;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;

/**
 * A Patcher
 * - knows how to parse various patch file formats into some in-memory structure,
 * - holds onto the parsed data and the options to use when applying the patches,
 * - knows how to apply the patches to files and folders.
 */
public class WorkspacePatcher extends Patcher {

	private DiffProject[] fDiffProjects;
	private boolean fIsWorkspacePatch= false;
	private boolean fIsGitPatch = false;
	private final Map<Object, IPath> retargetedDiffs = new HashMap<>();

	public WorkspacePatcher() {
		// nothing to do
	}

	public WorkspacePatcher(IResource target) {
		setTarget(target);
	}

	@Override
	protected void patchParsed(PatchReader patchReader) {
		super.patchParsed(patchReader);
		fDiffProjects = patchReader.getDiffProjects();
		fIsWorkspacePatch = patchReader.isWorkspacePatch();
		fIsGitPatch = patchReader.isGitPatch() && calculateStripGitPrefixSegments() > -1;
	}

	public DiffProject[] getDiffProjects() {
		return fDiffProjects;
	}

	public boolean isWorkspacePatch() {
		return fIsWorkspacePatch;
	}

	public boolean isGitPatch() {
		return fIsGitPatch;
	}

	//---- parsing patch files

	@Override
	public void applyAll(IProgressMonitor pm, IFileValidator validator) throws CoreException {
		if (pm == null)
			pm = new NullProgressMonitor();
		if (!fIsWorkspacePatch) {
			super.applyAll(pm, validator);
		} else {
			final int WORK_UNIT= 10;

			// get all files to be modified in order to call validateEdit
			List<IFile> list= new ArrayList<>();
			for (DiffProject diffProject : fDiffProjects) {
				if (Utilities.getProject(diffProject).isAccessible())
					list.addAll(Arrays.asList(getTargetFiles(diffProject)));
			}
			// validate the files for editing
			if (!validator.validateResources(list.toArray(new IFile[list.size()]))) {
				return;
			}

			FilePatch2[] diffs = getDiffs();
			if (pm != null) {
				String message= Messages.WorkspacePatcher_0;
				pm.beginTask(message, diffs.length * WORK_UNIT);
			}

			for (FilePatch2 diff : diffs) {
				int workTicks= WORK_UNIT;
				if (isAccessible(diff)) {
					IFile file= getTargetFile(diff);
					IPath path= file.getProjectRelativePath();
					if (pm != null)
						pm.subTask(path.toString());
					createPath(file.getProject(), path);

					List<Hunk> failed= new ArrayList<>();

					int type= diff.getDiffType(isReversed());
					switch (type) {
						case FilePatch2.ADDITION :
							// patch it and collect rejected hunks
							List<String> result= apply(diff, file, true, failed);
							if (result != null)
								store(LineReader.createString(isPreserveLineDelimeters(), result), file, SubMonitor.convert(pm, workTicks));
							workTicks -= WORK_UNIT;
							break;
						case FilePatch2.DELETION :
							file.delete(true, true, SubMonitor.convert(pm, workTicks));
							workTicks -= WORK_UNIT;
							break;
						case FilePatch2.CHANGE :
							// patch it and collect rejected hunks
							result= apply(diff, file, false, failed);
							if (result != null)
								store(LineReader.createString(isPreserveLineDelimeters(), result), file, SubMonitor.convert(pm, workTicks));
							workTicks -= WORK_UNIT;
							break;
					}

					if (isGenerateRejectFile() && failed.size() > 0) {
						IPath pp= null;
						if (path.segmentCount() > 1) {
							pp= path.removeLastSegments(1);
							pp= pp.append(path.lastSegment() + REJECT_FILE_EXTENSION);
						} else
							pp= IPath.fromOSString(path.lastSegment() + REJECT_FILE_EXTENSION);
						file= createPath(file.getProject(), pp);
						if (file != null) {
							store(getRejected(failed), file, pm);
							try {
								IMarker marker= file.createMarker(MARKER_TYPE);
								marker.setAttribute(IMarker.MESSAGE, Messages.WorkspacePatcher_1);
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
	}

	private boolean isAccessible(FilePatch2 diff) {
		return isEnabled(diff) && Utilities.getProject(diff.getProject()).isAccessible();
	}

	/**
	 * Returns the target files of all the Diffs contained by this
	 * DiffProject.
	 * @param project
	 * @return An array of IFiles that are targeted by the Diffs
	 */
	public IFile[] getTargetFiles(DiffProject project) {
		List<IFile> files= new ArrayList<>();
		FilePatch2[] diffs = project.getFileDiffs();
		for (FilePatch2 diff : diffs) {
			if (isEnabled(diff)) {
				files.add(getTargetFile(diff));
			}
		}
		return files.toArray(new IFile[files.size()]);
	}

	@Override
	public IFile getTargetFile(FilePatch2 diff) {
		IPath path = diff.getStrippedPath(getStripPrefixSegments(), isReversed());
		DiffProject project = getProject(diff);
		if (project != null)
			return Utilities.getProject(project).getFile(path);
		return super.getTargetFile(diff);
	}

	private IPath getFullPath(FilePatch2 diff) {
		IPath path = diff.getStrippedPath(getStripPrefixSegments(), isReversed());
		DiffProject project = getProject(diff);
		if (project != null)
			return Utilities.getProject(project).getFile(path).getFullPath();
		return getTarget().getFullPath().append(path);
	}

	public ISchedulingRule[] getTargetProjects() {
		List<ISchedulingRule> projects= new ArrayList<>();
		IResourceRuleFactory ruleFactory= ResourcesPlugin.getWorkspace().getRuleFactory();
		// Determine the appropriate scheduling rules
		for (DiffProject diffProject : fDiffProjects) {
			IProject tempProject = Utilities.getProject(diffProject);
			// The goal here is to lock as little of the workspace as necessary
			// but still allow the patcher to obtain the locks it needs.
			// As such, we need to get the modify rules from the rule factory for the .project file. A pessimistic
			// rule factory will return the root, while others might return just the project. Combining
			// this rule with the project will result in the smallest possible locking set.
			ISchedulingRule scheduleRule= ruleFactory.modifyRule(tempProject.getFile(IProjectDescription.DESCRIPTION_FILE_NAME));
			MultiRule multiRule= new MultiRule(new ISchedulingRule[] { scheduleRule, tempProject } );
			projects.add(multiRule);
		}

		return projects.toArray(new ISchedulingRule[projects.size()]);
	}

	public void setDiffProjects(DiffProject[] newProjectArray) {
		fDiffProjects = new DiffProject[newProjectArray.length];
		System.arraycopy(newProjectArray,0, fDiffProjects, 0, newProjectArray.length);
	}

	public void removeProject(DiffProject project) {
		DiffProject[] temp = new DiffProject[fDiffProjects.length - 1];
		int counter = 0;
		for (DiffProject diffProject : fDiffProjects) {
			if (diffProject != project) {
				temp[counter++] = diffProject;
			}
		}
		fDiffProjects = temp;
	}

	@Override
	protected Object getElementParent(Object element) {
		if (element instanceof FilePatch2 && fDiffProjects != null) {
			FilePatch2 diff = (FilePatch2) element;
			for (DiffProject project : fDiffProjects) {
				if (project.contains(diff))
					return project;
			}
		}
		return null;
	}

	public boolean isRetargeted(Object object) {
		return retargetedDiffs.containsKey(object);
	}

	public IPath getOriginalPath(Object object) {
		return retargetedDiffs.get(object);
	}

	public void retargetDiff(FilePatch2 diff, IFile file) {
		retargetedDiffs.put(diff, diff.getPath(false));
		IHunk[] hunks = diff.getHunks();

		if (isWorkspacePatch()){
			//since the diff has no more hunks to apply, remove it from the parent and the patcher
			diff.getProject().remove(diff);
		}
		removeDiff(diff);
		FilePatch2 newDiff = getDiffForFile(file);
		for (IHunk h : hunks) {
			Hunk hunk = (Hunk) h;
			newDiff.add(hunk);
		}
	}

	private FilePatch2 getDiffForFile(IFile file) {
		DiffProject diffProject = null;
		FilePatch2[] diffsToCheck;
		if (isWorkspacePatch()){
			// Check if the diff project already exists for the file
			IProject project = file.getProject();
			DiffProject[] diffProjects = getDiffProjects();
			for (DiffProject d : diffProjects) {
				if (Utilities.getProject(d).equals(project)) {
					diffProject = d;
					break;
				}
			}
			// If the project doesn't exist yet, create it and add it to the project list
			if (diffProject == null){
				diffProject = addDiffProjectForProject(project);
			}
			diffsToCheck = diffProject.getFileDiffs();
		} else {
			diffsToCheck = getDiffs();
		}
		// Check to see if a diff already exists for the file
		for (FilePatch2 fileDiff : diffsToCheck) {
			if (isDiffForFile(fileDiff, file)) {
				return fileDiff;
			}
		}

		// Create a new diff for the file
		IPath path = getDiffPath(file);
		FilePatch2 newDiff = new FilePatch2(path, 0, path, 0);
		if (diffProject != null){
			diffProject.add(newDiff);
		}
		addDiff(newDiff);
		return newDiff;
	}

	private IPath getDiffPath(IFile file) {
		DiffProject project = getDiffProject(file.getProject());
		if (project != null) {
			return file.getProjectRelativePath();
		}
		return file.getFullPath().removeFirstSegments(getTarget().getFullPath().segmentCount());
	}

	private boolean isDiffForFile(FilePatch2 fileDiff, IFile file) {
		return getFullPath(fileDiff).equals(file.getFullPath());
	}

	private DiffProject addDiffProjectForProject(IProject project) {
		DiffProject[] diffProjects = getDiffProjects();
		DiffProject diffProject = new DiffProject(project.getName());
		DiffProject[] newProjectArray = new DiffProject[diffProjects.length + 1];
		System.arraycopy(diffProjects, 0, newProjectArray, 0, diffProjects.length);
		newProjectArray[diffProjects.length] = diffProject;
		setDiffProjects(newProjectArray);
		return diffProject;
	}

	public void retargetHunk(Hunk hunk, IFile file) {
		FilePatch2 newDiff = getDiffForFile(file);
		newDiff.add(hunk);
	}

	public void retargetProject(DiffProject project, IProject targetProject) {
		retargetedDiffs.put(project, Utilities.getProject(project).getFullPath());
		FilePatch2[] diffs = project.getFileDiffs();
		DiffProject selectedProject = getDiffProject(targetProject);
		if (selectedProject == null)
			selectedProject = addDiffProjectForProject(targetProject);
		// Copy over the diffs to the new project
		for (FilePatch2 diff : diffs) {
			selectedProject.add(diff);
		}
		// Since the project has been retargeted, remove it from the patcher
		removeProject(project);
	}

	/**
	 * Return the diff project for the given project
	 * or <code>null</code> if the diff project doesn't exist
	 * or if the patch is not a workspace patch.
	 * @param project the project
	 * @return the diff project for the given project
	 * or <code>null</code>
	 */
	private DiffProject getDiffProject(IProject project) {
		if (!isWorkspacePatch())
			return null;
		DiffProject[] projects = getDiffProjects();
		for (DiffProject p : projects) {
			if (Utilities.getProject(p).equals(project)) {
				return p;
			}
		}
		return null;
	}

	@Override
	public int getStripPrefixSegments() {
		// Segments are never stripped from a workspace patch
		if (isWorkspacePatch())
			return 0;
		return super.getStripPrefixSegments();
	}

	int calculateStripGitPrefixSegments() {
		FilePatch2[] diffs = getDiffs();
		if (diffs.length == 0)
			return -1;
		int skip = -1;
		for (FilePatch2 diff : diffs) {
			IPath oldPath = diff.getPath(false);
			IPath newPath = diff.getPath(true);
			if (checkFirstSegments(new IPath[] { oldPath, newPath },
					new String[][] { { "a", "b" }, // change //$NON-NLS-1$ //$NON-NLS-2$
							{ "b", "b" }, // addition //$NON-NLS-1$ //$NON-NLS-2$
							{ "a", "a" } }) // deletion //$NON-NLS-1$ //$NON-NLS-2$
					&& oldPath.segmentCount() > 2 && newPath.segmentCount() > 2) {
				for (int j = 1; j < Math.min(oldPath.segmentCount(),
						newPath.segmentCount()); j++) {
					if (projectExists(oldPath.segment(j))
							|| projectExists(newPath.segment(j))) {
						if (skip == -1)
							skip = j;
						else if (skip != j)
							return -1; // a different number of segments to be
										// skipped, abort
						break;
					}
				}
			} else
				return -1; // not a git diff or custom prefixes used
		}
		return skip;
	}

	private boolean checkFirstSegments(IPath[] paths, String[][] segments) {
		SEGMENTS: for (String[] segment : segments) {
			for (int j = 0; j < paths.length; j++) {
				if (!paths[j].segment(0).equals(segment[j])) {
					continue SEGMENTS;
				}
			}
			return true;
		}
		return false;
	}

	private boolean projectExists(final String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists();
	}
}
