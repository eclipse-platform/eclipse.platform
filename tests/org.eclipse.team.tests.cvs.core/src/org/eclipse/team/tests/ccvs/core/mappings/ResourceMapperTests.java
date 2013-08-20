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
package org.eclipse.team.tests.ccvs.core.mappings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.core.variants.CachedResourceVariant;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.connection.CVSCommunicationException;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTreeBuilder;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.operations.CacheBaseContentsOperation;
import org.eclipse.team.internal.ccvs.ui.operations.CacheRemoteContentsOperation;
import org.eclipse.team.internal.core.ResourceVariantCache;
import org.eclipse.team.internal.core.ResourceVariantCacheEntry;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.TeamCVSTestPlugin;

import org.eclipse.core.internal.resources.mapping.SimpleResourceMapping;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;

import org.eclipse.jface.util.Util;

/**
 * Tests for using CVS operations with deep and shallow resource mappings.
 */
public class ResourceMapperTests extends EclipseTest {


    public ResourceMapperTests() {
        super();
    }

    public ResourceMapperTests(String name) {
        super(name);
    }

    public static Test suite() {
        return suite(ResourceMapperTests.class);
    }
    
    /**
     * Update the resources contained in the given mappers and ensure that the
     * update was performed properly by comparing the result with the reference projects.
     * @throws Exception 
     */
    protected void update(ResourceMapping mapper, LocalOption[] options) throws Exception {
        SyncInfoTree incomingSet = getIncoming(mapper.getProjects());
        update(new ResourceMapping[] { mapper }, options);
        assertUpdate(mapper, incomingSet);
    }
   
    /**
     * Replace the resources contained in the given mappers and ensure that the
     * update was performed properly by comparing the result with the reference projects.
     * @throws Exception 
     */
    protected void replace(ResourceMapping mapper) throws Exception {
        SyncInfoTree incomingSet = getIncoming(mapper.getProjects());
        replace(new ResourceMapping[] { mapper });
        assertUpdate(mapper, incomingSet);
    } 

    /**
     * Commit and check that all resources in containing project that should have been committed were and
     * that any not contained by the mappers were not.
     * @throws CoreException 
     * @see org.eclipse.team.tests.ccvs.core.EclipseTest#commit(org.eclipse.core.resources.mapping.IResourceMapper[], java.lang.String)
     */
    protected void commit(ResourceMapping mapper, String message) throws CoreException {
        SyncInfoTree set = getOutgoing(mapper.getProjects());
        commit(new ResourceMapping[] { mapper }, message);
        assertCommit(mapper, set);
    }
    
    /**
     * Tag the given resource mappings and assert that only the resources
     * within the mapping were tagged.
     * @throws CoreException 
     */
    protected void tag(ResourceMapping mapping, CVSTag tag) throws CoreException {
        tag(new ResourceMapping[] { mapping }, tag, false);
        assertTagged(mapping, tag);
    }
    
    /**
     * Branch the resources in the given mapping.
     * @throws CoreException 
     * @throws IOException 
     */
    protected void branch(ResourceMapping mapping, CVSTag branch) throws CoreException, IOException {
        CVSTag version = new CVSTag("Root_" + branch.getName(), CVSTag.VERSION);
        branch(new ResourceMapping[] { mapping }, version, branch, true /* update */);
        assertTagged(mapping, version);
        assertBranched(mapping, branch);
    }
    
    /**
     * Add any resources contained by the mapping
     * @param mapping
     * @throws CoreException 
     */
    protected void add(ResourceMapping mapping) throws CoreException {
        SyncInfoTree set = getUnaddedResource(mapping);
        add(new ResourceMapping[] { mapping });
        assertAdded(mapping, set);
    }

