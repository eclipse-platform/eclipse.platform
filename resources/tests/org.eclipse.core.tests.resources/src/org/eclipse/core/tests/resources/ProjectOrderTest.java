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

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * Performs black box testing of the following API methods:
 * <code>IWorkspace.computeProjectOrder(IProject[])</code>
 * <code>IWorkspace.computePrerequisiteOrder(IProject[])</code>
 */
public class ProjectOrderTest extends ResourceTest {

	/**
	 * Adds a project reference from the given source project, which must
	 * be accessible, to the given target project, which need not be accessible.
	 * Does nothing if the source project already has a project reference
	 * to the target project.
	 *
	 * @param source the source project; must be accessible
	 * @param target the target project; need not be accessible
	 */
	void addProjectReference(IProject source, IProject target) {
		try {
			IProjectDescription pd = source.getDescription();
			IProject[] a = pd.getReferencedProjects();
			Set<IProject> x = new HashSet<>();
			x.addAll(Arrays.asList(a));
			x.add(target);
			IProject[] r = new IProject[x.size()];
			x.toArray(r);
			pd.setReferencedProjects(r);
			source.setDescription(pd, null);
		} catch (CoreException e) {
			assertTrue("", false);
		}
	}

	/**
	 * P0, P1&lt;-P2&lt;-P3&lt;-P4, P5
	 */
	public void test0() {
		IWorkspace ws = getWorkspace();
		IWorkspaceRoot root = ws.getRoot();
		IProject p0 = root.getProject("p0");
		IProject p1 = root.getProject("p1");
		IProject p2 = root.getProject("p2");
		IProject p3 = root.getProject("p3");
		IProject p4 = root.getProject("p4");
		IProject p5 = root.getProject("p5");

		try {
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

			IProject[] projects = new IProject[] {p4, p3, p2, p5, p1, p0};
			IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
			assertTrue("0.1", oldOrder[1].length == 0);
			List<IProject> x = Arrays.asList(oldOrder[0]);
			assertTrue("0.2", x.size() == 6);
			assertTrue("0.3", x.indexOf(p0) >= 0);
			assertTrue("0.4", x.indexOf(p1) >= 0);
			assertTrue("0.5", x.indexOf(p5) >= 0);
			assertTrue("0.6", x.indexOf(p2) > x.indexOf(p1));
			assertTrue("0.7", x.indexOf(p3) > x.indexOf(p2));
			assertTrue("0.8", x.indexOf(p4) > x.indexOf(p3));

			IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("0.9", !order.hasCycles);
			assertTrue("0.10", order.knots.length == 0);
			assertTrue("0.11", x.size() == 6);
			assertTrue("0.12", x.indexOf(p0) < x.indexOf(p1)); //alpha
			assertTrue("0.13", x.indexOf(p2) > x.indexOf(p1)); //ref
			assertTrue("0.14", x.indexOf(p3) > x.indexOf(p2)); //ref
			assertTrue("0.15", x.indexOf(p4) > x.indexOf(p3)); //ref
			assertTrue("0.16", x.indexOf(p5) > x.indexOf(p4)); //alpha
		} catch (CoreException e) {
			assertTrue("0.0", false);
		}
	}

