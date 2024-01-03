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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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

@SuppressWarnings("restriction")
public class ExpressionInfoTests {

	// ---- test merging ------------------------------------------------------------------------

	@Test
	public void testMergeEmpty() {
		ExpressionInfo info= new ExpressionInfo();
		info.merge(new ExpressionInfo());
		assertNoAccess(info);
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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
		assertPropertyAccess(info, new String[] { "prop1", "prop2" });
	}

	@Test
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

	@Test
	public void testCountExpression() {
		assertDefaultAccessOnly((new CountExpression("10")).computeExpressionInfo());
	}

	@Test
	public void testEqualsExpression() {
		assertDefaultAccessOnly((new EqualsExpression(new Object())).computeExpressionInfo());
	}

	@Test
	public void testInstanceofExpression() {
		assertDefaultAccessOnly((new InstanceofExpression("java.lang.Object")).computeExpressionInfo());
	}

	@Test
	public void testNotExpression() {
		assertDefaultAccessOnly((new NotExpression(new CountExpression("10"))).computeExpressionInfo());
	}

	@Test
	public void testSystemExpression() {
		assertSystemPropertyOnly((new SystemTestExpression("property", "value")).computeExpressionInfo());
	}

	@Test
	public void testTestExpression() {
		assertPropertyAccess((new TestExpression("namespace", "property", null,
				new Object())).computeExpressionInfo(), "namespace.property", true);
	}

	// ---- composite expressions ---------------------------------------------------------

	@Test
	public void testAdaptExpression() throws Exception {
		assertDefaultAccessOnly(new AdaptExpression("java.lang.Object").computeExpressionInfo());
	}

	@Test
	public void testAndExpression() throws Exception {
		AndExpression and= new AndExpression();
		assertNoAccess(and.computeExpressionInfo());
		and.add(new CountExpression("10"));
		assertDefaultAccessOnly(and.computeExpressionInfo());
	}

	@Test
	public void testIterateExpression() throws Exception {
		assertDefaultAccessOnly(new IterateExpression("or").computeExpressionInfo());
	}

	@Test
	public void testResolveExpression() {
		ResolveExpression resolve= new ResolveExpression("variable", null);
		assertNoAccess(resolve.computeExpressionInfo());
		resolve.add(new CountExpression("10"));
		assertVariableAccess(resolve.computeExpressionInfo(), "variable");
	}

	@Test
	public void testWithExpression() {
		WithExpression with= new WithExpression("variable");
		assertNoAccess(with.computeExpressionInfo());
		with.add(new CountExpression("10"));
		assertVariableAccess(with.computeExpressionInfo(), "variable");
	}

	private void assertDefaultAccessOnly(ExpressionInfo info) {
		assertThat(info).matches(ExpressionInfo::hasDefaultVariableAccess, "accesses default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedVariableNames()).as("no variable accessed").isEmpty();
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
	}

	private void assertSystemPropertyOnly(ExpressionInfo info) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(ExpressionInfo::hasSystemPropertyAccess, "accesses system property");
		assertThat(info.getAccessedVariableNames()).as("no variable accessed").isEmpty();
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
	}

	private void assertNoAccess(ExpressionInfo info) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedVariableNames()).as("no variable accessed").isEmpty();
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
	}

	private void assertVariableAccess(ExpressionInfo info, String variable) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedVariableNames()).as("one variable accessed").containsExactly(variable);
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
	}

	private void assertVariableAccess(ExpressionInfo info, String[] variables) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedVariableNames()).as("all variables accessed").containsExactlyInAnyOrder(variables);
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
	}

	private void assertPropertyAccess(ExpressionInfo info, String property, boolean defaultVariable) {
		if (defaultVariable) {
			assertThat(info).matches(ExpressionInfo::hasDefaultVariableAccess, "accesses default variable");
		} else {
			assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		}
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedPropertyNames()).as("one property accessed").containsExactly(property);
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedVariableNames()).as("no variable accessed").isEmpty();
	}

	private void assertPropertyAccess(ExpressionInfo info, String[] properties) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedPropertyNames()).as("all properties accessed").containsExactlyInAnyOrder(properties);
		assertThat(info.getMisbehavingExpressionTypes()).as("no misbehaving expression types").isNull();
		assertThat(info.getAccessedVariableNames()).as("one variable accessed").isEmpty();
	}

	private void assertMisbehavedExpressionTypes(ExpressionInfo info, Class<?>[] types) {
		assertThat(info).matches(not(ExpressionInfo::hasDefaultVariableAccess), "doesn't access default variable");
		assertThat(info).matches(not(ExpressionInfo::hasSystemPropertyAccess), "doesn't access system property");
		assertThat(info.getAccessedVariableNames()).as("no variable accessed").isEmpty();
		assertThat(info.getAccessedPropertyNames()).as("no properties accessed").isEmpty();
		assertThat(info.getMisbehavingExpressionTypes()).as("all types accessed").containsExactlyInAnyOrder(types);
	}
}
