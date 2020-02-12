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

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.IStreamMerger;
import org.eclipse.compare.internal.merge.TextStreamMerger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class StreamMergerTest {

	private static final String ABC = "abc"; //$NON-NLS-1$
	private static final String DEF = "def"; //$NON-NLS-1$
	private static final String BAR = "bar"; //$NON-NLS-1$
	private static final String FOO = "foo"; //$NON-NLS-1$
	private static final String XYZ = "xyz"; //$NON-NLS-1$
	private static final String _123 = "123"; //$NON-NLS-1$
	private static final String _456 = "456"; //$NON-NLS-1$

	String encoding = "UTF-8"; //$NON-NLS-1$
	static final String SEPARATOR = System.lineSeparator();

	@Test
	public void testIncomingAddition() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	@Test
	public void testIncomingDeletion() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + XYZ + SEPARATOR);
	}

	@Test
	public void testIncomingReplacement() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	@Test
	public void testNonConflictingMerge() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + DEF + SEPARATOR + XYZ + SEPARATOR + FOO;
		String o = ABC + SEPARATOR + _123 + SEPARATOR + _456 + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(),
				ABC + SEPARATOR + _123 + SEPARATOR + _456 + SEPARATOR + XYZ + SEPARATOR + FOO + SEPARATOR);
	}

	@Test
	public void testConflictingReplacement() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + FOO + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + BAR + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.ERROR);
		assertEquals(status.getCode(), IStreamMerger.CONFLICT);
	}

	@Test
	public void testConflictingAddition() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + DEF + SEPARATOR + _123 + SEPARATOR + XYZ + SEPARATOR);
	}

	@Test
	public void testConflictingDeletion() {

		String a = ABC + SEPARATOR + DEF + SEPARATOR + XYZ;
		String t = ABC + SEPARATOR + XYZ;
		String o = ABC + SEPARATOR + XYZ;

		StringBuilder output = new StringBuilder();

		IStatus status = merge(output, a, t, o);

		assertEquals(status.getSeverity(), IStatus.OK);
		assertEquals(status.getCode(), IStatus.OK);
		assertEquals(output.toString(), ABC + SEPARATOR + XYZ + SEPARATOR);
	}

	private IStatus merge(StringBuilder output, String a, String m, String y) {
		InputStream ancestor = new ByteArrayInputStream(a.getBytes(StandardCharsets.UTF_8));
		InputStream target = new ByteArrayInputStream(m.getBytes(StandardCharsets.UTF_8));
		InputStream other = new ByteArrayInputStream(y.getBytes(StandardCharsets.UTF_8));

		return merge(output, ancestor, target, other);
	}

	private IStatus merge(StringBuilder output, InputStream ancestor, InputStream target, InputStream other) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		IStreamMerger merger = new TextStreamMerger();
		IStatus status = merger.merge(os, encoding, ancestor, encoding, target, encoding, other, encoding,
				(IProgressMonitor) null);

		output.append(new String(os.toByteArray(), StandardCharsets.UTF_8));

		return status;
	}
}
