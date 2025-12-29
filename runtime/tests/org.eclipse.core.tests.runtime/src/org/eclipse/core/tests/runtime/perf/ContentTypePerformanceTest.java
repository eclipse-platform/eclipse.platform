/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.core.tests.runtime.perf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.harness.FileSystemHelper.clear;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.runtime.RuntimeTestsPlugin.PI_RUNTIME_TESTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import org.eclipse.core.internal.content.ContentTypeBuilder;
import org.eclipse.core.internal.content.ContentTypeHandler;
import org.eclipse.core.internal.content.Util;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.BinarySignatureDescriber;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.harness.TestRegistryChangeListener;
import org.eclipse.core.tests.harness.session.PerformanceSessionTest;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

@SuppressWarnings("restriction")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContentTypePerformanceTest {

	@RegisterExtension
	static final SessionTestExtension sessionTestExtension = SessionTestExtension
			.forPlugin(RuntimeTestsPlugin.PI_RUNTIME_TESTS).create();

	private final static String CONTENT_TYPE_PREF_NODE = Platform.PI_RUNTIME + IPath.SEPARATOR + "content-types"; //$NON-NLS-1$
	private static final String DEFAULT_NAME = "file_" + ContentTypePerformanceTest.class.getName();
	private static final int ELEMENTS_PER_LEVEL = 4;
	private static final int NUMBER_OF_LEVELS = 4;
	private static final String TEST_DATA_ID = "org.eclipse.core.tests.runtime.contenttype.perf.testdata";
	private static final int TOTAL_NUMBER_OF_ELEMENTS = computeTotalTypes(NUMBER_OF_LEVELS, ELEMENTS_PER_LEVEL);

	private static int computeTotalTypes(int levels, int elementsPerLevel) {
		double sum = 0;
		for (int i = 0; i <= levels; i++) {
			sum += Math.pow(elementsPerLevel, i);
		}
		return (int) sum;
	}

	private static String createContentType(Writer writer, int number, String baseTypeId) throws IOException {
		String id = "performance" + number;
		String definition = generateContentType(number, id, baseTypeId, new String[] {DEFAULT_NAME}, null);
		writer.write(definition);
		writer.write(System.lineSeparator());
		return id;
	}

	public static int createContentTypes(Writer writer, String baseTypeId, int created, int numberOfLevels, int nodesPerLevel) throws IOException {
		if (numberOfLevels == 0) {
			return 0;
		}
		int local = nodesPerLevel;
		for (int i = 0; i < nodesPerLevel; i++) {
			String id = createContentType(writer, created + i, baseTypeId);
			local += createContentTypes(writer, id, created + local, numberOfLevels - 1, nodesPerLevel);
		}
		return local;
	}

	private static String generateContentType(int number, String id, String baseTypeId, String[] fileNames, String[] fileExtensions) {
		StringBuilder result = new StringBuilder();
		result.append("<content-type id=\"");
		result.append(id);
		result.append("\" name=\"");
		result.append(id);
		result.append("\" ");
		if (baseTypeId != null) {
			result.append("base-type=\"");
			result.append(baseTypeId);
			result.append("\" ");
		}
		String fileNameList = Util.toListString(fileNames);
		if (fileNameList != null) {
			result.append("file-names=\"");
			result.append(fileNameList);
			result.append("\" ");
		}
		String fileExtensionsList = Util.toListString(fileExtensions);
		if (fileExtensions != null && fileExtensions.length > 0) {
			result.append("file-extensions=\"");
			result.append(fileExtensionsList);
			result.append("\" ");
		}
		result.append("describer=\"");
		result.append(BinarySignatureDescriber.class.getName());
		result.append(":");
		result.append(getSignatureString(number));
		result.append("\"/>");
		return result.toString();
	}

	private static String getContentTypeId(int i) {
		return TEST_DATA_ID + ".performance" + i;
	}

	private static byte[] getSignature(int number) {
		byte[] result = new byte[4];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) ((number >> (i * 8)) & 0xFFL);
		}
		return result;
	}

	private static String getSignatureString(int number) {
		byte[] signature = getSignature(number);
		StringBuilder result = new StringBuilder(signature.length * 3 - 1);
		for (byte element : signature) {
			result.append(Integer.toHexString(0xFF & element));
			result.append(' ');
		}
		result.deleteCharAt(result.length() - 1);
		return result.toString();
	}

	private int countTestContentTypes(IContentType[] all) {
		String namespace = TEST_DATA_ID + '.';
		int count = 0;
		for (IContentType element : all) {
			if (element.getId().startsWith(namespace)) {
				count++;
			}
		}
		return count;
	}

	public static IPath getExtraPluginLocation() {
		return getTempDir().append(TEST_DATA_ID);
	}

	private static Bundle installContentTypes(int numberOfLevels, int nodesPerLevel)
			throws IOException, BundleException {
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME, ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		Bundle installed = null;
		listener.register();
		try {
			IPath pluginLocation = getExtraPluginLocation();
			assertTrue(pluginLocation.toFile().mkdirs());
			assertTrue(pluginLocation.append("META-INF").toFile().mkdirs());
			URL installURL = pluginLocation.toFile().toURI().toURL();
			String eol = System.lineSeparator();
			try (Writer writer = new BufferedWriter(new FileWriter(pluginLocation.append("plugin.xml").toFile()),
					0x10000)) {
				writer.write("<plugin>");
				writer.write(eol);
				writer.write("<extension point=\"org.eclipse.core.runtime.contentTypes\">");
				writer.write(eol);
				String root = createContentType(writer, 0, null);
				createContentTypes(writer, root, 1, numberOfLevels, nodesPerLevel);
				writer.write("</extension></plugin>");
			}
			try (Writer writer = new BufferedWriter(
					new FileWriter(pluginLocation.append("META-INF").append("MANIFEST.MF").toFile()),
					0x10000)) {
				writer.write("Manifest-Version: 1.0");
				writer.write(eol);
				writer.write("Bundle-ManifestVersion: 2");
				writer.write(eol);
				writer.write("Bundle-Name: Content Type Performance Test Data");
				writer.write(eol);
				writer.write("Bundle-SymbolicName: " + TEST_DATA_ID + "; singleton:=true");
				writer.write(eol);
				writer.write("Bundle-Version: 1.0\n");
				writer.write("Require-Bundle: " + PI_RUNTIME_TESTS);
				writer.write(eol);
			}
			installed = RuntimeTestsPlugin.getContext().installBundle(installURL.toExternalForm());
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {installed});
			assertTrue(listener.eventReceived(10000));
		} finally {
			listener.unregister();
		}
		return installed;
	}

	/**
	 * Warms up the content type registry.
	 */
	private void loadChildren() {
		final IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType[] allTypes = manager.getAllContentTypes();
		for (IContentType allType : allTypes) {
			String[] fileNames = allType.getFileSpecs(IContentType.IGNORE_USER_DEFINED | IContentType.FILE_NAME_SPEC);
			for (String fileName : fileNames) {
				manager.findContentTypeFor(fileName);
			}
			String[] fileExtensions = allType.getFileSpecs(IContentType.IGNORE_USER_DEFINED | IContentType.FILE_EXTENSION_SPEC);
			for (String fileExtension : fileExtensions) {
				manager.findContentTypeFor("anyname." + fileExtension);
			}
		}
	}

	/**
	 * Returns a loaded content type manager. Except for load time tests, this method should
	 * be called outside the scope of performance monitoring.
	 */
	private IContentTypeManager loadContentTypeManager() {
		// any cheap interaction that causes the catalog to be built
		Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		return Platform.getContentTypeManager();
	}

	/** Forces all describers to be loaded.*/
	private void loadDescribers() {
		final IContentTypeManager manager = Platform.getContentTypeManager();
		IContentType[] allTypes = manager.getAllContentTypes();
		for (IContentType allType : allTypes) {
			((ContentTypeHandler) allType).getTarget().getDescriber();
		}
	}

	private void loadPreferences() {
		InstanceScope.INSTANCE.getNode(CONTENT_TYPE_PREF_NODE);
	}

	/**
	 * Tests how much the size of the catalog affects the performance of content
	 * type matching by content analysis
	 */
	@PerformanceSessionTest
	@Order(0)
	public void testContentMatching() throws Exception {
		loadPreferences();
		// warm up content type registry
		final IContentTypeManager manager = loadContentTypeManager();
		loadDescribers();
		loadChildren();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int i = 0; i < TOTAL_NUMBER_OF_ELEMENTS; i++) {
					String id = getContentTypeId(i);
					IContentType[] result;
					try {
						result = manager.findContentTypesFor(new ByteArrayInputStream(getSignature(i)), DEFAULT_NAME);
					} catch (IOException e) {
						throw new IllegalStateException("unexpected exception occurred", e);
					}
					assertThat(result).as("content types for element " + i).singleElement()
							.satisfies(it -> assertThat(it.getId()).as("id").isEqualTo(id));
				}
			}
		}.run(getClass(), "testContentMatching", 10, 2);
	}

	@BeforeEach
	void setUp() throws Exception {
		Bundle installed = RuntimeTestsPlugin.getContext()
					.installBundle(getExtraPluginLocation().toFile().toURI().toURL().toExternalForm());
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] { installed });
	}

	@BeforeAll
	static void doSetUp() throws IOException, BundleException {
		installContentTypes(NUMBER_OF_LEVELS, ELEMENTS_PER_LEVEL);
	}

	@AfterAll
	static void doTearDown() {
		clear(getExtraPluginLocation().toFile());
	}

	@PerformanceSessionTest
	@Order(2)
	public void testIsKindOf() throws Exception {
		// warm up preference service
		loadPreferences();
		// warm up content type registry
		final IContentTypeManager manager = loadContentTypeManager();
		loadChildren();
		final IContentType root = manager.getContentType(getContentTypeId(0));
		assertNotNull(root);
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int i = 0; i < TOTAL_NUMBER_OF_ELEMENTS; i++) {
					IContentType type = manager.getContentType(getContentTypeId(i));
					assertNotNull(type, "element is null: " + i);
					assertTrue(type.isKindOf(root), "element is of wrong type: " + i);
				}
			}
		}.run(getClass(), "testIsKindOf", 10, 500);
	}

	/**
	 * This test is intended for running as a session test.
	 */
	@PerformanceSessionTest(repetitions = 10)
	@Order(3)
	public void testLoadCatalog() throws Exception {
		// warm up preference service
		loadPreferences();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void test() {
				// any interation that will cause the registry to be loaded
				Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
			}
		};
		runner.run(getClass(), "testLoadCatalog", 1,
				/* must run only once - the suite controls how many sessions are run */1);
		// sanity check to make sure we are running with good data
		assertEquals(TOTAL_NUMBER_OF_ELEMENTS,
				countTestContentTypes(Platform.getContentTypeManager().getAllContentTypes()), "missing content types");
	}

	/**
	 * Tests how much the size of the catalog affects the performance of content
	 * type matching by name
	 *
	 * @throws Exception
	 */
	@PerformanceSessionTest
	@Order(1)
	public void testNameMatching() throws Exception {
		// warm up preference service
		loadPreferences();
		// warm up content type registry
		final IContentTypeManager manager = loadContentTypeManager();
		loadDescribers();
		loadChildren();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				IContentType[] associated = manager.findContentTypesFor("foo.txt");
				// we know at least the etxt content type should be here
				assertTrue(associated.length >= 1);
				// and it is supposed to be the first one (since it is at the root)
				assertEquals(IContentTypeManager.CT_TEXT, associated[0].getId());
			}
		}.run(getClass(), "testNameMatching", 10, 200000);
	}
}