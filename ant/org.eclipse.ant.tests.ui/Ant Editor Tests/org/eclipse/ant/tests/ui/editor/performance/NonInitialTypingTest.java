/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.performance;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;

/**
 * Measures the time to type in one single target into a large buildfile
 * @since 3.1
 */
public class NonInitialTypingTest extends AbstractAntUITest {
	
	public NonInitialTypingTest(String name) {
		super(name);
	}

//	private ITextEditor fEditor;
//	
//	private static final char[] TARGET= ("<target name=\"newTarget\" >\r" +
//			"<echo>\"New Target\"</echo>\r" +
//			"</target>\r").toCharArray();
//
//	private PerformanceMeter fMeter;
//
//	private KeyboardProbe fKeyboardProbe;
//
//	protected void setUp() throws PartInitException, BadLocationException {
//		EditorTestHelper.runEventQueue();
//		IFile file= getProject().getFolder("buildfiles").getFolder("performance").getFile("build.xml");	
//		fEditor= (ITextEditor) EditorTestHelper.openInEditor(file, true);
//		// dirty editor to avoid initial dirtying / validate edit costs
//		dirtyEditor();
//		Performance performance= Performance.getDefault();
//		fMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
//		fKeyboardProbe= new KeyboardProbe();
//
//		int offset= getInsertPosition();
//		fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
//		EditorTestHelper.runEventQueue();
//		sleep(1000);
//	}
//	
//	private void dirtyEditor() {
//		fEditor.getSelectionProvider().setSelection(new TextSelection(0, 0));
//		EditorTestHelper.runEventQueue();
//		sleep(1000);
//		
//		Display display= EditorTestHelper.getActiveDisplay();
//		fKeyboardProbe.pressChar('{', display);
//		SWTEventHelper.pressKeyCode(display, SWT.BS);
//		sleep(1000);
//	}
//
//	protected void tearDown() throws Exception {
//		sleep(1000);
//		EditorTestHelper.revertEditor(fEditor, true);
//		EditorTestHelper.closeAllEditors();
//		
//		fMeter.commit();
//	}
//
//	public void testTypeAMethod() {
//		Display display= EditorTestHelper.getActiveDisplay();
//		
//		fMeter.start();
//		for (int i= 0; i < TARGET.length; i++) {
//			fKeyboardProbe.pressChar(TARGET[i], display);
//		}
//		fMeter.stop();
//	}
//
//	private synchronized void sleep(int time) {
//		try {
//			wait(time);
//		} catch (InterruptedException e) {
//		}
//	}
//	
//	private int getInsertPosition() throws BadLocationException {
//		IDocument document= EditorTestHelper.getDocument(fEditor);
//		int lines= document.getNumberOfLines();
//		int offset= document.getLineOffset(lines - 2);
//		return offset;
//	}
}
