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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.internal.content.ContentTypeHandler;
import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.content.ContentTypeSettings;
import org.eclipse.core.internal.resources.ContentDescriptionManager;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.prefs.Preferences;

@ExtendWith(WorkspaceResetExtension.class)
public class ContentDescriptionManagerTest {

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
	@Test
	public void testBug79151() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("MyProject");
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		String newExtension = "xml_bug_79151";
		IFile file1 = project.getFile("file.xml");
		IFile file2 = project.getFile("file." + newExtension);
		createInWorkspace(file1, CharsetTest.SAMPLE_XML_ISO_8859_1_ENCODING);
		createInWorkspace(file2, CharsetTest.SAMPLE_XML_US_ASCII_ENCODING);
		// ensure we start in a known state
		((Workspace) workspace).getContentDescriptionManager().invalidateCache(true, null);
		// wait for cache flush to finish
		waitForCacheFlush();
		// cache is new at this point
		assertEquals(ContentDescriptionManager.EMPTY_CACHE,
				((Workspace) workspace).getContentDescriptionManager().getCacheState());

		IContentDescription description1a = null, description1b = null, description1c = null, description1d = null;
		IContentDescription description2 = null;
		description1a = file1.getContentDescription();
		description2 = file2.getContentDescription();
		assertNotNull(description1a);
		assertEquals(xml, description1a.getContentType());
		assertNull(description2);

