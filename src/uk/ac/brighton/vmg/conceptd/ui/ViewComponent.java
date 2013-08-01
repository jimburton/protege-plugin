package uk.ac.brighton.vmg.conceptd.ui;

import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;
import icircles.gui.CirclesPanel;
import icircles.input.AbstractDiagram;
import icircles.input.Spider;
import icircles.util.CannotDrawException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
	private JComboBox<String> depthPicker;
	private OWLClass theSelectedClass;

	private int DIAG_SIZE;
	private final double DIAG_SCALE = 0.9;
	private int hierarchyDepth = 2;

	private static final Logger log = Logger.getLogger(ViewComponent.class);
	private static final int IC_VERSION = 1;

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
				hierarchyDepth = Integer.parseInt((String) depthPicker
						.getSelectedItem());
				if (theSelectedClass != null)
					updateView(theSelectedClass);
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
		// DIAG_SIZE = Math.max(getHeight(), getWidth()) - 100;
		theSelectedClass = selectedClass;
		DIAG_SIZE = getHeight() - 50;
		if (selectedClass != null) {
			/*
			 * AbstractDiagramBuilder builder = new
			 * AbstractDiagramBuilder(selectedClass, getOWLModelManager(),
			 * hierarchyDepth); builder.build();
			 */
			AbstractDiagramBuilder builder2 = new AbstractDiagramBuilder(
					selectedClass, getOWLModelManager(), hierarchyDepth);
			try {
				builder2.build();
			} catch (CannotDrawException e) {
				log.info("Too many curves to draw");
				return selectedClass;
			}
			String[] cs = builder2.getCurves();
			icircles.input.Zone[] zs = builder2.getZones();
			icircles.input.Zone[] szs = builder2.getShadedZones();
			debug("Curves", cs);
			debug("Zones", zs);
			debug("Shaded zones", szs);
			drawCD(cs, zs, szs);
		}
		return selectedClass;
	}

	private void drawCD(String[] c, icircles.input.Zone[] z,
			icircles.input.Zone[] sz) {
		// log.info("drawing CD");
		cdPanel.removeAll();
		cdPanel.add(getCDPanel(c, z, sz));
	}

	private JPanel getCDPanel(String[] c, icircles.input.Zone[] z,
			icircles.input.Zone[] sz) {
		// log.info("drawing diagram " + desc);
		Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC, 16);

		ConcreteDiagram cd = null;
		String failureMessage = null;
		try {
			cd = getDiagram(c, z, sz);
			cd.setFont(font);
			CirclesPanel cp = new CirclesPanel("", failureMessage, cd, true);// do
																				// use
																				// colours
			cp.setScaleFactor(DIAG_SCALE);
			return cp;
		} catch (CannotDrawException x) {
			log.error(x.message);
			return new JPanel();
		}
	}

	private ConcreteDiagram getDiagram(String[] c, icircles.input.Zone[] z,
			icircles.input.Zone[] sz) throws CannotDrawException {
		AbstractDiagram ad = new AbstractDiagram(IC_VERSION, c, z, sz,
				new Spider[] {});
		DiagramCreator dc = new DiagramCreator(ad.toAbstractDescription());
		ConcreteDiagram cd = dc.createDiagram(DIAG_SIZE);
		return cd;
	}

	private <T> void debug(String name, Object[] xs) {
		log.info("::::::::::   " + name + "   ::::::::::");
		for (Object x : xs)
			log.info(x);
	}

}
