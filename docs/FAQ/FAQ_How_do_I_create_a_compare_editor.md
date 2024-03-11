

FAQ How do I create a compare editor?
=====================================

  

  

Compare dialogs are typically used in simple contexts that ask the user to select from a list of available editions. For richer comparisons, a compare editor is typically used. The advantage of using an editor is that the user can take as long as needed to browse, modify, and merge the contents.

  
Compare editors display a tree of DiffNode elements, where each node represents a logical entity, such as a file or programming-language element. These nodes represent either a two-way or a three-way comparison, where the optional third element is the common ancestor of the two elements being compared. Each DiffNode references a left- and right-side element and, possibly, a third element representing the common ancestor. As with compare dialogs, these compare elements should implement ITypedElement and IStreamContentAccessor. You can construct these node trees manually or use the supplied Differencer class to help you construct it.

  
The DiffNode tree is computed by a CompareEditorInput subclass that is passed as an input to the editor. The subclass must implement the prepareInput method to return the tree represented by the DiffNode. The following example illustrates a compare editor input that uses the CompareItem class described in [FAQ\_How\_do\_I\_create\_a\_Compare_dialog?](./FAQ_How_do_I_create_a_Compare_dialog.md "FAQ How do I create a Compare dialog?")

      class CompareInput extends CompareEditorInput {
         public CompareInput() {
            super(new CompareConfiguration());
         }
         protected Object prepareInput(IProgressMonitor pm) {
            CompareItem ancestor = 
               new CompareItem("Common", "contents");
            CompareItem left = 
               new CompareItem("Left", "new contents");
            CompareItem right = 
               new CompareItem("Right", "old contents");
            return new DiffNode(null, Differencer.CONFLICTING, 
               ancestor, left, right);
         }
      }

Once you have a compare editor input, opening a compare editor on that input is trivial. Here is an example action that opens a compare editor, using the preceding input:

      public class CompareEditorAction implements 
      IWorkbenchWindowActionDelegate {
         public void run(IAction action) {
            CompareUI.openCompareEditor(new CompareInput());
         }
      }

  
If you want to support merging as well as comparing, two extra steps are involved. First, you need to specify which of the elements is editable. This is done by the CompareConfiguration object that is passed to the CompareEditorInput constructor. Use the setLeftEditable and setRightEditable methods to specify which of the comparison panes should support modification. Second, your editor input class should override the save method to perform the save of the editor contents.

