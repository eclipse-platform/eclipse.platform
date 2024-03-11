

FAQ How do I use properties to optimize a viewer?
=================================================

The word _property_ is one of those unfortunate programming terms that is overused because it can mean just about anything. The downside of using such a semantically weak term is that in different contexts it can mean very different things. The Eclipse Project certainly does not escape this foible: At the time of this writing, 154 classes and 1,105 methods contain the word _property_ in the Eclipse SDK code base.

In the context of JFace viewers, _property_ means any aspect that has relevance to how the domain objects being displayed are presented. Concretely, this means that whenever a property changes, the visual presentation of the viewer has to be updated in some way.

The interface IBasicPropertyConstants defines some simple properties-parent, children, text, and image-but the set of possible properties is open ended. Let's say, for example, that you have a tree viewer that is showing a person's ancestral tree. The label of each tree item may contain different information, such as the person's name and year of birth. The viewer may have several filters for showing subsets of the family tree, such as gender or hair color. You could define a set of domain-specific properties for that viewer to enumerate all these values:

      public interface IAncestralConstants {
         public static final String BIRTH_YEAR = "birth-year";
         public static final String NAME = "name";
         public static final String GENDER = "gender";
         public static final String HAIR_COLOR= "hair-color";
      }

Note that a property doesn't necessarily represent something that is directly visible but rather something that affects the presentation in some way.

The label provider, filters, and sorters associated with the viewer define which properties affect them. In this case, the label provider cares only about the name and birth year, so it would override the isLabelProperty method on IBaseLabelProvider as follows:

      public boolean isLabelProperty(Object el, String prop) {
         return IAncestralConstants.NAME.equals(prop) ||
            IAncestralConstants.BIRTH_YEAR.equals(prop);
      }

Similarly, a sorter that sorts entries by name would override the method ViewerSorter.isSorterProperty to return true only for the NAME property. A filter that defines a subset based on hair color would override ViewerFilter.isFilterProperty to return true only for the HAIR_COLOR property.

Still not seeing the point of all this? Well, here's the interesting bit. When changes are made to the domain objects, the viewer's update method, defined on StructuredViewer, can be passed a set of properties to help it optimize the update:

      viewer.update(person, new String[] {NAME});
      viewer.update(person, new String[] {GENDER, HAIR_COLOR});

The viewer update algorithm will ask the label provider and any installed sorter and filters whether they are affected by the property being changed. This information can allow the update method to make some very significant optimizations. If the sorter and filters are not affected by the change, the update is very fast. If any sorter or filter is affected by the change, significant work has to be done to update the view.

You can ignore this whole mechanism by passing null as the properties to the update method, but it means that a complete refresh will happen on every update. Resist the temptation to do this, especially if your viewer will ever have sorters or filters attached to it. For a viewer that contains a large number of values, the speedup from using properties can be significant.

See Also:
---------

*   [FAQ What is a viewer?](./FAQ_What_is_a_viewer.md "FAQ What is a viewer?")

