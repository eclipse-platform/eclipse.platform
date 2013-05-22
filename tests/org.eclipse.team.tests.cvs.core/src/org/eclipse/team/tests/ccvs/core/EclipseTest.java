/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.Policy;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Import;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.connection.CVSCommunicationException;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSServerException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.SyncFileChangeListener;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelReplaceOperation;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelUpdateOperation;
import org.eclipse.team.internal.ccvs.ui.operations.AddOperation;
import org.eclipse.team.internal.ccvs.ui.operations.BranchOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CVSOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CheckoutSingleProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CommitOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ITagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ReplaceOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation;
import org.eclipse.team.internal.ccvs.ui.operations.ShareProjectOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagInRepositoryOperation;
import org.eclipse.team.internal.ccvs.ui.operations.TagOperation;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOperation;
import org.eclipse.team.internal.ccvs.ui.operations.WorkspaceResourceMapper;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.ui.TeamOperation;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.tests.resources.ResourceTest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.decorators.DecoratorManager;

public class EclipseTest extends ResourceTest { 

	private static final int LOCK_WAIT_TIME = 1000;
    private static final String CVS_TEST_LOCK_FILE = ".lock";
    private static final String CVS_TEST_LOCK_PROJECT  = "cvsTestLock";
    protected static IProgressMonitor DEFAULT_MONITOR = new NullProgressMonitor();
	protected static final int RANDOM_CONTENT_SIZE = 3876;
	protected static String eol = System.getProperty("line.separator");
	private static boolean modelSync = true;
	private static final long LOCK_EXPIRATION_THRESHOLD = 1000 * 60 * 10; // 10 minutes
	private static final int MAX_LOCK_ATTEMPTS = 60 * 30; // 30 minutes

	private static final int MAX_RETRY_DELETE = 120;


	private String lockId;


	/**
	 * Removes a resource. Retries if deletion failed (e.g. because something still locks the
	 * resource).
	 * 
	 * @param resource the resource to delete
	 * @param updateFlags bit-wise or of update flag constants ( {@link #FORCE},
	 *            {@link #KEEP_HISTORY}, {@link #ALWAYS_DELETE_PROJECT_CONTENT}, and
	 *            {@link #NEVER_DELETE_PROJECT_CONTENT})
	 * @param progressMonitor the progress monitor
	 * @throws CoreException if operation failed
	 */
	public static void delete(IResource resource, int updateFlags, IProgressMonitor progressMonitor) throws CoreException {
		for (int i= 0; i < MAX_RETRY_DELETE; i++) {
			try {
				resource.delete(updateFlags, progressMonitor);
				i= MAX_RETRY_DELETE;
			} catch (CoreException e) {
				if (i == MAX_RETRY_DELETE - 1) {
					CVSProviderPlugin.log(e);
					throw e;
				}
				try {
					IStatus status= new Status(IStatus.ERROR, CVSProviderPlugin.ID, 0, "Error deleting resource", new IllegalStateException("sleep before retrying delete() for "
							+ resource.getLocationURI()));
					CVSProviderPlugin.log(status);
					Thread.sleep(1000); // give other threads time to close the file
				} catch (InterruptedException e1) {
				}
			}
		}
	}


