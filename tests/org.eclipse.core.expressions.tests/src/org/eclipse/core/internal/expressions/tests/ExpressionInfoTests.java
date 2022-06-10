/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions.tests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.expressions.AndExpression;
import org.eclipse.core.expressions.CountExpression;
import org.eclipse.core.expressions.EqualsExpression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.TestExpression;
import org.eclipse.core.expressions.WithExpression;
import org.eclipse.core.internal.expressions.AdaptExpression;
import org.eclipse.core.internal.expressions.InstanceofExpression;
import org.eclipse.core.internal.expressions.IterateExpression;
import org.eclipse.core.internal.expressions.NotExpression;
import org.eclipse.core.internal.expressions.ResolveExpression;
import org.eclipse.core.internal.expressions.SystemTestExpression;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ExpressionInfoTests extends TestCase {

	public static Test suite() {
		return new TestSuite(ExpressionInfoTests.class);
	}

	// ---- test merging ------------------------------------------------------------------------

	public void testMergeEmpty() {
		ExpressionInfo info= new ExpressionInfo();
		info.merge(new ExpressionInfo());
		assertNoAccess(info);
	}

	public void testMergeDefaultVariable() {
		ExpressionInfo info;
		ExpressionInfo other;

		info= new ExpressionInfo();
		info.markDefaultVariableAccessed();
		info.merge(new ExpressionInfo());
		assertDefaultAccessOnly(info);

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		other.markDefaultVariableAccessed();
		info.merge(other);
		assertDefaultAccessOnly(info);

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		info.markDefaultVariableAccessed();
		other.markDefaultVariableAccessed();
		info.merge(other);
		assertDefaultAccessOnly(info);
	}

	public void testMergeSystemProperty() {
		ExpressionInfo info;
		ExpressionInfo other;

		info= new ExpressionInfo();
		info.markSystemPropertyAccessed();
		info.merge(new ExpressionInfo());
		assertSystemPropertyOnly(info);

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		other.markSystemPropertyAccessed();
		info.merge(other);
		assertSystemPropertyOnly(info);

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		info.markSystemPropertyAccessed();
		other.markSystemPropertyAccessed();
		info.merge(other);
		assertSystemPropertyOnly(info);
	}

	public void testMergeVariableNames() {
		ExpressionInfo info;
		ExpressionInfo other;

		info= new ExpressionInfo();
		info.addVariableNameAccess("variable");
		info.merge(new ExpressionInfo());
		assertVariableAccess(info, "variable");

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		other.addVariableNameAccess("variable");
		info.merge(other);
		assertVariableAccess(info, "variable");

		info= new ExpressionInfo();
		info.addVariableNameAccess("variable");
		other= new ExpressionInfo();
		other.addVariableNameAccess("variable");
		info.merge(other);
		assertVariableAccess(info, "variable");

		info= new ExpressionInfo();
		info.addVariableNameAccess("variable_one");
		other= new ExpressionInfo();
		other.addVariableNameAccess("variable_two");
		info.merge(other);
		assertVariableAccess(info, new String[] {"variable_one", "variable_two"});
	}

	public void testMergePropertyNames() {
		ExpressionInfo info;
		ExpressionInfo other;

		info = new ExpressionInfo();
		info.addAccessedPropertyName("property");
		info.merge(new ExpressionInfo());
		assertPropertyAccess(info, "property", false);

		info = new ExpressionInfo();
		other = new ExpressionInfo();
		other.addAccessedPropertyName("property");
		info.merge(other);
		assertPropertyAccess(info, "property", false);

		info = new ExpressionInfo();
		other = new ExpressionInfo();
		info.addAccessedPropertyName("property");
		other.addAccessedPropertyName("property");
		info.merge(other);
		assertPropertyAccess(info, "property", false);

		info = new ExpressionInfo();
		other = new ExpressionInfo();
		info.addAccessedPropertyName("prop1");
		other.addAccessedPropertyName("prop2");
		info.merge(other);
		assertPropertyAccess(info, new String[] { "prop1", "prop2" }, false);
	}

	public void testMergeMisbehavingExpressionTypes() {
		ExpressionInfo info;
		ExpressionInfo other;

		info= new ExpressionInfo();
		info.addMisBehavingExpressionType(WithExpression.class);
		info.merge(new ExpressionInfo());
		assertMisbehavedExpressionTypes(info, new Class[] {WithExpression.class});

		info= new ExpressionInfo();
		other= new ExpressionInfo();
		other.addMisBehavingExpressionType(WithExpression.class);
		info.merge(other);
		assertMisbehavedExpressionTypes(info, new Class[] {WithExpression.class});

		info= new ExpressionInfo();
		info.addMisBehavingExpressionType(WithExpression.class);
		other= new ExpressionInfo();
		other.addMisBehavingExpressionType(WithExpression.class);
		info.merge(other);
		assertMisbehavedExpressionTypes(info, new Class[] {WithExpression.class});

		info= new ExpressionInfo();
		info.addMisBehavingExpressionType(WithExpression.class);
		other= new ExpressionInfo();
		other.addMisBehavingExpressionType(ResolveExpression.class);
		info.merge(other);
		assertMisbehavedExpressionTypes(info, new Class[] {WithExpression.class, ResolveExpression.class});
	}

	// ---- test expression ---------------------------------------------------------------------

	public void testCountExpression() {
		assertDefaultAccessOnly((new CountExpression("10")).computeExpressionInfo());
	}

	public void testEqualsExpression() {
		assertDefaultAccessOnly((new EqualsExpression(new Object())).computeExpressionInfo());
	}

	public void testInstanceofExpression() {
		assertDefaultAccessOnly((new InstanceofExpression("java.lang.Object")).computeExpressionInfo());
	}

	public void testNotExpression() {
		assertDefaultAccessOnly((new NotExpression(new CountExpression("10"))).computeExpressionInfo());
	}

	public void testSystemExpression() {
		assertSystemPropertyOnly((new SystemTestExpression("property", "value")).computeExpressionInfo());
	}

	public void testTestExpression() {
		assertPropertyAccess((new TestExpression("namespace", "property", null,
				new Object())).computeExpressionInfo(), "namespace.property", true);
	}

	// ---- composite expressions ---------------------------------------------------------

	public void testAdaptExpression() throws Exception {
		assertDefaultAccessOnly(new AdaptExpression("java.lang.Object").computeExpressionInfo());
	}

	public void testAndExpression() throws Exception {
		AndExpression and= new AndExpression();
		assertNoAccess(and.computeExpressionInfo());
		and.add(new CountExpression("10"));
		assertDefaultAccessOnly(and.computeExpressionInfo());
	}

	public void testIterateExpression() throws Exception {
		assertDefaultAccessOnly(new IterateExpression("or").computeExpressionInfo());
	}

	public void testResolveExpression() {
		ResolveExpression resolve= new ResolveExpression("variable", null);
		assertNoAccess(resolve.computeExpressionInfo());
		resolve.add(new CountExpression("10"));
		assertVariableAccess(resolve.computeExpressionInfo(), "variable");
	}

	public void testWithExpression() {
		WithExpression with= new WithExpression("variable");
		assertNoAccess(with.computeExpressionInfo());
		with.add(new CountExpression("10"));
		assertVariableAccess(with.computeExpressionInfo(), "variable");
	}

	private void assertDefaultAccessOnly(ExpressionInfo info) {
		assertTrue("Accesses default variable", info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		assertTrue("No variable accesses", info.getAccessedVariableNames().length == 0);
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
	}

	private void assertSystemPropertyOnly(ExpressionInfo info) {
		assertFalse("Doesn't accesses default variable", info.hasDefaultVariableAccess());
		assertTrue("Accesses system property", info.hasSystemPropertyAccess());
		assertTrue("No variable accesses", info.getAccessedVariableNames().length == 0);
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
	}

	private void assertNoAccess(ExpressionInfo info) {
		assertFalse("Doesn't accesses default variable", info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		assertTrue("No variable accesses", info.getAccessedVariableNames().length == 0);
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
	}

	private void assertVariableAccess(ExpressionInfo info, String variable) {
		assertFalse("Doesn't accesses default variable", info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		String[] accessedVariableNames= info.getAccessedVariableNames();
		assertEquals("One variable accessed", 1, accessedVariableNames.length);
		assertEquals("Variable accessed", variable, accessedVariableNames[0]);
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
	}

	private void assertVariableAccess(ExpressionInfo info, String[] variables) {
		assertFalse("Doesn't accesses default variable", info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		Set<String> accessedVariableNames= new HashSet<>(Arrays.asList(info.getAccessedVariableNames()));
		assertEquals("All variable accessed", variables.length, accessedVariableNames.size());
		for (String variable : variables) {
			assertTrue("Variable accessed", accessedVariableNames.contains(variable));
		}
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
	}

	private void assertPropertyAccess(ExpressionInfo info, String property, boolean defaultVariable) {
		assertEquals("Accesses default variable", defaultVariable, info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		String[] accessedPropertyNames= info.getAccessedPropertyNames();
		assertEquals("One property accessed", 1, accessedPropertyNames.length);
		assertEquals("Property accessed", property, accessedPropertyNames[0]);
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No variable accesses", 0, info.getAccessedVariableNames().length);
	}

	private void assertPropertyAccess(ExpressionInfo info, String[] properties, boolean defaultVariable) {
		assertEquals("Accesses default variable", defaultVariable, info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		Set<String> accessedPropertyNames= new HashSet<>(Arrays.asList(info.getAccessedPropertyNames()));
		assertEquals("All properties accessed", properties.length, accessedPropertyNames.size());
		for (String property : properties) {
			assertTrue("Property accessed", accessedPropertyNames.contains(property));
		}
		assertNull("No misbehaving expression types", info.getMisbehavingExpressionTypes());
		assertEquals("No variable accesses", 0, info.getAccessedVariableNames().length);
	}

	private void assertMisbehavedExpressionTypes(ExpressionInfo info, Class<?>[] types) {
		assertFalse("Doesn't accesses default variable", info.hasDefaultVariableAccess());
		assertFalse("Doesn't accesses system property", info.hasSystemPropertyAccess());
		assertTrue("No variable accesses", info.getAccessedVariableNames().length == 0);
		assertEquals("No properties accessed", 0, info.getAccessedPropertyNames().length);
		Set<?> misbehavedTypes = new HashSet<>(Arrays.asList(info.getMisbehavingExpressionTypes()));
		assertEquals("All types accessed", types.length, misbehavedTypes.size());
		for (Class<?> type : types) {
			assertTrue("Type collected", misbehavedTypes.contains(type));
		}
	}
}
