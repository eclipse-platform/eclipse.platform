package org.eclipse.debug.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * The launch manager manages the set of registered launches, maintaining
 * a collection of active processes and debug targets. Clients interested
 * in launch notification may register with the launch manager.
 * <p>
 * For convenience, a default launcher may be associated with a project.
 * The preference is stored as a persistent property with the project.
 * </p>
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see ILaunch
 * @see ILaunchListener
 */
public interface ILaunchManager {
	/**
	 * A launch in a normal, non-debug mode(value <code>"run"</code>).
	 */
	public static final String RUN_MODE= "run"; //$NON-NLS-1$
	/**
	 * A launch in a special debug mode (value <code>"debug"</code>).
	 */
	public static final String DEBUG_MODE= "debug"; //$NON-NLS-1$
	
	/**
	 * Adds the given listener to the collection of registered launch listeners.
	 * Has no effect if an identical listener is already registerd.
	 *
	 * @param listener the listener to register
	 */
	public void addLaunchListener(ILaunchListener listener);
	/**
	 * Deregisters the specified launch and notifies listeners. Has no effect
	 * if an identical launch is not already registered.
	 *
	 * @param launch the launch to deregister
	 */
	public void deregisterLaunch(ILaunch launch);
	/**
	 * Returns the launch the given process is contained in, or <code>null</code>
	 * if no registered launches contains the process.
	 *
	 * @param process the process for which to find a launch
	 * @return the launch containing the process, or <code>null</code> if none
	 */
	public ILaunch findLaunch(IProcess process);
	/**
	 * Returns the launch the given debug target is contained
	 * in, or <code>null</code> if no registered launches contain the debug target.
	 *
	 * @param target the debug target for which to find a launch
	 * @return the launch containing the debug target, or <code>null</code> if none	 
	 */
	public ILaunch findLaunch(IDebugTarget target);
	/**
	 * Returns the collection of debug targets currently registered with this
	 * launch manager.
	 *
	 * @return an array of debug targets
	 */
	public IDebugTarget[] getDebugTargets();
	/**
	 * Returns the default launcher for the given project,
	 * or <code>null</code> if no default launcher has been set.
	 * The default launcher is stored as a persistent property
	 * with a project.
	 *
	 * @param project the project for which to retrieve a default launcher
	 * @return the default launcher, or <code>null</code> if none has
	 *   been set for the project
	 * @exception CoreException if an error occurs accessing the
	 *   persistent property
	 */
	public ILauncher getDefaultLauncher(IProject project) throws CoreException;
	
	/**
	 * Returns the collection of registered launchers that can operate in the
	 * specified mode - run or debug.
	 *
	 * @return an array of launchers
	 */
	public ILauncher[] getLaunchers(String mode);
	
	/**
	 * Returns the collection of registered launchers.
	 *
	 * @return an array of launchers
	 */
	public ILauncher[] getLaunchers();
	/**
	 * Returns the collection of launches currently registered
	 * with this launch manager.
	 * 
	 * @return an array of launches
	 */
	public ILaunch[] getLaunches();
	/**
	 * Returns the collection of processes currently registered with this
	 * launch manager.
	 *
	 * @return an array of processes
	 */
	public IProcess[] getProcesses();
	/**
	 * Registers the specified launch with this launch manager
	 * and notifies listeners. Has no effect if an identical
	 * launch is already registered.
	 * 
	 * @param launch the launch to register
	 */
	public void registerLaunch(ILaunch launch);
	/**
	 * Removes the given listener from the collection of registered launch listeners.
	 * Has no effect if an identical listener is not already registerd.
	 *
	 * @param listener the listener to deregister
	 */
	public void removeLaunchListener(ILaunchListener listener);
	/**
	 * Sets the default launcher for the given project as a persistent property.
	 *
	 * @param project the project for which to set the preference
	 * @param launcher the default launcher preference
	 * @exception CoreException if an error occurs setting the persistent property
	 */
	public void setDefaultLauncher(IProject project, ILauncher launcher) throws CoreException;
	
	/**
	 * Returns all launch configurations defined by the specified
	 * project.
	 * 
	 * @param project a project
	 * @return all launch configurations defined by the specified
	 *  project
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(IProject project);
	
	/**
	 * Returns all launch configurations defined by the specified
	 * project of the specfied type.
	 * 
	 * @param project a project
	 * @param type a launch configuration type
	 * @return all launch configurations defined by the specified
	 *  project of the specified type
	 * @exception CoreException if an error occurrs while retreiving
	 *  a launch configuration
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(IProject project, ILaunchConfigurationType type) throws CoreException;
	
	/**
	 * Returns all launch configurations defined in the workspace.
	 * 
	 * @return all launch configurations defined in the workspace
	 */
	public ILaunchConfiguration[] getLaunchConfigurations();
	
	/**
	 * Returns all launch configurations defined in the workspace
	 * of the specified type
	 * 
	 * @param type a launch configuration type
	 * @return all launch configurations defined in the workspace
	 *  of the specified type
	 * @exception CoreException if an error occurrs while retreiving
	 *  a launch configuration
	 */
	public ILaunchConfiguration[] getLaunchConfigurations(ILaunchConfigurationType type) throws CoreException;
	
	/**
	 * Returns a handle to the launch configuration contained
	 * in the specified file. The file is not verified to exist
	 * or contain a launch configuration.
	 * 
	 * @param file launch configuration file
	 * @return a handle to the launch configuration contained
	 *  in the specified file
	 */
	public ILaunchConfiguration getLaunchConfiguration(IFile file);
	
	/**
	 * Returns a handle to the launch configuration specified by
	 * the given memento. The configuration may not exist.
	 * 
	 * @return a handle to the launch configuration specified by
	 *  the given memento
	 * @see ILaunchConfiguration#getMemento()
	 */
	public ILaunchConfiguration getLaunchConfiguration(String memento);
	
	/**
	 * Returns all defined launch configuration type extensions
	 * 
	 * @return all defined launch configuration type extensions
	 */
	public ILaunchConfigurationType[] getLaunchConfigurationTypes();
	
	/**
	 * Returns the lanuch configuration type extension with the specified
	 * id, or <code>null</code> if it does not exist.
	 * 
	 * @param id unique identifier for a launch configuration type extension
	 * @return the lanuch configuration type extension with the specified
	 * id, or <code>null</code> if it does not exist
	 */
	public ILaunchConfigurationType getLaunchConfigurationType(String id);
	
	/**
	 * Adds the given launch configuration listener to the list
	 * of listeners notified when a launch configuration is
	 * added, removed, or changed. Has no effect if the given listener
	 * is already registered.
	 * 
	 * @param listener launch configuration listener
	 */
	public void addLaunchConfigurationListener(ILaunchConfigurationListener listener);
	
	/**
	 * Removes the given launch configuration listener from the list
	 * of listeners notified when a launch configuration is
	 * added, removed, or changed. Has no effect if the given listener
	 * is not already registered.
	 * 
	 * @param listener launch configuration listener
	 */
	public void removeLaunchConfigurationListener(ILaunchConfigurationListener listener);	
	
}


