package org.eclipse.ant.internal.core.old;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import java.util.Vector;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.PatternSet;

/**
 * An Ant task which refreshes the Eclipse Platform's view of the local filesystem.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost 
 * certainly be broken (repeatedly) as the API evolves.
 * </p>
 * @see IResource#refreshLocal
 */
public class RefreshLocal extends Task {
	/**
	 * Unique identifier constant (value <code>"DEPTH_ZERO"</code>)
	 * indicating that refreshes should be performed only on the target
	 * resource itself
	 */
	public static final String DEPTH_ZERO = "zero";
	
	/**
	 * Unique identifier constant (value <code>"DEPTH_ONE"</code>)
	 * indicating that refreshes should be performed on the target
	 * resource and its children
	 */
	public static final String DEPTH_ONE = "one";
	
	/**
	 * Unique identifier constant (value <code>"DEPTH_INFINITE"</code>)
	 * indicating that refreshes should be performed on the target
	 * resource and all of its recursive children
	 */
	public static final String DEPTH_INFINITE = "infinite";
	
	/**
	 * The resource to refresh.
	 */
	protected IResource resource;
	
	/**
	 * The depth to refresh to.
	 */
	protected int depth = IResource.DEPTH_INFINITE;
	
	private IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

/**
 * Constructs a new <code>RefreshLocal</code> instance.
 */
public RefreshLocal() {
	super();
}

/**
 * Performs the refresh operation.
 * 
 * @exception BuildException thrown if a problem occurs during execution.
 */
public void execute() throws BuildException {
	// make sure we don't have an illegal set of options
	validateAttributes();

	// deal with the single resource
	if (resource != null) {
		if (!resource.exists())
			throw new BuildException(Policy.bind("exception.resourceNotFound",resource.toString()));
		try {
			resource.refreshLocal(depth, null);
			return;
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	} else
		throw new BuildException(Policy.bind("exception.resourceNotSpecified"));
}
/**
 * Refreshes a collection of resources.
 * 
 * @param resources the names of the resources to refresh
 * @exception BuildException thrown if a problem occurs during refresh
 */
protected void refreshResources(String[] resources) throws BuildException {
	for (int i = 0; i < resources.length; i++) {
		IResource target = root.findMember(resources[i]);
		if (target == null)
			throw new BuildException(Policy.bind("exception.resourceNotFound",resources[i].toString()));
		try {
			target.refreshLocal(depth, null);
		} catch (CoreException e) {
			throw new BuildException(e);
		}
	}
}

/**
 * Sets the depth of this task appropriately.  The specified argument must
 * by one of <code>RefreshLocal.DEPTH_ZERO</code>, <code>RefreshLocal.DEPTH_ONE</code>
 * or <code>RefreshLocal.DEPTH_INFINITE</code>.
 * 
 * @param value the depth to refresh to		
 */
public void setDepth(String value) {
	if (DEPTH_ZERO.equalsIgnoreCase(value))
		depth = IResource.DEPTH_ZERO;
	else
		if (DEPTH_ONE.equalsIgnoreCase(value))
			depth = IResource.DEPTH_ONE;
		else
			if (DEPTH_INFINITE.equalsIgnoreCase(value))
				depth = IResource.DEPTH_INFINITE;
}

/**
 * Sets the root of the workspace resource tree to refresh.
 * 
 * @param value the root value
 */
public void setResource(String value) {
	resource = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(value));
}

/**
 * Performs a validation of the receiver.
 * 
 * @exception BuildException thrown if a problem occurs during validation.
 */
protected void validateAttributes() throws BuildException {
}
}
