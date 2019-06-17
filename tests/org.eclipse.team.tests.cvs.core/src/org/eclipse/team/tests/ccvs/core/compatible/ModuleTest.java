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

public class ModuleTest extends JUnitTestCase {
	SameResultEnv env1;
	SameResultEnv env2;
	
	public ModuleTest(String arg) {
		super(arg);
		env1 = new SameResultEnv(arg + "checkout1");
		env2 = new SameResultEnv(arg + "checkout2");
	}

	public static void main(String[] args) {	
		run(ModuleTest.class);
	}

	@Override
	public void setUp() throws Exception {
		env1.setUp();
		env2.setUp();

		// Set the project to the content we need ...
		env1.magicDeleteRemote("CVSROOT/modules");
		env1.magicDeleteRemote("CVSROOT/modules,v");
		env1.magicSetUpRepo("proj2",new String[]{"a.txt","f1/b.txt","f1/c.txt","f2/d.txt","f2/f3/e.txt"});
		env2.deleteFile("proj2");
	}
	
	@Override
	public void tearDown() throws Exception {
		env1.tearDown();
		env2.tearDown();
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ModuleTest.class);
		return new CompatibleTestSetup(suite);
	}	
	
	private void setUpModuleFile(String[] change) throws Exception {
		
		// Write the modules-file
		env1.execute("co",EMPTY_ARGS,new String[]{"CVSROOT"});
		env1.writeToFile("CVSROOT/modules",change);
		
		// Send it up to the server
		env1.execute("add",new String[]{"-m","m"},new String[]{"modules"},"CVSROOT");
		env1.execute("ci",new String[]{"-m","m"},new String[]{"CVSROOT"});
		env1.deleteFile("CVSROOT");		
	}
	
	public void testSimpleModule() throws Exception {
		setUpModuleFile(new String[]{"mod1 proj2"});
		env1.execute("co",EMPTY_ARGS,new String[]{"mod1"});
		env1.appendToFile("mod1/a.txt", new String[] { "Append" });
		env1.execute("ci",new String[]{"-m","m"},new String[]{"mod1"});
		env1.execute("update",EMPTY_ARGS,new String[]{"mod1"});
	}

	public void testCompositeModule() throws Exception {
		setUpModuleFile(new String[]{	"mod1-f1 proj2/f1",
										"mod1-f2 proj2/f2",
										"mod1f &mod1-f1 &mod1-f2"});
		
		env1.execute("co",EMPTY_ARGS,new String[]{"mod1f"});
		env1.appendToFile("mod1f/mod1-f1/b.txt", new String[] { "Append" });
		env1.execute("ci",new String[]{"-m","m"},new String[]{"mod1f"});
		env1.execute("update",EMPTY_ARGS,new String[]{"mod1f"});
	}

	public void testCompositeAliasModule() throws Exception {
		setUpModuleFile(new String[]{"mod1-f1 proj2/f1",
										"mod1t proj2/f1 b.txt",
										"mod1-f2 &proj2/f2 &mod1t",
										"mod1f -a mod1-f1 mod1-f2"});
		
		env1.execute("co",EMPTY_ARGS,new String[]{"mod1f"});
		env1.appendToFile("mod1-f1/b.txt", new String[] { "Append" });
		env1.execute("ci",new String[]{"-m","m"},new String[]{"mod1-f1","mod1-f2"});
		env1.execute("update",EMPTY_ARGS,new String[]{"mod1-f1","mod1-f2"});
	}
}

