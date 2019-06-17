/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.compatible;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.team.tests.ccvs.core.JUnitTestCase;

public class ConflictTest extends JUnitTestCase {
	SameResultEnv env1;
	SameResultEnv env2;
	
	public ConflictTest() {
		this(null);
	}
	
	public ConflictTest(String arg) {
		super(arg);
		env1 = new SameResultEnv(arg + "checkout1");
		env2 = new SameResultEnv(arg + "checkout2");
	}

	@Override
	public void setUp() throws Exception {
		env1.setUp();
		env2.setUp();

		// Set the project to the content we need ...
		env1.magicSetUpRepo("proj2",new String[]{"a.txt","f1/b.txt","f1/c.txt"});
		env2.deleteFile("proj2");
	}
	
	@Override
	public void tearDown() throws Exception {
		env1.tearDown();
		env2.tearDown();
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ConflictTest.class);
		//return new CompatibleTestSetup(new ConflictTest("testSimpleConflict"));
		return new CompatibleTestSetup(suite);
	}
	
	public void testSimpleConflict() throws Exception {
		// Download content in two locations
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// change the file in both directories in a different way
		env1.appendToFile("proj2/f1/c.txt", new String[] { "AppendIt This" });
		env2.appendToFile("proj2/f1/c.txt", new String[] { "AppendIt That" });
		
		// commit changes of the first
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		
		// load the changes into the changed file
		// and submit the merge
		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// commit must fail because we have a merged conflict which has not been
		// edited.
		env2.setIgnoreExceptions(true);
		env2.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		env2.setIgnoreExceptions(false);
		
		// Make a change to the file in order to let the cvs-client know
		// that we solved the confilict
		env2.appendToFile("proj2/f1/c.txt", new String[] { "That's allright" });
		env2.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
	}
	
	public void testMergedUpdate() throws Exception {
		// Download content in two locations
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// change the file in both directories in a different way so that 
		// can be merged without conflicts
		env1.prefixToFile("proj2/f1/c.txt", new String[] { "AppendIt at top" });
		env2.appendToFile("proj2/f1/c.txt", new String[] { "AppendIt at bottom" });
		
		// commit changes of the first
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		
		// changes should be merged
		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");	
	}
}
