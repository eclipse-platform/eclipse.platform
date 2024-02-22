

FAQ How do I provide syntax coloring in an editor?
==================================================

Syntax coloring in a JFace text editor is performed by a _presentation reconciler_, which divides the document into a set of tokens, each describing a section of the document that has a different foreground, background, or font style. Note that this sounds very similar to a partition token scanner, which divides the document into a series of partitions.

The tokens produced by the presentation reconciler are much more fine-grained than the ones produced by the partition scanner. For example, a Java file may be divided into partitions representing either javadoc or code. Within each partition, the presentation reconciler will produce separate tokens for each set of characters that have the same color and font. So, a Java keyword would be one token, and a string literal would be another. Each partition can have a different presentation reconciler installed, allowing for different rules to be used, depending on the type of content in the partition.

  
Once the initial presentation of a document is calculated, it needs to be incrementally maintained as the document is modified. The presentation reconciler uses two helper classes to accomplish this: a damager and a repairer. The damager takes as input a description of how the document changed and produces as output a description of the regions of the document whose presentation needs to be updated. For example, if a user deletes the > character representing the end of a tag in an HTML file, the region up to the next > character now needs to be colored as an HTML tag. The repairer's job is to update the presentation for all the damaged regions.

  
This all sounds very complicated, but the text framework will usually do most of this work for you. Typically you simply need to create a set of rules that describe the various tokens in your document. The framework has a default presentation reconciler that allows you to plug these rules into it, and the rest of the reconciling work is done for you. As an example, this is a scanner created by the sample HTML editor for scanning HTML tags:

      ITokenScanner scanner = new RuleBasedScanner();
      IToken string = createToken(colorString);
      IRule[] rules = new IRule[3];
      // Add rule for double quotes
      rules[0] = new SingleLineRule("\\"", "\\"", string, '\\\');
      // Add a rule for single quotes
      rules[1] = new SingleLineRule("'", "'", string, '\\\');
      // Add generic whitespace rule.
      rules[2] = new WhitespaceRule(whitespaceDetector);
      scanner.setRules(rules);
      scanner.setDefaultReturnToken(createToken(colorTag));

This scanner creates unique tokens for string literals within a tag so it can color them differently. Outside of strings, the rest of the tag is divided into white-space-separated tokens, using a white-space rule.

The createToken method instantiates a Token object for a particular color:

      private IToken createToken(Color color) {
         return new Token(new TextAttribute(color));
      }

This scanner is finally fed to a standard presentation reconciler in the SourceConfiguration subclass. You need to specify a different damager/repairer for each partition of your document:

      public IPresentationReconciler getPresentationReconciler(
      ISourceViewer sv) {
         PresentationReconciler rec = new PresentationReconciler();
         DefaultDamagerRepairer dr = 
            new DefaultDamagerRepairer(getTagScanner());
         rec.setDamager(dr, HTML_TAG);
         rec.setRepairer(dr, HTML_TAG);
         ... same for other partitions ...
         return rec;
      }

For more complex documents or for optimized reconciling, you can build your own custom damager and repairer instances by directly implementing IPresentationDamager and IPresentationRepairer, respectively. However, for most kinds of documents, a simple rule-based approach is sufficient.

  

See Also:
---------

[FAQ What is a document partition?](./FAQ_What_is_a_document_partition.md "FAQ What is a document partition?")

Go to **Platform Plug-in Developer Guide > Programmer's Guide >** Editors > Syntax coloring**.**

