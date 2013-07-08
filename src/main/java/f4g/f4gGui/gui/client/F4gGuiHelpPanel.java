package f4g.f4gGui.gui.client;

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.LayoutPanel;

public class F4gGuiHelpPanel extends LayoutPanel {
	
	F4gGuiHelpPanel () {
		create();
	}
	
	private void create() {
	
		FlowPanel main = new FlowPanel();
	    main.setStyleName("panelTabContent");
	    this.add(main);
	    
	    Frame frame = new Frame("help/MainPage.html");
	    
	    frame.getElement().getStyle().setBorderWidth(0, Unit.PX);
	    frame.getElement().getStyle().setOverflow(Overflow.AUTO);
	    frame.getElement().getStyle().setWidth(100, Unit.PCT);
	    frame.getElement().getStyle().setHeight(100, Unit.PCT);
	    
	    main.add(frame);
	}
}
