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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.brighton.vmg.conceptd.syntax.AbstractDiagramBuilder;

//import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JPanel cdPanel;
    private JComboBox depthPicker;
    
    private int DIAG_SIZE;
    private final double DIAG_SCALE = 0.9;
    private int hierarchyDepth = 2;
    
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
		depthPicker.setSelectedIndex(1);
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
        if (selectedClass != null) {
        	AbstractDiagramBuilder builder = 
        			new AbstractDiagramBuilder(selectedClass, getOWLModelManager(), hierarchyDepth);
        	builder.build();
        	log.info("zones: "+builder.getZones());
            drawCD(builder.getZones(), builder.getShadedZones());
        }
        return selectedClass;
	}
	
	private void drawCD(ArrayList<ArrayList<String>> z, ArrayList<ArrayList<String>> sz) {
		//log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel(z, sz));
	}
    
    
    private JPanel getCDPanel(ArrayList<ArrayList<String>> z, ArrayList<ArrayList<String>> sz) {
        //log.info("drawing diagram " + desc);
        Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC,  16);

        ConcreteDiagram cd = null;
        String failureMessage = null; 
        try {
            cd = getDiagram(z, sz);
            cd.setFont(font);
        } catch (CannotDrawException x) {
            failureMessage = x.message;
        }

        CirclesPanel cp = new CirclesPanel("", failureMessage, cd,
                true);// do use colours
        cp.setScaleFactor(DIAG_SCALE);
        return cp;
    }

    private ConcreteDiagram getDiagram(ArrayList<ArrayList<String>> z, ArrayList<ArrayList<String>> sz) 
    		throws CannotDrawException {
      	AbstractDescription ad = AbstractDescription.makeForTesting(z, sz); 
      	DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
        return cd;
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
