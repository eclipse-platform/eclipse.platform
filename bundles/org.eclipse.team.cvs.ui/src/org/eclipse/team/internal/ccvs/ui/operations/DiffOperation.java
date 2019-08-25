/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.io.*;
import java.util.*;

import org.eclipse.compare.patch.WorkspacePatcherUI;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.listeners.DiffListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSCommunicationException;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.statushandlers.*;

public abstract class DiffOperation extends SingleCommandOperation {

	private static final int UNIFIED_FORMAT = 0;
	private static final int CONTEXT_FORMAT = 1;
	private static final int STANDARD_FORMAT = 2;
	
	protected boolean isMultiPatch;
	protected boolean includeFullPathInformation;
	protected PrintStream stream;
	protected IPath patchRoot;
	protected boolean patchHasContents;
	protected boolean patchHasNewFiles;
	
	/* see bug 116427 */
	private Object destination = null;
	
	/* see bug 159894 */
	private class CustomizableEOLPrintStream extends PrintStream{

		private boolean error = false;
		
		private String defaultLineEnding = "\n";  //$NON-NLS-1$
		
		public CustomizableEOLPrintStream(PrintStream openStream) {
			super(openStream);
			if(CVSProviderPlugin.getPlugin().isUsePlatformLineend()){
				defaultLineEnding = System.getProperty("line.separator"); //$NON-NLS-1$
			}
		}
		
		@Override
		public boolean checkError() {
			return error || super.checkError();
		}

		@Override
		public void println() {
			try{
				write(defaultLineEnding.getBytes());
			} catch (IOException e){
				error = true;
			}
		}
		
		@Override
		public void println(boolean x) {
			print(x);
			println();
		}
		
		@Override
		public void println(char x) {
			print(x);
			println();
		}

		@Override
		public void println(char[] x) {
			print(x);
			println();
		}

		@Override
		public void println(double x) {
			print(x);
			println();
		}

		@Override
		public void println(float x) {
			print(x);
			println();
		}

		@Override
		public void println(int x) {
			print(x);
			println();
		}

		@Override
		public void println(long x) {
			print(x);
			println();
		}

		@Override
		public void println(Object x) {
			print(x);
			println();
		}
		
		@Override
		public void println(String x) {
			print(x);
			println();
		}
	}
	
