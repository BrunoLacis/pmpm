package com.pmpm.theatre.view.dashboard;

import com.pmpm.theatre.model.Show;
import com.pmpm.theatre.model.User;
import com.pmpm.theatre.model.UserRole;
import com.pmpm.theatre.view.admin.AdminView;
import com.pmpm.theatre.view.user.ShowView;
import com.vaadin.addon.jpacontainer.JPAContainer;
import com.vaadin.addon.jpacontainer.JPAContainerFactory;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.*;

@Theme("dashboard")
@PreserveOnRefresh
@Title("Zoo")
public class DashboardUI extends UI {

    public static EntityManagerFactory emf = Persistence.createEntityManagerFactory("ZooVad");
    public static ResourceBundle RESOURCE_BUNDLE;
    private CssLayout root = new CssLayout();
    private VerticalLayout loginLayout;
    private CssLayout menu = new CssLayout();
    private CssLayout content = new CssLayout();
    private HashMap<String, Class<? extends View>> routes = new HashMap<String, Class<? extends View>>() {
        {
            put("/dashboard", DashboardView.class);
            put("/shows", ShowView.class);
            put("/admin", AdminView.class);
        }
    };
    private HashMap<String, Button> viewNameToMenuButton = new HashMap<String, Button>();
    boolean autoCreateReport = false;
    private User user;
    private Navigator nav;
    private HelpManager helpManager;
    private EntityManager em;

    @Override
    protected void init(VaadinRequest request) {
        helpManager = new HelpManager(this);

        em = emf.createEntityManager();
        setLocale(Locale.forLanguageTag("lv"));
        setContent(root);
        root.addStyleName("root");
        root.setSizeFull();
        RESOURCE_BUNDLE = ResourceBundle.getBundle("messages", getLocale());
        Label bg = new Label();
        bg.setSizeUndefined();
        bg.addStyleName("login-bg");
        root.addComponent(bg);
        buildLoginView(false);

    }

    private void buildLoginView(boolean exit) {
        if (exit) {
            root.removeAllComponents();
        }
        helpManager.closeAll();
        HelpOverlay w = helpManager
                .addOverlay(
                        RESOURCE_BUNDLE.getString("help.welcome.caption"),
                        RESOURCE_BUNDLE.getString("help.welcome.message"),
                        "login");
        w.center();
        addWindow(w);

        addStyleName("login");

        loginLayout = new VerticalLayout();
        loginLayout.setSizeFull();
        loginLayout.addStyleName("login-layout");
        root.addComponent(loginLayout);

        final CssLayout loginPanel = new CssLayout();
        loginPanel.addStyleName("login-panel");

        HorizontalLayout labels = new HorizontalLayout();
        labels.setWidth("100%");
        labels.setMargin(true);
        labels.addStyleName("labels");
        loginPanel.addComponent(labels);

        Label welcome = new Label(RESOURCE_BUNDLE.getString("help.welcome.caption"));
        welcome.setSizeUndefined();
        welcome.addStyleName("h4");
        labels.addComponent(welcome);
        labels.setComponentAlignment(welcome, Alignment.MIDDLE_LEFT);

        Label title = new Label(RESOURCE_BUNDLE.getString("dashboard.title"));
        title.setSizeUndefined();
        title.addStyleName("h2");
        title.addStyleName("light");
        labels.addComponent(title);
        labels.setComponentAlignment(title, Alignment.MIDDLE_RIGHT);

        HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.setMargin(true);
        fields.addStyleName("fields");

        final TextField username = new TextField(RESOURCE_BUNDLE.getString("auth.username"));
        username.focus();
        fields.addComponent(username);

        final PasswordField password = new PasswordField(RESOURCE_BUNDLE.getString("auth.password"));
        fields.addComponent(password);

        final Button signin = new Button(RESOURCE_BUNDLE.getString("auth.sign.button"));
        signin.addStyleName("default");
        fields.addComponent(signin);
        fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);

