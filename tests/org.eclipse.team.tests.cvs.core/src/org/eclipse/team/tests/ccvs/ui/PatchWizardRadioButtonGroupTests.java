/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;

public class PatchWizardRadioButtonGroupTests extends TestCase {

	private MyRadioButtonGroup group;
	private Button button1;
	private Button button2;
	private Button button3;

	private static final int FORMAT_UNIFIED = getFieldValue("FORMAT_UNIFIED");
	private static final int FORMAT_CONTEXT = getFieldValue("FORMAT_CONTEXT");
	private static final int FORMAT_STANDARD = getFieldValue("FORMAT_STANDARD");

	public PatchWizardRadioButtonGroupTests() {
		super();
	}

	public PatchWizardRadioButtonGroupTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(PatchWizardRadioButtonGroupTests.class
				.getName());
		suite.addTest(new PatchWizardRadioButtonGroupTests(
				"testSingleSelection"));
		suite.addTest(new PatchWizardRadioButtonGroupTests(
				"testMultipleSelection"));
		suite.addTest(new PatchWizardRadioButtonGroupTests(
				"testSelectDisabled1"));
		suite.addTest(new PatchWizardRadioButtonGroupTests(
				"testSelectDisabled2"));
		suite.addTest(new PatchWizardRadioButtonGroupTests(
				"testSelectDisabled3"));
		suite
				.addTest(new PatchWizardRadioButtonGroupTests(
						"testSetEnablement"));
		return suite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.tests.ccvs.core.EclipseTest#setUp()
	 */
	protected void setUp() throws Exception {
		Shell shell = new Shell(Display.getCurrent());
		Composite composite = new Composite(shell, SWT.NONE);
		group = new MyRadioButtonGroup();
		button1 = new Button(composite, SWT.RADIO);
		button2 = new Button(composite, SWT.RADIO);
		button3 = new Button(composite, SWT.RADIO);
	}

	private Button getButtonForCode(int code) {
		if (code == getFieldValue("FORMAT_UNIFIED"))
			return button1;
		else if (code == getFieldValue("FORMAT_CONTEXT"))
			return button2;
		else if (code == getFieldValue("FORMAT_STANDARD"))
			return button3;
		else
			fail();
		return null;
	}

	public void testSingleSelection() throws Exception {
		button1.setEnabled(true);
		button1.setSelection(true);
		group.add(FORMAT_UNIFIED, button1);

		button2.setEnabled(true);
		button2.setSelection(false);
		group.add(FORMAT_CONTEXT, button2);

		button3.setEnabled(true);
		button3.setSelection(false);
		group.add(FORMAT_STANDARD, button3);

		assertEquals(group.getSelected(), FORMAT_UNIFIED);

		assertEquals(true, button1.getSelection());
		assertEquals(false, button2.getSelection());
		assertEquals(false, button3.getSelection());
	}

	public void testMultipleSelection() throws Exception {
		button1.setEnabled(true);
		button1.setSelection(true);
		group.add(FORMAT_UNIFIED, button1);

		button2.setEnabled(true);
		button2.setSelection(true);
		group.add(FORMAT_CONTEXT, button2);

		button3.setEnabled(true);
		button3.setSelection(true);
		group.add(FORMAT_STANDARD, button3);

		assertEquals(FORMAT_STANDARD, group.getSelected());

		assertEquals(false, button1.getSelection());
		assertEquals(false, button2.getSelection());
		assertEquals(true, button3.getSelection());
	}

	public void testSelectDisabled1() throws Exception {
		button1.setEnabled(true);
		button1.setSelection(false);
		group.add(FORMAT_UNIFIED, button1);

		button2.setEnabled(false);
		button2.setSelection(true);
		group.add(FORMAT_CONTEXT, button2);

		button3.setEnabled(true);
		button3.setSelection(false);
		group.add(FORMAT_STANDARD, button3);

		group.selectEnabledOnly();

		assertFalse(group.getSelected() == FORMAT_CONTEXT);

		assertTrue(getButtonForCode(group.getSelected()).isEnabled());
		assertTrue(getButtonForCode(group.getSelected()).getSelection());
	}

	public void testSelectDisabled2() throws Exception {
		button1.setSelection(false);
		group.add(FORMAT_UNIFIED, button1);

		button2.setSelection(true);
		group.add(FORMAT_CONTEXT, button2);

		button3.setSelection(false);
		group.add(FORMAT_STANDARD, button3);

		group.setEnablement(false,
				new int[] { FORMAT_UNIFIED, FORMAT_CONTEXT }, FORMAT_STANDARD);

		assertEquals(FORMAT_STANDARD, group.getSelected());

		assertEquals(false, button1.getSelection());
		assertEquals(false, button2.getSelection());
		assertEquals(true, button3.getSelection());

		assertEquals(false, button1.isEnabled());
		assertEquals(false, button2.isEnabled());
		assertEquals(true, button3.isEnabled());

		assertTrue(getButtonForCode(group.getSelected()).isEnabled());
		assertTrue(getButtonForCode(group.getSelected()).getSelection());
	}

	public void testSelectDisabled3() throws Exception {
		button1.setSelection(true);
		group.add(FORMAT_UNIFIED, button1);

		button2.setSelection(false);
		group.add(FORMAT_CONTEXT, button2);

		button3.setSelection(false);
		group.add(FORMAT_STANDARD, button3);

		group
				.setEnablement(false, new int[] { FORMAT_UNIFIED,
						FORMAT_CONTEXT });

		assertEquals(FORMAT_STANDARD, group.getSelected());

		assertEquals(false, button1.getSelection());
		assertEquals(false, button2.getSelection());
		assertEquals(true, button3.getSelection());

		assertEquals(false, button1.isEnabled());
		assertEquals(false, button2.isEnabled());
		assertEquals(true, button3.isEnabled());

		assertTrue(getButtonForCode(group.getSelected()).isEnabled());
		assertTrue(getButtonForCode(group.getSelected()).getSelection());
	}

