package uk.ac.brighton.vmg.conceptd.ui;

import icircles.abstractDescription.AbstractDescription;
import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;
import icircles.gui.CirclesPanel;
import icircles.util.CannotDrawException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

//import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JPanel cdPanel;
    private JComboBox depthPicker;
    
    private ArrayList<ArrayList<String>> zones;
    private ArrayList<ArrayList<String>> shadedZones;
    private ArrayList<String> inconsistentClasses;
    private HashMap<OWLClass, String> classMap;//store the display names of classes
    private int hierarchyDepth = 3;
    private final String EMPTY_LABEL = "Nothing";
    
    private OWLClass topClass;
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    
    private OWLObjectHierarchyProvider<OWLClass> inferredHierarchyProvider;
    private OWLObjectHierarchyProvider<OWLClass> theProvider;
    private OWLReasoner theReasoner;
    
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    private int DIAG_SIZE;
    private final double DIAG_SCALE = 0.9;
    private enum MODEL {
    	INFERRED, ASSERTED
    };
    //private MODEL theModel = MODEL.INFERRED;
    private MODEL theModel = MODEL.ASSERTED;
    
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BorderLayout(6, 6));
		
		JPanel topPanel = new JPanel();
		JLabel depthLabel = new JLabel("Depth:");
		String[] depths = { "1", "2", "3", "4", "5" };
		depthPicker = new JComboBox(depths);
		depthPicker.setSelectedIndex(2);
		depthPicker.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            hierarchyDepth = Integer.parseInt((String)depthPicker.getSelectedItem());
	        }
	    });
		topPanel.add(depthLabel);
		topPanel.add(depthPicker);
		add(topPanel, BorderLayout.NORTH);
		cdPanel = new JPanel();
		cdPanel.setBackground(Color.WHITE);
		add(cdPanel, BorderLayout.CENTER);

        log.debug("CD View Component initialized");
	}

	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		DIAG_SIZE = Math.max(getHeight(), getWidth()) - 100;
        if (selectedClass != null){
        	topClass = selectedClass;
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
        	inferredHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider();
        	theProvider = (theModel == MODEL.INFERRED ? inferredHierarchyProvider : assertedHierarchyProvider);
        	theReasoner = getOWLModelManager().getOWLReasonerManager().getCurrentReasoner();
        	ren = getOWLModelManager().getOWLEntityRenderer();
        	
        	shadedZones = new ArrayList<ArrayList<String>>();
        	zones = new ArrayList<ArrayList<String>>();
        	inconsistentClasses = new ArrayList<String>();
        	classMap = new HashMap<OWLClass, String>();
        	
        	setClasses(topClass, hierarchyDepth);       
        	log.info("classes: "+classMap.values());
            setZones();
            drawCD();
        }
        return selectedClass;
	}
	
	private void drawCD() {
		//log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel());
	}
    
    //  get zone for class and recursively all of its subclasses
    private void setZones() {
    	
    	//get the Venn diagram 
    	ArrayList<ArrayList<String>> p0 = powerSet(classMap.values());
    	ArrayList<ArrayList<String>> p = (ArrayList<ArrayList<String>>)p0.clone();
    	//remove zones that don't contain the outer curve
    	String ocNm = classMap.get(topClass);
    	for(ArrayList<String> z : p0) {
    		if(!z.contains(ocNm)) p.remove(z);
    	}
  
    	ArrayList<String> disjoints;
    	//we only need to gather disjointness info for n-1 classes, where n is number of curves 
    	//inside the outer curve
    	//Use disjointness info to remove missing regions from diagram
    	ArrayList<ArrayList<String>> p2 = (ArrayList<ArrayList<String>>)p.clone();
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
    				for(ArrayList<String> ls : p) {
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
    						p2.remove(ls);
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
    		Set<OWLOntology> onts = getOWLModelManager().getActiveOntologies();
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
    	ArrayList<ArrayList<String>> p3 = (ArrayList<ArrayList<String>>)p2.clone();
    	//log.info("inconsistent classes: "+inconsistentClasses);
    	for(ArrayList<String> zone: p2) {
    		if(zone.contains(EMPTY_LABEL)) {
    			log.info(zone+" contains empty label");
    			p3.remove(zone);
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
    	zones = p3;
    }
    
    /**
     * Collect the list of curve labels in the diagram
     * @param selectedClass
     */
    private void setClasses(OWLClass cls, int depth) {
    	log.info("adding: "+render(cls));
    	log.info("sat: "+theReasoner.isSatisfiable(cls));
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
    
    ////////////////////////////////
    // iCircles 
    ////////////////////////////////
    
    private JPanel getCDPanel() {
        //log.info("drawing diagram " + desc);
        Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC,  16);

        ConcreteDiagram cd = null;
        String failureMessage = null; 
        try {
            cd = getDiagram();
            cd.setFont(font);
        } catch (CannotDrawException x) {
            failureMessage = x.message;
        }

        CirclesPanel cp = new CirclesPanel("", failureMessage, cd,
                true);// do use colours
        cp.setScaleFactor(DIAG_SCALE);
        return cp;
    }

    private ConcreteDiagram getDiagram() throws CannotDrawException {
      	AbstractDescription ad = AbstractDescription.makeForTesting(zones, shadedZones); 
      	DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
        return cd;
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
    
    
    /*private void writeZonesFirstPass(OWLClass selectedClass, TreeSet<AbstractCurve> z) {
    	//strip quotes
    	String name = ren.render(selectedClass).replaceAll("'", "");
    	AbstractCurve ac = new AbstractCurve(new CurveLabel(name));
    	TreeSet<AbstractCurve> z2 = (TreeSet<AbstractCurve>)z.clone();
        z2.add(ac);
        zones_old.add(new AbstractBasicRegion(z2));
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: theProvider.getChildren(selectedClass)){
        	if (!(sub == null) && sub.isOWLNothing()) {
        		shadedZones.add(new AbstractBasicRegion(z2));
        	} else {
        		writeZonesFirstPass(sub, z2);
        	}
        }
    }*/
    
//	private Set getDisjoints(OWLReasoner reasoner, OWLClass cls) {
//	OWLDataFactory factory = reasoner.getRootOntology().getOWLOntologyManager().getOWLDataFactory();
//	OWLClassExpression complement = factory.getOWLObjectComplementOf(cls);
//	Set<OWLClass> equivalentToComplement = reasoner.getEquivalentClasses(complement).getEntities();
//	if(!equivalentToComplement.isEmpty()) {
//		return equivalentToComplement;
//	} else {
//		return reasoner.getSubClasses(complement, true).getFlattened();
//	}
//}
}
