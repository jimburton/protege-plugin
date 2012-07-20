package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class AbstractDiagramBuilder {
    
    private ArrayList<ArrayList<String>> zones;
    private ArrayList<ArrayList<String>> shadedZones;
    private ArrayList<String> inconsistentClasses;
    private HashMap<OWLClass, String> classMap;//store the display names of classes
    private int hierarchyDepth;
    private final String EMPTY_LABEL = "Nothing";
    
    private OWLClass topClass;
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    
    private OWLObjectHierarchyProvider<OWLClass> inferredHierarchyProvider;
    private OWLObjectHierarchyProvider<OWLClass> theProvider;
    private OWLReasoner theReasoner;
    private OWLModelManager mgr;
    
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;

    private enum MODEL {
    	INFERRED, ASSERTED
    };
    //private MODEL theModel = MODEL.INFERRED;
    private MODEL theModel = MODEL.ASSERTED;
    
    private static final Logger log = Logger.getLogger(AbstractDiagramBuilder.class);

	public AbstractDiagramBuilder(OWLClass selectedClass, OWLModelManager mgr, int d)  {
		log.info("new builder for "+selectedClass);
        if (selectedClass != null){
        	this.mgr = mgr;
        	hierarchyDepth = d;
        	topClass = selectedClass;
        	assertedHierarchyProvider = 
        			mgr.getOWLHierarchyManager().getOWLClassHierarchyProvider();
        	inferredHierarchyProvider = 
        			mgr.getOWLHierarchyManager().getInferredOWLClassHierarchyProvider();
        	theProvider = (theModel == MODEL.INFERRED ? 
        			inferredHierarchyProvider : assertedHierarchyProvider);
        	theReasoner = mgr.getOWLReasonerManager().getCurrentReasoner();
        	ren = mgr.getOWLEntityRenderer();
        	
        	shadedZones = new ArrayList<ArrayList<String>>();
        	zones = new ArrayList<ArrayList<String>>();
        	inconsistentClasses = new ArrayList<String>();
        	classMap = new HashMap<OWLClass, String>();

        }
	}
	
	/**
	 * Create the diagram
	 */
	public void build() {
		if (topClass != null) {
        	setClasses(topClass, hierarchyDepth);       
        	log.info("classes: "+classMap.values());
            setZones();
        }
	}
    
    /**
     * Collect the list of curve labels in the diagram
     * @param selectedClass
     */
    private void setClasses(OWLClass cls, int depth) {
    	//log.info("adding: "+render(cls));
    	//log.info("sat: "+theReasoner.isSatisfiable(cls));
    	switch (depth) {
    	  case 0:
    		  return;
    	  case 1:
    		  String nm = render(cls);
    		  if(theProvider.getChildren(cls).size()>0) nm += "+";
    		  classMap.put(cls, nm);
    	      break;
    	  default:	
    		  classMap.put(cls, render(cls));
    		  int newDepth = --depth;
  	          for (OWLClass sub: theProvider.getChildren(cls)) {
  	        	  setClasses(sub, newDepth);
  	          }	
    	}
    	//build the list of inconsistent classes
    	if(!theReasoner.isSatisfiable(cls)) inconsistentClasses.add(classMap.get(cls));
    }
    
    //  get zone for class and recursively all of its subclasses
    private void setZones() {
    	
    	//get the Venn diagram 
    	ArrayList<ArrayList<String>> p = powerSet(classMap.values());
    	ArrayList<ArrayList<String>> pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	//remove zones that don't contain the outer curve
    	String ocNm = classMap.get(topClass);
    	for(ArrayList<String> z : pTemp) {
    		if(!z.contains(ocNm)) p.remove(z);
    	}
    	//remove some more zones to get subclass relationships right
    	pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	for(OWLClass c: classMap.keySet()) {
    		if(!c.equals(topClass)) {
    			Set<OWLClass> children = theProvider.getChildren(c);
    			for(ArrayList<String> z : pTemp) {
    				if(!z.contains(classMap.get(c))) {
    					boolean keeper = true;
    					for(OWLClass c2: children) {
    						if(z.contains(classMap.get(c2))) {
    							keeper = false;
    							break;
    						}
    					}
    					if(!keeper) p.remove(z);
    				}
    	    	}
    		}
    	}
    	ArrayList<String> disjoints;
    	//we only need to gather disjointness info for n-1 classes, where n is number of curves 
    	//inside the outer curve
    	//Use disjointness info to remove missing regions from diagram
    	pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	int i=0;
    	for(OWLClass c: classMap.keySet()) {
    		if(i<=classMap.size()-1 && !c.equals(topClass)) {
    			String nm = classMap.get(c);
	    		NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(c);//or use TR's method
	    		disjoints = new ArrayList<String>();
	    		Iterator<Node<OWLClass>> it = ds.iterator();
	    		OWLClass cls;
	    		
	    		while(it.hasNext()) {
	    			cls = it.next().getRepresentativeElement();
	    			if(classMap.keySet().contains(cls)) {
	    				disjoints.add(classMap.get(cls));
	    			}
	    		}
	    		
	    		for(String d: disjoints) {
    				for(ArrayList<String> ls : pTemp) {
    					//remove missing zones but keep zones containing inconsistent classes:
    					//these don't appear in the disjoints because the reasoner ignores them
    					/*boolean unsat = false;
    					for(String badLabel: inconsistentClasses) {
    						if(ls.contains(badLabel)) {
    							unsat = true;
    							break;
    						}
    					}
    					if(!unsat && ls.contains(nm) && ls.contains(d)) {
    						p2.remove(ls);
    					}*/
    					if(ls.contains(nm) && ls.contains(d)) {
    						p.remove(ls);
    					}
    				}
    			}
    		}
    	}
    	
    	//look for union classes and equivalent classes so we can add shading later on
    	HashMap<String, Set<OWLClass>> equivsInfo = new HashMap<String, Set<OWLClass>>();
    	HashMap<String, Set<OWLClass>> unionsInfo = new HashMap<String, Set<OWLClass>>();
    	for(OWLClass cls: classMap.keySet()) {
    		Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
    		if(!equivs.isSingleton()) {
    			Set<OWLClass> es = equivs.getEntities();
    			//only keep the classes in the current diagram
    			es.retainAll(classMap.keySet());
    			equivsInfo.put(render(cls), es);
    		}
    		Set<OWLOntology> onts = mgr.getActiveOntologies();
    		Set<OWLEquivalentClassesAxiom> eq;
    		for(OWLOntology ont: onts) {
    			eq = ont.getEquivalentClassesAxioms(cls);
    			if(eq.size()>0) {
    	    		for(OWLEquivalentClassesAxiom a: eq) {
    	    			boolean isUnion = true;
    	    			ClassExpressionType t;
    	    			for(OWLClassExpression e: a.getClassExpressions()) {
    	    				t = e.getClassExpressionType();
    	    				if (!(t.equals(ClassExpressionType.OWL_CLASS) 
    	    						|| t.equals(ClassExpressionType.OBJECT_UNION_OF))) {
    	    					isUnion = false;
    	    					break;
    	    				}
    	    			}
    	    			if(isUnion) {
    	    				Set<OWLClass> us = a.getClassesInSignature();
    	    				us.remove(cls);
    	    				//only keep the classes in the current diagram
    	    				us.retainAll(classMap.keySet());
    	    				if(us.size()>0) unionsInfo.put(render(cls), us);
            			}
    	    		}
        			
        		}
    		}
    	}
    	//add shaded zones
    	pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	//log.info("inconsistent classes: "+inconsistentClasses);
    	for(ArrayList<String> zone: pTemp) {
    		if(zone.contains(EMPTY_LABEL)) {
    			log.info(zone+" contains empty label");
    			p.remove(zone);
    			ArrayList<String> sz = (ArrayList<String>)zone;
    			sz.remove(EMPTY_LABEL);
    			shadedZones.add(sz);
    		} else {
    			
	    		for(String l: zone) {
	    			//log.info("consistent?: "+l);
	    			if(inconsistentClasses.contains(l)) {
	    				shadedZones.add(zone);
						break;
	    			}
		    		if(equivsInfo.containsKey(l)) {
		    			Set<OWLClass> equivs = equivsInfo.get(l);
		    			for(OWLClass e: equivs) {
		    				String eNm = classMap.get(e);
		    				if(!zone.contains(eNm)) {
		    					shadedZones.add(zone);
		    					break;
		    				}
		    			}
		    		}
		    		if(unionsInfo.containsKey(l)) {
		    			Set<OWLClass> unions = unionsInfo.get(l);
		    			boolean shadeMe = true;
		    			for(OWLClass u: unions) {
		    				String uNm = classMap.get(u);
		    				if(zone.contains(uNm)) {
		    					shadeMe = false;
		    					break;
		    				}
		    			}
		    			if(shadeMe) shadedZones.add(zone);
		    		}	
	    		}
    		}
    	}
    	zones = p;
    }
    
    private String render(OWLClass cls) {
    	return ren.render(cls).replaceAll("'", "").replaceAll(" ", "");
    }
    
    /**
     * Take the powerset of a list
     * @param originalSet
     * @return
     */
    private <T> ArrayList<ArrayList<T>> powerSet(Collection<T> originalSet) {
        ArrayList<ArrayList<T>> sets = new ArrayList<ArrayList<T>>();
        if (originalSet.isEmpty()) {
            sets.add(new ArrayList<T>());
            return sets;
        }
        List<T> list = new ArrayList<T>(originalSet);
        T head = list.get(0);
        ArrayList<T> rest = new ArrayList<T>(list.subList(1, list.size())); 
        for (ArrayList<T> set : powerSet(rest)) {
            ArrayList<T> newSet = new ArrayList<T>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }           
        return sets;
    }

	public ArrayList<ArrayList<String>> getZones() {
		return zones;
	}

	public ArrayList<ArrayList<String>> getShadedZones() {
		return shadedZones;
	}


}
