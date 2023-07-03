/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.internal.events.ILifecycleListener;
import org.eclipse.core.internal.events.LifecycleEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class FixDotProjectPathResourceListener implements IResourceChangeListener, ILifecycleListener {

	private Map<IProject, Instant> createdProjects = new HashMap<>();
	private boolean enabled;
	private final File tmpDir = createTmpDir();

	private static File createTmpDir() {
		try {
			return Files.createTempDirectory(FixDotProjectPathResourceListener.class.getSimpleName()).toFile();
		} catch (Exception x) {
			return null;
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (!enabled || event.getDelta() == null) {
			return;
		}
		try {
			event.getDelta().accept(delta -> {
				if (delta.getResource() instanceof IProject project && //
					project.isOpen() && //
					createdProjects.containsKey(project)) {
					Instant projectCreation = createdProjects.remove(project);
					Path dotProjectOnDisk = project.getLocation().append(IProjectDescription.DESCRIPTION_FILE_NAME).toPath();
					try {
						BasicFileAttributes attributes = Files.getFileAttributeView(dotProjectOnDisk, BasicFileAttributeView.class).readAttributes();
						if (attributes.creationTime().toInstant().isAfter(projectCreation)) {
							// cannot link to resource in current workspace task, plan it next
							Job job = new Job("Link .project for " + project.getName()) {
								@Override
								public IStatus run(IProgressMonitor monitor) {
									try {
										project.getWorkspace().run((ICoreRunnable) pm -> {
											project.refreshLocal(IResource.DEPTH_INFINITE, pm);
											linkDotProject(project);
											project.refreshLocal(IResource.DEPTH_INFINITE, pm);
										}, project.getWorkspace().getRuleFactory().modifyRule(project.getParent()), IWorkspace.AVOID_UPDATE, monitor);
										return Status.OK_STATUS;
									} catch (CoreException ex) {
										return ex.getStatus();
									} 
								}
							};
							job.setRule(project.getWorkspace().getRuleFactory().modifyRule(project.getParent()));
							job.setPriority(Job.INTERACTIVE);
							job.schedule(0);
						}
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				}
				return delta.getResource().getType() == IResource.ROOT;
			});
		} catch (CoreException e) {
			ResourcesPlugin.getPlugin().getLog().log(e.getStatus());
		}
	}

	private void linkDotProject(IProject project) throws CoreException {
		if (!enabled) {
			return;
		}
		IFile dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		if (dotProject.isLinked()) {
			return;
		}
		File targetDiskFile = getMetaDataFilePath(project.getName(), dotProject.getProjectRelativePath()).toFile();
		if (!targetDiskFile.exists()) {
			try {
				targetDiskFile.getParentFile().mkdirs();
				Files.copy(dotProject.getLocation().toPath(), targetDiskFile.toPath());
			} catch (Exception ex) {
				throw new CoreException(Status.error(targetDiskFile + " cannot be created", ex)); //$NON-NLS-1$
			}
		}
		File sourceDiskFile = dotProject.getLocation().toFile();
		dotProject.createLink(IPath.fromFile(targetDiskFile), IResource.FORCE | IResource.REPLACE, null);
		sourceDiskFile.delete();
	}
	
	private IPath getMetaDataFilePath(String name, IPath projectRelativePath) {
		return IPath.fromFile(tmpDir).append(name).append(projectRelativePath);
	}

	@Override
	public void handleEvent(LifecycleEvent event) throws CoreException {
		if (event.kind == LifecycleEvent.PRE_PROJECT_CREATE) {
			if (event.resource instanceof IProject project) {
				createdProjects.put(project, Instant.now());
			}
		} else if (event.kind == LifecycleEvent.PRE_REFRESH) {
			unlinkIfLocal(event.resource);
		} else if (event.kind == LifecycleEvent.PRE_PROJECT_CLOSE) {
			// a workaround for testing: closed projects will try to re-read .project when reopen
			// this case is so far not fully supported and doesn't seem to be useful for users of
			// linked .project
			unlink(event.resource.getProject().getFile(IProjectDescription.DESCRIPTION_FILE_NAME));
		}
	}

	private void unlink(IFile file) {
		if (!file.isLinked()) {
			return;
		}
		File targetFile = file.getProject().getLocation().append(file.getProjectRelativePath()).toFile();
		if (Objects.equals(file.getLocationURI(), targetFile.toURI())) {
			return;
		}
		try (InputStream contents = file.getContents()) {
			Files.copy(contents, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | CoreException ex) {
			ex.printStackTrace();
		}
	}

	private void unlinkIfLocal(IResource resource) {
		if (resource instanceof IProject project && project.isOpen()) { 
			try {
				Arrays.stream(project.members())
					.filter(IFile.class::isInstance)
					.map(IFile.class::cast)
					.filter(IFile::isLinked)
					.filter(file -> file.getProject().getLocation().append(file.getProjectRelativePath()).toFile().isFile())
					.forEach(file -> {
						try {
							file.delete(false, false, null);
						} catch (CoreException e) {
						}
					});
			} catch (CoreException e) {
				ResourcesPlugin.getPlugin().getLog().log(e.getStatus());
			}
		}
	}

	public void enable() {
		this.enabled = true;
	}

	public void disable() {
		this.enabled = false;
	}

}
