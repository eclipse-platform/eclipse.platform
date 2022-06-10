/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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
package org.eclipse.core.tools.nls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.*;

/*
 * Class that removes field declarations which aren't referenced.
 */
public class RemoveUnusedMessages extends Refactoring {

	IType accessorClass;
	IFile propertiesFile;
	CompositeChange change;

	public RemoveUnusedMessages(IType accessorClass, IFile propertiesFile) {
		super();
		this.accessorClass = accessorClass;
		this.propertiesFile = propertiesFile;
	}

	@Override
	public String getName() {
		return "Fix NLS References";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		change = new CompositeChange("Accessor Class Changes");
		RefactoringStatus result = new RefactoringStatus();
		ICompilationUnit unit = JavaCore.createCompilationUnitFrom((IFile) accessorClass.getResource());
		CompilationUnit root = new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true, null);
		ASTRewrite rewriter = ASTRewrite.create(root.getAST());

		// Search for references
		IField[] fields = accessorClass.getFields();
		ArrayList<String> toDelete = new ArrayList<>();
		// 10 units of work for modifying the properties file and AST
		monitor.beginTask("Searching for references.", fields.length + 10);
		try {
			for (IField field : fields) {
				if (monitor.isCanceled())
					throw new OperationCanceledException();
				String fieldName = field.getElementName();
				monitor.subTask("Searching for references to: " + fieldName);
				int flags = field.getFlags();
				// only want to look at the public static String fields
				if (!(Flags.isPublic(flags) && Flags.isStatic(flags)))
					continue;
				// search for references
				ICompilationUnit[] affectedUnits = RefactoringSearchEngine.findAffectedCompilationUnits(SearchPattern.createPattern(field, IJavaSearchConstants.REFERENCES), RefactoringScopeFactory.create(accessorClass), new SubProgressMonitor(monitor, 1), result);
				// there are references so go to the next field
				if (affectedUnits.length > 0)
					continue;
				System.out.println("Found unused field: " + fieldName);
				toDelete.add(fieldName);
			}
			System.out.println("Analysis of " + fields.length + " messages found " + toDelete.size() + " unused messages");

			// any work to do?
			if (toDelete.isEmpty())
				return result;

			if (monitor.isCanceled())
				throw new OperationCanceledException();

			// remove the field and its corresponding entry in the messages.properties file
			processAST(root, rewriter, toDelete);
			monitor.worked(5);
			processPropertiesFile(toDelete);
			monitor.worked(5);

			TextFileChange cuChange = new TextFileChange(unit.getElementName(), (IFile)unit.getResource());
			try {
				ITextFileBuffer buffer = RefactoringFileBuffers.acquire(unit);
				IDocument document = buffer.getDocument();
				cuChange.setEdit(rewriter.rewriteAST(document, null));
			} finally {
				RefactoringFileBuffers.release(unit);
			}
			change.add(cuChange);

		} finally {
			monitor.done();
		}

		// return the result
		return result;
	}

	private void processAST(final CompilationUnit root, final ASTRewrite rewriter, final List<String> toDelete) {
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationFragment node) {
				// check to see if its in our list of fields to delete
				if (!toDelete.contains(node.getName().toString()))
					return true;
				rewriter.remove(node.getParent(), null);
				return true;
			}
		};
		root.accept(visitor);
	}

	private void processPropertiesFile(List<String> list) throws CoreException {
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(propertiesFile.getFullPath(), LocationKind.NORMALIZE, null);
				manager.connect(accessorClass.getCompilationUnit().getCorrespondingResource().getFullPath(),
						LocationKind.NORMALIZE, null);
				change.add(new PropertyFileConverter().trim(propertiesFile, list));
			} finally {
				manager.disconnect(propertiesFile.getFullPath(), LocationKind.NORMALIZE, null);
				manager.disconnect(accessorClass.getCompilationUnit().getCorrespondingResource().getFullPath(),
						LocationKind.NORMALIZE, null);
			}
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, RemoveUnusedMessages.class, IStatus.ERROR, e.getMessage(), e));
		}
	}

	@Override
	public Change createChange(IProgressMonitor monitor) {
		return change;
	}

}
