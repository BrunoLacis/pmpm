package com.pmpm.theatre.view.dashboard;

import com.vaadin.navigator.View;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kristaps Kohs
 */
public class HelpManager {

    private DashboardUI ui;
    private List<HelpOverlay> overlays = new ArrayList<HelpOverlay>();

    public HelpManager(DashboardUI ui) {
        this.ui = ui;
    }

    public void closeAll() {
        for (HelpOverlay overlay : overlays) {
            overlay.close();
        }
        overlays.clear();
    }

    public void showHelpFor(View view) {
    }

    public void showHelpFor(Class<? extends View> view) {

    }

    protected HelpOverlay addOverlay(String caption, String text, String style) {
        HelpOverlay o = new HelpOverlay();
        o.setCaption(caption);
        o.addComponent(new Label(text, ContentMode.HTML));
        o.setStyleName(style);
        overlays.add(o);
        return o;
    }

}