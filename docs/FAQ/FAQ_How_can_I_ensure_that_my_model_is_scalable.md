

FAQ How can I ensure that my model is scalable?
===============================================

  

  

  
Because eScript programs are assumed to be small, we did not put too much care into the optimization of the memory usage of our DOM. However, for languages in which programs tend to get much larger, such as C, Pascal, or C#, the definition of each individual field has to be carefully weighed. To represent `<windows.h>`, the number of DOM elements easily runs into hundreds of thousands or even millions.

  
When writing a model that needs to scale to large systems, you must pay careful attention to the memory your model uses. Traditional performance optimization techniques apply here, but _lazy loading_ and _most recently used (MRU) caching_ are particularly effective. The JDT's Java model, for example, is populated lazily as the elements are accessed. Java element handles do not exist in memory at any one time for every Java project in your workspace. Most Java elements implement the Openable interface, indicating that they are lazily populated when you invoke methods on them that require access to underlying structures. Although all Openable elements can be explicitly closed when no longer needed, the model automatically closes the MRU elements by storing references to all open elements in a least recently used (LRU) cache. The abstraction layer of thin Java element handles allows expensive data structures to be flushed even if clients maintain references to those Java elements indefinitely. Abstract syntax trees are also generated lazily as they are needed and are quickly discarded when no longer referenced.

  
Although some of these performance enhancements can be added retroactively to your model implementation, it is important to think about scalability right from the start. Performance aspects of your model and of the tools built on top of it, need to be considered early in architectural planning. In particular, an architecture that does not let clients of your API maintain references to large internal data structures is essential to the scalability of your model.

  

See Also:
---------

[FAQ Language integration phase 3: How do I edit programs?](./FAQ_Language_integration_phase_3_How_do_I_edit_programs.md "FAQ Language integration phase 3: How do I edit programs?")

[FAQ How do I create Java elements?](./FAQ_How_do_I_create_Java_elements.md "FAQ How do I create Java elements?")

