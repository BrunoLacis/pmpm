package com.pmpm.theatre.view.dashboard;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

public class DashboardUIProvider extends UIProvider {

    @Override
    public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
        if (event.getRequest().getParameter("mobile") != null
                && event.getRequest().getParameter("mobile").equals("false")) {
            return DashboardUI.class;
        }

        return DashboardUI.class;
    }
}