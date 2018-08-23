/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.cvsresources;

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProjectSetCapability;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.filesystem.CVSURI;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class CVSURITest extends EclipseTest {

	public CVSURITest() {
		super();
	}

	public CVSURITest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(CVSURITest.class);
		return new CVSTestSetup(suite);
	}
	
	public void testURIParse() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user~host.here_!root!path/project/path");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user@host.here:/root/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), null);
		assertEquals(cvsUri.toURI(), uri);
	}
	
	public void testURIParse2() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user_password~host.here_1234!root!path/project/path");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user:password@host.here:1234/root/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), null);
		assertEquals(cvsUri.toURI(), uri);
	}
	
	public void testURIParse3() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user_password~host.here_1234!root!path/project/path?version=v1");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user:password@host.here:1234/root/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("v1", CVSTag.VERSION));
		assertEquals(cvsUri.toURI(), uri);
	}
	
	public void testURIParse4() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user_password~host.here_1234!root!path/project/path?branch=b1");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user:password@host.here:1234/root/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("b1", CVSTag.BRANCH));
		assertEquals(cvsUri.toURI(), uri);
	}
	
	public void testURIParse5() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user_password~host.here_1234!root!path/project/path?revision=1.5");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user:password@host.here:1234/root/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getRevision(), "1.5");
		assertEquals(cvsUri.toURI(), uri);
	}
	
	public void testURIParse6() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_user_pass~~word~host.here_1234!the__root!path!!/project/path?revision=1.5");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/project/path", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:user:pass~word@host.here:1234/the_root/path!");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getRevision(), "1.5");
		assertEquals(cvsUri.toURI(), uri);
	}
	
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=231190
	public void testURIParse7() throws URISyntaxException, CVSException {
		URI uri = new URI("cvs://_pserver_username_password~testserver.acme.com_!root/");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("/", cvsUri.getPath().toString());
		// location string taken from the bug
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver;username=username;password=password:testserver.acme.com/root");
		assertEquals("pserver", location.getMethod().getName());
		assertEquals("username", location.getUsername());
		assertEquals(ICVSRepositoryLocation.USE_DEFAULT_PORT, location.getPort()); 
		assertEquals("testserver.acme.com", location.getHost());
		assertEquals("/root", location.getRootDirectory());
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(null, cvsUri.getTag());
		assertEquals(cvsUri.toURI(), uri);
	}

	// CVS SCM URL tests, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=326926
	public void testScmUri1() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:module;tag=tag");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("tag", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, "project");
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,module,project,tag", refString);
	}

	public void testScmUri2() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:module;version=version");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("version", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, "project");
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,module,project,version", refString);
	}

	public void testScmUri3() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;version=version;project=project1");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("version", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, "project2");
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,project2,version", refString);
	}

	public void testScmUri4() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;version=version;project=project1");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("version", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,project1,version", refString);
	}

	public void testScmUri5() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;project=project1;version=version");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("version", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,project1,version", refString);
	}

	public void testScmUri6() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;tag=tag");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("tag", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,module,tag", refString);
	}

	public void testScmUri7() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;version=version");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("version", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,module,version", refString);
	}
	
	public void testScmUri8() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;project=");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), CVSTag.DEFAULT);

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,module", refString);
	}

	public void testScmUri9() throws CVSException {
		URI uri = URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), CVSTag.DEFAULT);

		String refString = new CVSProjectSetCapability().asReference(uri, "project");
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,project", refString);
	}

	public void testScmUri10() throws URISyntaxException, CVSException {
		URI uri = new URI("scm:cvs:pserver:anonymous:@host.com:/cvsroot/path:module");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:anonymous:@host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), CVSTag.DEFAULT);

		String refString = new CVSProjectSetCapability().asReference(uri, "project");
		assertEquals("1.0,:pserver:anonymous:@host.com:/cvsroot/path,module,project", refString);
	}

	public void testScmUri11() throws URISyntaxException, CVSException {
		URI uri = new URI("scm:cvs:pserver:username@host.com:/cvsroot/path:module");
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:username@host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), CVSTag.DEFAULT);

		String refString = new CVSProjectSetCapability().asReference(uri, "project");
		assertEquals("1.0,:pserver:username@host.com:/cvsroot/path,module,project", refString);
	}
	
	public void testScmUri12() throws URISyntaxException {
		URI uri = new URI("notScm:cvs:pserver:username@host.com:/cvsroot/path:module");
		try {
			new CVSProjectSetCapability().asReference(uri, "project");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	public void testScmUri13() throws URISyntaxException {
		URI uri = new URI("scm:cvs:pserver:username@host.com:/cvsroot/path:");
		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertNull(refString);
	}
	
	public void testScmUri14() {
		try {
			URI.create("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;tag=\"tag\"");
		} catch (IllegalArgumentException e) {
			// expected, " are not allowed in a URI reference
		}
	}
	
	public void testScmUri15() throws CVSException {
		// ScmUrlImportDescription can handle " in Strings expected to be URI refs
		ScmUrlImportDescription description = new ScmUrlImportDescription("scm:cvs:pserver:host.com:/cvsroot/path:path/to/module;tag=\"tag\"", null);
		URI uri = description.getUri();
		CVSURI cvsUri = CVSURI.fromUri(uri);
		assertEquals("path/to/module", cvsUri.getPath().toString());
		CVSRepositoryLocation location = CVSRepositoryLocation.fromString(":pserver:host.com:/cvsroot/path");
		assertEquals(cvsUri.getRepository().getLocation(false), location.getLocation(false));
		assertEquals(cvsUri.getTag(), new CVSTag("tag", CVSTag.VERSION));

		String refString = new CVSProjectSetCapability().asReference(uri, null);
		assertEquals("1.0,:pserver:host.com:/cvsroot/path,path/to/module,module,tag", refString);
	}
}