        Button create = new Button(RESOURCE_BUNDLE.getString("auth.create.button"));
        create.addStyleName("default");
        fields.addComponent(create);
        fields.setComponentAlignment(create, Alignment.BOTTOM_LEFT);
        create.addClickListener( new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                addUserWindow();
            }
        });
        final ShortcutListener enter = new ShortcutListener(RESOURCE_BUNDLE.getString("auth.sign.button"),
                KeyCode.ENTER, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                signin.click();
            }
        };

        signin.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                if (username.getValue() != null && password.getValue() != null) {
                    Query query = em.createNamedQuery("getUser");
                    query.setParameter("name", username.getValue());
                    User user = null;
                    try {
                        user = (User) query.getSingleResult();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (user != null && StringUtils.equalsIgnoreCase(user.getPassword(), (DigestUtils.md5Hex(password.getValue())))) {
                        signin.removeShortcutListener(enter);
                        DashboardUI.this.user = user;
                        setData(user);
                        buildMainView();
                    } else {
                        if (loginPanel.getComponentCount() > 2) {
                            // Remove the previous error message
                            loginPanel.removeComponent(loginPanel.getComponent(2));
                        }
                        // Add new error message
                        Label error = new Label(
                                RESOURCE_BUNDLE.getString("auth.error"),
                                ContentMode.HTML);
                        error.addStyleName("error");
                        error.setSizeUndefined();
                        error.addStyleName("light");
                        // Add animation
                        error.addStyleName("v-animate-reveal");
                        loginPanel.addComponent(error);
                        username.focus();
                    }
                } else {
                    if (loginPanel.getComponentCount() > 2) {
                        // Remove the previous error message
                        loginPanel.removeComponent(loginPanel.getComponent(2));
                    }
                    // Add new error message
                    Label error = new Label(
                            RESOURCE_BUNDLE.getString("auth.error"),
                            ContentMode.HTML);
                    error.addStyleName("error");
                    error.setSizeUndefined();
                    error.addStyleName("light");
                    // Add animation
                    error.addStyleName("v-animate-reveal");
                    loginPanel.addComponent(error);
                    username.focus();
                }
            }
        });

        signin.addShortcutListener(enter);

        loginPanel.addComponent(fields);

        loginLayout.addComponent(loginPanel);
        loginLayout.setComponentAlignment(loginPanel, Alignment.MIDDLE_CENTER);
    }

    public User getUser() {
        return user;
    }

    private void buildMainView() {

        nav = new Navigator(this, content);

        for (String route : routes.keySet()) {
            nav.addView(route, routes.get(route));
        }

        helpManager.closeAll();
        removeStyleName("login");
        root.removeComponent(loginLayout);

        root.addComponent(new HorizontalLayout() {
            {
                setSizeFull();
                addStyleName("main-view");
                addComponent(new VerticalLayout() {
                    // Sidebar
                    {
                        addStyleName("sidebar");
                        setWidth(null);
                        setHeight("100%");

                        // Branding element
                        addComponent(new CssLayout() {
                            {
                                addStyleName("branding");
                                Label logo = new Label(
                                        RESOURCE_BUNDLE.getString("dashboard.title"),
                                        ContentMode.HTML);
                                logo.setSizeUndefined();
                                addComponent(logo);
                                // addComponent(new Image(null, new
                                // ThemeResource(
                                // "img/branding.png")));
                            }
                        });

                        // Main menu
                        addComponent(menu);
                        setExpandRatio(menu, 1);

                        // User menu
                        addComponent(new VerticalLayout() {
                            {
                                setSizeUndefined();
                                addStyleName("user");
                                Image profilePic = new Image(
                                        null,
                                        new ThemeResource("img/profile-pic.png"));
                                profilePic.setWidth("34px");
                                addComponent(profilePic);
                                Label userName = new Label(user.getRealName());
                                userName.setSizeUndefined();
                                addComponent(userName);

                                Command cmd = new Command() {
                                    @Override
                                    public void menuSelected(
                                            MenuItem selectedItem) {

                                    }
                                };

                                Command locale = new Command() {
                                    @Override
                                    public void menuSelected(
                                            MenuItem selectedItem) {
                                        changeLocale();
                                    }
                                };
                                MenuBar settings = new MenuBar();
                                MenuItem settingsMenu = settings.addItem("",
                                        null);
                                settingsMenu.setStyleName("icon-cog");
                                settingsMenu.addItem("Settings", locale);
                                settingsMenu.addSeparator();
                                addComponent(settings);

                                Button exit = new NativeButton("Exit");
                                exit.addStyleName("icon-cancel");
                                exit.setDescription("Sign Out");
                                addComponent(exit);
                                exit.addClickListener(new ClickListener() {
                                    @Override
                                    public void buttonClick(ClickEvent event) {
                                        buildLoginView(true);
                                    }
                                });
                            }
                        });
                    }
                });
                // Content
                addComponent(content);
                content.setSizeFull();
                content.addStyleName("view-content");
                setExpandRatio(content, 1);
            }

        });

        menu.removeAllComponents();

        for (final String view : new String[]{"dashboard", "admin", "shows"}) {
            if ("admin".equals(view) && user.getRole() != UserRole.ADMIN) continue;
            if ("tickets".equals(view) && user.getRole() != UserRole.ADMIN) continue;
            Button b = new NativeButton(RESOURCE_BUNDLE.getString(view));
            b.addStyleName("icon-" + view);
            b.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    clearMenuSelection();
                    event.getButton().addStyleName("selected");
                    if (!nav.getState().equals("/" + view))
                        nav.navigateTo("/" + view);
                }
            });


            menu.addComponent(b);

            viewNameToMenuButton.put("/" + view, b);
        }
        menu.addStyleName("menu");
        menu.setHeight("100%");

        viewNameToMenuButton.get("/dashboard").setHtmlContentAllowed(true);
        viewNameToMenuButton.get("/dashboard").setCaption(
                RESOURCE_BUNDLE.getString("dashboard"));

        String f = Page.getCurrent().getUriFragment();
        if (f != null && f.startsWith("!")) {
            f = f.substring(1);
        }
        if (f == null || f.equals("") || f.equals("/")) {
            nav.navigateTo("/dashboard");
            menu.getComponent(0).addStyleName("selected");
            helpManager.showHelpFor(DashboardView.class);
        } else {
            nav.navigateTo(f);
            helpManager.showHelpFor(routes.get(f));
            viewNameToMenuButton.get(f).addStyleName("selected");
        }

        nav.addViewChangeListener(new ViewChangeListener() {

            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                helpManager.closeAll();
                return true;
            }

            @Override
            public void afterViewChange(ViewChangeEvent event) {
                View newView = event.getNewView();
                helpManager.showHelpFor(newView);

                autoCreateReport = false;
            }
        });

    }

    public EntityManager getEm() {
        return em;
    }

    private void clearMenuSelection() {
        for (Iterator<Component> it = menu.getComponentIterator(); it.hasNext(); ) {
            Component next = it.next();
            if (next instanceof NativeButton) {
                next.removeStyleName("selected");
            } else if (next instanceof DragAndDropWrapper) {
                // Wow, this is ugly (even uglier than the rest of the code)
                ((DragAndDropWrapper) next).iterator().next()
                        .removeStyleName("selected");
            }
        }
    }
    @Override
    public void close() {
        super.close();    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void changeLocale() {

        VerticalLayout content = new VerticalLayout();
        final ComboBox typeBox = new ComboBox("Type", Arrays.asList(new String[]{"LV", "US"}));
        typeBox.setWidth("100%");
        content.addComponent(typeBox);
        content.setComponentAlignment(typeBox,
                Alignment.TOP_LEFT);


        final Window window = new Window("Change language", content);
        final Button button = new Button("Add");
        button.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {

                    String locale = (String) typeBox.getValue();
                    if (!StringUtils.isBlank(locale)) {
                        setLocale(Locale.forLanguageTag(locale));
                        RESOURCE_BUNDLE = ResourceBundle.getBundle("messages", getLocale());
                    }
                } finally {
                    window.close();
                }
            }
        });

        content.addComponent(button);

        content.setSizeFull();
        window.setWidth("10%");
        window.setHeight("10%");
        window.setVisible(true);
        window.setModal(true);
        window.center();
        getUI().addWindow(window);
    }


    private void addUserWindow() {
        final JPAContainer<User> userJPAContainer = JPAContainerFactory.make(User.class, em);

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
                    zooUser.setRole(UserRole.USER);

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

}