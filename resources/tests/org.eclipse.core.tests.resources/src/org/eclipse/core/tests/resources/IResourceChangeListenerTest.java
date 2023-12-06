/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 * 		tammo.freese@offis.de - tests for swapping files and folders
 ******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.function.ThrowingRunnable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * Tests behavior of IResourceChangeListener, including validation
 * that correct deltas are received for all types of workspace changes.
 */
public class IResourceChangeListenerTest extends ResourceTest {
	static class SimpleListener implements IResourceChangeListener {
		Object source;
		int trigger;

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			source = event.getSource();
			trigger = event.getBuildKind();
		}
	}

	private static final ThrowingRunnable NOOP_RUNNABLE = () -> {
	};
	protected static final String VERIFIER_NAME = "TestListener";
	IFile file1; //below folder1
	IFile file2; //below folder1
	IFile file3; //below folder2
	IFolder folder1; //below project2
	IFolder folder2; //below folder1
	IFolder folder3; //same as file1
	IFolder settings; // .settings
	/* some random resource handles */
	IProject project1;
	IFile project1MetaData;
	IFile prefs; // org.eclipse.core.resources.prefs
	IProject project2;
	IFile project2MetaData;
	ResourceDeltaVerifier verifier;

	public void testBenchMark_1GBYQEZ() throws Throwable {
		// start with a clean workspace
		getWorkspace().removeResourceChangeListener(verifier);
		getWorkspace().getRoot().delete(false, createTestMonitor());

		final AtomicReference<CoreException> exceptionInListener = new AtomicReference<>();
		// create the listener
		IResourceChangeListener listener = new IResourceChangeListener() {
			private int fCounter;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					System.out.println("Start");
					for (int i = 0; i < 10; i++) {
						fCounter = 0;
						long start = System.currentTimeMillis();
						IResourceDelta delta = event.getDelta();
						delta.accept(delta2 -> {
							fCounter++;
							return true;
						});
						long end = System.currentTimeMillis();
						System.out.println("    Number of deltas: " + fCounter + ". Time needed: " + (end - start));
					}
					System.out.println("End");
				} catch (CoreException e) {
					exceptionInListener.set(e);
				}
			}
		};
		// add the listener
		getWorkspace().addResourceChangeListener(listener);
		// setup the test data
		IWorkspaceRunnable body = monitor -> {
			IProject project = getWorkspace().getRoot().getProject("Test");
			IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
			IPath root = getWorkspace().getRoot().getLocation();
			IPath contents = root.append("temp/testing");
			deleteOnTearDown(root.append("temp"));
			description.setLocation(contents);
			project.create(description, createTestMonitor());
			project.open(createTestMonitor());
			project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		};
		getWorkspace().run(body, createTestMonitor());

		// touch all resources (so that they appear in the delta)
		body = monitor -> {
			IResourceVisitor visitor = resource -> {
				resource.touch(createTestMonitor());
				return true;
			};
			getWorkspace().getRoot().accept(visitor);
		};
		getWorkspace().run(body, createTestMonitor());

		// un-register our listener
		getWorkspace().removeResourceChangeListener(listener);
		if (exceptionInListener.get() != null) {
			throw exceptionInListener.get();
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void assertDelta() {
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Asserts that a manual traversal of the delta does not find the given
	 * resources.
	 */
	void assertNotDeltaIncludes(IResourceDelta delta, IResource[] resources) {
		IResource deltaResource = delta.getResource();
		for (IResource resource : resources) {
			assertTrue(!deltaResource.equals(resource));
		}
		IResourceDelta[] children = delta.getAffectedChildren();
		for (IResourceDelta element : children) {
			assertNotDeltaIncludes(element, resources);
		}
	}

	/**
	 * Asserts that a visitor traversal of the delta does not find the given
	 * resources.
	 */
	void assertNotDeltaVisits(IResourceDelta delta, final IResource[] resources) throws CoreException {
		delta.accept(delta2 -> {
			IResource deltaResource = delta2.getResource();
			for (IResource resource : resources) {
				assertTrue(!deltaResource.equals(resource));
			}
			return true;
		});
	}

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Create some resource handles
		project1 = getWorkspace().getRoot().getProject("Project" + 1);
		project2 = getWorkspace().getRoot().getProject("Project" + 2);
		folder1 = project1.getFolder("Folder" + 1);
		folder2 = folder1.getFolder("Folder" + 2);
		folder3 = folder1.getFolder("File" + 1);
		settings = project1.getFolder(".settings");
		prefs = settings.getFile("org.eclipse.core.resources.prefs");
		file1 = folder1.getFile("File" + 1);
		file2 = folder1.getFile("File" + 2);
		file3 = folder2.getFile("File" + 1);
		project1MetaData = project1.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		project2MetaData = project2.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		// Create and open a project, folder and file
		IWorkspaceRunnable body = monitor -> {
			project1.create(createTestMonitor());
			project1.open(createTestMonitor());
			folder1.create(true, true, createTestMonitor());
			file1.create(getRandomContents(), true, createTestMonitor());
		};
		verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);
		getWorkspace().run(body, createTestMonitor());

		//ensure all background jobs are done before we reset the delta verifier
		waitForBuild();
		waitForRefresh();
		verifier.reset();
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	@Override
	protected void tearDown() throws Exception {
		getWorkspace().removeResourceChangeListener(verifier);
		super.tearDown();
	}

	/*
	 * Create a resource change listener and register it for POST_BUILD
	 * events. Ensure that you are able to modify the workspace tree.
	 */
	public void test_1GDK9OG() throws Throwable {
		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		// create the resource change listener
		IResourceChangeListener listener = event -> {
			try {
				IWorkspaceRunnable body = monitor -> {
					// modify the tree.
					IResourceDeltaVisitor visitor = delta -> {
						IResource resource = delta.getResource();
						try {
							resource.touch(createTestMonitor());
						} catch (RuntimeException e) {
							throw e;
						}
						resource.createMarker(IMarker.PROBLEM);
						return true;
					};
					event.getDelta().accept(visitor);
				};
				getWorkspace().run(body, createTestMonitor());
			} catch (CoreException e) {
				listenerInMainThreadCallback.set(() -> {
					throw e;
				});
			}
		};
		// register the listener with the workspace.
		getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_BUILD);
		try {
			IWorkspaceRunnable body = new IWorkspaceRunnable() {
				// cause a delta by touching all resources
				final IResourceVisitor visitor = resource -> {
					resource.touch(createTestMonitor());
					return true;
				};

				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					getWorkspace().getRoot().accept(visitor);
				}
			};
			getWorkspace().run(body, createTestMonitor());
			//wait for autobuild so POST_BUILD will fire
			try {
				Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
			} catch (OperationCanceledException | InterruptedException e) {
				//ignore
			}
		} finally {
			// cleanup: ensure that the listener is removed
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	public void testAddAndRemoveFile() throws CoreException {
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and deleting", 100);
			try {
				file2.create(getRandomContents(), true, SubMonitor.convert(m, 50));
				file2.delete(true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		// should not have been verified since there was no change
		assertTrue("Unexpected notification on no change", !verifier.hasBeenNotified());
	}

	public void testAddAndRemoveFolder() throws CoreException {
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and deleting", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				folder2.delete(true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		// should not have been verified since there was no change
		assertTrue("Unexpected notification on no change", !verifier.hasBeenNotified());
	}

	public void testAddFile() throws CoreException {
		verifier.addExpectedChange(file2, IResourceDelta.ADDED, 0);
		file2.create(getRandomContents(), true, createTestMonitor());
		assertDelta();
	}

	public void testAddFileAndFolder() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, 0);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating folder and file", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file3.create(getRandomContents(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testAddFolder() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		folder2.create(true, true, createTestMonitor());
		assertDelta();
	}

	public void testAddProject() throws CoreException {
		verifier.addExpectedChange(project2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(project2MetaData, IResourceDelta.ADDED, 0);
		project2.create(createTestMonitor());
		assertDelta();
	}

	/*
	 * Create a resource change listener and register it for POST_CHANGE events.
	 * Ensure that you are NOT able to modify the workspace tree.
	 */
	public void testBug45996() throws Throwable {
		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		// create the resource change listener
		IResourceChangeListener listener = event -> {
			try {
				IWorkspaceRunnable body = monitor -> {
					// modify the tree.
					IResourceDeltaVisitor visitor = delta -> {
						IResource resource = delta.getResource();
						try {
							resource.touch(createTestMonitor());
						} catch (RuntimeException e) {
							throw e;
						}
						resource.createMarker(IMarker.PROBLEM);
						return true;
					};
					event.getDelta().accept(visitor);
				};
				getWorkspace().run(body, createTestMonitor());
			} catch (CoreException e) {
				return;
			}
			listenerInMainThreadCallback.set(() -> fail("1.0"));
		};
		// register the listener with the workspace.
		getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
		try {
			IWorkspaceRunnable body = new IWorkspaceRunnable() {
				// cause a delta by touching all resources
				final IResourceVisitor visitor = resource -> {
					resource.touch(createTestMonitor());
					return true;
				};

				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					getWorkspace().getRoot().accept(visitor);
				}
			};
			getWorkspace().run(body, createTestMonitor());
		} finally {
			// cleanup: ensure that the listener is removed
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	public void testBuildKind() throws CoreException {
		SimpleListener preBuild = new SimpleListener();
		SimpleListener postBuild = new SimpleListener();
		SimpleListener postChange = new SimpleListener();
		final IWorkspace workspace = getWorkspace();
		try {
			setAutoBuilding(false);
			workspace.addResourceChangeListener(preBuild, IResourceChangeEvent.PRE_BUILD);
			workspace.addResourceChangeListener(postBuild, IResourceChangeEvent.POST_BUILD);
			workspace.addResourceChangeListener(postChange, IResourceChangeEvent.POST_CHANGE);

			final int[] triggers = new int[] {IncrementalProjectBuilder.INCREMENTAL_BUILD, IncrementalProjectBuilder.FULL_BUILD, IncrementalProjectBuilder.CLEAN_BUILD,};
			for (int i = 0; i < triggers.length; i++) {
				final int trigger = triggers[i];
				workspace.run((IWorkspaceRunnable) monitor -> {
					file1.touch(null);
					workspace.build(trigger, monitor);
				}, createTestMonitor());
				assertEquals("1.0." + i, workspace, preBuild.source);
				assertEquals("1.1." + i, workspace, postBuild.source);
				assertEquals("1.2." + i, workspace, postChange.source);
				assertEquals("1.3." + i, trigger, preBuild.trigger);
				assertEquals("1.4." + i, trigger, postBuild.trigger);
				assertEquals("1.5." + i, 0, postChange.trigger);

				workspace.run((IWorkspaceRunnable) monitor -> {
					file1.touch(null);
					project1.build(trigger, createTestMonitor());
				}, createTestMonitor());
				assertEquals("2.0." + i, project1, preBuild.source);
				assertEquals("2.2." + i, project1, postBuild.source);
				assertEquals("2.2." + i, workspace, postChange.source);
				assertEquals("2.3." + i, trigger, preBuild.trigger);
				assertEquals("2.4." + i, trigger, postBuild.trigger);
				assertEquals("2.5." + i, 0, postChange.trigger);

			}

			//test autobuild trigger
			setAutoBuilding(true);
			file1.touch(null);
			waitForBuild();
			int trigger = IncrementalProjectBuilder.AUTO_BUILD;
			assertEquals("1.0", workspace, preBuild.source);
			assertEquals("1.1", workspace, postBuild.source);
			assertEquals("1.2", workspace, postChange.source);
			assertEquals("1.3", trigger, preBuild.trigger);
			assertEquals("1.4", trigger, postBuild.trigger);
			assertEquals("1.5", 0, postChange.trigger);

		} finally {
			workspace.removeResourceChangeListener(preBuild);
			workspace.removeResourceChangeListener(postBuild);
			workspace.removeResourceChangeListener(postChange);
		}
	}

	public void testChangeFile() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		file1.setContents(getRandomContents(), true, false, createTestMonitor());
		assertDelta();
	}

	/**
	 * Checks that even with autobuild disabled,
	 * {@code IResourceChangeEvent.PRE_BUILD} and
	 * {@code IResourceChangeEvent.POST_BUILD} are fired.
	 */
	public void testTouchFileWithAutobuildOff() throws Exception {
		SimpleListener preBuild = new SimpleListener();
		SimpleListener postBuild = new SimpleListener();
		final IWorkspace workspace = getWorkspace();
		try {
			setAutoBuilding(false);

			workspace.addResourceChangeListener(preBuild, IResourceChangeEvent.PRE_BUILD);
			workspace.addResourceChangeListener(postBuild, IResourceChangeEvent.POST_BUILD);

			file1.touch(createTestMonitor());

			// wait for noBuildJob so POST_BUILD will fire
			((Workspace) getWorkspace()).getBuildManager().waitForAutoBuildOff();

			int trigger = IncrementalProjectBuilder.AUTO_BUILD;
			assertEquals("Should see PRE_BUILD event", trigger, preBuild.trigger);
			assertEquals("Should see POST_BUILD event", trigger, postBuild.trigger);
			assertEquals("Should see workspace root on PRE_BUILD event", workspace, preBuild.source);
			assertEquals("Should see workspace root on POST_BUILD event", workspace, postBuild.source);
		} finally {
			workspace.removeResourceChangeListener(preBuild);
			workspace.removeResourceChangeListener(postBuild);
		}
	}

	public void testChangeFileToFolder() throws CoreException {
		/* change file1 into a folder */
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED,
				IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Deleting and Creating", 100);
			try {
				file1.delete(true, SubMonitor.convert(m, 50));
				folder3.create(true, true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testChangeFolderToFile() throws CoreException {
		/* change to a folder */
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			file1.delete(true, createTestMonitor());
			folder3.create(true, true, createTestMonitor());
		}, null);
		/* now change back to a file and verify */
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED,
				IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Deleting and Creating", 100);
			try {
				folder3.delete(true, SubMonitor.convert(m, 50));
				file1.create(getRandomContents(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testChangeProject() throws CoreException {
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			project2.create(createTestMonitor());
			project2.open(createTestMonitor());
		}, null);
		IProjectDescription desc = project2.getDescription();
		desc.setReferencedProjects(new IProject[] { project1 });
		verifier.addExpectedChange(project2, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		verifier.addExpectedChange(project2MetaData, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		project2.setDescription(desc, IResource.FORCE, createTestMonitor());
		assertDelta();
	}

	public void testCopyChangeFile() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, 0, null, null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 150);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.copy(file3.getFullPath(), true, SubMonitor.convert(m, 50));
				file3.setContents(getRandomContents(), IResource.NONE, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testCopyFile() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, 0, null, null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.copy(file3.getFullPath(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testCloseOpenReplaceFile() throws CoreException {
		// FIXME: how to do this?
		// workspace.save(getMonitor());
		// workspace.close(getMonitor());
		// workspace.open(getMonitor());
		verifier.reset();
		getWorkspace().addResourceChangeListener(verifier);
		/* change file1's contents */
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.CONTENT);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Deleting and Creating", 100);
			try {
				file1.delete(true, SubMonitor.convert(m, 50));
				file1.create(getRandomContents(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testDeleteInPostBuildListener() throws Throwable {
		final AtomicReference<CoreException> exceptionInListener = new AtomicReference<>();
		// create the resource change listener
		IResourceChangeListener listener = event -> {
			try {
				event.getDelta().accept(delta -> {
					IResource resource = delta.getResource();
					if (resource.getType() == IResource.FILE) {
						try {
							((IFile) resource).delete(true, true, null);
						} catch (RuntimeException e) {
							throw e;
						}
					}
					return true;
				});
			} catch (CoreException e) {
				exceptionInListener.set(e);
			}
		};
		// register the listener with the workspace.
		getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_BUILD);
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> getWorkspace().getRoot().accept(resource -> {
				resource.touch(createTestMonitor());
				return true;
			}), createTestMonitor());
		} finally {
			// cleanup: ensure that the listener is removed
			getWorkspace().removeResourceChangeListener(listener);
		}
		if (exceptionInListener.get() != null) {
			throw exceptionInListener.get();
		}
	}

	/**
	 * Tests deleting a file, then moving another file to that deleted location.
	 * See bug 27527.
	 */
	public void testDeleteMoveFile() throws CoreException {
		verifier.reset();
		file2.create(getRandomContents(), IResource.NONE, createTestMonitor());
		verifier.reset();
		int flags = IResourceDelta.REPLACED | IResourceDelta.MOVED_FROM | IResourceDelta.CONTENT;
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, flags, file2.getFullPath(), null);
		verifier.addExpectedChange(file2, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file1.getFullPath());
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("deleting and moving", 100);
			try {
				file1.delete(IResource.NONE, SubMonitor.convert(m, 50));
				file2.move(file1.getFullPath(), IResource.NONE, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testDeleteProject() throws Throwable {
		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		//test that marker deltas are fired when projects are deleted
		verifier.reset();
		final IMarker marker = project1.createMarker(IMarker.TASK);
		class Listener1 implements IResourceChangeListener {
			public boolean done = false;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				done = true;
				IMarkerDelta[] deltas = event.findMarkerDeltas(IMarker.TASK, false);
				listenerInMainThreadCallback.set(() -> {
					assertEquals("1.0", 1, deltas.length);
					assertEquals("1.1", marker.getId(), deltas[0].getId());
					assertEquals("1.2", IResourceDelta.REMOVED, deltas[0].getKind());
					synchronized (this) {
						notifyAll();
					}
				});
			}
		}
		Listener1 listener = new Listener1();
		try {
			getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
			project1.delete(true, false, createTestMonitor());
			synchronized (listener) {
				int i = 0;
				while (!listener.done) {
					try {
						listener.wait(1000);
					} catch (InterruptedException e) {
					}
					assertTrue("2.0", ++i < 60);
				}
			}
		} finally {
			getWorkspace().removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	public void testDeleteFolderDuringRefresh() throws Throwable {
		project1 = getWorkspace().getRoot().getProject(createUniqueString());
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());

		project2 = getWorkspace().getRoot().getProject(createUniqueString());
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());

		assertTrue("1.0", project1.isOpen());
		assertTrue("2.0", project2.isOpen());

		final IFolder f = project1.getFolder(createUniqueString());
		f.create(true, true, createTestMonitor());

		// the listener checks if an attempt to modify the tree succeeds if made in a job
		// that belongs to FAMILY_MANUAL_REFRESH
		class Listener1 implements IResourceChangeListener {
			public volatile boolean deletePerformed = false;
			public volatile Exception exception;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				new Job("deleteFolder") {
					@Override
					public boolean belongsTo(Object family) {
						return family == ResourcesPlugin.FAMILY_MANUAL_REFRESH;
					}

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							f.delete(true, createTestMonitor());
							deletePerformed = true;
						} catch (Exception e) {
							exception = e;
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		}

		Listener1 listener1 = new Listener1();

		// perform a refresh to test the added listeners
		try {
			getWorkspace().addResourceChangeListener(listener1, IResourceChangeEvent.PRE_REFRESH);

			project2.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_MANUAL_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, null);

			assertTrue("deletion did unexpectedly not succeed", listener1.deletePerformed);
			assertDoesNotExistInWorkspace(f);
		} finally {
			getWorkspace().removeResourceChangeListener(listener1);
		}
		if (listener1.exception != null) {
			throw listener1.exception;
		}
	}

	public void testRefreshOtherProjectDuringRefresh() throws Throwable {
		final IProject p = getWorkspace().getRoot().getProject(createUniqueString());
		p.create(null);
		p.open(null);

		project1 = getWorkspace().getRoot().getProject(createUniqueString());
		project1.create(null);
		project1.open(null);

		assertTrue("1.0", p.isOpen());
		assertTrue("2.0", project1.isOpen());

		// the listener checks if an attempt to modify the tree succeeds if made in a job
		// that belongs to FAMILY_MANUAL_REFRESH
		class Listener1 implements IResourceChangeListener {
			public volatile boolean refreshPerformed = false;
			public volatile Exception exception;

			@Override
			public void resourceChanged(final IResourceChangeEvent event) {
				new Job("refreshProject") {
					@Override
					public boolean belongsTo(Object family) {
						return family == ResourcesPlugin.FAMILY_MANUAL_REFRESH;
					}

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							if (event.getResource() != p) {
								p.refreshLocal(IResource.DEPTH_INFINITE, null);
							}
							refreshPerformed = true;
						} catch (Exception e) {
							exception = e;
						}
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		}
		Listener1 listener1 = new Listener1();

		// the listener checks if an attempt to modify the tree in the refresh thread fails
		class Listener2 implements IResourceChangeListener {
			public volatile boolean refreshSucceeded = false;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				try {
					if (event.getResource() != p) {
						p.refreshLocal(IResource.DEPTH_INFINITE, null);
						refreshSucceeded = true;
					}
				} catch (Exception e) {
					// should fail
				}
			}
		}
		Listener2 listener2 = new Listener2();

		// perform a refresh to test the added listeners
		try {
			getWorkspace().addResourceChangeListener(listener1, IResourceChangeEvent.PRE_REFRESH);
			getWorkspace().addResourceChangeListener(listener2, IResourceChangeEvent.PRE_REFRESH);

			project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_MANUAL_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, null);

			assertThat("Refreshing resource in first resource change listener did not succeed",
					listener1.refreshPerformed);
			if (listener1.exception != null) {
				throw listener1.exception;
			}
			assertThat("Refreshing resource in second resource change listener unexpectedly succeeded",
					!listener2.refreshSucceeded);

		} finally {
			getWorkspace().removeResourceChangeListener(listener1);
			getWorkspace().removeResourceChangeListener(listener2);
		}
	}

	public void testPreRefreshNotification() throws Exception {
		final IWorkspaceRoot root = getWorkspace().getRoot();

		project1 = root.getProject(createUniqueString());
		project1.create(null);
		project1.open(null);

		assertTrue("1.0", project1.isOpen());

		class Listener1 implements IResourceChangeListener {
			public volatile boolean wasPerformed = false;
			public volatile Object eventSource;
			public volatile Object eventResource;

			@Override
			public void resourceChanged(final IResourceChangeEvent event) {
				wasPerformed = true;
				eventSource = event.getSource();
				eventResource = event.getResource();
			}
		}

		Listener1 listener1 = new Listener1();

		// perform a refresh to test the added listeners
		try {
			getWorkspace().addResourceChangeListener(listener1, IResourceChangeEvent.PRE_REFRESH);

			root.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_MANUAL_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, null);

			assertTrue("2.0", listener1.wasPerformed);
			assertEquals("3.0", getWorkspace(), listener1.eventSource);
			assertEquals("4.0", null, listener1.eventResource);

			project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_MANUAL_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, null);

			assertTrue("5.0", listener1.wasPerformed);
			assertEquals("6.0", project1, listener1.eventSource);
			assertEquals("7.0", project1, listener1.eventResource);
		} finally {
			getWorkspace().removeResourceChangeListener(listener1);
		}
	}

	/**
	 * Tests that phantom members don't show up in resource deltas when standard
	 * traversal and visitor are used.
	 */
	public void testHiddenPhantomChanges() throws Throwable {
		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		final IWorkspace workspace = getWorkspace();
		final IFolder phantomFolder = project1.getFolder("PhantomFolder");
		final IFile phantomFile = folder1.getFile("PhantomFile");
		final IResource[] phantomResources = new IResource[] {phantomFolder, phantomFile};
		final QualifiedName partner = new QualifiedName("Test", "Infected");
		IResourceChangeListener listener = event -> {
			ThrowingRunnable oldCallback = listenerInMainThreadCallback.get();
			listenerInMainThreadCallback.set(() -> {
				oldCallback.run();
				// make sure the delta doesn't include the phantom members
				assertNotDeltaIncludes(event.getDelta(), phantomResources);
				// make sure a visitor does not find phantom members
				assertNotDeltaVisits(event.getDelta(), phantomResources);
			});
		};
		workspace.addResourceChangeListener(listener);
		workspace.getSynchronizer().add(partner);
		removeFromWorkspace(phantomResources);
		try {
			//create a phantom folder
			workspace.run((IWorkspaceRunnable) monitor -> workspace.getSynchronizer().setSyncInfo(partner, phantomFolder, new byte[] {1}), createTestMonitor());
			//create children in phantom folder
			IFile fileInFolder = phantomFolder.getFile("FileInPrivateFolder");
			workspace.getSynchronizer().setSyncInfo(partner, fileInFolder, new byte[] {1});
			//modify children in phantom folder
			workspace.getSynchronizer().setSyncInfo(partner, fileInFolder, new byte[] {2});
			//delete children in phantom folder
			workspace.getSynchronizer().flushSyncInfo(partner, fileInFolder, IResource.DEPTH_INFINITE);
			//delete phantom folder and change some other file
			workspace.run((IWorkspaceRunnable) monitor -> {
				phantomFolder.delete(IResource.NONE, createTestMonitor());
				file1.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			}, createTestMonitor());
			//create phantom file
			workspace.run((IWorkspaceRunnable) monitor -> workspace.getSynchronizer().setSyncInfo(partner, phantomFile, new byte[] {2}), createTestMonitor());
			//modify phantom file
			workspace.getSynchronizer().setSyncInfo(partner, phantomFile, new byte[] {3});
			//delete phantom file
			workspace.getSynchronizer().flushSyncInfo(partner, phantomFile, IResource.DEPTH_INFINITE);
		} finally {
			workspace.removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	/**
	 * Tests that team private members don't show up in resource deltas when
	 * standard traversal and visitor are used.
	 */
	public void testHiddenTeamPrivateChanges() throws Throwable {
		final AtomicReference<ThrowingRunnable> listenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		IWorkspace workspace = getWorkspace();
		final IFolder teamPrivateFolder = project1.getFolder("TeamPrivateFolder");
		final IFile teamPrivateFile = folder1.getFile("TeamPrivateFile");
		final IResource[] privateResources = new IResource[] {teamPrivateFolder, teamPrivateFile};
		IResourceChangeListener listener = event -> {
			ThrowingRunnable oldCallback = listenerInMainThreadCallback.get();
			listenerInMainThreadCallback.set(() -> {
				oldCallback.run();
				// make sure the delta doesn't include the team private members
				assertNotDeltaIncludes(event.getDelta(), privateResources);
				// make sure a visitor does not find team private members
				assertNotDeltaVisits(event.getDelta(), privateResources);
			});
		};
		workspace.addResourceChangeListener(listener);
		try {
			//create a team private folder
			workspace.run((IWorkspaceRunnable) monitor -> {
				teamPrivateFolder.create(true, true, createTestMonitor());
				teamPrivateFolder.setTeamPrivateMember(true);
			}, createTestMonitor());
			//create children in team private folder
			IFile fileInFolder = teamPrivateFolder.getFile("FileInPrivateFolder");
			fileInFolder.create(getRandomContents(), true, createTestMonitor());
			//modify children in team private folder
			fileInFolder.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			//delete children in team private folder
			fileInFolder.delete(IResource.NONE, createTestMonitor());
			//delete team private folder and change some other file
			workspace.run((IWorkspaceRunnable) monitor -> {
				teamPrivateFolder.delete(IResource.NONE, createTestMonitor());
				file1.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			}, createTestMonitor());
			//create team private file
			workspace.run((IWorkspaceRunnable) monitor -> {
				teamPrivateFile.create(getRandomContents(), true, createTestMonitor());
				teamPrivateFile.setTeamPrivateMember(true);
			}, createTestMonitor());
			//modify team private file
			teamPrivateFile.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			//delete team private file
			teamPrivateFile.delete(IResource.NONE, createTestMonitor());
		} finally {
			workspace.removeResourceChangeListener(listener);
		}
		listenerInMainThreadCallback.get().run();
	}

	public void testModifyMoveFile() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM | IResourceDelta.CONTENT,
				file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
				file1.move(file3.getFullPath(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveFile() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.move(file3.getFullPath(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveFileAddMarker() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM | IResourceDelta.MARKERS,
				file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.move(file3.getFullPath(), true, SubMonitor.convert(m, 50));
				file3.createMarker(IMarker.TASK);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	/**
	 * Regression test for bug 42514
	 */
	public void testMoveFileDeleteFolder() throws CoreException {
		// file2 moved to file1, and colliding folder3 is deleted
		file1.delete(IResource.NONE, null);
		file2.create(getRandomContents(), IResource.NONE, null);
		folder3.create(IResource.NONE, true, null);
		verifier.reset();
		verifier.addExpectedChange(file2, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file1.getFullPath());
		int flags = IResourceDelta.MOVED_FROM | IResourceDelta.REPLACED | IResourceDelta.TYPE | IResourceDelta.CONTENT;
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, flags, file2.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Deleting and moving", 100);
			try {
				folder3.delete(IResource.FORCE, SubMonitor.convert(m, 50));
				file2.move(file1.getFullPath(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveFileDeleteSourceParent() throws CoreException {
		file1.delete(IResource.NONE, null);
		createInWorkspace(file3);
		verifier.reset();
		verifier.addExpectedChange(folder2, IResourceDelta.REMOVED, 0, null, null);
		verifier.addExpectedChange(file1, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file3.getFullPath(), null);
		verifier.addExpectedChange(file3, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file1.getFullPath());
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				file3.move(file1.getFullPath(), true, SubMonitor.convert(m, 50));
				folder2.delete(IResource.NONE, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveModifyFile() throws CoreException {
		verifier.addExpectedChange(folder2, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM | IResourceDelta.CONTENT,
				file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				folder2.create(true, true, SubMonitor.convert(m, 50));
				file1.move(file3.getFullPath(), true, SubMonitor.convert(m, 50));
				file3.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveMoveFile() throws CoreException {
		file2 = project1.getFile("File2");
		file3 = project1.getFile("File3");
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("moving and moving file", 100);
			try {
				file1.move(file2.getFullPath(), false, null);
				file2.move(file3.getFullPath(), false, null);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMoveMoveFolder() throws CoreException {
		folder2 = project1.getFolder("Folder2");
		folder3 = project1.getFolder("Folder3");
		file3 = folder3.getFile(file1.getName());
		verifier.addExpectedChange(folder1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null,
				folder3.getFullPath());
		verifier.addExpectedChange(folder3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, folder1.getFullPath(),
				null);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
		verifier.addExpectedChange(file3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file1.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("moving and moving folder", 100);
			try {
				folder1.move(folder2.getFullPath(), false, null);
				folder2.move(folder3.getFullPath(), false, null);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	/**
	 * Move a project via rename. Note that the DESCRIPTION flag should be set
	 * in the delta for the destination only.
	 */
	public void testMoveProject1() throws CoreException {
		verifier.reset();
		verifier.addExpectedChange(project1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFullPath());
		verifier.addExpectedChange(project1.getFile(".project"), IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFile(".project").getFullPath());
		verifier.addExpectedChange(folder1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFolder(folder1.getProjectRelativePath()).getFullPath());
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFile(file1.getProjectRelativePath()).getFullPath());

		verifier.addExpectedChange(settings, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFolder(settings.getProjectRelativePath()).getFullPath());
		verifier.addExpectedChange(prefs, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, project2.getFile(prefs.getProjectRelativePath()).getFullPath());

		verifier.addExpectedChange(project2, IResourceDelta.ADDED, IResourceDelta.OPEN | IResourceDelta.DESCRIPTION | IResourceDelta.MOVED_FROM, project1.getFullPath(), null);
		verifier.addExpectedChange(project2.getFile(".project"), IResourceDelta.ADDED, IResourceDelta.CONTENT | IResourceDelta.MOVED_FROM, project1.getFile(".project").getFullPath(), null);
		verifier.addExpectedChange(project2.getFolder(folder1.getProjectRelativePath()), IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, folder1.getFullPath(), null);
		verifier.addExpectedChange(project2.getFile(file1.getProjectRelativePath()), IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file1.getFullPath(), null);

		verifier.addExpectedChange(project2.getFolder(settings.getProjectRelativePath()), IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, settings.getFullPath(), null);
		verifier.addExpectedChange(project2.getFile(prefs.getProjectRelativePath()), IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, prefs.getFullPath(), null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				project1.move(project2.getFullPath(), IResource.NONE, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	/**
	 * Move a project via a location change only. Note that the DESCRIPTION flag
	 * should be set in the delta.
	 */
	public void testMoveProject2() throws CoreException {
		final IPath path = getRandomLocation();
		deleteOnTearDown(path);
		verifier.addExpectedChange(project1, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Creating and moving", 100);
			try {
				IProjectDescription desc = project1.getDescription();
				desc.setLocation(path);
				project1.move(desc, IResource.NONE, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testMulti() throws CoreException {
		class Listener implements IResourceChangeListener {
			public volatile boolean done = false;
			public volatile int eventType = -1;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				eventType = event.getType();
				done = true;
			}
		}
		Listener listener1 = new Listener();
		Listener listener2 = new Listener();
		getWorkspace().addResourceChangeListener(listener1, IResourceChangeEvent.POST_CHANGE);
		getWorkspace().addResourceChangeListener(listener2, IResourceChangeEvent.POST_BUILD);
		try {
			project1.touch(createTestMonitor());
			int i = 0;
			while (!(listener1.done && listener2.done)) {
				// timeout if the listeners are never called
				assertTrue("Listeners were never called", ++i < 600);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
			}
			assertEquals("Improper change event processed by first listener", IResourceChangeEvent.POST_CHANGE,
					listener1.eventType);
			assertEquals("Improper change event processed by second listener", IResourceChangeEvent.POST_BUILD,
					listener2.eventType);
		} finally {
			getWorkspace().removeResourceChangeListener(listener1);
			getWorkspace().removeResourceChangeListener(listener2);
		}
	}

	public void testAutoPublishService() throws Throwable {
		final AtomicReference<ThrowingRunnable> logListenerInMainThreadCallback = new AtomicReference<>(NOOP_RUNNABLE);
		class Loggy implements LogListener {
			public boolean done = false;
			@Override
			public void logged(LogEntry entry) {
				String message = entry.getMessage();
				LogLevel level = entry.getLogLevel();
				if (level == LogLevel.WARN && message.startsWith("event.mask of IResourceChangeListener")) {
					done = true;
					logListenerInMainThreadCallback.set(() -> assertEquals(
							"event.mask of IResourceChangeListener service: expected Integer but was class java.lang.String (from class org.eclipse.core.tests.resources.IResourceChangeListenerTest$2Listener): Not an integer",
							message));
				}
			}
		}
		class Listener implements IResourceChangeListener {
			public volatile boolean done = false;
			public volatile int eventType = -1;

			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				eventType = event.getType();
				done = true;
			}
		}
		Loggy loggy = new Loggy();
		Listener listener1 = new Listener();
		Listener listener2 = new Listener();
		Listener listener3 = new Listener();
		Bundle bundle = FrameworkUtil.getBundle(getWorkspace().getClass());
		BundleContext context = bundle.getBundleContext();
		ServiceReference<LogReaderService> logReaderService = context.getServiceReference(LogReaderService.class);
		LogReaderService reader = logReaderService == null ? null : context.getService(logReaderService);
		if (reader != null) {
			reader.addLogListener(loggy);
		}
		// Default is event.mask = IResourceChangeEvent.PRE_CLOSE |
		// IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE
		ServiceRegistration<IResourceChangeListener> reg1 = context.registerService(IResourceChangeListener.class,
				listener1, null);
		ServiceRegistration<IResourceChangeListener> reg2 = context.registerService(IResourceChangeListener.class,
				listener2, with("event.mask", IResourceChangeEvent.POST_BUILD));
		ServiceRegistration<IResourceChangeListener> reg3 = context.registerService(IResourceChangeListener.class,
				listener3, with("event.mask", "Not an integer"));
		try {
			assertTrue(waitUntil(() -> reg1.getReference().getUsingBundles() != null));
			assertTrue(waitUntil(() -> reg2.getReference().getUsingBundles() != null));
			assertTrue(waitUntil(() -> reg3.getReference().getUsingBundles() != null));
			project1.touch(createTestMonitor());
			assertTrue(waitUntil(
					() -> listener1.done && listener2.done && listener3.done && (loggy.done || reader == null)));
		} finally {
			if (reader != null) {
				reader.removeLogListener(loggy);
			}
			if (logReaderService != null) {
				context.ungetService(logReaderService);
			}
			if (reg1 != null) {
				reg1.unregister();
			}
			if (reg2 != null) {
				reg2.unregister();
			}
			if (reg3 != null) {
				reg3.unregister();
			}
		}
		assertEquals("Improper change event processed by first listener", IResourceChangeEvent.POST_CHANGE,
				listener1.eventType);
		assertEquals("Improper change event processed by second listener", IResourceChangeEvent.POST_BUILD,
				listener2.eventType);
		assertEquals("Improper change event processed by third listener", IResourceChangeEvent.POST_CHANGE,
				listener3.eventType);
		logListenerInMainThreadCallback.get().run();
	}

	public boolean waitUntil(BooleanSupplier condition) {
		int i = 0;
		while (!condition.getAsBoolean()) {
			if (i++ > 600) {
				return false;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
			}
		}
		return true;
	}

	private static Dictionary<String, Object> with(String key, Object value) {
		Hashtable<String, Object> dict = new Hashtable<>();
		dict.put(key, value);
		return dict;
	}

	public void testProjectDescriptionComment() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(project1, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		verifier.addExpectedChange(project1MetaData, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		IProjectDescription description = project1.getDescription();
		description.setComment("new comment");
		project1.setDescription(description, IResource.NONE, createTestMonitor());
		assertDelta();
	}

	public void testProjectDescriptionDynamicRefs() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(project1, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		IProjectDescription description = project1.getDescription();
		description.setDynamicReferences(new IProject[] { project2 });
		project1.setDescription(description, IResource.NONE, createTestMonitor());
		assertDelta();
	}

	public void testProjectDescriptionNatures() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(project1, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		verifier.addExpectedChange(project1MetaData, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		IProjectDescription description = project1.getDescription();
		description.setNatureIds(new String[] { NATURE_SIMPLE });
		project1.setDescription(description, IResource.NONE, createTestMonitor());
		assertDelta();
	}

	public void testProjectDescriptionStaticRefs() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(project1, IResourceDelta.CHANGED, IResourceDelta.DESCRIPTION);
		verifier.addExpectedChange(project1MetaData, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		IProjectDescription description = project1.getDescription();
		description.setReferencedProjects(new IProject[] { project2 });
		project1.setDescription(description, IResource.NONE, createTestMonitor());
		assertDelta();
	}

	public void testRemoveFile() throws CoreException {
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, 0);
		file1.delete(true, createTestMonitor());
		assertDelta();
	}

	public void testRemoveFileAndFolder() throws CoreException {
		verifier.addExpectedChange(folder1, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.REMOVED, 0);
		folder1.delete(true, createTestMonitor());
		assertDelta();
	}

	public void testReplaceFile() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.CONTENT);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("Deleting and Creating", 100);
			try {
				file1.delete(true, SubMonitor.convert(m, 50));
				file1.create(getRandomContents(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testReplaceFolderWithFolder() throws CoreException {
		folder2 = project1.getFolder("Folder2");
		folder3 = project1.getFolder("Folder3");
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			file1.delete(false, null);
			folder2.create(false, true, null);
		}, null);
		verifier.reset();
		verifier.addExpectedChange(folder1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null,
				folder2.getFullPath());
		int flags = IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO | IResourceDelta.REPLACED
				| IResourceDelta.CONTENT;
		verifier.addExpectedChange(folder2, IResourceDelta.CHANGED, flags, folder1.getFullPath(),
				folder3.getFullPath());
		verifier.addExpectedChange(folder3, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, folder2.getFullPath(),
				null);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("replace folder with folder", 100);
			try {
				folder2.move(folder3.getFullPath(), false, null);
				folder1.move(folder2.getFullPath(), false, null);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testSetLocal() throws CoreException {
		verifier.reset();
		// set local on a file that is already local -- should be no change
		file1.setLocal(true, IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("Unexpected notification on no change", !verifier.hasBeenNotified());
		// set non-local, still shouldn't appear in delta
		verifier.reset();
		file1.setLocal(false, IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue("Unexpected notification on no change", !verifier.hasBeenNotified());
	}

	public void testSwapFiles() throws CoreException {
		file1 = project1.getFile("File1");
		file2 = project1.getFile("File2");
		file3 = project1.getFile("File3");
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			file1.create(new ByteArrayInputStream(new byte[] { 65 }), false, null);
			file2.create(new ByteArrayInputStream(new byte[] { 67 }), false, null);
		}, null);
		verifier.reset();
		final int flags = IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO | IResourceDelta.REPLACED
				| IResourceDelta.CONTENT;
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, flags, file2.getFullPath(), file2.getFullPath());
		verifier.addExpectedChange(file2, IResourceDelta.CHANGED, flags, file1.getFullPath(), file1.getFullPath());
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("swap files", 100);
			try {
				file1.move(file3.getFullPath(), false, null);
				file2.move(file1.getFullPath(), false, null);
				file3.move(file2.getFullPath(), false, null);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testSwapFolders() throws CoreException {
		verifier.reset();
		getWorkspace().run((IWorkspaceRunnable) m -> {
			folder2 = project1.getFolder("Folder2");
			folder3 = project1.getFolder("Folder3");
			file1.delete(false, null);
			folder2.create(false, true, null);
		}, null);
		verifier.reset();
		final int flags = IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO | IResourceDelta.REPLACED | IResourceDelta.CONTENT;
		verifier.addExpectedChange(folder1, IResourceDelta.CHANGED, flags, folder2.getFullPath(), folder2.getFullPath());
		verifier.addExpectedChange(folder2, IResourceDelta.CHANGED, flags, folder1.getFullPath(), folder1.getFullPath());
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("swap folders", 100);
			try {
				folder1.move(folder3.getFullPath(), false, null);
				folder2.move(folder1.getFullPath(), false, null);
				folder3.move(folder2.getFullPath(), false, null);
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	/**
	 * Asserts that the delta is correct for changes to team private members.
	 */
	public void testTeamPrivateChanges() throws CoreException {
		IWorkspace workspace = getWorkspace();
		final IFolder teamPrivateFolder = project1.getFolder("TeamPrivateFolder");
		final IFile teamPrivateFile = folder1.getFile("TeamPrivateFile");
		// create a team private folder
		verifier.reset();
		verifier.addExpectedChange(teamPrivateFolder, IResourceDelta.ADDED, 0);
		workspace.run((IWorkspaceRunnable) monitor -> {
			teamPrivateFolder.create(true, true, createTestMonitor());
			teamPrivateFolder.setTeamPrivateMember(true);
		}, createTestMonitor());
		assertDelta();
		verifier.reset();
		// create children in team private folder
		IFile fileInFolder = teamPrivateFolder.getFile("FileInPrivateFolder");
		verifier.addExpectedChange(fileInFolder, IResourceDelta.ADDED, 0);
		fileInFolder.create(getRandomContents(), true, createTestMonitor());
		assertDelta();
		verifier.reset();
		// modify children in team private folder
		verifier.addExpectedChange(fileInFolder, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		fileInFolder.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertDelta();
		verifier.reset();
		// delete children in team private folder
		verifier.addExpectedChange(fileInFolder, IResourceDelta.REMOVED, 0);
		fileInFolder.delete(IResource.NONE, createTestMonitor());
		assertDelta();
		verifier.reset();
		// delete team private folder and change some other file
		verifier.addExpectedChange(teamPrivateFolder, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		workspace.run((IWorkspaceRunnable) monitor -> {
			teamPrivateFolder.delete(IResource.NONE, createTestMonitor());
			file1.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		}, createTestMonitor());
		assertDelta();
		verifier.reset();
		// create team private file
		verifier.addExpectedChange(teamPrivateFile, IResourceDelta.ADDED, 0);
		workspace.run((IWorkspaceRunnable) monitor -> {
			teamPrivateFile.create(getRandomContents(), true, createTestMonitor());
			teamPrivateFile.setTeamPrivateMember(true);
		}, createTestMonitor());
		assertDelta();
		verifier.reset();
		// modify team private file
		verifier.addExpectedChange(teamPrivateFile, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		teamPrivateFile.setContents(getRandomContents(), IResource.NONE, createTestMonitor());
		assertDelta();
		verifier.reset();
		// delete team private file
		verifier.addExpectedChange(teamPrivateFile, IResourceDelta.REMOVED, 0);
		teamPrivateFile.delete(IResource.NONE, createTestMonitor());
		assertDelta();
		verifier.reset();
	}

	public void testTwoFileChanges() throws CoreException {
		verifier.addExpectedChange(file1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		verifier.addExpectedChange(file2, IResourceDelta.ADDED, 0);
		getWorkspace().run((IWorkspaceRunnable) m -> {
			m.beginTask("setting contents and creating", 100);
			try {
				file1.setContents(getRandomContents(), true, false, SubMonitor.convert(m, 50));
				file2.create(getRandomContents(), true, SubMonitor.convert(m, 50));
			} finally {
				m.done();
			}
		}, createTestMonitor());
		assertDelta();
	}

	public void testRemoveAndCreateUnderlyingFileForLinkedResource() throws CoreException, IOException {
		IPath path = getTempDir().addTrailingSeparator().append(createUniqueString());
		deleteOnTearDown(path);
		path.toFile().createNewFile();

		IFile linkedFile = project1.getFile(createUniqueString());
		linkedFile.createLink(path, IResource.NONE, createTestMonitor());

		// check the delta when underlying file is removed
		verifier.addExpectedChange(linkedFile, IResourceDelta.CHANGED, IResourceDelta.LOCAL_CHANGED);
		path.toFile().delete();
		project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertDelta();

		// check the delta when underlying file is recreated
		verifier.addExpectedChange(linkedFile, IResourceDelta.CHANGED, IResourceDelta.LOCAL_CHANGED | IResourceDelta.CONTENT);
		path.toFile().createNewFile();

		project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertDelta();
	}

	public void testRemoveAndCreateUnderlyingFolderForLinkedResource() throws CoreException {
		IPath path = getTempDir().addTrailingSeparator().append(createUniqueString());
		deleteOnTearDown(path);

		path.toFile().mkdir();
		IFolder linkedFolder = project1.getFolder(createUniqueString());
		linkedFolder.createLink(path, IResource.NONE, createTestMonitor());

		// check the delta when underlying folder is removed
		verifier.addExpectedChange(linkedFolder, IResourceDelta.CHANGED, IResourceDelta.LOCAL_CHANGED);
		path.toFile().delete();
		project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertDelta();

		// check the delta when underlying folder is recreated
		verifier.addExpectedChange(linkedFolder, IResourceDelta.CHANGED, IResourceDelta.LOCAL_CHANGED);
		path.toFile().mkdir();
		project1.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertDelta();
	}

	public void testBug228354() throws CoreException {
		IPath path = getTempDir().addTrailingSeparator().append(createUniqueString());
		deleteOnTearDown(path);

		path.toFile().mkdir();
		IFolder linkedFolder = project1.getFolder(createUniqueString());
		linkedFolder.createLink(path, IResource.NONE, createTestMonitor());

		IFolder regularFolder = project1.getFolder(createUniqueString());
		regularFolder.create(true, true, createTestMonitor());

		// check the delta when underlying folder is removed
		verifier.addExpectedChange(regularFolder, IResourceDelta.REMOVED, 0);
		regularFolder.delete(true, createTestMonitor());
		assertDelta();
	}
}