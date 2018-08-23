/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.provider;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryManager;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class RepositoryRootTest extends EclipseTest {

	private RepositoryRoot repositoryRoot;
	private RepositoryManager repositoryManager;

	protected void setUp() throws Exception {
		super.setUp();
		repositoryManager = CVSUIPlugin.getPlugin().getRepositoryManager();
		repositoryRoot = repositoryManager
				.getRepositoryRootFor(getRepository());
		clearRepositoryRootCache();
	}

	public static Test suite() {
		return suite(RepositoryRootTest.class);
	}

	private void clearRepositoryRootCache() {
		String remotePaths[] = repositoryRoot.getKnownRemotePaths();
		for (int i = 0; i < remotePaths.length; i++) {
			repositoryRoot.removeTags(remotePaths[i],
					repositoryRoot.getAllKnownTags(remotePaths[i]));
		}
		assertEquals("Repository cache was not cleaned.", 0,
				repositoryRoot.getAllKnownTags().length);
	}

	private CVSTag[] refreshTags(IProject project) throws TeamException {
		return refreshTags(CVSWorkspaceRoot.getCVSFolderFor(project));
	}

	private CVSTag[] refreshTags(ICVSFolder folder) throws TeamException {
		return repositoryManager.refreshDefinedTags(folder, true, true,
				DEFAULT_MONITOR);
	}

	private IProject createProject(String baseName, String repoPrefix)
			throws CoreException {
		// create project
		IProject project = getUniqueTestProject(baseName);
		// share project under module
		shareProject(getRepository(), project,
				repoPrefix == null ? project.getName()
						: (repoPrefix + "/" + project.getName()),
				DEFAULT_MONITOR);
		assertValidCheckout(project);
		// add some files
		addResources(project, new String[] { "file1.txt" }, true);
		return project;
	}

	private void assertTags(List knownTags, CVSTag[] tagsToHave,
			CVSTag[] tagsNotToHave) {
		for (int i = 0; i < tagsToHave.length; i++) {
			assertTrue("Missing tag " + tagsToHave[i].getName(),
					knownTags.contains(tagsToHave[i]));
		}
		for (int i = 0; i < tagsNotToHave.length; i++) {
			assertFalse("Extraneous tag " + tagsNotToHave[i].getName(),
					knownTags.contains(tagsNotToHave[i]));
		}
	}

	private void assertProjectTags(CVSCacheTestData data) throws CVSException {
		// Root should contain all known tags
		List knownTags = Arrays.asList(repositoryRoot.getAllKnownTags());
		assertTags(knownTags,
				new CVSTag[] { data.branch_1, data.branch_2, data.branch_3,
						data.version_1, data.version_2, data.version_3 },
				new CVSTag[0]);

		// Project_1 should contain Branch_1 and Branch_2
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(data.project1)));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_2,
				data.version_1, data.version_2 }, new CVSTag[] { data.branch_3,
				data.version_3 });
		// Project_2 should contain Branch_1 and Branch_3
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(data.project2)));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_3,
				data.version_1, data.version_3 }, new CVSTag[] { data.branch_2,
				data.version_2 });
	}

	private class CVSCacheTestData {
		IProject project1; // tagged with Branch_1 and Branch_2
		IProject project2; // tagged with Branch_1 and Branch_3
		CVSTag branch_1;
		CVSTag branch_2;
		CVSTag branch_3;
		CVSTag version_1;
		CVSTag version_2;
		CVSTag version_3;

		private void init(String project1Path, String project2Path)
				throws CoreException {
			project1 = createProject("Project_1", project1Path);
			project2 = createProject("Project_2", project2Path);
			branch_1 = new CVSTag("Branch_1" + System.currentTimeMillis(),
					CVSTag.BRANCH);
			version_1 = new CVSTag("Root_" + branch_1.getName(), CVSTag.VERSION);
			branch_2 = new CVSTag("Branch_2" + System.currentTimeMillis(),
					CVSTag.BRANCH);
			version_2 = new CVSTag("Root_" + branch_2.getName(), CVSTag.VERSION);
			branch_3 = new CVSTag("Branch_3" + System.currentTimeMillis(),
					CVSTag.BRANCH);
			version_3 = new CVSTag("Root_" + branch_3.getName(), CVSTag.VERSION);

			makeBranch(new IResource[] { project1, project2 }, version_1,
					branch_1, true);
			makeBranch(new IResource[] { project1 }, version_2, branch_2, true);
			makeBranch(new IResource[] { project2 }, version_3, branch_3, true);
		}

		public CVSCacheTestData(String project1Path, String project2Path)
				throws CoreException {
			init(project1Path, project2Path);
		}
	}

	public void testProjectsAtRoot() throws CoreException {
		CVSCacheTestData data = new CVSCacheTestData(null, null);

		// verify that tags are correct after creating branches
		assertProjectTags(data);

		clearRepositoryRootCache();
		refreshTags(data.project1);
		refreshTags(data.project2);
		assertProjectTags(data);
	}

	public void testProjectsInSubmodule() throws CoreException {
		String submodule = "Submodule_1" + System.currentTimeMillis();
		CVSCacheTestData data = new CVSCacheTestData(submodule, submodule);
		ICVSFolder submoduleFolder = repositoryRoot.getRemoteFolder(submodule,
				null, getMonitor());

		// verify that tags are correct after creating branches
		assertProjectTags(data);

		clearRepositoryRootCache();
		refreshTags(data.project1);
		refreshTags(data.project2);
		assertProjectTags(data);
		// verify that parent module has tags from both projects
		List knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder));
		assertTags(knownTags,
				new CVSTag[] { data.branch_1, data.branch_2, data.branch_3,
						data.version_1, data.version_2, data.version_3 },
				new CVSTag[0]);

		clearRepositoryRootCache();
		refreshTags(submoduleFolder);
		assertProjectTags(data);
		// verify that parent module has tags from both projects
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder));
		assertTags(knownTags,
				new CVSTag[] { data.branch_1, data.branch_2, data.branch_3,
						data.version_1, data.version_2, data.version_3 },
				new CVSTag[0]);
	}

	public void testProjectsInTwoSubmodules() throws CoreException {
		String submodule1 = "Submodule_1" + System.currentTimeMillis();
		String submodule2 = "Submodule_2" + System.currentTimeMillis();
		CVSCacheTestData data = new CVSCacheTestData(submodule1, submodule2);
		ICVSFolder submoduleFolder1 = repositoryRoot.getRemoteFolder(
				submodule1, null, getMonitor());
		ICVSFolder submoduleFolder2 = repositoryRoot.getRemoteFolder(
				submodule2, null, getMonitor());

		// verify that tags are correct after creating branches
		assertProjectTags(data);

		clearRepositoryRootCache();
		refreshTags(data.project1);
		refreshTags(data.project2);
		assertProjectTags(data);
		// verify that parent modules have tags from subordinate project, but
		// not the other project
		List knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder1));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_2,
				data.version_1, data.version_2 }, new CVSTag[] { data.branch_3,
				data.version_3 });
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder2));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_3,
				data.version_1, data.version_3 }, new CVSTag[] { data.branch_2,
				data.version_2 });
		// clear the cache, refresh it only one for submodule and verify if tags
		// are correct
		clearRepositoryRootCache();
		refreshTags(submoduleFolder1);

		// verify that correct tags where added to this submodule
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder1));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_2,
				data.version_1, data.version_2 }, new CVSTag[] { data.branch_3,
				data.version_3 });
		// verify if only tags from the first submodule are known
		knownTags = Arrays.asList(repositoryRoot.getAllKnownTags());
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_2,
				data.version_1, data.version_2 }, new CVSTag[] { data.branch_3,
				data.version_3 });

		refreshTags(submoduleFolder2);
		// verify tags for the second submodule
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(submoduleFolder2));
		assertTags(knownTags, new CVSTag[] { data.branch_1, data.branch_3,
				data.version_1, data.version_3 }, new CVSTag[] { data.branch_2,
				data.version_2 });
		// verify if tags are merged correctly
		assertProjectTags(data);
	}

	public void testNestedProjects() throws CoreException {
		IProject superProject = createProject("SuperProject", (String) null);
		CVSTag superProjectBranch = new CVSTag("Branch_"
				+ superProject.getName(), CVSTag.BRANCH);
		CVSTag superProjectVersion = new CVSTag("Root_"
				+ superProjectBranch.getName(), CVSTag.VERSION);
		makeBranch(new IResource[] { superProject }, superProjectVersion,
				superProjectBranch, true);
		// subProject1 and subProject2 are nested in superProject
		// each of them has its own tags
		IProject subProject1 = createProject("SubProject_1",
				superProject.getName());
		CVSTag subProject1Branch = new CVSTag(
				"Branch_" + subProject1.getName(), CVSTag.BRANCH);
		CVSTag subProject1Version = new CVSTag("Root_"
				+ subProject1Branch.getName(), CVSTag.VERSION);
		makeBranch(new IResource[] { subProject1 }, subProject1Version,
				subProject1Branch, true);
		IProject subProject2 = createProject("SubProject_2",
				superProject.getName());
		CVSTag subProject2Branch = new CVSTag(
				"Branch_" + subProject2.getName(), CVSTag.BRANCH);
		CVSTag subProject2Version = new CVSTag("Root_"
				+ subProject2Branch.getName(), CVSTag.VERSION);
		makeBranch(new IResource[] { subProject2 }, subProject2Version,
				subProject2Branch, true);
		// check if subProjects have tags from superProject but not tags from
		// each other, check if superProject has tags from all subProjects
		List knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(superProject)));
		assertTags(knownTags, new CVSTag[] { superProjectBranch,
				superProjectVersion, subProject1Branch, subProject1Version,
				subProject2Branch, subProject2Version }, new CVSTag[0]);
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(subProject1)));
		assertTags(knownTags, new CVSTag[] { superProjectBranch,
				superProjectVersion, subProject1Branch, subProject1Version },
				new CVSTag[] { subProject2Branch, subProject2Version });
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(subProject2)));
		assertTags(knownTags, new CVSTag[] { superProjectBranch,
				superProjectVersion, subProject2Branch, subProject2Version },
				new CVSTag[] { subProject1Branch, subProject1Version });
		// remove tag from one of the subProjects, check if it was not removed
		// from the other subProject, check if superProject still has this tag
		repositoryManager.removeTags(
				CVSWorkspaceRoot.getCVSFolderFor(subProject1), new CVSTag[] {
						superProjectBranch, superProjectVersion });
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(superProject)));
		assertTags(knownTags, new CVSTag[] { superProjectBranch,
				superProjectVersion, subProject1Branch, subProject1Version,
				subProject2Branch, subProject2Version }, new CVSTag[0]);
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(subProject1)));
		assertTags(knownTags, new CVSTag[] { subProject1Branch,
				subProject1Version }, new CVSTag[] { subProject2Branch,
				subProject2Version });
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(subProject2)));
		assertTags(knownTags, new CVSTag[] { superProjectBranch,
				superProjectVersion, subProject2Branch, subProject2Version },
				new CVSTag[] { subProject1Branch, subProject1Version });
	}

	public void testTagsForSubfolder() throws TeamException, CoreException {
		IProject project = createProject("Project_1", (String) null);
		String folderName = "testFolder";
		addResources(project, new String[] { folderName + "/",
				folderName + "/testFile.txt" }, true);
		IFolder folder = project.getFolder(folderName);
		CVSTag branch1 = new CVSTag("Branch_1" + System.currentTimeMillis(),
				CVSTag.BRANCH);
		CVSTag version1 = new CVSTag("Root_" + branch1.getName(),
				CVSTag.VERSION);
		makeBranch(new IResource[] { project }, version1, branch1, true);
		// verify if project's tags are known for subfolder
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
		List knownTags = Arrays.asList(repositoryManager
				.getKnownTags(cvsFolder));
		assertTags(knownTags, new CVSTag[] { branch1, version1 }, new CVSTag[0]);
		// verify if removing tags from subfolder is correctly handled
		repositoryManager.removeTags(cvsFolder, new CVSTag[] { branch1,
				version1 });
		knownTags = Arrays.asList(repositoryManager.getKnownTags(cvsFolder));
		assertTags(knownTags, new CVSTag[0], new CVSTag[] { branch1, version1 });
		// verify if after refreshing tags are back for both project and its
		// subfolder
		refreshTags(cvsFolder);
		knownTags = Arrays.asList(repositoryManager.getKnownTags(cvsFolder));
		assertTags(knownTags, new CVSTag[] { branch1, version1 }, new CVSTag[0]);
		knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(project)));
		assertTags(knownTags, new CVSTag[] { branch1, version1 }, new CVSTag[0]);
	}

	public void testRefreshProjectUsingAutoRefreshFile() throws CoreException {
		IProject project = createProject("Project_1", (String) null);
		String autoRefreshFileName = "sampleAuthoRefresh.txt";
		String notAutoRefreshFileName = "notAutoRefresh.txt";
		addResources(project, new String[] { autoRefreshFileName,
				notAutoRefreshFileName }, true);
		IFile autoRefreshFile = project.getFile(autoRefreshFileName);
		IFile notAutoRefreshFile = project.getFile(notAutoRefreshFileName);
		repositoryManager.setAutoRefreshFiles(
				CVSWorkspaceRoot.getCVSFolderFor(project),
				new String[] { CVSWorkspaceRoot.getCVSResourceFor(
						autoRefreshFile).getRepositoryRelativePath() });
		CVSTag branch1 = new CVSTag("Branch_1" + System.currentTimeMillis(),
				CVSTag.BRANCH);
		CVSTag version1 = new CVSTag("Root_" + branch1.getName(),
				CVSTag.VERSION);
		// branch the auto refresh file
		makeBranch(new IResource[] { autoRefreshFile }, version1, branch1, true);
		CVSTag branch2 = new CVSTag("Branch_2" + System.currentTimeMillis(),
				CVSTag.BRANCH);
		CVSTag version2 = new CVSTag("Root_" + branch2.getName(),
				CVSTag.VERSION);
		// branch not auto refresh file
		makeBranch(new IResource[] { notAutoRefreshFile }, version2, branch2,
				true);

		clearRepositoryRootCache();
		refreshTags(project);

		// cache should contain branches from auto refresh file, but no branches
		// from other files
		List knownTags = Arrays.asList(repositoryManager
				.getKnownTags(CVSWorkspaceRoot.getCVSFolderFor(project)));
		assertTags(knownTags, new CVSTag[] { branch1, version1 }, new CVSTag[] {
				branch2, version2 });
	}

	public void testProjectWithNoTags() throws CoreException {
		IProject project = createProject("Project_1", (String) null);
		CVSTag knownTags[] = repositoryManager.getKnownTags(CVSWorkspaceRoot
				.getCVSFolderFor(project));
		assertEquals(0, knownTags.length);
		knownTags = refreshTags(project);
		assertEquals(0, knownTags.length);
	}

	public void testDateTags() throws CoreException {
		CVSTag dateTag = new CVSTag(new Date());
		repositoryManager.addDateTag(getRepository(), dateTag);
		// verify if date tags are returned by repository manager
		CVSTag[] dateTags = repositoryManager.getDateTags(getRepository());
		assertEquals(1, dateTags.length);
		assertEquals(dateTag, dateTags[0]);
		dateTags = repositoryManager.getKnownTags(getRepository(), CVSTag.DATE);
		assertEquals(1, dateTags.length);
		assertEquals(dateTag, dateTags[0]);
		// verify if date tags are returned in list of known tags for every
		// project
		IProject project = createProject("Project_1", (String) null);
		CVSTag[] allTags = repositoryManager.getKnownTags(CVSWorkspaceRoot
				.getCVSFolderFor(project));
		assertEquals(1, allTags.length);
		assertEquals(dateTag, allTags[0]);
	}
}
