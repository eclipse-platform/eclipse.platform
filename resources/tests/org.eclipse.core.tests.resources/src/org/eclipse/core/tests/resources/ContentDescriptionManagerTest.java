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
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

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
	public void testBug79151() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("MyProject");
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType xml = contentTypeManager.getContentType("org.eclipse.core.runtime.xml");
		String newExtension = "xml_bug_79151";
		IFile file1 = project.getFile("file.xml");
		IFile file2 = project.getFile("file." + newExtension);
		ensureExistsInWorkspace(file1, CharsetTest.SAMPLE_XML_ISO_8859_1_ENCODING);
		ensureExistsInWorkspace(file2, CharsetTest.SAMPLE_XML_US_ASCII_ENCODING);
		// ensure we start in a known state
		((Workspace) workspace).getContentDescriptionManager().invalidateCache(true, null);
		// wait for cache flush to finish
		waitForCacheFlush();
		// cache is new at this point
		assertEquals("0.9", ContentDescriptionManager.EMPTY_CACHE, ((Workspace) workspace).getContentDescriptionManager().getCacheState());

		IContentDescription description1a = null, description1b = null, description1c = null, description1d = null;
		IContentDescription description2 = null;
		description1a = file1.getContentDescription();
		description2 = file2.getContentDescription();
		assertNotNull("1.1", description1a);
		assertEquals("1.2", xml, description1a.getContentType());
		assertNull("1.3", description2);

		description1b = file1.getContentDescription();
		// ensure it comes from the cache (should be the very same object)
		assertNotNull(" 2.1", description1b);
		assertSame("2.2", description1a, description1b);
		try {
			// change the content type
			xml.addFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
			description1c = file1.getContentDescription();
			description2 = file2.getContentDescription();
			// ensure it does *not* come from the cache (should be a different object)
			assertNotNull("4.1", description1c);
			assertNotSame("4.2", description1a, description1c);
			assertEquals("4.3", xml, description1c.getContentType());
			assertNotNull("4.4", description2);
			assertEquals("4.5", xml, description2.getContentType());
		} finally {
			// dissociate the xml2 extension from the XML content type
			xml.removeFileSpec(newExtension, IContentType.FILE_EXTENSION_SPEC);
		}
		description1d = file1.getContentDescription();
		description2 = file2.getContentDescription();
		// ensure it does *not* come from the cache (should be a different object)
		assertNotNull("5.1", description1d);
		assertNotSame("5.2", description1c, description1d);
		assertEquals("5.3", xml, description1d.getContentType());
		assertNull("5.4", description2);
	}

	public void testBug94516() throws Exception {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType text = contentTypeManager.getContentType("org.eclipse.core.runtime.text");
		assertNotNull("0.1", text);
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFile unrelatedFile = project.getFile("file." + getName());
		ensureExistsInWorkspace(unrelatedFile, "");

		IContentDescription description = null;
		description = unrelatedFile.getContentDescription();
		assertNull("0.8", description);

		try {
			text.addFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);

			description = unrelatedFile.getContentDescription();
			assertNotNull("1.1", description);
			assertEquals("1.2", text, description.getContentType());

			final ProjectScope projectScope = new ProjectScope(project);
			Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
			// enable project-specific settings for this project
			contentTypePrefs.putBoolean("enabled", true);
			contentTypePrefs.flush();
			// global settings should not matter anymore
			description = unrelatedFile.getContentDescription();
			assertNull("2.1", description);

			IContentTypeSettings settings = null;
			settings = text.getSettings(projectScope);
			assertNotNull("3.1", settings);
			assertNotSame("3.2", text, settings);
			assertTrue("3.3", settings instanceof ContentTypeSettings);

			settings.addFileSpec(unrelatedFile.getFullPath().getFileExtension(), IContentType.FILE_EXTENSION_SPEC);
			contentTypePrefs.flush();
			description = unrelatedFile.getContentDescription();
			assertNotNull("5.1", description);
			assertEquals("5.2", text, description.getContentType());
		} finally {
			text.removeFileSpec(unrelatedFile.getName(), IContentType.FILE_NAME_SPEC);
		}
	}

	/**
	 * Ensures content type-nature associations work as expected.
	 */
	public void testNatureContentTypeAssociation() throws Exception {
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
		try (InputStream input = projectDescriptionWithNatures(project.getName(), new String[0])) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull("1.2", description);
		assertSame("1.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include one of the natures
		try (InputStream input = projectDescriptionWithNatures(project.getName(), new String[] { CONTENT_TYPE_RELATED_NATURE1 })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull("2.2", description);
		assertSame("2.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include the other nature
		try (InputStream input = projectDescriptionWithNatures(project.getName(),
				new String[] { CONTENT_TYPE_RELATED_NATURE2 })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull("3.2", description);
		assertSame("3.3", ((ContentTypeHandler) derivedType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// change project description to include both of the natures
		try (InputStream input = projectDescriptionWithNatures(project.getName(),
				new String[] { CONTENT_TYPE_RELATED_NATURE1, CONTENT_TYPE_RELATED_NATURE2  })) {
			descFile.setContents(input, IResource.FORCE, createTestMonitor());
		}
		waitForCacheFlush();

		description = file.getContentDescription();
		assertNotNull("4.2", description);
		assertSame("4.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());

		// back to no natures
		descFile.setContents(projectDescriptionWithNatures(project.getName(), new String[0]), IResource.FORCE,
				createTestMonitor());
		waitForCacheFlush();
		description = file.getContentDescription();
		assertNotNull("5.2", description);
		assertSame("5.3", ((ContentTypeHandler) baseType).getTarget(), ((ContentTypeHandler) description.getContentType()).getTarget());
	}

	public void testProjectSpecificCharset() throws Exception {
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

		project.setDefaultCharset("FOO", createTestMonitor());
		assertEquals("1.0", "FOO", txtFile.getCharset());
		assertEquals("1.1", "UTF-8", xmlFile.getCharset());

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();
		IContentTypeSettings settings = null;
		settings = text.getSettings(projectScope);
		settings.setDefaultCharset("BAR");
		contentTypePrefs.flush();
		assertEquals("3.0", "BAR", txtFile.getCharset());
		assertEquals("3.1", "UTF-8", xmlFile.getCharset());

		settings = xml.getSettings(projectScope);
		settings.setDefaultCharset("");
		contentTypePrefs.flush();
		assertEquals("4.1", "BAR", txtFile.getCharset());
		assertEquals("4.2", "FOO", xmlFile.getCharset());
	}

	public void testProjectSpecificFileAssociations() throws Exception {
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

		description = txtFile.getContentDescription();
		assertNotNull("0.7b", description);
		assertEquals("0.7c", text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull("0.8b", description);
		assertEquals("0.8c", xml, description.getContentType());

		assertNull("0.9b", unrelatedFile.getContentDescription());

		final ProjectScope projectScope = new ProjectScope(project);
		Preferences contentTypePrefs = projectScope.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE);
		// enable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();
		// there are no local settings yet, everything should be the same
		description = txtFile.getContentDescription();
		assertNotNull("1.0b", description);
		assertEquals("1.0c", text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull("1.1b", description);
		assertEquals("1.1c", xml, description.getContentType());

		assertNull("1.2b", unrelatedFile.getContentDescription());

		IContentTypeSettings settings = null;
		settings = text.getSettings(projectScope);
		assertNotNull("2.1", settings);
		assertNotSame("2.2", text, settings);
		assertTrue("2.3", settings instanceof ContentTypeSettings);

		settings.addFileSpec(getName(), IContentTypeSettings.FILE_EXTENSION_SPEC);
		contentTypePrefs.flush();
		description = unrelatedFile.getContentDescription();
		assertNotNull("3.2b", description);
		assertEquals("3.2c", text, description.getContentType());

		// other content types should still be recognized
		description = txtFile.getContentDescription();
		assertNotNull("3.3b", description);
		assertEquals("3.3c", text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull("3.4b", description);
		assertEquals("3.4c", xml, description.getContentType());

		// disable project-specific settings for this project
		contentTypePrefs.putBoolean("enabled", false);
		contentTypePrefs.flush();

		// no project settings should be in effect
		description = txtFile.getContentDescription();
		assertNotNull("4.0b", description);
		assertEquals("4.0c", text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull("4.1b", description);
		assertEquals("4.1c", xml, description.getContentType());

		assertNull("4.2b", unrelatedFile.getContentDescription());

		// enable project-specific settings again
		contentTypePrefs.putBoolean("enabled", true);
		contentTypePrefs.flush();

		// now associate the full name of the xml file to the text content type
		settings.addFileSpec(xmlFile.getName(), IContentTypeSettings.FILE_NAME_SPEC);
		contentTypePrefs.flush();

		description = unrelatedFile.getContentDescription();
		assertNotNull("5.2b", description);
		assertEquals("5.2c", text, description.getContentType());

		description = txtFile.getContentDescription();
		assertNotNull("5.3b", description);
		assertEquals("5.3c", text, description.getContentType());

		description = xmlFile.getContentDescription();
		assertNotNull("5.4b", description);
		assertEquals("5.4c", text, description.getContentType());
	}

}
