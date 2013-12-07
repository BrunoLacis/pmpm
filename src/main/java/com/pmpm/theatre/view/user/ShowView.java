package com.pmpm.theatre.view.user;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.view.dashboard.DashboardUI;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.components.calendar.CalendarComponentEvents.*;
import com.vaadin.ui.components.calendar.event.CalendarEvent;
import com.vaadin.ui.components.calendar.event.CalendarEventProvider;
import com.vaadin.ui.components.calendar.handler.BasicEventMoveHandler;
import com.vaadin.ui.components.calendar.handler.BasicEventResizeHandler;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DISCLAIMER
 *
 * The quality of the code is such that you should not copy any of it as best
 * practice how to build Vaadin applications.
 *
 * @author jouni@vaadin.com
 *
 */

public class ShowView extends CssLayout implements View {

    boolean[] created = new boolean[366];
    HorizontalLayout tray;
    private CssLayout catalog;

    // private CSSInject css;
    private Window popup;
    private EntityManager entityManager;
    private Calendar cal;
    private MovieEventProvider provider = new MovieEventProvider();

    @Override
    public void enter(ViewChangeEvent event) {
        entityManager = ((DashboardUI) getUI()).getEm();

        setSizeFull();
        addStyleName("schedule");

        // css = new CSSInject(UI.getCurrent());

        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        tabs.addStyleName("borderless");
        addComponent(tabs);
        tabs.addComponent(buildCalendarView());

        catalog = new CssLayout();
        catalog.setCaption("Catalog");
        catalog.addStyleName("catalog");
        tabs.addComponent(catalog);
        List<Show> shows = entityManager.createQuery("select s from Show s where s.available is not empty ")
                .getResultList();
        for (final Show show : shows) {
            Image poster = new Image(show.getDescription(), new ExternalResource(
                    show.getImageUrl()));

            CssLayout frame = new CssLayout();
            frame.addComponent(poster);
            frame.addLayoutClickListener(new LayoutClickListener() {
                @Override
                public void layoutClick(LayoutClickEvent event) {
                    if (event.getButton() == MouseButton.LEFT) {
                        Window w = new ShowDetailView(show, null);
                        UI.getCurrent().addWindow(w);
                        w.focus();
                    }
                }
            });
            catalog.addComponent(frame);
        }

    }

    private Component buildCalendarView() {
        VerticalLayout calendarLayout = new VerticalLayout();
        calendarLayout.setCaption("Calendar");
        calendarLayout.addStyleName("dummy");
        calendarLayout.setMargin(true);

        cal = new Calendar(provider);
        cal.setWidth("100%");
        cal.setHeight("1000px");

        // cal.setStartDate(new Date());
        // cal.setEndDate(new Date());

        cal.setHandler(new EventClickHandler() {
            @Override
            public void eventClick(EventClick event) {
                hideTray();
                getUI().removeWindow(popup);
                buildPopup((ShowEvent) event.getCalendarEvent());
                getUI().addWindow(popup);
                popup.focus();
                // if (!helpShown) {
                // ((QuickTicketsDashboardUI) getUI())
                // .getHelpManager()
                // .addOverlay(
                // "Change the movie",
                // "Try to drag the movie posters from the tray onto the poster in the window",
                // "poster").center();
                // helpShown = true;
                // }
            }
        });
        calendarLayout.addComponent(cal);

        cal.setFirstVisibleHourOfDay(11);
        cal.setLastVisibleHourOfDay(23);

        cal.setHandler(new BackwardHandler() {
            @Override
            public void backward(BackwardEvent event) {
                createEvents();
            }
        });

        cal.setHandler(new ForwardHandler() {
            @Override
            public void forward(ForwardEvent event) {
                createEvents();
            }
        });

        cal.setDropHandler(new DropHandler() {
            private static final long serialVersionUID = -8939822725278862037L;

            public void drop(DragAndDropEvent event) {
            }

            public AcceptCriterion getAcceptCriterion() {
                return AcceptAll.get();
            }

        });

        cal.setHandler(new BasicEventMoveHandler() {
            @Override
            public void eventMove(MoveEvent event) {
                CalendarEvent calendarEvent = event.getCalendarEvent();
                if (calendarEvent instanceof ShowEvent) {
                    ShowEvent editableEvent = (ShowEvent) calendarEvent;

                    Date newFromTime = event.getNewStart();

                    // Update event dates
                    long length = editableEvent.getEnd().getTime()
                            - editableEvent.getStart().getTime();
                    setDates(editableEvent, newFromTime,
                            new Date(newFromTime.getTime() + length));
                    showTray();
                }
            }

            protected void setDates(ShowEvent event, Date start, Date end) {
                event.start = start;
                event.end = end;
            }
        });
        cal.setHandler(new BasicEventResizeHandler() {
            @Override
            public void eventResize(EventResize event) {
                Notification
                        .show("You're not allowed to change the movie duration");
            }
        });

        createEvents();

        cal.setReadOnly(true);
        return calendarLayout;
    }

