package com.pmpm.theatre.view.dashboard;

import com.pmpm.theatre.model.Ticket;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.*;
import org.apache.commons.lang.time.DateUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.pmpm.theatre.view.dashboard.DashboardUI.RESOURCE_BUNDLE;


public class DashboardView extends VerticalLayout implements View {

    private TextArea notes;


    public DashboardView() {
        setSizeFull();
        addStyleName("dashboard-view");

        HorizontalLayout top = new HorizontalLayout();
        top.setWidth("100%");
        top.setSpacing(true);
        top.addStyleName("toolbar");
        addComponent(top);
        final Label title = new Label(RESOURCE_BUNDLE.getString("dashboard.title"));
        title.setSizeUndefined();
        title.addStyleName("h1");
        top.addComponent(title);
        top.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
        top.setExpandRatio(title, 1);

        notes = new TextArea(RESOURCE_BUNDLE.getString("dashboard.notes"));
        notes.setSizeFull();
        CssLayout panel = createPanel(notes);
        panel.addStyleName("notes");
        addComponent(panel);
        setExpandRatio(panel, 2);


    }

    private CssLayout createPanel(Component content) {
        CssLayout panel = new CssLayout();
        panel.addStyleName("layout-panel");
        panel.setSizeFull();

        Button configure = new Button();
        configure.addStyleName("configure");
        configure.addStyleName("icon-cog");
        configure.addStyleName("icon-only");
        configure.addStyleName("borderless");
        configure.setDescription("Configure");
        configure.addStyleName("small");
        panel.addComponent(configure);

        panel.addComponent(content);
        return panel;
    }

    @Override
    public void enter(ViewChangeEvent event) {
        StringBuilder builder = new StringBuilder();
        EntityManager em = ((DashboardUI) getUI()).getEm();

        Query query = em.createQuery("select t from Ticket t where t.owner = :user and t.show.start > :before  and  t.show.start  < :after");
        query.setParameter("user", ((DashboardUI) getUI()).getUser());
        query.setParameter("before", DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
        query.setParameter("after", DateUtils.addDays(new Date(), 1));
        List<Ticket> tasks = query.getResultList();

        builder.append(String.format(RESOURCE_BUNDLE.getString("dashboard.tasks"), tasks.size()));
        builder.append("\n");

        for (Ticket ticket : tasks) {
            builder.append(String.format(RESOURCE_BUNDLE.getString("dashboard.task.description"), ticket.getShow().getName()));
            builder.append("\n");

            builder.append(String.format(RESOURCE_BUNDLE.getString("dashboard.task.time"), ticket.getShow().getStart()));
            builder.append("\n");
            builder.append("\n");

            builder.append(String.format(RESOURCE_BUNDLE.getString("dashboard.task.price"), String.valueOf(ticket.getShow().getTicketPrice())));
            builder.append("\n");
            builder.append("\n");

            System.out.println(String.valueOf(ticket.getShow().getTicketPrice()));

            builder.append(String.format(RESOURCE_BUNDLE.getString("dashboard.task.type") + " "));
            builder.append(String.format(RESOURCE_BUNDLE.getString(ticket.getState().name())));
            builder.append("\n");
            builder.append("\n");
        }
        builder.append("\n");
        builder.append("\n");
        notes.setValue(builder.toString());
    }


}
