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
    private JPanel cdPanel;
    private ArrayList<AbstractBasicRegion> zones; 
    private ArrayList<String> rawZones;
    // convenience class for querying the asserted subsumption hierarchy directly
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    private int DIAG_SIZE;
    private final double DIAG_SCALE = 0.9;
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BorderLayout(6, 6));

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
            ren = getOWLModelManager().getOWLEntityRenderer();
            getZones(selectedClass);
            System.out.println(rawZones);
            drawCD();
        }
        return selectedClass;
	}
	
	private void drawCD() {
		log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel());
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
    
    //  get zone for class and recursively all of its subclasses
    private void getZones(OWLClass selectedClass) {
    	zones = new ArrayList<AbstractBasicRegion>();
    	rawZones = new ArrayList<String>();
    	writeZonesFirstPass(selectedClass, new TreeSet<AbstractCurve>());
    	writeRawZones(selectedClass, new StringBuffer());
    	//zones = removeDisconnectedZones(zones);
    	/*Iterator<AbstractBasicRegion> it = zones.iterator();
    	while(it.hasNext()) {
    		zoneDesc.append(it.next().toString());
    		zoneDesc.append("\n");
    	}
    	return zoneDesc;*/
    	/*rawZones = new ArrayList<String>();
    	writeZoneString(selectedClass, new StringBuffer());*/
    	//rawZones = new ArrayList<String>();
    	//writeRawZones(selectedClass, new StringBuffer());
    	//rawZonesToAbstractRegions();
    }
    
    private void rawZonesToAbstractRegions() {
    	
		for(String s: rawZones) {
			String[] zs = s.split(":");
			TreeSet<AbstractCurve> z = new TreeSet<AbstractCurve>();
			for(int i=0;i<zs.length;i++) {
				if(zs[i].length()> 0)
					z.add(new AbstractCurve(CurveLabel.get(zs[i])));
			}
			zones.add(AbstractBasicRegion.get(z));
		}
	}

	private void writeRawZones(OWLClass selectedClass, StringBuffer prefix) {
    	StringBuffer zone = new StringBuffer(prefix);
    	zone.append(":").append(ren.render(selectedClass).replaceAll("'", ""));
    	rawZones.add(zone.toString());
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            writeRawZones(sub, zone);
        }
	}

	/*private ArrayList<AbstractBasicRegion> writeZonesFirstPass(OWLClass selectedClass, 
    		ArrayList<AbstractBasicRegion> s, TreeSet<AbstractCurve> z) {
    	//strip quotes
    	String name = ren.render(selectedClass).replaceAll("'", "");
    	TreeSet<AbstractCurve> z2 = (TreeSet<AbstractCurve>)z.clone();
        z2.add(new AbstractCurve(CurveLabel.get(name)));
        s.add(AbstractBasicRegion.get(z2));
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            s.addAll(writeZonesFirstPass(sub, s, z2));
        }
        return s;
    }*/
    private void writeZonesFirstPass(OWLClass selectedClass, TreeSet<AbstractCurve> z) {
    	//strip quotes
    	String name = ren.render(selectedClass).replaceAll("'", "");
    	TreeSet<AbstractCurve> z2 = (TreeSet<AbstractCurve>)z.clone();
        z2.add(new AbstractCurve(CurveLabel.get(name)));
        zones.add(AbstractBasicRegion.get(z2));
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            writeZonesFirstPass(sub, z2);
        }
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
        AbstractDescription ad = AbstractDescription.makeForTesting(zones);
        DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
        return cd;
    }
}
