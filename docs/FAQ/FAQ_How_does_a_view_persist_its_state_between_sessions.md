

FAQ How does a view persist its state between sessions?
=======================================================

Storing view state is done in two commons ways, depending on whether you want to store settings between workbench sessions or across invocations of your view. The first of these facilities is found directly on IViewPart. When the workbench is shut down, the method saveState is called on all open views. The parameter to this method is an IMemento, a simple data structure that stores hierarchies of nodes containing numbers and strings. Here is an example from the recipe application in the FAQ examples, where a view is persisting the current selection of a list viewer in a memento:

      private static final String STORE_SELECTION = "ShoppingList.SELECTION";
      public void saveState(IMemento memento) {
         super.saveState(memento);
         ISelection sel = viewer.getSelection();
         IStructuredSelection ss = (IStructuredSelection) sel;
         StringBuffer buf = new StringBuffer();
         for (Iterator it = ss.iterator(); it.hasNext();) {
            buf.append(it.next());
            buf.append(',');
         }
         memento.putString(STORE_SELECTION, buf.toString());
      }

When the workbench is reopened, the method init(IViewSite, IMemento) is called the first time each view becomes visible. The IMemento will contain all the information that was added to it when the workbench was shut down. Note that init is called before the createPartControl method, so you will not be able to restore widget state directly from the init method. You can store the IMemento instance in a field and restore state later on when your widgets have been created. Continuing this example, here is the code for restoring the viewer selection when the view is reopened:

      private IMemento memento;
      ...
      public void init(IViewSite site, IMemento memento)
         throws PartInitException {
         super.init(site, memento);
         this.memento = memento;
      }
      public void createPartControl(Composite parent) {
         //create widgets ...
         if (memento == null) return;
         String value = memento.getString(STORE_SELECTION);
         if (value == null) return;
         IStructuredSelection ss = new StructuredSelection(value.split(","));
         viewer.setSelection(ss);
      }

Note that the IMemento instance can be null if the view state was not saved from a previous session: for example, when the view is first created.

Another mechanism for persisting view state is the JFace IDialogSettings facility. The advantage of dialog settings over the view save/init mechanism is that you can control when settings are persisted. The saveState method is called only if your view is open when the workbench shuts down, so it is not useful for storing view state when the view is closed by the user. Dialog settings, on the other hand, can be changed and persisted whenever you want.

Views commonly use a combination of both dialog settings and a memento for persisting view state. Important settings, such as filters, sorters, and other view preferences, will be stored as dialog settings; more transient attributes, such as selection and expansion state, will be stored in the memento only when the workbench is being shut down.

See Also:
---------

*   [FAQ How do I save settings for a dialog or a wizard?](./FAQ_How_do_I_save_settings_for_a_dialog_or_a_wizard.md "FAQ How do I save settings for a dialog or a wizard?")

