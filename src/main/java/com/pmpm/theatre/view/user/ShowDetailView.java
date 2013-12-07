package com.pmpm.theatre.view.user;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.view.dashboard.DashboardUI;
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

public class ShowDetailView extends Window {

    Label synopsis = new Label();

    public ShowDetailView(final Show show, ShowView.ShowEvent event) {
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

        final Button more = new Button("More…");

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
                updateSynopsis(m, false);
                more.setVisible(true);
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

        Label label;
        if (event != null) {
            SimpleDateFormat df = new SimpleDateFormat();

            df.applyPattern("dd-mm-yyyy");
            label = new Label(df.format(event.start));
            label.setSizeUndefined();
            label.setCaption("Date");
            fields.addComponent(label);

            df.applyPattern("hh:mm a");
            label = new Label(df.format(event.start));
            label.setSizeUndefined();
            label.setCaption("Starts");
            fields.addComponent(label);

            label = new Label(df.format(event.end));
            label.setSizeUndefined();
            label.setCaption("Ends");
            fields.addComponent(label);
        }

        label = new Label(getDateDiff(show.getStart(), show.getEnd(), TimeUnit.MINUTES)+ " minutes");
        label.setSizeUndefined();
        label.setCaption("Duration");
        fields.addComponent(label);

        synopsis.setData(show.getDescription());
        synopsis.setCaption("Synopsis");
        updateSynopsis(show, false);
        fields.addComponent(synopsis);

        more.addStyleName("link");
        fields.addComponent(more);
        more.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                updateSynopsis(null, true);
                event.getButton().setVisible(false);
            }
        });
        
        HorizontalLayout footer = new HorizontalLayout();
        footer.addStyleName("footer");
        footer.setWidth("100%");
        footer.setMargin(true);

        Button ok = new Button("Close");
        ok.addStyleName("wide");
        ok.addStyleName("default");
        ok.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        Button buy = new Button("Buy");
        buy.addStyleName("wide");
        buy.addStyleName("default");
        buy.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
                Window w = new TicketView(show);
                UI.getCurrent().addWindow(w);
                w.focus();
            }
        });

        footer.addComponent(buy);
        footer.addComponent(ok);
        footer.setComponentAlignment(ok, Alignment.TOP_RIGHT);
        l.addComponent(footer);
    }

    public void updateSynopsis(Show m, boolean expand) {
        String synopsisText = synopsis.getData().toString();
        if (m != null) {
            synopsisText = m.getDescription();
            synopsis.setData(m.getDescription());
        }
        if (!expand) {
            synopsisText = synopsisText.length() > 300 ? synopsisText
                    .substring(0, 300) + "…" : synopsisText;

        }
        synopsis.setValue(synopsisText);
    }
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
}