	public static Test suite(Class c) {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(c);
			return new CVSTestSetup(suite);
		} else {
			try {
				return new CVSTestSetup((Test)c.getConstructor(new Class[] { String.class }).newInstance(new Object[] {testName}));
			} catch (Exception e) {
				fail(e.getMessage());
				// Above will throw so below is never actually reached
				return null;
			}
		}
	}
	
	public static boolean isModelSyncEnabled() {
		return modelSync;
	}
	
	public static void setModelSync(boolean modelSync) {
		EclipseTest.modelSync = modelSync;
	}
	
	public EclipseTest() {
		super();
		if (eol == null) eol = "\n";
	}

	public EclipseTest(String name) {
		super(name);
		if (eol == null) eol = "\n";
	}

	public ICVSRemoteResource getRemoteTree(IResource resource, CVSTag tag, IProgressMonitor progress) throws TeamException {
		return CVSWorkspaceRoot.getRemoteTree(resource, tag, false /* cache file contents hint */, IResource.DEPTH_INFINITE, progress);
	}
	
	/*
	 * Get the resources for the given resource names
	 */
	public IResource[] getResources(IContainer container, String[] hierarchy) {
		IResource[] resources = new IResource[hierarchy.length];
		for (int i=0;i<resources.length;i++) {
			resources[i] = container.findMember(hierarchy[i]);
			if (resources[i] == null) {
				resources[i] = buildResources(container, new String[] {hierarchy[i]})[0];
			}
		}
		return resources;
	}
	
	/**
	 * Add the resources to an existing container and upload them to CVS
	 */
	public IResource[] addResources(IContainer container, String[] hierarchy, boolean checkin) throws CoreException, TeamException {
		IResource[] newResources = buildResources(container, hierarchy, false);
		addResources(newResources);
		if (checkin) commitResources(newResources, IResource.DEPTH_ZERO);
		return newResources;
	}
	
	protected void addResources(IResource[] newResources) throws CoreException {
		if (newResources.length == 0) return;
		ResourceMapping[] mappings = asResourceMappers(newResources, IResource.DEPTH_INFINITE);
        add(mappings);
	}

    protected void add(ResourceMapping[] mappings) throws CVSException {
        executeHeadless(new AddOperation(null, mappings));
    }
	
	/**
	 * Perform a CVS edit of the given resources
	 */
	public IResource[] editResources(IContainer container, String[] hierarchy) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		getProvider(container).edit(resources, true /* recurse */, true /* notifyServer */, false /* notifyForWritable */, ICVSFile.NO_NOTIFICATION, DEFAULT_MONITOR);
		assertReadOnly(resources, false /* isReadOnly */, true /* recurse */);
		return resources;
	}
	
	/**
	 * Perform a CVS unedit of the given resources
	 */
	public IResource[] uneditResources(IContainer container, String[] hierarchy) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		getProvider(container).unedit(resources, true /* recurse */, true/* notifyServer */, DEFAULT_MONITOR);
		assertReadOnly(resources, true /* isReadOnly */, true /* recurse */);
		return resources;
	}
	
	public void appendText(IResource resource, String text, boolean prepend) throws CoreException, IOException, CVSException {
		IFile file = (IFile)resource;
		String contents = getFileContents(file);
		StringBuffer buffer = new StringBuffer();
		if (prepend) {
			buffer.append(text);
		}
		buffer.append(contents);
		if (!prepend) {
			buffer.append(eol + text);
		}
		setContentsAndEnsureModified(file, buffer.toString());
	}
	
	public void assertEndsWith(IFile file, String text) throws IOException, CoreException {
		assertTrue(getFileContents(file).endsWith(text));		
	}
	
	public void assertStartsWith(IFile file, String text) throws IOException, CoreException {
		assertTrue(getFileContents(file).startsWith(text));		
	}
	
	public static String getFileContents(IFile file) throws IOException, CoreException {
		StringBuffer buf = new StringBuffer();
		Reader reader = new InputStreamReader(new BufferedInputStream(file.getContents()));
		try {
			int c;
			while ((c = reader.read()) != -1) buf.append((char)c);
		} finally {
			reader.close();
		}
		return buf.toString();		
	}
	
	/**
	 * Delete the resources from an existing container and the changes to CVS
	 */
	public IResource[] changeResources(IContainer container, String[] hierarchy, boolean checkin) throws CoreException, TeamException {
		List changedResources = new ArrayList(hierarchy.length);
		for (int i=0;i<hierarchy.length;i++) {
			IResource resource = container.findMember(hierarchy[i]);
			if (resource.getType() == IResource.FILE) {
				changedResources.add(resource);
				setContentsAndEnsureModified((IFile)resource);
			}
		}
		IResource[] resources = (IResource[])changedResources.toArray(new IResource[changedResources.size()]);
		if (checkin) commitResources(resources, IResource.DEPTH_ZERO);
		return resources;
	}
	
	/**
	 * Delete the resources from an existing container and the changes to CVS
	 */
	public IResource[] deleteResources(IContainer container, String[] hierarchy, boolean checkin) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		deleteResources(resources);
		if (checkin)
			commitResources(resources, IResource.DEPTH_INFINITE);
		return resources;
	}
	
	/**
	 * Delete the resources and mark them as outgoing deletions.
	 * Deleting the resources is enough since the move/delete hook will
	 * tak care of making them outgoing deletions.
	 */
	protected void deleteResources(IResource[] resources) throws TeamException, CoreException {
		if (resources.length == 0) return;
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			delete(resource, IResource.NONE, DEFAULT_MONITOR);
		}
	}
	/**
	 * Unmanage the resources
	 */
	public void unmanageResources(IContainer container, String[] hierarchy) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		unmanageResources(resources);
	}
	
	protected void unmanageResources(IResource[] resources) throws TeamException, CoreException {
		for (int i=0;i<resources.length;i++) {
			CVSWorkspaceRoot.getCVSResourceFor(resources[i]).unmanage(null);
		}
	}
	
	/**
	 * Update the resources from an existing container with the changes from the CVS repository
	 */
	public IResource[] updateResources(IContainer container, String[] hierarchy, boolean ignoreLocalChanges) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		return updateResources(resources, ignoreLocalChanges);
	}
	
	/**
	 * Update the resources from an existing container with the changes from the CVS repository
	 */
    protected IResource[] updateResources(IResource[] resources, boolean ignoreLocalChanges) throws CVSException {
        LocalOption[] options = Command.NO_LOCAL_OPTIONS;
		if(ignoreLocalChanges) {
			options = new LocalOption[] {Update.IGNORE_LOCAL_CHANGES};
		}
        ResourceMapping[] mappers = asResourceMappers(resources, IResource.DEPTH_INFINITE);
		update(mappers, options);
		return resources;
    }

    /**
     * Update the resources contained in the given mappers.
     */
    protected void update(ResourceMapping[] mappings, LocalOption[] options) throws CVSException {
        if (options == null)
            options = Command.NO_LOCAL_OPTIONS;
        if (isModelSyncEnabled() && options == Command.NO_LOCAL_OPTIONS) {
	        executeHeadless(new ModelUpdateOperation(null, mappings, false) {
	        	protected boolean isAttemptHeadlessMerge() {
	        		return true;
	        	}
	        	protected void handlePreviewRequest() {
	        		// Don't preview anything
	        	}
	        	protected void handleNoChanges() {
	        		// Do nothing
	        	}
	        	protected void handleValidationFailure(IStatus status) {
	        		// Do nothing
	        	}
	        	protected void handleMergeFailure(IStatus status) {
	        		// Do nothing
	        	}
	        });
        } else {
        	executeHeadless(new UpdateOperation(null, mappings, options, null));
        }
    }

	protected void replace(IContainer container, String[] hierarchy, CVSTag tag, boolean recurse) throws CoreException {
		IResource[] resources = getResources(container, hierarchy);
		replace(resources, tag, recurse);
	}
	
	protected void replace(IResource[] resources, CVSTag tag, boolean recurse) throws CoreException {
		ReplaceOperation op = new ReplaceOperation(null, resources, tag, recurse);
		executeHeadless(op);
	}
	
    protected void replace(ResourceMapping[] mappings) throws CVSException {
    	if (isModelSyncEnabled()) {
	        executeHeadless(new ModelReplaceOperation(null, mappings, false) {
	        	protected boolean promptForOverwrite() {
	        		return true;
	        	}
	        	protected void handlePreviewRequest() {
	        		// Don't prompt
	        	}
	        	protected void handleMergeFailure(IStatus status) {
	        		// Don't prompt
	        	}
	        	protected void handleValidationFailure(IStatus status) {
	        		// Don't prompt
	        	}
	        });
    	} else {
    		executeHeadless(new ReplaceOperation(null, mappings, null));
    	}
    }
    
	public void updateProject(IProject project, CVSTag tag, boolean ignoreLocalChanges) throws TeamException {
		if (tag == null) {
			ResourceMapping[] mappings = asResourceMappers(new IResource[] { project }, IResource.DEPTH_INFINITE);
			if (ignoreLocalChanges)
				replace(mappings);
			else
				update(mappings, Command.NO_LOCAL_OPTIONS);
		} else {
			LocalOption[] options = Command.NO_LOCAL_OPTIONS;
			if(ignoreLocalChanges) {
				options = new LocalOption[] {Update.IGNORE_LOCAL_CHANGES};
			}
			executeHeadless(new UpdateOperation(null, new IResource[] {project}, options, tag));
		}
	}
	
	public void commitProject(IProject project) throws TeamException, CoreException {
		commitResources(project, true);
	}
	
	public void commitResources(IContainer container, boolean deep) throws TeamException, CoreException {
		commitResources(new IResource[] {container }, deep?IResource.DEPTH_INFINITE:IResource.DEPTH_ZERO);
	}
	
	/**
	 * Commit the resources from an existing container to the CVS repository
	 */
	public IResource[] commitResources(IContainer container, String[] hierarchy) throws CoreException, TeamException {
		IResource[] resources = getResources(container, hierarchy);
		commitResources(resources, IResource.DEPTH_ZERO);
		return resources;
	}
	
	protected void commitResources(IResource[] resources, int depth) throws TeamException, CoreException {
		commitResources(resources, depth, "");
	}
	
	/*
	 * Commit the provided resources which must all be in the same project
	 */
	protected void commitResources(IResource[] resources, int depth, String message) throws TeamException, CoreException {
		if (resources.length == 0) return;
		ResourceMapping[] resourceMappers = asResourceMappers(resources, depth);
        commit(resourceMappers, message);
	}

    /**
     * Commit the resources contained by the mappers.
     */
    protected void commit(ResourceMapping[] mappers, String message) throws CVSException {
        executeHeadless(new CommitOperation(null, mappers, new Command.LocalOption[0], message));
    }

    /**
     * Convert the resources to a resource mapper that traverses the resources
     * to the specified depth.
     * @param resources the resource
     * @return a resource mapper for traversing the resources to the depth specified
     */
    protected ResourceMapping[] asResourceMappers(IResource[] resources, int depth) {
        return WorkspaceResourceMapper.asResourceMappers(resources, depth);
    }
    
    protected ICVSResource asCVSResource(IResource resource) {
        return CVSWorkspaceRoot.getCVSResourceFor(resource);
    }
    
	/**
	 * Commit the resources from an existing container to the CVS repository
	 */
	public void tagProject(IProject project, CVSTag tag, boolean force) throws TeamException {
		ResourceMapping[] mappings = RepositoryProviderOperation.asResourceMappers(new IResource[] {project});
        tag(mappings, tag, force);
	}

    /**
     * Tag the resources contained in the given mappings
     */
    protected void tag(ResourceMapping[] mappings, CVSTag tag, boolean force) throws TeamException {
        ITagOperation op = new TagOperation((IWorkbenchPart)null, mappings);
        runTag(op, tag, force);
    }
	
	public void tagRemoteResource(ICVSRemoteResource resource, CVSTag tag, boolean force) throws TeamException  {
		ITagOperation op = new TagInRepositoryOperation(null, new ICVSRemoteResource[] {resource});
		runTag(op, tag, force);
	}
    
	protected void runTag(ITagOperation op, CVSTag tag, boolean force) throws TeamException {
		op.setTag(tag);
		if (force) op.moveTag();
		try {
			((CVSOperation)op).run(DEFAULT_MONITOR);
		} catch (InterruptedException e) {
			fail("Tag interrupted.");
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof TeamException)  {
				throw (TeamException) e.getTargetException();
			} else  {
				e.printStackTrace();
				fail("Unexpected error while tagging");
			}
		}
	}
	public void makeBranch(IResource[] resources, CVSTag version, CVSTag branch, boolean update) throws CVSException {
		ResourceMapping[] mappings = asResourceMappers(resources, IResource.DEPTH_INFINITE);
        branch(mappings, version, branch, update);
	}

    protected void branch(ResourceMapping[] mappings, CVSTag version, CVSTag branch, boolean update) throws CVSException {
        BranchOperation op = new BranchOperation(null, mappings);
        op.setTags(version, branch, update);
        executeHeadless(op);
    }
    
	/**
	 * Return a collection of resources defined by hierarchy. The resources
	 * are added to the workspace and to the file system. If the manage flag is true, the
	 * resources are auto-managed, if false, they are left un-managed.
	 */
	public IResource[] buildResources(IContainer container, String[] hierarchy, boolean includeContainer) throws CoreException {
		List resources = new ArrayList(hierarchy.length + 1);
		resources.addAll(Arrays.asList(buildResources(container, hierarchy)));
		if (includeContainer)
			resources.add(container);
		IResource[] result = (IResource[]) resources.toArray(new IResource[resources.size()]);
		ensureExistsInWorkspace(result, true);
		for (int i = 0; i < result.length; i++) {
			if (result[i].getType() == IResource.FILE)
				// 3786 bytes is the average size of Eclipse Java files!
				 ((IFile) result[i]).setContents(getRandomContents(RANDOM_CONTENT_SIZE), true, false, null);
		}
		return result;
	}

	/*
	 * Checkout a copy of the project into a project with the given postfix
	 */
	 protected IProject checkoutCopy(IProject project, String postfix) throws TeamException {
		// Check the project out under a different name and validate that the results are the same
		IProject copy = getWorkspace().getRoot().getProject(project.getName() + postfix);
		checkout(getRepository(), copy, CVSWorkspaceRoot.getCVSFolderFor(project).getFolderSyncInfo().getRepository(), null, DEFAULT_MONITOR);
		return copy;
	 }
	 
	 protected IProject checkoutCopy(IProject project, CVSTag tag) throws TeamException {
		// Check the project out under a different name and validate that the results are the same
		IProject copy = getWorkspace().getRoot().getProject(project.getName() + tag.getName());
		checkout(getRepository(), copy, 
			CVSWorkspaceRoot.getCVSFolderFor(project).getFolderSyncInfo().getRepository(), 
			tag, DEFAULT_MONITOR);
		return copy;
	 }
	 
	public static void checkout(
		final ICVSRepositoryLocation repository,
		final IProject project,
		final String sourceModule,
		final CVSTag tag,
		IProgressMonitor monitor)
		throws TeamException {
		
		RemoteFolder remote = new RemoteFolder(null, repository, sourceModule == null ? project.getName() : sourceModule, tag);
		executeHeadless(new CheckoutSingleProjectOperation(null, remote, project, null, false /* the project is not preconfigured */) {
			public boolean promptToOverwrite(String title, String msg, IResource resource) {
				return true;
			}
		});

	}

	protected IProject checkoutProject(IProject project, String moduleName, CVSTag tag) throws TeamException {
	 	if (project == null)
	 		project = getWorkspace().getRoot().getProject(new Path(moduleName).lastSegment());
		checkout(getRepository(), project, moduleName, tag, DEFAULT_MONITOR);
		return project;
	 }
	/*
	 * This method creates a project with the given resources, imports
	 * it to CVS and checks it out
	 */
	protected IProject createProject(String prefix, String[] resources) throws CoreException, TeamException {
		IProject project = getUniqueTestProject(prefix);
		buildResources(project, resources, true);
		shareProject(project);
		assertValidCheckout(project);
		return project;
	}
	
	/*
	 * Create a test project using the currently running test case as the project name prefix
	 */
	protected IProject createProject(String[] strings) throws CoreException {
		return createProject(getName(), strings);
	}
	
	/*
	 * Compare two projects by comparing thier providers
	 */
	protected void assertEquals(IProject project1, IProject project2) throws CoreException, IOException {
		assertEquals(project1, project2, false, false);
	}
	
	protected void assertEquals(IProject project1, IProject project2, boolean includeTimestamps, boolean includeTags) throws CoreException, IOException {
		assertEquals(getProvider(project1), getProvider(project2), includeTimestamps, includeTags);
	}
	
	/*
	 * Compare CVS team providers by comparing the cvs resource corresponding to the provider's project
	 */
	protected void assertEquals(CVSTeamProvider provider1, CVSTeamProvider provider2, boolean includeTimestamps, boolean includeTags) throws CoreException, IOException {
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSFolderFor(provider1.getProject()), 
			CVSWorkspaceRoot.getCVSFolderFor(provider2.getProject()), 
			includeTimestamps, includeTags);
	}
	
	protected void assertContentsEqual(IContainer c1, IContainer c2) throws CoreException {
		assertTrue("The number of resource in " + c1.getProjectRelativePath().toString() + " differs", 
			c1.members().length == c2.members().length);
		IResource[] resources = c1.members();
		for (int i= 0;i <resources.length;i++) {
			assertContentsEqual(resources[i], c2.findMember(resources[i].getName()));
		}
	}
	
	protected void assertContentsEqual(IResource resource, IResource resource2) throws CoreException {
		if (resource.getType() == IResource.FILE) {
			assertContentsEqual((IFile)resource, (IFile)resource2);
		} else {
			assertContentsEqual((IContainer)resource, (IContainer)resource2);
		}
	}

	protected void assertContentsEqual(IFile resource, IFile resource2) throws CoreException {
		assertTrue("Contents of " + resource.getProjectRelativePath() + " do not match", compareContent(resource.getContents(), resource2.getContents()));
	}
	
	protected void assertContentsEqual(IFile file, String contents) throws CoreException {
		assertTrue(compareContent(file.getContents(), new ByteArrayInputStream(contents.getBytes())));
	}
	
	/*
	 * Compare resources by casting them to their prpoer type
	 */
	protected void assertEquals(IPath parent, ICVSResource resource1, ICVSResource resource2, boolean includeTimestamps, boolean includeTags) throws CoreException, CVSException, IOException {
        if ((resource1 == null && resource2 == null) 
                || (resource1 == null && ! resource2.exists())
                || (resource2 == null && ! resource1.exists()))
            return;
        if (resource1 == null && resource2 != null) {
        	fail("Expected no resource for " + resource2.getRepositoryRelativePath() + " but there was one");
        }
        if (resource2 == null && resource1 != null) {
        	fail("Expected resource " + resource1.getRepositoryRelativePath() + " was missing");
        }
		assertEquals("Resource types do not match for " + parent.append(resource1.getName()), resource1.isFolder(), resource2.isFolder());
		if (!resource1.isFolder())
			assertEquals(parent, (ICVSFile)resource1, (ICVSFile)resource2, includeTimestamps, includeTags);
		else 
			assertEquals(parent, (ICVSFolder)resource1, (ICVSFolder)resource2, includeTimestamps, includeTags);
	}
	
	/*
	 * Compare folders by comparing their folder sync info and there children
	 * 
	 * XXX What about unmanaged children?
	 */
	protected void assertEquals(IPath parent, ICVSFolder container1, ICVSFolder container2, boolean includeTimestamps, boolean includeTags) throws CoreException, CVSException, IOException {
		IPath path = parent.append(container1.getName());
		assertEquals(path, container1.getFolderSyncInfo(), container2.getFolderSyncInfo(), includeTags);
		assertTrue("The number of resource in " + path.toString() + " differs", 
			container1.members(ICVSFolder.ALL_EXISTING_MEMBERS).length 
			== container2.members(ICVSFolder.ALL_EXISTING_MEMBERS).length);
		ICVSResource[] resources = container1.members(ICVSFolder.ALL_EXISTING_MEMBERS);
		for (int i= 0;i <resources.length;i++) {
			assertEquals(path, resources[i], container2.getChild(resources[i].getName()), includeTimestamps, includeTags);
		}

	}
	
	/*
	 * Compare the files contents and sync information
	 */
	protected void assertEquals(IPath parent, ICVSFile file1, ICVSFile file2, boolean includeTimestamps, boolean includeTags) throws CoreException, CVSException {
		if (file1.getName().equals(".project")) return;
		// Getting the contents first is important as it will fetch the proper sync info if one of the files is a remote handle
		assertEquals(parent.append(file1.getName()), file1.getSyncInfo(), file2.getSyncInfo(), includeTimestamps, includeTags);
		assertTrue("Contents of " + parent.append(file1.getName()) + " do not match", compareContent(getContents(file1), getContents(file2)));
	}
	
	protected boolean isFailOnSyncInfoMismatch() {
		return true;
	}
	
	/*
	 * Compare sync info by comparing the entry line generated by the sync info
	 */
	protected void assertEquals(IPath path, ResourceSyncInfo info1, ResourceSyncInfo info2, boolean includeTimestamp, boolean includeTag) {
		if (!isFailOnSyncInfoMismatch())
			return;
		if (info1 == null || info2 == null) {
			if (info1 == info2) return;
			if (info1 == null) {
				fail("Expected no resource sync info  for " + path.toString() + " but it was " + info2 + " instead");
			}
			if (info2 == null) {
				fail("Expected resource sync info of " + info1 + " for " + path.toString() + " but there was no sync info.");
			}
			fail("Shouldn't be able to get here");
			return;
		}
		String line1;
		String line2;
		if(includeTimestamp) {
			line1 = info1.getEntryLine();
			line2 = info2.getEntryLine();
		} else {
			line1 = info1.getServerEntryLine(null);
			line2 = info2.getServerEntryLine(null);
		}
		if (!includeTag) {
			// Strip everything past the last slash
			line1 = line1.substring(0, line1.lastIndexOf('/'));
			line2 = line2.substring(0, line2.lastIndexOf('/'));
		}
		assertEquals("Resource Sync info differs for " + path.toString(), line1, line2);
	}
	
	/*
	 * Use the equals of folder sync info unless the tag is not included in which case we just
	 * compare the root and repository
	 */
	protected void assertEquals(IPath path, FolderSyncInfo info1, FolderSyncInfo info2, boolean includeTag) {
		if (info1 == null && info2 == null) {
			return;
		} else if (info1 == null) {
			fail("Expected " + path.toString() + " not to be a CVS folder but it is.");
		} else if (info2 == null) {
			fail("Expected " + path.toString() + " to be a CVS folder but it isn't.");
		}
		
		if (includeTag) {
			assertTrue("Folder sync info differs for " + path.toString(), info1.equals(info2));
		} else {
			assertTrue("Repository Root differs for " + path.toString(), info1.getRoot().equals(info2.getRoot()));
			assertTrue("Repository relative path differs for " + path.toString(), info1.getRepository().equals(info2.getRepository()));
		}
	}
	
	
	/*
	 * Compare folders by comparing their folder sync info and there children
	 * 
	 * XXX What about unmanaged children?
	 */
	protected void assertEquals(IPath parent, RemoteFolder container1, RemoteFolder container2, boolean includeTags) throws CoreException, TeamException, IOException {
		IPath path = parent.append(container1.getName());
		assertEquals(path, container1.getFolderSyncInfo(), container2.getFolderSyncInfo(), includeTags);
		ICVSRemoteResource[] members1 = container1.getMembers(DEFAULT_MONITOR);
		ICVSRemoteResource[] members2 = container2.getMembers(DEFAULT_MONITOR);
		assertTrue("Number of members differ for " + path, members1.length == members2.length);
		Map memberMap2 = new HashMap();
		for (int i= 0;i <members2.length;i++) {
			memberMap2.put(members2[i].getName(), members2[i]);
		}
		for (int i= 0;i <members1.length;i++) {
			ICVSRemoteResource member2 = (ICVSRemoteResource)memberMap2.get(members1[i].getName());
			assertNotNull("Resource does not exist: " + path.append(members1[i].getName()) + member2);
			assertEquals(path, members1[i], member2, includeTags);
		}
	}
	protected void assertEquals(IPath parent, ICVSRemoteResource resource1, ICVSRemoteResource resource2, boolean includeTags) throws CoreException, TeamException, IOException {
		assertEquals("Resource types do not match for " + parent.append(resource1.getName()), resource1.isContainer(), resource2.isContainer());
		if (resource1.isContainer())
			assertEquals(parent, (RemoteFolder)resource1, (RemoteFolder)resource2, includeTags);
		else 
			assertEquals(parent, (ICVSFile)resource1, (ICVSFile)resource2, false, includeTags);
	}
	
	
	/*
	 * Compare the local project with the remote state by checking out a copy of the project.
	 */
	protected void assertLocalStateEqualsRemote(IProject project) throws TeamException, CoreException, IOException {
		assertEquals(getProvider(project), getProvider(checkoutCopy(project, "-remote")), false, true);
	}
	
	/*
	 * Compare the local project with the remote state indicated by the given tag by checking out a copy of the project.
	 */
	protected void assertLocalStateEqualsRemote(String message, IProject project, CVSTag tag) throws TeamException, CoreException, IOException {
		assertEquals(getProvider(project), getProvider(checkoutCopy(project, tag)), true, false);
	}
	
	protected void assertHasNoRemote(String prefix, IResource[] resources) {
		for (int i=0;i<resources.length;i++) 
			assertHasNoRemote(prefix, resources[i]);
	}
	
	protected void assertHasNoRemote(String prefix, IResource resource) {
		assertTrue(prefix + " resource should not have a remote", !CVSWorkspaceRoot.hasRemote(resource));
	}
	
	protected void assertHasRemote(String prefix, IResource[] resources) {
		for (int i=0;i<resources.length;i++) 
			assertHasRemote(prefix, resources[i]);
	}
	
	protected void assertHasRemote(String prefix, IResource resource) {
		assertTrue(prefix + " resource should have a remote", CVSWorkspaceRoot.hasRemote(resource));
	}
	
	protected void assertIsModified(String prefix, IResource[] resources) throws TeamException {
		for (int i=0;i<resources.length;i++) 
			assertIsModified(prefix, resources[i]);
	}
	
	protected void assertIsModified(String prefix, IResource resource) throws TeamException {
		// Only check for files as CVS doesn't dirty folders
		if (resource.getType() == IResource.FILE)
			assertTrue(prefix + " resource " + resource.getFullPath() + " should be dirty.", ((ICVSFile)getCVSResource(resource)).isModified(null));
	}
	
	protected void assertNotModified(String prefix, IResource[] resources) throws TeamException {
		for (int i=0;i<resources.length;i++) 
			assertNotModified(prefix, resources[i]);
	}
	
	protected void assertNotModified(String prefix, IResource resource) throws TeamException {
		assertTrue(prefix + " resource should be dirty", !((ICVSFile)getCVSResource(resource)).isModified(null));
	}
	
	protected void assertIsIgnored(IResource resource, boolean ignoredState) throws TeamException {
		assertEquals("Resource " + resource.getFullPath() + " should be ignored but isn't.", 
						ignoredState, getCVSResource(resource).isIgnored());
	}
	
	protected void assertValidCheckout(IProject project) {
		// NOTE: Add code to ensure that the project was checkout out properly
		CVSTeamProvider provider = (CVSTeamProvider)RepositoryProvider.getProvider(project);
		assertNotNull(provider);
	}

	protected void assertReadOnly(IResource[] resources, final boolean isReadOnly, final boolean recurse) throws CoreException {
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			resource.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource.getType() == IResource.FILE) {
						assertEquals(isReadOnly, resource.getResourceAttributes().isReadOnly());
					}
					return recurse;
				}
			});
		}
	}
	
	protected InputStream getContents(ICVSFile file) throws CVSException {
		if (file instanceof ICVSRemoteFile)
			return ((RemoteFile)file).getContents(DEFAULT_MONITOR);
		else
			return new BufferedInputStream(file.getContents());
	}
	
	/*
	 * Get the CVS Resource for the given resource
	 */
	protected ICVSResource getCVSResource(IResource resource) {
		return CVSWorkspaceRoot.getCVSResourceFor(resource);
	}
	
	protected IProject getNamedTestProject(String name) throws CoreException {
		IProject target = getWorkspace().getRoot().getProject(name);
		if (!target.exists()) {
			target.create(null);
			target.open(null);		
		}
		assertExistsInFileSystem(target);
		return target;
	}
	protected CVSTeamProvider getProvider(IResource resource) {
		return (CVSTeamProvider)RepositoryProvider.getProvider(resource.getProject());
	}
	protected static InputStream getRandomContents(int sizeAtLeast) {
		StringBuffer randomStuff = new StringBuffer(sizeAtLeast + 100);
		while (randomStuff.length() < sizeAtLeast) {
			randomStuff.append(getRandomSnippet() + eol);
		}
		return new ByteArrayInputStream(randomStuff.toString().getBytes());
	}
	/**
	 * Return String with some random text to use
	 * as contents for a file resource.
	 */
	public static String getRandomSnippet() {
		switch ((int) Math.round(Math.random() * 10)) {
			case 0 :
				return "este e' o meu conteudo (portuguese)";
			case 1 :
				return "Dann brauchen wir aber auch einen deutschen Satz!";
			case 2 :
				return "I'll be back";
			case 3 :
				return "don't worry, be happy";
			case 4 :
				return "there is no imagination for more sentences";
			case 5 :
				return "customize yours";
			case 6 :
				return "foo";
			case 7 :
				return "bar";
			case 8 :
				return "foobar";
			case 9 :
				return "case 9";
			default :
				return "these are my contents";
		}
	}
	protected IProject getUniqueTestProject(String prefix) throws CoreException {
		// manage and share with the default stream create by this class
		return getNamedTestProject(prefix + "-" + Long.toString(System.currentTimeMillis()));
	}
	
	protected CVSRepositoryLocation getRepository() {
		return CVSTestSetup.repository;
	}
	protected void importProject(IProject project) throws TeamException {
		
		// Create the root folder for the import operation
		ICVSFolder root = CVSWorkspaceRoot.getCVSFolderFor(project);

		// Perform the import
		IStatus status;
		Session s = new Session(getRepository(), root);
		s.open(DEFAULT_MONITOR, true /* open for modification */);
		try {
			status = Command.IMPORT.execute(s,
				Command.NO_GLOBAL_OPTIONS,
				new LocalOption[] {Import.makeArgumentOption(Command.MESSAGE_OPTION, "Initial Import")},
				new String[] { project.getName(), getRepository().getUsername(), "start" },
				null,
				DEFAULT_MONITOR);
		} finally {
			s.close();
		}

		if (status.getCode() == CVSStatus.SERVER_ERROR) {
			throw new CVSServerException(status);
		}
	}
	
	protected void shareProject(IProject project) throws TeamException, CoreException {
		mapNewProject(project);
		commitNewProject(project);
	}
	
	protected void mapNewProject(IProject project) throws TeamException {
		shareProject(getRepository(), project, null, DEFAULT_MONITOR);
	}
	
	/**
	 * Map the given local project to remote folder, creating the remote folder or any of
	 * its ancestors as necessary.
	 * @param location
	 * @param project
	 * @param moduleName
	 * @param default_monitor
	 */
	protected void shareProject(CVSRepositoryLocation location, IProject project, String moduleName, IProgressMonitor default_monitor) throws CVSException {
		ShareProjectOperation op = new ShareProjectOperation(null, location, project, moduleName);
		executeHeadless(op);
	}
	
	protected void commitNewProject(IProject project) throws CoreException, CVSException, TeamException {
		List resourcesToAdd = new ArrayList();
		IResource[] members = project.members();
		for (int i = 0; i < members.length; i++) {
			if ( ! CVSWorkspaceRoot.getCVSResourceFor(members[i]).isIgnored()) {
				resourcesToAdd.add(members[i]);
			}
		}
		addResources((IResource[]) resourcesToAdd.toArray(new IResource[resourcesToAdd.size()]));
		commitResources(new IResource[] {project}, IResource.DEPTH_INFINITE);
		// Pause to ensure that future operations happen later than timestamp of committed resources
		waitMsec(1500);
	}
	
	/**
	 * Return an input stream with some random text to use
	 * as contents for a file resource.
	 */
	public InputStream getRandomContents() {
		return getRandomContents(RANDOM_CONTENT_SIZE);
	}
	
	protected void setContentsAndEnsureModified(IFile file) throws CoreException, TeamException {
		setContentsAndEnsureModified(file, getRandomContents());
	}
	
	protected void setContentsAndEnsureModified(IFile file, String contents) throws CoreException, CVSException {
		if (contents == null) contents ="";
		setContentsAndEnsureModified(file, new ByteArrayInputStream(contents.getBytes()));
	}
	
	protected void setContentsAndEnsureModified(IFile file, InputStream stream) throws CoreException, CVSException {
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		int count = 0;
		file.setContents(stream, false, false, null);
		do {
			assertTrue("Timestamp granularity is too small. Increase test wait factor", count <= CVSTestSetup.WAIT_FACTOR);
			if (!cvsFile.isModified(null)) {
				waitMsec(1500);
				count++;
				try {
					file.setContents(new ByteArrayInputStream(getFileContents(file).getBytes()), false, false, null);
				} catch (IOException e) {
					CVSStatus status = new CVSStatus(IStatus.ERROR, "Error reading file contents", e);
					throw new CVSException(status);
				}
			}
		} while (!cvsFile.isModified(null));
	}
	
	public void waitMsec(int msec) {	
		try {
			Thread.sleep(msec);
		} catch(InterruptedException e) {
			fail("wait-problem");
		}
	}
	
	public static void waitForJobCompletion(Job job) {
		// process UI events first, give the main thread a chance
		// to handle any syncExecs or asyncExecs posted as a result
		// of the event processing thread.
		while (Display.getCurrent().readAndDispatch()) {};
		
		// wait for the event handler to process changes.
		while(job.getState() != Job.NONE) {
			while (Display.getCurrent().readAndDispatch()) {};
			try {
				Thread.sleep(10);		
			} catch (InterruptedException e) {
			}
		}
		while (Display.getCurrent().readAndDispatch()) {};
	}
	
	public static void waitForIgnoreFileHandling() {
		waitForJobCompletion(SyncFileChangeListener.getDeferredHandler().getEventHandlerJob());
        waitForDecorator();
	}

    protected static void waitForDecorator() {
        // Wait for the decorator job
        Job[] decorators = Job.getJobManager().find(DecoratorManager.FAMILY_DECORATE);
        for (int i = 0; i < decorators.length; i++) {
            Job job = decorators[i];
            waitForJobCompletion(job);
        }
    }
	
	public static void waitForSubscriberInputHandling(SubscriberSyncInfoCollector input) {
		input.waitForCollector(new IProgressMonitor() {
			public void beginTask(String name, int totalWork) {
			}
			public void done() {
			}
			public void internalWorked(double work) {
			}
			public boolean isCanceled() {
				return false;
			}
			public void setCanceled(boolean value) {
			}
			public void setTaskName(String name) {
			}
			public void subTask(String name) {
			}
			public void worked(int work) {
				while (Display.getCurrent().readAndDispatch()) {}
			}
		});
	}
	
	protected static void executeHeadless(final TeamOperation op) throws CVSException {
		EclipseRunnable tempRunnable = new EclipseRunnable(op, DEFAULT_MONITOR);
		Thread tempThread = new Thread(tempRunnable);
		tempThread.start();
		while (tempThread.isAlive()) {
			try {
				Thread.sleep(100);
				while (Display.getCurrent().readAndDispatch()) {}
			} catch (InterruptedException e) {
				//ignore
			}
		}
		//check for errors
		Exception ex = tempRunnable.getException();
		if (ex instanceof InvocationTargetException)
			throw CVSException.wrapException(ex);
	}
    
    protected void setUp() throws Exception {
    	RepositoryProviderOperation.consultModelsWhenBuildingScope = false;
    	if (CVSTestSetup.ENSURE_SEQUENTIAL_ACCESS)
    		obtainCVSServerLock();
        super.setUp();
    }

    /* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		RepositoryProviderOperation.consultModelsWhenBuildingScope = true;
		if (CVSTestSetup.ENSURE_SEQUENTIAL_ACCESS)
			releaseCVSServerLock();
		super.tearDown();
		if (CVSTestSetup.logListener != null) {
			try {
				CVSTestSetup.logListener.checkErrors();
			} catch (CoreException e) {
				if (CVSTestSetup.FAIL_IF_EXCEPTION_LOGGED) {
					fail("Exception written to log: ", e);
				} else {
					// Write the log to standard out so it can be more easily seen
					write(e.getStatus(), 0);
				}
			}
		}
	}
        
    private void obtainCVSServerLock() {
        IProject project = null;
        boolean firstTry = true;
        while (project == null) {
            try {
                project = checkoutProject(null, CVS_TEST_LOCK_PROJECT , null);
            } catch (TeamException e) {
                // The checkout of the lock project failed so lets create it if it doesn't exist
                if (firstTry) {
                    try {
                        createTestLockProject(DEFAULT_MONITOR);
                    } catch (TeamException e1) {
                        // We couldn't check out the project or create it
                        // It's possible someone beat us to it so we'll try the checkout again.
                    }
                } else {
                    // We tried twice to check out the project and failed.
                    // Lets just go ahead and run but we'll log the fact that we couldn't get the lock
                    write(new CVSStatus(IStatus.ERROR, "Could not obtain the CVS server lock. The test will containue but any performance timings may be affected", e), 0);
                    return;
                }
                firstTry = false;
            }
        }
        if (project != null) {
            IFile lockFile = project.getFile(CVS_TEST_LOCK_FILE);
            boolean obtained = false;
            int attempts = 0;
            while (!obtained) {
                attempts++;
                if (lockFile.exists()) {
                    // If the file exists, check if the lock has expired
                    if (hasExpired(lockFile)) {
                        try {
                            overwriteLock(lockFile);
                            return;
                        } catch (CoreException e) {
                            // Ignore the error and continue
                        }
                    }
                } else {
                    try {
                        writeLock(lockFile);
                        return;
                    } catch (CoreException e) {
                        // Ignore the error, since it probably means someone beat us to it.
                    }
                }
                // Wait for a while before testing the lock again
                try {
                    Thread.sleep(LOCK_WAIT_TIME);
                } catch (InterruptedException e) {
                    // Ignore
                }
                try {
                    // Update the lockfile in case someone else got to it first
                    replace(new IResource[] { lockFile }, null, true);
                } catch (CoreException e) {
                    // An error updated is not recoverable so just continue
                    write(new CVSStatus(IStatus.ERROR, "Could not obtain the CVS server lock. The test will continue but any performance timings may be affected", e), 0);
                    return;
                }
                if (attempts > MAX_LOCK_ATTEMPTS) {
                    write(new CVSStatus(IStatus.ERROR, "Could not obtain the CVS server lock. The test will continue but any performance timings may be affected", new Exception()), 0);
                    return;
                }
            }
        }
    }
    
    private boolean hasExpired(IFile lockFile) {
        long timestamp = lockFile.getLocalTimeStamp();
        return System.currentTimeMillis() - timestamp > LOCK_EXPIRATION_THRESHOLD;
    }

    private void overwriteLock(IFile lockFile) throws CoreException {
        lockFile.setContents(getLockContents(), true, true, null);
        commitResources(new IResource[] { lockFile }, IResource.DEPTH_ZERO);
    }

    private void writeLock(IFile lockFile) throws CoreException {
        lockFile.create(getLockContents(), false, null);
        addResources(new IResource[] { lockFile });
        commitResources(new IResource[] { lockFile }, IResource.DEPTH_ZERO);
    }

    private InputStream getLockContents() {
        lockId = Long.toString(System.currentTimeMillis());
        return new ByteArrayInputStream(lockId.getBytes());
    }

    private void createTestLockProject(IProgressMonitor monitor) throws TeamException {
        CVSRepositoryLocation repository = getRepository();
        RemoteFolderTree root = new RemoteFolderTree(null, repository, Path.EMPTY.toString(), null);
        RemoteFolderTree child = new RemoteFolderTree(root, CVS_TEST_LOCK_PROJECT, repository, new Path(null, root.getRepositoryRelativePath()).append(CVS_TEST_LOCK_PROJECT).toString(), null);
        root.setChildren(new ICVSRemoteResource[] { child });
        Session s = new Session(repository, root);
        s.open(monitor, true /* open for modification */);
        try {
            IStatus status = Command.ADD.execute(s,
                    Command.NO_GLOBAL_OPTIONS,
                    Command.NO_LOCAL_OPTIONS,
                    new String[] { CVS_TEST_LOCK_PROJECT },
                    null,
                    monitor);
            // If we get a warning, the operation most likely failed so check that the status is OK
            if (status.getCode() == CVSStatus.SERVER_ERROR  || ! status.isOK()) {
                throw new CVSServerException(status);
            }
        } finally {
            s.close();
        }
    }
        
	private void releaseCVSServerLock() {
        if (lockId != null) {
    	    try {
                IProject project = getWorkspace().getRoot().getProject(CVS_TEST_LOCK_PROJECT);
                // Update the project and verify we still have the lock
                IFile file = project.getFile(CVS_TEST_LOCK_FILE);
                String id = getFileContents(file);
                if (id.equals(lockId)) {
                    // We have the lock so let's free it (but first check if someone preempted us)
                    ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
                    byte[] bytes = cvsFile.getSyncBytes();
                    if (bytes != null) {
                        String revision = ResourceSyncInfo.getRevision(bytes);
                        updateResources(new IResource[] { file }, true);
                        bytes = cvsFile.getSyncBytes();
                        if (bytes == null || !ResourceSyncInfo.getRevision(bytes).equals(revision)) {
                            write(new CVSStatus(IStatus.ERROR, "The CVS server lock expired while this test was running. Any performance timings may be affected", new Exception()), 0);
                            return;
                        }
                    }
                    // Delete the lock file and commit
                    deleteResources(project, new String[] { CVS_TEST_LOCK_FILE }, true);
                }
            } catch (CoreException e) {
                write(e.getStatus(), 0);
            } catch (IOException e) {
                write(new CVSStatus(IStatus.ERROR, "An error occurred while reading the lock file", e), 0);
            }
        }
    }

    protected void write(IStatus status, int indent) {
		PrintStream output = System.out;
		indent(output, indent);
		output.println("Severity: " + status.getSeverity());

		indent(output, indent);
		output.println("Plugin ID: " + status.getPlugin());

		indent(output, indent);
		output.println("Code: " + status.getCode());

		indent(output, indent);
		output.println("Message: " + status.getMessage());
		
		Throwable t = status.getException();
		if (t != null) {
			t.printStackTrace(output);
			if (t instanceof CoreException) {
				write(((CoreException)t).getStatus(), indent + 1);
			}
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				write(children[i], indent + 1);
		}
	}
	
	protected static void indent(OutputStream output, int indent) {
		for (int i = 0; i < indent; i++)
			try {
				output.write("  ".getBytes());
			} catch (IOException e) {
				// ignore
			}
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#runBare()
	 */
	public void runBare() throws Throwable {
		try {
			super.runBare();
		} catch (CVSException e) {
			// If a communication exception occurred
			// perhaps it is a server problem
			// Try again, just in case it is
			if (containsCommunicationException(e)) {
				super.runBare();
			} else {
				throw e;
			}
		}
	}

	private boolean containsCommunicationException(CVSException e) {
		if (e instanceof CVSCommunicationException) return true;
		IStatus status = e.getStatus();
		if (status.getException() instanceof CVSCommunicationException) return true;
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++) {
				IStatus child = children[i];
				if (child.getException() instanceof CVSCommunicationException) return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.tests.harness.EclipseWorkspaceTest#ensureDoesNotExistInWorkspace(org.eclipse.core.resources.IResource)
	 */
	public void ensureDoesNotExistInWorkspace(IResource resource) {
		// Overridden to change how the workspace is deleted on teardown
		if (resource.getType() == IResource.ROOT) {
			// Delete each project individually
			Job[] allJobs = Job.getJobManager().find(null /* all families */);
			IProject[] projects = ((IWorkspaceRoot)resource).getProjects();
			try {
				ensureDoesNotExistInWorkspace(projects);
			} catch (AssertionFailedError e) {
				// The delete failed. Write the active jobs to stdout
				System.out.println(e.getMessage());
				System.out.println("Jobs active at time of deletion failure: "); //$NON-NLS-1$
				if (allJobs.length == 0) {
					System.out.println("None"); //$NON-NLS-1$
				}
				for (int i = 0; i < allJobs.length; i++) {
					Job job = allJobs[i];
					System.out.println(job.getName() + ": " + job.getState());
				}
				if (CVSTestSetup.FAIL_IF_EXCEPTION_LOGGED) {
					throw e;
				}
			}
		} else {
			ensureNotReadOnly(resource);
			super.ensureDoesNotExistInWorkspace(resource);
		}
	}
	
	private void ensureNotReadOnly(IResource resource) {
		if (resource.exists()) {
			try {
				resource.accept(new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						ResourceAttributes attrs = resource.getResourceAttributes();
						if (resource.exists() && attrs.isReadOnly()) {
							attrs.setReadOnly(false);
							resource.setResourceAttributes(attrs);
						}
						return true;
					}
				});
			} catch (CoreException e) {
				fail("#ensureNotReadOnly " + resource.getFullPath(), e);
			}
		}
		
	}

	/**
	 * Delete each project from the workspace and return a status that
	 * contains any failures
	 */
	public void ensureDoesNotExistInWorkspace(final IProject[] projects) {
		final Map failures = new HashMap();
		IWorkspaceRunnable body = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				for (int i = 0; i < projects.length; i++) {
					try {
						if (projects[i].exists()) {
							try {
								projects[i].delete(true, null);
							} catch (CoreException e) {
								// Ignore the exception and try again after making
								// sure the project doesn't contain any read-only resources
								ensureNotReadOnly(projects[i]);
								if (projects[i].exists()) {
									projects[i].refreshLocal(IResource.DEPTH_INFINITE, null);
									projects[i].delete(true, null);
								}
							}
						}
					} catch (CoreException e) {
						write(new CVSStatus(IStatus.ERROR, "Could not delete project " + projects[i].getName(), e), 0);
						failures.put(projects[i], e);
					}
				}
			}
		};
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			fail("#ensureDoesNotExistInWorkspace(IResource[])", e);
		}
		if (!failures.isEmpty()) {
			StringBuffer text = new StringBuffer();
			text.append("Could not delete all projects: ");
			for (Iterator iter = failures.keySet().iterator(); iter.hasNext();) {
				IProject project = (IProject) iter.next();
				text.append(project.getName());
			}
			fail(text.toString());
		}
	}
	
    protected void assertStatusContainsCode(IStatus status, int code) {
        if (status.isMultiStatus()) {
            IStatus[] children = status.getChildren();
            for (int i = 0; i < children.length; i++) {
                IStatus child = children[i];
                if (child.getCode() == code)
                    return;
            }
            fail("Expected status code was not present");
        } else {
            assertEquals("Status code is not what is expected", status.getCode(), code);
        }
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#runTest()
     */
    protected void runTest() throws Throwable {
        if (!CVSTestSetup.RECORD_PROTOCOL_TRAFFIC) {
            super.runTest();
            return;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Policy.recorder = new PrintStream(os);
        try {
            try {
                // Override the runTest method in order to print the entire trace of a
                // test that failed due to a CoreException including nested exceptions
                super.runTest();
            } catch (CoreException e) {
                e.printStackTrace();
                write(e.getStatus(), 0);
                throw e;
            }
        } catch (Throwable e) {
            // Transfer the recorded debug info to stdout
            Policy.recorder.close();
            System.out.println(new String(os.toByteArray()));
            throw e;
        } finally {
            Policy.recorder.close();
            Policy.recorder = null;
        }
    }
    
    protected void cleanup() throws CoreException {
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
		getWorkspace().save(true, null);
		//don't leak builder jobs, since they may affect subsequent tests
		waitForBuild();
    }
}
