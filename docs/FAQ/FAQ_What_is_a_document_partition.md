

FAQ What is a document partition?
=================================

Each document is divided into one or more nonoverlapping _partitions_. Many of the text-framework features can be configured to operate differently for each partition. Thus, an editor can have different syntax highlighting, formatting, or Content Assist for each partition. For example, the Java editor in Eclipse has different partitions for strings, characters, and comments.

  
If no partitions are explicitly defined, the single default partition is of type IDocument.DEFAULT\_CONTENT\_TYPE. If the explicitly defined partitions do not span the entire document, all remaining portions of the document implicitly belong to this default partition. In other words, every character in the document belongs to exactly one partition. Most editors define explicit partitions for small portions of the document that need custom behavior, and the bulk of the document remains in the default partition.

  
Documents are partitioned by connecting them to an instance of org.eclipse.jface.text.IDocumentPartitioner. In the case of editors, this is usually added by the document provider as soon as the document is created. You can implement the partitioner interface directly if you want complete control, but in most cases you can simply use the default implementation, DefaultPartitioner. This example from the HTML editor defines a partitioner and connects it to a document:

      IDocumentPartitioner partitioner =
         new DefaultPartitioner(
            createScanner(),
            new String[] {
               HTMLConfiguration.HTML_TAG,
               HTMLConfiguration.HTML_COMMENT });
      partitioner.connect(document);
      document.setDocumentPartitioner(partitioner);

The default partitioner's constructor takes as arguments a scanner and an array of partition types for the document. The _partition scanner_ is responsible for computing the partitions. It is given a _region_ of the document and must produce a set of tokens describing each of the partitions in that region. When a document is created, the scanner is asked to create tokens for the entire document. When a document is changed, the scanner is asked to repartition only the region of the document that changed. Figure 15.2 shows the relationships among editor, document, partitioner, and scanner.

  

  
The text framework provides a powerful rule-based scanner infrastructure for creating a scanner based on a set of predicate rules. You simply create an instance of the scanner and plug in the rules that define the regions in your document. Each rule is given a stream of characters and must return a token representing the characters if they match the rule. Browse through the type hierarchy of IPredicateRule to see what default rules are available. The following snippet shows the creation of the scanner for the HTML editor example:

      IPartitionTokenScanner createScanner() {
         IToken cmt = new Token(HTMLConfiguration.HTML_COMMENT);
         IToken tag = new Token(HTMLConfiguration.HTML_TAG);
         IPredicateRule[] rules = new IPredicateRule[2];
         rules[0] = new MultiLineRule("", cmt);
         rules[1] = new TagRule(tag);
         RuleBasedPartitionScanner scanner = 
            new RuleBasedPartitionScanner();
         scanner.setPredicateRules(rules);
         return sc;
      }

  

See Also:
---------

[FAQ How do I get started with creating a custom text editor?](./FAQ_How_do_I_get_started_with_creating_a_custom_text_editor.md "FAQ How do I get started with creating a custom text editor?")

Go to **Platform Plug-in Developer Guide > Programmer's Guide >Editors > Documents and partitions** in Eclipse help, or at [help.eclipse.org](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/guide/editors_documents.htm).

