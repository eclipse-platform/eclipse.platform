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
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.team.tests.ccvs.core.JUnitTestCase;

public class BasicTest extends JUnitTestCase {
	SameResultEnv env1;
	SameResultEnv env2;
	
	public BasicTest(String arg) {
		super(arg);
		env1 = new SameResultEnv(arg + "-checkout1");
		env2 = new SameResultEnv(arg + "-checkout2");
	}
	
	public BasicTest() {
		this("BasicTest");
	}

	public static void main(String[] args) {	
		run(BasicTest.class);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(BasicTest.class);
		return new CompatibleTestSetup(suite);
		//return new CompatibleTestSetup(new BasicTest("testReadOnly"));
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
	
	public void testAdd() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.writeToFile("proj2/d.txt",new String[]{"The file to be added","next"});
		env1.mkdirs("proj2/f2/f3");
		env1.mkdirs("proj2/f4/f5");
		env1.writeToFile("proj2/f4/f5/e.txt", new String[]{"Another file to be added","next"});
		
		env1.execute("add",EMPTY_ARGS,new String[]{"d.txt"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f2","f2/f3"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4/f5"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4/f5/e.txt"},"proj2");
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"});

		// Check the stuff out somewhere else to acctually check, that
		// the file has been accepted
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"});		
	}

	public void testAddUpdate() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"});		

		env1.writeToFile("proj2/d.txt",new String[]{"The file to be added","next"});
		env1.mkdirs("proj2/f2/f3");
		env1.mkdirs("proj2/f4/f5");
		env1.writeToFile("proj2/f4/f5/e.txt", new String[]{"Another file to be added","next"});
		
		env1.execute("add",EMPTY_ARGS,new String[]{"d.txt"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f2","f2/f3"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4/f5"},"proj2");
		env1.execute("add",EMPTY_ARGS,new String[]{"f4/f5/e.txt"},"proj2");
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"});

		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"});		

	}

	public void testRemove() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		
		env1.execute("remove",EMPTY_ARGS,new String[]{"a.txt"},"proj2");
		env1.execute("remove",EMPTY_ARGS,new String[]{"f1/c.txt"},"proj2");
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"});

		// Check the stuff out somewhere else to acctually check, that
		// the file has been accepted
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"});		
	}
	
	public void testRemoveRecusive() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"});		

		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		
		env1.execute("remove",EMPTY_ARGS,new String[0],"proj2");
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"});

