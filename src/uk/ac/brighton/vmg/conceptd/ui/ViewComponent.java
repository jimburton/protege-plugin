package uk.ac.brighton.vmg.conceptd.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JTextArea namesComponent;
    private JTextArea zonesComponent;
    // convenience class for querying the asserted subsumption hierarchy directly
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    private TreeSet<String> labels;
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // in our implementation, just create a simple text area in a scrollpane
        namesComponent = new JTextArea();
        namesComponent.setTabSize(2);
        Dimension d = new Dimension(900, 450);
        namesComponent.setMaximumSize(d);
        namesComponent.setMinimumSize(d);
        JScrollPane p1 = new JScrollPane(namesComponent);
        p1.setMaximumSize(d);
        p1.setMinimumSize(d);
        add(p1);
        add(Box.createRigidArea(new Dimension(0,5)));
        zonesComponent = new JTextArea();
        zonesComponent.setMaximumSize(d);
        zonesComponent.setMinimumSize(d);
        JScrollPane p2 = new JScrollPane(zonesComponent);
        p2.setMaximumSize(d);
        p2.setMinimumSize(d);
        add(p2);
        log.debug("CD View Component initialized");//
	}

	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		namesComponent.setText("");
		zonesComponent.setText("");
        if (selectedClass != null){
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            ren = getOWLModelManager().getOWLEntityRenderer();
            render(selectedClass, 0);
            writeZones(selectedClass);
        }
        return selectedClass;
	}
	
	// render the class and recursively all of its subclasses
    private void render(OWLClass selectedClass, int indent) {
        for (int i=0; i<indent; i++){
            namesComponent.append("\t");
        }
        namesComponent.append(ren.render(selectedClass));
        namesComponent.append("\n");
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            render(sub, indent+1);
        }
    }
    
    // render the class and recursively all of its subclasses
    private void writeZones(OWLClass selectedClass) {
    	labels = new TreeSet<String>();
    	TreeSet<Zone> zones = writeZonesFirstPass(selectedClass, new TreeSet<Zone>(), new Zone());
    	//zones = removeDisconnectedZones(zones);
    	Iterator<Zone> it = zones.iterator();
    	while(it.hasNext()) {
    		zonesComponent.append(it.next().toString());
    		zonesComponent.append("\n");
    	}
    }
    
    private TreeSet<Zone> writeZonesFirstPass(OWLClass selectedClass, TreeSet<Zone> s, Zone z) {
    	String name = ren.render(selectedClass);
    	labels.add(name);
    	Zone z2 = z.clone();
        z2.add(name);
        s.add(z2);
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            s.addAll(writeZonesFirstPass(sub, s, z2));
        }
        return s;
    }
    
    private TreeSet<Zone> removeDisconnectedZones(TreeSet<Zone> zones) {
    	TreeSet<Zone> result = (TreeSet<Zone>)zones.clone();
    	Iterator<Zone> it = zones.iterator();
    	Zone z1,z2;
    	while(it.hasNext()) {
    		z1 = it.next();
    		Iterator<Zone> it2 = zones.iterator();
    		while(it2.hasNext()) {
    			z2 = it2.next();
    			if(!z1.equals(z2)) {
    				ArrayList<String> intersect = z1.intersect(z2);
    				for(String s: intersect) {
    					Zone miss = (z1.sub(s)).union(z2.sub(s));
    					if(!zones.contains(miss)) {
    						result.remove(z1);
    						result.remove(z2);
    						result.add(miss);
    						result.add(miss.addGetZone(s));
    					}
    				}
    			}
    		}
    	}
    	return result;
    }
}