		description1b = file1.getContentDescription();
		// ensure it comes from the cache (should be the very same object)
		assertNotNull(description1b);
		assertSame(description1a, description1b);
		try {
			// change the content type
			xml.addFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
			description1c = file1.getContentDescription();
			description2 = file2.getContentDescription();
			// ensure it does *not* come from the cache (should be a different object)
			assertNotNull(description1c);
			assertNotSame(description1a, description1c);
			assertEquals(xml, description1c.getContentType());
			assertNotNull(description2);
			assertEquals(xml, description2.getContentType());
		} finally {
			// dissociate the xml2 extension from the XML content type
			xml.removeFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
		}
		description1d = file1.getContentDescription();
		description2 = file2.getContentDescription();
		// ensure it does *not* come from the cache (should be a different object)
		assertNotNull(description1d);
		assertNotSame(description1c, description1d);
		assertEquals(xml, description1d.getContentType());
		assertNull(description2);
	}

	@Test
	public void testBug94516() throws Exception {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		assertNotNull(text);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFile unrelatedFile = project.getFile("file.unrelated");
		createInWorkspace(unrelatedFile, "");

		IContentDescription description = null;
		description = unrelatedFile.getContentDescription();
		assertNull(description);

		try {
			text.addFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);

			description = unrelatedFile.getContentDescription();
			assertNotNull(description);
			assertEquals(text, description.getContentType());

			final ProjectScope projectScope = new ProjectScope(project);
			Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
			// enable project-specific settings for this project
			contentTypePrefs.putBoolean("enabled", true);
			contentTypePrefs.flush();
			// global settings should not matter anymore
			description = unrelatedFile.getContentDescription();
			assertNull(description);

			IContentTypeSettings settings = null;
			settings = text.getSettings(projectScope);
			assertNotNull(settings);
			assertNotSame(text, settings);
			assertTrue(settings instanceof ContentTypeSettings);

			settings.addFileSpec(unrelatedFile.getFullPath().getFileExtension(), IContentType.FILE_EXTENSION_SPEC);
			contentTypePrefs.flush();
			description = unrelatedFile.getContentDescription();
			assertNotNull(description);
			assertEquals(text, description.getContentType());
		} finally {
			text.removeFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);
		}
	}

	/**
	 * Ensures content type-nature associations work as expected.
	 */
	@Test
	public void testNatureContentTypeAssociation() throws Exception {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType baseType = contentTypeManager.getContentType("org.eclipse.core.tests.resources.nature_associated_1");
		IContentType derivedType = contentTypeManager.getContentType("org.eclipse.core.tests.resources.nature_associated_2");
		assertNotNull(baseType);
		assertNotNull(derivedType);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFile file = project.getFile("file.nature-associated");
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		createInWorkspace(file, "it really does not matter");
		IContentDescription description = null;

		// originally, project description has no natures
		try (InputStream input = projectDescriptionWithNatures(project.getName(), new String[0])) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull(description);
		assertSame(((ContentTypeHandler) baseType).getTarget(),
				((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include one of the natures
		try (InputStream input = projectDescriptionWithNatures(project.getName(), new String[] { CONTENT_TYPE_RELATED_NATURE1 })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull(description);
		assertSame(((ContentTypeHandler) baseType).getTarget(),
				((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include the other nature
		try (InputStream input = projectDescriptionWithNatures(project.getName(),
				new String[] { CONTENT_TYPE_RELATED_NATURE2 })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull(description);
		assertSame(((ContentTypeHandler) derivedType).getTarget(),
				((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include both of the natures
		try (InputStream input = projectDescriptionWithNatures(project.getName(),
				new String[] { CONTENT_TYPE_RELATED_NATURE1, CONTENT_TYPE_RELATED_NATURE2  })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();

		description = file.getContentDescription();
		assertNotNull(description);
		assertSame(((ContentTypeHandler) baseType).getTarget(),
				((ContentTypeHandler) description.getContentType()).getTarget());

		// back to no natures
		descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[0]), IResource.FORCE,
				createTestMonitor());
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull(description);
		assertSame(((ContentTypeHandler) baseType).getTarget(),
				((ContentTypeHandler) description.getContentType()).getTarget());
	}

	@Test
	public void testProjectSpecificCharset() throws Exception {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		assertNotNull(text);
		assertNotNull(xml);
		IProject project = getWorkspace().getRoot().getProject("proj1");

		IFile txtFile = project.getFile("example.txt");
		IFile xmlFile = project.getFile("example.xml");

		createInWorkspace(txtFile, "");
		createInWorkspace(xmlFile, "");

		project.setDefaultCharset("FOO", createTestMonitor());
		assertEquals("FOO", txtFile.getCharset());
		assertEquals("UTF-8", xmlFile.getCharset());

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();
		IContentTypeSettings settings = null;
		settings = text.getSettings(projectScope);
		settings.setDefaultCharset("BAR");
		contentTypePrefs.flush();
		assertEquals("BAR", txtFile.getCharset());
		assertEquals("UTF-8", xmlFile.getCharset());

		settings = xml.getSettings(projectScope);
		settings.setDefaultCharset("");
		contentTypePrefs.flush();
		assertEquals("BAR", txtFile.getCharset());
		assertEquals("FOO", xmlFile.getCharset());
	}

	@Test
	public void testProjectSpecificFileAssociations() throws Exception {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		assertNotNull(text);
		assertNotNull(xml);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		String unrelatedFileExtension = "unrelated";

		IFile txtFile = project.getFile("example.txt");
		IFile xmlFile = project.getFile("example.xml");
		IFile unrelatedFile = project.getFile("file." + unrelatedFileExtension);

		createInWorkspace(txtFile, "");
		createInWorkspace(xmlFile, "");
		createInWorkspace(unrelatedFile, "");
		IContentDescription description = null;

		description = txtFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull(description);
		assertEquals(xml, description.getContentType());

		assertNull(unrelatedFile.getContentDescription());

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();
		// there are no local settings yet, everything should be the same
		description = txtFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull(description);
		assertEquals(xml, description.getContentType());

		assertNull(unrelatedFile.getContentDescription());

		IContentTypeSettings settings = null;
		settings = text.getSettings(projectScope);
		assertNotNull(settings);
		assertNotSame(text, settings);
		assertTrue(settings instanceof ContentTypeSettings);

		settings.addFileSpec(unrelatedFileExtension, IContentTypeSettings.FILE_EXTENSION_SPEC);
		contentTypePrefs.flush();
		description = unrelatedFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		// other content types should still be recognized
		description = txtFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull(description);
		assertEquals(xml, description.getContentType());

		// disable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", false);
		contentTypePrefs.flush();

		// no project settings should be in effect
		description = txtFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull(description);
		assertEquals(xml, description.getContentType());

		assertNull(unrelatedFile.getContentDescription());

		// enable project-specific settings again
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();

		// now associate the full name of the xml file to the text content type
		settings.addFileSpec(xmlFile.getName(), IContentTypeSettings.FILE_NAME_SPEC);
		contentTypePrefs.flush();

		description = unrelatedFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = txtFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull(description);
		assertEquals(text, description.getContentType());
	}

}
