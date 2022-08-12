/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.*;
import java.io.File;
import java.util.*;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

//
public class ISynchronizerTest extends ResourceTest {
	public static int NUMBER_OF_PARTNERS = 100;
	public IResource[] resources;

	protected void assertEquals(String message, byte[] b1, byte[] b2) {
		assertTrue(message, b1.length == b2.length);
		for (int i = 0; i < b1.length; i++) {
			assertTrue(message, b1[i] == b2[i]);
		}
	}

	/**
	 * Return a string array which defines the hierarchy of a tree.
	 * Folder resources must have a trailing slash.
	 */
	@Override
	public String[] defineHierarchy() {
		return new String[] {"/", "1/", "1/1", "1/2/", "1/2/1", "1/2/2/", "2/", "2/1", "2/2/", "2/2/1", "2/2/2/"};
	}

	/*
	 * Internal method used for flushing all sync information for a particular resource
	 * and its children.
	 */
	protected void flushAllSyncInfo(final IResource root) throws CoreException {
		assertNotNull(root);

		final ISynchronizer synchronizer = getWorkspace().getSynchronizer();
		final QualifiedName[] partners = synchronizer.getPartners();
		IWorkspaceRunnable body = monitor -> {
			IResourceVisitor visitor = resource -> {
				for (QualifiedName partner : partners) {
					synchronizer.setSyncInfo(partner, resource, null);
				}
				return true;
			};
			root.accept(visitor, IResource.DEPTH_INFINITE, true);
		};
		getWorkspace().run(body, null);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		resources = createHierarchy();
	}

	@Override
	public void tearDown() throws Exception {
		// remove all registered sync partners so we don't create
		// phantoms when we delete
		QualifiedName[] names = getWorkspace().getSynchronizer().getPartners();
		for (QualifiedName name : names) {
			getWorkspace().getSynchronizer().remove(name);
		}

		// delete the root and everything under it
		super.tearDown();
	}

	public void testDeleteResources() {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = getRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			synchronizer.setSyncInfo(qname, resource, b);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// get the info and ensure its the same
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("1.0." + resource.getFullPath(), actual);
					return true;
				}
				assertNotNull("1.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("1.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("1.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.4", e);
		}

		// delete all resources under the projects.
		final IProject[] projects = getWorkspace().getRoot().getProjects();
		IWorkspaceRunnable body = monitor -> {
			for (IProject project : projects) {
				IResource[] children = project.members();
				for (IResource element : children) {
					element.delete(false, getMonitor());
				}
			}
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}

