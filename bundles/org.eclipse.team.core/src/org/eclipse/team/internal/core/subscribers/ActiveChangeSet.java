/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.core.subscribers;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.TeamPlugin;
import org.osgi.service.prefs.Preferences;

/**
 * An active change set represents a set of local resource changes
 * that are grouped together as a single logical change.
 * @since 3.1
 */
public class ActiveChangeSet extends DiffChangeSet {

    private static final String CTX_TITLE = "title"; //$NON-NLS-1$
    private static final String CTX_COMMENT = "comment"; //$NON-NLS-1$
    private static final String CTX_RESOURCES = "resources"; //$NON-NLS-1$
    private static final String CTX_USER_CREATED = "userCreated"; //$NON-NLS-1$

    private final ActiveChangeSetManager manager;
    private String comment;
	private boolean userCreated = true;

	/**
	 * Create a change set with the given title
	 * @param manager the manager that owns this set
     * @param title the title of the set
     */
    public ActiveChangeSet(ActiveChangeSetManager manager, String title) {
        super(title);
        this.manager = manager;
    }

    /**
     * Get the title of the change set. The title is used
     * as the comment when the set is checking in if no comment
     * has been explicitly set using <code>setComment</code>.
     * @return the title of the set
     */
    public String getTitle() {
        return getName();
    }

    /**
     * Set the title of the set. The title is used
     * as the comment when the set is committed if no comment
     * has been explicitly set using <code>setComment</code>.
     * @param title the title of the set
     */
    public void setTitle(String title) {
        setName(title);
        getManager().fireNameChangedEvent(this);
    }

    /**
     * Get the comment of this change set. If the comment
     * as never been set, the title is returned as the comment
     * @return the comment to be used when the set is committed
     */
    public String getComment() {
        if (comment == null) {
            return getTitle();
        }
        return comment;
    }

    /**
     * Set the comment to be used when the change set is committed.
     * If <code>null</code> is passed, the title of the set
     * will be used as the comment.
     * @param comment the comment for the set or <code>null</code>
     * if the title should be the comment
     */
    public void setComment(String comment) {
        if (comment != null && comment.equals(getTitle())) {
            this.comment = null;
        } else {
            this.comment = comment;
        }
    }

    /*
     * Override inherited method to only include outgoing changes
     */
    protected boolean isValidChange(IDiff diff) {
        return getManager().isModified(diff);
    }

    private void addResource(IResource resource) throws CoreException {
        IDiff diff = getManager().getDiff(resource);
        if (diff != null) {
            add(diff);
        }
    }

    private ActiveChangeSetManager getManager() {
        return manager;
    }

    /**
     * Return whether the set has a comment that differs from the title.
     * @return whether the set has a comment that differs from the title
     */
    public boolean hasComment() {
        return comment != null;
    }

    public void save(Preferences prefs) {
        prefs.put(CTX_TITLE, getTitle());
        if (comment != null) {
            prefs.put(CTX_COMMENT, comment);
        }
        if (!isEmpty()) {
	        StringBuffer buffer = new StringBuffer();
	        IResource[] resources = getResources();
	        for (int i = 0; i < resources.length; i++) {
                IResource resource = resources[i];
	            buffer.append(resource.getFullPath().toString());
	            buffer.append('\n');
	        }
	        prefs.put(CTX_RESOURCES, buffer.toString());
        }
        prefs.putBoolean(CTX_USER_CREATED, isUserCreated());
    }

    public void init(Preferences prefs) {
        setName(prefs.get(CTX_TITLE, "")); //$NON-NLS-1$
        comment = prefs.get(CTX_COMMENT, null);
        String resourcePaths = prefs.get(CTX_RESOURCES, null);
        if (resourcePaths != null) {
            ResourceDiffTree tree = internalGetDiffTree();
            try {
                tree.beginInput();
	            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
	            StringTokenizer tokenizer = new StringTokenizer(resourcePaths, "\n"); //$NON-NLS-1$
	            while (tokenizer.hasMoreTokens()) {
	                String next = tokenizer.nextToken();
	                if (next.trim().length() > 0) {
	                    IResource resource = getResource(root, next);
                        // Only include the resource if it is out-of-sync
                        try {
							if (resource != null && getManager().getDiff(resource) != null) {
								addResource(resource);
							}
						} catch (CoreException e) {
							TeamPlugin.log(e);
						}
	                }
	            }
            } finally {
                tree.endInput(null);
            }
        }
        userCreated = prefs.getBoolean(CTX_USER_CREATED, true);
    }

    private IResource getResource(IWorkspaceRoot root, String next) {
        IResource resource = root.findMember(next);
        if (resource == null) {
            // May be an outgoing deletion
            Path path = new Path(null, next);
            if (next.charAt(next.length()-1) == IPath.SEPARATOR) {
                if (path.segmentCount() == 1) {
                    // resource is a project
                    resource = root.getProject(path.lastSegment());
                } else {
                    // resource is a folder
                    resource = root.getFolder(path);
                }
            } else {
                // resource is a file
                resource = root.getFile(path);
            }
        }
        return resource;
    }

    /**
     * Add the resources to the change set if they are outgoing changes.
     * @param resources the resources to add.
     * @throws CoreException
     */
    public void add(IResource[] resources) throws CoreException {
        List toAdd = new ArrayList();
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            IDiff diff = getManager().getDiff(resource);
            if (diff != null) {
                toAdd.add(diff);
            }
        }
        if (!toAdd.isEmpty()) {
            add((IDiff[]) toAdd.toArray(new IDiff[toAdd.size()]));
        }
    }

    /**
     * Set whether this set was created by the user.
     * @param userCreated whether this set was created by the user
     */
	public void setUserCreated(boolean userCreated) {
		this.userCreated = userCreated;
	}

	/**
	 * Return whether this set was created by the user.
	 * @return whether this set was created by the user
	 */
	public boolean isUserCreated() {
		return userCreated;
	}
}