	public void testSetEnablement() throws Exception {
		button1.setSelection(true);
		group.add(FORMAT_UNIFIED, button1);

		button2.setSelection(false);
		button2.setEnabled(false);
		group.add(FORMAT_CONTEXT, button2);

		button3.setSelection(false);
		button3.setEnabled(false);
		group.add(FORMAT_STANDARD, button3);

		group.setEnablement(true, new int[] { FORMAT_UNIFIED, FORMAT_CONTEXT });

		assertEquals(FORMAT_UNIFIED, group.getSelected());

		assertEquals(true, button1.getSelection());
		assertEquals(false, button2.getSelection());
		assertEquals(false, button3.getSelection());

		assertEquals(true, button1.isEnabled());
		assertEquals(true, button2.isEnabled());
		assertEquals(false, button3.isEnabled());

		assertTrue(getButtonForCode(group.getSelected()).isEnabled());
		assertTrue(getButtonForCode(group.getSelected()).getSelection());
	}

	private class MyRadioButtonGroup {

		Object groupObject;
		Class clazz;

		public MyRadioButtonGroup() {

			try {
				GenerateDiffFileWizard wizard = new GenerateDiffFileWizard(
						null, null, false);
				clazz = Class
						.forName("org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard$RadioButtonGroup");
				Constructor[] constructors = clazz.getDeclaredConstructors();
				constructors[0].setAccessible(true);
				groupObject = constructors[0]
						.newInstance(new Object[] { wizard });
			} catch (ClassNotFoundException e) {
				fail(e.getMessage());
			} catch (InstantiationException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
		}

		public void add(int buttonCode, Button button) {
			try {
				Class partypes[] = new Class[2];
				partypes[0] = Integer.TYPE;
				partypes[1] = Button.class;
				Method method = clazz.getMethod("add", partypes);
				method.setAccessible(true);
				Object arglist[] = new Object[2];
				arglist[0] = new Integer(buttonCode);
				arglist[1] = button;
				method.invoke(groupObject, arglist);
			} catch (SecurityException e) {
				fail(e.getMessage());
			} catch (NoSuchMethodException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
		}

		public int getSelected() {
			try {
				Class partypes[] = new Class[0];
				Method method = clazz.getMethod("getSelected", partypes);
				method.setAccessible(true);
				Object arglist[] = new Object[0];
				Object retobj = method.invoke(groupObject, arglist);
				return ((Integer) retobj).intValue();
			} catch (SecurityException e) {
				fail(e.getMessage());
			} catch (NoSuchMethodException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
			return 0;
		}

		public int selectEnabledOnly() {
			try {
				Class partypes[] = new Class[0];
				Method method = clazz.getMethod("selectEnabledOnly", partypes);
				method.setAccessible(true);
				Object arglist[] = new Object[0];
				Object retobj = method.invoke(groupObject, arglist);
				return ((Integer) retobj).intValue();
			} catch (SecurityException e) {
				fail(e.getMessage());
			} catch (NoSuchMethodException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
			return 0;
		}

		public void setEnablement(boolean enabled, int[] buttonsToChange,
				int defaultSelection) {
			try {
				Class partypes[] = new Class[3];
				partypes[0] = Boolean.TYPE;
				partypes[1] = buttonsToChange.getClass();
				partypes[2] = Integer.TYPE;
				Method method = clazz.getMethod("setEnablement", partypes);
				method.setAccessible(true);
				Object arglist[] = new Object[3];
				arglist[0] = Boolean.valueOf(enabled);
				arglist[1] = buttonsToChange;
				arglist[2] = new Integer(defaultSelection);
				method.invoke(groupObject, arglist);
			} catch (SecurityException e) {
				fail(e.getMessage());
			} catch (NoSuchMethodException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
		}

		public void setEnablement(boolean enabled, int[] buttonsToChange) {
			try {
				Class partypes[] = new Class[2];
				partypes[0] = Boolean.TYPE;
				partypes[1] = buttonsToChange.getClass();
				Method method = clazz.getMethod("setEnablement", partypes);
				method.setAccessible(true);
				Object arglist[] = new Object[2];
				arglist[0] = Boolean.valueOf(enabled);
				arglist[1] = buttonsToChange;
				method.invoke(groupObject, arglist);
			} catch (SecurityException e) {
				fail(e.getMessage());
			} catch (NoSuchMethodException e) {
				fail(e.getMessage());
			} catch (IllegalArgumentException e) {
				fail(e.getMessage());
			} catch (IllegalAccessException e) {
				fail(e.getMessage());
			} catch (InvocationTargetException e) {
				fail(e.getMessage());
			}
		}

	}

	static private int getFieldValue(String fieldName) {
		Class clazz;
		try {
			clazz = Class
					.forName("org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard$OptionsPage");
			Field field = clazz.getField(fieldName);
			field.setAccessible(true);
			return ((Integer) field.get(null)).intValue();
		} catch (ClassNotFoundException e) {
			fail(e.getMessage());
		} catch (SecurityException e) {
			fail(e.getMessage());
		} catch (NoSuchFieldException e) {
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		} catch (IllegalAccessException e) {
			fail(e.getMessage());
		}
		return -1;
	}
}