		// sync info should remain for the resources since they are now phantoms
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("3.0", actual);
					return true;
				}
				assertNotNull("3.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("3.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("3.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("3.4", e);
		}

		// delete the projects
		for (IProject project : projects) {
			try {
				project.delete(false, getMonitor());
			} catch (CoreException e) {
				ResourcesPlugin.getPlugin().getLog().log(e.getStatus());
				fail("4.0", e);
			}
		}

		// sync info should be gone since projects can't become phantoms
		visitor = resource -> {
			try {
				assertNull("5.0." + resource.getFullPath(), synchronizer.getSyncInfo(qname, resource));
			} catch (CoreException e) {
				fail("5.1." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("5.2", e);
		}
	}

	public void testDeleteResources2() {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = getRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			synchronizer.setSyncInfo(qname, resource, b);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// get the info and ensure its the same
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("1.0." + resource.getFullPath(), actual);
					return true;
				}
				assertNotNull("1.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("1.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("1.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.4", e);
		}

		// delete all resources under the projects.
		final IProject[] projects = getWorkspace().getRoot().getProjects();
		IWorkspaceRunnable body = monitor -> {
			for (IProject project : projects) {
				for (IResource element : project.members()) {
					if (!element.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
						element.delete(false, getMonitor());
					}
				}
			}
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}

		// sync info should remain for the resources since they are now phantoms
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("3.0", actual);
					return true;
				}
				assertNotNull("3.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("3.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("3.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("3.4", e);
		}

		// remove the sync info for the immediate children of the projects.
		body = monitor -> {
			for (IProject project : projects) {
				for (IResource element : project.members(true)) {
					synchronizer.setSyncInfo(qname, element, null);
				}
			}
		};
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("4.99", e);
		}

		// there should be no sync info for any resources except the project
		visitor = resource -> {
			int type = resource.getType();
			if (type == IResource.ROOT || type == IResource.PROJECT) {
				return true;
			}
			if (type == IResource.FILE && resource.getParent().getType() == IResource.PROJECT && resource.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
				return true;
			}
			assertNull("5.0." + resource.getFullPath(), synchronizer.getSyncInfo(qname, resource));
			return true;
		};

		try {
			getWorkspace().getRoot().accept(visitor, IResource.DEPTH_INFINITE, true);
		} catch (CoreException e) {
			fail("5.99", e);
		}
	}

	public void testMoveResource() {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// cleanup auto-created resources
		try {
			getWorkspace().getRoot().delete(true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// setup
		IResource[] resources = buildResources(getWorkspace().getRoot(), new String[] {"/Foo", "/Foo/file.txt"});
		IProject project = (IProject) resources[0];
		IFile source = (IFile) resources[1];
		// create in workspace
		ensureExistsInWorkspace(resources, true);

		// register partner and add sync info
		synchronizer.add(qname);
		byte[] b = new byte[] {1, 2, 3, 4};
		try {
			synchronizer.setSyncInfo(qname, source, b);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// move the file
		IFile destination = project.getFile("newFile.txt");
		try {
			source.move(destination.getFullPath(), true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// check sync info
		try {
			byte[] old = synchronizer.getSyncInfo(qname, source);
			assertNotNull("4.0", old);
			assertEquals("4.1", b, old);
			assertNull("4.2", synchronizer.getSyncInfo(qname, destination));
		} catch (CoreException e) {
			fail("4.3", e);
		}
	}

	public void testMoveResource2() {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// cleanup auto-created resources
		try {
			getWorkspace().getRoot().delete(true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// setup
		IResource[] toTest = buildResources(getWorkspace().getRoot(), new String[] {"/Foo", "/Foo/file.txt"});
		IProject sourceProject = (IProject) toTest[0];
		IFile sourceFile = (IFile) toTest[1];
		// create in workspace
		ensureExistsInWorkspace(toTest, true);

		// register partner and add sync info
		synchronizer.add(qname);
		byte[] b = new byte[] {1, 2, 3, 4};
		try {
			synchronizer.setSyncInfo(qname, sourceProject, b);
			synchronizer.setSyncInfo(qname, sourceFile, b);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// move the file
		IFile destFile = sourceProject.getFile("newFile.txt");
		try {
			sourceFile.move(destFile.getFullPath(), true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// check sync info
		try {
			byte[] old = synchronizer.getSyncInfo(qname, sourceFile);
			assertNotNull("4.0", old);
			assertEquals("4.1", b, old);
			assertNull("4.2", synchronizer.getSyncInfo(qname, destFile));
		} catch (CoreException e) {
			fail("4.3", e);
		}

		// move the file back
		try {
			destFile.move(sourceFile.getFullPath(), true, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}

		// check the sync info
		try {
			byte[] old = synchronizer.getSyncInfo(qname, sourceFile);
			assertNotNull("6.0", old);
			assertEquals("6.1", b, old);
			assertNull("6.2", synchronizer.getSyncInfo(qname, destFile));
		} catch (CoreException e) {
			fail("6.3", e);
		}

		// rename the file and ensure that the sync info is moved with it
		IProject destProject = getWorkspace().getRoot().getProject("newProject");
		try {
			sourceProject.move(destProject.getFullPath(), true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
		try {
			assertNull("7.1", synchronizer.getSyncInfo(qname, sourceProject));
			assertNull("7.2", synchronizer.getSyncInfo(qname, sourceFile));
			byte[] old = synchronizer.getSyncInfo(qname, destProject.getFile(sourceFile.getName()));
			assertNotNull("7.3", old);
			assertEquals("7.4", b, old);
			old = synchronizer.getSyncInfo(qname, destProject);
			assertNotNull("7.5", old);
			assertEquals("7.6", b, old);
		} catch (CoreException e) {
			fail("7.3", e);
		}
	}

	public void testRegistration() {
		// setup
		QualifiedName[] partners = new QualifiedName[NUMBER_OF_PARTNERS];
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			QualifiedName name = new QualifiedName("org.eclipse.core.deployment", "myTarget" + i);
			partners[i] = name;
		}
		ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the targets...twice to ensure dups aren't registered
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			synchronizer.add(partners[i]);
			synchronizer.add(partners[i]);
		}

		// get the array of targets
		QualifiedName[] list = synchronizer.getPartners();
		assertNotNull("3.0", list);
		assertEquals("3.1", NUMBER_OF_PARTNERS, list.length);

		// unregister all targets
		for (int i = 0; i < NUMBER_OF_PARTNERS; i++) {
			synchronizer.remove(partners[i]);
		}
		assertEquals("4.0", 0, synchronizer.getPartners().length);
	}

	public void testSave() {
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final Synchronizer synchronizer = (Synchronizer) ResourcesPlugin.getWorkspace().getSynchronizer();

		// register the sync partner and set the sync info on the resources
		synchronizer.add(qname);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			try {
				byte[] b = getRandomString().getBytes();
				synchronizer.setSyncInfo(qname, resource, b);
				table.put(resource.getFullPath(), b);
			} catch (CoreException e) {
				fail("0.0." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("0.1", e);
		}

		// write out the data
		File file = Platform.getLocation().append(".testsyncinfo").toFile();
		OutputStream fileOutput = null;
		DataOutputStream o1 = null;
		try {
			fileOutput = new FileOutputStream(file);
			o1 = new DataOutputStream(fileOutput);
		} catch (FileNotFoundException e) {
			fail("1.0", e);
		}
		final DataOutputStream output = o1;
		final List<QualifiedName> list = new ArrayList<>(5);
		visitor = resource -> {
			try {
				ResourceInfo info = ((Resource) resource).getResourceInfo(false, false);
				if (info == null) {
					return true;
				}
				IPathRequestor requestor = new IPathRequestor() {
					@Override
					public IPath requestPath() {
						return resource.getFullPath();
					}

					@Override
					public String requestName() {
						return resource.getName();
					}
				};
				synchronizer.saveSyncInfo(info, requestor, output, list);
			} catch (IOException e) {
				fail("1.1", e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.2", e);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				fail("1.3", e);
			}
		}

		// flush the sync info in memory
		try {
			flushAllSyncInfo(getWorkspace().getRoot());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// read in the data
		try {
			InputStream fileInput = new FileInputStream(file);
			final DataInputStream input = new DataInputStream(fileInput);
			IWorkspaceRunnable body = monitor -> {
				SyncInfoReader reader = new SyncInfoReader((Workspace) getWorkspace(), synchronizer);
				try {
					reader.readSyncInfo(input);
				} catch (IOException e) {
					fail("3.0", e);
				}
			};
			try {
				getWorkspace().run(body, getMonitor());
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					fail("3.1", e);
				}
			}
		} catch (FileNotFoundException e) {
			fail("3.2", e);
		} catch (CoreException e) {
			fail("3.3", e);
		}

		// confirm the sync bytes are the same
		visitor = resource -> {
			byte[] actual = synchronizer.getSyncInfo(qname, resource);
			if (resource.getType() == IResource.ROOT) {
				assertNull("4.0", actual);
				return true;
			}
			assertNotNull("4.1." + resource.getFullPath(), actual);
			byte[] expected = table.get(resource.getFullPath());
			assertEquals("4.2." + resource.getFullPath(), expected, actual);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("4.3", e);
		}
	}

	public void testSnap() {
		/*
		 final Hashtable table = new Hashtable(10);
		 final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		 final Synchronizer synchronizer = (Synchronizer) ResourcesPlugin.getWorkspace().getSynchronizer();

		 // register the sync partner and set the sync info on the resources
		 synchronizer.add(qname);
		 IResourceVisitor visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 if (resource.getType() == IResource.ROOT)
		 return true;
		 try {
		 byte[] b = getRandomString().getBytes();
		 synchronizer.setSyncInfo(qname, resource, b);
		 table.put(resource.getFullPath(), b);
		 } catch (CoreException e) {
		 fail("0.0." + resource.getFullPath(), e);
		 }
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 } catch (CoreException e) {
		 fail("0.1", e);
		 }

		 // write out the data
		 File file = Platform.getLocation().append(".testsyncinfo.snap").toFile();
		 SafeChunkyOutputStream safeOutput = null;
		 DataOutputStream o1 = null;
		 try {
		 safeOutput = new SafeChunkyOutputStream(file);
		 o1 = new DataOutputStream(safeOutput);
		 } catch (IOException e) {
		 if (safeOutput != null)
		 try {
		 safeOutput.close();
		 } catch (IOException e2) {
		 }
		 fail("1.0", e);
		 }
		 final DataOutputStream output = o1;
		 visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 try {
		 synchronizer.snapSyncInfo(resource, output);
		 } catch (IOException e) {
		 fail("1.1", e);
		 }
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 safeOutput.succeed();
		 } catch (CoreException e) {
		 fail("1.2", e);
		 } catch (IOException e) {
		 fail("1.3", e);
		 } finally {
		 try {
		 output.close();
		 } catch (IOException e) {
		 fail("1.4", e);
		 }
		 }

		 // flush the sync info in memory
		 try {
		 flushAllSyncInfo(getWorkspace().getRoot());
		 } catch (CoreException e) {
		 fail("2.0", e);
		 }

		 // read in the data
		 try {
		 InputStream safeInput = new SafeChunkyInputStream(file);
		 final DataInputStream input = new DataInputStream(safeInput);
		 IWorkspaceRunnable body = new IWorkspaceRunnable() {
		 public void run(IProgressMonitor monitor) throws CoreException {
		 SyncInfoSnapReader reader = new SyncInfoSnapReader((Workspace) getWorkspace(), synchronizer);
		 try {
		 reader.readSyncInfo(input);
		 } catch (IOException e) {
		 fail("3.0", e);
		 }
		 }
		 };
		 try {
		 getWorkspace().run(body, getMonitor());
		 } finally {
		 try {
		 input.close();
		 } catch (IOException e) {
		 fail("3.1", e);
		 }
		 }
		 } catch (FileNotFoundException e) {
		 fail("3.2", e);
		 } catch (IOException e) {
		 fail("3.3", e);
		 } catch (CoreException e) {
		 fail("3.4", e);
		 }

		 // confirm the sync bytes are the same
		 visitor = new IResourceVisitor() {
		 public boolean visit(IResource resource) throws CoreException {
		 byte[] actual = synchronizer.getSyncInfo(qname, resource);
		 if (resource.getType() == IResource.ROOT) {
		 assertNull("4.0", actual);
		 return true;
		 } else
		 assertNotNull("4.1." + resource.getFullPath(), actual);
		 byte[] expected = (byte[]) table.get(resource.getFullPath());
		 assertEquals("4.2." + resource.getFullPath(), expected, actual);
		 return true;
		 }
		 };
		 try {
		 getWorkspace().getRoot().accept(visitor);
		 } catch (CoreException e) {
		 fail("4.3", e);
		 }
		 */
	}

	public void testSyncInfo() {
		final QualifiedName qname = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final ISynchronizer synchronizer = ResourcesPlugin.getWorkspace().getSynchronizer();

		// setup the sync bytes
		final Hashtable<IPath, byte[]> table = new Hashtable<>(10);
		IResourceVisitor visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			byte[] b = getRandomString().getBytes();
			table.put(resource.getFullPath(), b);
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// should not be able to set sync info before the target has been registered.
		visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			try {
				synchronizer.setSyncInfo(qname, resource, table.get(resource.getFullPath()));
				assertTrue("1.0." + resource.getFullPath(), false);
			} catch (CoreException e) {
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// should not be able to get sync info before the target has been registered
		visitor = resource -> {
			try {
				synchronizer.getSyncInfo(qname, resource);
				assertTrue("2.0." + resource.getFullPath(), false);
			} catch (CoreException e) {
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("2.1", e);
		}

		// register the target so now we should be able to do stuff
		synchronizer.add(qname);

		// there shouldn't be any info yet
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				assertNull("3.0." + resource.getFullPath(), actual);
			} catch (CoreException e) {
				fail("3.1." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("3.2", e);
		}

		// set the sync info
		visitor = resource -> {
			try {
				synchronizer.setSyncInfo(qname, resource, table.get(resource.getFullPath()));
			} catch (CoreException e) {
				fail("4.0." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("4.1", e);
		}

		// get the info and ensure its the same
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("5.0", actual);
					return true;
				}
				assertNotNull("5.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("5.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("5.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("5.4", e);
		}

		// change the info and then set it
		visitor = resource -> {
			if (resource.getType() == IResource.ROOT) {
				return true;
			}
			try {
				byte[] b = getRandomString().getBytes();
				synchronizer.setSyncInfo(qname, resource, b);
				table.put(resource.getFullPath(), b);
			} catch (CoreException e) {
				fail("6.0", e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("6.1", e);
		}

		// get the new info
		visitor = resource -> {
			try {
				byte[] actual = synchronizer.getSyncInfo(qname, resource);
				if (resource.getType() == IResource.ROOT) {
					assertNull("7.0", actual);
					return true;
				}
				assertNotNull("7.1." + resource.getFullPath(), actual);
				byte[] expected = table.get(resource.getFullPath());
				assertEquals("7.2." + resource.getFullPath(), expected, actual);
			} catch (CoreException e) {
				fail("7.3." + resource.getFullPath(), e);
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("7.4", e);
		}

		// cleanup
		synchronizer.remove(qname);

		// should not be able to get sync info because the target has been unregistered
		visitor = resource -> {
			try {
				synchronizer.getSyncInfo(qname, resource);
				assertTrue("9.0." + resource.getFullPath(), false);
			} catch (CoreException e) {
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor);
		} catch (CoreException e) {
			fail("9.1", e);
		}
	}

	/**
	 * Removes resources, sets sync info to <code>null</code> and ensures the
	 * phantoms do not exist any more (see bug 3024)
	 */
	public void testPhantomRemoval() {
		final QualifiedName partner = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final IWorkspace workspace = getWorkspace();
		final ISynchronizer synchronizer = workspace.getSynchronizer();

		// set up
		synchronizer.add(partner);
		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("foo");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		ensureExistsInWorkspace(new IResource[] {file1, file2}, true);

		// sets sync info for the folder and its children
		try {
			synchronizer.setSyncInfo(partner, folder, getRandomString().getBytes());
			synchronizer.setSyncInfo(partner, file1, getRandomString().getBytes());
			synchronizer.setSyncInfo(partner, file2, getRandomString().getBytes());
		} catch (CoreException ce) {
			fail("1.0", ce);
		}

		// 1) tests with one child first
		assertTrue("1.1", file1.exists());
		assertTrue("1.2", !file1.isPhantom());
		// deletes file
		try {
			file1.delete(true, getMonitor());
		} catch (CoreException ce) {
			fail("2.0", ce);
		}
		// file is now a phantom resource
		assertTrue("2.1", !file1.exists());
		assertTrue("2.2", file1.isPhantom());
		// removes sync info
		try {
			synchronizer.setSyncInfo(partner, file1, null);
		} catch (CoreException ce) {
			fail("3.0", ce);
		}
		// phantom should not exist any more
		assertTrue("3.1", !file1.exists());
		assertTrue("3.2", !file1.isPhantom());

		// 2) tests with the folder and remaining child
		assertTrue("4.1", folder.exists());
		assertTrue("4.2", !folder.isPhantom());
		assertTrue("4.3", file2.exists());
		assertTrue("4.4", !file2.isPhantom());
		// deletes the folder and its only child
		try {
			folder.delete(true, getMonitor());
		} catch (CoreException ce) {
			fail("5.0", ce);
		}
		// both resources are now phantom resources
		assertTrue("5.1", !folder.exists());
		assertTrue("5.2", folder.isPhantom());
		assertTrue("5.3", !file2.exists());
		assertTrue("5.4", file2.isPhantom());
		// removes only folder sync info
		try {
			synchronizer.setSyncInfo(partner, folder, null);
		} catch (CoreException ce) {
			fail("6.0", ce);
		}
		// phantoms should not exist any more
		assertTrue("6.1", !folder.exists());
		assertTrue("6.2", !folder.isPhantom());
		assertTrue("6.3", !file2.exists());
		assertTrue("6.4", !file2.isPhantom());

		// clean-up
		synchronizer.remove(partner);
		ensureDoesNotExistInWorkspace(project);
	}
}
