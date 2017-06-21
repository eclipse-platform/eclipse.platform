/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

import org.eclipse.compare.IStreamMerger;
import org.eclipse.compare.internal.merge.TextStreamMerger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class StreamMergerTest extends TestCase {

	private static final String ABC= "abc"; //$NON-NLS-1$
	private static final String DEF= "def"; //$NON-NLS-1$
	private static final String BAR= "bar"; //$NON-NLS-1$
	private static final String FOO= "foo"; //$NON-NLS-1$
	private static final String XYZ= "xyz"; //$NON-NLS-1$
	private static final String _123= "123"; //$NON-NLS-1$
	private static final String _456= "456"; //$NON-NLS-1$

	String encoding= "UTF-8"; //$NON-NLS-1$
	static final String SEPARATOR= System.getProperty("line.separator"); //$NON-NLS-1$

	public StreamMergerTest(String name) {
		super(name);
	}

	public void testIncomingAddition() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	public void testIncomingDeletion() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + XYZ + SEPARATOR);
	}

	public void testIncomingReplacement() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	public void testNonConflictingMerge() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + DEF + SEPARATOR + XYZ + SEPARATOR + FOO;
		String o= ABC + SEPARATOR + _123 + SEPARATOR + _456 + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + _123 + SEPARATOR + _456 + SEPARATOR + XYZ + SEPARATOR + FOO + SEPARATOR);
	}

	public void testConflictingReplacement() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + FOO + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + BAR + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.ERROR);
		assertEquals(status.getCode(), IStreamMerger.CONFLICT);
	}

	public void testConflictingAddition() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	public void testConflictingDeletion() throws UnsupportedEncodingException {

		String a= ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t= ABC + SEPARATOR + XYZ;
		String o= ABC + SEPARATOR + XYZ;

		StringBuffer output= new StringBuffer();

		IStatus status= merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + XYZ + SEPARATOR);
	}

	private IStatus merge(StringBuffer output, String a, String m, String y) throws UnsupportedEncodingException {
		InputStream ancestor= new ByteArrayInputStream(a.getBytes(StandardCharsets.UTF_8));
		InputStream target= new ByteArrayInputStream(m.getBytes(StandardCharsets.UTF_8));
		InputStream other= new ByteArrayInputStream(y.getBytes(StandardCharsets.UTF_8));

		return merge(output, ancestor, target, other);
	}

	private IStatus merge(StringBuffer output, InputStream ancestor,
			InputStream target, InputStream other)
			throws UnsupportedEncodingException {
		ByteArrayOutputStream os= new ByteArrayOutputStream();

		IStreamMerger merger= new TextStreamMerger();
		IStatus status= merger.merge(os, encoding, ancestor, encoding, target, encoding, other, encoding, (IProgressMonitor) null);

		output.append(new String(os.toByteArray(), StandardCharsets.UTF_8));

		return status;
	}
}
