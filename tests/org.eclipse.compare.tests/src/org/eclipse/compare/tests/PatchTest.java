/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.compare.internal.core.patch.*;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.patch.*;
import org.eclipse.compare.tests.PatchUtils.*;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.junit.Assert;
import org.junit.Test;

import junit.framework.AssertionFailedError;

public class PatchTest {

	private static final String PATCH_CONFIGURATION = "patchConfiguration.properties";

	Properties defaultPatchProperties;

	public PatchTest() {
		defaultPatchProperties = new Properties();
		defaultPatchProperties.setProperty("patchFile", "patch.txt");
		defaultPatchProperties.setProperty("contextFile", "context.txt");
		defaultPatchProperties.setProperty("expectedResultFile", "exp_context.txt");
		defaultPatchProperties.setProperty("fuzzFactor", "-1");
	}

	@Test
	public void testCreatePatch() throws CoreException, IOException {
		patch("addition.txt", "patch_addition.txt", "exp_addition.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testUnterminatedCreatePatch() throws CoreException, IOException {
		patch("addition.txt", "patch_addition2.txt", "exp_addition2.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testContext0Patch() throws CoreException, IOException {
		patch("context.txt", "patch_context0.txt", "exp_context.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testContext1Patch() throws CoreException, IOException {
		patch("context.txt", "patch_context1.txt", "exp_context.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testContext3Patch() throws CoreException, IOException {
		patch("context.txt", "patch_context3.txt", "exp_context.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testHunkFilter() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_hunkFilter.txt");
		IStorage expStorage = new StringStorage("context.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(1, patches.length);
		IHunk[] hunks = patches[0].getHunks();
		assertEquals(5, hunks.length);
		PatchConfiguration pc = new PatchConfiguration();
		final IHunk toFilterOut = hunks[3];
		pc.addHunkFilter(hunk -> hunk != toFilterOut);
		IFilePatchResult result = patches[0].apply(expStorage, pc, new NullProgressMonitor());
		IHunk[] rejects = result.getRejects();
		assertEquals(2, rejects.length);
		boolean aFiltered = pc.getHunkFilters()[0].select(rejects[0]);
		boolean bFiltered = pc.getHunkFilters()[0].select(rejects[1]);
		assertTrue((aFiltered && !bFiltered) || (!aFiltered && bFiltered));

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_hunkFilter.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertEquals(expected, PatchUtils.asString(actual));
	}

	@Test
	public void testContext3PatchWithHeader() throws CoreException, IOException {
		patch("context.txt", "patch_context3_header.txt", "exp_context.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IStorage patchStorage = new StringStorage("patch_context3_header.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		String header = patches[0].getHeader();
		LineReader reader = new LineReader(
				new BufferedReader(new InputStreamReader(new ByteArrayInputStream(header.getBytes()))));
		List<String> lines = reader.readLines();
		List<String> expected = new ArrayList<>();
		expected.add("Index: old.txt\n");
		expected.add("UID: 42\n");
		assertEquals(LineReader.createString(false, expected), LineReader.createString(false, lines));
	}

	@Test
	public void testDateUnknown() throws CoreException {
		IStorage patchStorage = new StringStorage("patch_dateunknown.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(IFilePatch.DATE_UNKNOWN, patches[0].getBeforeDate());
		assertEquals(IFilePatch.DATE_UNKNOWN, patches[0].getAfterDate());
	}

	@Test
	public void testDateError() throws CoreException {
		IStorage patchStorage = new StringStorage("patch_dateerror.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(IFilePatch.DATE_UNKNOWN, patches[0].getBeforeDate());
		assertEquals(IFilePatch.DATE_UNKNOWN, patches[0].getAfterDate());
	}

	@Test
	public void testDateKnown() throws CoreException {
		IStorage patchStorage = new StringStorage("patch_datevalid.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertFalse(IFilePatch.DATE_UNKNOWN == patches[0].getBeforeDate());
		assertFalse(IFilePatch.DATE_UNKNOWN == patches[0].getAfterDate());
	}

	// Test creation of new workspace patch
	@Test
	public void testWorkspacePatch_Create() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(new String[] { "addition.txt", "addition.txt" }, "patch_workspacePatchAddition.txt", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				new String[] { "exp_workspacePatchAddition2.txt", "exp_workspacePatchAddition.txt" }, false, 0); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// Test applying the reverse of workspace creation patch
	@Test
	public void testWorkspacePatch_Create_Reverse() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(new String[] { "exp_workspacePatchAddition2.txt", "exp_workspacePatchAddition.txt" }, //$NON-NLS-1$//$NON-NLS-2$
				"patch_workspacePatchAddition.txt", new String[] { "addition.txt", "addition.txt" }, true, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	// Test the patching of an already existing file, the creation of a new one and
	// the deletion of elements in a file
	@Test
	public void testWorkspacePatch_Modify() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(
				new String[] { "exp_workspacePatchAddition2.txt", "exp_workspacePatchAddition.txt", "addition.txt" }, //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				"patch_workspacePatchMod.txt", new String[] { "exp_workspacePatchMod1.txt", //$NON-NLS-1$ //$NON-NLS-2$
						"exp_workspacePatchMod2.txt", "exp_workspacePatchMod3.txt" }, //$NON-NLS-1$ //$NON-NLS-2$
				false, 0);
	}

	// Test applying the reverse of a workspace modify patch
	@Test
	public void testWorkspacePatch_Modify_Reverse() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(
				new String[] { "exp_workspacePatchMod1.txt", "exp_workspacePatchMod2.txt", //$NON-NLS-1$//$NON-NLS-2$
						"exp_workspacePatchMod3.txt" }, //$NON-NLS-1$
				"patch_workspacePatchMod.txt", //$NON-NLS-1$
				new String[] { "exp_workspacePatchAddition2.txt", "exp_workspacePatchAddition.txt", "addition.txt" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				true, 0);
	}

	// Tests the deletion of an already existing file, and the modification of
	// another file
	@Test
	public void testWorkspacePatch_Delete() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(
				new String[] { "exp_workspacePatchMod2.txt", "addition.txt", "exp_workspacePatchMod1.txt", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						"addition.txt" }, //$NON-NLS-1$
				"patch_workspacePatchDelete.txt", new String[] { "addition.txt", "exp_workspacePatchDelete2.txt", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"addition.txt", "exp_workspacePatchDelete1.txt" }, //$NON-NLS-1$ //$NON-NLS-2$
				false, 0);
	}

	// Test applying the reverse of a workspace deletion patch
	@Test
	public void testWorkspacePatch_Delete_Reverse() {
		// Note the order that exists in the array of expected results is based purely
		// on the order of the files in the patch
		patchWorkspace(
				new String[] { "addition.txt", "exp_workspacePatchDelete2.txt", "addition.txt", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						"exp_workspacePatchDelete1.txt" }, //$NON-NLS-1$
				"patch_workspacePatchDelete.txt", new String[] { "exp_workspacePatchMod2.txt", "addition.txt", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"exp_workspacePatchMod1.txt", "addition.txt" }, //$NON-NLS-1$ //$NON-NLS-2$
				true, 0);
	}

	// Keeps track of the failures
	private List<AssertionError> failures = new ArrayList<>();

	@Test
	public void testPatchdataSubfolders() throws IOException, CoreException {
		URL patchdataUrl = new URL(PatchUtils.getBundle().getEntry("/"), new Path(PatchUtils.PATCHDATA).toString());
		patchdataUrl = FileLocator.resolve(patchdataUrl);

		Map<String, PatchTestConfiguration> map = null;
		switch (patchdataUrl.getProtocol()) {
		case "file":
			map = extractNamesForFileProtocol(patchdataUrl);
			break;
		case "jar":
			map = extractNamesForJarProtocol(patchdataUrl);
			break;
		default:
			fail("Unknown protocol");
			break;
		}
		assertNotNull(map);

		for (Entry<String, PatchTestConfiguration> entry : map.entrySet()) {
			String sf = entry.getKey(); // subfolder
			PatchTestConfiguration ptc = entry.getValue();
			String[] originalFiles = ptc.originalFileNames;
			String patch = ptc.patchFileName;
			String[] expectedFiles = ptc.expectedFileNames;
			String[] actualFiles = ptc.actualFileNames;
			PatchConfiguration pc = ptc.pc;

			// create a message to distinguish tests from different subfolders
			String msg = "Test for subfolder [" + PatchUtils.PATCHDATA + "/" + sf + "] failed.";

			try {
				// test with expected result
				patchWorkspace(msg, originalFiles, patch, expectedFiles, pc);
			} catch (AssertionError e) {
				failures.add(e);
			}

			// test with actual result, should fail
			if (actualFiles != null) {
				try {
					patchWorkspace(msg, originalFiles, patch, actualFiles, pc);
				} catch (AssertionError e) {
					// a failure is expected
					continue; // continue with a next subfolder
				}
				failures.add(new AssertionError(
						"\npatchWorkspace should fail for folder [" + PatchUtils.PATCHDATA + "/" + sf + "]."));
			}
		}

		if (failures.isEmpty())
			return;

		if (failures.size() == 1)
			throw failures.get(0);

		StringBuilder sb = new StringBuilder(
				"Failures occured while testing data from patchdata subfolder (Please check log for further details):");
		for (AssertionError error : failures) {
			log("org.eclipse.compare.tests", error);
			sb.append("\n" + error.getMessage());
		}
		throw new AssertionFailedError(sb.toString());
	}

	// both copy-pasted from CoreTest

	private void log(String pluginID, IStatus status) {
		Platform.getLog(Platform.getBundle(pluginID)).log(status);
	}

	private void log(String pluginID, Throwable e) {
		log(pluginID, new Status(IStatus.ERROR, pluginID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	/**
	 * @param patchdataUrl
	 * @return A map with subfolder name as a key and an array of objects as a
	 *         value. The first object in the array is another array (of Strings)
	 *         containing file names for the test. The last value in this array can
	 *         be <code>null</code> as testing against actual result is optional.
	 *         The second object is an instance of <code>PatchConfiguration</code>
	 *         class.
	 * @throws IOException
	 * @throws CoreException
	 */
	private Map<String, PatchTestConfiguration> extractNamesForJarProtocol(URL patchdataUrl)
			throws IOException, CoreException {
		JarFile jarFile = ((JarURLConnection) patchdataUrl.openConnection()).getJarFile();

		// look for the patchdata folder entry
		String patchdataName = null;
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith("/" + PatchUtils.PATCHDATA + "/")) {
				patchdataName = entryName;
				break;
			}
		}
		// patchdata folder not found
		if (patchdataName == null)
			return null;

		Map<String, PatchTestConfiguration> result = new HashMap<>();
		entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryName = entry.getName();
			if (entry.isDirectory()) {
				if (!entryName.equals(patchdataName) && entryName.startsWith(patchdataName)) {
					// a subfolder found
					ZipEntry patchConf = jarFile.getEntry(entryName + "/" + PATCH_CONFIGURATION);
					if (patchConf != null) {
						JarEntryStorage jes = new JarEntryStorage(entry, jarFile);
						Properties properties = new Properties();
						try {
							properties.load(jes.getContents());
						} catch (IOException e) {
							fail("IOException occured while loading the Patch Configuration file for "
									+ entryName.toString());
						}
						processProperties(result, properties, entryName);
					} else {
						processProperties(result, defaultPatchProperties, entryName);
					}
				}
			}
		}
		return result;
	}

	private Map<String, PatchTestConfiguration> extractNamesForFileProtocol(URL patchdataUrl) throws CoreException {

		Map<String, PatchTestConfiguration> result = new HashMap<>(); // configuration map

		IPath patchdataFolderPath = new Path(patchdataUrl.getPath());
		File patchdataFolderFile = patchdataFolderPath.toFile();
		assertTrue(patchdataFolderFile.isDirectory());
		File[] listOfSubfolders = patchdataFolderFile.listFiles((FileFilter) File::isDirectory);
		for (File subfolder : listOfSubfolders) {
			Path pcPath = new Path(subfolder.getPath() + "/" + PATCH_CONFIGURATION);
			File pcFile = pcPath.toFile();

			if (subfolder.getName().equals("CVS"))
				continue;
			if (pcFile.exists()) {
				Properties properties = new Properties();
				try {
					properties.load(new FileInputStream(pcFile));
				} catch (IOException e) {
					fail("IOException occured while loading the Patch Configuration file for " + subfolder.toString());
				}
				processProperties(result, properties, subfolder.getName());
			} else {
				processProperties(result, defaultPatchProperties, subfolder.getName());
			}
		}
		return result;
	}

	private void processProperties(Map<String, PatchTestConfiguration> result, Properties p, String subfolderName) {
		boolean skipTest = Boolean.valueOf(p.getProperty("skipTest", "false")).booleanValue();
		if (skipTest)
			return;
		String pf = p.getProperty("patchFile", "patch.txt");
		String[] cf = p.getProperty("contextFile", "context.txt").split(",");
		String[] erf = p.getProperty("expectedResultFile", "exp_context").split(",");
		// optional, can't guess the file name here, it might left empty intentionally
		String[] arf = null;
		String arfp = p.getProperty("actualResultFile", null);
		if (arfp != null)
			arf = arfp.split(",");
		int fuzzFactor = Integer.parseInt(p.getProperty("fuzzFactor", "0"));
		boolean ignoreWhitespace = Boolean.valueOf(p.getProperty("ignoreWhitespace", "false")).booleanValue();
		int prefixSegmentStrip = Integer.parseInt(p.getProperty("prefixSegmentStrip", "0"));
		boolean reversed = Boolean.valueOf(p.getProperty("reversed", "false")).booleanValue();

		PatchConfiguration pc = new PatchConfiguration();
		pc.setFuzz(fuzzFactor);
		pc.setIgnoreWhitespace(ignoreWhitespace);
		pc.setPrefixSegmentStripCount(prefixSegmentStrip);
		pc.setReversed(reversed);

		// make the paths relative
		pf = subfolderName + "/" + pf;
		for (int i = 0; i < cf.length; i++) {
			cf[i] = subfolderName + "/" + cf[i];
		}
		for (int i = 0; i < erf.length; i++) {
			erf[i] = subfolderName + "/" + erf[i];
		}
		if (arf != null) { // optional
			for (int i = 0; i < arf.length; i++) {
				arf[i] = subfolderName + "/" + arf[i];
			}
		}

		PatchTestConfiguration tpc = new PatchTestConfiguration();
		tpc.originalFileNames = cf;
		tpc.patchFileName = pf;
		tpc.subfolderName = subfolderName;
		tpc.expectedFileNames = erf;
		tpc.actualFileNames = arf;
		tpc.pc = pc;

		result.put(subfolderName, tpc);
	}

	private void patch(final String old, String patch, String expt) throws CoreException, IOException {
		patcherPatch(old, patch, expt);
		filePatch(old, patch, expt);
	}

	private void filePatch(final String old, String patch, String expt) throws CoreException, IOException {
		LineReader lr = new LineReader(PatchUtils.getReader(expt));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		IStorage oldStorage = new StringStorage(old);
		IStorage patchStorage = new StringStorage(patch);
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertTrue(patches.length == 1);
		IFilePatchResult result = patches[0].apply(oldStorage, new PatchConfiguration(), null);
		assertTrue(result.hasMatches());
		assertFalse(result.hasRejects());
		InputStream actualStream = result.getPatchedContents();
		String actual = PatchUtils.asString(actualStream);
		assertEquals(expected, actual);
	}

	private void patcherPatch(String old, String patch, String expt) {
		LineReader lr = new LineReader(PatchUtils.getReader(old));
		List<String> inLines = lr.readLines();

		WorkspacePatcher patcher = new WorkspacePatcher();
		try {
			patcher.parse(PatchUtils.getReader(patch));
		} catch (IOException e) {
			e.printStackTrace();
		}

		FilePatch2[] diffs = patcher.getDiffs();
		Assert.assertEquals(diffs.length, 1);

		FileDiffResult diffResult = patcher.getDiffResult(diffs[0]);
		diffResult.patch(inLines, null);

		LineReader expectedContents = new LineReader(PatchUtils.getReader(expt));
		List<String> expectedLines = expectedContents.readLines();

		Assert.assertArrayEquals(expectedLines.toArray(), inLines.toArray());
	}

	private void patchWorkspace(String[] originalFiles, String patch, String[] expectedOutcomeFiles, boolean reverse,
			int fuzzFactor) {
		PatchConfiguration pc = new PatchConfiguration();
		pc.setReversed(reverse);
		pc.setFuzz(fuzzFactor);
		patchWorkspace(null, originalFiles, patch, expectedOutcomeFiles, pc);
	}

	/**
	 * Parses a workspace patch and applies the diffs to the appropriate files
	 *
	 * @param msg
	 * @param originalFiles
	 * @param patch
	 * @param expectedOutcomeFiles
	 * @param patchConfiguration   The patch configuration to use. One of its
	 *                             parameters is fuzz factor. If it equals
	 *                             <code>-1</code> it means that the fuzz should be
	 *                             calculated automatically.
	 */
	private void patchWorkspace(String msg, String[] originalFiles, String patch, String[] expectedOutcomeFiles,
			PatchConfiguration patchConfiguration) {

		// ensure that we have the same number of input files as we have expected files
		Assert.assertEquals(originalFiles.length, expectedOutcomeFiles.length);

		// Parse the passed in patch and extract all the Diffs
		WorkspacePatcher patcher = new WorkspacePatcher();
		try {
			patcher.getConfiguration().setFuzz(patchConfiguration.getFuzz());
			patcher.getConfiguration().setIgnoreWhitespace(patchConfiguration.isIgnoreWhitespace());
			patcher.getConfiguration().setPrefixSegmentStripCount(patchConfiguration.getPrefixSegmentStripCount());
			patcher.parse(PatchUtils.getReader(patch));
			patcher.setReversed(patchConfiguration.isReversed());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Sort the diffs by project
		FilePatch2[] diffs = patcher.getDiffs();

		// Iterate through all of the original files, apply the diffs that belong to the
		// file and compare
		// with the corresponding outcome file
		for (int i = 0; i < originalFiles.length; i++) {
			LineReader lr = new LineReader(PatchUtils.getReader(originalFiles[i]));
			List<String> inLines = lr.readLines();

			FileDiffResult diffResult = patcher.getDiffResult(diffs[i]);
			diffResult.patch(inLines, null);

			LineReader expectedContents = new LineReader(PatchUtils.getReader(expectedOutcomeFiles[i]));
			List<String> expectedLines = expectedContents.readLines();

			Object[] expected = expectedLines.toArray();

			String resultString = LineReader.createString(patcher.isPreserveLineDelimeters(), inLines);
			LineReader resultReader = new LineReader(new BufferedReader(new StringReader(resultString)));
			Object[] result = resultReader.readLines().toArray();

			Assert.assertArrayEquals(msg, expected, result);
		}
	}
}
