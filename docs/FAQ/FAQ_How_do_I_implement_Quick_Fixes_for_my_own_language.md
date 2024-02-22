

FAQ How do I implement Quick Fixes for my own language?
=======================================================

The JDT has support for so-called Quick Fixes. Whenever a marker is generated, a set of resolutions is associated with it for users to click on and choose an automatic fix of the problem as shown in Figure 19.5. Quick Fixes are implemented through the org.eclipse.ui.ide.markerResolution extension point:

      <extension point="org.eclipse.ui.ide.markerResolution">
         <markerResolutionGenerator
            markerType="org.eclipse.core.resources.problemmarker"
            class="org.eclipse.escript.quickfix.QuickFixer"/>
      </extension>

  

The implementation class implements the IMarkerResolutionGenerator interface. Use the IMarkerResolutionGenerator2 when resolutions are expensive to implement. See the javadoc for the interface for an explanation. Here is what the implementation class may look like:

      public class QuickFixer implements IMarkerResolutionGenerator {
         public IMarkerResolution[] getResolutions(IMarker mk) {
            try {
               Object problem = mk.getAttribute("WhatsUp");
               return new IMarkerResolution[] {
                  new QuickFix("Fix #1 for "+problem),
                  new QuickFix("Fix #2 for "+problem),
               };
            }
            catch (CoreException e) {
               return new IMarkerResolution\[0\];
            }
         }
      }

An array of Quick Fixes has to be returned for the problem associated with the current marker.

Each marker resolution, or Quick Fix, implements IMarkerResolution or, when a description and an image are available, IMarkerResolution2. Here is what the implementation may look like:

      public class QuickFix implements IMarkerResolution {
         String label;
         QuickFix(String label) {
            this.label = label;
         }
         public String getLabel() {
            return label;
         }
         public void run(IMarker marker) {
            MessageDialog.openInformation(null, "QuickFix Demo",
               "This quick-fix is not yet implemented");
         }
      }

The problem indicator-in our sample, the WhatsUp attribute- is associated with the marker by the parser. Typically, the Quick Fix handler that resolves the problem, as shown in this example, lives somewhere in the UI. Following this paradigm is advisable as it separates the problem detection in the compiler/parser from how it is presented to the user.

Quick Fixes can be inspected and executed by using the context menu on a given problem in the Problems view. Note how the JDT uses a context menu and the double-click action on a marker to active Quick Fix.

See Also:
---------

[FAQ How do I support refactoring for my own language?](./FAQ_How_do_I_support_refactoring_for_my_own_language.md "FAQ How do I support refactoring for my own language?")

