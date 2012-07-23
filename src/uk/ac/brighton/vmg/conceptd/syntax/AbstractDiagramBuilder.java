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
    private HashMap<String, ArrayList<String>> disjointsInfo;
    HashMap<String, Set<OWLClass>> equivsInfo;
	HashMap<String, Set<OWLClass>> unionsInfo;
    private int hierarchyDepth;
    private final String EMPTY_LABEL = "Nothing";
    
    private OWLClass topClass;
    private String topClassName;
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    
    private OWLObjectHierarchyProvider<OWLClass> inferredHierarchyProvider;
    private OWLObjectHierarchyProvider<OWLClass> theProvider;
    private OWLReasoner theReasoner;
    private OWLModelManager mgr;
    private Set<OWLOntology> activeOntologies;
    
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
        	topClassName = render(topClass);
        	activeOntologies = mgr.getActiveOntologies();
        	
        	shadedZones = new ArrayList<ArrayList<String>>();
        	zones = new ArrayList<ArrayList<String>>();
        	inconsistentClasses = new ArrayList<String>();
        	classMap = new HashMap<OWLClass, String>();
        	disjointsInfo = new HashMap<String, ArrayList<String>>();
        	equivsInfo = new HashMap<String, Set<OWLClass>>();
        	unionsInfo = new HashMap<String, Set<OWLClass>>();

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
        	//setDisjoints();
        	//setZones2();
        }
	}
    
    /**
     * Collect the list of curve labels in the diagram
     * @param selectedClass
     */
    private void setClasses(OWLClass cls, int depth) {
    	//log.info("adding: "+render(cls));
    	//log.info("sat: "+theReasoner.isSatisfiable(cls));
    	if(depth>0) {
    		String nm = render(cls);
    		if(depth==1) {
      		  	if(theProvider.getChildren(cls).size()>0) nm += "+";
      		  	classMap.put(cls, nm);
    		} else {
    			classMap.put(cls, nm);
      		  	int newDepth = --depth;
    	        for (OWLClass sub: theProvider.getChildren(cls)) {
    	        	setClasses(sub, newDepth);
    	        }	
    		}
    		NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(cls);//or use TR's method
    		ArrayList<String> disjoints = new ArrayList<String>();
    		Iterator<Node<OWLClass>> it = ds.iterator();
    		OWLClass c;
    		
    		while(it.hasNext()) {
    			c = it.next().getRepresentativeElement();
    			disjoints.add(render(c));
    		}
    		disjointsInfo.put(nm, disjoints);
    		Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
    		if(!equivs.isSingleton()) {
    			equivsInfo.put(nm, equivs.getEntities());
    		}
    		Set<OWLEquivalentClassesAxiom> eq;
    		for(OWLOntology ont: activeOntologies) {
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
    	    				if(us.size()>0) unionsInfo.put(render(cls), us);
            			}
    	    		}
        			
        		}
    		}
    		//build the list of inconsistent classes
        	if(!theReasoner.isSatisfiable(cls)) inconsistentClasses.add(classMap.get(cls));
    	}
    }
    
    //  get zone for class and recursively all of its subclasses
    private void setZones() {
    	
    	//get the Venn diagram 
    	ArrayList<ArrayList<String>> p = powerSet(classMap.values());
    	ArrayList<ArrayList<String>> pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	//remove zones that don't contain the outer curve
    	for(ArrayList<String> z : pTemp) {
    		if(!z.contains(topClassName)) {
    			p.remove(z);
    		} else if(z.contains(EMPTY_LABEL)) {
    			log.info(z+" contains empty label");
    			p.remove(z);
    			ArrayList<String> sz = (ArrayList<String>)z;
    			sz.remove(EMPTY_LABEL);
    			shadedZones.add(sz);
    		} else {
    	    	//we only need to gather disjointness info for n-1 classes, where n is number of curves 
    	    	//inside the outer curve
    	    	//Use disjointness info to remove missing regions from diagram
    			outer:
    			for(String l: z) {
    				if(disjointsInfo.containsKey(l)) {
        				for(String d: disjointsInfo.get(l)) {
    						if(z.contains(d)) {
    							p.remove(z);
    							break outer;
    						}
        				}
        			}
    			}
	    		for(String l: z) {
	    			//log.info("consistent?: "+l);
	    			if(inconsistentClasses.contains(l)) {
	    				shadedZones.add(z);
	    				break;
		    		}
		    		if(equivsInfo.containsKey(l)) {
		    			Set<OWLClass> equivs = equivsInfo.get(l);
		    			for(OWLClass e: equivs) {
		    				String eNm = classMap.get(e);
		    				if(!z.contains(eNm)) {
		    					shadedZones.add(z);
		    					break;
		    				}
		    			}
		    		}
		    		if(unionsInfo.containsKey(l)) {
		    			Set<OWLClass> unions = unionsInfo.get(l);
		    			boolean shadeMe = true;
		    			for(OWLClass u: unions) {
		    				String uNm = classMap.get(u);
		    				if(z.contains(uNm)) {
		    					shadeMe = false;
		    					break;
		    				}
		    			}
		    			if(shadeMe) shadedZones.add(z);
		    		}	
	    		}
    		}
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
    	zones = p;
    }
    
//  get zone for class and recursively all of its subclasses
    private void setZones2() {
    	
    	zones = perms(classMap.values());
    	
    	//look for union classes and equivalent classes so we can add shading later on
    	HashMap<String, Set<OWLClass>> equivsInfo = new HashMap<String, Set<OWLClass>>();
    	HashMap<String, Set<OWLClass>> unionsInfo = new HashMap<String, Set<OWLClass>>();
    	for(OWLClass cls: classMap.keySet()) {
    		Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
    		if(!equivs.isSingleton()) {
    			Set<OWLClass> es = equivs.getEntities();
    			//only keep the classes in the current diagram
    			es.retainAll(classMap.keySet());
    			equivsInfo.put(classMap.get(cls), es);
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
    	ArrayList<ArrayList<String>> zonesTemp = (ArrayList<ArrayList<String>>)zones.clone();
    	//log.info("inconsistent classes: "+inconsistentClasses);
    	for(ArrayList<String> zone: zonesTemp) {
    		if(zone.contains(EMPTY_LABEL)) {
    			log.info(zone+" contains empty label");
    			zones.remove(zone);
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
    
    private ArrayList<ArrayList<String>> perms(Collection<String> originalSet) {
        ArrayList<ArrayList<String>> sets = new ArrayList<ArrayList<String>>();
        if (originalSet.isEmpty()) {
            sets.add(new ArrayList<String>());
            return sets;
        }
        List<String> list = new ArrayList<String>(originalSet);
        String head = list.get(0);
        ArrayList<String> rest = new ArrayList<String>(list.subList(1, list.size())); 
        for (ArrayList<String> set : perms(rest)) {
            ArrayList<String> newSet = new ArrayList<String>();
            newSet.add(head);
            newSet.addAll(set);
            if(newSet.contains(classMap.get(topClass)) && subClassRelationshipOK(newSet) 
            		&& noDisjoints(newSet)) {
            	sets.add(newSet);
            }
            if(set.contains(classMap.get(topClass)) && subClassRelationshipOK(set) && noDisjoints(set)) {
            	sets.add(set);
            }
        }           
        return sets;
    }
    
    private boolean subClassRelationshipOK(ArrayList<String> z) {
    	boolean keeper = true;
    	for(OWLClass c: classMap.keySet()) {
    		if(!c.equals(topClass)) {
    			Set<OWLClass> children = theProvider.getChildren(c);
    			if(!z.contains(classMap.get(c))) {
					for(OWLClass c2: children) {
						if(z.contains(classMap.get(c2))) {
							keeper = false;
							break;
						}
					}
    			}
    		}
    	}
    	return keeper;
    }
    
    private boolean noDisjoints(ArrayList<String> z) {
    	for(String l: z) {
    		if(disjointsInfo.containsKey(l)) {
	    		ArrayList<String> disjoints = disjointsInfo.get(l);
	    		for(String d: disjoints) {
	    			if(z.contains(d)) return false;
	    		}
    		}
    	}
    	return true;
    }
    
    //TODO merge this with setClasses
    private void setDisjoints() {
    	int i=0;
    	ArrayList<String> disjoints;
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
	    		disjointsInfo.put(classMap.get(c), disjoints);
    		}
    	}
    }

	public ArrayList<ArrayList<String>> getZones() {
		return zones;
	}

	public ArrayList<ArrayList<String>> getShadedZones() {
		return shadedZones;
	}


}
