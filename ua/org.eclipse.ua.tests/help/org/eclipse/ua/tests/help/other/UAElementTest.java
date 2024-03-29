/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.junit.jupiter.api.Test;

public class UAElementTest {

	private UAElement parent1;
	private UAElement parent2;
	private UAElement child1;
	private UAElement child2;
	private UAElement child3;
	private UAElement child4;
	private UAElement grandchild1;

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testSimpleUAElement() {
		UAElement element = new UAElement("name1");
		assertEquals("name1", element.getElementName());
		assertThat(element.getChildren()).isEmpty();
		Object topicChildren = element.getChildren(Topic.class);
		assertTrue(topicChildren instanceof Topic[]);
		assertThat(((Topic[]) topicChildren)).isEmpty();
		assertNull(element.getParentElement());
		assertNull(element.getAttribute("a1"));
		assertTrue(element.equals(element));
		assertFalse(element.equals(null));
		assertFalse(element.equals("A string"));
	}

	@Test
	public void testAttributes() {
		UAElement element = new UAElement("name1");
		element.setAttribute("a1", "v1");
		assertEquals("v1", element.getAttribute("a1"));
		assertNull(element.getAttribute("a2"));
		assertNull(element.getAttribute("A1"));
		element.setAttribute("a1", "v2");
		element.setAttribute("a2", "v1");
		assertEquals("v2", element.getAttribute("a1"));
		assertEquals("v1", element.getAttribute("a2"));
		element.setAttribute("a1", null);
		assertNull(element.getAttribute("a1"));
	}

	@Test
	public void testChildInsertionDeletion() {
		initializeElements();
		parent1.appendChild(child1);
		IUAElement[] children = parent1.getChildren();
		assertThat(children).hasSize(1);
		assertEquals("c1", ((UAElement)children[0]).getElementName());
		assertEquals(parent1, ((UAElement)children[0]).getParentElement());
		assertEquals("c1", ((UAElement)children[0]).getAttribute("id"));
		parent1.appendChild(child2);
		children = parent1.getChildren();
		assertThat(children).hasSize(2);
		assertEquals("c2", ((UAElement)children[1]).getElementName());
		parent1.insertBefore(child3, child2);
		children = parent1.getChildren();
		assertThat(children).hasSize(3);
		assertEquals("c3", ((UAElement)children[1]).getElementName());
		parent1.removeChild(child1);
		children = parent1.getChildren();
		assertThat(children).hasSize(2);
		assertEquals("c3", ((UAElement)children[0]).getElementName());
		parent1.appendChildren(new IUAElement[] {child1, child4});
		children = parent1.getChildren();
		assertThat(children).hasSize(4);
		assertEquals("c1", ((UAElement)children[2]).getElementName());
		assertEquals("c4", ((UAElement)children[3]).getElementName());
	}

	@Test
	public void testDuplicateChildren() {
		initializeElements();
		parent1.appendChild(child1);
		parent1.appendChild(child2);
		parent1.appendChild(child2);
		parent1.appendChild(child1);
		IUAElement[] children = parent1.getChildren();
		assertThat(children).hasSize(4);
		assertEquals("c1", ((UAElement)children[0]).getElementName());
		assertEquals("c2", ((UAElement)children[1]).getElementName());
		assertEquals("c2", ((UAElement)children[2]).getElementName());
		assertEquals("c1", ((UAElement)children[3]).getElementName());
	}

	@Test
	public void testGrandchildren() {
		initializeElements();
		child1.appendChild(grandchild1);
		parent2.appendChild(child1);
		UAElement firstChild = (UAElement) parent2.getChildren()[0];
		assertThat(firstChild.getChildren()).hasSize(1);
		UAElement firstGrandchild = (UAElement) firstChild.getChildren()[0];
		assertEquals("g1", firstGrandchild.getElementName());
		assertEquals(firstGrandchild.getParentElement(), firstChild);
	}

	@Test
	public void testMultipleParents() {
		initializeElements();
		child1.appendChild(grandchild1);
		parent1.appendChild(child1);
		parent2.appendChild(child1);
		UAElement firstChild1 = (UAElement) parent1.getChildren()[0];
		assertEquals(firstChild1.getParentElement(), parent1);
		UAElement firstChild2 = (UAElement) parent2.getChildren()[0];
		assertEquals(firstChild2.getParentElement(), parent2);
	}

	@Test
	public void testMultipleGrandParents() {
		initializeElements();
		child1.appendChild(grandchild1);
		parent1.appendChild(child1);
		parent2.appendChild(child1);
		UAElement firstChild1 = (UAElement) parent1.getChildren()[0];
		UAElement firstGrandchild1 = (UAElement) firstChild1.getChildren()[0];
		assertEquals(firstGrandchild1.getParentElement(), firstChild1);
		assertEquals(firstChild1.getParentElement(), parent1);
		UAElement firstChild2 = (UAElement) parent2.getChildren()[0];
		UAElement firstGrandchild2 = (UAElement) firstChild2.getChildren()[0];
		assertEquals(firstGrandchild2.getParentElement(), firstChild2);
		assertEquals(firstChild2.getParentElement(), parent2);
	}

	/*
	 * commented out because the assumptions made are not valid
	*/

	private void initializeElements() {
		parent1 = new UAElement("p1");
		parent2 = new UAElement("p2");
		child1 = new UAElement("c1");
		child1.setAttribute("id", "c1");
		child2 = new UAElement("c2");
		child2.setAttribute("id", "c2");
		child3 = new UAElement("c3");
		child3.setAttribute("id", "c3");
		child4 = new UAElement("c4");
		grandchild1 = new UAElement("g1");
		grandchild1.setAttribute("id", "g1");
	}


}
