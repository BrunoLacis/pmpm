package com.pmpm.theatre.view.admin;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.model.Ticket;
import com.pmpm.theatre.model.User;
import com.pmpm.theatre.model.UserRole;
import com.pmpm.theatre.view.dashboard.DashboardUI;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

import static com.pmpm.theatre.view.dashboard.DashboardUI.RESOURCE_BUNDLE;

public class AdminView extends VerticalLayout implements View {


    Window notifications;
    private JPAContainer<User> userJPAContainer;
    private JPAContainer<Show> showJPAContainer;
    private JPAContainer<Ticket> ticketJPAContainer;
    private EntityManager em;
    private Table table;

    public AdminView() {
        init();
    }

    private void init() {

        setSizeFull();
        addStyleName("schedule");

        Label header = new Label(RESOURCE_BUNDLE.getString("user.management.title"));
        header.addStyleName("h1");
        addComponent(header);

        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidth("100%");
        toolbar.setSpacing(true);
        toolbar.setMargin(true);
        toolbar.addStyleName("toolbar");
        addComponent(toolbar);


        Button addUser = new Button(RESOURCE_BUNDLE.getString("user.management.add"));
        addUser.addClickListener(new AddUserListener());
        toolbar.addComponent(addUser);

        Button removeUser = new Button(RESOURCE_BUNDLE.getString("user.management.remove"));
        removeUser.addClickListener(new RemoveUserListener());
        toolbar.addComponent(removeUser);


        Button addTask = new Button(RESOURCE_BUNDLE.getString("user.management.task"));
        addTask.addClickListener(new AddShow());
        toolbar.addComponent(addTask);


        table = new Table("", userJPAContainer);
        table.setSelectable(true);
        table.setSizeFull();
        table.setWidth("100%");
        table.addContainerProperty("username", String.class, null, RESOURCE_BUNDLE.getString("auth.username"), null, null);
        table.addContainerProperty("password", String.class, null, RESOURCE_BUNDLE.getString("auth.password"), null, null);
        table.addContainerProperty("realName", String.class, null, RESOURCE_BUNDLE.getString("user.real.name"), null, null);
        table.addContainerProperty("email", String.class, null, RESOURCE_BUNDLE.getString("user.email"), null, null);
        table.addContainerProperty("role", String.class, null, RESOURCE_BUNDLE.getString("user.role"), null, null);

        addComponent(table);
        setExpandRatio(table, 1);

    }

    private void addUserWindow() {

        // Have some content for it
        FormLayout content = new FormLayout();
        final TextField userNameField = new TextField(RESOURCE_BUNDLE.getString("auth.username"));
        content.addComponent(userNameField);
        content.setComponentAlignment(userNameField,
                Alignment.MIDDLE_LEFT);

        final PasswordField password = new PasswordField(RESOURCE_BUNDLE.getString("auth.password"));
        content.addComponent(password);
        content.setComponentAlignment(password,
                Alignment.MIDDLE_LEFT);
        final TextField realName = new TextField(RESOURCE_BUNDLE.getString("user.real.name"));
        content.addComponent(realName);
        content.setComponentAlignment(realName,
                Alignment.MIDDLE_LEFT);


        final TextField email = new TextField(RESOURCE_BUNDLE.getString("user.email"));
        content.addComponent(email);
        content.setComponentAlignment(email,
                Alignment.MIDDLE_LEFT);

        final ComboBox userRights = new ComboBox(RESOURCE_BUNDLE.getString("user.role"), Arrays.asList(UserRole.values()));
        userRights.select(UserRole.USER);
        content.addComponent(userRights);
        content.setComponentAlignment(userRights,
                Alignment.MIDDLE_LEFT);
        content.setSizeFull();
        final Window window = new Window("Add show", content);
        final Button button = new Button(RESOURCE_BUNDLE.getString("add"));
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                try {


                    User zooUser = new User();
                    zooUser.setUsername(userNameField.getValue());
                    zooUser.setPassword(DigestUtils.md5Hex(password.getValue()));
                    zooUser.setRealName(realName.getValue());
                    zooUser.setRole((UserRole) userRights.getValue());

                    if (StringUtils.isBlank(zooUser.getUsername())) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.error.username"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    if (StringUtils.isBlank(zooUser.getPassword())) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.error.password"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    if (StringUtils.isBlank(zooUser.getRealName())) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.error.realname"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }

                    if (zooUser.getRole() == null) {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.error.role"), Notification.Type.ERROR_MESSAGE);
                        return;
                    }
                    Query query = em.createNamedQuery("getUser");
                    query.setParameter("name", zooUser.getUsername());
                    List<User> u = query.getResultList();
                    if (u.isEmpty()) {

                        userJPAContainer.addEntity(zooUser);
                        userJPAContainer.commit();
                        table.refreshRowCache();
                    } else {
                        Notification.show(RESOURCE_BUNDLE.getString("user.management.error.exists"), Notification.Type.ERROR_MESSAGE);
                    }
                } finally {
                    window.close();
                }
            }
        });

        content.addComponent(button);

        content.setSizeFull();
        window.setWidth("40%");
        window.setHeight("40%");
        window.setVisible(true);
        window.setModal(true);
        window.center();
        getUI().addWindow(window);
    }



    @Override
    public void enter(ViewChangeEvent event) {
        if (em == null && userJPAContainer == null && showJPAContainer == null) {
            em = ((DashboardUI) getUI()).getEm();
            userJPAContainer = JPAContainerFactory.make(User.class, em);
            showJPAContainer = JPAContainerFactory.make(Show.class, em);
            ticketJPAContainer = JPAContainerFactory.make(Ticket.class, em);
            table.setContainerDataSource(userJPAContainer);
            table.setVisibleColumns(new String[]{"username", "password", "realName", "role"});
        }
    }

    private class AddUserListener implements Button.ClickListener {

        @Override
        public void buttonClick(Button.ClickEvent event) {

            addUserWindow();
        }
    }

    private class RemoveUserListener implements Button.ClickListener {

        @Override
        public void buttonClick(Button.ClickEvent event) {
            Long selected = (Long) table.getValue();
            if (selected != null) {

                User user = userJPAContainer.getItem(selected).getEntity();
                if (user.getId().equals(((DashboardUI) getUI()).getUser().getId())) {
                    Notification.show(RESOURCE_BUNDLE.getString("user.management.error.remove"), Notification.Type.ERROR_MESSAGE);
                    return;
                }
                em.getTransaction().begin();


                Query query = em.createQuery("update Ticket t set t.owner = null where t.owner = :user");
                query.setParameter("user", userJPAContainer.getItem(selected).getEntity());
                query.executeUpdate();
                em.getTransaction().commit();
                userJPAContainer.removeItem(selected);
                table.refreshRowCache();
            } else {
                Notification.show(RESOURCE_BUNDLE.getString("user.management.error.remove"), Notification.Type.ERROR_MESSAGE);
            }

        }
    }

    private class AddShow implements Button.ClickListener {

        @Override
        public void buttonClick(Button.ClickEvent event) {
            Window w = new AddShowView(table, showJPAContainer, ticketJPAContainer);
            UI.getCurrent().addWindow(w);
            w.focus();
        }
    }


}
