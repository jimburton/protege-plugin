Ontology Visualisation with ConceptViz
======================================

ConceptViz is a visualisation plugin for the [Protégé ontology editor] [1].
It allow the user to visualise their ontology as a [concept diagram] [2].
The notation of concept diagrams is based on Euler diagrams, whereby circles
represent classes, the placement of the circles represents taxonomical 
information, and shading represents inconsistency. Concept diagrams include
additional syntax to represent individuals and roles, making them expressive 
enough to model an entire ontology, unlike other visualisations we know of.

ConceptViz makes use of the [iCircles] [3] library to draw Euler diagrams. At 
present, roles and individuals are not represented -- these are a high priority 
and development is under way. The immediate goal, however, is to produce a 
stable Euler-based ontology visualisation tool. Concept diagrams and the 
ConceptViz tool were originally developed at the University of Brighton. For 
more information visit (http://ontologyengineering.org). 

[1]: http://protege.stanford.edu "Protégé ontology editor"
[2]: http://www.ontologyengineering.org/node/19 "concept diagram"
[3]: http://www.eulerdiagrams.com/inductivecircles.html "iCircles"
