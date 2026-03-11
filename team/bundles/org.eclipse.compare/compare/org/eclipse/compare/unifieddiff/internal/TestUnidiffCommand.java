package org.eclipse.compare.unifieddiff.internal;

import org.eclipse.compare.unifieddiff.UnifiedDiff;
import org.eclipse.compare.unifieddiff.UnifiedDiff.UnifiedDiffMode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

//TODO (tm) used for manual testing which will later be removed
public abstract class TestUnidiffCommand extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (ed instanceof MultiPageEditorPart mpe) {
			Object p = mpe.getSelectedPage();
			if (p instanceof IEditorPart) {
				ed = (IEditorPart) p;
			}
		}
		if (ed instanceof ITextEditor ted) {
			String r = """
					package jp1;
					// TO BE ADDED BEGINNING
					public class Test1 {
						public void m1() {
							System.err.bla("n");
							int i=0;
						}
						public static void main(String[] args) {
							args[0]="a";
						}
					}
					// TO BE ADDED END
					""";
//			r = """
//					package jp1;
//
//					public class Test1 {
//						public void m1() {
//							System.out.println("c");
//							System.out.println("d");
//						}
//						public static void main(String[] args) {
//							System.out.println("abb");
//							System.out.println("b");
//						}
//					}
//					""";
			r = """
					package jp1;

					public class Test {

						public static void main(String[] args) {
							// comment
							m1();
							m1();
							m2();
							m3();
							m4();
							m5();
							m6();
							m7();
							m8();
							m9();
							m10();
						}

						private static void m5() {
							// TODO Auto-generated method stub

						}

						private static void m6() {
							// TODO Auto-generated method stub

						}

						private static void m7() {
							// TODO Auto-generated method stub

						}

						private static void m8() {
							println("a");
							println("b");

						}

						private static void m9() {
							// TODO Auto-generated method stub

						}

						private static void m10() {
							// TODO Auto-generated method stub

						}

						static void m1() {
							// com
						}


						static void m2() {

						}

						static void m3() {

						}
					}
										""";
			r = """
				package jp1;

				public class Test1 {
					public static void main(String[] args) {
						System.out.println(sum(1,2) + max(2,1));
					}

					private static int sum(int first, int second) {
						return first+second - third + "str";
					}
					System.out.println("a");}
				""";

			UnifiedDiff.create(ted, r, getMode()).open();
		}
		return null;
	}

	protected abstract UnifiedDiffMode getMode();

}
