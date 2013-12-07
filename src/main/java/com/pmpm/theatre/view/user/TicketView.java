package com.pmpm.theatre.view.user;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.model.Ticket;
import com.pmpm.theatre.model.TicketState;
import com.pmpm.theatre.model.User;
import com.pmpm.theatre.view.dashboard.DashboardUI;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pmpm.theatre.view.dashboard.DashboardUI.RESOURCE_BUNDLE;

public class TicketView extends Window {
    private JPAContainer<User> userJPAContainer;
    private JPAContainer<Show> showJPAContainer;
    private JPAContainer<Ticket> ticketJPAContainer;
    private EntityManager em;


    public TicketView(final Show show) {

        em = DashboardUI.emf.createEntityManager();
        userJPAContainer = JPAContainerFactory.make(User.class, em);
        showJPAContainer = JPAContainerFactory.make(Show.class, em);
        ticketJPAContainer = JPAContainerFactory.make(Ticket.class, em);

        VerticalLayout l = new VerticalLayout();
        l.setSpacing(true);

        setCaption(show.getName());
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

        final Image coverImage = new Image("", new ExternalResource(
                show.getImageUrl()));

        DragAndDropWrapper cover = new DragAndDropWrapper(coverImage);
        cover.setDragStartMode(DragStartMode.NONE);
        cover.setWidth("50%");
        cover.setHeight("270px");
        cover.addStyleName("cover");
        cover.setDropHandler(new DropHandler() {
            @Override
            public void drop(DragAndDropEvent event) {
                DragAndDropWrapper d = (DragAndDropWrapper) event
                        .getTransferable().getSourceComponent();
                if (d == event.getTargetDetails().getTarget())
                    return;
                Show m = (Show) d.getData();
                coverImage.setSource(new ExternalResource(m.getImageUrl()));
                coverImage.setAlternateText(m.getName());
                setCaption(m.getName());
            }

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }
        });
        details.addComponent(cover);

        FormLayout fields = new FormLayout();
        fields.setWidth("50%");
        fields.setSpacing(true);
        fields.setMargin(true);
        details.addComponent(fields);


        Label label = new Label(String.valueOf(show.getName()));
        label.setSizeUndefined();
        label.setCaption(RESOURCE_BUNDLE.getString("show.name"));
        fields.addComponent(label);


        label = new Label(String.valueOf(show.getTicketPrice()));
        label.setSizeUndefined();
        label.setCaption(RESOURCE_BUNDLE.getString("tickets.price"));
        fields.addComponent(label);

        final List<Ticket> availableTickets = showJPAContainer.getItem(show.getId()).getEntity().getAvailable();


        Set<Integer> rows = new HashSet<Integer>();
        for (Ticket ticket : availableTickets) {
            if (!TicketState.AVAILABLE.equals(ticket.getState())) continue;
            rows.add(ticket.getRow());
        }

        final ComboBox rowBox = new ComboBox(RESOURCE_BUNDLE.getString("row"), rows);
        rowBox.setValue(rows.iterator().next());
        rowBox.setNullSelectionAllowed(false);
        fields.addComponent(rowBox);
        final ComboBox seatBox = new ComboBox(RESOURCE_BUNDLE.getString("seat"));
        fields.addComponent(seatBox);

        rowBox.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                IndexedContainer container = new IndexedContainer();
                Integer sel = (Integer) event.getProperty().getValue();
                for (Ticket ticket : availableTickets) {
                    if (ticket.getRow() == sel) {
                        if (!TicketState.AVAILABLE.equals(ticket.getState())) continue;
                        container.addItem(ticket.getSeat());
                    }
                }
                seatBox.setContainerDataSource(container);
                seatBox.setVisible(true);
            }
        });
        rowBox.valueChange(new Field.ValueChangeEvent(rowBox));


        final ComboBox type = new ComboBox(RESOURCE_BUNDLE.getString("type"), Arrays.asList(new TicketState[]{TicketState.PURCHASE, TicketState.RESERVE}));
        type.setNullSelectionAllowed(false);
        type.setValue(TicketState.PURCHASE);
        fields.addComponent(type);

        final TextField cardNumber = new TextField();
        cardNumber.setSizeUndefined();
        cardNumber.setCaption(RESOURCE_BUNDLE.getString("card.number"));
        fields.addComponent(cardNumber);
        cardNumber.setVisible(false);

        rowBox.setTextInputAllowed(false);
        seatBox.setTextInputAllowed(false);

        HorizontalLayout footer = new HorizontalLayout();
        footer.addStyleName("footer");
        footer.setWidth("100%");
        footer.setMargin(true);

        final Button cancel = new Button(RESOURCE_BUNDLE.getString("cancel"));
        cancel.addStyleName("wide");
        cancel.addStyleName("default");
        cancel.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        final Button ok = new Button(RESOURCE_BUNDLE.getString("buy"));
        ok.addStyleName("wide");
        ok.addStyleName("default");
        ok.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {


                Integer row = (Integer) rowBox.getValue();
                Integer seat = (Integer) seatBox.getValue();
                TicketState state = (TicketState) type.getValue();
                if (row == null || row < 1) {
                    Notification.show(RESOURCE_BUNDLE.getString("error.show.row"), Notification.Type.ERROR_MESSAGE);
                    return;
                }

                if (seat == null || seat < 1) {
                    Notification.show(RESOURCE_BUNDLE.getString("error.show.seat"), Notification.Type.ERROR_MESSAGE);
                    return;
                }

                if (state.equals(TicketState.PURCHASE) && StringUtils.length(cardNumber.getValue()) != 16) {
                    Notification.show(RESOURCE_BUNDLE.getString("error.show.card"), Notification.Type.ERROR_MESSAGE);
                    return;
                }


                for (Ticket ticket : showJPAContainer.getItem(show.getId()).getEntity().getAvailable()) {
                    if (row == ticket.getRow() && seat == ticket.getSeat()) {
                        User user = userJPAContainer.getItem(((DashboardUI) getUI()).getUser().getId()).getEntity();
                        user.getTickets().add(ticket);
                        Show s = showJPAContainer.getItem(ticket.getShow().getId()).getEntity();
                        s.getAvailable().remove(ticket);
                        ticket.setState(state);
                        ticket.setOwner(user);

                        ticketJPAContainer.addEntity(ticket);

                        userJPAContainer.commit();
                        showJPAContainer.commit();
                        ticketJPAContainer.commit();
                        break;
                    }
                }
                close();
            }
        });


        type.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                TicketState state = (TicketState) event.getProperty().getValue();
                switch (state) {
                    case PURCHASE: {
                        cardNumber.setVisible(true);
                        ok.setCaption(RESOURCE_BUNDLE.getString("buy"));
                    }
                    break;
                    case RESERVE: {
                        cardNumber.setVisible(false);
                        ok.setCaption(RESOURCE_BUNDLE.getString("reserve"));

                    }
                }
            }
        });
        type.valueChange(new Field.ValueChangeEvent(type));
        footer.addComponent(cancel);
        footer.addComponent(ok);
        footer.setComponentAlignment(ok, Alignment.TOP_RIGHT);
        l.addComponent(footer);
    }


}
