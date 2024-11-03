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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.internal.content.ContentType;
import org.eclipse.core.internal.content.ContentTypeBuilder;
import org.eclipse.core.internal.content.ContentTypeHandler;
import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.content.IContentConstants;
import org.eclipse.core.internal.content.Util;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeManager.ContentTypeChangeEvent;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.content.XMLContentDescriber;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.harness.TestRegistryChangeListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class IContentTypeManagerTest {
	private static final String TEST_FILES_ROOT = "Plugin_Testing/";

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

	@AfterEach
	public void tearDown() throws Exception {
		// some tests here will trigger a charset delta job (any causing
		// ContentTypeChangeEvents to be broadcast)
		// ensure none is left running after we finish
		Job.getJobManager().wakeUp(FAMILY_CHARSET_DELTA);
		Job.getJobManager().join(FAMILY_CHARSET_DELTA, new FussyProgressMonitor());
	}

	/**
	 * This test shows how we deal with aliases.
	 */
	@Test
	public void testAlias() throws Exception {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType alias = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".alias");
		assertNotNull(alias);
		IContentType derived = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".derived-from-alias");
		assertNotNull(derived);
		IContentType target = contentTypeManager.getContentType("org.eclipse.bundle02.missing-target");
		assertNull(target);
		IContentType[] selected;
		selected = contentTypeManager.findContentTypesFor("foo.missing-target");
		assertThat(selected).containsExactly(alias, derived);
		selected = contentTypeManager.findContentTypesFor(createRandomContentsStream(), "foo.missing-target");
		assertThat(selected).containsExactly(alias, derived);

		// test late addition of content type
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles(() -> {
			IContentType alias1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".alias");
			assertNull(alias1);
			IContentType derived1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".derived-from-alias");
			assertNotNull(derived1);
			IContentType target1 = contentTypeManager.getContentType("org.eclipse.bundle02.missing-target");
			assertNotNull(target1);
			// checks associations
			IContentType[] selected1 = contentTypeManager.findContentTypesFor("foo.missing-target");
			assertThat(selected1).containsExactly(target1, derived1);
			IContentType[] selected2;
			selected2 = contentTypeManager.findContentTypesFor(createRandomContentsStream(), "foo.missing-target");
			assertThat(selected2).containsExactly(target1, derived1);
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bundle02" }, listener);
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
		assertTrue(assoc1.isAssociatedWith(changeCase("text.txt")));
		assertTrue(assoc1.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue(assoc1.isAssociatedWith(changeCase("text.txt_pluginadded")));
		assertTrue(assoc1.isAssociatedWith(changeCase("text.txt_assoc1pluginadded")));
		assertTrue(assoc1.isAssociatedWith(changeCase("text.txt_assoc1useradded")));

		assertFalse(assoc2.isAssociatedWith(changeCase("text.txt")));
		assertFalse(assoc2.isAssociatedWith(changeCase("text.txt_useradded")));
		assertFalse(assoc2.isAssociatedWith(changeCase("text.txt_pluginadded")));
		assertTrue(assoc2.isAssociatedWith(changeCase("text.txt_assoc2pluginadded")));
		assertTrue(assoc2.isAssociatedWith(changeCase("text.txt_assoc2builtin")));
		assertTrue(assoc2.isAssociatedWith(changeCase("text.txt_assoc2useradded")));

		IContentType[] selected;
		// text built-in associations
		selected = finder.findContentTypesFor(changeCase("text.txt"));
		assertThat(selected).containsExactly(text, assoc1);

		// text user-added associations
		selected = finder.findContentTypesFor(changeCase("text.txt_useradded"));
		assertThat(selected).containsExactly(text, assoc1);

		// text provider-added associations
		selected = finder.findContentTypesFor(changeCase("text.txt_pluginadded"));
		assertThat(selected).containsExactly(text, assoc1);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc1pluginadded"));
		assertThat(selected).containsExactly(assoc1);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc1useradded"));
		assertThat(selected).containsExactly(assoc1);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2pluginadded"));
		assertThat(selected).containsExactly(assoc2);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2useradded"));
		assertThat(selected).containsExactly(assoc2);

		selected = finder.findContentTypesFor(changeCase("text.txt_assoc2builtin"));
		assertThat(selected).containsExactly(assoc2);
	}

	@Test
	public void testAssociations() throws CoreException {
		IContentType text = Platform.getContentTypeManager().getContentType(Platform.PI_RUNTIME + ".text");

		// associate a user-defined file spec
		text.addFileSpec("txt_useradded", IContentType.FILE_EXTENSION_SPEC);

		// test associations
		assertTrue(text.isAssociatedWith(changeCase("text.txt")));
		assertTrue(text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue(text.isAssociatedWith(changeCase("text.txt_pluginadded")));

		// check provider defined settings
		String[] providerDefinedExtensions = text
				.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED);
		assertThat(providerDefinedExtensions).contains("txt");
		assertThat(providerDefinedExtensions).doesNotContain("txt_useradded");
		assertThat(providerDefinedExtensions).contains("txt_pluginadded");

		// check user defined settings
		String[] textUserDefinedExtensions = text
				.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertThat(textUserDefinedExtensions).doesNotContain("txt");
		assertThat(textUserDefinedExtensions).contains("txt_useradded");
		assertThat(textUserDefinedExtensions).doesNotContain("txt_pluginadded");

		// removing pre-defined file specs should not do anything
		text.removeFileSpec("txt", IContentType.FILE_EXTENSION_SPEC);
		assertThat(text.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_USER_DEFINED))
				.contains("txt");
		assertTrue(text.isAssociatedWith(changeCase("text.txt")));
		assertTrue(text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue(text.isAssociatedWith(changeCase("text.txt_pluginadded")));

		// removing user file specs is the normal case and has to work as expected
		text.removeFileSpec("txt_useradded", IContentType.FILE_EXTENSION_SPEC);
		assertThat(text.getFileSpecs(IContentType.FILE_EXTENSION_SPEC | IContentType.IGNORE_PRE_DEFINED))
				.doesNotContain("ini");
		assertTrue(text.isAssociatedWith(changeCase("text.txt")));
		assertFalse(text.isAssociatedWith(changeCase("text.txt_useradded")));
		assertTrue(text.isAssociatedWith(changeCase("text.txt_pluginadded")));
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
		assertNotNull(description);
		assertEquals(sampleBinary1, description.getContentType());

		contents = getInputStream(
				new byte[][] { SAMPLE_BIN2_OFFSET.getBytes(), SAMPLE_BIN2_SIGNATURE, " extra contents".getBytes() });
		description = contentTypeManager.getDescriptionFor(contents, null, IContentDescription.ALL);
		assertNotNull(description);
		assertEquals(sampleBinary2, description.getContentType());

		// make sure we ignore that content type when contents are text
		// (see bug 100032)
		// first check if our test environment is sane
		IContentType[] selected = contentTypeManager.findContentTypesFor("test.samplebin2");
		assertThat(selected).hasSize(1)
				.allSatisfy(element -> assertThat(element.getId()).isEqualTo(sampleBinary2.getId()));
		// (we used to blow up here)
		description = contentTypeManager.getDescriptionFor(getReader(createRandomString()), "test.samplebin2",
				IContentDescription.ALL);
		assertNull(description);
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
		assertNotNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals(IContentDescription.BOM_UTF_8,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// tests with UTF-16 Little Endian BOM
		String UTF16_LE_BOM = new String(IContentDescription.BOM_UTF_16LE, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNotNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals(IContentDescription.BOM_UTF_16LE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// tests with UTF-16 Big Endian BOM
		String UTF16_BE_BOM = new String(IContentDescription.BOM_UTF_16BE, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNotNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertEquals(IContentDescription.BOM_UTF_16BE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// test with no BOM
		description = text.getDescriptionFor(
				new ByteArrayInputStream(MINIMAL_XML.getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// tests for partial BOM
		// first byte of UTF-16 Big Endian + minimal xml
		String UTF16_BE_BOM_1byte = new String(new byte[] { (byte) 0xFE }, "ISO-8859-1");
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-16 Big Endian only (see
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=199252)
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_BE_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// first byte of UTF-16 Little Endian + minimal xml
		String UTF16_LE_BOM_1byte = new String(new byte[] { (byte) 0xFF }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-16 Little Endian only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF16_LE_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// first byte of UTF-8 + minimal xml
		String UTF8_BOM_1byte = new String(new byte[] { (byte) 0xEF }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_1byte + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// first byte of UTF-8 only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_1byte).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// two first bytes of UTF-8 + minimal xml
		String UTF8_BOM_2bytes = new String(new byte[] { (byte) 0xEF, (byte) 0xBB }, StandardCharsets.ISO_8859_1);
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_2bytes + MINIMAL_XML).getBytes(StandardCharsets.ISO_8859_1)),
				options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		// two first bytes of UTF-8 only
		description = text.getDescriptionFor(
				new ByteArrayInputStream((UTF8_BOM_2bytes).getBytes(StandardCharsets.ISO_8859_1)), options);
		assertNull(description.getProperty(IContentDescription.BYTE_ORDER_MARK));
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
		assertThat(selected).hasSize(3);
		// all we know is the first one is the base type (only one with a VALID match)
		assertEquals(base, selected[0]);

		selected = manager.findContentTypesFor(getInputStream(contents0), "foo.mybinary");
		// we know also that the second one will be derived1, because it has a full name
		// matching
		assertThat(selected).hasSize(3);
		assertEquals(base, selected[0]);
		assertEquals(derived1, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(contents1), "foo.mybinary");
		// derived1 will be first because both base and derived1 have a strong content
		// matching, so more specific wins
		assertThat(selected).hasSize(3);
		assertEquals(derived1, selected[0]);
		assertEquals(base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(contents2), "foo.mybinary");
		// same as 3.* - derived1 is last because content matching is weak, althoug name
		// matching is strong
		assertThat(selected).hasSize(3);
		assertEquals(derived2, selected[0]);
		assertEquals(base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(invalidContents), "foo.mybinary");
		// all types have weak content matching only - derived1 has strong name matching
		assertThat(selected).hasSize(3);
		assertEquals(derived1, selected[0]);
		assertEquals(base, selected[1]);

		selected = manager.findContentTypesFor(getInputStream(invalidContents), "anything.mybinary");
		// all types have weak content/name matching only - most general wins
		assertThat(selected).hasSize(3);
		assertEquals(base, selected[0]);
	}

	/*
	 * Tests both text and byte stream-based getDescriptionFor methods.
	 */
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testContentDescription(boolean text) throws IOException, CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType xmlType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		IContentType mytext = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext");
		IContentType mytext1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext1");
		IContentType mytext2 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext2");

		IContentDescription description;

		description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml", IContentDescription.ALL,
				text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		// the default charset should have been filled by the content type manager
		assertEquals("UTF-8", description.getProperty(IContentDescription.CHARSET));
		assertSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, XML_ISO_8859_1, StandardCharsets.ISO_8859_1, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("ISO-8859-1", description.getProperty(IContentDescription.CHARSET));
		assertNotSame(xmlType.getDefaultDescription(), description);

		// ensure we handle single quotes properly (bug 65443)
		description = getDescriptionFor(finder, XML_ISO_8859_1_SINGLE_QUOTES, StandardCharsets.ISO_8859_1, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("ISO-8859-1", description.getProperty(IContentDescription.CHARSET));
		assertNotSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, XML_UTF_16, StandardCharsets.UTF_16, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET, IContentDescription.BYTE_ORDER_MARK }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("UTF-16", description.getProperty(IContentDescription.CHARSET));
		assertTrue(text
				|| IContentDescription.BOM_UTF_16BE == description.getProperty(IContentDescription.BYTE_ORDER_MARK));
		assertNotSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, XML_UTF_16BE, StandardCharsets.UTF_8, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("UTF-16BE", description.getProperty(IContentDescription.CHARSET));
		assertNotSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, XML_UTF_16LE, StandardCharsets.UTF_8, "foo.xml",
				new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		// the default charset should have been filled by the content type manager
		assertEquals("UTF-16LE", description.getProperty(IContentDescription.CHARSET));
		assertNotSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, MINIMAL_XML, StandardCharsets.UTF_8, "foo.xml", IContentDescription.ALL,
				text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("UTF-8", description.getProperty(IContentDescription.CHARSET));
		assertNotNull(mytext);
		assertEquals("BAR", mytext.getDefaultCharset());
		assertSame(xmlType.getDefaultDescription(), description);

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
		assertNotNull(description);
		assertEquals(mytext, description.getContentType());
		assertEquals("BAR", description.getProperty(IContentDescription.CHARSET));
		assertSame(mytext.getDefaultDescription(), description);
		// now plays with setting a non-default default charset
		mytext.setDefaultCharset("FOO");

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
		assertNotNull(description);
		assertEquals(mytext, description.getContentType());
		assertEquals("FOO", description.getProperty(IContentDescription.CHARSET));
		assertSame(mytext.getDefaultDescription(), description);
		mytext.setDefaultCharset(null);

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, text);
		assertNotNull(description);
		assertEquals(mytext, description.getContentType());
		assertEquals("BAR", description.getProperty(IContentDescription.CHARSET));
		assertSame(mytext.getDefaultDescription(), description);

		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=176354
		description = getDescriptionFor(finder,
				"<?xml version=\'1.0\' encoding=\'UTF-8\'?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://www.example.org/\" xmlns:ns0=\"http://another.example.org/\"><soapenv:Header /><soapenv:Body><ns0:x /></soapenv:Body></soapenv:Envelope>",
				StandardCharsets.UTF_8, "foo.xml", new QualifiedName[] { IContentDescription.CHARSET }, text);
		assertNotNull(description);
		assertEquals(xmlType, description.getContentType());
		assertEquals("UTF-8", description.getProperty(IContentDescription.CHARSET));
		assertEquals(xmlType.getDefaultDescription().getCharset(), description.getCharset());

		assertNotNull(mytext1);
		assertEquals("BAR", mytext1.getDefaultCharset());
		assertNotNull(mytext2);
		assertNull(mytext2.getDefaultCharset());

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
		assertNull(finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if inappropriate and appropriate are provided, appropriate will be selected
		finder = contentTypeManager
				.getMatcher(new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate }), null);
		assertEquals(appropriate, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if inappropriate, appropriate and a more specific appropriate type are
		// provided, the specific type will be selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate, appropriateSpecific1 }),
				null);
		assertEquals(appropriateSpecific1, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { inappropriate, appropriate, appropriateSpecific2 }),
				null);
		assertEquals(appropriateSpecific2, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if all are provided, the more specific types will appear before the more
		// generic types
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(
						new IContentType[] { inappropriate, appropriate, appropriateSpecific1, appropriateSpecific2 }),
				null);
		IContentType[] selected = finder.findContentTypesFor(getInputStream(MINIMAL_XML), null);
		assertThat(selected).satisfiesAnyOf(
				elements -> assertThat(elements).containsExactly(appropriateSpecific1, appropriateSpecific2,
						appropriate),
				elements -> assertThat(elements).containsExactly(appropriateSpecific2, appropriateSpecific1,
						appropriate));

		// if appropriate and a more specific appropriate type (but with low priority)
		// are provided, the specific type will be selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(new IContentType[] { appropriate, appropriateSpecific1LowPriority }), null);
		assertEquals(appropriateSpecific1LowPriority,
				finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));

		// if appropriate and two specific appropriate types (but one with lower
		// priority) are provided, the specific type with higher priority will be
		// selected
		finder = contentTypeManager.getMatcher(
				new SubsetSelectionPolicy(
						new IContentType[] { appropriate, appropriateSpecific1, appropriateSpecific1LowPriority }),
				null);
		assertEquals(appropriateSpecific1, finder.findContentTypeFor(getInputStream(MINIMAL_XML), null));
	}

	@Test
	public void testDefaultProperties() throws IOException /* never actually thrown */ {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();

		IContentType mytext = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext");
		IContentType mytext1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext1");
		IContentType mytext2 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "mytext2");
		assertNotNull(mytext);
		assertNotNull(mytext1);
		assertNotNull(mytext2);

		QualifiedName charset = IContentDescription.CHARSET;
		QualifiedName localCharset = new QualifiedName(PI_RESOURCES_TESTS, "charset");
		QualifiedName property1 = new QualifiedName(PI_RESOURCES_TESTS, "property1");
		QualifiedName property2 = new QualifiedName(PI_RESOURCES_TESTS, "property2");
		QualifiedName property3 = new QualifiedName(PI_RESOURCES_TESTS, "property3");
		QualifiedName property4 = new QualifiedName(PI_RESOURCES_TESTS, "property4");

		IContentDescription description;
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt", IContentDescription.ALL, true);
		assertNotNull(description);
		assertEquals(mytext, description.getContentType());
		assertEquals("value1", description.getProperty(property1));
		assertNull(description.getProperty(property2));
		assertEquals("value3", description.getProperty(property3));
		assertEquals("BAR", description.getProperty(charset));

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt1", IContentDescription.ALL, true);
		assertNotNull(description);
		assertEquals(mytext1, description.getContentType());
		assertEquals("value1", description.getProperty(property1));
		assertEquals("value2", description.getProperty(property2));
		assertNull(description.getProperty(property3));
		assertEquals("value4", description.getProperty(property4));
		assertEquals("BAR", description.getProperty(charset));

		description = getDescriptionFor(finder, "some contents", null, "abc.tzt2", IContentDescription.ALL, true);
		assertNotNull(description);
		assertEquals(mytext2, description.getContentType());
		assertNull(description.getProperty(property1));
		assertNull(description.getProperty(property2));
		assertNull(description.getProperty(property3));
		assertNull(description.getProperty(property4));
		assertNull(description.getProperty(charset));
		assertEquals("mytext2", description.getProperty(localCharset));
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
		assertNotNull(fooBarType);
		IContentType subFooBarType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "subFooBar");
		assertNotNull(subFooBarType);
		// ensure we don't get fooBar twice
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);
		IContentType[] fooBarAssociated = finder.findContentTypesFor(changeCase("foo.bar"));
		assertThat(fooBarAssociated).containsExactlyInAnyOrder(fooBarType, subFooBarType);
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
	public void testDynamicChanges() throws Exception {
		final IContentType[] text = new IContentType[4];
		final IContentTypeManager manager = Platform.getContentTypeManager();
		text[0] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[0]);
		text[1] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[1]);
		text[0] = ((ContentTypeHandler) text[0]).getTarget();
		text[1] = ((ContentTypeHandler) text[1]).getTarget();
		assertEquals(text[0], text[1]);
		assertEquals(text[0], text[1]);
		// make arbitrary dynamic changes to the contentTypes extension point
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles(() -> {
			IContentType missing = manager.getContentType("org.eclipse.bundle01.missing");
			assertNotNull(missing);
			// ensure the content type instances are different
			text[2] = manager.getContentType(IContentTypeManager.CT_TEXT);
			assertNotNull(text[2]);
			text[2] = ((ContentTypeHandler) text[2]).getTarget();
			assertEquals(text[0], text[2]);
			assertNotSame(text[0], text[2]);
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bundle01" }, listener);
		assertNull(manager.getContentType("org.eclipse.bundle01.missing"));
		// ensure the content type instances are all different
		text[3] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[3]);
		text[3] = ((ContentTypeHandler) text[3]).getTarget();
		assertEquals(text[0], text[3]);
		assertEquals(text[2], text[3]);
		assertNotSame(text[0], text[3]);
		assertNotSame(text[2], text[3]);
	}

	/**
	 * Similar to testDynamicChanges, but using the
	 * org.eclipse.core.contenttype.contentTypes extension point.
	 */
	@Test
	public void testDynamicChangesNewExtension() throws Exception {
		final IContentType[] text = new IContentType[4];
		final IContentTypeManager manager = Platform.getContentTypeManager();
		text[0] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[0]);
		text[1] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[1]);
		text[0] = ((ContentTypeHandler) text[0]).getTarget();
		text[1] = ((ContentTypeHandler) text[1]).getTarget();
		assertEquals(text[0], text[1]);
		assertSame(text[0], text[1]);
		// make arbitrary dynamic changes to the contentTypes extension point
		TestRegistryChangeListener listener = new TestRegistryChangeListener(IContentConstants.CONTENT_NAME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles(() -> {
			IContentType contentType = manager.getContentType("org.eclipse.bug485227.bug485227_contentType");
			assertNotNull(contentType, "Contributed content type not found");
			// ensure the content type instances are different
			text[2] = manager.getContentType(IContentTypeManager.CT_TEXT);
			assertNotNull(text[2], "Text content type not modified");
			text[2] = ((ContentTypeHandler) text[2]).getTarget();
			assertEquals(text[0], text[2]);
			assertNotSame(text[0], text[2]);
			assertEquals(contentType, manager.findContentTypeFor("file.bug485227"), "default extension not associated");
			assertEquals(contentType, manager.findContentTypeFor("file.bug485227_2"),
					"additional extension not associated");
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bug485227" }, listener);
		assertNull(manager.getContentType("org.eclipse.bug485227.bug485227_contentType"),
				"Content type not cleared after bundle uninstall");
		// ensure the content type instances are all different
		text[3] = manager.getContentType(IContentTypeManager.CT_TEXT);
		assertNotNull(text[3]);
		text[3] = ((ContentTypeHandler) text[3]).getTarget();
		assertEquals(text[0], text[3]);
		assertEquals(text[2], text[3]);
		assertNotSame(text[0], text[3]);
		assertNotSame(text[2], text[3]);
	}

	@Test
	public void testEvents() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType myType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".myContent");
		assertNotNull(myType);

		ContentTypeChangeTracer tracer;

		tracer = new ContentTypeChangeTracer();
		contentTypeManager.addContentTypeChangeListener(tracer);

		// add a file spec and check event
		myType.addFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertTrue(tracer.isOnlyChange(myType));

		// remove a non-existing file spec - should not cause an event to be fired
		tracer.reset();
		myType.removeFileSpec("another.file.name", IContentType.FILE_EXTENSION_SPEC);
		assertFalse(tracer.isOnlyChange(myType));

		// add a file spec again and check no event is generated
		tracer.reset();
		myType.addFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertFalse(tracer.isOnlyChange(myType));

		// remove a file spec and check event
		tracer.reset();
		myType.removeFileSpec("another.file.name", IContentType.FILE_NAME_SPEC);
		assertTrue(tracer.isOnlyChange(myType));

		// change the default charset and check event
		tracer.reset();
		myType.setDefaultCharset("FOO");
		assertTrue(tracer.isOnlyChange(myType));

		// set the default charset to the same - no event should be generated
		tracer.reset();
		myType.setDefaultCharset("FOO");
		assertFalse(tracer.isOnlyChange(myType));

		myType.setDefaultCharset("ABC");
	}

	@Test
	public void testFileSpecConflicts() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		// when not submitting contents, for related types, most general type prevails
		IContentType conflict1a = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict1");
		IContentType conflict1b = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict1");
		assertNotNull(conflict1a);
		assertNotNull(conflict1b);
		IContentType preferredConflict1 = manager.findContentTypeFor("test.conflict1");
		assertNotNull(preferredConflict1);
		assertEquals(conflict1a, preferredConflict1);

		IContentType conflict2base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict2");
		IContentType conflict2sub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict2");
		assertNotNull(conflict2base);
		assertNotNull(conflict2sub);

		// when submitting contents, for related types with indeterminate match, general
		// comes first
		IContentType[] selectedConflict2 = manager.findContentTypesFor(createRandomContentsStream(), "test.conflict2");
		assertThat(selectedConflict2).containsExactly(conflict2base, conflict2sub);

		IContentType conflict2abase = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict2a");
		IContentType conflict2asub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict2a");
		assertNotNull(conflict2abase);
		assertNotNull(conflict2asub);

		// when submitting contents, for related types with valid match, specific
		// comes first
		IContentType[] selectedConflict2a = manager
				.findContentTypesFor(getInputStream("conflict2a", StandardCharsets.UTF_8), "test.conflict2a");
		assertThat(selectedConflict2a).containsExactly(conflict2asub, conflict2abase);

		// when not submitting contents, for related types, most general type prevails
		selectedConflict2a = manager.findContentTypesFor("test.conflict2a");
		assertThat(selectedConflict2a).containsExactly(conflict2abase, conflict2asub);

		IContentType conflict3base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict3");
		IContentType conflict3sub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict3");
		IContentType conflict3unrelated = manager.getContentType(PI_RESOURCES_TESTS + ".unrelated_conflict3");
		assertNotNull(conflict3base);
		assertNotNull(conflict3sub);
		assertNotNull(conflict3unrelated);

		// Two unrelated types (sub_conflict3 and unrelated conflict3) are in conflict.
		// Order will be arbitrary (lexicographically).

		IContentType[] selectedConflict3 = manager.findContentTypesFor(createRandomContentsStream(), "test.conflict3");
		assertThat(selectedConflict3).containsExactly(conflict3sub, conflict3unrelated);

		IContentType conflict4base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict4");
		IContentType conflict4sub = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict4");
		IContentType conflict4unrelated_lowPriority = manager.getContentType(PI_RESOURCES_TESTS + ".unrelated_conflict4");
		assertNotNull(conflict4base);
		assertNotNull(conflict4sub);
		assertNotNull(conflict4unrelated_lowPriority);

		// Two unrelated types (sub_conflict4 and unrelated conflict4) are in conflict,
		// but with different priorities
		// Order will be based on priority

		IContentType[] selectedConflict4 = manager.findContentTypesFor(createRandomContentsStream(), "test.conflict4");
		assertThat(selectedConflict4).containsExactly(conflict4sub, conflict4unrelated_lowPriority);

		IContentType conflict5base = manager.getContentType(PI_RESOURCES_TESTS + ".base_conflict5");
		IContentType conflict5sub_lowPriority = manager.getContentType(PI_RESOURCES_TESTS + ".sub_conflict5");
		IContentType conflict5unrelated = manager.getContentType(PI_RESOURCES_TESTS + ".unrelated_conflict5");
		assertNotNull(conflict5base);
		assertNotNull(conflict5sub_lowPriority);
		assertNotNull(conflict5unrelated);

		// Two unrelated types (sub_conflict5 and unrelated conflict5) are in conflict,
		// but with different priorities
		// Order will be based on priority

		IContentType[] selectedConflict5 = manager.findContentTypesFor(createRandomContentsStream(), "test.conflict5");
		assertThat(selectedConflict5).containsExactly(conflict5unrelated, conflict5sub_lowPriority);
	}

	@Test
	public void testFindContentType() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		IContentType xmlContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");

		IContentType single;

		single = finder.findContentTypeFor(getInputStream("Just a test"), changeCase("file.txt"));
		assertNotNull(single);
		assertEquals(textContentType, single);

		single = finder.findContentTypeFor(getInputStream(XML_UTF_8, StandardCharsets.UTF_8), changeCase("foo.xml"));
		assertNotNull(single);
		assertEquals(xmlContentType, single);

		IContentType[] multiple = finder.findContentTypesFor(getInputStream(XML_UTF_8, StandardCharsets.UTF_8), null);
		assertThat(multiple).contains(xmlContentType);
	}

	@Test
	public void testFindContentTypPredefinedRegexp() throws IOException, CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);

		IContentType targetContentType = contentTypeManager
				.getContentType("org.eclipse.core.tests.resources.predefinedContentTypeWithRegexp");
		assertNotNull(targetContentType, "Target content-type not found");

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
		assertNull(single, "File pattern unknown at that point");

		textContentType.addFileSpec("*Text*", IContentType.FILE_PATTERN_SPEC);
		try {
			single = finder.findContentTypeFor(getInputStream("Just a test"), "someText.unknown");
			assertEquals(textContentType, single, "Text content should now match *Text* files");
		} finally {
			textContentType.removeFileSpec("*Text*", IContentType.FILE_PATTERN_SPEC);
		}
		single = finder.findContentTypeFor(getInputStream("Just a test"), "someText.unknown");
		assertNull(single, "File pattern unknown at that point");
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
	public void testInvalidMarkup() throws Exception {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentTypeMatcher finder = contentTypeManager.getMatcher(new LocalSelectionPolicy(), null);
		assertThat(finder.findContentTypesFor("invalid.missing.identifier")).isEmpty();
		assertThat(finder.findContentTypesFor("invalid.missing.name")).isEmpty();
		assertNull(contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "invalid-missing-name"));
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles(() -> {
			// ensure the invalid content types are not available
			assertThat(contentTypeManager.findContentTypesFor("invalid.missing.identifier")).isEmpty();
			assertThat(contentTypeManager.findContentTypesFor("invalid.missing.name")).isEmpty();
			assertNull(contentTypeManager.getContentType("org.eclipse.bundle03.invalid-missing-name"));
			// this content type has good markup, but invalid describer class
			IContentType invalidDescriber = contentTypeManager.getContentType("org.eclipse.bundle03.invalid-describer");
			assertNotNull(invalidDescriber);
			// name based matching should work fine
			assertEquals(invalidDescriber, contentTypeManager.findContentTypeFor("invalid.describer"));
			// the describer class is invalid, content matchong should fail
			IContentType nullContentType;
			nullContentType = contentTypeManager.findContentTypeFor(createRandomContentsStream(), "invalid.describer");
			assertNull(nullContentType);
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bundle03" }, listener);
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
		assertThat(selected).contains(xml, rootElement);

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
		assertTrue(textContentType.isKindOf(textContentType));
		assertTrue(xmlContentType.isKindOf(textContentType));
		assertFalse(textContentType.isKindOf(xmlContentType));
		assertTrue(xmlContentType.isKindOf(xmlContentType));
		assertTrue(xmlBasedDifferentExtensionContentType.isKindOf(textContentType));
		assertTrue(xmlBasedDifferentExtensionContentType.isKindOf(xmlContentType));
		assertFalse(xmlBasedDifferentExtensionContentType.isKindOf(xmlBasedSpecificNameContentType));
		assertFalse(binaryContentType.isKindOf(textContentType));
	}

	@Test
	public void testListParsing() {
		String[] list;
		list = Util.parseItems(null);
		assertThat(list).isEmpty();
		list = Util.parseItems("");
		assertThat(list).containsExactly("");
		list = Util.parseItems("foo");
		assertThat(list).containsExactly("foo");
		list = Util.parseItems(",");
		assertThat(list).containsExactly("", "");
		list = Util.parseItems(",foo,bar");
		assertThat(list).containsExactly("", "foo", "bar");
		list = Util.parseItems("foo,bar,");
		assertThat(list).containsExactly("foo", "bar", "");
		list = Util.parseItems("foo,,bar");
		assertThat(list).containsExactly("foo", "", "bar");
		list = Util.parseItems("foo,,,bar");
		assertThat(list).containsExactly("foo", "", "", "bar");
		list = Util.parseItems(",,foo,bar");
		assertThat(list).containsExactly("", "", "foo", "bar");
		list = Util.parseItems("foo,bar,,");
		assertThat(list).containsExactly("foo", "bar", "", "");
		list = Util.parseItems(",,,");
		assertThat(list).containsExactly("", "", "", "");
	}

	@Test
	public void testMyContentDescriber() throws IOException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType myContent = manager.getContentType(PI_RESOURCES_TESTS + '.' + "myContent");
		assertNotNull(myContent);
		assertEquals(myContent, manager.findContentTypeFor("myContent.mc1"));
		assertEquals(myContent, manager.findContentTypeFor("myContent.mc2"));
		assertEquals(myContent, manager.findContentTypeFor("foo.myContent1"));
		assertEquals(myContent, manager.findContentTypeFor("bar.myContent2"));
		IContentDescription description = manager.getDescriptionFor(
				getInputStream(MyContentDescriber.SIGNATURE, StandardCharsets.US_ASCII), "myContent.mc1",
				IContentDescription.ALL);
		assertNotNull(description);
		assertEquals(myContent, description.getContentType());
		assertNotSame(myContent.getDefaultDescription(), description);
		for (int i = 0; i < MyContentDescriber.MY_OPTIONS.length; i++) {
			assertEquals(MyContentDescriber.MY_OPTION_VALUES[i],
					description.getProperty(MyContentDescriber.MY_OPTIONS[i]), i + "");
		}
	}

	@Test
	public void testNoExtensionAssociation() throws Exception {
		// TODO use a IContentTypeMatcher instead
		final IContentTypeManager manager = Platform.getContentTypeManager();

		IContentType[] selected = manager.findContentTypesFor("file_with_no_extension");
		assertThat(selected).isEmpty();

		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		BundleTestingHelper.runWithBundles(() -> {
			final String namespace = "org.eclipse.bundle04";

			IContentType empty1 = manager.getContentType(namespace + ".empty_extension1");
			IContentType empty2 = manager.getContentType(namespace + ".empty_extension2");
			IContentType empty3 = manager.getContentType(namespace + ".empty_extension3");
			IContentType empty4 = manager.getContentType(namespace + ".empty_extension4");
			IContentType nonEmpty = manager.getContentType(namespace + ".non_empty_extension");

			assertNotNull(empty1);
			assertNotNull(empty2);
			assertNotNull(empty3);
			assertNotNull(empty4);
			assertNotNull(nonEmpty);

			IContentType[] selected1 = manager.findContentTypesFor("file_with_no_extension");
			assertThat(selected1).containsExactlyInAnyOrder(empty1, empty2, empty3, empty4);

			selected1 = manager.findContentTypesFor("file_with_extension.non-empty");
			assertThat(selected1).containsExactly(nonEmpty);

			nonEmpty.addFileSpec("", IContentType.FILE_EXTENSION_SPEC);
			try {
				selected1 = manager.findContentTypesFor("file_with_no_extension");
				assertThat(selected1).hasSize(5).containsOnlyOnce(nonEmpty);
			} finally {
				nonEmpty.removeFileSpec("", IContentType.FILE_EXTENSION_SPEC);
			}
			selected1 = manager.findContentTypesFor("file_with_no_extension");
			assertThat(selected1).hasSize(4).doesNotContain(nonEmpty);
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bundle04" }, listener);
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
		assertEquals(xml, selected);
		// it should be equivalent to omitting the contents
		assertEquals(xml, finder.findContentTypeFor("foo.xml"));
	}

	/**
	 * This test shows how we deal with orphan file associations (associations whose
	 * content types are missing).
	 */
	@Test
	public void testOrphanContentType() throws Exception {
		final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType orphan = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".orphan");
		assertNull(orphan);
		IContentType missing = contentTypeManager.getContentType("org.eclipse.bundle01.missing");
		assertNull(missing);
		assertThat(contentTypeManager.findContentTypesFor("foo.orphan")).isEmpty();
		assertThat(contentTypeManager.findContentTypesFor("orphan.orphan")).isEmpty();
		assertThat(contentTypeManager.findContentTypesFor("foo.orphan2")).isEmpty();

		// test late addition of content type - orphan2 should become visible
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME,
				ContentTypeBuilder.PT_CONTENTTYPES, null, null);

		BundleTestingHelper.runWithBundles(() -> {
			IContentType orphan1 = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".orphan");
			assertNotNull(orphan1);
			IContentType missing1 = contentTypeManager.getContentType("org.eclipse.bundle01.missing");
			assertNotNull(missing1);
			// checks orphan's associations
			assertThat(contentTypeManager.findContentTypesFor("foo.orphan")).containsExactly(orphan1);
			assertThat(contentTypeManager.findContentTypesFor("orphan.orphan")).containsExactly(orphan1);
			// check whether an orphan association was added to the dynamically added bundle
			assertThat(contentTypeManager.findContentTypesFor("foo.orphan2")).containsExactly(missing1);
			return null;
		}, getContext(), new String[] { TEST_FILES_ROOT + "content/bundle01" }, listener);
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
		assertNotNull(text);

		// ensure the "default charset" preference is being properly used
		assertNull(text.getDefaultCharset());
		assertNull(textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));
		text.setDefaultCharset("UTF-8");
		assertEquals("UTF-8", textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));
		text.setDefaultCharset(null);
		assertNull(textPrefs.get(ContentType.PREF_DEFAULT_CHARSET, null));

		// ensure the file spec preferences are being properly used
		// some sanity checking
		assertFalse(text.isAssociatedWith("xyz.foo"));
		assertFalse(text.isAssociatedWith("xyz.bar"));
		assertFalse(text.isAssociatedWith("foo.ext"));
		assertFalse(text.isAssociatedWith("bar.ext"));
		// Null entries first to avoid interference from other tests
		textPrefs.remove(ContentType.PREF_FILE_NAMES);
		textPrefs.remove(ContentType.PREF_FILE_EXTENSIONS);
		// play with file name associations first...
		assertNull(textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		assertNull(textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		text.addFileSpec("foo.ext", IContentType.FILE_NAME_SPEC);
		assertTrue(text.isAssociatedWith("foo.ext"));
		assertEquals("foo.ext", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		text.addFileSpec("bar.ext", IContentType.FILE_NAME_SPEC);
		assertTrue(text.isAssociatedWith("bar.ext"));
		assertEquals("foo.ext,bar.ext", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
		// ... and then with file extensions
		text.addFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		assertTrue(text.isAssociatedWith("xyz.foo"));
		assertEquals("foo", textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		text.addFileSpec("bar", IContentType.FILE_EXTENSION_SPEC);
		assertTrue(text.isAssociatedWith("xyz.bar"));
		assertEquals("foo,bar", textPrefs.get(ContentType.PREF_FILE_EXTENSIONS, null));
		// remove all associations made
		text.removeFileSpec("foo.ext", IContentType.FILE_NAME_SPEC);
		text.removeFileSpec("bar.ext", IContentType.FILE_NAME_SPEC);
		text.removeFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		text.removeFileSpec("bar", IContentType.FILE_EXTENSION_SPEC);
		// ensure all is as before
		assertFalse(text.isAssociatedWith("xyz.foo"));
		assertFalse(text.isAssociatedWith("xyz.bar"));
		assertFalse(text.isAssociatedWith("foo.ext"));
		assertFalse(text.isAssociatedWith("bar.ext"));

		// ensure the serialization format is correct
		try {
			text.addFileSpec("foo.bar", IContentType.FILE_NAME_SPEC);
			textPrefs.sync();
			assertEquals("foo.bar", textPrefs.get(ContentType.PREF_FILE_NAMES, null));
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
		assertNotNull(textContentType);
		assertTrue(isText(contentTypeManager, textContentType));
		assertNotNull(((ContentTypeHandler) textContentType).getTarget().getDescriber());

		IContentType xmlContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + ".xml");
		assertNotNull(xmlContentType);
		assertTrue(isText(contentTypeManager, xmlContentType));
		assertEquals(textContentType, xmlContentType.getBaseType());
		IContentDescriber xmlDescriber = ((ContentTypeHandler) xmlContentType).getTarget().getDescriber();
		assertNotNull(xmlDescriber);
		assertThat(xmlDescriber).isInstanceOf(XMLContentDescriber.class);

		IContentType xmlBasedDifferentExtensionContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-different-extension");
		assertNotNull(xmlBasedDifferentExtensionContentType);
		assertTrue(isText(contentTypeManager, xmlBasedDifferentExtensionContentType));
		assertEquals(xmlContentType, xmlBasedDifferentExtensionContentType.getBaseType());

		IContentType xmlBasedSpecificNameContentType = contentTypeManager
				.getContentType(PI_RESOURCES_TESTS + '.' + "xml-based-specific-name");
		assertNotNull(xmlBasedSpecificNameContentType);
		assertTrue(isText(contentTypeManager, xmlBasedSpecificNameContentType));
		assertEquals(xmlContentType, xmlBasedSpecificNameContentType.getBaseType());

		IContentType[] xmlTypes = finder.findContentTypesFor(changeCase("foo.xml"));
		assertThat(xmlTypes).contains(xmlContentType);

		IContentType binaryContentType = contentTypeManager.getContentType(PI_RESOURCES_TESTS + '.' + "sample-binary1");
		assertNotNull(binaryContentType);
		assertFalse(isText(contentTypeManager, binaryContentType));
		assertNull(binaryContentType.getBaseType());

		IContentType[] binaryTypes = finder.findContentTypesFor(changeCase("foo.samplebin1"));
		assertThat(binaryTypes).containsExactly(binaryContentType);

		IContentType myText = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".mytext");
		assertNotNull(myText);
		assertEquals("BAR", myText.getDefaultCharset());

		IContentType[] fooBarTypes = finder.findContentTypesFor(changeCase("foo.bar"));
		assertThat(fooBarTypes).hasSize(2);

		IContentType fooBar = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".fooBar");
		assertNotNull(fooBar);
		IContentType subFooBar = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".subFooBar");
		assertNotNull(subFooBar);
		assertThat(fooBarTypes).contains(fooBar, subFooBar);
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
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(rootElement, atIndex(0));

		// bugs 64053 and 63298
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_EXTERNAL_ENTITY, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(rootElement, atIndex(0));

		// bug 63625
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_EXTERNAL_ENTITY2, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(rootElement, atIndex(0));

		// bug 135575
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MATCH1, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(nsRootElement, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(nsRootElement, atIndex(0));
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_WRONG_ELEM, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(xmlType, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WRONG_NS, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(xmlType, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_MIXUP, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(xmlType, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WILDCARD, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(nsWildcard, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NS_WILDCARD2, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(nsWildcard, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_EMPTY_NS, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(emptyNsRootElement, atIndex(0));

		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_DTD_US_ASCII, StandardCharsets.US_ASCII), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(dtdElement, atIndex(0));
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_DTD_EXTERNAL_ENTITY, StandardCharsets.UTF_8), "fake.xml");
		assertThat(contentTypes).hasSizeGreaterThan(0).contains(dtdElement, atIndex(0));

		// bug 67975
		IContentDescription description = contentTypeManager.getDescriptionFor(getInputStream(
				new byte[][] { IContentDescription.BOM_UTF_16BE, XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-16BE") }),
				"fake.xml", IContentDescription.ALL);
		assertNotNull(description);
		assertEquals(rootElement, description.getContentType());
		assertEquals(IContentDescription.BOM_UTF_16BE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		description = contentTypeManager.getDescriptionFor(getInputStream(
				new byte[][] { IContentDescription.BOM_UTF_16LE, XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-16LE") }),
				"fake.xml", IContentDescription.ALL);
		assertNotNull(description);
		assertEquals(rootElement, description.getContentType());
		assertEquals(IContentDescription.BOM_UTF_16LE,
				description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// due to bug 67048, the test below fails with Crimson parser (does not handle
		// UTF-8 BOMs)
		// description = contentTypeManager.getDescriptionFor(getInputStream(new
		// byte[][]
		// {IContentDescription.BOM_UTF_8,XML_ROOT_ELEMENT_NO_DECL.getBytes("UTF-8")}),
		// "fake.xml", IContentDescription.ALL);
		// assertTrue(description != null);
		// assertEquals(rootElement, description.getContentType());
		// assertEquals(IContentDescription.BOM_UTF_8,
		// description.getProperty(IContentDescription.BYTE_ORDER_MARK));

		// bug 84354
		contentTypes = contentTypeManager
				.findContentTypesFor(getInputStream(XML_ROOT_ELEMENT_NO_DECL, StandardCharsets.UTF_8), "test.txt");
		assertThat(contentTypes).hasSizeGreaterThan(0)
				.contains(contentTypeManager.getContentType(IContentTypeManager.CT_TEXT), atIndex(0));
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
		assertNotNull(selected);
		assertEquals(rootElement, selected);
	}

	/**
	 * See also: bug 72796.
	 */
	@Test
	public void testUserDefinedAssociations() throws CoreException {
		IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType text = manager.getContentType((Platform.PI_RUNTIME + ".text"));

		assertNull(manager.findContentTypeFor("test.mytext"));
		// associate a user-defined file spec
		text.addFileSpec("mytext", IContentType.FILE_EXTENSION_SPEC);
		boolean assertionFailed = false;
		try {
			IContentType result = manager.findContentTypeFor("test.mytext");
			assertNotNull(result);
			assertEquals(text, result);
		} catch (AssertionError afe) {
			assertionFailed = true;
			throw afe;
		} finally {
			text.removeFileSpec("mytext", IContentType.FILE_EXTENSION_SPEC);
			assertFalse(assertionFailed);
		}
		IContentType result = manager.findContentTypeFor("test.mytext");
		assertNull(result);
	}

	@Test
	public void testDescriberInvalidation() throws IOException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		IContentType type_bug182337_A = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".Bug182337_A");
		IContentType type_bug182337_B = contentTypeManager.getContentType(PI_RESOURCES_TESTS + ".Bug182337_B");

		IContentType[] contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "Bug182337.Bug182337");
		assertThat(contentTypes).containsExactly(type_bug182337_A, type_bug182337_B);

		InputStream is = new InputStream() {
			@Override
			public int read() {
				// throw a non checked exception to emulate a problem with the describer itself
				throw new RuntimeException();
			}
		};
		contentTypes = contentTypeManager.findContentTypesFor(is, "Bug182337.Bug182337");
		assertThat(contentTypes).isEmpty();

		// Describer should be invalidated by now
		contentTypes = contentTypeManager.findContentTypesFor(
				getInputStream(XML_ROOT_ELEMENT_NS_MATCH2, StandardCharsets.UTF_8), "Bug182337.Bug182337");
		assertThat(contentTypes).isEmpty();
	}

	private BundleContext getContext() {
		return Platform.getBundle(PI_RESOURCES_TESTS).getBundleContext();
	}

}
