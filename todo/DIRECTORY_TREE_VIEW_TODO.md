Create a Directory Tree View.

Create a new view . The view will display the directory tree. The tree view is not the same as the tree view we're using already. This new tree view, i will call a graph. It will be layed out like a graph where the nodes are ous and dcs, and the edges are children of the parent.

This is require create a lot of our own custom UI elements.

The background of the view should be the Intellij Island background. There should be equal spaced dim dots (say 8 dp space horizontally and vertically). These dots cannot be interacted with, its just a background.

Nodes should be Square with rounded corners and a slight border. They will highlight when moused over. Edges should be a thin line that is border color. 

In order to open this view, add a new button to the MainToolbar that opens and loads the tree. The view should live in the "Record View", and will have a named tab that a user can close.