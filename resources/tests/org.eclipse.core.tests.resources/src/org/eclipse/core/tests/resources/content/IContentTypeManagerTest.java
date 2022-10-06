/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - [263316] regexp for file association
 *******************************************************************************/
package org.eclipse.core.tests.resources.content;

import static org.eclipse.core.tests.resources.AutomatedResourceTests.PI_RESOURCES_TESTS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.internal.content.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.content.IContentTypeManager.ContentTypeChangeEvent;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.core.tests.harness.*;
import org.junit.After;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class IContentTypeManagerTest extends ContentTypeTest {

	private static class ContentTypeChangeTracer implements IContentTypeManager.IContentTypeChangeListener {
		private final Set<IContentType> changed = new HashSet<>();

		public ContentTypeChangeTracer() {
		}

		@Override
		public void contentTypeChanged(ContentTypeChangeEvent event) {
			changed.add(event.getContentType());
		}

		public boolean isOnlyChange(IContentType myType) {
			return changed.size() == 1 && changed.contains(myType);
		}

		public void reset() {
			changed.clear();
		}
	}

	// XXX this is copied from CharsetDeltaJob in the resources plug-in
	private static final String FAMILY_CHARSET_DELTA = "org.eclipse.core.resources.charsetJobFamily";

	private final static String MINIMAL_XML = "<?xml version=\"1.0\"?><org.eclipse.core.resources.tests.root/>";
	private final static String SAMPLE_BIN1_OFFSET = "12345";
	private final static byte[] SAMPLE_BIN1_SIGNATURE = { 0x10, (byte) 0xAB, (byte) 0xCD, (byte) 0xFF };
	private final static String SAMPLE_BIN2_OFFSET = "";
	private final static byte[] SAMPLE_BIN2_SIGNATURE = { 0x10, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
	private final static String XML_DTD_EXTERNAL_ENTITY = "<?xml version=\"1.0\"?><!DOCTYPE project  SYSTEM \"org.eclipse.core.resources.tests.some.dtd\"  [<!ENTITY someentity SYSTEM \"someentity.xml\">]><org.eclipse.core.resources.tests.root/>";
	private final static String XML_DTD_US_ASCII = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?><!DOCTYPE sometype SYSTEM \"org.eclipse.core.resources.tests.some.dtd\"><org.eclipse.core.resources.tests.root/>";
	private final static String XML_ISO_8859_1 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><org.eclipse.core.resources.tests.root/>";
	private final static String XML_ISO_8859_1_SINGLE_QUOTES = "<?xml version='1.0' encoding='ISO-8859-1'?><org.eclipse.core.resources.tests.root/>";
	private final static String XML_ROOT_ELEMENT_EXTERNAL_ENTITY = "<?xml version=\"1.0\"?><!DOCTYPE project   [<!ENTITY someentity SYSTEM \"someentity.xml\">]><org.eclipse.core.resources.tests.root-element/>";
	private final static String XML_ROOT_ELEMENT_EXTERNAL_ENTITY2 = "<?xml version=\"1.0\"?><!DOCTYPE org.eclipse.core.resources.tests.root-element PUBLIC \"org.eclipse.core.resources.tests.root-elementId\" \"org.eclipse.core.resources.tests.root-element.dtd\" ><org.eclipse.core.resources.tests.root-element/>";
	private final static String XML_ROOT_ELEMENT_ISO_8859_1 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><org.eclipse.core.resources.tests.root-element/>";
	private final static String XML_ROOT_ELEMENT_NO_DECL = "<org.eclipse.core.resources.tests.root-element/>";
	private final static String XML_US_ASCII_INVALID = "<?xml version='1.0' encoding='us-ascii'?><!-- Non-ASCII chars: ����� --><org.eclipse.core.resources.tests.root/>";
	private final static String XML_UTF_16 = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><org.eclipse.core.resources.tests.root/>";
	private final static String XML_UTF_16BE = "<?xml version=\"1.0\" encoding=\"UTF-16BE\"?><org.eclipse.core.resources.tests.root/>";
	private final static String XML_UTF_16LE = "<?xml version=\"1.0\" encoding=\"UTF-16LE\"?><org.eclipse.core.resources.tests.root/>";
	private final static String XML_UTF_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><org.eclipse.core.resources.tests.root/>";

	// used also by FilePropertyTesterTest
	public static final String XML_ROOT_ELEMENT_NS_MATCH1 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><prefix:rootElement1 xmlns:prefix='urn:eclipse.core.runtime.ns1'/>";

	private static final String XML_ROOT_ELEMENT_NS_MATCH2 = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><!DOCTYPE rootElement2 SYSTEM \"org.eclipse.core.resources.tests.nothing\"><rootElement2 xmlns='urn:eclipse.core.runtime.ns2'/>";
	private static final String XML_ROOT_ELEMENT_NS_WRONG_ELEM = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rootElement3 xmlns='urn:eclipse.core.runtime.ns2'/>";
	private static final String XML_ROOT_ELEMENT_NS_WRONG_NS = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><prefix:rootElement1 xmlns='http://example.com/'/>";
	private static final String XML_ROOT_ELEMENT_NS_MIXUP = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rootElement2 xmlns='urn:eclipse.core.runtime.ns1'/>";
	private static final String XML_ROOT_ELEMENT_NS_WILDCARD = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><weCouldPutAnythingHere xmlns='urn:eclipse.core.runtime.nsWild'/>";
	private final static String XML_ROOT_ELEMENT_NS_WILDCARD2 = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?><!DOCTYPE Joker SYSTEM \"org.eclipse.core.resources.tests.some.dtd3\"><Joker/>";
	private final static String XML_ROOT_ELEMENT_EMPTY_NS = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?><!DOCTYPE Joker SYSTEM \"org.eclipse.core.resources.tests.some.dtd3\"><rootElement>";

	/**
	 * Helps to ensure we don't get fooled by case sensitivity in file names/specs.
	 */
	private String changeCase(String original) {
		StringBuilder result = new StringBuilder(original);
		for (int i = result.length() - 1; i >= 0; i--) {
			char originalChar = original.charAt(i);
			result.setCharAt(i, i % 2 == 0 ? Character.toLowerCase(originalChar) : Character.toUpperCase(originalChar));
		}
		return result.toString();
	}

	boolean contains(Object[] array, Object element) {
		for (Object element2 : array) {
			if (element2.equals(element)) {
				return true;
			}
		}
		return false;
	}

	private IContentDescription getDescriptionFor(IContentTypeMatcher finder, String contents, Charset encoding,
			String fileName, QualifiedName[] options, boolean text) throws IOException {
		return text ? finder.getDescriptionFor(getReader(contents), fileName, options)
				: finder.getDescriptionFor(getInputStream(contents, encoding), fileName, options);
	}

	public InputStream getInputStream(byte[][] contents) {
		int size = 0;
		// computes final array size
		for (byte[] content : contents) {
			size += content.length;
		}
		byte[] full = new byte[size];
		int fullIndex = 0;
		// concatenates all byte arrays
		for (byte[] content : contents) {
			for (byte element : content) {
				full[fullIndex++] = element;
			}
		}
		return new ByteArrayInputStream(full);
	}

	public InputStream getInputStream(String contents) {
		return new ByteArrayInputStream(contents.getBytes());
	}

	public InputStream getInputStream(String contents, Charset encoding) {
		return new ByteArrayInputStream(encoding == null ? contents.getBytes() : contents.getBytes(encoding));
	}

	public Reader getReader(String contents) {
		return new CharArrayReader(contents.toCharArray());
	}

	private boolean isText(IContentTypeManager manager, IContentType candidate) {
		IContentType text = manager.getContentType(IContentTypeManager.CT_TEXT);
		return candidate.isKindOf(text);
	}

	@After
	public void tearDown() throws Exception {
		// some tests here will trigger a charset delta job (any causing
		// ContentTypeChangeEvents to be broadcast)
		// ensure none is left running after we finish
		Job.getJobManager().join(FAMILY_CHARSET_DELTA, new FussyProgressMonitor());
	}

	/**
	 * This test shows how we deal with aliases.
	 */
	@Test
	public void testAlias() throws IOException {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType alias = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".alias");
		assertNotNull("0.7", alias);
		IContentType derived = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".derived-from-alias");
		assertNotNull("0.8", derived);
		IContentType target = contentTypeManager.getContentType("org.eclipse.bundle02.missing-target");
		assertNull("0.9", target);
		IContentType[] selected;
		selected = contentTypeManager.findContentTypesFor("foo.missing-target");
		assertEquals("1.1", 2, selected.length);
		assertEquals("1.2", alias, selected[0]);
		assertEquals("1.3", derived, selected[1]);
		selected = contentTypeManager.findContentTypesFor(getRandomContents(), "foo.missing-target");
		assertEquals("1.4", 2, selected.length);
		assertEquals("1.5", alias, selected[0]);
		assertEquals("1.6", derived, selected[1]);

		// test late addition of content type
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles("2", () -> {
			IContentType alias1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".alias");
			assertNull("2.1.1", alias1);
			IContentType derived1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".derived-from-alias");
			assertNotNull("2.1.2", derived1);
			IContentType target1 = contentTypeManager.getContentType("org.eclipse.bundle02.missing-target");
			assertNotNull("2.1.3", target1);
			// checks associations
			IContentType[] selected1 = contentTypeManager.findContentTypesFor("foo.missing-target");
			assertEquals("2.2.1", 2, selected1.length);
			assertEquals("2.2.2", target1, selected1[0]);
			assertEquals("2.2.3", derived1, selected1[1]);
			IContentType[] selected2;
			try {
				selected2 = contentTypeManager.findContentTypesFor(getRandomContents(), "foo.missing-target");
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			assertEquals("2.2.5", 2, selected2.length);
			assertEquals("2.2.6", target1, selected2[0]);
			assertEquals("2.2.7", derived1, selected2[1]);
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bundle02" }, listener);
	}

	@Test
	public void testAssociationInheritance() throws CoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = manager.getMatcher(new LocalSelectionPolicy(), null);
		IContentType text = manager.getContentType(Platform.PI_RUNTIME + ".text");
		IContentType assoc1 = manager.getContentType(PI_RESOURCES_TESTS + ".assoc1");
		IContentType assoc2 = manager.getContentType(PI_RESOURCES_TESTS + ".assoc2");

		// associate a user-defined file spec
		text.addFileSpec("txt_useradded", IContentType.FILE_EXTENSION_SPEC);
		assoc1.addFileSpec("txt_assoc1useradded", IContentType.FILE_EXTENSION_SPEC);
		assoc2.addFileSpec("txt_assoc2useradded", IContentType.FILE_EXTENSION_SPEC);

		// test associations
		assertTrue("1.1", assoc1.isAssociatedWith(changeCase("text.txt")));
		assertTrue("1.2", assoc1.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue("1.3", assoc1.isAssociatedWith(changeCase("text.txt_pluginadded")));
		assertTrue("1.4", assoc1.isAssociatedWith(changeCase("text.txt_assoc1pluginadded")));
		assertTrue("1.5", assoc1.isAssociatedWith(changeCase("text.txt_assoc1useradded")));

		assertFalse("2.1", assoc2.isAssociatedWith(changeCase("text.txt")));
		assertFalse("2.2", assoc2.isAssociatedWith(changeCase("text.txt_useradded")));
		assertFalse("2.3", assoc2.isAssociatedWith(changeCase("text.txt_pluginadded")));
		assertTrue("2.4", assoc2.isAssociatedWith(changeCase("text.txt_assoc2pluginadded")));
		assertTrue("2.5", assoc2.isAssociatedWith(changeCase("text.txt_assoc2builtin")));
		assertTrue("2.6", assoc2.isAssociatedWith(changeCase("text.txt_assoc2useradded")));

		IContentType[] selected;
		// text built-in associations
		selected = finder.findContentTypesFor(changeCase("text.txt"));
		assertEquals("3.0", 2, selected.length);
		assertEquals("3.1", assoc1, selected[1]);
		assertEquals("3.2", text, selected[0]);

		// text user-added associations
		selected = finder.findContentTypesFor(changeCase("text.txt_useradded"));
		assertEquals("4.0", 2, selected.length);
		assertEquals("4.1", assoc1, selected[1]);
		assertEquals("4.2", text, selected[0]);

		// text provider-added associations
		selected = finder.findContentTypesFor(changeCase("text.txt_pluginadded"));
		assertEquals("5.0", 2, selected.length);
		assertEquals("5.1", assoc1, selected[1]);
		assertEquals("5.2", text, selected[0]);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc1pluginadded"));
		assertEquals("6.0", 1, selected.length);
		assertEquals("6.1", assoc1, selected[0]);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc1useradded"));
		assertEquals("7.0", 1, selected.length);
		assertEquals("7.1", assoc1, selected[0]);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2pluginadded"));
		assertEquals("8.0", 1, selected.length);
		assertEquals("8.1", assoc2, selected[0]);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2useradded"));
		assertEquals("9.0", 1, selected.length);
		assertEquals("9.1", assoc2, selected[0]);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2builtin"));
		assertEquals("10.0", 1, selected.length);
		assertEquals("10.1", assoc2, selected[0]);
	}

	@Test
	public void testAssociations() throws CoreException {
		IContentType text = Platform.getContentTypeManager().getContentType(Platform.PI_RUNTIME + ".text");

		// associate a user-defined file spec
		text.addFileSpec("txt_useradded", IContentType.FILE_EXTENSION_SPEC);

		// test associations
		assertTrue("0.1", text.isAssociatedWith(changeCase("text.txt")));
		assertTrue("0.2", text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue("0.3", text.isAssociatedWith(changeCase("text.txt_pluginadded")));

		// check provider defined settings
		String[] providerDefinedExtensions = text
				.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED);
		assertTrue("1.0", contains(providerDefinedExtensions, "txt"));
		assertFalse("1.1", contains(providerDefinedExtensions, "txt_useradded"));
		assertTrue("1.2", contains(providerDefinedExtensions, "txt_pluginadded"));

		// check user defined settings
		String[] textUserDefinedExtensions = text
				.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertFalse("2.0", contains(textUserDefinedExtensions, "txt"));
		assertTrue("2.1", contains(textUserDefinedExtensions, "txt_useradded"));
		assertFalse("2.2", contains(textUserDefinedExtensions, "txt_pluginadded"));

		// removing pre-defined file specs should not do anything
		text.removeFileSpec("txt", IContentType.FILE_EXTENSION_SPEC);
		assertTrue("3.0", contains(
				text.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED), "txt"));
		assertTrue("3.1", text.isAssociatedWith(changeCase("text.txt")));
		assertTrue("3.2", text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue("3.3", text.isAssociatedWith(changeCase("text.txt_pluginadded")));

		// removing user file specs is the normal case and has to work as expected
		text.removeFileSpec("txt_useradded", IContentType.FILE_EXTENSION_SPEC);
		assertFalse("4.0",
				contains(text.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_PRE_DEFINED), "ini"));
		assertTrue("4.1", text.isAssociatedWith(changeCase("text.txt")));
		assertFalse("4.2", text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue("4.3", text.isAssociatedWith(changeCase("text.txt_pluginadded")));
	}

	@Test
	public void testBinaryTypes() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType sampleBinary1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".sample-binary1");
		IContentType sampleBinary2 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".sample-binary2");
		InputStream contents;

		contents = getInputStream(
				new byte[][] { SAMPLE_BIN1_OFFSET.getBytes(), SAMPLE_BIN1_SIGNATURE, " extra contents".getBytes() });
		IContentDescription description = contentTypeManager.getDescriptionFor(contents, null, IContentDescription.ALL);
		assertNotNull("6.0", description);
		assertEquals("6.1", sampleBinary1, description.getContentType());

		contents = getInputStream(
				new byte[][] { SAMPLE_BIN2_OFFSET.getBytes(), SAMPLE_BIN2_SIGNATURE, " extra contents".getBytes() });
		description = contentTypeManager.getDescriptionFor(contents, null, IContentDescription.ALL);
		assertNotNull("7.0", description);
		assertEquals("7.1", sampleBinary2, description.getContentType());

		// make sure we ignore that content type when contents are text
		// (see bug 100032)
		// first check if our test environment is sane
		IContentType[] selected = contentTypeManager.findContentTypesFor("test.samplebin2");
		assertEquals("8.1", 1, selected.length);
		assertEquals("8.2", sampleBinary2.getId(), selected[0].getId());
		// (we used to blow up here)
		description = contentTypeManager.getDescriptionFor(getReader(getRandomString()), "test.samplebin2",
				IContentDescription.ALL);
		assertNull("8.3", description);
	}

	@Test
	public void testByteOrderMark() throws IOException {
		IContentType text = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		QualifiedName[] options = new QualifiedName[] { IContentDescription.BYTE_ORDER_MARK };
		IContentDescription description;
		// tests with UTF-8 BOM
		String UTF8_BOM = new String(IContentDescription.BOM_UTF_8, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNotNull("1.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals("1.1", IContentDescription.BOM_UTF_8,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// tests with UTF-16 Little Endian BOM
		String UTF16_LE_BOM = new String(IContentDescription.BOM_UTF_16LE, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNotNull("2.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals("2.1", IContentDescription.BOM_UTF_16LE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// tests with UTF-16 Big Endian BOM
		String UTF16_BE_BOM = new String(IContentDescription.BOM_UTF_16BE, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNotNull("3.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals("3.1", IContentDescription.BOM_UTF_16BE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// test with no BOM
		description = text.getDescriptionFor(
				new ByteArrayInputStream(MINIMAL_XML.getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull("4.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// tests for partial BOM
		// first byte of UTF-16 Big Endian + minimal xml
		String UTF16_BE_BOM_1byte = new String(new byte[] { (byte) 0xFE }, "ISO-8859-1");
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull("5.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-16 Big Endian only (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=199252)
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull("5.1", description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// first byte of UTF-16 Little Endian + minimal xml
		String UTF16_LE_BOM_1byte = new String(new byte[] { (byte) 0xFF }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull("6.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-16 Little Endian only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull("6.1", description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// first byte of UTF-8 + minimal xml
		String UTF8_BOM_1byte = new String(new byte[] { (byte) 0xEF }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull("7.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-8 only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull("7.1", description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// two first bytes of UTF-8 + minimal xml
		String UTF8_BOM_2bytes = new String(new byte[] { (byte) 0xEF, (byte) 0xBB }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_2bytes + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull("8.0", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// two first bytes of UTF-8 only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_2bytes).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull("8.1", description.getProperty(IContentDescription.BYTE_ORDER_MARK));
	}

	/**
	 * See bug 90218.
	 */
	@Test
	public void testContentAndNameMatching() throws IOException /* not really */ {
		IContentTypeManager manager = Platform.getContentTypeManager();

		byte[][] contents0 = { { 0x0A, 0x0B, 0x0E, 0x10 } };
		byte[][] contents1 = { { 0x0A, 0x0B, 0x0C, 0x10 } };
		byte[][] contents2 = { { 0x0A, 0x0B, 0x0D, 0x10 } };
		byte[][] invalidContents = { { 0, 0, 0, 0 } };

		// base matches *.mybinary files starting with 0x0a 0x0b
		IContentType base = manager.getContentType(PI_RESOURCES_TESTS + ".binary_base");
		// derived1 matches *.mybinary and specifically foo.mybinary files starting with
		// 0x0a 0x0b 0xc
		IContentType derived1 = manager.getContentType(PI_RESOURCES_TESTS + ".binary_derived1");
		// derived2 matches *.mybinary (inherits filespec from base) files starting with
		// 0x0a 0x0b 0xd
		IContentType derived2 = manager.getContentType(PI_RESOURCES_TESTS + ".binary_derived2");

		IContentType[] selected;

		selected = manager.findContentTypesFor(getInputStream(contents0), "anything.mybinary");
		assertEquals("1.0", 3, selected.length);
		// all we know is the first one is the base type (only one with a VALID match)
		assertEquals("1.1", base, selected[0]);

		selected = manager.findContentTypesFor(getInputStream(contents0), "foo.mybinary");
		// we know also that the second one will be derived1, because it has a full name
		// matching
		assertEquals("2.0", 3, selected.length);
		assertEquals("2.1", base, selected[0]);
		assertEquals("2.2", derived1, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(contents1), "foo.mybinary");
		// derived1 will be first because both base and derived1 have a strong content
		// matching, so more specific wins
		assertEquals("3.0", 3, selected.length);
		assertEquals("3.1", derived1, selected[0]);
		assertEquals("3.2", base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(contents2), "foo.mybinary");
		// same as 3.* - derived1 is last because content matching is weak, althoug name
		// matching is strong
		assertEquals("4.0", 3, selected.length);
		assertEquals("4.1", derived2, selected[0]);
		assertEquals("4.2", base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(invalidContents), "foo.mybinary");
		// all types have weak content matching only - derived1 has strong name matching
		assertEquals("5.0", 3, selected.length);
		assertEquals("5.1", derived1, selected[0]);
		assertEquals("5.2", base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(invalidContents), "anything.mybinary");
		// all types have weak content/name matching only - most general wins
		assertEquals("6.0", 3, selected.length);
		assertEquals("6.1", base, selected[0]);
	}

	/*
	 * Tests both text and byte stream-based getDescriptionFor methods.
	 */@Test
	public void testContentDescription() throws IOException, CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType xmlType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		IContentType mytext = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext");
		IContentType mytext1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext1");
		IContentType mytext2 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext2");

		boolean text = false;

		for (int i = 0; i < 2; i++, text = !text) {
			String sufix = text ? "-text" : "-binary";
			IContentDescription description;

			description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml",
					IContentDescription.ALL, text);
			assertNotNull("1.0" + sufix, description);
			assertEquals("1.1" + sufix, xmlType, description.getContentType());
			assertSame("1.2", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml",
					new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("2.0" + sufix, description);
			assertEquals("2.1" + sufix, xmlType, description.getContentType());
			// the default charset should have been filled by the content type manager
			assertEquals("2.2" + sufix, "UTF-8", description.getProperty(IContentDescription.CHARSET));
			assertSame("2.3", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, XML_ISO_8859_1, StandardCharsets.ISO_8859_1, "foo.xml",
					new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("2.3a" + sufix, description);
			assertEquals("2.3b" + sufix, xmlType, description.getContentType());
			assertEquals("2.3c" + sufix, "ISO-8859-1", description.getProperty(IContentDescription.CHARSET));
			assertNotSame("2.3d", xmlType.getDefaultDescription(), description);

			// ensure we handle single quotes properly (bug 65443)
			description = getDescriptionFor(finder, XML_ISO_8859_1_SINGLE_QUOTES, StandardCharsets.ISO_8859_1,
					"foo.xml", new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("2.3e" + sufix, description);
			assertEquals("2.3f" + sufix, xmlType, description.getContentType());
			assertEquals("2.3g" + sufix, "ISO-8859-1", description.getProperty(IContentDescription.CHARSET));
			assertNotSame("2.3h", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, XML_UTF_16, StandardCharsets.UTF_16, "foo.xml",
					new QualifiedName[] { IContentDescription.CHARSET, IContentDescription.BYTE_ORDER_MARK }, text);
			assertNotNull("2.4a" + sufix, description);
			assertEquals("2.4b" + sufix, xmlType, description.getContentType());
			assertEquals("2.4c" + sufix, "UTF-16", description.getProperty(IContentDescription.CHARSET));
			assertTrue("2.4d" + sufix, text || IContentDescription.BOM_UTF_16BE == description
					.getProperty(IContentDescription.BYTE_ORDER_MARK));
			assertNotSame("2.4e", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, XML_UTF_16BE, StandardCharsets.UTF_8, "foo.xml",
					new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("2.5a" + sufix, description);
			assertEquals("2.5b" + sufix, xmlType, description.getContentType());
			assertEquals("2.5c" + sufix, "UTF-16BE", description.getProperty(IContentDescription.CHARSET));
			assertNotSame("2.5d", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, XML_UTF_16LE, StandardCharsets.UTF_8, "foo.xml",
					new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("2.6a" + sufix, description);
			assertEquals("2.6b" + sufix, xmlType, description.getContentType());
			// the default charset should have been filled by the content type manager
			assertEquals("2.6c" + sufix, "UTF-16LE", description.getProperty(IContentDescription.CHARSET));
			assertNotSame("2.6d", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml",
					IContentDescription.ALL, text);
			assertNotNull("4.0" + sufix, description);
			assertEquals("4.1" + sufix, xmlType, description.getContentType());
			assertEquals("4.2" + sufix, "UTF-8", description.getProperty(IContentDescription.CHARSET));
			assertNotNull("5.0" + sufix, mytext);
			assertEquals("5.0b" + sufix, "BAR", mytext.getDefaultCharset());
			assertSame("5.0c", xmlType.getDefaultDescription(), description);

			description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
			assertNotNull("5.1" + sufix, description);
			assertEquals("5.2" + sufix, mytext, description.getContentType());
			assertEquals("5.3" + sufix, "BAR", description.getProperty(IContentDescription.CHARSET));
			assertSame("5.4", mytext.getDefaultDescription(), description);
			// now plays with setting a non-default default charset
			mytext.setDefaultCharset("FOO");

			description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
			assertNotNull("5.5" + sufix, description);
			assertEquals("5.6" + sufix, mytext, description.getContentType());
			assertEquals("5.7" + sufix, "FOO", description.getProperty(IContentDescription.CHARSET));
			assertSame("5.8", mytext.getDefaultDescription(), description);
			mytext.setDefaultCharset(null);

			description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
			assertNotNull("5.10" + sufix, description);
			assertEquals("5.11" + sufix, mytext, description.getContentType());
			assertEquals("5.12" + sufix, "BAR", description.getProperty(IContentDescription.CHARSET));
			assertSame("5.13", mytext.getDefaultDescription(), description);

			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=176354
			description = getDescriptionFor(finder,
					"<?xml version=\'1.0\' encoding=\'UTF-8\'?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://www.example.org/\" xmlns:ns0=\"http://another.example.org/\"><soapenv:Header /><soapenv:Body><ns0:x /></soapenv:Body></soapenv:Envelope>",
					StandardCharsets.UTF_8, "foo.xml", new QualifiedName[] { IContentDescription.CHARSET }, text);
			assertNotNull("5.14" + sufix, description);
			assertEquals("5.15" + sufix, xmlType, description.getContentType());
			assertEquals("5.16" + sufix, "UTF-8", description.getProperty(IContentDescription.CHARSET));
			assertEquals("5.17", xmlType.getDefaultDescription().getCharset(), description.getCharset());
		}
		assertNotNull("6.0", mytext1);
		assertEquals("6.1", "BAR", mytext1.getDefaultCharset());
		assertNotNull("6.2", mytext2);
		assertEquals("6.3", null, mytext2.getDefaultCharset());

	}

	@Test
	public void testContentDetection() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder;

		IContentType inappropriate = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".sample-binary1");
		IContentType appropriate = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		IContentType appropriateSpecific1 = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + ".xml-based-different-extension");
		IContentType appropriateSpecific1LowPriority = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + ".xml-based-different-extension-low-priority");
		IContentType appropriateSpecific2 = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + ".xml-based-specific-name");

		// if only inappropriate is provided, none will be selected
		finder = contentTypeManager.getMatcher(new SubsetSelectionPolicy(new IContentType[] { inappropriate }), null);
		assertNull("1.0", finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if inappropriate and appropriate are provided, appropriate will be selected
		finder = contentTypeManager
				.getMatcher(new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate }), null);
		assertEquals("2.0", appropriate, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if inappropriate, appropriate and a more specific appropriate type are
		// provided, the specific type will be selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate, appropriateSpecific1 }),
				null);
		assertEquals("3.0", appropriateSpecific1, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate, appropriateSpecific2 }),
				null);
		assertEquals("3.1", appropriateSpecific2, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if all are provided, the more specific types will appear before the more
		// generic types
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(
						new IContentType[] { inappropriate, appropriate, appropriateSpecific1, appropriateSpecific2 }),
				null);
		IContentType[] selected = finder.findContentTypesFor(getInputStream(MINIMAL_XML), null);
		assertEquals("4.0", 3, selected.length);
		assertTrue("4.1", appropriateSpecific1.equals(selected[0]) || appropriateSpecific1.equals(selected[1]));
		assertTrue("4.2", appropriateSpecific2.equals(selected[0]) || appropriateSpecific2.equals(selected[1]));
		assertTrue("4.3", appropriate.equals(selected[2]));

		// if appropriate and a more specific appropriate type (but with low priority)
		// are provided, the specific type will be selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { appropriate, appropriateSpecific1LowPriority }), null);
		assertEquals("5.0", appropriateSpecific1LowPriority,
				finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if appropriate and two specific appropriate types (but one with lower
		// priority) are provided, the specific type with higher priority will be
		// selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(
						new IContentType[] { appropriate, appropriateSpecific1, appropriateSpecific1LowPriority }),
				null);
		assertEquals("5.1", appropriateSpecific1, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));
	}

	@Test
	public void testDefaultProperties() throws IOException /* never actually thrown */ {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();

		IContentType mytext = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext");
		IContentType mytext1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext1");
		IContentType mytext2 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext2");
		assertNotNull("0.1", mytext);
		assertNotNull("0.2", mytext1);
		assertNotNull("0.3", mytext2);

		QualifiedName charset = IContentDescription.CHARSET;
		QualifiedName localCharset = new QualifiedName(PI_RESOURCES_TESTS, "charset");
		QualifiedName property1 = new QualifiedName(PI_RESOURCES_TESTS, "property1");
		QualifiedName property2 = new QualifiedName(PI_RESOURCES_TESTS, "property2");
		QualifiedName property3 = new QualifiedName(PI_RESOURCES_TESTS, "property3");
		QualifiedName property4 = new QualifiedName(PI_RESOURCES_TESTS, "property4");

		IContentDescription description;
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, true);
		assertNotNull("1.0", description);
		assertEquals("1.1", mytext, description.getContentType());
		assertEquals("1.2", "value1", description.getProperty(property1));
		assertNull("1.3", description.getProperty(property2));
		assertEquals("1.4", "value3", description.getProperty(property3));
		assertEquals("1.5", "BAR", description.getProperty(charset));

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt1", IContentDescription.ALL, true);
		assertNotNull("2.0", description);
		assertEquals("2.1", mytext1, description.getContentType());
		assertEquals("2.2", "value1", description.getProperty(property1));
		assertEquals("2.3", "value2", description.getProperty(property2));
		assertNull("2.4", description.getProperty(property3));
		assertEquals("2.5", "value4", description.getProperty(property4));
		assertEquals("2.6", "BAR", description.getProperty(charset));

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt2", IContentDescription.ALL, true);
		assertNotNull("3.0", description);
		assertEquals("3.1", mytext2, description.getContentType());
		assertNull("3.2", description.getProperty(property1));
		assertNull("3.3", description.getProperty(property2));
		assertNull("3.4", description.getProperty(property3));
		assertNull("3.5", description.getProperty(property4));
		assertNull("3.6", description.getProperty(charset));
		assertEquals("3.7", "mytext2", description.getProperty(localCharset));
	}

	/**
	 * The fooBar content type is associated with the "foo.bar" file name and the
	 * "bar" file extension (what is bogus, anyway). This test ensures it does not
	 * appear twice in the list of content types associated with the "foo.bar" file
	 * name.
	 */
	@Test
	public void testDoubleAssociation() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();

		IContentType fooBarType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "fooBar");
		assertNotNull("1.0", fooBarType);
		IContentType subFooBarType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "subFooBar");
		assertNotNull("1.1", subFooBarType);
		// ensure we don't get fooBar twice
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);
		IContentType[] fooBarAssociated = finder.findContentTypesFor(changeCase("foo.bar"));
		assertEquals("2.1", 2, fooBarAssociated.length);
		assertTrue("2.2", contains(fooBarAssociated, fooBarType));
		assertTrue("2.3", contains(fooBarAssociated, subFooBarType));
	}

	/**
	 * Obtains a reference to a known content type, then installs a bundle that
	 * contributes a content type, and makes sure a new obtained reference to the
	 * same content type is not identical (shows that the content type catalog has
	 * been discarded and rebuilt). Then uninstalls that bundle and checks again the
	 * same thing (because the content type catalog should be rebuilt whenever
	 * content types are dynamicaly added/removed).
	 */
	@Test
	public void testDynamicChanges() {
		final IContentType[] text = new IContentType[4];
		final IContentTypeManager manager = Platform.getContentTypeManager();
		text[0] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.0", text[0]);
		text[1] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.1", text[1]);
		text[0] = ((ContentTypeHandler) text[0]).getTarget();
		text[1] = ((ContentTypeHandler) text[1]).getTarget();
		assertEquals("2.0", text[0], text[1]);
		assertEquals("2.1", text[0], text[1]);
		// make arbitrary dynamic changes to the contentTypes extension point
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles("3", () -> {
			IContentType missing = manager.getContentType("org.eclipse.bundle01.missing");
			assertNotNull("3.1", missing);
			// ensure the content type instances are different
			text[2] = manager.getContentType(IContentTypeManager.CT_TEXT);
			assertNotNull("3.2", text[2]);
			text[2] = ((ContentTypeHandler) text[2]).getTarget();
			assertEquals("3.3", text[0], text[2]);
			assertNotSame("3.4", text[0], text[2]);
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bundle01" }, listener);
		assertNull("4.0", manager.getContentType("org.eclipse.bundle01.missing"));
		// ensure the content type instances are all different
		text[3] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("5.0", text[3]);
		text[3] = ((ContentTypeHandler) text[3]).getTarget();
		assertEquals("5.1", text[0], text[3]);
		assertEquals("5.2", text[2], text[3]);
		assertNotSame("5.3", text[0], text[3]);
		assertNotSame("5.4", text[2], text[3]);
	}

	/**
	 * Similar to testDynamicChanges, but using the
	 * org.eclipse.core.contenttype.contentTypes extension point.
	 */
	@Test
	public void testDynamicChangesNewExtension() {
		final IContentType[] text = new IContentType[4];
		final IContentTypeManager manager = Platform.getContentTypeManager();
		text[0] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.0", text[0]);
		text[1] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("1.1", text[1]);
		text[0] = ((ContentTypeHandler) text[0]).getTarget();
		text[1] = ((ContentTypeHandler) text[1]).getTarget();
		assertEquals("2.0", text[0], text[1]);
		assertSame("2.1", text[0], text[1]);
		// make arbitrary dynamic changes to the contentTypes extension point
		TestRegistryChangeListener listener = new TestRegistryChangeListener(IContentConstants.CONTENT_NAME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles("3", () -> {
			IContentType contentType = manager.getContentType("org.eclipse.bug485227.bug485227_contentType");
			assertNotNull("3.1 Contributed content type not found", contentType);
			// ensure the content type instances are different
			text[2] = manager.getContentType(IContentTypeManager.CT_TEXT);
			assertNotNull("3.2 Text content type not modified", text[2]);
			text[2] = ((ContentTypeHandler) text[2]).getTarget();
			assertEquals("3.3", text[0], text[2]);
			assertNotSame("3.4", text[0], text[2]);
			assertEquals("3.5 default extension not associated", contentType,
					manager.findContentTypeFor("file.bug485227"));
			assertEquals("3.6 additional extension not associated", contentType,
					manager.findContentTypeFor("file.bug485227_2"));
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bug485227" }, listener);
		assertNull("4.0 Content type not cleared after bundle uninstall",
				manager.getContentType("org.eclipse.bug485227.bug485227_contentType"));
		// ensure the content type instances are all different
		text[3] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull("5.0", text[3]);
		text[3] = ((ContentTypeHandler) text[3]).getTarget();
		assertEquals("5.1", text[0], text[3]);
		assertEquals("5.2", text[2], text[3]);
		assertNotSame("5.3", text[0], text[3]);
		assertNotSame("5.4", text[2], text[3]);
	}

	@Test
	public void testEvents() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType myType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".myContent");
		assertNotNull("0.9", myType);

		ContentTypeChangeTracer tracer;

		tracer = new ContentTypeChangeTracer();
		contentTypeManager.addContentTypeChangeListener(tracer);

		// add a file spec and check event
		myType.addFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertTrue("1.1", tracer.isOnlyChange(myType));

		// remove a non-existing file spec - should not cause an event to be fired
		tracer.reset();
		myType.removeFileSpec("another.file.name", IContentType.FILE_EXTENSION_SPEC);
		assertTrue("2.1", !tracer.isOnlyChange(myType));

		// add a file spec again and check no event is generated
		tracer.reset();
		myType.addFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertTrue("3.1", !tracer.isOnlyChange(myType));

		// remove a file spec and check event
		tracer.reset();
		myType.removeFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertTrue("4.1", tracer.isOnlyChange(myType));

		// change the default charset and check event
		tracer.reset();
		myType.setDefaultCharset("FOO");
		assertTrue("5.1", tracer.isOnlyChange(myType));

		// set the default charset to the same - no event should be generated
		tracer.reset();
		myType.setDefaultCharset("FOO");
		assertTrue("6.1", !tracer.isOnlyChange(myType));

		myType.setDefaultCharset("ABC");
	}

	@Test
	public void testFileSpecConflicts() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		// when not submitting contents, for related types, most general type prevails
		IContentType conflict1a = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict1");
		IContentType conflict1b = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict1");
		assertNotNull("1.0", conflict1a);
		assertNotNull("1.1", conflict1b);
		IContentType preferredConflict1 = manager.findContentTypeFor("test.conflict1");
		assertNotNull("1.2", preferredConflict1);
		assertEquals("1.3", conflict1a, preferredConflict1);

		IContentType conflict2base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict2");
		IContentType conflict2sub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict2");
		assertNotNull("2.0", conflict2base);
		assertNotNull("2.1", conflict2sub);
		// when submitting contents, for related types, descendant comes first

		IContentType[] selectedConflict2 = manager.findContentTypesFor(getRandomContents(), "test.conflict2");
		assertEquals("2.2", 2, selectedConflict2.length);
		assertEquals("2.3", selectedConflict2[0], conflict2base);
		assertEquals("2.4", selectedConflict2[1], conflict2sub);

		IContentType conflict3base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict3");
		IContentType conflict3sub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict3");
		IContentType conflict3unrelated = manager.getContentType(PI_RESOURCES_TESTS + ".unrelated_conflict3");
		assertNotNull("3.0.1", conflict3base);
		assertNotNull("3.0.2", conflict3sub);
		assertNotNull("3.0.3", conflict3unrelated);

		// Two unrelated types (sub_conflict3 and unrelated conflict3) are in conflict.
		// Order will be based on depth (more general first since they don't have
		// describers)

		IContentType[] selectedConflict3 = manager.findContentTypesFor(getRandomContents(), "test.conflict3");
		assertEquals("4.0", 2, selectedConflict3.length);
		assertEquals("4.1", selectedConflict3[0], conflict3unrelated);
		assertEquals("4.2", selectedConflict3[1], conflict3sub);
	}

	@Test
	public void testFindContentType() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		IContentType xmlContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");

		IContentType single;

		single = finder.findContentTypeFor(getInputStream("Just a test"), changeCase("file.txt"));
		assertNotNull("1.0", single);
		assertEquals("1.1", textContentType, single);

		single = finder.findContentTypeFor(getInputStream(XML_UTF_8, StandardCharsets.UTF_8), changeCase("foo.xml"));
		assertNotNull("2.0", single);
		assertEquals("2.1", xmlContentType, single);

		IContentType[] multiple = finder.findContentTypesFor(getInputStream(XML_UTF_8, StandardCharsets.UTF_8), null);
		assertTrue("3.0", contains(multiple, xmlContentType));
	}

	@Test
	public void testFindContentTypPredefinedRegexp() throws IOException, CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType targetContentType = contentTypeManager
				.getContentType("org.eclipse.core.tests.resources.predefinedContentTypeWithRegexp");
		assertNotNull("Target content-type not found", targetContentType);

		IContentType single = finder.findContentTypeFor(getInputStream("Just a test"),
				"somepredefinedContentTypeWithRegexpFile");
		assertEquals(targetContentType, single);
		single = finder.findContentTypeFor(getInputStream("Just a test"), "somepredefinedContentTypeWithPatternFile");
		assertEquals(targetContentType, single);
		single = finder.findContentTypeFor(getInputStream("Just a test"), "somepredefinedContentTypeWithWildcardsFile");
		assertEquals(targetContentType, single);
	}

	@Test
	public void testFindContentTypeUserRegexp() throws IOException, CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");

		IContentType single = finder.findContentTypeFor(getInputStream("Just a test"), "someText.unknown");
		assertNull("File pattern unknown at that point", single);

		textContentType.addFileSpec("*Text*", IContentType.FILE_PATTERN_SPEC);
		single = finder.findContentTypeFor(getInputStream("Just a test"), "someText.unknown");
		assertEquals("Text content should now match *Text* files", textContentType, single);
	}

	@Test
	public void testImportFileAssociation() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		assertNull(contentTypeManager.findContentTypeFor("*.bug122217"));
		IPreferencesService service = Platform.getPreferencesService();
		String prefs = "file_export_version=3.0\n/instance/org.eclipse.core.runtime/content-types/org.eclipse.core.runtime.xml/file-extensions=bug122217";
		IExportedPreferences exported = service.readPreferences(new ByteArrayInputStream(prefs.getBytes()));
		assertTrue(service.applyPreferences(exported).isOK());
		assertNotNull(contentTypeManager.findContentTypeFor("*.bug122217"));
	}

	@Test
	public void testInvalidMarkup() {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);
		assertEquals("1.0", 0, finder.findContentTypesFor("invalid.missing.identifier").length);
		assertEquals("2.0", 0, finder.findContentTypesFor("invalid.missing.name").length);
		assertNull("3.0", contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "invalid-missing-name"));
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles("1", () -> {
			// ensure the invalid content types are not available
			assertEquals("1.2", 0, contentTypeManager.findContentTypesFor("invalid.missing.identifier").length);
			assertEquals("1.3", 0, contentTypeManager.findContentTypesFor("invalid.missing.name").length);
			assertNull("1.4", contentTypeManager.getContentType("org.eclipse.bundle03.invalid-missing-name"));
			// this content type has good markup, but invalid describer class
			IContentType invalidDescriber = contentTypeManager.getContentType("org.eclipse.bundle03.invalid-describer");
			assertNotNull("1.5", invalidDescriber);
			// name based matching should work fine
			assertEquals("1.6", invalidDescriber, contentTypeManager.findContentTypeFor("invalid.describer"));
			// the describer class is invalid, content matchong should fail
			IContentType nullContentType;
			try {
				nullContentType = contentTypeManager.findContentTypeFor(getRandomContents(), "invalid.describer");
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			assertNull("1.7", nullContentType);
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bundle03" }, listener);
	}

	/**
	 * Bugs 67841 and 62443
	 */
	@Test
	public void testIOException() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType xml = manager.getContentType(Platform.PI_RUNTIME + ".xml");
		IContentType rootElement = manager.getContentType(PI_RESOURCES_TESTS + ".root-element");
		// a SAXException is usually caught (and silently ignored) in
		// XMLRootElementDescriber in these cases
		IContentType[] selected = manager
				.findContentTypesFor(getInputStream(XML_US_ASCII_INVALID, StandardCharsets.ISO_8859_1), "test.xml");
		assertTrue("1.1", contains(selected, xml));
		assertTrue("1.2", contains(selected, rootElement));

		// induce regular IOExceptions... these should be thrown to clients
		class FakeIOException extends IOException {
			/**
			 * All serializable objects should have a stable serialVersionUID
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getMessage() {
				return "This exception was thrown for testing purposes";
			}
		}
		assertThrows(FakeIOException.class, () -> manager.findContentTypesFor(new InputStream() {

			@Override
			public int available() {
				// trick the client into reading the file
				return Integer.MAX_VALUE;
			}

			@Override
			public int read() throws IOException {
				throw new FakeIOException();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				throw new FakeIOException();
			}
		}, "test.xml"));
	}

	@Test
	public void testIsKindOf() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		IContentType xmlContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		IContentType xmlBasedDifferentExtensionContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-different-extension");
		IContentType xmlBasedSpecificNameContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-specific-name");
		IContentType binaryContentType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "sample-binary1");
		assertTrue("1.0", textContentType.isKindOf(textContentType));
		assertTrue("2.0", xmlContentType.isKindOf(textContentType));
		assertFalse("2.1", textContentType.isKindOf(xmlContentType));
		assertTrue("2.2", xmlContentType.isKindOf(xmlContentType));
		assertTrue("3.0", xmlBasedDifferentExtensionContentType.isKindOf(textContentType));
		assertTrue("3.1", xmlBasedDifferentExtensionContentType.isKindOf(xmlContentType));
		assertFalse("4.0", xmlBasedDifferentExtensionContentType.isKindOf(xmlBasedSpecificNameContentType));
		assertFalse("5.0", binaryContentType.isKindOf(textContentType));
	}

	@Test
	public void testListParsing() {
		String[] list;
		list = Util.parseItems(null);
		assertEquals("0.0", 0, list.length);
		list = Util.parseItems("");
		assertEquals("1.0", 1, list.length);
		assertEquals("1.1", "", list[0]);
		list = Util.parseItems("foo");
		assertEquals("2.0", 1, list.length);
		assertEquals("2.1", "foo", list[0]);
		list = Util.parseItems(",");
		assertEquals("3.0", 2, list.length);
		assertEquals("3.1", "", list[0]);
		assertEquals("3.2", "", list[1]);
		list = Util.parseItems(",foo,bar");
		assertEquals("4.0", 3, list.length);
		assertEquals("4.1", "", list[0]);
		assertEquals("4.2", "foo", list[1]);
		assertEquals("4.3", "bar", list[2]);
		list = Util.parseItems("foo,bar,");
		assertEquals("5.0", 3, list.length);
		assertEquals("5.1", "foo", list[0]);
		assertEquals("5.2", "bar", list[1]);
		assertEquals("5.3", "", list[2]);
		list = Util.parseItems("foo,,bar");
		assertEquals("6.0", 3, list.length);
		assertEquals("6.1", "foo", list[0]);
		assertEquals("6.2", "", list[1]);
		assertEquals("6.3", "bar", list[2]);
		list = Util.parseItems("foo,,,bar");
		assertEquals("7.0", 4, list.length);
		assertEquals("7.1", "foo", list[0]);
		assertEquals("7.2", "", list[1]);
		assertEquals("7.3", "", list[2]);
		assertEquals("7.4", "bar", list[3]);
		list = Util.parseItems(",,foo,bar");
		assertEquals("8.0", 4, list.length);
		assertEquals("8.1", "", list[0]);
		assertEquals("8.2", "", list[1]);
		assertEquals("8.3", "foo", list[2]);
		assertEquals("8.4", "bar", list[3]);
		list = Util.parseItems("foo,bar,,");
		assertEquals("9.0", 4, list.length);
		assertEquals("9.1", "foo", list[0]);
		assertEquals("9.2", "bar", list[1]);
		assertEquals("9.3", "", list[2]);
		assertEquals("9.4", "", list[3]);
		list = Util.parseItems(",,,");
		assertEquals("10.0", 4, list.length);
		assertEquals("10.1", "", list[0]);
		assertEquals("10.2", "", list[1]);
		assertEquals("10.3", "", list[2]);
		assertEquals("10.4", "", list[3]);
	}

	@Test
	public void testMyContentDescriber() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType myContent = manager.getContentType(PI_RESOURCES_TESTS + '.' + "myContent");
		assertNotNull("0.5", myContent);
		assertEquals("0.6", myContent, manager.findContentTypeFor("myContent.mc1"));
		assertEquals("0.7", myContent, manager.findContentTypeFor("myContent.mc2"));
		assertEquals("0.8", myContent, manager.findContentTypeFor("foo.myContent1"));
		assertEquals("0.9", myContent, manager.findContentTypeFor("bar.myContent2"));
		IContentDescription description = manager.getDescriptionFor(
				getInputStream(MyContentDescriber.SIGNATURE, StandardCharsets.US_ASCII), "myContent.mc1",
				IContentDescription.ALL);
		assertNotNull("1.0", description);
		assertEquals("1.1", myContent, description.getContentType());
		assertNotSame("1.2", myContent.getDefaultDescription(), description);
		for (int i = 0; i < MyContentDescriber.MY_OPTIONS.length; i++) {
			assertEquals("2." + i, MyContentDescriber.MY_OPTION_VALUES[i],
					description.getProperty(MyContentDescriber.MY_OPTIONS[i]));
		}
	}

	@Test
	public void testNoExtensionAssociation() {
		// TODO use a IContentTypeMatcher instead
		final IContentTypeManager manager = Platform.getContentTypeManager();

		IContentType[] selected = manager.findContentTypesFor("file_with_no_extension");
		assertEquals("0.1", 0, selected.length);

		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles("1", () -> {
			final String namespace = "org.eclipse.bundle04";

			IContentType empty1 = manager.getContentType(namespace + ".empty_extension1");
			IContentType empty2 = manager.getContentType(namespace + ".empty_extension2");
			IContentType empty3 = manager.getContentType(namespace + ".empty_extension3");
			IContentType empty4 = manager.getContentType(namespace + ".empty_extension4");
			IContentType nonEmpty = manager.getContentType(namespace + ".non_empty_extension");

			assertNotNull("1.1.1", empty1);
			assertNotNull("1.1.2", empty2);
			assertNotNull("1.1.3", empty3);
			assertNotNull("1.1.4", empty4);
			assertNotNull("1.1.5", nonEmpty);

			IContentType[] selected1 = manager.findContentTypesFor("file_with_no_extension");
			assertEquals("1.2.0", 4, selected1.length);
			assertTrue("1.2.1", contains(selected1, empty1));
			assertTrue("1.2.2", contains(selected1, empty2));
			assertTrue("1.2.3", contains(selected1, empty3));
			assertTrue("1.2.4", contains(selected1, empty4));

			selected1 = manager.findContentTypesFor("file_with_extension.non-empty");
			assertEquals("1.2.5", 1, selected1.length);
			assertTrue("1.2.6", contains(selected1, nonEmpty));

			try {
				nonEmpty.addFileSpec("", IContentType.FILE_EXTENSION_SPEC);
			} catch (CoreException e) {
				throw new AssertionError(e);
			}
			try {
				selected1 = manager.findContentTypesFor("file_with_no_extension");
				assertEquals("1.3.1", 5, selected1.length);
				assertTrue("1.3.2", contains(selected1, nonEmpty));
			} finally {
				try {
					nonEmpty.removeFileSpec("", IContentType.FILE_EXTENSION_SPEC);
				} catch (CoreException e) {
					throw new AssertionError(e);
				}
			}
			selected1 = manager.findContentTypesFor("file_with_no_extension");
			assertEquals("1.4.0", 4, selected1.length);
			assertFalse("1.4.1", contains(selected1, nonEmpty));
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bundle04" }, listener);
	}

	@Test
	public void testOrderWithEmptyFiles() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = manager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType xml = manager.getContentType(Platform.PI_RUNTIME + ".xml");
		manager.getContentType(PI_RESOURCES_TESTS + ".root-element");
		manager.getContentType(PI_RESOURCES_TESTS + ".dtd");
		// for an empty file, the most generic content type should be returned
		IContentType selected = finder.findContentTypeFor(getInputStream(""), "foo.xml");
		assertEquals("1.0", xml, selected);
		// it should be equivalent to omitting the contents
		assertEquals("1.1", xml, finder.findContentTypeFor("foo.xml"));
	}

	/**
	 * This test shows how we deal with orphan file associations (associations whose
	 * content types are missing).
	 */
	@Test
	public void testOrphanContentType() {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType orphan = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".orphan");
		assertNull("0.8", orphan);
		IContentType missing = contentTypeManager.getContentType("org.eclipse.bundle01.missing");
		assertNull("0.9", missing);
		assertEquals("1.1", 0, contentTypeManager.findContentTypesFor("foo.orphan").length);
		assertEquals("1.2", 0, contentTypeManager.findContentTypesFor("orphan.orphan").length);
		assertEquals("1.3", 0, contentTypeManager.findContentTypesFor("foo.orphan2").length);

		// test late addition of content type - orphan2 should become visible
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);

		BundleTestingHelper.runWithBundles("2", () -> {
			IContentType orphan1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".orphan");
			assertNotNull("2.1", orphan1);
			IContentType missing1 = contentTypeManager.getContentType("org.eclipse.bundle01.missing");
			assertNotNull("2.2", missing1);
			// checks orphan's associations
			assertEquals("2.3", 1, contentTypeManager.findContentTypesFor("foo.orphan").length);
			assertEquals("2.4", orphan1, contentTypeManager.findContentTypesFor("foo.orphan")[0]);
			assertEquals("2.5", 1, contentTypeManager.findContentTypesFor("orphan.orphan").length);
			assertEquals("2.6", orphan1, contentTypeManager.findContentTypesFor("foo.orphan")[0]);
			// check whether an orphan association was added to the dynamically added bundle
			assertEquals("2.7", 1, contentTypeManager.findContentTypesFor("foo.orphan2").length);
			assertEquals("2.8", missing1, contentTypeManager.findContentTypesFor("foo.orphan2")[0]);
		}, getContext(), new String[] { ContentTypeTest.TEST_FILES_ROOT + "content/bundle01" }, listener);
	}

	/**
	 * Regression test for bug 68894
	 */
	@Test
	public void testPreferences() throws CoreException, BackingStoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType text = manager.getContentType(IContentTypeManager.CT_TEXT);
		Preferences textPrefs = InstanceScope.INSTANCE.getNode(ContentTypeManager.CONTENT_TYPE_PREF_NODE)
				.node(text.getId());
		assertNotNull("0.1", text);

		// ensure the "default charset" preference is being properly used
		assertNull("1.0", text.getDefaultCharset());
		assertNull("1.1", textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));
		text.setDefaultCharset("UTF-8");
		assertEquals("1.2", "UTF-8", textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));
		text.setDefaultCharset(null);
		assertNull("1.3", textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));

		// ensure the file spec preferences are being properly used
		// some sanity checking
		assertFalse("2.01", text.isAssociatedWith("xyz.foo"));
		assertFalse("2.01", text.isAssociatedWith("xyz.bar"));
		assertFalse("2.03", text.isAssociatedWith("foo.ext"));
		assertFalse("2.04", text.isAssociatedWith("bar.ext"));
		// Null entries first to avoid interference from other tests
		textPrefs.remove(ContentType.PREF_FILE_NAMES);
		textPrefs.remove(ContentType.PREF_FILE_EXTENSIONS);
		// play with file name associations first...
		assertNull("2.0a", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		assertNull("2.0b", textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		text.addFileSpec("foo.ext", IContentType.FILE_NAME_SPEC);
		assertTrue("2.1", text.isAssociatedWith("foo.ext"));
		assertEquals("2.2", "foo.ext", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		text.addFileSpec("bar.ext", IContentType.FILE_NAME_SPEC);
		assertTrue("2.3", text.isAssociatedWith("bar.ext"));
		assertEquals("2.4", "foo.ext,bar.ext", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		// ... and then with file extensions
		text.addFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		assertTrue("2.5", text.isAssociatedWith("xyz.foo"));
		assertEquals("2.6", "foo", textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		text.addFileSpec("bar", IContentType.FILE_EXTENSION_SPEC);
		assertTrue("2.7", text.isAssociatedWith("xyz.bar"));
		assertEquals("2.4", "foo,bar", textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		// remove all associations made
		text.removeFileSpec("foo.ext", IContentType.FILE_NAME_SPEC);
		text.removeFileSpec("bar.ext", IContentType.FILE_NAME_SPEC);
		text.removeFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		text.removeFileSpec("bar", IContentType.FILE_EXTENSION_SPEC);
		// ensure all is as before
		assertFalse("3.1", text.isAssociatedWith("xyz.foo"));
		assertFalse("3.2", text.isAssociatedWith("xyz.bar"));
		assertFalse("3.3", text.isAssociatedWith("foo.ext"));
		assertFalse("3.4", text.isAssociatedWith("bar.ext"));

		// ensure the serialization format is correct
		try {
			text.addFileSpec("foo.bar", IContentType.FILE_NAME_SPEC);
			textPrefs.sync();
			assertEquals("4.0", "foo.bar", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		} finally {
			// clean-up
			text.removeFileSpec("foo.bar", IContentType.FILE_NAME_SPEC);
		}
	}

	@Test
	public void testRegistry() {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		assertNotNull("1.0", textContentType);
		assertTrue("1.1", isText(contentTypeManager, textContentType));
		assertNotNull("1.2", ((ContentTypeHandler) textContentType).getTarget().getDescriber());

		IContentType xmlContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		assertNotNull("2.0", xmlContentType);
		assertTrue("2.1", isText(contentTypeManager, xmlContentType));
		assertEquals("2.2", textContentType, xmlContentType.getBaseType());
		IContentDescriber xmlDescriber = ((ContentTypeHandler) xmlContentType).getTarget().getDescriber();
		assertNotNull("2.3", xmlDescriber);
		assertTrue("2.4", xmlDescriber instanceof XMLContentDescriber);

		IContentType xmlBasedDifferentExtensionContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-different-extension");
		assertNotNull("3.0", xmlBasedDifferentExtensionContentType);
		assertTrue("3.1", isText(contentTypeManager, xmlBasedDifferentExtensionContentType));
		assertEquals("3.2", xmlContentType, xmlBasedDifferentExtensionContentType.getBaseType());

		IContentType xmlBasedSpecificNameContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-specific-name");
		assertNotNull("4.0", xmlBasedSpecificNameContentType);
		assertTrue("4.1", isText(contentTypeManager, xmlBasedSpecificNameContentType));
		assertEquals("4.2", xmlContentType, xmlBasedSpecificNameContentType.getBaseType());

		IContentType[] xmlTypes = finder.findContentTypesFor(changeCase("foo.xml"));
		assertTrue("5.1", contains(xmlTypes, xmlContentType));

		IContentType binaryContentType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "sample-binary1");
		assertNotNull("6.0", binaryContentType);
		assertFalse("6.1", isText(contentTypeManager, binaryContentType));
		assertNull("6.2", binaryContentType.getBaseType());

		IContentType[] binaryTypes = finder.findContentTypesFor(changeCase("foo.samplebin1"));
		assertEquals("7.0", 1, binaryTypes.length);
		assertEquals("7.1", binaryContentType, binaryTypes[0]);

		IContentType myText = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".mytext");
		assertNotNull("8.0", myText);
		assertEquals("8.1", "BAR", myText.getDefaultCharset());

		IContentType[] fooBarTypes = finder.findContentTypesFor(changeCase("foo.bar"));
		assertEquals("9.0", 2, fooBarTypes.length);

		IContentType fooBar = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".fooBar");
		assertNotNull("9.1", fooBar);
		IContentType subFooBar = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".subFooBar");
		assertNotNull("9.2", subFooBar);
		assertTrue("9.3", contains(fooBarTypes, fooBar));
		assertTrue("9.4", contains(fooBarTypes, subFooBar));
	}

	@Test
	public void testRootElementAndDTDDescriber() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType rootElement = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".root-element");
		IContentType dtdElement = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".dtd");
		IContentType nsRootElement = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".ns-root-element");
		IContentType nsWildcard = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".ns-wildcard");
		IContentType emptyNsRootElement = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + ".empty-ns-root-element");
		IContentType xmlType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");

		IContentType[] contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_ISO_8859_1, StandardCharsets.ISO_8859_1), "fake.xml");
		assertTrue("1.0", contentTypes.length > 0);
		assertEquals("1.1", rootElement, contentTypes[0]);

		// bugs 64053 and 63298
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_EXTERNAL_ENTITY, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("2.0", contentTypes.length > 0);
		assertEquals("2.1", rootElement, contentTypes[0]);

		// bug 63625
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_EXTERNAL_ENTITY2, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("3.0", contentTypes.length > 0);
		assertEquals("3.1", rootElement, contentTypes[0]);

		// bug 135575
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MATCH1, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.0", contentTypes.length > 0);
		assertEquals("4.1", nsRootElement, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.2", contentTypes.length > 0);
		assertEquals("4.3", nsRootElement, contentTypes[0]);
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_WRONG_ELEM, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.4", contentTypes.length > 0);
		assertEquals("4.5", xmlType, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WRONG_NS, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.6", contentTypes.length > 0);
		assertEquals("4.7", xmlType, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MIXUP, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.8", contentTypes.length > 0);
		assertEquals("4.9", xmlType, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WILDCARD, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.10", contentTypes.length > 0);
		assertEquals("4.11", nsWildcard, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WILDCARD2, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.12", contentTypes.length > 0);
		assertEquals("4.13", nsWildcard, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_EMPTY_NS, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("4.14", contentTypes.length > 0);
		assertEquals("4.15", emptyNsRootElement, contentTypes[0]);

		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_DTD_US_ASCII, StandardCharsets.US_ASCII), "fake.xml");
		assertTrue("5.0", contentTypes.length > 0);
		assertEquals("5.1", dtdElement, contentTypes[0]);
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_DTD_EXTERNAL_ENTITY, StandardCharsets.UTF_8), "fake.xml");
		assertTrue("5.4", contentTypes.length > 0);
		assertEquals("5.5", dtdElement, contentTypes[0]);

		// bug 67975
		IContentDescription description = contentTypeManager.getDescriptionFor(getInputStream(
				new byte[][] { IContentDescription.BOM_UTF_16BE, XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-16BE") }),
				"fake.xml", IContentDescription.ALL);
		assertNotNull("6.0", description);
		assertEquals("6.1", rootElement, description.getContentType());
		assertEquals("6.2", IContentDescription.BOM_UTF_16BE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		description = contentTypeManager.getDescriptionFor(getInputStream(
				new byte[][] { IContentDescription.BOM_UTF_16LE, XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-16LE") }),
				"fake.xml", IContentDescription.ALL);
		assertNotNull("7.0", description);
		assertEquals("7.1", rootElement, description.getContentType());
		assertEquals("7.2", IContentDescription.BOM_UTF_16LE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// due to bug 67048, the test below fails with Crimson parser (does not handle
		// UTF-8 BOMs)
		// description = contentTypeManager.getDescriptionFor(getInputStream(new
		// byte[][]
		// {IContentDescription.BOM_UTF_8,XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-8")}),
		// "fake.xml", IContentDescription.ALL);
		// assertTrue("7.0", description != null);
		// assertEquals("7.1", rootElement, description.getContentType());
		// assertEquals("7.2", IContentDescription.BOM_UTF_8,
		// description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// bug 84354
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NO_DECL, StandardCharsets.UTF_8), "test.txt");
		assertTrue("8.0", contentTypes.length > 0);
		assertEquals("8.1", contentTypeManager.getContentType(IContentTypeManager.CT_TEXT), contentTypes[0]);
	}

	/**
	 * Bug 66976
	 */
	@Test
	public void testSignatureBeyondBufferLimit() throws IOException {
		int bufferLimit = ContentTypeManager.BLOCK_SIZE * 4;
		// create a long XML comment as prefix
		StringBuilder comment = new StringBuilder("<!--");
		for (int i = 0; i < bufferLimit; i++) {
			comment.append('*');
		}
		comment.append("-->");
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType rootElement = manager.getContentType(PI_RESOURCES_TESTS + ".root-element");
		IContentType selected = manager.findContentTypeFor(
				getInputStream(comment + XML_ROOT_ELEMENT_NO_DECL, StandardCharsets.US_ASCII), "fake.xml");
		assertNotNull("1.0", selected);
		assertEquals("1.1", rootElement, selected);
	}

	/**
	 * See also: bug 72796.
	 */
	@Test
	public void testUserDefinedAssociations() throws CoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType text = manager.getContentType((Platform.PI_RUNTIME + ".text"));

		assertNull("0.1", manager.findContentTypeFor("test.mytext"));
		// associate a user-defined file spec
		text.addFileSpec("mytext", IContentType.FILE_EXTENSION_SPEC);
		boolean assertionFailed = false;
		try {
			IContentType result = manager.findContentTypeFor("test.mytext");
			assertNotNull("1.1", result);
			assertEquals("1.2", text, result);
		} catch (AssertionError afe) {
			assertionFailed = true;
			throw afe;
		} finally {
			text.removeFileSpec("mytext", IContentType.FILE_EXTENSION_SPEC);
			assertFalse(assertionFailed);
		}
		IContentType result = manager.findContentTypeFor("test.mytext");
		assertNull("3.0", result);
	}

	@Test
	public void testDescriberInvalidation() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType type_bug182337_A = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".Bug182337_A");
		IContentType type_bug182337_B = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".Bug182337_B");

		IContentType[] contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "Bug182337.Bug182337");
		assertEquals("1.0", 2, contentTypes.length);
		assertEquals("1.1", type_bug182337_A, contentTypes[0]);
		assertEquals("1.1", type_bug182337_B, contentTypes[1]);

		InputStream is = new InputStream() {
			@Override
			public int read() {
				// throw a non checked exception to emulate a problem with the describer itself
				throw new RuntimeException();
			}
		};
		contentTypes = contentTypeManager.findContentTypesFor(is, "Bug182337.Bug182337");
		assertEquals("1.2", 0, contentTypes.length);

		// Describer should be invalidated by now
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "Bug182337.Bug182337");
		assertEquals("1.3", 0, contentTypes.length);
	}

}
