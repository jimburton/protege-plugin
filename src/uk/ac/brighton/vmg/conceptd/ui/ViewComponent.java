package uk.ac.brighton.vmg.conceptd.ui;

import icircles.abstractDescription.AbstractDescription;
import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;
import icircles.gui.CirclesPanel;
import icircles.util.CannotDrawException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
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
    //private JTextArea namesComponent;
    private JPanel cdPanel;
    private JTextArea zonesComponent;
    private TreeSet<Zone> zones; 
    // convenience class for querying the asserted subsumption hierarchy directly
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    private HashMap<String, Character> labelCharMap;
    private char labelIndex;
    private int SIZE;
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
		Dimension d = new Dimension(900, 450);
		/*namesComponent = new JTextArea();
        namesComponent.setTabSize(2);
        namesComponent.setMaximumSize(d);
        namesComponent.setMinimumSize(d);
        JScrollPane p1 = new JScrollPane(namesComponent);
        p1.setMaximumSize(d);
        p1.setMinimumSize(d);
        add(p1);*/
		cdPanel = new JPanel();
		cdPanel.setBackground(Color.WHITE);
		add(cdPanel);
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
		SIZE = Math.max(getHeight(), getWidth()) - 50;
		zonesComponent.setText("");
        if (selectedClass != null){
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            ren = getOWLModelManager().getOWLEntityRenderer();
            //render(selectedClass, 0);
            StringBuffer zoneDesc = getZones(selectedClass);
            if(labelCharMap.size()<27) {
            	zonesComponent.setText(zoneDesc.toString());
            	drawCD();
            } else {
            	cdPanel.removeAll();
            	zonesComponent.setText("TOO MANY LABELS: [" + labelCharMap.size() + "]\n");
            	zonesComponent.append(zoneDesc.toString());
            }
        }
        return selectedClass;
	}
	
	private void drawCD() {
		String desc = zonesToDesc();
		zonesComponent.append(desc);
		log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel(desc));
	}
	
	private String zonesToDesc() {
		StringBuffer sb = new StringBuffer();
		Iterator<Zone> it = zones.iterator();
		Zone z;
		String[] labels;
		while (it.hasNext()) {
			z = it.next();
			labels = z.getLabelsArray();
			for(int i=0;i<labels.length;i++) {
				sb.append(labelCharMap.get(labels[i]).charValue());
			}
			sb.append(" ");
		}
		return sb.toString();
	}
	
	// write tab-indented name of class and recursively all of its subclasses
    /*private void render(OWLClass selectedClass, int indent) {
        for (int i=0; i<indent; i++){
            namesComponent.append("\t");
        }
        namesComponent.append(ren.render(selectedClass));
        namesComponent.append("\n");
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            render(sub, indent+1);
        }
    }*/
    
    //  class and recursively all of its subclasses
    private StringBuffer getZones(OWLClass selectedClass) {
    	labelCharMap = new HashMap<String, Character>();
    	labelIndex = 'a';
    	StringBuffer zoneDesc = new StringBuffer();
    	zones = writeZonesFirstPass(selectedClass, 
    			new TreeSet<Zone>(), new Zone());
    	//zones = removeDisconnectedZones(zones);
    	Iterator<Zone> it = zones.iterator();
    	while(it.hasNext()) {
    		zoneDesc.append(it.next().toString());
    		zoneDesc.append("\n");
    	}
    	return zoneDesc;
    }
    
    private TreeSet<Zone> writeZonesFirstPass(OWLClass selectedClass, 
    		TreeSet<Zone> s, Zone z) {
    	String name = ren.render(selectedClass);
    	if(!labelCharMap.containsKey(name)) {
    		labelCharMap.put(name, new Character(labelIndex));
    		labelIndex++;
    	}
    	Zone z2 = z.clone();
        z2.add(name);
        s.add(z2);
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            s.addAll(writeZonesFirstPass(sub, s, z2));
        }
        return s;
    }
    
   /* private TreeSet<Zone> removeDisconnectedZones(TreeSet<Zone> zones) {
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
    }*/
    ////////////////////////////////
    // iCircles test
    ////////////////////////////////
    
    private JPanel getCDPanel(String desc) {
        log.info("drawing diagram " + desc);
        Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC,  16);

        ConcreteDiagram cd = null;
        String failureMessage = null;
        try {
            cd = getDiagram(desc, SIZE);
            cd.setFont(font);
        } catch (CannotDrawException x) {
            failureMessage = x.message;
        }

        CirclesPanel cp = new CirclesPanel(desc, failureMessage, cd,
                true);// do use colours
        cp.setScaleFactor(.9);
        return cp;
    }
    
    private ConcreteDiagram getDiagram(String desc,
            int size) throws CannotDrawException {
        AbstractDescription ad = AbstractDescription.makeForTesting(desc, false);
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(size);
        return cd;
    }
}
