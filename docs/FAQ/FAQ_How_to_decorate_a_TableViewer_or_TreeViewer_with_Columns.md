

FAQ How to decorate a TableViewer or TreeViewer with Columns?
=============================================================

Eclipse does not support decorating TableViewers and TreeViewers with TreeColumns as they have proposed Decorators for TreeViewers. But in many cases where we use TreeViewers with TreeColumns or TableViewers we need to provide some decorations based on some Condition.

This can be achieved by extending the DecoratingLabelProvider class and creating your own class called as <Your Class Name> for e.g.TableDecoratingLabelProvider and implement ITableLabelProvider in that class.

  
It would look something like this

    /**
     *  Class that supports Decoration of TableViewer and TreeViewer with TreeColumns
     */
    package decorators;
     
    import org.eclipse.jface.viewers.DecoratingLabelProvider;
    import org.eclipse.jface.viewers.ILabelDecorator;
    import org.eclipse.jface.viewers.ILabelProvider;
    import org.eclipse.jface.viewers.ITableLabelProvider;
    import org.eclipse.swt.graphics.Image;
     
    /**
     *
     */
    public class TableDecoratingLabelProvider extends DecoratingLabelProvider
        implements ITableLabelProvider {
     
    ITableLabelProvider provider;
    ILabelDecorator decorator;
    /**
    * @param provider
    * @param decorator
    */
    public TableDecoratingLabelProvider(ILabelProvider provider,
        ILabelDecorator decorator) {
      super(provider, decorator);
      this.provider = (ITableLabelProvider) provider;
      this.decorator = decorator;
    }
     
    /* (non-Javadoc)
    * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
    */
    public Image getColumnImage(Object element, int columnIndex) {
      Image image = provider.getColumnImage(element, columnIndex);
      if (decorator != null) {
        Image decorated = decorator.decorateImage(image, element);
        if (decorated != null) {
          return decorated;
        }
      }
      return image;
    }
     
    /* (non-Javadoc)
    * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
    */
    public String getColumnText(Object element, int columnIndex) {
      String text = provider.getColumnText(element, columnIndex);
      if (decorator != null) {
        String decorated = decorator.decorateText(text, element);
        if (decorated != null) {
          return decorated;
        }
      }
      return text;
    }

This class can now handle Trees which have Columns and TableViewers. How you use it is specified below

While attaching the LabelProvider to your viewer use this Code

    ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
    <Your Viewer>.setLabelProvider(new TableDecoratingLabelProvider(<Your LabelProvider>, decorator));

instead of

    ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
    <Your Viewer>.setLabelProvider(new DecoratingLabelProvider(<Your LabelProvider>, decorator));

See Also:
---------

*   [FAQ What is a label decorator?](./FAQ_What_is_a_label_decorator.md "FAQ What is a label decorator?")
*   [FAQ How do I create a label decorator declaratively?](./FAQ_How_do_I_create_a_label_decorator_declaratively.md "FAQ How do I create a label decorator declaratively?")