	public DiffOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options, boolean isMultiPatch, boolean includeFullPathInformation, IPath patchRoot, Object destination) {
		super(part, mappings, options);
		this.isMultiPatch = isMultiPatch;
		this.includeFullPathInformation=includeFullPathInformation;
		this.patchRoot=patchRoot;
		this.patchHasContents=false;
		this.patchHasNewFiles=false;
		this.destination = destination;
	}
	
	@Override
	protected boolean shouldRun(){
		if (super.shouldRun() == false){
			return false;
		}
		Job[] jobs = Job.getJobManager().find(destination);
		if(jobs.length != 0){
			MessageDialog question = new MessageDialog(getShell(), 
					CVSUIMessages.DiffOperation_CreatePatchConflictTitle, null, 
					NLS.bind(CVSUIMessages.DiffOperation_CreatePatchConflictMessage, destination.toString()), 
					MessageDialog.QUESTION, 
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 
					1);
			if(question.open() == 0){
				Job.getJobManager().cancel(destination);
			} else {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
			stream = new CustomizableEOLPrintStream(openStream());
			if (isMultiPatch){
				stream.println(WorkspacePatcherUI.getWorkspacePatchHeader());
			}
			super.execute(monitor);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}
 
	/**
	 * Open and return a stream for the diff output.
	 * @return a stream for the diff output
	 */
	protected abstract PrintStream openStream() throws CVSException;
	
	private static Comparator COMPARATOR = new Comparator() {
		private int compare(IResource r1, IResource r2) {
			return r1.getFullPath().toString().compareTo(r2.getFullPath().toString());
		}
		@Override
		public int compare(Object o1, Object o2) {
			IResource r1 = null;
			IResource r2 = null;
			if (o1 instanceof ICVSResource) {
				r1 = ((ICVSResource)o1).getIResource();
			} else {
				r1 = (IResource)o1;
			}
			if (o2 instanceof ICVSResource) {
				r2 = ((ICVSResource)o2).getIResource();
			} else {
				r2 = (IResource)o2;
			}
			return compare(r1, r2);
		}
	};
	@Override
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		
		//add this project to the total projects encountered
		final HashSet<ICVSFile> newFiles = new HashSet<>(); // need HashSet to guard for duplicate entries
		final HashSet<IResource> existingFiles = new HashSet<>(); // need HashSet to guard for duplicate entries
		
		monitor.beginTask(null,100);
		final IProgressMonitor subMonitor = Policy.subMonitorFor(monitor,10);
		for (IResource resource : resources) {
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
			cvsResource.accept(new ICVSResourceVisitor() {
				@Override
				public void visitFile(ICVSFile file) throws CVSException {
					if (!(file.isIgnored()))  {
						if (!file.isManaged() || file.getSyncInfo().isAdded() ){
							//this is a new file
							if (file.exists())
								newFiles.add(file);
						}else if (file.isModified(subMonitor)){
							existingFiles.add(file.getIResource());
						}
					}
				}
				
				@Override
				public void visitFolder(ICVSFolder folder) throws CVSException {
					// Even if we are not supposed to recurse we still need to go into
					// the root directory.
					if (!folder.exists() || folder.isIgnored() )  {
						return;
					} 
					
					folder.acceptChildren(this);
					
				}
			}, recurse);
		}

		final SortedSet<Object> allFiles = new TreeSet<>(COMPARATOR);
		allFiles.addAll(existingFiles);
		allFiles.addAll(newFiles);

		subMonitor.done();
		
		//Check options 
		//Append our diff output to the server diff output.
		// Our diff output includes new files and new files in new directories.
		int format = STANDARD_FORMAT;
	
		LocalOption[] localoptions = getLocalOptions(recurse);
		for (LocalOption option : localoptions) {
			if (option.equals(Diff.UNIFIED_FORMAT) ||
					isMultiPatch)  {
				format = UNIFIED_FORMAT;
			} else if (option.equals(Diff.CONTEXT_FORMAT))  {
				format = CONTEXT_FORMAT;
			} 
		}
		
		boolean haveAddedProjectHeader=false;
		
		if (!existingFiles.isEmpty()){
			if (isMultiPatch && !haveAddedProjectHeader){
				haveAddedProjectHeader=true;
				IProject project=resources[0].getProject();
				stream.println(WorkspacePatcherUI.getWorkspacePatchProjectHeader(project));
			}
		}

		if (!newFiles.isEmpty() && Diff.INCLUDE_NEWFILES.isElementOf(localoptions)){
			//Set new file to flag to let us know that we have added something to the current patch
			patchHasNewFiles=true;
			
			if (isMultiPatch &&!haveAddedProjectHeader){
				haveAddedProjectHeader=true;
				IProject project=resources[0].getProject();
				stream.println(WorkspacePatcherUI.getWorkspacePatchProjectHeader(project));
			}
		}
		
		List<Object> existingFilesSubList = new ArrayList<>();
		for (Iterator iter = allFiles.iterator(); iter.hasNext();) {
			Object file = iter.next();
			if (existingFiles.contains(file)) {
				existingFilesSubList.add(file);
			} else if (newFiles.contains(file)){
				addExistingFilesSubListToDiff(provider, existingFilesSubList, recurse, monitor, existingFiles.size());
				ICVSFile cvsFile = (ICVSFile) file;
				addFileToDiff(getNewFileRoot(cvsFile), cvsFile,stream,format);
			}
		}
		addExistingFilesSubListToDiff(provider, existingFilesSubList, recurse, monitor, existingFiles.size());

		monitor.done();
	}

	private void addExistingFilesSubListToDiff(CVSTeamProvider provider, Collection subList, boolean recurse, IProgressMonitor monitor, int existingFilesTotal) throws InterruptedException {
		if (!subList.isEmpty()) {
			int ticks = 90 * subList.size() / existingFilesTotal;
			try{
				super.execute(provider, (IResource[]) subList.toArray(new IResource[subList.size()]), recurse, Policy.subMonitorFor(monitor, ticks));
			} catch(CVSCommunicationException ex){ // see bug 123430
				StatusAdapter adapter = new StatusAdapter(ex.getStatus());
				adapter.setProperty(
						IStatusAdapterConstants.TITLE_PROPERTY,
						CVSUIMessages.DiffOperation_ErrorsOccurredWhileCreatingThePatch);
				StatusManager.getManager().handle(adapter,
						StatusManager.SHOW | StatusManager.NONE);
			} catch (CVSException ex) {
				handleCVSException(ex);
			}
			subList.clear();
		}
	}

	/**
	 * Checks if the exception contain a status that has to be shown to the
	 * user. If yes, the method shows the dialog.
	 * 
	 * @param ex exception to handle
	 */
	private void handleCVSException(CVSException ex) {
		IStatus status = ex.getStatus();
		List<IStatus> toShow = new ArrayList<>();
		IStatus children[] = status.getChildren();
		boolean may = true;
		for (IStatus child : children) {
			// ignore all errors except those found by DiffListener
			if (child.getCode() == CVSStatus.BINARY_FILES_DIFFER || child.getCode() == CVSStatus.PROTOCOL_ERROR || child.getCode() == CVSStatus.ERROR_LINE) {
				toShow.add(child);
				if (child.getCode() == CVSStatus.BINARY_FILES_DIFFER) {
					// the patch does not contain some changes for sure
					may = false;
				}
			}
		}
		if (toShow.size() > 0) {
			String msg = may ? CVSUIMessages.DiffOperation_ThePatchMayNotContainAllTheChanges
					: CVSUIMessages.DiffOperation_ThePatchDoesNotContainAllTheChanges;
			StatusAdapter adapter = new StatusAdapter(
					new MultiStatus(
							CVSProviderPlugin.ID,
							CVSStatus.SERVER_ERROR,
							toShow.toArray(new IStatus[toShow.size()]),
							CVSUIMessages.DiffOperation_ErrorsOccurredWhileCreatingThePatch,
							null));
			adapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, msg);
			StatusManager.getManager().handle(adapter,
					StatusManager.SHOW | StatusManager.LOG);
		}
	}

	private ICVSFolder getNewFileRoot(ICVSFile cvsFile) {
		ICVSFolder patchRootFolder = getPatchRootFolder();
		if (patchRootFolder != null)
			return patchRootFolder;
		return CVSWorkspaceRoot.getCVSFolderFor(cvsFile.getIResource().getProject());
	}
	
	@Override
	protected IStatus executeCommand(Session session, CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		
		DiffListener diffListener = new DiffListener(stream);
		
		IStatus status = Command.DIFF.execute(session,
							Command.NO_GLOBAL_OPTIONS,
							getLocalOptions(recurse),
							resources,
							diffListener,
							monitor);
		
		//Once any run of the Diff commands reports that it has written something to the stream, the patch 
		//in its entirety is considered non-empty - until then keep trying to set the flag.
		if (!patchHasContents)
			patchHasContents = diffListener.wroteToStream();

		return status;
	}

	@Override
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.DiffOperation_0, new String[]{provider.getProject().getName()});
	}

	@Override
	protected String getTaskName() {
		return CVSUIMessages.DiffOperation_1;
	}
	
	@Override
	Map getProviderTraversalMapping(IProgressMonitor monitor) throws CoreException {
		Map providerTraversal = super.getProviderTraversalMapping(monitor);
		SortedMap result = new TreeMap((o1, o2) -> {
			CVSTeamProvider p1 = (CVSTeamProvider) o1;
			CVSTeamProvider p2 = (CVSTeamProvider) o2;
			return COMPARATOR.compare(p1.getProject(), p2.getProject());
		});
		result.putAll(providerTraversal);
		return result;
	}

	private void addFileToDiff(ICVSFolder patchRoot, ICVSFile file, PrintStream printStream, int format) throws CVSException {
		
		String nullFilePrefix = ""; //$NON-NLS-1$
		String newFilePrefix = ""; //$NON-NLS-1$
		String positionInfo = ""; //$NON-NLS-1$
		String linePrefix = ""; //$NON-NLS-1$
		
		String pathString=""; //$NON-NLS-1$

		
		//get the path string for this file
		pathString= file.getRelativePath(patchRoot);
	
		int lines = 0;
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getContents()));
		try {
			while (fileReader.readLine() != null)  {
				lines++;
			}
		} catch (IOException e) {
			throw CVSException.wrapException(file.getIResource(), NLS.bind(CVSUIMessages.DiffOperation_ErrorAddingFileToDiff, new String[] { pathString }), e);
		} finally {
			try {
				fileReader.close();
			} catch (IOException e1) {
				//ignore
			}
		}

		// Ignore empty files
		if (lines == 0)
			return;
		
		switch (format) {
		case UNIFIED_FORMAT:
			nullFilePrefix = "--- ";	//$NON-NLS-1$
			newFilePrefix = "+++ "; 	//$NON-NLS-1$
			positionInfo = "@@ -0,0 +1," + lines + " @@" ;	//$NON-NLS-1$ //$NON-NLS-2$
			linePrefix = "+"; //$NON-NLS-1$
			break;
			
		case CONTEXT_FORMAT :
			nullFilePrefix = "*** ";	//$NON-NLS-1$
			newFilePrefix = "--- ";		//$NON-NLS-1$
			positionInfo = "--- 1," + lines + " ----";	//$NON-NLS-1$ //$NON-NLS-2$
			linePrefix = "+ ";	//$NON-NLS-1$
			break;
			
		default :
			positionInfo = "0a1," + lines;	//$NON-NLS-1$
		linePrefix = "> ";	//$NON-NLS-1$
					break;
		}
		
		fileReader = new BufferedReader(new InputStreamReader(file.getContents()));
		try {
				
			printStream.println("Index: " + pathString);		//$NON-NLS-1$
			printStream.println("===================================================================");	//$NON-NLS-1$
			printStream.println("RCS file: " + pathString);	//$NON-NLS-1$
			printStream.println("diff -N " + pathString);	//$NON-NLS-1$
			
			
			if (format != STANDARD_FORMAT)  {
				printStream.println(nullFilePrefix + "/dev/null	1 Jan 1970 00:00:00 -0000");	//$NON-NLS-1$
				// Technically this date should be the local file date but nobody really cares.
				printStream.println(newFilePrefix + pathString + "	1 Jan 1970 00:00:00 -0000");	//$NON-NLS-1$
			}
			
			if (format == CONTEXT_FORMAT)  {
				printStream.println("***************");	//$NON-NLS-1$
				printStream.println("*** 0 ****");		//$NON-NLS-1$
			}

			printStream.println(positionInfo);

			for (int i = 0; i < lines - 1; i++) {
				printStream.print(linePrefix);
				printStream.println(fileReader.readLine());
			}

			printStream.print(linePrefix);
			readLastLine(fileReader, printStream);
		} catch (IOException e) {
			throw CVSException.wrapException(file.getIResource(), NLS.bind(CVSUIMessages.DiffOperation_ErrorAddingFileToDiff, new String[] { pathString }), e);
		} finally  {
			try {
				fileReader.close();
			} catch (IOException e1) {
			}
		}
	}

	// based on org.eclipse.compare.internal.core.patch.LineReader.readLine()
	private void readLastLine(BufferedReader reader, PrintStream printStream)
			throws IOException {
		boolean sawCRorLF = false;
		boolean sawEOF = false;
		// TODO: hardcoded, set to the same value as initially in LineReader
		boolean ignoreSingleCR = false;
		while (!sawEOF) {
			int c = reader.read();
			if (c == -1) {
				sawEOF = true;
				break;
			}
			printStream.print((char) c);
			if (c == '\n') {
				sawCRorLF = true;
				break;
			}
			if (c == '\r') {
				sawCRorLF = true;
				c = reader.read();
				if (c == -1) {
					sawEOF = true;
					break; // EOF
				}
				if (c != '\n') {
					if (ignoreSingleCR) {
						sawCRorLF = false;
						printStream.print((char) c);
						continue;
					}
				} else { // '\n'
					printStream.print((char) c);
				}
				break;
			}
		}
		if (!sawCRorLF) {
			printStream.println();
			printStream.println("\\ No newline at end of file"); //$NON-NLS-1$
		}
	}

	public void setStream(PrintStream stream) {
		this.stream = new CustomizableEOLPrintStream(stream);
	}

	protected void reportEmptyDiff() {
		StatusAdapter adapter = new StatusAdapter(new Status(IStatus.INFO,
				CVSProviderPlugin.ID,
				CVSUIMessages.GenerateCVSDiff_noDiffsFoundMsg));
		adapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
				CVSUIMessages.GenerateCVSDiff_noDiffsFoundTitle);
		StatusManager.getManager().handle(adapter,
				StatusManager.SHOW);
	}

	@Override
	protected ICVSFolder getLocalRoot(CVSTeamProvider provider) throws CVSException {
		ICVSFolder root = getPatchRootFolder();
		if (root != null)
			return root;
		return super.getLocalRoot(provider);
	}

	private ICVSFolder getPatchRootFolder() {
		if (!isMultiPatch &&
			!includeFullPathInformation){
			//Check to see if the selected patchRoot has enough segments to consider it a folder/resource
			//if not just get the project

			IResource patchFolder = null;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (patchRoot.segmentCount() > 1){
				patchFolder = root.getFolder(patchRoot);
			} else {
				patchFolder = root.getProject(patchRoot.toString());
			}
		
			ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(patchFolder);
			if (!cvsResource.isFolder()) {
				cvsResource = cvsResource.getParent();
			}
			return (ICVSFolder) cvsResource;
		}
		return null;
	}
	
	@Override
	public boolean consultModelsForMappings() {
		return false;
	}
	
	@Override
	public boolean belongsTo(Object family){
		if(family != null && family.equals(destination))
			return true;
		return super.belongsTo(family);
	}

}
