/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;
 
import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.team.core.RepositoryProvider;

/**
 * The <code>PessimisticFilesystemProvider</code> is a repository provider.
 * 
 * The provider manages a file named ".pessimistic" in each container it
 * controls.  This is where it stores metadata on which files it controls
 * in that container.  This file is considered to be controlled by the
 * provider and may be deleted.
 * 
 * The provider provides very simple checkin/checkout facilities by marking
 * files read-only to check them in and read-write to check them out.  It
 * also supports ignoring derived files.
 */
public class PessimisticFilesystemProvider extends RepositoryProvider  {
	/**
	 * The name of the file used to store metadata on which
	 * files are controlled by this provider.
	 */	
	private static final String CONTROL_FILE_NAME= ".pessimistic";
	/**
	 * The file modification validator for this provider.
	 */
	private FileModificationValidator validator;
	/**
	 * The cache of resources that are currently controlled.
	 * The cache is a map of parent resource -> set of controlled children.
	 */
	Map fControlledResources;
	
	/**
	 * Creates a new provider, required for team repository extension.
	 */
	public PessimisticFilesystemProvider() {
		validator = new PessimisticModificationValidator(this);
		fControlledResources= new HashMap(1);
	}		
	
	/**
	 * Adds the resources to the control of this provider.
	 */
	public void addToControl(final IResource[] resources, IProgressMonitor monitor) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Add to control:");
			if (resources != null) {
				for (int i= 0; i < resources.length; i++) {
					System.out.println("\t" + resources[i]);
				}
			} else {
				System.out.println("null resources");
			}
		}
		if (resources == null || resources.length == 0) {
			return;
		}
		final Set toAdd= new HashSet(resources.length);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int i = 0; i < resources.length; i++) {
					IResource resource= resources[i];
					if (!isControlled(resource)) {
						toAdd.add(resource);
					}
				}
				Map byParent= sortByParent(toAdd);

				monitor.beginTask("Adding to control", 1000);
				for (Iterator i= byParent.keySet().iterator(); i.hasNext();) {
					IContainer parent= (IContainer) i.next();
					Set controlledResources= (Set)fControlledResources.get(parent);
					if (controlledResources == null) {
						controlledResources= new HashSet(1);
						fControlledResources.put(parent, controlledResources);
					}
					controlledResources.addAll((Set)byParent.get(parent));
					writeControlFile(parent, monitor);
				}
				monitor.done();				
			}
		};
		run(runnable, monitor);
		fireStateChanged(toAdd, false);
	}

	/**
	 * Removes the resources from the control of this provider.
	 */
	public void removeFromControl(final IResource[] resources, IProgressMonitor monitor) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Remove from control:");
			if (resources != null) {
				for (int i= 0; i < resources.length; i++) {
					System.out.println("\t" + resources[i]);
				}
			} else {
				System.out.println("null resources");
			}
		}
		if (resources == null || resources.length == 0) {
			return;
		}
		final Set toRemove= new HashSet(resources.length);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int i = 0; i < resources.length; i++) {
					IResource resource= resources[i];
					if (isControlled(resource)) {	
						toRemove.add(resource);
					}
				}
				Map byParent= sortByParent(toRemove);

				monitor.beginTask("Removing from control", 1000);
				for (Iterator i= byParent.keySet().iterator(); i.hasNext();) {
					IContainer parent= (IContainer) i.next();
					Set controlledResources= (Set)fControlledResources.get(parent);
					if (controlledResources == null) {
						deleteControlFile(parent, monitor);
					} else {
						Set toRemove= (Set)byParent.get(parent);
						controlledResources.removeAll(toRemove);
						if (controlledResources.isEmpty()) {
							fControlledResources.remove(parent);
							deleteControlFile(parent, monitor);
						} else {
							writeControlFile(parent, monitor);
						}
						for (Iterator j= controlledResources.iterator(); j.hasNext();) {
							IResource resource= (IResource) j.next();
							if (!resource.exists()) {
								j.remove();
							}
						}
					}
				}
				monitor.done();
			}
		};
		run(runnable, monitor);
		fireStateChanged(toRemove, false);
	}

	/*
	 * Returns a map of IContainer -> Set of IResource.
	 */
	private Map sortByParent(Set resources) {
		Map byParent= new HashMap(1);
		for (Iterator i = resources.iterator(); i.hasNext();) {
			IResource resource= (IResource) i.next();
			IContainer parent= resource.getParent();
			Set set= (Set)byParent.get(parent);
			if (set == null) {
				set= new HashSet(1);
				byParent.put(parent, set);
			}
			set.add(resource);
		}
		return byParent;
	}	

	/*
	 * Deletes the control file for the given container.
	 */
	private void deleteControlFile(final IContainer container, IProgressMonitor monitor) {
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile controlFile= getControlFile(container, monitor);
				monitor.beginTask("Deleting control file " + controlFile, 1);
				if (controlFile.exists()) {
					controlFile.delete(true, false, monitor);
				}
				monitor.done();
			}
		};
		run(runnable, monitor);
	}

	/*
	 * Answers the control file for the given container.  If the control
	 * file exists, but is a directory, it will be deleted!
	 */
	private IFile getControlFile(IContainer container, IProgressMonitor monitor) throws CoreException {
		IResource child= container.findMember(CONTROL_FILE_NAME);
		if (child != null) {
			if (child.getType() == IResource.FILE) {
				return (IFile)child;
			}
			child.delete(true, monitor);
		}
		IFile controlFile= container.getFile(new Path(CONTROL_FILE_NAME));
		monitor.beginTask("Creating control file " + controlFile, 2);
		controlFile.create(new ByteArrayInputStream(new byte[0]), true, monitor);
		controlFile.setDerived(true);
		controlFile.setTeamPrivateMember(true);
		monitor.done();
		return controlFile;
	}

	/*
	 * Reads the contents of a control file, answering the set of
	 * resources that was specified in the file.
	 */	
	Set readControlFile(IFile controlFile) {
		Set controlledResources= new HashSet(1);
		if (controlFile.exists()) {
			InputStream in= null;
			try {
				try {
					in= controlFile.getContents(true);
				} catch (CoreException e) {
					PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Could not open stream on control file: " + controlFile);
				}
				DataInputStream dIn= new DataInputStream(in);
				int count= 0;
				try {
					count= dIn.readInt();
				} catch (IOException e) {
					// could be empty
				}
				if (count > 0) {
					try {
						for(int i= 0; i < count; i++) {
							String name= dIn.readUTF();
							IResource resource= getProject().findMember(new Path(name));
							if (resource != null) {
								controlledResources.add(resource);
							}
						}
					} catch (IOException e) {
						// corrupt control file
						try {
							controlFile.delete(true, null);
						} catch (CoreException ce) {
							PessimisticFilesystemProviderPlugin.getInstance().logError(ce, "Could not delete corrupt control file: " + controlFile);
						}
					}
				}
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems closing input stream on control file: " + controlFile);
					}
				}
			}
		}		
		return controlledResources;
	}

	/*
	 * Writes the currently controled resources to the control file for the container.
	 */
	private void writeControlFile(IContainer container, IProgressMonitor monitor) throws CoreException {
		IFile controlFile= getControlFile(container, monitor);
		Set controlledResources= (Set)fControlledResources.get(container);
		InputStream contents= generateControlFileContents(controlledResources);
		monitor.beginTask("Writing control file " + controlFile, 1000);
		if (contents == null) {
			controlFile.delete(true, false, monitor);
		} else {
			controlFile.setContents(contents, true, false, monitor);
		}
		monitor.done();
	}

	/*
	 * Generates an InputStream on a byte array which specifies 
	 * the resources given in controlledResources.
	 */
	private InputStream generateControlFileContents(Set controlledResources) {
		if (controlledResources == null || controlledResources.isEmpty()) {
			return null;
		}
		ByteArrayOutputStream byteOut= new ByteArrayOutputStream();
		DataOutputStream out= new DataOutputStream(byteOut);
		try {
			out.writeInt(controlledResources.size());
			for (Iterator i= controlledResources.iterator(); i.hasNext();) {
				IResource resource= (IResource) i.next();
				out.writeUTF(resource.getProjectRelativePath().toString());
			}
			out.flush();
		} catch (IOException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Unexpected problems during content generation");
		}
		return new ByteArrayInputStream(byteOut.toByteArray());
	}

	/*
	 * @see IProjectNature#setProject(IProject)
	 */
	public void setProject(IProject project) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Set project to " + project);
		}
		super.setProject(project);
		configureProject();		
	}
	
	/*
	 * @see IRepositoryProvider#getID()
	 */
	public String getID() {
		return PessimisticFilesystemProviderPlugin.NATURE_ID;
	}

	/*
	 * @see IRepositoryProvider#getFileModificationValidator()
	 */
	public IFileModificationValidator getFileModificationValidator() {
		return getFileModificationValidator();
	}
	
	public FileModificationValidator getFileModificationValidator2() {
		return validator;
	}
	
	/*
	 * @see IRepositoryProvider#deconfigure()
	 */
	public void deconfigure() {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Deconfigure " + getProject());
		}
		
		fControlledResources.clear();
		fireStateChanged(getSubtreeMembers(getProject()), true);
	}

	/*
	 * @see IRepositoryProvider#configure()
	 */
	public void configureProject() {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Configure " + getProject());
		}
		
		readControlFiles();
		fireStateChanged(getSubtreeMembers(getProject()), true);
	}

	/*
	 * Reads the control files located in the project
	 */
	private void readControlFiles() {
		IProject project= getProject();
		Set set= new HashSet(1);
		set.add(project);
		fControlledResources.put(project.getParent(), set);
		try {
			getProject().accept(new IResourceVisitor() {
				public boolean visit(IResource resource) {
					if (resource.getType() == IResource.FILE) {
						if (CONTROL_FILE_NAME.equals(resource.getName())) {
							Set controlledResources= readControlFile((IFile)resource);
							fControlledResources.put(resource.getParent(), controlledResources);
						}
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems traversing resource tree");
		}
	}

	/**
	 * Checks the resources in by marking them read-only.
	 */	
	public void checkin(final IResource[] resources, IProgressMonitor monitor) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Check in:");
			if (resources != null) {
				for (int i= 0; i < resources.length; i++) {
					System.out.println("\t" + resources[i]);
				}
			} else {
				System.out.println("null resources");
			}
		}
		if (resources == null || resources.length == 0) {
			return;
		}
		final Set modified= new HashSet(1);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask("Checking in resources", 1000);
				for(int i= 0; i < resources.length; i++) {
					IResource resource= resources[i];
					if (isControlled(resource)) { 
						if (resource.exists()) {
							resource.setReadOnly(true);
							modified.add(resource);
						}
					}
				}
				monitor.done();
			}
		};
		run(runnable, monitor);
		fireStateChanged(modified, false);
	}
	
	/**
	 * Unchecks the resources out.  In this provider this operation is 
	 * equivalent to checkin.
	 * 
	 * @see PessimisticFilesystemProvider#checkin(IResource[], IProgressMonitor)
	 */
	public void uncheckout(final IResource[] resources, IProgressMonitor monitor) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Uncheckout:");
			if (resources != null) {
				for (int i= 0; i < resources.length; i++) {
					System.out.println("\t" + resources[i]);
				}
			} else {
				System.out.println("null resources");
			}
		}
		if (resources == null || resources.length == 0) {
			return;
		}
		final Set modified= new HashSet(1);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask("Unchecking in resources", 1000);
				for(int i= 0; i < resources.length; i++) {
					IResource resource= resources[i];
					if (isControlled(resource)) {
						if (resource.exists()) {
							resource.setReadOnly(true);
							modified.add(resource);
						}
					}
				}
				monitor.done();
			}
		};
		run(runnable, monitor);
		fireStateChanged(modified, false);
	}

	/**
	 * Checks the resources out by marking the resources read-write.
	 */
	public void checkout(final IResource[] resources, IProgressMonitor monitor) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Check out:");
			if (resources != null) {
				for (int i= 0; i < resources.length; i++) {
					System.out.println("\t" + resources[i]);
				}
			} else {
				System.out.println("null resources");
			}
		}
		if (resources == null || resources.length == 0) {
			return;
		}
		final Set modified= new HashSet(1);
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask("Checking out resources", 1000);
				for(int i= 0; i < resources.length; i++) {
					IResource resource= resources[i];
					if (isControlled(resource)) {
						if(resource.exists()) {
							resource.setReadOnly(false);								
							modified.add(resource);
						}
					}
				}
				monitor.done();
			}
		};
		run(runnable, monitor);
		fireStateChanged(modified, false);
	}	

	/**
	 * Answers <code>true</code> if and only if the resource is 
	 * not <code>null</code>, controlled, not ignored, and checked out.
	 * Otherwise this method answers <code>false</code>.
	 */
	public boolean isCheckedout(IResource resource) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Is checked out: " + resource);
		}
		if (resource == null) {
			return false;
		}
		if (!isControlled(resource)) {
			return false;
		}
		if (isIgnored(resource)) {
			return false;
		}
		return !resource.isReadOnly();
	}	

	/**
	 * Answers <code>true</code> if the resource is not <code>null</code>,
	 * and is controlled, <code>false</code> otherwise.
	 */
	public boolean isControlled(IResource resource) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Is controlled: " + resource);
		}
		if (resource == null) {
			return false;
		}
		IProject project= getProject();
		if (!project.equals(resource.getProject())) {
			return false;
		}
		Set controlled= (Set)fControlledResources.get(resource.getParent());
		if (controlled == null) {
			return false;
		}
		return controlled.contains(resource);
	}
	
	/**
	 * Answers <code>true</code> if the resource is ignored.
	 * Resources are ignored if they are derived.
	 * Will return <code>false</code> when a resource is derived, but
	 * has explicitly been added to the control of this provider.
	 */
	public boolean isIgnored (IResource resource) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Is ignored: " + resource);
		}
		if (resource.isDerived()) {
			if (isControlled(resource)) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Answers <code>true</code> if the preference to change the content
	 * of the file has been set to <code>true</code>, <code>false</code>
	 * otherwise.
	 */
	public boolean hasContentChanged(IResource resource) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging()) {
			System.out.println("Has content change: " + resource);
		}

		IPreferenceStore preferences= PessimisticFilesystemProviderPlugin.getInstance().getPreferenceStore();
		boolean change= preferences.getBoolean(IPessimisticFilesystemConstants.PREF_TOUCH_DURING_VALIDATE_EDIT);
		if (change) {
			try {
				if(resource.getType() == IResource.FILE) {
					try {
						appendText((IFile)resource, getRandomSnippet(), false);
					} catch (IOException e1) {
						// ignore
					}
				} else {
					resource.touch(null);
				}
			} catch (CoreException e) {
				PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems touching resource: " + resource);
			}
		}
		return change;
	}

	public void appendText(IFile file, String text, boolean prepend) throws CoreException, IOException {
		String contents = getFileContents(file);
		StringBuffer buffer = new StringBuffer();
		if (prepend) {
			buffer.append(text);
		}
		buffer.append(contents);
		if (!prepend) {
			buffer.append(System.getProperty("line.separator") + text);
		}
		file.setContents(new ByteArrayInputStream(buffer.toString().getBytes()), false, false, null);
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

	/*
	 * Notifies listeners that the state of the resources has changed.
	 * 
	 * @param resources	A collection of resources whose state has changed.
	 * @param queueAfterWorkspaceOperation	If <code>true</code>, indicates that the 
	 * 						notification should occur after the current workspace runnable
	 * 						has completed.
	 */
	private void fireStateChanged(final Collection resources, boolean queueAfterWorkspaceOperation) {
		if (resources == null || resources.isEmpty()) {
			return;
		}

		if (queueAfterWorkspaceOperation) {
			Thread t= new Thread(new Runnable() {
				public void run() {
					try {
						ResourcesPlugin.getWorkspace().run(
							new IWorkspaceRunnable() {
								public void run(IProgressMonitor monitor) {
								}
							}, 
							null);
					} catch (CoreException e) {
						PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problem during empty runnable");
					}
					fireStateChanged(resources, false);
				}
			});
			t.start();
		} else {
			PessimisticFilesystemProviderPlugin.getInstance().fireResourcesChanged(
				(IResource[])resources.toArray(new IResource[resources.size()]));
		}			
	}
	
	/*
	 * Answers a collection of all of the resources contained below
	 * the given resource and the resource itself.
	 */
	private Collection getSubtreeMembers(IResource resource) {
		final Set resources= new HashSet(1);
		IResourceVisitor visitor= new IResourceVisitor() {
			public boolean visit(IResource resource) {
				switch (resource.getType()) {
					case IResource.PROJECT:
					case IResource.FOLDER:
					case IResource.FILE:
						resources.add(resource);
						return true;
				}
				return true;
			}
		};
		try {
			resource.accept(visitor);
		} catch (CoreException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problem during resource visiting");
		}
		return resources;
	}

	/*
	 * Runs a workspace operation reporting errors to the PessimisticFilesystemProviderPlugin.
	 */
	private void run(IWorkspaceRunnable runnable, IProgressMonitor monitor) {
		try {
			ResourcesPlugin.getWorkspace().run(runnable, monitor);
		} catch (CoreException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems during workspace operation.");
		}
	}
}

