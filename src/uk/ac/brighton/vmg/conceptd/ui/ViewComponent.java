package uk.ac.brighton.vmg.conceptd.ui;

import icircles.abstractDescription.AbstractBasicRegion;
import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.AbstractDescription;
import icircles.abstractDescription.CurveLabel;
import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;
import icircles.gui.CirclesPanel;
import icircles.util.CannotDrawException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

//import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    //private JTextArea namesComponent;
    private JPanel cdPanel;
    //private JTextArea zonesComponent;
    private ArrayList<AbstractBasicRegion> zones; 
    // convenience class for querying the asserted subsumption hierarchy directly
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    //private HashMap<String, Character> labelCharMap;
    //private char labelIndex;
    private int SIZE;
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BorderLayout(6, 6));
		
		//Dimension d = new Dimension(900, 450);
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
		add(cdPanel, BorderLayout.CENTER);
        //add(Box.createRigidArea(new Dimension(0,5)));
        //zonesComponent = new JTextArea();
        //zonesComponent.setMaximumSize(d);
        //zonesComponent.setMinimumSize(d);
        //JScrollPane p2 = new JScrollPane(zonesComponent);
        //p2.setMaximumSize(d);
        //p2.setMinimumSize(d);
        //add(p2);
        log.debug("CD View Component initialized");//
	}

	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		SIZE = Math.max(getHeight(), getWidth()) - 50;
		//zonesComponent.setText("");
        if (selectedClass != null){
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            ren = getOWLModelManager().getOWLEntityRenderer();
            //render(selectedClass, 0);
            StringBuffer zoneDesc = getZones(selectedClass);
            //if(labelCharMap.size()<27) {
            	//zonesComponent.setText(zoneDesc.toString());
            	drawCD();
            //} else {
            	//cdPanel.removeAll();
            	//zonesComponent.setText("TOO MANY LABELS: [" + labelCharMap.size() + "]\n");
            	//zonesComponent.append(zoneDesc.toString());
            //}
        }
        return selectedClass;
	}
	
	private void drawCD() {
		//String desc = zonesToDesc();
		//zonesComponent.append(desc);
		log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel(zones));
	}
	
	/*private String zonesToDesc() {
		StringBuffer sb = new StringBuffer();
		Iterator<AbstractBasicRegion> it = zones.iterator();
		AbstractBasicRegion z;
		Iterator<AbstractCurve> contours;
		AbstractCurve c;
		while (it.hasNext()) {
			z = it.next();
			contours = z.getContourIterator();
			while(contours.hasNext()) {
				c = contours.next();
				sb.append(c.getLabel().getLabel());
				sb.append(",");
			}
			sb.append(" ");
		}
		return sb.toString();
	}*/
	
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
    	//labelCharMap = new HashMap<String, Character>();
    	//labelIndex = 'a';
    	StringBuffer zoneDesc = new StringBuffer();
    	zones = writeZonesFirstPass(selectedClass, 
    			new ArrayList<AbstractBasicRegion>(), new TreeSet<AbstractCurve>());
    	//zones = removeDisconnectedZones(zones);
    	Iterator<AbstractBasicRegion> it = zones.iterator();
    	while(it.hasNext()) {
    		zoneDesc.append(it.next().toString());
    		zoneDesc.append("\n");
    	}
    	return zoneDesc;
    }
    
    private ArrayList<AbstractBasicRegion> writeZonesFirstPass(OWLClass selectedClass, 
    		ArrayList<AbstractBasicRegion> s, TreeSet<AbstractCurve> z) {
    	//strip quotes
    	String name = ren.render(selectedClass).replaceAll("'", "");
    	/*if(!labelCharMap.containsKey(name)) {
    		labelCharMap.put(name, new Character(labelIndex));
    		labelIndex++;
    	}*/
    	TreeSet<AbstractCurve> z2 = (TreeSet<AbstractCurve>)z.clone();
        z2.add(new AbstractCurve(CurveLabel.get(name)));
        s.add(AbstractBasicRegion.get(z2));
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
    
    /*private JPanel getCDPanel(String desc) {
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
    }*/
    private JPanel getCDPanel(ArrayList<AbstractBasicRegion> zones) {
        //log.info("drawing diagram " + desc);
        Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC,  16);

        ConcreteDiagram cd = null;
        String failureMessage = null;
        try {
            cd = getDiagram(zones, SIZE);
            cd.setFont(font);
        } catch (CannotDrawException x) {
            failureMessage = x.message;
        }

        CirclesPanel cp = new CirclesPanel("", failureMessage, cd,
                true);// do use colours
        cp.setScaleFactor(.9);
        return cp;
    }
    
    /*private ConcreteDiagram getDiagram(String desc,
            int size) throws CannotDrawException {
        AbstractDescription ad = AbstractDescription.makeForTesting(desc, false);
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(size);
        return cd;
    }*/
    private ConcreteDiagram getDiagram(ArrayList<AbstractBasicRegion> zones,
            int size) throws CannotDrawException {
        AbstractDescription ad = AbstractDescription.makeForTesting(zones);
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(size);
        return cd;
    }
}
