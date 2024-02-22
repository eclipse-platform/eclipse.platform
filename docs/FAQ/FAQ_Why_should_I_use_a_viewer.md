

FAQ Why should I use a viewer?
==============================

The main benefit of JFace viewers is that they allow you to work directly with your model elements instead of with low-level widgets. Instead of creating, disposing of, and modifying TreeItem instances in a Tree widget, you use the TreeViewer convenience methods, which deal directly with objects from your domain model. This makes your code cleaner and simpler, off-loading the complex work of finding and updating the widgets corresponding to your model elements when they change. These viewer methods have also been heavily tested and optimized over the years, which means that you will probably get a much more efficient, bug-free implementation than you would if you had written it yourself.

Another benefit of the viewer framework is that it is designed to encourage reuse. Viewers are customized by plugging in modular pieces rather than by subclassing. Each of these modular pieces addresses a specific concern, such as structure, appearance, and sorting. These pieces act as the unit of reuse: Any given piece can be reused in multiple viewers independently of the others. Thus, the piece that describes the appearance of your model elements-the label provider-can be reused in several viewers, independently of the piece that describes the relationship between elements-the content provider.

Aside from these main benefits, JFace viewers provide numerous other features that we don't have room to describe in detail.

Here is a quick overview of some of the main benefits of JFace viewers.

*   TreeViewer populates lazily, avoiding the work of creating and updating items that are not visible.
*   They support simple, model-based drag-and-drop.
*   They have methods for querying or changing the selection and for adding selection listeners.
*   They support pluggable sorters and content filters.
*   Cell editors allow the user to modify table values in place.
*   Decorators are available for adding annotations to the text and icons of viewer items.

In an earlier version of JFace, predating open source, viewers largely encapsulated the underlying widget toolkit, allowing you to create an application with few direct dependencies on the underlying widgets. Porting the application to another toolkit would thus mainly consist of porting JFace. This saved a great deal of effort when early prototypes built on JFace switched from a Swing-based implementation to an SWT-based implementation. But it was decided that this encapsulation required far too much duplication of SWT-level functionality and didn't always allow the native look and feel to shine through. Viewers were then simplified to their current form as adapters on the side of an SWT widget, enhancing but not hiding its function.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")
*   [FAQ What kinds of viewers does JFace provide?](./FAQ_What_kinds_of_viewers_does_JFace_provide.md "FAQ What kinds of viewers does JFace provide?")