    void createEvents() {
       createEventsForDay();

        // Add all movie cover images as classes to CSSInject
        String styles = "";
        List<Show> shows = entityManager.createQuery("select s from Show s where s.available is not empty ")
                .getResultList();
        for (Show m : shows) {
            WebBrowser webBrowser = Page.getCurrent().getWebBrowser();

            String bg = "url(VAADIN/themes/" + UI.getCurrent().getTheme()
                    + "/img/event-title-bg.png), url(" + m.getImageUrl() + ")";

            // IE8 doesn't support multiple background images
            if (webBrowser.isIE() && webBrowser.getBrowserMajorVersion() == 8) {
                bg = "url(" + m.getImageUrl() + ")";
            }

            styles += ".v-calendar-event-" + m.getName().replaceAll("&", "_")
                    + " .v-calendar-event-content {background-image:" + bg
                    + ";}";
        }

        Page.getCurrent().getStyles().add(styles);
    }

    void createEventsForDay() {
        List<Show> shows = entityManager.createQuery("select s from Show s where s.available is not empty ")
                .getResultList();
        boolean[] used = new boolean[shows.size()];
       for(Show show : shows) {
            Date start = show.getStart();
            Date end = show.getEnd();
            ShowEvent e = new ShowEvent(start, end, show);
            provider.addEvent(e);
        }

    }

    void buildPopup(final ShowEvent event) {
        popup = new ShowDetailView(event.show, event);
    }

    void buildTray() {
        if (tray != null)
            return;

        tray = new HorizontalLayout();
        tray.setWidth("100%");
        tray.addStyleName("tray");
        tray.setSpacing(true);
        tray.setMargin(true);

        Label warning = new Label(
                "You have unsaved changes made to the schedule");
        warning.addStyleName("warning");
        warning.addStyleName("icon-attention");
        tray.addComponent(warning);
        tray.setComponentAlignment(warning, Alignment.MIDDLE_LEFT);
        tray.setExpandRatio(warning, 1);

        ClickListener close = new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                tray.removeStyleName("v-animate-reveal");
                tray.addStyleName("v-animate-hide");
            }
        };

        Button confirm = new Button("Confirm");
        confirm.addStyleName("wide");
        confirm.addStyleName("default");
        confirm.addClickListener(close);
        tray.addComponent(confirm);
        tray.setComponentAlignment(confirm, Alignment.MIDDLE_LEFT);

        Button discard = new Button("Discard");
        discard.addStyleName("wide");
        discard.addClickListener(close);
        tray.addComponent(discard);
        tray.setComponentAlignment(discard, Alignment.MIDDLE_LEFT);
    }

    void showTray() {
        buildTray();
        tray.removeStyleName("v-animate-hide");
        tray.addStyleName("v-animate-reveal");
        addComponent(tray);
    }

    void hideTray() {
        if (tray != null)
            removeComponent(tray);
    }

    // boolean helpShown = false;

    class MovieEventProvider implements CalendarEventProvider {
        private List<CalendarEvent> events = new ArrayList<CalendarEvent>();

        @Override
        public List<CalendarEvent> getEvents(Date startDate, Date endDate) {
            return events;
        }

        public void addEvent(CalendarEvent MovieEvent) {
            events.add(MovieEvent);
        }

    }

    class ShowEvent implements CalendarEvent {

        Date start;
        Date end;
        String caption;
        Show show;

        public ShowEvent(Date start, Date end, Show show) {
            this.start = start;
            this.end = end;
            this.caption = show.getName();
            this.show = show;
        }

        @Override
        public Date getStart() {
            return start;
        }

        @Override
        public Date getEnd() {
            return end;
        }

        @Override
        public String getCaption() {
            return caption;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getStyleName() {
            return show.getName().replaceAll("&", "_");
        }

        @Override
        public boolean isAllDay() {
            return false;
        }

    }

}
