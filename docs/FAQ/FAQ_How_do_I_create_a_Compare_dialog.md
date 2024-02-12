

FAQ How do I create a Compare dialog?
=====================================

  

  

The Eclipse SDK includes actions for comparing and replacing files in the workspace with one another and with editions in the local history. The same mechanisms can be used for comparing any kind of text content, regardless of its source. EditionSelectionDialog is used to ask the user to select from an array of input elements. The inputs must implement several interfaces. First, they must implement ITypedElement to provide the name, image, and content type of the object to be compared. Second, they must implement IModificationDate to provide a timestamp of the object's creation or modification date. The timestamp is used to sort the input elements chronologically. Finally, they must implement IStreamContentAccessor

to supply the content to be compared. Here is an example of a class that implements all these interfaces for string-based content:

      class CompareItem implements IStreamContentAccessor,
               ITypedElement, IModificationDate {
         private String contents, name;
         private long time;
         CompareItem(String name, String contents, long time) {
            this.name = name;
            this.contents = contents;
            this.time = time;
         }
         public InputStream getContents() throws CoreException {
            return new ByteArrayInputStream(contents.getBytes());
         }
         public Image getImage() {return null;}
         public long getModificationDate() {return time;}
         public String getName() {return name;}
         public String getString() {return contents;}
         public String getType() {return ITypedElement.TEXT_TYPE;}
      }

The most interesting method here is the getType method, which should return the file extension of the input element. The file extension is used to determine the viewer for displaying the contents of the object.

  
The method EditionSelectionDialog.selectEdition accepts an array of objects that implement all the interfaces mentioned earlier. It will open a dialog, allow the user to select one of the available editions, and return the chosen result. The EditionSelectionDialog instance is initialized in a somewhat unorthodox manner by requiring a ResourceBundle object in the constructor. This bundle must supply all the text messages that appear in the dialog, in addition to such parameters as the dialog's default width and height. See the EditionSelectionDialog constructor comment for more details. In the FAQ Examples plug-in, see the CompareStringsAction example for a complete illustration of how this dialog is used.

