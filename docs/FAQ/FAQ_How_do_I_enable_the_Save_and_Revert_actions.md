

FAQ How do I enable the Save and Revert actions?
================================================

An editor with unsaved changes is said to be _dirty_. If an editor is closed while dirty, changes made in the editor since the last save should be discarded. The framework asks an editor whether it is dirty by calling the IEditorPart method isDirty. When the dirty state of an editor changes, it lets the world know by firing a property change event, IEditorPart.PROP_DIRTY, on the property. Here are the relevant minimal-editor example sections that control the dirty state:

      public class MinimalEditor extends EditorPart {
         protected boolean dirty = false;
         ...
         public boolean isDirty() {
            return dirty;
         }
         protected void setDirty(boolean value) {
            dirty = value;
            firePropertyChange(PROP_DIRTY);
         }
      }
	

  
The editor **Save** action should persist the current editor contents and then set the dirty state to false. Unlike most actions that are defined within an instance of IAction, the editor **Save** and **Save As...** actions are built directly into the editor part. These actions are always enabled when the editor is in a dirty state. The editor must support these actions by implementing the methods on ISaveablePart, which is extended by IEditorPart. Here are trivial implementations of these methods from the minimal-editor example:

      public void doSave(IProgressMonitor monitor) {
         setDirty(false);
      }
      public void doSaveAs() {
         doSave(null);
      }
      public boolean isSaveAsAllowed() {
         return false;
      }

  
Unlike the **Save** action, the **Revert** action is not built into the editor framework. The **Revert** action is one of the standard workbench global actions and is entirely optional for editors to implement. The global action is hooked in just like other global actions. The **Revert** action should be equivalent to closing an editor without saving, then reopening on the previously saved contents. Like the **Save** action, it should change the dirty state to false and fire a property change event on IEditorPart.PROP_DIRTY when it completes successfully.

  

See Also:
---------

[FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?](./FAQ_How_do_I_enable_global_actions_such_as_Cut_Paste_and_Print_in_my_editor.md "FAQ How do I enable global actions such as Cut, Paste, and Print in my editor?")

