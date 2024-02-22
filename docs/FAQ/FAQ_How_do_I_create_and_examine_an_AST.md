

FAQ How do I create and examine an AST?
=======================================
  
An AST is created by using an instance of ASTParser, created using the newParser factory method. You will typically create an AST for a compilation unit in the workspace, but you can also create ASTs for class files or source code from other locations. A powerful feature introduced in Eclipse 3.0 is the ability to produce a partial AST. For example, you can create a skeletal AST representing only the principal structure of the file or with only a single method body fully resolved. This offers a considerable performance gain over a full-blown AST if you need to extract information from only a small portion of a file. See the javadoc of ASTParser for more details.
  
Once an AST is created, the most common way to traverse or manipulate it is through a visitor. As with the traditional visitor pattern, each variety of AST node has a different visit method, so you can implement a visitor that analyzes only certain kinds of expressions or statements. Outside of visitors, each AST node offers accessor methods for each child type that is appropriate for that node. For example, a MethodDeclaration node has getBody and setBody methods for accessing or replacing the block statement representing the body of the method. There are no methods for generically accessing the children of a node, although there is a generic getParent method for accessing the parent of a node.
  
The PrintASTAction class in the FAQ Examples plug-in shows a simple example of constructing and traversing an AST for the currently selected compilation unit. A visitor prints out the name of each AST node in the file with braces surrounding the children of each node:

      class ASTPrinter extends ASTVisitor {
         StringBuffer buffer = new StringBuffer();
         public void preVisit(ASTNode node) {
            //write the name of the node being visited
            printDepth(node);
            String name = node.getClass().getName();
            name = name.substring(name.lastIndexOf('.')+1);
            buffer.append(name);
            buffer.append(" {\r\n");
         }
         public void postVisit(ASTNode node) {
            //write a closing brace to indicate end of the node
            printDepth(node);
            buffer.append("}\r\n");
         }
         void printDepth(ASTNode node) {
            //indent the current line to an appropriate depth
            while (node != null) {
               node = node.getParent();
               buffer.append("  ");
            }
         }
      }
      ...
      //java model handle for selected file
      ICompilationUnit unit = ...;
      ASTParser parser = ASTParser.newParser(AST.JLS2);
      parser.setKind(ASTParser.K\_COMPILATION\_UNIT);
      CompilationUnit ast = 
                     (CompilationUnit)parser.createAST(null);
      ASTPrinter printer = new ASTPrinter();
      ast.accept(printer);
      MessageDialog.openInformation(shell, "AST for: " + 
         unit.getElementName(), printer.buffer.toString());

  

See Also:
---------

[FAQ\_How\_do\_I\_manipulate\_Java\_code?](./FAQ_How_do_I_manipulate_Java_code.md "FAQ How do I manipulate Java code?")

[FAQ\_What\_is\_an\_AST?](./FAQ_What_is_an_AST.md "FAQ What is an AST?")

