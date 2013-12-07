package com.pmpm.theatre.view.admin;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.model.Ticket;
import com.pmpm.theatre.model.User;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Calendar;
import org.apache.commons.lang.StringUtils;
import org.vaadin.risto.stepper.FloatStepper;
import org.vaadin.risto.stepper.IntStepper;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.pmpm.theatre.view.dashboard.DashboardUI.RESOURCE_BUNDLE;

public class AddShowView extends Window {

    Label synopsis = new Label();

    private JPAContainer<Show> showJPAContainer;
    private JPAContainer<Ticket> ticketJPAContainer;
    private Table table;

    public AddShowView(final Table table, JPAContainer<Show> showJPAContainer, JPAContainer<Ticket> ticketJPAContainer) {
        this.showJPAContainer = showJPAContainer;
        this.ticketJPAContainer = ticketJPAContainer;
        this.table = table;
        VerticalLayout l = new VerticalLayout();
        l.setSpacing(true);


        setContent(l);
        center();
        setCloseShortcut(KeyCode.ESCAPE, null);
        setResizable(false);
        setClosable(false);

        addStyleName("no-vertical-drag-hints");
        addStyleName("no-horizontal-drag-hints");

        HorizontalLayout details = new HorizontalLayout();
        details.setSpacing(true);
        details.setMargin(true);
        l.addComponent(details);

        FormLayout fields = new FormLayout();
        fields.setWidth("35em");
        fields.setSpacing(true);
        fields.setMargin(true);

        details.addComponent(fields);

        Label label;

        final TextField name = new TextField(RESOURCE_BUNDLE.getString("show.name"));
        fields.addComponent(name);

        final TextArea description = new TextArea(RESOURCE_BUNDLE.getString("show.description"));

        fields.addComponent(description);


        final TextField image = new TextField(RESOURCE_BUNDLE.getString("show.image"));
        fields.addComponent(image);

        final FloatStepper price = new FloatStepper(RESOURCE_BUNDLE.getString("tickets.price"));
        fields.addComponent(price);

        final IntStepper rows = new IntStepper(RESOURCE_BUNDLE.getString("show.rows"));
        fields.addComponent(rows);


        final IntStepper seats = new IntStepper(RESOURCE_BUNDLE.getString("show.seats"));
        fields.addComponent(seats);


        final PopupDateField dateField = new PopupDateField();
        dateField.setResolution(Resolution.MINUTE);
        dateField.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateField.setDateFormat("yyyy-MM-dd hh:mm");
        dateField.setValue(new Date());
        fields.addComponent(dateField);

        final PopupDateField endDate = new PopupDateField();
        endDate.setValue(new Date());
        endDate.setImmediate(true);
        endDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        endDate.setDateFormat("yyyy-MM-dd hh:mm");
        java.util.Calendar ca = java.util.Calendar.getInstance();
        ca.add(java.util.Calendar.HOUR_OF_DAY , 2);
        endDate.setResolution(Resolution.MINUTE);
        endDate.setValue(ca.getTime());
        fields.addComponent(endDate);





        HorizontalLayout footer = new HorizontalLayout();
        footer.addStyleName("footer");
        footer.setWidth("100%");
        footer.setMargin(true);

        final Button button = new Button(RESOURCE_BUNDLE.getString("add"));
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                try {


                    Show show = new Show();
                    show.setDescription(description.getValue());
                    show.setImageUrl(image.getValue());
                    show.setName(name.getValue());
                    show.setStart(dateField.getValue());
                    show.setTicketPrice(price.getValue());
                    show.setEnd(endDate.getValue());

                    if(StringUtils.isBlank(show.getImageUrl())){
                        show.setImageUrl("http://t0.gstatic.com/images?q=tbn:ANd9GcT-v_g6TREfwbLwV58R_TuEMPjZVd4Qg3gp56TapxjqkRo-hKGipQ");
                    }

                    if (StringUtils.isBlank(show.getDescription())) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.task.error.description"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    if (dateField.getValue() == null) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.task.error.date"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    if (endDate.getValue() == null) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.task.error.date"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }

                    if (price.getValue() == null || show.getTicketPrice() == 0) {
                        Notification.show(RESOURCE_BUNDLE.getString("error.show.price"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }

                    if (rows.getValue() == null || rows.getValue() < 1) {
                        Notification.show(RESOURCE_BUNDLE.getString("error.show.row"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }

                    if (seats.getValue() == null || seats.getValue() < 1) {
                        Notification.show(RESOURCE_BUNDLE.getString("error.show.seat"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    Long id = (Long) AddShowView.this.showJPAContainer.addEntity(show);
                    AddShowView.this.showJPAContainer.commit();

                    Show added = AddShowView.this.showJPAContainer.getItem(id).getEntity();

                    for (int row = 1; row <= rows.getValue(); row++) {
                        for (int seat = 1; seat <= seats.getValue(); seat++) {
                            final Ticket ticket = new Ticket();
                            ticket.setPrice(price.getValue());
                            ticket.setRow(row);
                            ticket.setSeat(seat);
                            ticket.setShow(added);

                            added.getAvailable().add(
                                    AddShowView.this.ticketJPAContainer.getItem(
                                            AddShowView.this.ticketJPAContainer.addEntity(ticket)).getEntity());

                        }
                    }

                    AddShowView.this.ticketJPAContainer.commit();
                    AddShowView.this.showJPAContainer.addEntity(show);
                    AddShowView.this.showJPAContainer.commit();
                    table.refreshRowCache();
                } finally {
                    AddShowView.this.close();
                }
            }
        });
        footer.addComponent(button);
        footer.setComponentAlignment(button, Alignment.TOP_RIGHT);
        l.addComponent(footer);
    }
}
