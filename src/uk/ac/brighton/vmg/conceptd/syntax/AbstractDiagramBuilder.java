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
	HashMap<String, ArrayList<String>> supersInfo;
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
        	supersInfo = new HashMap<String, ArrayList<String>>();
        }
	}
	
	/**
	 * Create the diagram
	 */
	public void build() {
		if (topClass != null) {
        	setClasses(topClass, hierarchyDepth, new ArrayList<String>());       
        	log.info("classes: "+classMap.values());
            setZones();
        	//setDisjoints();
        	//setZones2();
        }
	}
    
    /**
     * Collect the list of curve labels in the diagram, along with info on disjoint and union classes
     * @param selectedClass
     */
    private void setClasses(OWLClass cls, int depth, ArrayList<String> supers) {

    	if(depth>0) {
    		String nm = render(cls);
    		supersInfo.put(nm, supers);
    		if(depth==1) {
      		  	if(theProvider.getChildren(cls).size()>0) nm += "+";
      		  	classMap.put(cls, nm);
    		} else {
    			classMap.put(cls, nm);
      		  	int newDepth = --depth;
      		  	ArrayList<String> supers2 = (ArrayList<String>)supers.clone();
      		  	supers2.add(nm);
    	        for (OWLClass sub: theProvider.getChildren(cls)) {
    	        	setClasses(sub, newDepth, supers2);
    	        }	
    		}
    		//collect the disjointness info
    		NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(cls);//or use TR's method
    		ArrayList<String> disjoints = new ArrayList<String>();
    		Iterator<Node<OWLClass>> it = ds.iterator();
    		OWLClass c;	
    		while(it.hasNext()) {
    			c = it.next().getRepresentativeElement();
    			disjoints.add(render(c));
    		}
    		if(disjoints.size()>0) disjointsInfo.put(nm, disjoints);
    		//collect the equivalent classes info
    		Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
    		if(!equivs.isSingleton()) equivsInfo.put(nm, equivs.getEntities());
    		//collect the union classes info
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
    		//collect the inconsistent classes info
        	if(!theReasoner.isSatisfiable(cls)) inconsistentClasses.add(classMap.get(cls));
    	}
    }
    
    //  get zone for class and recursively all of its subclasses
    private void setZones() {
    	
    	//get the Venn diagram 
    	ArrayList<ArrayList<String>> p = powerSet(classMap.values());
    	ArrayList<ArrayList<String>> pTemp = (ArrayList<ArrayList<String>>)p.clone();
    	//remove various zones 
    	for(ArrayList<String> z : pTemp) {
    		if(!z.contains(topClassName)) {//remove zones that don't contain the outer curve
    			p.remove(z);
    		} else if(z.contains(EMPTY_LABEL)) {//remove inconsistent zones and shade the parent
    			log.info(z+" contains empty label");
    			p.remove(z);
    			ArrayList<String> sz = (ArrayList<String>)z;
    			sz.remove(EMPTY_LABEL);
    			shadedZones.add(sz);
    		} else {//remove zones which should be missing -- i.e. which contain disjoint classes
    			boolean removed = false;
    			for(String l: z) {
    				if(disjointsInfo.containsKey(l)) {
        				for(String d: disjointsInfo.get(l)) {
    						if(z.contains(d)) {
    							p.remove(z);
    							removed = true;
    							break;
    						}
        				}
        				if(!removed) {
        					if(supersInfo.containsKey(l)) {
	        					for(String c: supersInfo.get(l)) {
			    					if(!z.contains(c)) {
			    						p.remove(z);
		    							removed = true;
		    							break;
			    					}
			    				}
        					}
        				}
        			}
    				if(removed) break;
    			}
        	
    			//add shading
	    		for(String l: z) {//shade zones that contain inconsistent classes
	    			if(inconsistentClasses.contains(l)) {
	    				shadedZones.add(z);
	    				break;
		    		}
		    		if(equivsInfo.containsKey(l)) {//if l has a set of equivalent classes, S, only regions that contain l+S should be unshaded
		    			Set<OWLClass> equivs = equivsInfo.get(l);
		    			for(OWLClass e: equivs) {
		    				String eNm = classMap.get(e);
		    				if(!z.contains(eNm)) {
		    					shadedZones.add(z);
		    					break;
		    				}
		    			}
		    		}
		    		if(unionsInfo.containsKey(l)) {//if l is a union of classes, S, regions containing l but not S should be shaded
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
