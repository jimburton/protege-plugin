package uk.ac.brighton.vmg.conceptd.ui;

import icircles.abstractDescription.AbstractBasicRegion;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

//import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JPanel cdPanel;
    private JComboBox depthPicker;
    
    private ArrayList<ArrayList<String>> rawZones;
    private ArrayList<ArrayList<String>> shadedZones;
    private ArrayList<String> rawClasses;
    private ArrayList<OWLClass> classes;
    private String outerCurve;
    private int hierarchyDepth = 3;
    private final String EMPTY_LABEL = "Nothing";
    
    // convenience class for querying the asserted subsumption hierarchy directly
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
    private MODEL theModel = MODEL.INFERRED;
    //private MODEL theModel = MODEL.ASSERTED;
    
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

        log.debug("CD View Component initialized");//
	}

	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		DIAG_SIZE = Math.max(getHeight(), getWidth()) - 50;
        if (selectedClass != null){
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
        	inferredHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getInferredOWLClassHierarchyProvider();
        	theProvider = (theModel == MODEL.INFERRED ? inferredHierarchyProvider : assertedHierarchyProvider);
        	theReasoner = getOWLModelManager().getOWLReasonerManager().getCurrentReasoner();
            ren = getOWLModelManager().getOWLEntityRenderer();
            getZones(selectedClass);
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
    private void getZones(OWLClass selectedClass) {
    	outerCurve = ren.render(selectedClass).replaceAll("'", "");
    	shadedZones = new ArrayList<ArrayList<String>>();
    	rawClasses = new ArrayList<String>();
    	rawZones = new ArrayList<ArrayList<String>>();
    	classes = new ArrayList<OWLClass>();
    	
    	getClasses(selectedClass, hierarchyDepth);
    	log.info(rawClasses);
    	//get the Venn diagram 
    	ArrayList<ArrayList<String>> p0 = powerSet(rawClasses);
    	ArrayList<ArrayList<String>> p = (ArrayList<ArrayList<String>>)p0.clone();
    	//remove zones that don't contain the outer curve
    	for(ArrayList<String> z : p0) {
    		if(!(z.contains(outerCurve) || z.contains(outerCurve+"+"))) {
    			p.remove(z);
    		} 
    	}
    	//log.info("Venn:");
    	//log.info(p);
    	//store the disjointness info in a table: Label->ArrayList of disjoint labels
    	HashMap<String, ArrayList<String>> table = new HashMap<String, ArrayList<String>>();
    	ArrayList<String> v;
    	//we only need to gather disjointness info for n-1 classes, where n is number of curves 
    	//inside the outer curve
    	OWLClass c;
    	theReasoner.flush();
    	for(int i=1;i<classes.size()-1;i++) {
    		c = classes.get(i);
    		NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(c);//or use TR's method
    		v = new ArrayList<String>();
    		Iterator<Node<OWLClass>> it = ds.iterator();
    		OWLClass cls;
    		String nm;
    		while(it.hasNext()) {
    			cls = it.next().getRepresentativeElement();
    			nm = ren.render(cls).replaceAll("'", "");
    			if(rawClasses.contains(nm)) {
    				v.add(nm);
    			} else if(rawClasses.contains(nm+"+")) {
    				v.add(nm+"+");
    			}
    		}
    		table.put(ren.render(c).replaceAll("'", ""), v);
    	}
    	
    	//remove missing regions from diagram
    	ArrayList<ArrayList<String>> p2 = (ArrayList<ArrayList<String>>)p.clone();
    	
    	for(String rc: rawClasses) {
    		String realName = rc.replaceFirst("\\+$", "");
    		if(table.containsKey(realName)) {
    			for(String d: table.get(realName)) {
    				for(ArrayList<String> ls : p) {
    					log.info("Removing: "+ls);
    					//remove missing zones
    					if(ls.contains(rc) && ls.contains(d)) {
    						//log.info("de-Venn: removing zone with "+rc+" and "+d);
    						p2.remove(ls);
    					} 
    				}
    				//p = (ArrayList<ArrayList<String>>)p2.clone();
    			}
    		}
    	}
    	ArrayList<ArrayList<String>> p3 = (ArrayList<ArrayList<String>>)p2.clone();
    	for(ArrayList<String> zone: p2) {
    		if(zone.contains(EMPTY_LABEL)) {
    			p3.remove(zone);
    			ArrayList<String> sz = (ArrayList<String>)zone;
    			sz.remove(EMPTY_LABEL);
    			shadedZones.add(sz);
    		}
    	}
    	rawZones = p3;
    }
    
    /**
     * Collect the list of curve labels in the diagram
     * @param selectedClass
     */
    private void getClasses(OWLClass selectedClass, int depth) {
    	switch (depth) {
    	  case 0:
    		  break;
    	  case 1:
    		  classes.add(selectedClass);
    		  String nm = ren.render(selectedClass).replaceAll("'", "");
    		  if(theProvider.getChildren(selectedClass).size()>0) nm += "+";
    	      rawClasses.add(nm);
    	      break;
    	  default:	
    		classes.add(selectedClass);
  	    	rawClasses.add(ren.render(selectedClass).replaceAll("'", ""));
  	    	int newDepth = --depth;
  	        for (OWLClass sub: theProvider.getChildren(selectedClass)){
  	          getClasses(sub, newDepth);
  	        }
    	}
    }

    
    /*private void rawZonesToAbstractRegions() {
    	zones = new ArrayList<AbstractBasicRegion>();
    	shadedZones = new ArrayList<AbstractBasicRegion>();
    	//Collections.reverse(rawZones);//TODO remove
    	TreeSet<AbstractCurve> z;
    	for(ArrayList<String> zIn: rawZones) {
			z = new TreeSet<AbstractCurve>();
			for(String label: zIn) {
				if(label.length()>0) {
					z.add(new AbstractCurve(new CurveLabel(label)));
				}
			}
			zones.add(new AbstractBasicRegion(z));
		}
	}*/
    
    private String rawZonesToStringOfChars() {
    	StringBuilder s = new StringBuilder();
    	char c = 'a';
    	HashMap<String, String> lookup = new HashMap<String, String>(); 
    	for(String l: rawClasses) {
    		lookup.put(l, c+"");
    		c++;
    	}
    	for(ArrayList<String> zIn: rawZones) {
			for(String label: zIn) {
				s.append(lookup.get(label));
			}
			s.append(" ");
		}
    	return s.toString();
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
    
    /**
     * Take the powerset of a list
     * @param originalSet
     * @return
     */
    private <T> ArrayList<ArrayList<T>> powerSet(List<T> originalSet) {
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
        //AbstractDescription ad = AbstractDescription.makeForTesting(zones_old, shadedZones);
    	//String s = rawZonesToString();
    	log.info("zones: "+rawZones);
    	log.info("shadedZones: "+shadedZones);
    	AbstractDescription ad = AbstractDescription.makeForTesting(rawZones, shadedZones); 
        //log.info(ad.debug());
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
        return cd;
    }
    
    private void debugZones(ArrayList<AbstractBasicRegion> zones) {
    	log.info("############ zones ##############");
    	for(AbstractBasicRegion abr: zones) {
    		log.info(abr.journalString());
    	}
    	log.info("#################################");
    }
    
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
