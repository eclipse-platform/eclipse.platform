/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.junit.Before;
import org.junit.Test;

public class TestXMLStructureCreator {

	Document fdoc;
	XMLStructureCreator fsc;

	public static class TestStream implements IEncodedStreamContentAccessor {
		String fString;

		public TestStream(String string) {
			fString = string;
		}

		@Override
		public InputStream getContents() throws CoreException {
				return new ByteArrayInputStream(fString.getBytes(StandardCharsets.UTF_16));
		}

		@Override
		public String getCharset() {
			return "UTF-16"; //$NON-NLS-1$
		}
	}

	@Before
	public void setUp() {
		fdoc = new Document();
		fsc = new XMLStructureCreator();
	}

	@Test
	public void test0() {
		TestStream s = new TestStream(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a attr1=\"&lt;b&gt;&lt;/b&gt;\"\nattr2=\"he\n ll\n o2\" attr3=\"hello3\"\nattr4=\"hello4\"><b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n<b2/>\n</a>\n"); //$NON-NLS-1$
		XMLChildren Tree = (XMLChildren) fsc.getStructure(s);

		XMLChildren ExpectedTree = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, XMLStructureCreator.ROOT_ID, "", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		// create Expected Tree
		XMLChildren parent = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "a<[1]", //$NON-NLS-1$
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a attr1=\"&lt;b&gt;&lt;/b&gt;\"\nattr2=\"he\n ll\n o2\" attr3=\"hello3\"\nattr4=\"hello4\"><b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n<b2/>\n</a>\n", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		parent.setName("a [1]"); //$NON-NLS-1$
		ExpectedTree.addChild(parent);
		parent.setParent(ExpectedTree);
		XMLChildren current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr1", "<b></b>", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "attr1" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr1"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr2", "he  ll  o2", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "attr2" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr2"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr3", "hello3", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "attr3" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr3"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr4", "hello4", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "attr4" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr4"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "b<[1]", //$NON-NLS-1$
				"<b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("b [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr", "battr", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "attr" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE, "attr2", "battr2", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "attr2" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE),
				fdoc, 0, 0);
		current.setName("attr2"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "c<[1]", "<c/>", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("c [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "b2<[1]", "<b2/>", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b2" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("b2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);

		checkTrees(Tree, ExpectedTree);
	}

	@Test
	public void test1() {
		TestStream s = new TestStream(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a>body_a_1\n  <b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\nbody_a_2\n  <b2>\n  </b2>\nbody_a_3\n</a>"); //$NON-NLS-1$
		XMLChildren Tree = (XMLChildren) fsc.getStructure(s);

		XMLChildren ExpectedTree = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, XMLStructureCreator.ROOT_ID, "", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		// create Expected Tree
		XMLChildren parent = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "a<[1]", //$NON-NLS-1$
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a>body_a_1\n  <b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\nbody_a_2\n  <b2>\n  </b2>\nbody_a_3\n</a>", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		parent.setName("a [1]"); //$NON-NLS-1$
		ExpectedTree.addChild(parent);
		parent.setParent(ExpectedTree);
		XMLChildren current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(1)", "body_a_1\n  ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "b<[1]", //$NON-NLS-1$
				"<b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\n", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("b [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(1)", "body_b_1\n    ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "c<[1]", //$NON-NLS-1$
				"<c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\n", //$NON-NLS-1$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("c [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(1)", "body_c_1\n      ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "d2<[1]", "<d2>body_d2\n      </d2>\n", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + "d2" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("d2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(1)", "body_d2\n      ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + "d2" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(2)", "\nbody_c_2\n    ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" //$NON-NLS-1$ //$NON-NLS-2$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(2)", "\nbody_b_2\n  ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(2)", "\nbody_a_2\n  ", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT, "b2<[1]", "<b2>\n  </b2>\n", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + "b2" + XMLStructureCreator.SIGN_SEPARATOR //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_ELEMENT),
				fdoc, 0, 0);
		current.setName("b2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT, "body_(3)", "\nbody_a_3\n", //$NON-NLS-1$ //$NON-NLS-2$
				(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" //$NON-NLS-1$
						+ XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT),
				fdoc, 0, 0);
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);

		checkTrees(Tree, ExpectedTree);
	}

	protected void checkTrees(XMLNode left, XMLNode right) {
		if (left != null && right != null) {
			// System.out.println(left.getName() + ", " + right.getName());
			// System.out.println(">" + left.getValue() + "<\n>" + right.getValue() + "<");
			assertTrue(left.testEquals(right));
			Object[] leftChildren = left.getChildren();
			Object[] rightChildren = right.getChildren();
			if (leftChildren != null && rightChildren != null) {
				if (leftChildren.length == rightChildren.length) {
					for (int i = 0; i < leftChildren.length; i++)
						checkTrees((XMLNode) leftChildren[i], (XMLNode) rightChildren[i]);
				} else
					fail();
			}
		} else if (((left == null) && (right != null)) || ((left != null) && (right == null))) {
			fail();
		}
	}
}
