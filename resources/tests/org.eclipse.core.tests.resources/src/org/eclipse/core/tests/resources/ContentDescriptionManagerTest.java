/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.internal.content.*;
import org.eclipse.core.internal.resources.ContentDescriptionManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ContentDescriptionManagerTest extends ResourceTest {

	private static final String CONTENT_TYPE_RELATED_NATURE1 = "org.eclipse.core.tests.resources.contentTypeRelated1";
	private static final String CONTENT_TYPE_RELATED_NATURE2 = "org.eclipse.core.tests.resources.contentTypeRelated2";

	/**
	 * Blocks the calling thread until the cache flush job completes.
	 */
	public static void waitForCacheFlush() {
		try {
			Job.getJobManager().wakeUp(ContentDescriptionManager.FAMILY_DESCRIPTION_CACHE_FLUSH);
			Job.getJobManager().join(ContentDescriptionManager.FAMILY_DESCRIPTION_CACHE_FLUSH, null);
		} catch (OperationCanceledException | InterruptedException e) {
			//ignore
		}
	}

	private IContentDescription getDescription(String tag, IFile file) {
		IContentDescription description;
		description = null;
		try {
			description = file.getContentDescription();
		} catch (CoreException e) {
			fail(tag, e);
		}
		return description;
	}

	protected InputStream projectDescriptionWithNatures(String project, String[] natures) {
		StringBuilder contents = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><projectDescription><name>" + project + "</name><natures>");
		for (String nature : natures) {
			contents.append("<nature>" + nature + "</nature>");
		}
		contents.append("</natures></projectDescription>");
		return new ByteArrayInputStream(contents.toString().getBytes());
	}

	/**
	 * Ensure we react to changes to the content type registry in an appropriated way.
	 */
	public void testBug79151() {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("MyProject");
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		String newExtension = "xml_bug_79151";
		IFile file1 = project.getFile("file.xml");
		IFile file2 = project.getFile("file." + newExtension);
		ensureExistsInWorkspace(file1, getContents(CharsetTest.SAMPLE_XML_ISO_8859_1_ENCODING));
		ensureExistsInWorkspace(file2, getContents(CharsetTest.SAMPLE_XML_US_ASCII_ENCODING));
		// ensure we start in a known state
		((Workspace) workspace).getContentDescriptionManager().invalidateCache(true, null);
		// wait for cache flush to finish
		waitForCacheFlush();
		// cache is new at this point
		assertEquals("0.9", ContentDescriptionManager.EMPTY_CACHE, ((Workspace) workspace).getContentDescriptionManager().getCacheState());

		IContentDescription description1a = null, description1b = null, description1c = null, description1d = null;
		IContentDescription description2 = null;
		description1a = getDescription("1.0a", file1);
		description2 = getDescription("1.0b", file2);
		assertNotNull("1.1", description1a);
		assertEquals("1.2", xml, description1a.getContentType());
		assertNull("1.3", description2);

		description1b = getDescription("2.0", file1);
		// ensure it comes from the cache (should be the very same object)
		assertNotNull(" 2.1", description1b);
		assertSame("2.2", description1a, description1b);
		try {
			// change the content type
			xml.addFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			description1c = getDescription("4.0a", file1);
			description2 = getDescription("4.0b", file2);
			// ensure it does *not* come from the cache (should be a different object)
			assertNotNull("4.1", description1c);
			assertNotSame("4.2", description1a, description1c);
			assertEquals("4.3", xml, description1c.getContentType());
			assertNotNull("4.4", description2);
			assertEquals("4.5", xml, description2.getContentType());
		} finally {
			try {
				// dissociate the xml2 extension from the XML content type
				xml.removeFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
			} catch (CoreException e) {
				fail("4.99", e);
			}
		}
		description1d = getDescription("5.0a", file1);
		description2 = getDescription("5.0b", file2);
		// ensure it does *not* come from the cache (should be a different object)
		assertNotNull("5.1", description1d);
		assertNotSame("5.2", description1c, description1d);
		assertEquals("5.3", xml, description1d.getContentType());
		assertNull("5.4", description2);
	}

	public void testBug94516() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		assertNotNull("0.1", text);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFile unrelatedFile = project.getFile("file." + getName());
		ensureExistsInWorkspace(unrelatedFile, "");

		IContentDescription description = null;
		description = getDescription("0.7", unrelatedFile);
		assertNull("0.8", description);

		try {
			try {
				text.addFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);
			} catch (CoreException e) {
				fail("0.99", e);
			}

			description = getDescription("1.0", unrelatedFile);
			assertNotNull("1.1", description);
			assertEquals("1.2", text, description.getContentType());

			final ProjectScope projectScope = new ProjectScope(project);
			Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
			// enable project-specific settings for this project
			contentTypePrefs.putBoolean("enabled", true);
			try {
				contentTypePrefs.flush();
			} catch (BackingStoreException e) {
				fail("1.99", e);
			}
			// global settings should not matter anymore
			description = getDescription("2.0", unrelatedFile);
			assertNull("2.1", description);

			IContentTypeSettings settings = null;
			try {
				settings = text.getSettings(projectScope);
			} catch (CoreException e) {
				fail("3.0", e);
			}
			assertNotNull("3.1", settings);
			assertNotSame("3.2", text, settings);
			assertTrue("3.3", settings instanceof ContentTypeSettings);

			try {
				settings.addFileSpec(unrelatedFile.getFullPath().getFileExtension(), IContentType.FILE_EXTENSION_SPEC);
			} catch (CoreException e) {
				fail("4.0", e);
			}
			try {
				contentTypePrefs.flush();
			} catch (BackingStoreException e) {
				fail("4.1", e);
			}
			description = getDescription("5.0", unrelatedFile);
			assertNotNull("5.1", description);
			assertEquals("5.2", text, description.getContentType());
		} finally {
			try {
				text.removeFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);
			} catch (CoreException e) {
				fail("6.0", e);
			}
		}
	}

	/**
	 * Ensures content type-nature associations work as expected.
	 */
	public void testNatureContentTypeAssociation() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType baseType = contentTypeManager.getContentType("org.eclipse.core.tests.resources.nature_associated_1");
		IContentType derivedType = contentTypeManager.getContentType("org.eclipse.core.tests.resources.nature_associated_2");
		assertNotNull("0.1", baseType);
		assertNotNull("0.2", derivedType);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFile file = project.getFile("file.nature-associated");
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ensureExistsInWorkspace(file, "it really does not matter");
		IContentDescription description = null;

		// originally, project description has no natures
		try {
			descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[0]), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		waitForCacheFlush();
		description = getDescription("1.1", file);
		assertNotNull("1.2", description);
		assertSame("1.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include one of the natures
		try {
			descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[] {CONTENT_TYPE_RELATED_NATURE1}), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		waitForCacheFlush();
		description = getDescription("2.1", file);
		assertNotNull("2.2", description);
		assertSame("2.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include the other nature
		try {
			descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[] {CONTENT_TYPE_RELATED_NATURE2}), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
		waitForCacheFlush();
		description = getDescription("3.1", file);
		assertNotNull("3.2", description);
		assertSame("3.3", ((ContentTypeHandler) derivedType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include both of the natures
		try {
			descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[] {CONTENT_TYPE_RELATED_NATURE1, CONTENT_TYPE_RELATED_NATURE2}), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		waitForCacheFlush();

		description = getDescription("4.1", file);
		assertNotNull("4.2", description);
		assertSame("4.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// back to no natures
		try {
			descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[0]), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
		waitForCacheFlush();
		description = getDescription("5.1", file);
		assertNotNull("5.2", description);
		assertSame("5.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());
	}

	public void testProjectSpecificCharset() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		assertNotNull("0.1", text);
		assertNotNull("0.2", xml);
		IProject project = getWorkspace().getRoot().getProject("proj1");

		IFile txtFile = project.getFile(getName() + ".txt");
		IFile xmlFile = project.getFile(getName() + ".xml");

		ensureExistsInWorkspace(txtFile, "");
		ensureExistsInWorkspace(xmlFile, "");

		project.setDefaultCharset("FOO", getMonitor());
		assertEquals("1.0", "FOO", txtFile.getCharset());
		assertEquals("1.1", "UTF-8", xmlFile.getCharset());

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("2.0", e);
		}
		IContentTypeSettings settings = null;
		settings = text.getSettings(projectScope);
		settings.setDefaultCharset("BAR");
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("2.1", e);
		}
		assertEquals("3.0", "BAR", txtFile.getCharset());
		assertEquals("3.1", "UTF-8", xmlFile.getCharset());

		settings = xml.getSettings(projectScope);
		settings.setDefaultCharset("");
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("4.0", e);
		}
		assertEquals("4.1", "BAR", txtFile.getCharset());
		assertEquals("4.2", "FOO", xmlFile.getCharset());
	}

	public void testProjectSpecificFileAssociations() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		assertNotNull("0.1", text);
		assertNotNull("0.2", xml);
		IProject project = getWorkspace().getRoot().getProject("proj1");

		IFile txtFile = project.getFile(getName() + ".txt");
		IFile xmlFile = project.getFile(getName() + ".xml");
		IFile unrelatedFile = project.getFile("file." + getName());

		ensureExistsInWorkspace(txtFile, "");
		ensureExistsInWorkspace(xmlFile, "");
		ensureExistsInWorkspace(unrelatedFile, "");
		IContentDescription description = null;

		description = getDescription("0.7a", txtFile);
		assertNotNull("0.7b", description);
		assertEquals("0.7c", text, description.getContentType());

		description = getDescription("0.8a", xmlFile);
		assertNotNull("0.8b", description);
		assertEquals("0.8c", xml, description.getContentType());

		assertNull("0.9b", getDescription("0.9a", unrelatedFile));

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("0.99", e);
		}
		// there are no local settings yet, everything should be the same
		description = getDescription("1.0a", txtFile);
		assertNotNull("1.0b", description);
		assertEquals("1.0c", text, description.getContentType());

		description = getDescription("1.1a", xmlFile);
		assertNotNull("1.1b", description);
		assertEquals("1.1c", xml, description.getContentType());

		assertNull("1.2b", getDescription("1.2a", unrelatedFile));

		IContentTypeSettings settings = null;
		try {
			settings = text.getSettings(projectScope);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertNotNull("2.1", settings);
		assertNotSame("2.2", text, settings);
		assertTrue("2.3", settings instanceof ContentTypeSettings);

		try {
			settings.addFileSpec(getName(), IContentTypeSettings.FILE_EXTENSION_SPEC);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("3.1", e);
		}
		description = getDescription("3.2a", unrelatedFile);
		assertNotNull("3.2b", description);
		assertEquals("3.2c", text, description.getContentType());

		// other content types should still be recognized
		description = getDescription("3.3a", txtFile);
		assertNotNull("3.3b", description);
		assertEquals("3.3c", text, description.getContentType());

		description = getDescription("3.4a", xmlFile);
		assertNotNull("3.4b", description);
		assertEquals("3.4c", xml, description.getContentType());

		// disable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", false);
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("3.99", e);
		}

		// no project settings should be in effect
		description = getDescription("4.0a", txtFile);
		assertNotNull("4.0b", description);
		assertEquals("4.0c", text, description.getContentType());

		description = getDescription("4.1a", xmlFile);
		assertNotNull("4.1b", description);
		assertEquals("4.1c", xml, description.getContentType());

		assertNull("4.2b", getDescription("4.2a", unrelatedFile));

		// enable project-specific settings again
		contentTypePrefs.putBoolean("enabled", true);
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("4.99", e);
		}

		// now associate the full name of the xml file to the text content type
		try {
			settings.addFileSpec(xmlFile.getName(), IContentTypeSettings.FILE_NAME_SPEC);
		} catch (CoreException e) {
			fail("5.0", e);
		}
		try {
			contentTypePrefs.flush();
		} catch (BackingStoreException e) {
			fail("5.1", e);
		}

		description = getDescription("5.2a", unrelatedFile);
		assertNotNull("5.2b", description);
		assertEquals("5.2c", text, description.getContentType());

		description = getDescription("5.3a", txtFile);
		assertNotNull("5.3b", description);
		assertEquals("5.3c", text, description.getContentType());

		description = getDescription("5.4a", xmlFile);
		assertNotNull("5.4b", description);
		assertEquals("5.4c", text, description.getContentType());

	}

}
