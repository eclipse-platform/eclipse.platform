/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.watson;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Stack;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeIterator;
import org.eclipse.core.internal.watson.IElementContentVisitor;
import org.eclipse.core.internal.watson.IElementTreeData;
import org.eclipse.core.runtime.IPath;
import org.junit.Test;

/**
 * Unit tests for <code>ElementTreeIterator</code>.
 */
public class ElementTreeIteratorTest {

	static void setupElementTree(ElementTree tree, int num) {
		final IElementTreeData data = new IElementTreeData() {
			@Override
			public Object clone() {
				try {
					return super.clone();
				} catch (CloneNotSupportedException e) {
				}
				return null;
			}
		};
		IPath sol = IPath.ROOT.append("sol");
		tree.createElement(sol, data);
		for (int p = 0; p < num; p++) {
			IPath proj = sol.append("proj" + p);
			tree.createElement(proj, data);
			for (int k = 0; k < num; k++) {
				IPath folder = proj.append("folder" + k);
				tree.createElement(folder, data);
				for (int c = 0; c < num; c++) {
					IPath file = folder.append("file" + c);
					tree.createElement(file, data);
				}
			}
		}
	}

	@Test
	public void testConcurrentModification() {
		//the dining detectives problem
		ElementTree baseTree = new ElementTree();
		int n = 3;
		setupElementTree(baseTree, n);
		baseTree.immutable();
		final ElementTree tree = baseTree.newEmptyDelta();
		modifyTree(tree);
		final ArrayList<Object> elts = new ArrayList<>();
		final IElementContentVisitor visitor = (tree1, requestor, info) -> {
			elts.add(info);
			return true;
		};
		Thread reader = new Thread((Runnable) () -> {
			for (int i = 0; i < 80000; i++) {
				new ElementTreeIterator(tree, IPath.ROOT).iterate(visitor);
			}
		}, "Holmes (reader)");
		Thread writer = new Thread((Runnable) () -> {
			for (int i = 0; i < 1000; i++) {
				modifyTree(tree);
				recursiveDelete(tree, IPath.ROOT);
				setupElementTree(tree, 3);
			}
		}, "Doyle (writer)");

		reader.start();
		writer.start();
		//wait for both threads to finish
		try {
			reader.join();
			writer.join();
		} catch (InterruptedException e) {
		}
	}

	@Test
	public void testContentIterator() {
		ElementTree tree = new ElementTree();
		int n = 3;
		setupElementTree(tree, n);
		final ArrayList<IPath> elts = new ArrayList<>();
		IElementContentVisitor elementVisitor = (tree1, requestor, info) -> {
			elts.add(requestor.requestPath());
			return true;
		};
		new ElementTreeIterator(tree, IPath.ROOT).iterate(elementVisitor);
		assertEquals("1", 2 + n + n * n + n * n * n, elts.size());

		elts.clear();
		IPath innerElement = IPath.ROOT.append("sol").append("proj1");
		new ElementTreeIterator(tree, innerElement).iterate(elementVisitor);
		assertEquals("2", 1 + n + n * n, elts.size());
	}

	/**
	 * Method deleteChild.
	 * @param path
	 */
	void recursiveDelete(ElementTree tree, IPath path) {
		IPath[] children = tree.getChildren(path);
		for (IPath element : children) {
			recursiveDelete(tree, element);
		}
		tree.deleteElement(path);
	}

	protected void modifyTree(ElementTree tree) {
		class MyStack extends Stack<IPath> {
			/**
			 * All serializable objects should have a stable serialVersionUID
			 */
			private static final long serialVersionUID = 1L;

			public void pushAll(IPath[] array) {
				for (IPath element : array) {
					push(element);
				}
			}
		}
		MyStack toModify = new MyStack();
		IPath[] children = tree.getChildren(IPath.ROOT);
		toModify.pushAll(children);
		while (!toModify.isEmpty()) {
			IPath visit = toModify.pop();
			tree.openElementData(visit);
			toModify.pushAll(tree.getChildren(visit));
		}
	}
}
