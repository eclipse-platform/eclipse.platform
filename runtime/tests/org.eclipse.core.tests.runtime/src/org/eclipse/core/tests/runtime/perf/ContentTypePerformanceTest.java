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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.internal.content.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.harness.*;
import org.eclipse.core.tests.runtime.RuntimeTest;
import org.eclipse.core.tests.runtime.RuntimeTestsPlugin;
import org.eclipse.core.tests.session.PerformanceSessionTestSuite;
import org.eclipse.core.tests.session.SessionTestSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class ContentTypePerformanceTest extends RuntimeTest {

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

	public static Test suite() {
		TestSuite suite = new TestSuite(ContentTypePerformanceTest.class.getName());

		//		suite.addTest(new ContentTypePerformanceTest("testDoSetUp"));
		//		suite.addTest(new ContentTypePerformanceTest("testContentMatching"));
		//		suite.addTest(new ContentTypePerformanceTest("testContentTXTMatching"));
		//		suite.addTest(new ContentTypePerformanceTest("testContentXMLMatching"));
		//		suite.addTest(new ContentTypePerformanceTest("testDoTearDown"));

		SessionTestSuite setUp = new SessionTestSuite(PI_RUNTIME_TESTS, "testDoSetUp");
		setUp.addTest(new ContentTypePerformanceTest("testDoSetUp"));
		suite.addTest(setUp);

		TestSuite singleRun = new PerformanceSessionTestSuite(PI_RUNTIME_TESTS, 1, "singleSessionTests");
		singleRun.addTest(new ContentTypePerformanceTest("testContentMatching"));
		singleRun.addTest(new ContentTypePerformanceTest("testNameMatching"));
		singleRun.addTest(new ContentTypePerformanceTest("testIsKindOf"));
		suite.addTest(singleRun);

		TestSuite loadCatalog = new PerformanceSessionTestSuite(PI_RUNTIME_TESTS, 10, "multipleSessionTests");
		loadCatalog.addTest(new ContentTypePerformanceTest("testLoadCatalog"));
		suite.addTest(loadCatalog);

		TestSuite tearDown = new SessionTestSuite(PI_RUNTIME_TESTS, "testDoTearDown");
		tearDown.addTest(new ContentTypePerformanceTest("testDoTearDown"));
		suite.addTest(tearDown);
		return suite;
	}

	public ContentTypePerformanceTest(String name) {
		super(name);
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

	public IPath getExtraPluginLocation() {
		return getTempDir().append(TEST_DATA_ID);
	}

	private Bundle installContentTypes(String tag, int numberOfLevels, int nodesPerLevel) {
		TestRegistryChangeListener listener = new TestRegistryChangeListener(Platform.PI_RUNTIME, ContentTypeBuilder.PT_CONTENTTYPES, null, null);
		Bundle installed = null;
		listener.register();
		try {
			IPath pluginLocation = getExtraPluginLocation();
			assertTrue(pluginLocation.toFile().mkdirs());
			assertTrue(pluginLocation.append("META-INF").toFile().mkdirs());
			URL installURL = null;
			try {
				installURL = pluginLocation.toFile().toURI().toURL();
			} catch (MalformedURLException e) {
				fail(tag + ".0.5", e);
			}
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
			} catch (IOException e) {
				fail(tag + ".1.0", e);
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
			} catch (IOException e) {
				fail(tag + ".2.0", e);
			}
			try {
				installed = RuntimeTestsPlugin.getContext().installBundle(installURL.toExternalForm());
			} catch (BundleException e) {
				fail(tag + ".3.0", e);
			}
			BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] {installed});
			assertTrue(tag + ".4.0", listener.eventReceived(10000));
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

	/** Tests how much the size of the catalog affects the performance of content type matching by content analysis */
	public void testContentMatching() {
		loadPreferences();
		// warm up content type registry
		final IContentTypeManager manager = loadContentTypeManager();
		loadDescribers();
		loadChildren();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					for (int i = 0; i < TOTAL_NUMBER_OF_ELEMENTS; i++) {
						String id = getContentTypeId(i);
						IContentType[] result = manager.findContentTypesFor(new ByteArrayInputStream(getSignature(i)), DEFAULT_NAME);
						assertEquals("1.0." + i, 1, result.length);
						assertEquals("1.1." + i, id, result[0].getId());
					}
				} catch (IOException e) {
					fail("2.0", e);
				}
			}
		}.run(this, 10, 2);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (getName().equals("testDoSetUp") || getName().equals("testDoTearDown")) {
			return;
		}
		Bundle installed = null;
		try {
			installed = RuntimeTestsPlugin.getContext()
					.installBundle(getExtraPluginLocation().toFile().toURI().toURL().toExternalForm());
		} catch (BundleException e) {
			fail("1.0", e);
		} catch (MalformedURLException e) {
			fail("2.0", e);
		}
		BundleTestingHelper.refreshPackages(RuntimeTestsPlugin.getContext(), new Bundle[] { installed });
	}

	public void testDoSetUp() {
		installContentTypes("1.0", NUMBER_OF_LEVELS, ELEMENTS_PER_LEVEL);
	}

	public void testDoTearDown() {
		ensureDoesNotExistInFileSystem(getExtraPluginLocation().toFile());
	}

	public void testIsKindOf() {
		// warm up preference service
		loadPreferences();
		// warm up content type registry
		final IContentTypeManager manager = loadContentTypeManager();
		loadChildren();
		final IContentType root = manager.getContentType(getContentTypeId(0));
		assertNotNull("2.0", root);
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				for (int i = 0; i < TOTAL_NUMBER_OF_ELEMENTS; i++) {
					IContentType type = manager.getContentType(getContentTypeId(i));
					assertNotNull("3.0." + i, type);
					assertTrue("3.1." + i, type.isKindOf(root));
				}
			}
		}.run(this, 10, 500);
	}

	/**
	 * This test is intended for running as a session test.
	 */
	public void testLoadCatalog() {
		// warm up preference service
		loadPreferences();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void test() {
				// any interation that will cause the registry to be loaded
				Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
			}
		};
		runner.run(this, 1, /* must run only once - the suite controls how many sessions are run */1);
		// sanity check to make sure we are running with good data
		assertEquals("missing content types", TOTAL_NUMBER_OF_ELEMENTS, countTestContentTypes(Platform.getContentTypeManager().getAllContentTypes()));
	}

	/** Tests how much the size of the catalog affects the performance of content type matching by name */
	public void testNameMatching() {
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
				assertTrue("2.0", associated.length >= 1);
				// and it is supposed to be the first one (since it is at the root)
				assertEquals("2.1", IContentTypeManager.CT_TEXT, associated[0].getId());
			}
		}.run(this, 10, 200000);
	}
}