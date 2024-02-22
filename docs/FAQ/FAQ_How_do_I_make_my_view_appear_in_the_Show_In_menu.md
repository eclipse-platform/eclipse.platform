

FAQ How do I make my view appear in the Show In menu?
=====================================================

The **Navigate > Show In** menu displays a list of views that the user can jump to from the active editor or view. This facility allows linking between views and editors that may not even know about each other.

The active perspective gets to decide what views appear in this list, but you can contribute your views to this list by using the perspectiveExtensions extension point. Here is an extension definition that adds the bookshelf example Chapters view to the **Show In** menu of the Resource perspective:

      <extension
         point="org.eclipse.ui.perspectiveExtensions">
         <perspectiveExtension targetID = 
            "org.eclipse.ui.resourcePerspective">
         <showInPart id = "org.eclipse.faq.examples.ChaptersView"/>
         </perspectiveExtension>
      </extension>

  
The Chapters view then implements the show method from the IShowInTarget interface. This method is called by the platform when the user selects the view in the **Show In** menu. The method has a parameter, ShowInContext, that is passed from the view that is the source of the **Show In** action. The method must return true if it accepts the context as a valid input for that view and false otherwise. Here is the example implementation from the Chapters view:

      public boolean show(ShowInContext context) {
         if (viewer == null || context == null)
            return false;
         ISelection sel = context.getSelection();
         if (sel instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)sel;
            Object first = ss.getFirstElement();
            if (first instanceof Book) {
               viewer.setInput(first);
               return true;
            }
         }
         return false;
      }

A view that wants to act as a source for the **Show In** menu must implement IShowInSource. This interface defines the method getShowInContext, which creates the context object to be passed to the target. In our bookshelf example, the Books view will act as a **Show In** source by implementing the getShowInContext method as follows:

   public ShowInContext getShowInContext() {
      return new ShowInContext(null, viewer.getSelection());
   }

The context instance may contain an input object and a selection. If your view needs to provide extra context information, you can create your own ShowInContext subclass that carries additional data. Of course, only views that know about that special subclass will be able to make use of the extra information, so you should also provide the basic context information if you can.

See Also:
---------

[FAQ\_How\_can\_I\_add\_my\_views\_and\_actions\_to\_an\_existing\_perspective?](./FAQ_How_can_I_add_my_views_and_actions_to_an_existing_perspective.md "FAQ How can I add my views and actions to an existing perspective?")

