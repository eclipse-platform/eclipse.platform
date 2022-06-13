/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
import java.util.List;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.*;

public class MessageBundleRefactoring extends Refactoring {

	IType fAccessorClass;
	IFile fPropertiesFile;
	CompositeChange fChange;
	ITypeBinding fAccessorTypeBinding;

	public MessageBundleRefactoring(IType accessorClass, IFile propertiesFile) {
		super();
		fAccessorClass = accessorClass;
		fPropertiesFile = propertiesFile;
	}

	@Override
	public String getName() {
		return "Message Bundle Refactoring";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		RefactoringStatus result = new RefactoringStatus();
		fAccessorTypeBinding = computeAccessorClassBinding(new SubProgressMonitor(monitor, 1));
		if (fAccessorTypeBinding == null) {
			result.addFatalError("Couldn't resolve accessor class");
			return result;
		}
		fChange = new CompositeChange("Accessor Class Changes");
		ICompilationUnit[] affectedUnits = RefactoringSearchEngine.findAffectedCompilationUnits(SearchPattern.createPattern(fAccessorClass, IJavaSearchConstants.REFERENCES), RefactoringScopeFactory.create(fAccessorClass), new SubProgressMonitor(monitor, 5), result);
		monitor.beginTask("", affectedUnits.length + 1);
		for (ICompilationUnit unit : affectedUnits) {
			if (unit.equals(fAccessorClass.getCompilationUnit()))
				continue;
			processCompilationUnit(result, unit, new SubProgressMonitor(monitor, 1));
		}
		processPropertiesFile(result, new SubProgressMonitor(monitor, 1));
		return result;
	}

	private void processPropertiesFile(RefactoringStatus result, IProgressMonitor monitor) throws CoreException {
		// TODO need to roll the changes to the properties file into a text edit that we can hook into
		// the "Undo -> Refactoring" framework
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			try {
				manager.connect(fPropertiesFile.getFullPath(), LocationKind.NORMALIZE, null);
				manager.connect(fAccessorClass.getCompilationUnit().getCorrespondingResource().getFullPath(),
						LocationKind.NORMALIZE, null);
				fChange.addAll(new PropertyFileConverter().convertFile(fAccessorClass, fPropertiesFile));
			} finally {
				manager.disconnect(fPropertiesFile.getFullPath(), LocationKind.NORMALIZE, null);
				manager.disconnect(fAccessorClass.getCompilationUnit().getCorrespondingResource().getFullPath(),
						LocationKind.NORMALIZE, null);
			}
		} catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, MessageBundleRefactoring.class, IStatus.ERROR, e.getMessage(), e));
		}
	}

	private void processCompilationUnit(RefactoringStatus result, ICompilationUnit unit, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 2);
		CompilationUnit root = new RefactoringASTParser(AST.getJLSLatest()).parse(unit, true,
				new SubProgressMonitor(monitor, 1));
		ASTRewrite rewriter = ASTRewrite.create(root.getAST());

		processAST(result, root, rewriter, new SubProgressMonitor(monitor, 1));

		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile)unit.getResource());
		try {
			ITextFileBuffer buffer = RefactoringFileBuffers.acquire(unit);
			IDocument document = buffer.getDocument();
			change.setEdit(rewriter.rewriteAST(document, null));
		} finally {
			RefactoringFileBuffers.release(unit);
		}
		fChange.add(change);
		monitor.done();
	}

	private void processAST(RefactoringStatus result, final CompilationUnit root, final ASTRewrite rewriter, SubProgressMonitor monitor) {
		// keep track of the number of changes we make per line so we can get rid of the NLS comments.
		final IntegerMap map = new IntegerMap(10);
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				Name messageBundleName = getMessageBundleReceiver(node);
				if (messageBundleName == null)
					return true;
				IMethodBinding method = node.resolveMethodBinding();
				// TODO here we have to do some checks whether the called method on the
				// resource bundle is something we have to rewrite. This depends on
				// the kind of the bundle and needs some AI.
				ITypeBinding[] params = method.getParameterTypes();
				if (params.length == 0)
					return true;
				if (!"java.lang.String".equals(params[0].getQualifiedName()))
					return true;
				List args = node.arguments();
				if (args.size() != 1)
					return true;
				Object obj = args.get(0);
				if (!(obj instanceof StringLiteral))
					return true;
				// compute the key of the message property
				StringLiteral string = (StringLiteral) obj;
				String key = PropertyFileConverter.convertToJavaIdentifier(string.getLiteralValue());

				// create the field access object
				FieldAccess fieldAccess = root.getAST().newFieldAccess();
				fieldAccess.setExpression((Expression) rewriter.createCopyTarget(messageBundleName));
				fieldAccess.setName(root.getAST().newSimpleName(key));

				// replace the method invocation with the field access
				rewriter.replace(node, fieldAccess, null);
				int line = 11;
				int value = map.get(line);
				value++;
				map.put(line, value);
				return true;
			}

			private Name getMessageBundleReceiver(MethodInvocation node) {
				Expression expression = node.getExpression();
				if (expression == null)
					return null;
				if (expression instanceof Name && Bindings.equals(fAccessorTypeBinding, ((Name) expression).resolveBinding())) {
					return (Name) expression;
				}
				return null;
			}
		};
		root.accept(visitor);

		// create another visitor to trim the //$NON-NLS-N$ comments
		//		visitor = new ASTVisitor() {
		//			public boolean visit(LineComment node) {
		//				return true;
		//			}
		//		};
		//		root.accept(visitor);
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		return fChange;
	}

	private ITypeBinding computeAccessorClassBinding(IProgressMonitor monitor) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setProject(fAccessorClass.getJavaProject());
		return (ITypeBinding) parser.createBindings(new IJavaElement[] {fAccessorClass}, monitor)[0];
	}
}