	public void test1() {
		IWorkspace ws = getWorkspace();
		IWorkspaceRoot root = ws.getRoot();
		IProject p0 = root.getProject("p0");
		IProject p1 = root.getProject("p1");

		IProject[] projects = new IProject[] {};
		IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
		List<IProject> x = Arrays.asList(oldOrder[0]);
		assertTrue("1.1", oldOrder[1].length == 0);
		assertTrue("1.2", x.isEmpty());

		// no projects
		IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
		x = Arrays.asList(order.projects);
		assertTrue("1.3", !order.hasCycles);
		assertTrue("1.4", order.knots.length == 0);
		assertTrue("1.5", x.isEmpty());

		// 1 project
		try {
			// open projects show up
			p0.create(null);
			p0.open(null);
			projects = new IProject[] {p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.6.1", oldOrder[1].length == 0);
			assertTrue("1.6.2", x.size() == 1);
			assertTrue("1.6.3", x.indexOf(p0) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.6.4", !order.hasCycles);
			assertTrue("1.6.5", order.knots.length == 0);
			assertTrue("1.6.6", x.size() == 1);
			assertTrue("1.6.7", x.indexOf(p0) >= 0);

			// closed projects do not show up
			p0.close(null);
			projects = new IProject[] {p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.6,8", oldOrder[1].length == 0);
			assertTrue("1.6.9", x.isEmpty());

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.6.10", !order.hasCycles);
			assertTrue("1.6.11", order.knots.length == 0);
			assertTrue("1.6.12", x.isEmpty());

			// non-existent projects do not show up either
			projects = new IProject[] {p0, p1};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.6.13", oldOrder[1].length == 0);
			assertTrue("1.6.14", x.isEmpty());

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.6.15", !order.hasCycles);
			assertTrue("1.6.16", order.knots.length == 0);
			assertTrue("1.6.17", x.isEmpty());

			p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		} catch (CoreException e) {
			assertTrue("1.6.0", false);
		}

		// 2 projects no references
		try {
			// open projects show up
			p0.create(null);
			p0.open(null);
			p1.create(null);
			p1.open(null);

			projects = new IProject[] {p1, p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.7.1", oldOrder[1].length == 0);
			assertTrue("1.7.2", x.size() == 2);
			assertTrue("1.7.3", x.indexOf(p0) >= 0);
			assertTrue("1.7.4", x.indexOf(p1) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.7.5", x.size() == 2);
			assertTrue("1.7.6", x.indexOf(p0) >= 0);
			assertTrue("1.7.7", x.indexOf(p1) >= 0);
			// alpha order
			assertTrue("1.7.8", x.indexOf(p0) < x.indexOf(p1));
			assertTrue("1.7.9", !order.hasCycles);
			assertTrue("1.7.10", order.knots.length == 0);

			// closed projects do not show up
			p0.close(null);
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.7.11", oldOrder[1].length == 0);
			assertTrue("1.7.12", x.size() == 1);
			assertTrue("1.7.13", x.indexOf(p0) < 0);
			assertTrue("1.7.14", x.indexOf(p1) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.7.15", !order.hasCycles);
			assertTrue("1.7.16", order.knots.length == 0);
			assertTrue("1.7.17", x.size() == 1);
			assertTrue("1.7.18", x.indexOf(p0) < 0);
			assertTrue("1.7.19", x.indexOf(p1) >= 0);

			p0.open(null);
			p1.close(null);
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.7.20", oldOrder[1].length == 0);
			assertTrue("1.7.21", x.size() == 1);
			assertTrue("1.7.22", x.indexOf(p0) >= 0);
			assertTrue("1.7.23", x.indexOf(p1) < 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.7.24", !order.hasCycles);
			assertTrue("1.7.25", order.knots.length == 0);
			assertTrue("1.7.26", x.size() == 1);
			assertTrue("1.7.27", x.indexOf(p0) >= 0);
			assertTrue("1.7.28", x.indexOf(p1) < 0);

			p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
			p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		} catch (CoreException e) {
			assertTrue("1.7.0", false);
		}

		// 2 projects: "p0" refs "p1"
		try {
			p0.create(null);
			p0.open(null);
			p1.create(null);
			p1.open(null);
			addProjectReference(p0, p1);

			projects = new IProject[] {p1, p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.8.1", oldOrder[1].length == 0);
			assertTrue("1.8.2", x.size() == 2);
			assertTrue("1.8.3", x.indexOf(p0) >= 0);
			assertTrue("1.8.4", x.indexOf(p1) >= 0);
			assertTrue("1.8.5", x.indexOf(p0) > x.indexOf(p1));

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.8.6", x.size() == 2);
			assertTrue("1.8.7", x.indexOf(p0) >= 0);
			assertTrue("1.8.8", x.indexOf(p1) >= 0);
			assertTrue("1.8.9", x.indexOf(p0) > x.indexOf(p1));
			assertTrue("1.8.10", !order.hasCycles);
			assertTrue("1.8.11", order.knots.length == 0);

			p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
			p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		} catch (CoreException e) {
			assertTrue("1.8.0", false);
		}

		// 2 projects: "p1" refs "p0"
		try {
			p0.create(null);
			p0.open(null);
			p1.create(null);
			p1.open(null);
			addProjectReference(p1, p0);

			projects = new IProject[] {p1, p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.9.1", oldOrder[1].length == 0);
			assertTrue("1.9.2", x.size() == 2);
			assertTrue("1.9.3", x.indexOf(p0) >= 0);
			assertTrue("1.9.4", x.indexOf(p1) >= 0);
			assertTrue("1.9.5", x.indexOf(p1) > x.indexOf(p0));

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.9.6", !order.hasCycles);
			assertTrue("1.9.7", order.knots.length == 0);
			assertTrue("1.9.8", x.size() == 2);
			assertTrue("1.9.9", x.indexOf(p0) >= 0);
			assertTrue("1.9.10", x.indexOf(p1) >= 0);
			assertTrue("1.9.11", x.indexOf(p1) > x.indexOf(p0));

			p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
			p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		} catch (CoreException e) {
			assertTrue("1.9.0", false);
		}

		// 2 projects: "p0" refs "p1" and "p1" refs "p0"
		try {
			// open projects show up
			p0.create(null);
			p0.open(null);
			p1.create(null);
			p1.open(null);
			addProjectReference(p1, p0);
			addProjectReference(p0, p1);

			projects = new IProject[] {p1, p0};
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			List<IProject> unordered = Arrays.asList(oldOrder[1]);
			assertTrue("1.10.1", oldOrder[1].length == 2);
			assertTrue("1.10.2", x.isEmpty());
			assertTrue("1.10.3", unordered.size() == 2);
			assertTrue("1.10.4", unordered.indexOf(p0) >= 0);
			assertTrue("1.10.5", unordered.indexOf(p1) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.10.6", order.hasCycles);
			assertTrue("1.10.7", order.knots.length == 1);
			assertTrue("1.10.8", x.size() == 2);
			assertTrue("1.10.9", x.indexOf(p0) >= 0);
			assertTrue("1.10.10", x.indexOf(p1) >= 0);
			List<IProject> knot = Arrays.asList(order.knots[0]);
			assertTrue("1.10.11", knot.size() == 2);
			assertTrue("1.10.12", knot.indexOf(p0) >= 0);
			assertTrue("1.10.13", knot.indexOf(p1) >= 0);

			// closing one breaks the cycle
			p0.close(null);
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.10.14", oldOrder[1].length == 0);
			assertTrue("1.10.15", x.size() == 1);
			assertTrue("1.10.16", x.indexOf(p1) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.10.17", !order.hasCycles);
			assertTrue("1.10.18", order.knots.length == 0);
			assertTrue("1.10.19", x.size() == 1);
			assertTrue("1.10.20", x.indexOf(p1) >= 0);

			// closing other instead breaks the cycle
			p0.open(null);
			p1.close(null);
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			assertTrue("1.10.21", oldOrder[1].length == 0);
			assertTrue("1.10.22", x.size() == 1);
			assertTrue("1.10.23", x.indexOf(p0) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.10.24", !order.hasCycles);
			assertTrue("1.10.25", order.knots.length == 0);
			assertTrue("1.10.26", x.size() == 1);
			assertTrue("1.10.27", x.indexOf(p0) >= 0);

			// reopening both brings back the cycle
			p1.open(null);
			oldOrder = ws.computePrerequisiteOrder(projects);
			x = Arrays.asList(oldOrder[0]);
			unordered = Arrays.asList(oldOrder[1]);
			assertTrue("1.10.28", oldOrder[1].length == 2);
			assertTrue("1.10.29", x.isEmpty());
			assertTrue("1.10.30", unordered.size() == 2);
			assertTrue("1.10.31", unordered.indexOf(p0) >= 0);
			assertTrue("1.10.32", unordered.indexOf(p1) >= 0);

			order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("1.10.33", order.hasCycles);
			assertTrue("1.10.34", order.knots.length == 1);
			assertTrue("1.10.35", x.size() == 2);
			assertTrue("1.10.36", x.indexOf(p0) >= 0);
			assertTrue("1.10.37", x.indexOf(p1) >= 0);
			knot = Arrays.asList(order.knots[0]);
			assertTrue("1.10.38", knot.size() == 2);
			assertTrue("1.10.39", knot.indexOf(p0) >= 0);
			assertTrue("1.10.40", knot.indexOf(p1) >= 0);

			p0.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
			p1.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
		} catch (CoreException e) {
			assertTrue("1.10.41", false);
		}
	}

	/**
	 * Introduction to Algorithms
	 * Cormen, Leiserson, Rivest
	 * Figure 23.9(b)
	 */
	public void test2() {
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

		try {
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

			IProject[] projects = new IProject[] {a, b, c, d, e, f, g, h};
			IProject[][] oldOrder = ws.computePrerequisiteOrder(projects);
			List<IProject> x = Arrays.asList(oldOrder[0]);
			List<IProject> unordered = Arrays.asList(oldOrder[1]);
			assertTrue("2.1", x.size() == 1);
			assertTrue("2.2", x.indexOf(h) >= 0);
			assertTrue("2.3", unordered.size() == 7);
			assertTrue("2.4", unordered.indexOf(a) >= 0);
			assertTrue("2.5", unordered.indexOf(b) >= 0);
			assertTrue("2.6", unordered.indexOf(c) >= 0);
			assertTrue("2.7", unordered.indexOf(d) >= 0);
			assertTrue("2.8", unordered.indexOf(e) >= 0);
			assertTrue("2.9", unordered.indexOf(f) >= 0);
			assertTrue("2.10", unordered.indexOf(g) >= 0);

			IWorkspace.ProjectOrder order = ws.computeProjectOrder(projects);
			x = Arrays.asList(order.projects);
			assertTrue("2.11", x.size() == 8);
			assertTrue("2.12", x.indexOf(a) >= 0);
			assertTrue("2.13", x.indexOf(b) >= 0);
			assertTrue("2.14", x.indexOf(c) >= 0);
			assertTrue("2.15", x.indexOf(d) >= 0);
			assertTrue("2.16", x.indexOf(e) >= 0);
			assertTrue("2.17", x.indexOf(f) >= 0);
			assertTrue("2.18", x.indexOf(g) >= 0);
			assertTrue("2.19", x.indexOf(h) >= 0);
			// {a, b, e} < {c,d} < {f, g} < {h}
			assertTrue("2.20", x.indexOf(b) < x.indexOf(c));
			assertTrue("2.21", x.indexOf(b) < x.indexOf(d));
			assertTrue("2.22", x.indexOf(a) < x.indexOf(c));
			assertTrue("2.23", x.indexOf(a) < x.indexOf(d));
			assertTrue("2.24", x.indexOf(e) < x.indexOf(c));
			assertTrue("2.25", x.indexOf(e) < x.indexOf(d));
			assertTrue("2.26", x.indexOf(c) < x.indexOf(f));
			assertTrue("2.27", x.indexOf(c) < x.indexOf(g));
			assertTrue("2.28", x.indexOf(d) < x.indexOf(f));
			assertTrue("2.29", x.indexOf(d) < x.indexOf(g));
			assertTrue("2.30", x.indexOf(f) < x.indexOf(h));
			assertTrue("2.31", x.indexOf(g) < x.indexOf(h));
			assertTrue("2.32", order.hasCycles);
			assertTrue("2.33", order.knots.length == 3);
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
			assertTrue("2.34", k1.size() == 3);
			assertTrue("2.35", k1.indexOf(a) >= 0);
			assertTrue("2.36", k1.indexOf(b) >= 0);
			assertTrue("2.37", k1.indexOf(e) >= 0);
			// knot 2
			assertTrue("2.38", k2.size() == 2);
			assertTrue("2.39", k2.indexOf(c) >= 0);
			assertTrue("2.40", k2.indexOf(d) >= 0);
			// knot 3
			assertTrue("2.41", k3.size() == 2);
			assertTrue("2.42", k3.indexOf(f) >= 0);
			assertTrue("2.43", k3.indexOf(g) >= 0);
		} catch (CoreException ex) {
			assertTrue("2.4.0", false);
		}
	}

}
