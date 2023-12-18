/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.team.tests.ui;

import static java.util.Collections.synchronizedList;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.MergeSourceViewer;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.tests.ReflectionUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.mapping.AbstractCompareInput;
import org.eclipse.team.internal.ui.mapping.CompareInputChangeNotifier;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SaveableCompareEditorInputTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final String COMPARE_EDITOR = CompareUIPlugin.PLUGIN_ID
			+ ".CompareEditor"; //$NON-NLS-1$

	private IFile file1;
	private IFile file2;
	private final String appendFileContents = "_append";
	private final String fileContents1 = "FileContents";
	private final String fileContents2 = "FileContents2";
	private final TestLogListener logListener = new TestLogListener();
	private List<IStatus> errorsInListener = synchronizedList(new ArrayList<>());
	private IProject project;

	@Before
	public void setUp() throws Exception {
		project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		file1 = project.getFile("File1.txt");
		file2 = project.getFile("File2.txt");
		createInWorkspace(file1);
		createInWorkspace(file2);
		file1.setContents(createInputStream(fileContents1), true, true, null);
		file2.setContents(createInputStream(fileContents2), true, true, null);

		Platform.addLogListener(logListener);
	}

	@After
	public void tearDown() throws Exception {
		// remove log listener
		Platform.removeLogListener(logListener);
		assertThat("Unexpected errors in log listener", errorsInListener, empty());
	}

	private class TestFileElement implements ITypedElement {

		private final IFile file;

		public IFile getFile() {
			return file;
		}

		public TestFileElement(IFile file) {
			super();
			this.file = file;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getType() {
			return TEXT_TYPE;
		}
	}

	private class TestLogListener implements ILogListener {
		@Override
		public void logging(IStatus status, String plugin) {
			if (status.getSeverity() == IStatus.ERROR) {
				errorsInListener.add(status);
			}
		}
	}

	private class TestDiffNode extends AbstractCompareInput {

		private final CompareInputChangeNotifier notifier = new CompareInputChangeNotifier() {

			private IResource getResource(ITypedElement el) {
				if (el instanceof LocalResourceTypedElement) {
					return ((LocalResourceTypedElement) el).getResource();
				}
				if (el instanceof TestFileElement) {
					return ((TestFileElement) el).getFile();
				}
				return null;
			}

			@Override
			protected IResource[] getResources(ICompareInput input) {

				List resources = new ArrayList();
				if (getResource(getLeft()) != null) {
					resources.add(getResource(getLeft()));
				}
				if (getResource(getRight()) != null) {
					resources.add(getResource(getRight()));
				}
				return (IResource[]) resources.toArray(new IResource[2]);
			}
		};

		public TestDiffNode(ITypedElement left, ITypedElement right) {
			super(Differencer.CHANGE, null, left, right);
		}

		@Override
		public void fireChange() {
			super.fireChange();
		}

		@Override
		protected CompareInputChangeNotifier getChangeNotifier() {
			return notifier;
		}

		@Override
		public boolean needsUpdate() {
			// The remote never changes
			return false;
		}

		@Override
		public void update() {
			fireChange();
		}
	}

	private class TestSaveableEditorInput extends SaveableCompareEditorInput {

		protected ITypedElement left;
		protected ITypedElement right;
		private ICompareInput input;

		@Override
		public Object getCompareResult() {
			return input;
		}

		public TestSaveableEditorInput(ITypedElement left, ITypedElement right,
				CompareConfiguration conf) {
			super(conf, PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage());
			this.left = left;
			this.right = right;
		}

		@Override
		protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			input = createCompareInput();
			getCompareConfiguration().setLeftEditable(true);
			getCompareConfiguration().setRightEditable(false);
			return null;
		}

		private ICompareInput createCompareInput() {
			return new TestDiffNode(left, right);
		}

		@Override
		protected void fireInputChange() {
			((TestDiffNode) getCompareResult()).fireChange();
		}
	}

	private void verifyDirtyStateChanges(
			TestSaveableEditorInput compareEditorInput)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getShell();

		TextMergeViewer viewer = (TextMergeViewer) compareEditorInput
				.findContentViewer(null, compareEditorInput.input, shell);
		viewer.setInput(compareEditorInput.getCompareResult());

		MergeSourceViewer left = (MergeSourceViewer) ReflectionUtils.getField(
				viewer, "fLeft");

		StyledText leftText = left.getSourceViewer().getTextWidget();

		// modify the left side of editor
		leftText.append(appendFileContents);

		assertTrue(compareEditorInput.isDirty());

		// save editor
		viewer.flush(null);

		assertFalse(compareEditorInput.isDirty());
	}

	@Test
	public void testDirtyFlagOnLocalResourceTypedElement()
			throws CoreException, InvocationTargetException,
			InterruptedException, IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException,
			NoSuchMethodException, IOException {

		// Create left element by SaveableCompareEditorInput to be properly
		// saved, see javadoc to SaveableCompareEditorInput
		LocalResourceTypedElement el1 = (LocalResourceTypedElement) SaveableCompareEditorInput
				.createFileElement(file1);
		ITypedElement el2 = new TestFileElement(file2);

		CompareConfiguration conf = new CompareConfiguration();
		conf.setLeftEditable(true);
		TestSaveableEditorInput compareEditorInput = new TestSaveableEditorInput(
				el1, el2, conf);

		compareEditorInput.prepareCompareInput(null);

		verifyDirtyStateChanges(compareEditorInput);

		// check whether file was saved

		assertTrue(compareContent(new ByteArrayInputStream(
				(fileContents1 + appendFileContents).getBytes()),
				file1.getContents()));
	}

	@Test
	public void testDirtyFlagOnCustomTypedElement() throws CoreException,
			InvocationTargetException, InterruptedException,
			IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException,
			NoSuchMethodException, IOException {

		ITypedElement el1 = new TestFileElement(file1);
		ITypedElement el2 = new TestFileElement(file2);

		CompareConfiguration conf = new CompareConfiguration();
		conf.setLeftEditable(true);
		TestSaveableEditorInput compareEditorInput = new TestSaveableEditorInput(
				el1, el2, conf);

		compareEditorInput.prepareCompareInput(null);

		verifyDirtyStateChanges(compareEditorInput);

		/*
		 * not checking if changes were saved because in this case saving is not
		 * handled, see javadoc to SaveableCompareEditorInput.
		 */
	}

	@Test
	public void testDirtyFlagOnLocalResourceTypedElementAndEmptyRight()
			throws CoreException, InvocationTargetException,
			InterruptedException, IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException,
			NoSuchMethodException, IOException {

		// Create left element by SaveableCompareEditorInput to be properly
		// saved, see javadoc to SaveableCompareEditorInput
		LocalResourceTypedElement el1 = (LocalResourceTypedElement) SaveableCompareEditorInput
				.createFileElement(file1);
		ITypedElement el2 = null;

		CompareConfiguration conf = new CompareConfiguration();
		conf.setLeftEditable(true);
		TestSaveableEditorInput compareEditorInput = new TestSaveableEditorInput(
				el1, el2, conf);

		compareEditorInput.prepareCompareInput(null);

		verifyDirtyStateChanges(compareEditorInput);

		// check whether file was saved

		assertTrue(compareContent(new ByteArrayInputStream(
				(fileContents1 + appendFileContents).getBytes()),
				file1.getContents()));
	}

	@Test
	public void testDirtyFlagOnCustomTypedElementAndEmptyRight()
			throws CoreException, InvocationTargetException,
			InterruptedException, IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException,
			NoSuchMethodException, IOException {

		ITypedElement el1 = new TestFileElement(file1);
		ITypedElement el2 = null;

		CompareConfiguration conf = new CompareConfiguration();
		conf.setLeftEditable(true);
		TestSaveableEditorInput compareEditorInput = new TestSaveableEditorInput(
				el1, el2, conf);

		compareEditorInput.prepareCompareInput(null);

		verifyDirtyStateChanges(compareEditorInput);

		/*
		 * not checking if changes were saved because in this case saving is not
		 * handled, see javadoc to SaveableCompareEditorInput.
		 */
	}

	private void verifyModifyAndSaveBothSidesOfCompareEditor(String extention)
			throws InterruptedException, InvocationTargetException,
			IllegalArgumentException, SecurityException,
			IllegalAccessException, NoSuchFieldException, CoreException, IOException {

		// create files to compare
		IFile file1 = project.getFile("CompareFile1." + extention);
		IFile file2 = project.getFile("CompareFile2." + extention);
		file1.create(new ByteArrayInputStream(fileContents1.getBytes()), true,
				null);
		file2.create(new ByteArrayInputStream(fileContents2.getBytes()), true,
				null);

		// prepare comparison
		SaveablesCompareEditorInput input = new SaveablesCompareEditorInput(
				null, SaveablesCompareEditorInput.createFileElement(file1),
				SaveablesCompareEditorInput.createFileElement(file2),
				PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage());
		input.run(null);

		// open CompareEditor
		CompareEditor editor = (CompareEditor) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.openEditor(input, COMPARE_EDITOR, true);

		CompareViewerSwitchingPane pane = (CompareViewerSwitchingPane) ReflectionUtils
				.getField(input, "fContentInputPane", true);

		Viewer viewer = pane.getViewer();

		MergeSourceViewer left = (MergeSourceViewer) ReflectionUtils.getField(
				viewer, "fLeft", true);
		MergeSourceViewer right = (MergeSourceViewer) ReflectionUtils.getField(
				viewer, "fRight", true);

		// modify both sides of CompareEditor
		StyledText leftText = left.getSourceViewer().getTextWidget();
		StyledText rightText = right.getSourceViewer().getTextWidget();
		leftText.append(appendFileContents);
		rightText.append(appendFileContents);

		// save both sides
		editor.doSave(null);

		assertFalse(editor.isDirty());

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.closeEditor(editor, false);

		// validate if both sides where saved
		assertTrue(compareContent(new ByteArrayInputStream(
				(fileContents1 + appendFileContents).getBytes()),
				file1.getContents()));
		assertTrue(compareContent(new ByteArrayInputStream(
				(fileContents2 + appendFileContents).getBytes()),
				file2.getContents()));
	}

	@Test
	public void testModifyAndSaveBothSidesOfCompareEditorHtml() throws Exception {
		verifyModifyAndSaveBothSidesOfCompareEditor("html");
	}

	@Test
	public void testModifyAndSaveBothSidesOfCompareEditorTxt() throws Exception {
		verifyModifyAndSaveBothSidesOfCompareEditor("txt");
	}

	@Test
	public void testModifyAndSaveBothSidesOfCompareEditorJava() throws Exception {
		verifyModifyAndSaveBothSidesOfCompareEditor("java");
	}

	@Test
	public void testModifyAndSaveBothSidesOfCompareEditorXml() throws Exception {
		verifyModifyAndSaveBothSidesOfCompareEditor("xml");
	}

	@Test
	public void testModifyAndSaveBothSidesOfCompareEditorProperties() throws Exception {
		verifyModifyAndSaveBothSidesOfCompareEditor("properties");
	}

}
