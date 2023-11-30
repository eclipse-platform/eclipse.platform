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
 *     Simon Scholz <simon.scholz@vogella.com> - Ongoing maintenance
 *******************************************************************************/
package org.eclipse.core.tools.search;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;

/*
 * Class that removes field declarations which aren't referenced.
 */
@SuppressWarnings("restriction")
public class FindUnusedMembers implements IRunnableWithProgress {

	public static interface IResultReporter {
		void unusedElementFound(IMember member) throws CoreException;
	}

	public static class OutputWriter implements IResultReporter {

		private final Writer output;
		private IType lastType;

		public OutputWriter(Writer writer) {
			output = writer;
		}

		@Override
		public void unusedElementFound(IMember member) throws CoreException {
			try {
				if (!member.getDeclaringType().equals(lastType)) {
					lastType = member.getDeclaringType();
					writeHeader(lastType);
				}
				if (member instanceof IMethod) {
					writeResult((IMethod) member);
				} else if (member instanceof IField) {
					writeResult((IField) member);
				}
			} catch (IOException e) {
				// to do
			}
		}

		private void writeHeader(IType type) throws IOException {
			output.write("\n\n" + type.getFullyQualifiedName()); //$NON-NLS-1$
		}

		private void writeResult(IField field) throws IOException, JavaModelException {
			output.write("\n\t"); //$NON-NLS-1$
			output.write(Signature.toString(field.getTypeSignature()));
			output.write(" "); //$NON-NLS-1$
			output.write(field.getElementName());
		}

		private void writeResult(IMethod method) throws IOException, JavaModelException {
			output.write("\n\t");//$NON-NLS-1$
			output.write(Signature.toString(method.getReturnType()));
			output.write(" "); //$NON-NLS-1$
			output.write(method.getElementName());
			output.write("("); //$NON-NLS-1$
			String[] types = method.getParameterTypes();
			for (int i = 0; i < types.length; i++) {
				output.write(Signature.toString(types[i]));
				if (i < types.length - 1)
					output.write(","); //$NON-NLS-1$
			}
			output.write(")"); //$NON-NLS-1$
		}
	}

	private final IResultReporter result;
	private final ICompilationUnit[] units;
	protected int unusedMemberCount = 0;

	public FindUnusedMembers(ICompilationUnit[] units, Writer writer) {
		this(units, new OutputWriter(writer));
	}

	public FindUnusedMembers(ICompilationUnit[] units, IResultReporter resultReporter) {
		this.units = units;
		this.result = resultReporter;
	}

	private void doSearchCU(ICompilationUnit cu, IProgressMonitor monitor) throws CoreException {
		IType[] allTypes = cu.getAllTypes();
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Processing " + cu.getElementName(), allTypes.length + 1); //$NON-NLS-1$

		ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
		astParser.setResolveBindings(true);
		astParser.setProject(cu.getJavaProject());

		IBinding[] bindings = astParser.createBindings(allTypes, subMonitor.newChild(1));
		for (IBinding binding : bindings) {
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			ITypeBinding typeBinding = (ITypeBinding) binding;
			subMonitor.setTaskName("Processing '" + typeBinding.getQualifiedName() + "'"); //$NON-NLS-1$//$NON-NLS-2$
			doSearchType(typeBinding, subMonitor.newChild(1));
		}
	}

	private boolean methodOverrides(IMethodBinding binding) {
		return Bindings.findOverriddenMethod(binding, true) != null;
	}

	public void doSearchType(ITypeBinding typeBinding, IProgressMonitor monitor) throws CoreException {
		IMethodBinding[] methods = typeBinding.getDeclaredMethods();
		IVariableBinding[] fields = typeBinding.getDeclaredFields();
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Searching for references.", //$NON-NLS-1$
				methods.length + fields.length);

		for (IMethodBinding methodBinding : methods) {
			if (monitor.isCanceled())
				throw new OperationCanceledException();

			if (methodOverrides(methodBinding))
				continue;

			IMethod method = (IMethod) methodBinding.getJavaElement();
			if (method == null)
				continue;

			if (hasReferences(method, subMonitor.newChild(1)))
				continue;
			result.unusedElementFound(method);
			unusedMemberCount++;
		}
		for (IVariableBinding fieldBinding : fields) {
			IField field = (IField) fieldBinding.getJavaElement();
			if (field == null)
				continue;
			if (hasReferences(field, subMonitor.split(1)))
				continue;
			result.unusedElementFound(field);
			unusedMemberCount++;
		}

		if (monitor.isCanceled())
			throw new OperationCanceledException();
	}

	public int getUnusedMethodCount() {
		return unusedMemberCount;
	}

	private boolean hasReferences(IMember member, IProgressMonitor monitor) throws JavaModelException {

		final class ReferenceFound extends Error {
			private static final long serialVersionUID = 1L;
		}

		try {
			IJavaSearchScope searchScope = RefactoringScopeFactory.create(member.getDeclaringType());
			SearchPattern pattern = SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES);
			SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
						throw new ReferenceFound();
					}
				}
			};
			new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
					searchScope, requestor, monitor);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} catch (ReferenceFound e) {
			return true;
		}
		return false;
	}

	public void process(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Searching unused members", this.units.length); //$NON-NLS-1$
		for (ICompilationUnit unit : this.units) {
			doSearchCU(unit, subMonitor.split(1));
		}
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			process(monitor);
		} catch (OperationCanceledException e) {
			InterruptedException interruptedException = new InterruptedException(e.getMessage());
			interruptedException.initCause(e);
			throw interruptedException;
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

}