		// Check the stuff out somewhere else to acctually check, that
		// the file has been accepted
		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"});		
	}
	
	public void testRoundRewrite() throws Exception {
				
		// Download content in two locations
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// change the file "proj1/folder1/c.txt" in env1 check it in
		// on the server
		env1.appendToFile("proj2/f1/c.txt", new String[] { "AppendIt" });
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		
		// assure that the file is different in env1 and env2
		// try {
		//	assertEqualsArrays(env1.readFromFile("proj2/f1/c.txt"),
		//				   	   env2.readFromFile("proj2/f1/c.txt"));
		//	throw new IllegalArgumentException("This is a failed Assertion");
		// } catch (AssertionFailedError e) {}
		
		// update env2 and make sure the changes are there
		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"},"");
		// assertEqualsArrays(env1.readFromFile("proj2/f1/c.txt"),
		//				   env2.readFromFile("proj2/f1/c.txt"));
	}
	
	public void testUpdateMinusN() throws Exception {	
		String[] fileContent1;
		
		fileContent1 = new String[]{"RandomNumber", Math.random() + ""};
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		
		env1.writeToFile("proj1/folder1/c.txt",fileContent1);
		
		env1.execute("ci",new String[]{"-n"},new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		env2.execute("update",EMPTY_ARGS,new String[]{"proj2"});		
	}
	
	public void testStatus() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("status",EMPTY_ARGS,new String[]{"proj2"});
		
		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });

		env1.execute("status",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("status",EMPTY_ARGS,new String[0],"proj2");

		env1.createRandomFile("proj2/d.txt");

		env1.execute("status",EMPTY_ARGS,new String[0],"proj2");
		env1.execute("status",EMPTY_ARGS,new String[]{"f1/b.txt"},"proj2");
		// env1.execute("status",localOptions,new String[]{"d.txt"},"proj2");
	}

	public void testLog() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("log",EMPTY_ARGS,new String[]{"proj2"});
		
		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });

		env1.execute("log",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("log",EMPTY_ARGS,new String[0],"proj2");

		env1.createRandomFile("proj2/d.txt");

		env1.execute("log",EMPTY_ARGS,new String[0],"proj2");
		env1.execute("log",EMPTY_ARGS,new String[]{"f1/b.txt"},"proj2");
	
	}	
	
	public void testBranchTag() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("tag",new String[]{"-b"},new String[]{"tag1","proj2"});
		
		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });

		env1.execute("tag",new String[]{"-b"},new String[]{"tag2","proj2"});

		env1.createRandomFile("proj2/d.txt");
		
		env1.deleteFile("proj2");
		
		// Try an commit and an add in the two different streams
		env1.execute("co",new String[]{"-r","tag1"},new String[]{"proj2"});
		env1.appendToFile("proj2/f1/b.txt", new String[] { "AppendItTwo" });
		env1.createRandomFile("proj2/d.txt");
		env1.execute("add",new String[0],new String[]{"d.txt"},"proj2");
		env1.execute("ci",new String[]{"-m","branch"},new String[]{"proj2"});
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","tag1"},new String[]{"proj2"});

		env2.execute("co",new String[]{"-r","tag2"},new String[]{"proj2"});
		env2.appendToFile("proj2/f1/b.txt", new String[] { "AppendItThree" });
		env2.createRandomFile("proj2/d.txt");
		env2.execute("add",new String[0],new String[]{"d.txt"},"proj2");
		env2.execute("ci",new String[]{"-m","branch"},new String[]{"proj2"});	
		env2.deleteFile("proj2");
		env2.execute("co",new String[]{"-r","tag2"},new String[]{"proj2"});
	}
		
	public void testBranchingWithLocalChanges() throws Exception {
		// Try to branch of a workspace with local changes
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		JUnitTestCase.waitMsec(1500);
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });
		env1.execute("tag",new String[]{"-b"},new String[]{"branch-with-changes","proj2"});
		env1.execute("update",new String[]{"-r", "branch-with-changes"},new String[]{"proj2"});
	}

	public void testTag() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("tag",EMPTY_ARGS,new String[]{"tag1","proj2"});
		
		env1.deleteFile("proj2/a.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });

		env1.execute("tag",EMPTY_ARGS,new String[]{"tag2","proj2"});
		env1.execute("tag",EMPTY_ARGS,new String[]{"tag2"},"proj2");

		env1.createRandomFile("proj2/d.txt");

		env1.execute("tag",EMPTY_ARGS,new String[]{"tag3"},"proj2");
		env1.execute("tag",EMPTY_ARGS,new String[]{"tag3","f1/b.txt"},"proj2");
		
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","tag1"},new String[]{"proj2"});
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","tag2"},new String[]{"proj2"});
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","tag3"},new String[]{"proj2"});
		
		// env1.execute("tag",localOptions,new String[]{"d.txt"},"proj2");
		
		env1.execute("update", new String[]{"-r","tag1"}, new String[]{"proj2"});
		env1.execute("update", new String[]{"-r","tag2"}, new String[]{"proj2"});
		env1.execute("update", new String[]{"-r","tag3"}, new String[]{"proj2"});
		env1.execute("update", new String[]{"-A"}, new String[]{"proj2"});
	}
	
	public void testRTag() throws Exception {
		
		// Checkout and tag the project
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("tag",EMPTY_ARGS,new String[]{"tag1","proj2"});
		env1.deleteFile("proj2");
		
		// Use rtag to tag the above tag as both a version and a branch
		env1.execute("rtag",new String[]{"-r", "tag1"},new String[]{"rtag1","proj2"});
		env1.execute("rtag",new String[]{"-b", "-r", "tag1"},new String[]{"btag1","proj2"});
		
		// Checkout the version and branch
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","rtag1"},new String[]{"proj2"});
		env1.deleteFile("proj2");
		env1.execute("co",new String[]{"-r","btag1"},new String[]{"proj2"});
	}
	
	public void testPrune() throws Exception {
				
		// Download content in two locations
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// change the file "proj1/folder1/c.txt" in env1 check it in
		// on the server
		env1.deleteFile("proj2/f1/b.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.execute("remove",EMPTY_ARGS,new String[0],"proj2");
		
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		env1.execute("update",new String[]{"-P"},new String[]{"proj2"},"");
		
		// update env2 and make sure the changes are there
		env2.execute("update",new String[]{"-P"},new String[]{"proj2"},"");

	}
	
	public void testPrune2() throws Exception {
				
		// Download content in two locations
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		// change the file "proj1/folder1/c.txt" in env1 check it in
		// on the server
		env1.deleteFile("proj2/f1/b.txt");
		env1.deleteFile("proj2/f1/c.txt");
		env1.execute("remove",EMPTY_ARGS,new String[0],"proj2");
		
		env1.execute("ci",new String[]{"-m","TestMessage"},new String[]{"proj2"},"");
		env1.execute("update",new String[]{"-P"},new String[]{},"proj2");
		
		// update env2 and make sure the changes are there
		env2.execute("update",new String[]{"-P"},new String[]{},"proj2");

	}
	
	public void testAdmin() throws Exception {
				
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		
		env1.execute("admin",new String[]{"-kb"},new String[]{"proj2/f1/b.txt"},"");
		env1.execute("update",EMPTY_ARGS,new String[]{"proj2"},"");
		
		env2.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
	}

	public void testDiff() throws Exception {
		
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("diff",EMPTY_ARGS,new String[]{"proj2"});
		
		env1.setIgnoreExceptions(true);
		
		env1.appendToFile("proj2/f1/c.txt",new String[] {"AppendIt2" });
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt" });
		
		env1.execute("diff",EMPTY_ARGS,new String[]{"proj2"});
		env1.execute("diff",EMPTY_ARGS,new String[0],"proj2");
		
		env1.createRandomFile("proj2/d.txt");

		env1.execute("diff",EMPTY_ARGS,new String[0],"proj2");
		env1.execute("diff",EMPTY_ARGS,new String[]{"f1/b.txt"},"proj2");
		// env1.execute("diff",localOptions,new String[]{"d.txt"},"proj2");

		env1.setIgnoreExceptions(false);
	}

	public void testReadOnly() throws Exception {
		
		// Checkout a read-only copy
		env1.execute("co",new String[]{"-r"},EMPTY_ARGS,new String[]{"proj2"},"");
		// Checkout and modify a writable copy
		env2.execute("co",new String[]{},EMPTY_ARGS,new String[]{"proj2"},"");
		env2.appendToFile("proj2/f1/c.txt",new String[] {"AppendIt2" });
		// Update the read only copy
		env1.execute("update",new String[] {"-r"},EMPTY_ARGS,new String[]{"proj2"},"");
		
		// Update the read-only copy to writable
		env1.execute("update",new String[] {},EMPTY_ARGS,new String[]{"proj2"},"");
	}
	
	public void testQuestionables() throws Exception {			
		env1.execute("co",EMPTY_ARGS,new String[]{"proj2"},"");
		env1.writeToFile("proj2/f2/d.txt", new String[]{"content"});
		env1.writeToFile("proj2/f3/f4/d.txt", new String[]{"content"});	
		env1.writeToFile("proj2/f5/f6/f7/d.txt", new String[]{"content"});	
		env1.execute("add",new String[0],new String[]{"f3"},"proj2");
		env1.execute("add",new String[0],new String[]{"f3/f4"},"proj2");
		env1.execute("update",new String[0],new String[]{"."},"proj2");
	}
	
	public void testImportWrappers() throws Exception {
		// Make the project empty
		env1.magicSetUpRepo("proj3",new String[]{"NoImportant.txt"});
		env2.deleteFile("proj3");
		
		// Create resouces and import them with the 
		// special wrapper
		env1.createRandomFile(new String[]{"a.txt","a.class","a.xxx"},"proj3");
		env1.execute("import",
					new String[]{"-W","*.txt -k 'kv'","-W","*.class -k 'b'","-I","*.xxx","-m","msg"},
					new String[]{"proj3","aTag","bTag"},
					"proj3");
		
		// download the server-version	
		env2.execute("co",EMPTY_ARGS,new String[]{"proj3"},"");
	}
	
	public void testImportIgnores() throws Exception {
		// Make the project empty
		env1.magicSetUpRepo("proj3",new String[]{"NoImportant.txt"});
		env2.deleteFile("proj3");
		
		// Create resouces and import them with the 
		// special wrapper
		env1.createRandomFile(new String[]{"a.txt","a.class","a.xxx"},"proj3");
		env1.execute("import",
					new String[]{"-I","*.xxx","-m","msg"},
					new String[]{"proj3","aTag","bTag"},
					"proj3");
		
		// download the server-version	
		env2.execute("co",EMPTY_ARGS,new String[]{"proj3"},"");
	}
	
	private String toGMTString(Date d) {
		return d.toGMTString();
	}
	
	public void testDate() throws Exception {
		
		// IMPOTANT:
		// Do not make tests with relative dates, because the times coming back form the server will 
		// sometimes differ from the reference-client to the eclipse-client due to the time-delay in calling

		Date beforeChange;
		Date firstChange;
		Date secondChange;
		
		env1.execute("co",new String[0],new String[]{"proj2"});
		
		// Change the file "a.txt" and record the times you are doing that at
		waitMsec(1100); // here we wait for the import to finish
		beforeChange = GregorianCalendar.getInstance().getTime();

		waitMsec(1100);
	
		env1.appendToFile("proj2/a.txt",new String[] { "AppendIt" });
		env1.execute("ci",new String[]{"-m","msg"},new String[]{"proj2"});
		firstChange = GregorianCalendar.getInstance().getTime();

		waitMsec(1100);
			
		env1.appendToFile("proj2/a.txt",new String[] { "AppendIt2" });
		env1.appendToFile("proj2/f1/b.txt",new String[] { "AppendIt2" });
		env1.execute("ci",new String[]{"-m","msg"},new String[]{"proj2"});
		secondChange = GregorianCalendar.getInstance().getTime();

		waitMsec(1100);		

		env1.deleteFile("proj2");
		
		// Now check the project at different times out
		env1.execute("co",new String[]{"-D",toGMTString(beforeChange)},new String[]{"proj2"});
		env1.deleteFile("proj2");
		
		env1.execute("co",new String[]{"-D",toGMTString(firstChange)},new String[]{"proj2"});
		env1.deleteFile("proj2");
		
		env1.execute("co",new String[]{"-D",toGMTString(secondChange)},new String[]{"proj2"});
		env1.deleteFile("proj2");
		
		// Now do some updates to look if update -D works
		env1.execute("co",new String[0],new String[]{"proj2"});
		env1.execute("update",new String[]{"-D",toGMTString(beforeChange)},new String[]{"proj2"});
		env1.execute("update",new String[]{"-D",toGMTString(firstChange)},new String[]{"proj2"});
		env1.execute("update",new String[]{"-D",toGMTString(secondChange)},new String[]{"proj2"});

		// We look if the parameter -a is working
		env1.execute("update",new String[]{"-D",toGMTString(beforeChange),"-A"},new String[]{"proj2"});
		env1.execute("update",new String[]{"-D",toGMTString(secondChange)},new String[]{"proj2"});
		env1.deleteFile("proj2");
		
		// We get try to merge changes from different dates
		env1.execute("co",new String[0],new String[]{"proj2"});
		env1.appendToFile("proj2/a.txt", new String[] { "This is the world ..." });
		env1.execute("update",new String[]{"-D",toGMTString(beforeChange)},new String[]{"proj2"});
		env1.appendToFile("proj2/a.txt", new String[] {"... which constantly changes" });
		env1.execute("update",new String[]{"-A"},new String[]{"proj2"});
		// Change something to be able to commit
		env1.appendToFile("proj2/a.txt", new String[] { "... and the changes are approved" });
		env1.execute("ci",new String[]{"-m","msg"},new String[]{"proj2"});
		
	}
}

