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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.junit.Rule;
import org.junit.Test;

/**
 * Performs black box testing of the following API methods:
 * <code>IWorkspace.computeProjectOrder(IProject[])</code>
 * <code>IWorkspace.computePrerequisiteOrder(IProject[])</code>
 */
public class ProjectOrderTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Adds a project reference from the given source project, which must
	 * be accessible, to the given target project, which need not be accessible.
	 * Does nothing if the source project already has a project reference
	 * to the target project.
	 *
	 * @param source the source project; must be accessible
	 * @param target the target project; need not be accessible
	 */
	void addProjectReference(IProject source, IProject target) throws CoreException {
		IProjectDescription pd = source.getDescription();
		IProject[] a = pd.getReferencedProjects();
		Set<IProject> x = new HashSet<>();
		x.addAll(Arrays.asList(a));
		x.add(target);
		IProject[] r = new IProject[x.size()];
		x.toArray(r);
		pd.setReferencedProjects(r);
		source.setDescription(pd, null);
	}

	/**
	 * P0, P1&lt;-P2&lt;-P3&lt;-P4, P5
	 */
	@Test
	public void test0() throws CoreException {
		IWorkspace ws = getWorkspace();
		IWorkspaceRoot root = ws.getRoot();
		IProject p0 = root.getProject("p0");
		IProject p1 = root.getProject("p1");
		IProject p2 = root.getProject("p2");
		IProject p3 = root.getProject("p3");
		IProject p4 = root.getProject("p4");
		IProject p5 = root.getProject("p5");

		p0.create(null);
		p0.open(null);
		p1.create(null);
		p1.open(null);
		p2.create(null);
		p2.open(null);
		p3.create(null);
		p3.open(null);
		p4.create(null);
		p4.open(null);
		p5.create(null);
		p5.open(null);

		addProjectReference(p2, p1);
		addProjectReference(p3, p2);
		addProjectReference(p4, p3);

		IProject[] projects = new IProject[] { p4, p3, p2, p5, p1, p0 };
		IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
		assertThat(oldOrder[1]).isEmpty();
		List<IProject> x = Arrays.asList(oldOrder[0]);
		assertThat(x).hasSize(6);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p2)).as("index p2").isGreaterThan(x.indexOf(p1));
		assertThat(x.indexOf(p3)).as("index p3").isGreaterThan(x.indexOf(p2));
		assertThat(x.indexOf(p4)).as("index p4").isGreaterThan(x.indexOf(p3));
		assertThat(x.indexOf(p5)).as("index p5").isGreaterThanOrEqualTo(0);

		IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(6);
		assertThat(x.indexOf(p0)).as("index p0").isLessThan(x.indexOf(p1)); // alpha
		assertThat(x.indexOf(p2)).as("index p2").isGreaterThan(x.indexOf(p1)); // ref
		assertThat(x.indexOf(p3)).as("index p3").isGreaterThan(x.indexOf(p2)); // ref
		assertThat(x.indexOf(p4)).as("index p4").isGreaterThan(x.indexOf(p3)); // ref
		assertThat(x.indexOf(p5)).as("index p5").isGreaterThan(x.indexOf(p4)); // alpha
		assertThat(x.indexOf(p5)).as("index p5").isGreaterThanOrEqualTo(0);
	}

	@Test
	public void test1() throws CoreException {
		IWorkspace ws = getWorkspace();
		IWorkspaceRoot root = ws.getRoot();
		IProject p0 = root.getProject("p0");
		IProject p1 = root.getProject("p1");

		IProject[] projects = new IProject[] {};
		IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
		List<IProject> x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).isEmpty();

		// no projects
		IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).isEmpty();

		// 1 project
		// open projects show up
		p0.create(null);
		p0.open(null);
		projects = new IProject[] { p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);

		// closed projects do not show up
		p0.close(null);
		projects = new IProject[] { p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).isEmpty();

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).isEmpty();

		// non-existent projects do not show up either
		projects = new IProject[] { p0, p1 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).isEmpty();

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).isEmpty();

		p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		// 2 projects no references
		// open projects show up
		p0.create(null);
		p0.open(null);
		p1.create(null);
		p1.open(null);

		projects = new IProject[] { p1, p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		// alpha order
		assertThat(x.indexOf(p0)).as("index p0").isLessThan(x.indexOf(p1));
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();

		// closed projects do not show up
		p0.close(null);
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isLessThan(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isLessThan(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		p0.open(null);
		p1.close(null);
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isLessThan(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isLessThan(0);

		p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		// 2 projects: "p0" refs "p1"
		p0.create(null);
		p0.open(null);
		p1.create(null);
		p1.open(null);
		addProjectReference(p0, p1);

		projects = new IProject[] { p1, p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThan(x.indexOf(p1));

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThan(x.indexOf(p1));
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();

		p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		// 2 projects: "p1" refs "p0"
		p0.create(null);
		p0.open(null);
		p1.create(null);
		p1.open(null);
		addProjectReference(p1, p0);

		projects = new IProject[] { p1, p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThan(x.indexOf(p0));

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThan(x.indexOf(p0));

		p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);

		// 2 projects: "p0" refs "p1" and "p1" refs "p0"
		// open projects show up
		p0.create(null);
		p0.open(null);
		p1.create(null);
		p1.open(null);
		addProjectReference(p1, p0);
		addProjectReference(p0, p1);

		projects = new IProject[] { p1, p0 };
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		List<IProject> unordered = Arrays.asList(oldOrder[1]);
		assertThat(oldOrder[1]).hasSize(2);
		assertThat(x).isEmpty();
		assertThat(unordered).hasSize(2);
		assertThat(unordered.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> it.hasCycles, "has cycles");
		assertThat(order.knots).hasNumberOfRows(1);
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		List<IProject> knot = Arrays.asList(order.knots[0]);
		assertThat(knot).hasSize(2);
		assertThat(knot.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(knot.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		// closing one breaks the cycle
		p0.close(null);
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		// closing other instead breaks the cycle
		p0.open(null);
		p1.close(null);
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		assertThat(oldOrder[1]).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> !it.hasCycles, "has no cycles");
		assertThat(order.knots).isEmpty();
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);

		// reopening both brings back the cycle
		p1.open(null);
		oldOrder = ws.computePrerequisiteOrder(projects);
		x = Arrays.asList(oldOrder[0]);
		unordered = Arrays.asList(oldOrder[1]);
		assertThat(oldOrder[1]).hasSize(2);
		assertThat(x).isEmpty();
		assertThat(unordered).hasSize(2);
		assertThat(unordered.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(order).matches(it -> it.hasCycles, "has cycles");
		assertThat(order.knots).hasNumberOfRows(1);
		assertThat(x).hasSize(2);
		assertThat(x.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);
		knot = Arrays.asList(order.knots[0]);
		assertThat(x).hasSize(2);
		assertThat(knot.indexOf(p0)).as("index p0").isGreaterThanOrEqualTo(0);
		assertThat(knot.indexOf(p1)).as("index p1").isGreaterThanOrEqualTo(0);

		p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
	}

	/**
	 * Introduction to Algorithms
	 * Cormen, Leiserson, Rivest
	 * Figure 23.9(b)
	 */
	@Test
	public void test2() throws CoreException {
		IWorkspace ws = getWorkspace();
		IWorkspaceRoot root = ws.getRoot();
		IProject a = root.getProject("a");
		IProject b = root.getProject("b");
		IProject c = root.getProject("c");
		IProject d = root.getProject("d");
		IProject e = root.getProject("e");
		IProject f = root.getProject("f");
		IProject g = root.getProject("g");
		IProject h = root.getProject("h");

		a.create(null);
		a.open(null);
		b.create(null);
		b.open(null);
		c.create(null);
		c.open(null);
		d.create(null);
		d.open(null);
		e.create(null);
		e.open(null);
		f.create(null);
		f.open(null);
		g.create(null);
		g.open(null);
		h.create(null);
		h.open(null);

		// knot 1
		addProjectReference(b, a);
		addProjectReference(a, e);
		addProjectReference(e, b);

		// knot 2
		addProjectReference(c, d);
		addProjectReference(d, c);

		// knot 3
		addProjectReference(f, g);
		addProjectReference(g, f);

		// self-reference
		addProjectReference(h, h);

		// inter-knot references
		addProjectReference(c, b);
		addProjectReference(f, b);
		addProjectReference(g, c);
		addProjectReference(h, d);
		addProjectReference(h, g);

		IProject[] projects = new IProject[] { a, b, c, d, e, f, g, h };
		IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
		List<IProject> x = Arrays.asList(oldOrder[0]);
		List<IProject> unordered = Arrays.asList(oldOrder[1]);
		assertThat(x).hasSize(1);
		assertThat(x.indexOf(h)).as("index h").isGreaterThanOrEqualTo(0);
		assertThat(unordered).hasSize(7);
		assertThat(unordered.indexOf(a)).as("index a").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(b)).as("index b").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(c)).as("index c").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(d)).as("index d").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(e)).as("index e").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(f)).as("index f").isGreaterThanOrEqualTo(0);
		assertThat(unordered.indexOf(g)).as("index g").isGreaterThanOrEqualTo(0);

		IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertThat(x).hasSize(8);
		assertThat(x.indexOf(a)).as("index a").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(b)).as("index b").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(c)).as("index c").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(d)).as("index d").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(e)).as("index e").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(f)).as("index f").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(g)).as("index g").isGreaterThanOrEqualTo(0);
		assertThat(x.indexOf(h)).as("index h").isGreaterThanOrEqualTo(0);
		// {a, b, e} < {c,d} < {f, g} < {h}
		assertThat(x.indexOf(b)).as("index b").isLessThan(x.indexOf(c));
		assertThat(x.indexOf(b)).as("index b").isLessThan(x.indexOf(d));
		assertThat(x.indexOf(a)).as("index a").isLessThan(x.indexOf(c));
		assertThat(x.indexOf(a)).as("index a").isLessThan(x.indexOf(d));
		assertThat(x.indexOf(e)).as("index e").isLessThan(x.indexOf(c));
		assertThat(x.indexOf(e)).as("index e").isLessThan(x.indexOf(d));
		assertThat(x.indexOf(c)).as("index c").isLessThan(x.indexOf(f));
		assertThat(x.indexOf(c)).as("index c").isLessThan(x.indexOf(g));
		assertThat(x.indexOf(d)).as("index d").isLessThan(x.indexOf(f));
		assertThat(x.indexOf(d)).as("index d").isLessThan(x.indexOf(g));
		assertThat(x.indexOf(f)).as("index f").isLessThan(x.indexOf(h));
		assertThat(x.indexOf(g)).as("index g").isLessThan(x.indexOf(h));
		assertThat(order).matches(it -> it.hasCycles, "has cycles");
		assertThat(order.knots).hasNumberOfRows(3);
		List<IProject> k1 = Arrays.asList(order.knots[0]);
		List<IProject> k2 = Arrays.asList(order.knots[1]);
		List<IProject> k3 = Arrays.asList(order.knots[2]);
		// sort 3 groups
		if (k2.indexOf(b) >= 0) {
			List<IProject> temp = k1;
			k1 = k2;
			k2 = temp;
		} else if (k3.indexOf(b) >= 0) {
			List<IProject> temp = k1;
			k1 = k3;
			k3 = temp;
		}
		if (k3.indexOf(c) >= 0) {
			List<IProject> temp = k2;
			k2 = k3;
			k3 = temp;
		}
		// knot 1
		assertThat(k1).hasSize(3);
		assertThat(k1.indexOf(a)).as("index a").isGreaterThanOrEqualTo(0);
		assertThat(k1.indexOf(b)).as("index b").isGreaterThanOrEqualTo(0);
		assertThat(k1.indexOf(e)).as("index e").isGreaterThanOrEqualTo(0);
		// knot 2
		assertThat(k2).hasSize(2);
		assertThat(k2.indexOf(c)).as("index c").isGreaterThanOrEqualTo(0);
		assertThat(k2.indexOf(d)).as("index d").isGreaterThanOrEqualTo(0);
		// knot 3
		assertThat(k3).hasSize(2);
		assertThat(k3.indexOf(f)).as("index f").isGreaterThanOrEqualTo(0);
		assertThat(k3.indexOf(g)).as("index g").isGreaterThanOrEqualTo(0);
	}

}
