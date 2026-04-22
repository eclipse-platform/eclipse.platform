/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.llm.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.eclipse.core.llm.LlmConfigurationDialog;
import org.eclipse.core.llm.LlmModel;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.Test;

public class LlmConfigurationDialogTest {

	@Test
	void okBuildsConfiguredModel() throws Exception {
		runOnUi(display -> {
			Shell parent = new Shell(display);
			try {
				LlmConfigurationDialog dlg = new LlmConfigurationDialog(parent,
						new LlmModel("http://orig/", "orig-model")); //$NON-NLS-1$ //$NON-NLS-2$
				dlg.setBlockOnOpen(false);
				dlg.open();
				setText(dlg, "urlText", "http://new/"); //$NON-NLS-1$ //$NON-NLS-2$
				setText(dlg, "modelText", "new-model"); //$NON-NLS-1$ //$NON-NLS-2$
				pressButton(dlg, org.eclipse.jface.dialogs.IDialogConstants.OK_ID);
				LlmModel m = dlg.getModel();
				assertEquals("http://new/", m.url()); //$NON-NLS-1$
				assertEquals("new-model", m.model()); //$NON-NLS-1$
			} finally {
				parent.dispose();
			}
		});
	}

	@Test
	void cancelYieldsNoModel() throws Exception {
		runOnUi(display -> {
			Shell parent = new Shell(display);
			try {
				LlmConfigurationDialog dlg = new LlmConfigurationDialog(parent, null);
				dlg.setBlockOnOpen(false);
				dlg.open();
				dlg.close();
				assertNull(dlg.getModel());
				assertEquals(Window.CANCEL, dlg.getReturnCode());
			} finally {
				parent.dispose();
			}
		});
	}

	private static void setText(Object target, String fieldName, String value) throws Exception {
		Field f = target.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		((Text) f.get(target)).setText(value);
	}

	private static void pressButton(Object dialog, int buttonId) throws Exception {
		java.lang.reflect.Method m = org.eclipse.jface.dialogs.Dialog.class
				.getDeclaredMethod("buttonPressed", int.class); //$NON-NLS-1$
		m.setAccessible(true);
		m.invoke(dialog, buttonId);
	}

	private interface UiRunnable {
		void run(Display display) throws Exception;
	}

	private static void runOnUi(UiRunnable r) throws Exception {
		Display display = Display.getDefault();
		Throwable[] err = new Throwable[1];
		display.syncExec(() -> {
			try {
				r.run(display);
			} catch (Throwable t) {
				err[0] = t;
			}
		});
		if (err[0] instanceof Exception e) {
			throw e;
		}
		if (err[0] != null) {
			throw new RuntimeException(err[0]);
		}
	}
}