    private void assertAdded(ResourceMapping mapping, final SyncInfoTree set) throws CoreException {
        // Assert that all resources covered by the mapping are now under version control (i.e. are in-sync)
        // Remove the resources contained in the mapping from the set of unadded resources.
        visit(mapping, ResourceMappingContext.LOCAL_CONTEXT, new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                ICVSResource cvsResource = getCVSResource(resource);
                assertTrue("Resource was not added but should have been: " + resource.getFullPath(), 
                        (cvsResource.isManaged() 
                                || (cvsResource.isFolder() 
                                        && ((ICVSFolder)cvsResource).isCVSFolder())));
                set.remove(resource);
                return true;
            }
        });
        // Assert that the remaining unadded resources are still unadded
        SyncInfo[] infos = set.getSyncInfos();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            ICVSResource cvsResource = getCVSResource(info.getLocal());
            assertTrue("Resource was added but should not have been: " + info.getLocal().getFullPath(), !cvsResource.isManaged());
        }
    }

    /*
     * Need to ensure that only the resources contained in the mapping
     * have the branch tag associated with them.
     */
	private void assertBranched(ResourceMapping mapping, CVSTag branch) throws CoreException {
        // First, make sure the proper resources are tagged in the repo
        assertTagged(mapping, branch);
        // Now make sure the proper local files are tagged
        final Map remotes = getTaggedRemoteFilesByPath(mapping, branch);
        final Map locals = getTaggedLocalFilesByPath(mapping, branch);
        for (Iterator iter = remotes.keySet().iterator(); iter.hasNext();) {
            String key = (String)iter.next();
            ICVSRemoteFile remote = (ICVSRemoteFile)remotes.get(key);
            ICVSFile local = (ICVSFile)locals.get(key);
            assertNotNull("Remotely tagged resource was not tagged locally: " + remote.getRepositoryRelativePath(), local);
            assertEquals(local.getIResource().getParent().getFullPath(), remote, local, false, false /* include tags */);
            assertEquals("Remotely tagged resource was not tagged locally: " + remote.getRepositoryRelativePath(), branch, local.getSyncInfo().getTag());
            locals.remove(key);
            iter.remove();
        }
        // The remote map should be empty after traversal
        for (Iterator iter = remotes.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            fail("Remote file " + path + " was tagged remotely but not locally.");
        }
        // The local map should be empty after traversal
        for (Iterator iter = locals.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            fail("Local file " + path + " was tagged locally but not remotely.");
        }
    }

    private void assertTagged(ResourceMapping mapping, final CVSTag tag) throws CoreException {
        final Map tagged = getTaggedRemoteFilesByPath(mapping, tag);
        // Visit all the resources in the traversal and ensure that they are tagged
        visit(mapping, ResourceMappingContext.LOCAL_CONTEXT, new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                if (resource.getType() == IResource.FILE) {
                    ICVSRemoteFile file = popRemote(resource, tagged);
                    assertNotNull("Resource was not tagged: " + resource.getFullPath(), file);
                }
                return true;
            }
        });
        
        // The tagged map should be empty after traversal
        for (Iterator iter = tagged.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            fail("Remote file " + path + " was tagged but should not have been.");
        }
    }

    private Map getTaggedLocalFilesByPath(ResourceMapping mapping, final CVSTag branch) throws CoreException {
        final Map tagged = new HashMap();
        IProject[] projects = mapping.getProjects();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            project.accept(new IResourceVisitor() {
                public boolean visit(IResource resource) throws CoreException {
                    if (resource.getType() == IResource.FILE) {
                        ICVSFile file = (ICVSFile)getCVSResource(resource);
                        ResourceSyncInfo info = file.getSyncInfo();
                        if (info != null && info.getTag() != null && info.getTag().equals(branch)) {
                            tagged.put(file.getRepositoryRelativePath(), file);
                        }
                    }
                    return true;
                }
            });
        }
        return tagged;
    }
    
    private Map getTaggedRemoteFilesByPath(ResourceMapping mapping, final CVSTag tag) throws CVSException {
        IProject[] projects = mapping.getProjects();
        ICVSResource[] remotes = getRemoteTrees(projects, tag);
        final Map tagged = getFilesByPath(remotes);
        return tagged;
    }

    private ICVSResource[] getRemoteTrees(IProject[] projects, CVSTag tag) throws CVSException {
        List result = new ArrayList();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            RemoteFolderTree tree = RemoteFolderTreeBuilder.buildRemoteTree(getRepository(), project, tag, DEFAULT_MONITOR);
            result.add(tree);
        }
        return (ICVSResource[]) result.toArray(new ICVSResource[result.size()]);
    }

    private Map getFilesByPath(ICVSResource[] remotes) throws CVSException {
        Map result = new HashMap();
        for (int i = 0; i < remotes.length; i++) {
            ICVSResource resource = remotes[i];
            collectFiles(resource, result);
        }
        return result;
    }

    private void collectFiles(ICVSResource resource, Map result) throws CVSException {
        if (resource.isFolder()) {
            ICVSResource[] members = ((ICVSFolder)resource).members(ICVSFolder.ALL_EXISTING_MEMBERS);
            for (int i = 0; i < members.length; i++) {
                ICVSResource member = members[i];
                collectFiles(member, result);
            }
        } else {
            result.put(resource.getRepositoryRelativePath(), resource);
        } 
    }

    private ICVSRemoteFile popRemote(IResource resource, Map tagged) throws CVSException {
        ICVSResource cvsResource = getCVSResource(resource);
        ICVSRemoteFile remote = (ICVSRemoteFile)tagged.get(cvsResource.getRepositoryRelativePath());
        if (remote != null) {
            tagged.remove(remote.getRepositoryRelativePath());
        }
        return remote;
    }
    
    private ResourceMapping asResourceMapping(final IResource[] resources, final int depth) {
        return new ResourceMapping() {
        	private Object object = new Object();
            public Object getModelObject() {
                return object;
            }
            public IProject[] getProjects() {
                return getProjects(resources);
            }
            private IProject[] getProjects(IResource[] resources) {
                Set projects = new HashSet();
                for (int i = 0; i < resources.length; i++) {
                    IResource resource = resources[i];
                    projects.add(resource.getProject());
                }
                return (IProject[]) projects.toArray(new IProject[projects.size()]);
            }
            public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) throws CoreException {
                return new ResourceTraversal[] {
                        new ResourceTraversal(resources, depth, IResource.NONE)
                    };
            }
			public String getModelProviderId() {
				return "org.eclipse.team.tests.cvs.core.modelProvider";
			}
        };
    }
    
    private void assertUpdate(ResourceMapping mapper, final SyncInfoTree set) throws Exception {
        final Exception[] exception = new Exception[] { null };
        visit(mapper, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                SyncInfo info = set.getSyncInfo(resource);
                if (info != null) {
                    set.remove(resource);
                    try {
                        // Assert that the local sync info matches the remote info
                        assertEquals(resource.getParent().getFullPath(), getCVSResource(resource), (ICVSResource)info.getRemote(), false, false);
                    } catch (CVSException e) {
                        exception[0] = e;
                    } catch (CoreException e) {
                        exception[0] = e;
                    } catch (IOException e) {
                        exception[0] = e;
                    }
                }
                return true;
            }
        });
        if (exception[0] != null) throw exception[0];
        
        // check the the state of the remaining resources has not changed
        assertUnchanged(set);
    }

    private void assertCommit(ResourceMapping mapper, final SyncInfoTree set) throws CoreException {
        visit(mapper, new SyncInfoSetTraveralContext(set), new IResourceVisitor() {
            public boolean visit(IResource resource) throws CoreException {
                SyncInfo info = set.getSyncInfo(resource);
                if (info != null) {
                    set.remove(resource);
                    assertTrue("Committed resource is not in-sync: " + resource.getFullPath(), getSyncInfo(resource).getKind() == SyncInfo.IN_SYNC);
                }
                return true;
            }
        });
        // check the the state of the remaining resources has not changed
        assertUnchanged(set);
    }

    /*
     * Assert that the state of the resources in the set have not changed
     */
    private void assertUnchanged(SyncInfoTree set) throws TeamException {
        //TODO: Need to refresh the subscriber since flush of remote state is deep
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(set.getResources(), IResource.DEPTH_ZERO, DEFAULT_MONITOR);
        SyncInfo[] infos = set.getSyncInfos();
        for (int i = 0; i < infos.length; i++) {
            SyncInfo info = infos[i];
            assertUnchanged(info);
        }
    }

    private void assertUnchanged(SyncInfo info) throws TeamException {
        SyncInfo current = getSyncInfo(info.getLocal());
        assertEquals("The sync info changed for " + info.getLocal().getFullPath(), info, current);
    }

    private SyncInfo getSyncInfo(IResource local) throws TeamException {
        return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().getSyncInfo(local);
    }

    private SyncInfoTree getIncoming(IProject[] projects) throws TeamException {
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(projects, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
        SyncInfoTree set = getAllOutOfSync(projects);
        set.removeOutgoingNodes();
        set.removeConflictingNodes();
        return set;
    }
    
    private SyncInfoTree getOutgoing(IProject[] projects) {
        SyncInfoTree set = getAllOutOfSync(projects);
        set.removeIncomingNodes();
        set.removeConflictingNodes();
        return set;
    }

    private SyncInfoTree getUnaddedResource(ResourceMapping mapping) {
        SyncInfoTree set = getAllOutOfSync(mapping.getProjects());
        set.selectNodes(new FastSyncInfoFilter() {
            public boolean select(SyncInfo info) {
                try {
                    if (info.getLocal().getType() != IResource.PROJECT && info.getRemote() == null && info.getBase() == null) {
                        ICVSResource resource = getCVSResource(info.getLocal());
                        return !resource.isManaged();
                    }
                } catch (CVSException e) {
                    fail(e.getMessage());
                }
                return false;
            }
        });
        return set;
    }
    
    private SyncInfoTree getAllOutOfSync(IProject[] projects) {
        SyncInfoTree set = new SyncInfoTree();
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().collectOutOfSync(projects, IResource.DEPTH_INFINITE, set, DEFAULT_MONITOR);
        return set;
    }
    
    private IResourceDiffTree getAllDiffs(IProject[] projects) throws CoreException {
        final ResourceDiffTree tree = new ResourceDiffTree();
        CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().accept(projects, IResource.DEPTH_INFINITE, new IDiffVisitor() {
			public boolean visit(IDiff delta) {
				tree.add(delta);
				return true;
			}
		});
        return tree;
    }
    
    private void visit(ResourceMapping mapper, ResourceMappingContext context, IResourceVisitor visitor) throws CoreException {
        ResourceTraversal[] traversals = mapper.getTraversals(context, null);
        for (int i = 0; i < traversals.length; i++) {
            ResourceTraversal traversal = traversals[i];
            visit(traversal, context, visitor);
        }
    }

    private void visit(ResourceTraversal traversal, ResourceMappingContext context, IResourceVisitor visitor) throws CoreException {
        IResource[] resources = traversal.getResources();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            visit(resource, visitor, context, traversal.getDepth());
        }
    }

    private void visit(IResource resource, IResourceVisitor visitor, ResourceMappingContext context, int depth) throws CoreException {
       if (!visitor.visit(resource) || depth == IResource.DEPTH_ZERO || resource.getType() == IResource.FILE) return;
       Set members = new HashSet();
       members.addAll(Arrays.asList(((IContainer)resource).members(false)));
       if (context instanceof RemoteResourceMappingContext) {
           RemoteResourceMappingContext remoteContext = (RemoteResourceMappingContext) context;  
           members.addAll(Arrays.asList(remoteContext.fetchMembers((IContainer)resource, DEFAULT_MONITOR)));
       }
       for (Iterator iter = members.iterator(); iter.hasNext();) {
           IResource member = (IResource) iter.next();
           visit(member, visitor, context, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
       }
    }
    
	private boolean isTimeout(Throwable e) {
		if (e == null) {
			return false;
		}

		if (e instanceof InterruptedIOException
				&& e.getMessage() != null
				&& e.getMessage().indexOf(
						"Timeout while writing to output stream") >= 0) {
			return true;
		}

		if (e instanceof CoreException) {
			CoreException ce = (CoreException) e;
			if (ce.getStatus() != null && ce.getStatus().isMultiStatus()) {
				MultiStatus multistatus = (MultiStatus) ce.getStatus();
				for (int i = 0; i < multistatus.getChildren().length; i++) {
					if (isTimeout(multistatus.getChildren()[i].getException())) {
						return true;
					}
				}
			}

		}

		return isTimeout(e.getCause());
	}

    public void testUpdate() throws Exception {
    	try{
	        // Create a test project, import it into cvs and check it out
	        IProject project = createProject("testUpdate", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt" });
	
	        // Check the project out under a different name
	        IProject copy = checkoutCopy(project, "-copy");
	        
	        // Perform some operations on the copy and commit them all
	        addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
	        setContentsAndEnsureModified(copy.getFile("changed.txt"));
	        deleteResources(new IResource[] {copy.getFile("deleted.txt")});
	        setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
	        setContentsAndEnsureModified(copy.getFile("folder1/subfolder1/c.txt"));
	        commit(asResourceMapping(new IResource[] { copy }, IResource.DEPTH_INFINITE), "A commit message");
	        
	        // Update the project using depth one and ensure we got only what was asked for
	        update(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), null);
	        
	        // Update a subfolder using depth one and ensure we got only what was asked for
	        update(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE), null);
	        
	        // Update the specific file
	        update(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO), null);
	        
	        // Update the remaining resources
	        update(asResourceMapping(new IResource[] { project.getFolder("folder2") }, IResource.DEPTH_INFINITE), null);
	        assertEquals(project, copy);
		} catch (Exception e) {
			if (isTimeout(e)) {
				//TODO see Bug 399375
				System.err.println("Timeout while testUpdate");
				e.printStackTrace();
				return;
			}
			throw e;
		}
    }
    
    public void testReplace() throws Exception {
    	try{
	        // Create a test project, import it into cvs and check it out
	        IProject project = createProject("testReplace", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt" });
	
	        // Check the project out under a different name
	        IProject copy = checkoutCopy(project, "-copy");
	        
	        // Perform some operations on the copy and commit them all
	        addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
	        setContentsAndEnsureModified(copy.getFile("changed.txt"));
	        deleteResources(new IResource[] {copy.getFile("deleted.txt")});
	        setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
	        setContentsAndEnsureModified(copy.getFile("folder1/subfolder1/c.txt"));
	        commit(asResourceMapping(new IResource[] { copy }, IResource.DEPTH_INFINITE), "A commit message");
	        
	        // Update the project using depth one and ensure we got only what was asked for
	        replace(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE));
	        
	        // Update a subfolder using depth one and ensure we got only what was asked for
	        deleteResources(new IResource[] {project.getFile("folder1/b.txt")});
	        replace(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE));
	        
	        // Update the specific file
	        replace(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO));
	        
	        // Update the remaining resources
	        replace(asResourceMapping(new IResource[] { project.getFolder("folder2") }, IResource.DEPTH_INFINITE));
	        assertEquals(project, copy);
		} catch (Exception e) {
			if (isTimeout(e)) {
				//TODO see Bug 399375
				System.err.println("Timeout while testReplace");
				e.printStackTrace();
				return;
			}
			throw e;
		}
    }

    public void testCommit() throws Exception {
		if (TeamCVSTestPlugin.IS_UNSTABLE_TEST && Util.isMac())
			return;

        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testCommit", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt" });
        
        // Perform some operations on the copy and commit only the top level
        addResources(project, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
        setContentsAndEnsureModified(project.getFile("changed.txt"));
        deleteResources(new IResource[] {project.getFile("deleted.txt")});
        setContentsAndEnsureModified(project.getFile("folder1/a.txt"));
        setContentsAndEnsureModified(project.getFile("folder1/subfolder1/c.txt"));
        
        // Commit the project shallow
        commit(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), "A commit message");
        
        // Commit a subfolder shallow
        commit(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE), "A commit message");
        
        // Now commit the file specifically
        commit(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO), "A commit message");
        
        // Now commit the rest
        commit(asResourceMapping(new IResource[] { project.getFolder("folder2") }, IResource.DEPTH_INFINITE), "A commit message");
        
        // Check the project out under a different name
        IProject copy = checkoutCopy(project, "-copy");
        assertEquals(project, copy);
    }
    
    public void testTag() throws Exception {
        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testTag", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt" });

        tag(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), new CVSTag("v1", CVSTag.VERSION));
        tag(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE), new CVSTag("v2", CVSTag.VERSION));
        tag(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO), new CVSTag("v3", CVSTag.VERSION));
        tag(asResourceMapping(new IResource[] { project}, IResource.DEPTH_INFINITE), new CVSTag("v4", CVSTag.VERSION));
    }
    
    public void testBranch() throws Exception {

		if (TeamCVSTestPlugin.IS_UNSTABLE_TEST)
			return;

        // Create a test project, import it into cvs and check it out
        IProject project = createProject("testBranch", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt"  });

        branch(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE), new CVSTag("b1", CVSTag.BRANCH));
        branch(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE), new CVSTag("b2", CVSTag.BRANCH));
        branch(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO), new CVSTag("b3", CVSTag.BRANCH));
        branch(asResourceMapping(new IResource[] { project }, IResource.DEPTH_INFINITE), new CVSTag("b4", CVSTag.BRANCH));
    }
    
    public void testAdd() throws TeamException, CoreException {
        // Create an empty project
        IProject project = createProject("testAdd", new String[] { });
        // add some resources
        buildResources(project, new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt"  }, false);
        // add them to CVS
        add(asResourceMapping(new IResource[] { project }, IResource.DEPTH_ONE));
        add(asResourceMapping(new IResource[] { project.getFolder("folder1") }, IResource.DEPTH_ONE));
        add(asResourceMapping(new IResource[] { project.getFile("folder1/subfolder1/c.txt") }, IResource.DEPTH_ZERO));
        add(asResourceMapping(new IResource[] { project }, IResource.DEPTH_INFINITE));
    }

    public void testCacheBase() throws TeamException, CoreException {
    	IProject project = createProject("testCacheBase", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt"  });
    	IProject copy = checkoutCopy(project, "-copy");

    	// First, make some local changes and then cache the bases
    	setContentsAndEnsureModified(project.getFile("changed.txt"), "Uncommitted text");
    	setContentsAndEnsureModified(project.getFile("folder1/b.txt"));
    	project.getFile("deleted.txt").delete(false, true, null);
    	try {
    		cacheBase(project, true /* cache for outgoing and conflicting */);
    		cacheBase(project, false /* cache for conflicting only*/);

    		// Next, retry after releasing some changes (to ensure proper contents are fetched)
    		setContentsAndEnsureModified(copy.getFile("changed.txt"), "Text comited from the copy");
    		commitProject(copy);
    		cacheBase(project, true /* cache for outgoing and conflicting */);
    		cacheBase(project, false /* cache for conflicting only */);
    	} catch (TeamException e) {
    		// see bug 325553
    		logIfCausedByInterruptedIOException(e);
    	}
    }

	private void logIfCausedByInterruptedIOException(TeamException e)
			throws TeamException {
		IStatus status = e.getStatus();
		if (status.isMultiStatus()) {
			MultiStatus mstatus = (MultiStatus) status;
			status = mstatus.getChildren()[0];
			if (status instanceof TeamStatus) {
				Throwable ex = status.getException();
				if (ex instanceof CVSCommunicationException) {
					CVSCommunicationException cce = (CVSCommunicationException) ex;
					status = cce.getStatus();
					if (status.isMultiStatus()) {
						if (status.getException() instanceof InterruptedIOException) {
							// Prevent the test from failing but log the exception
							log("org.eclipse.team.tests.cvs.core", e.getStatus());
							return;
						}
					}
				}
			}
		}
		throw e;
	}

    public void testCacheRemote() throws TeamException, CoreException {
        IProject project = createProject("testCacheRemote", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder1/subfolder1/c.txt"  });
        IProject copy = checkoutCopy(project, "-copy");
        
        // Make some remote changes
        setContentsAndEnsureModified(copy.getFile("changed.txt"), "Uncommitted text");
        setContentsAndEnsureModified(copy.getFile("folder1/b.txt"));
        commitProject(copy);
        // Delete a local file
        project.getFile("deleted.txt").delete(false, true, null);
        cacheRemote(project);
    }

	private void cacheRemote(IProject project) throws CoreException {
		clearCache(project);
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(new IProject[] { project }, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
		IResourceDiffTree tree = getAllDiffs(new IProject[] { project });
		ResourceMapping[] mappings = new ResourceMapping[] {new SimpleResourceMapping(project)};
		CacheRemoteContentsOperation op = new CacheRemoteContentsOperation(null, mappings, tree);
		executeHeadless(op);
		ensureRemoteCached(tree);
	}

	private void cacheBase(IProject project, boolean includeOutgoing) throws CoreException {
		clearCache(project);
		CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber().refresh(new IProject[] { project }, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
		IResourceDiffTree tree = getAllDiffs(new IProject[] { project });
		ResourceMapping[] mappings = new ResourceMapping[] {new SimpleResourceMapping(project)};
		CacheBaseContentsOperation op = new CacheBaseContentsOperation(null, mappings, tree, includeOutgoing);
		executeHeadless(op);
		ensureBaseCached(tree, includeOutgoing);
	}

	private void ensureRemoteCached(IResourceDiffTree tree) {
		IResource[] resources = tree.getAffectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			IDiff node = tree.getDiff(resource);
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				IResourceVariant remote = SyncInfoToDiffConverter.getRemoteVariant(twd);
				if (remote != null) {
					boolean isCached = ((CachedResourceVariant)remote).isContentsCached();
					int direction = twd.getDirection();
					if (direction == IThreeWayDiff.CONFLICTING || direction == IThreeWayDiff.INCOMING) {
						assertTrue(NLS.bind("The remote contents should be cached for {0}", new String[] {resource.getFullPath().toString()}), isCached);
					} else {
						assertFalse(NLS.bind("The base contents should NOT be cached for {0}", new String[] {resource.getFullPath().toString()}), isCached);
					}
				}
			}
		}
	}
	
	private void ensureBaseCached(IResourceDiffTree tree, boolean includeOutgoing) throws TeamException, CoreException {
		IResource[] resources = tree.getAffectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			IDiff node = tree.getDiff(resource);
			if (node instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) node;
				IResourceVariant base = SyncInfoToDiffConverter.getBaseVariant(twd);
				if (base != null) {
					boolean isCached = ((CachedResourceVariant)base).isContentsCached();
					int direction = twd.getDirection();
					if (direction == IThreeWayDiff.CONFLICTING || (includeOutgoing && direction == IThreeWayDiff.OUTGOING)) {
						assertTrue(NLS.bind("The base contents should be cached for {0}", new String[] {resource.getFullPath().toString()}), isCached);
						// For conflicts, ensure that the cache contents do not match the remote
						if (direction == SyncInfo.CONFLICTING) {
							IResourceVariant remote = SyncInfoToDiffConverter.getRemoteVariant(twd);
							if (remote != null) {
								InputStream baseIn = base.getStorage(DEFAULT_MONITOR).getContents();
								if (baseIn == null) {
									fail(NLS.bind("Base was not fetched for {0}", new String[] {resource.getFullPath().toString()}));
								}
								InputStream remoteIn = remote.getStorage(DEFAULT_MONITOR).getContents();
								if (compareContent(baseIn, remoteIn)) {
									fail(NLS.bind("The remote was fetched instead of the base for {0}", new String[] {resource.getFullPath().toString()}));
								}
							}
						}
					} else {
						// assertFalse(NLS.bind("The base contents should NOT be cached for {0}", new String[] {resource.getFullPath().toString()}), isCached);
					}
				}
			}
		}
	}

	private void clearCache(IProject project) {
		ResourceVariantCache cache = ResourceVariantCache.getCache(CVSProviderPlugin.ID);
		if (cache != null) {
			ResourceVariantCacheEntry[] entries = cache.getEntries();
			for (int i = 0; i < entries.length; i++) {
				ResourceVariantCacheEntry entry = entries[i];
				entry.dispose();
			}
		}
	}
    
	public void testBug134517() throws Exception {
        IProject project = createProject("testBug134517", new String[] { "file1.txt", "file2.txt"});
        IProject copy = checkoutCopy(project, "-copy");
        addResources(copy, new String[] { "file0.txt", 
        		"new_folder1/", "new_folder1/file2.txt", "new_folder1/new_folder2/", 
        		"new_folder1/new_folder2/new_folder3/", "new_folder1/new_folder2/new_folder3/file3.txt"  }, true);
        IResource[] resources = new IResource[] {
        		project.getFile("file0.txt"),
        		project.getFile("file1.txt"),
        		project.getFile("new_folder1/file2.txt"),
        		project.getFile("new_folder1/new_folder2/new_folder3/file3.txt")
        };
        update(asResourceMapping(resources, IResource.DEPTH_ZERO), null);
        assertEquals(project, copy);
	}
	
	public void testDeepNewFolder() throws Exception {
        IProject project = createProject("testBug134517", new String[] { "file1.txt", "file2.txt"});
        IProject copy = checkoutCopy(project, "-copy");
        addResources(copy, new String[] {
        		"new_folder1/", 
        		"new_folder1/new_folder2/", 
        		"new_folder1/new_folder2/new_folder3/", 
        		"new_folder1/new_folder2/new_folder3/file3.txt"  }, true);
        IResource[] resources = new IResource[] {
        		project.getFolder("new_folder1/new_folder2/new_folder3/")
        };
        update(asResourceMapping(resources, IResource.DEPTH_INFINITE), null);
        assertEquals(project, copy);
	}
}
