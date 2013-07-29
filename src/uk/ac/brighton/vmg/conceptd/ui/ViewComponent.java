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
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.brighton.vmg.conceptd.syntax.AbstractDiagramBuilder2;
import uk.ac.brighton.vmg.conceptd.syntax.Zone;

//import uk.ac.brighton.vmg.conceptd.syntax.Zone;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JPanel cdPanel;
    private JComboBox<String> depthPicker;
    
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
		depthPicker = new JComboBox<String>(depths);
		depthPicker.setSelectedIndex(1);
		depthPicker.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            hierarchyDepth = Integer.parseInt((String) depthPicker.getSelectedItem());
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
		//DIAG_SIZE = Math.max(getHeight(), getWidth()) - 100;
		DIAG_SIZE = getHeight() - 50;
        if (selectedClass != null) {
        	/*AbstractDiagramBuilder builder = 
        			new AbstractDiagramBuilder(selectedClass, getOWLModelManager(), hierarchyDepth);
        	builder.build();
        	*/
        	AbstractDiagramBuilder2 builder2 = 
        			new AbstractDiagramBuilder2(selectedClass, getOWLModelManager(), hierarchyDepth);
        	builder2.build();
        	log.info("zones: "+builder2.getZones());
            drawCD(builder2.getZones(), builder2.getShadedZones());
        }
        return selectedClass;
	}
	
	private void drawCD(Set<Zone> z, Set<Zone> sz) {
		//log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel(z, sz));
	}
    
    
    private JPanel getCDPanel(Set<Zone> z, Set<Zone> sz) {
        //log.info("drawing diagram " + desc);
        Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC,  16);

        ConcreteDiagram cd = null;
        String failureMessage = null; 
        try {
            cd = getDiagram(z, sz);
            cd.setFont(font);
            CirclesPanel cp = new CirclesPanel("", failureMessage, cd,
                    true);// do use colours
            cp.setScaleFactor(DIAG_SCALE);
            return cp;
        } catch (CannotDrawException x) {
        	log.error(x.message);
            return new JPanel();
        }   
    }

    private ConcreteDiagram getDiagram(Set<Zone> zSet, Set<Zone> szSet) 
    		throws CannotDrawException {
    	ArrayList<ArrayList<String>> zs = new ArrayList<ArrayList<String>>();
    	for(Zone z: zSet) zs.add(new ArrayList<String>(z.getIn()));
    	ArrayList<ArrayList<String>> szs = new ArrayList<ArrayList<String>>(); 
    	for(Zone z: szSet) szs.add(new ArrayList<String>(z.getIn()));
      	AbstractDescription ad = AbstractDescription.makeForTesting(zs, szs); 
      	DiagramCreator dc = new DiagramCreator(ad);
        ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
        return cd;
    }

}
