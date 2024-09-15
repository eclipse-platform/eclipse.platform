/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.propertytester;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.core.internal.propertytester.FilePropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.content.IContentTypeManagerTest;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(WorkspaceResetExtension.class)
public class FilePropertyTesterTest {

	private static final String CONTENT_TYPE_ID = "contentTypeId";
	private static final String IS_KIND_OF = "kindOf";
	private static final String USE_FILENAME_ONLY = "useFilenameOnly";

	private FilePropertyTester tester = null;
	private IProject project = null;

	@BeforeEach
	public void setUp() throws CoreException {
		project = getWorkspace().getRoot().getProject("project1");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		tester = new FilePropertyTester();
	}

	private static Stream<List<String>> arguments() {
		return Stream.of( //
				List.of(), //
				List.of(IS_KIND_OF), //
				List.of(USE_FILENAME_ONLY), //
				List.of(IS_KIND_OF, USE_FILENAME_ONLY) //
		);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void testNonExistingTextFile(List<String> arguments) throws Throwable {
		String expected = "org.eclipse.core.runtime.text";
		IFile target = project.getFile("tmp.txt");

		boolean testResult = tester.test(target, CONTENT_TYPE_ID, arguments.toArray(), expected);
		assertEquals(arguments.contains(USE_FILENAME_ONLY), testResult);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void testExistingTextFile(List<String> arguments) throws Throwable {
		String expected = "org.eclipse.core.runtime.text";
		IFile target = project.getFile("tmp.txt");
		target.create(createRandomContentsStream(), true, createTestMonitor());

		assertTrue(tester.test(target, CONTENT_TYPE_ID, arguments.toArray(), expected));
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void testNonExistingNsRootElementFile(List<String> arguments) throws Throwable {
		String expectedBase = "org.eclipse.core.runtime.xml";
		String expectedExact = "org.eclipse.core.tests.resources.ns-root-element";
		String expected = arguments.isEmpty() ? expectedExact : expectedBase;
		IFile target = project.getFile("tmp.xml");

		boolean testResult = tester.test(target, CONTENT_TYPE_ID, arguments.toArray(), expected);
		assertEquals(arguments.contains(USE_FILENAME_ONLY), testResult);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	public void testExistingNsRootElementFile(List<String> arguments) throws Throwable {
		String expectedBase = "org.eclipse.core.runtime.xml";
		String expectedExact = "org.eclipse.core.tests.resources.ns-root-element";
		String expected = arguments.isEmpty() ? expectedExact : expectedBase;
		IFile target = project.getFile("tmp.xml");
		byte[] bytes = IContentTypeManagerTest.XML_ROOT_ELEMENT_NS_MATCH1.getBytes(StandardCharsets.UTF_8);
		target.create(new ByteArrayInputStream(bytes), true, createTestMonitor());

		assertTrue(tester.test(target, CONTENT_TYPE_ID, arguments.toArray(), expected));
	}

}
