/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.examples.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.*;

import org.eclipse.jface.text.Document;

import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.examples.xml.XMLChildren;
import org.eclipse.compare.examples.xml.XMLNode;
import org.eclipse.compare.examples.xml.XMLStructureCreator;
import org.eclipse.core.runtime.CoreException;

public class TestXMLStructureCreator extends TestCase {
	
	Document fdoc;
	XMLStructureCreator fsc;
	
	public class TestStream implements IEncodedStreamContentAccessor {
		String fString;
		
		public TestStream(String string) {
			fString= string;
		}
		
		@Override
		public InputStream getContents() throws CoreException {
			try {
				return new ByteArrayInputStream(fString.getBytes("UTF-16")); //$NON-NLS-1$
			} catch (Exception e) {
				return new ByteArrayInputStream(fString.getBytes());
			}
		}
		
		@Override
		public String getCharset() {
			return "UTF-16"; //$NON-NLS-1$
		}
	}
	
	public TestXMLStructureCreator(String name) {
		super(name);
	}

	@Override
	protected void setUp() {
		System.out.println("TestXMLStructureCreator.name()==" + getName()); //$NON-NLS-1$
		fdoc = new Document();
		fsc = new XMLStructureCreator();
	}
	
	@Override
	protected void tearDown() throws Exception {
		//remove set-up
	}
	
	public static Test suite() {
		return new TestSuite(TestXMLStructureCreator.class);
	}

	public void test0() {
		TestStream s= new TestStream("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a attr1=\"&lt;b&gt;&lt;/b&gt;\"\nattr2=\"he\n ll\n o2\" attr3=\"hello3\"\nattr4=\"hello4\"><b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n<b2/>\n</a>\n"); //$NON-NLS-1$
		XMLChildren Tree= (XMLChildren) fsc.getStructure(s);

		XMLChildren ExpectedTree = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,XMLStructureCreator.ROOT_ID, "",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$
		//create Expected Tree
		XMLChildren parent = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"a<[1]","<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a attr1=\"&lt;b&gt;&lt;/b&gt;\"\nattr2=\"he\n ll\n o2\" attr3=\"hello3\"\nattr4=\"hello4\"><b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n<b2/>\n</a>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		parent.setName("a [1]"); //$NON-NLS-1$
		ExpectedTree.addChild(parent);
		parent.setParent(ExpectedTree);
		XMLChildren current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr1","<b></b>",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "attr1" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("attr1"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr2","he  ll  o2",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "attr2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("attr2"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr3","hello3",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "attr3" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("attr3"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr4","hello4",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "attr4" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("attr4"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"b<[1]","<b attr=\n\"battr\" attr2=\"battr2\">\n<c/>\n</b>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR +"b" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("b [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr","battr",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "attr" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("attr"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ATTRIBUTE,"attr2","battr2",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "attr2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ATTRIBUTE), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("attr2"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"c<[1]","<c/>",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("c [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"b2<[1]","<b2/>",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR +"b2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("b2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		
		checkTrees(Tree,ExpectedTree);
	}
		
	public void test1() {
		TestStream s= new TestStream("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a>body_a_1\n  <b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\nbody_a_2\n  <b2>\n  </b2>\nbody_a_3\n</a>"); //$NON-NLS-1$
		XMLChildren Tree= (XMLChildren) fsc.getStructure(s);

		XMLChildren ExpectedTree = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,XMLStructureCreator.ROOT_ID, "",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$
		//create Expected Tree
		XMLChildren parent = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"a<[1]","<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<a>body_a_1\n  <b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\nbody_a_2\n  <b2>\n  </b2>\nbody_a_3\n</a>",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		parent.setName("a [1]"); //$NON-NLS-1$
		ExpectedTree.addChild(parent);
		parent.setParent(ExpectedTree);
		XMLChildren current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(1)","body_a_1\n  ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"b<[1]","<b>body_b_1\n    <c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\nbody_b_2\n  </b>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("b [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(1)","body_b_1\n    ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"c<[1]","<c>body_c_1\n      <d2>body_d2\n      </d2>\nbody_c_2\n    </c>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR +"a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("c [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(1)","body_c_1\n      ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"d2<[1]","<d2>body_d2\n      </d2>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + "d2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		current.setName("d2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = current;
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(1)","body_d2\n      ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + "d2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		current.setName("body (1)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(2)","\nbody_c_2\n    ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + "c" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(2)","\nbody_b_2\n  ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		parent = (XMLChildren) parent.getParent();
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(2)","\nbody_a_2\n  ",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_ELEMENT,"b2<[1]","<b2>\n  </b2>\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + "b2" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_ELEMENT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		current.setName("b2 [1]"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);
		current = new XMLChildren(XMLStructureCreator.TYPE_TEXT,"body_(3)","\nbody_a_3\n",(XMLStructureCreator.ROOT_ID + XMLStructureCreator.SIGN_SEPARATOR + "a" + XMLStructureCreator.SIGN_SEPARATOR + XMLStructureCreator.SIGN_TEXT), fdoc, 0, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		current.setName("body (2)"); //$NON-NLS-1$
		parent.addChild(current);
		current.setParent(parent);

		checkTrees(Tree,ExpectedTree);
	}
		
	protected void checkTrees(XMLNode left, XMLNode right) {
		if (left != null && right != null) {
			//System.out.println(left.getName() + ", " + right.getName());
			//System.out.println(">" + left.getValue() + "<\n>" + right.getValue() + "<");
			assertTrue(left.testEquals(right));
			Object[] leftChildren = left.getChildren();
			Object[] rightChildren = right.getChildren();
			if (leftChildren != null && rightChildren != null) {
				if (leftChildren.length == rightChildren.length) {
					for (int i=0; i<leftChildren.length; i++)
						checkTrees((XMLNode) leftChildren[i], (XMLNode) rightChildren[i]);
				} else
					assertTrue(false);
			}
		} else if ( ((left == null) && (right != null)) || ((left != null) && (right == null)) ) {
			assertTrue(false);
		}
	}
}

