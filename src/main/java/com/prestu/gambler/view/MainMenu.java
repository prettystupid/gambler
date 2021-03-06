package com.prestu.gambler.view;

import com.google.common.eventbus.Subscribe;
import com.prestu.gambler.component.ProfilePreferencesWindow;
import com.prestu.gambler.domain.User;
import com.prestu.gambler.event.AppEvent;
import com.prestu.gambler.event.AppEventBus;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;

public class MainMenu extends CustomComponent {

    private static final String STYLE_VISIBLE = "valo-menu-visible";
    private MenuItem settingsItem;

    MainMenu() {
        setPrimaryStyleName("valo-menu");
        setSizeUndefined();

        AppEventBus.register(this);

        setCompositionRoot(buildContent());
    }

    private Component buildContent() {
        CssLayout menuContent = new CssLayout();
        menuContent.addStyleName("sidebar");
        menuContent.addStyleName(ValoTheme.MENU_PART);
        menuContent.addStyleName("no-vertical-drag-hints");
        menuContent.addStyleName("no-horizontal-drag-hints");
        menuContent.setWidth(null);
        menuContent.setHeight("100%");

        menuContent.addComponent(buildTitle());
        menuContent.addComponent(buildUserMenu());
        menuContent.addComponent(buildToggleButton());
        menuContent.addComponent(buildMenuItems());

        return menuContent;
    }

    private Component buildTitle() {
        Label logo = new Label("<strong>GAMBLER</strong>",
                ContentMode.HTML);
        logo.setSizeUndefined();
        HorizontalLayout logoWrapper = new HorizontalLayout(logo);
        logoWrapper.setComponentAlignment(logo, Alignment.MIDDLE_CENTER);
        logoWrapper.addStyleName("valo-menu-title");
        return logoWrapper;
    }

    private User getCurrentUser() {
        return (User) VaadinSession.getCurrent().getAttribute(
                User.class.getName());
    }

    private Component buildUserMenu() {
        MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        final User user = getCurrentUser();
        settingsItem = settings.addItem("", new ThemeResource("img/logo/logo0.jpg"), null);
        updateUserNameAndLogo(null);
        settingsItem.addItem("Редактировать", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                ProfilePreferencesWindow.open(user);
            }
        });
        settingsItem.addSeparator();
        settingsItem.addItem("Выйти", new Command() {
            @Override
            public void menuSelected(MenuItem selectedItem) {
                AppEventBus.post(new AppEvent.UserLoggedOutEvent(getCurrentUser().getUsername()));
            }
        });
        return settings;
    }

    private Component buildToggleButton() {
        Button valoMenuToggleButton = new Button("Menu", new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                if (getCompositionRoot().getStyleName().contains(STYLE_VISIBLE)) {
                    getCompositionRoot().removeStyleName(STYLE_VISIBLE);
                } else {
                    getCompositionRoot().addStyleName(STYLE_VISIBLE);
                }
            }
        });
        valoMenuToggleButton.setIcon(FontAwesome.LIST);
        valoMenuToggleButton.addStyleName("valo-menu-toggle");
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        return valoMenuToggleButton;
    }

    private Component buildMenuItems() {
        CssLayout menuItemsLayout = new CssLayout();
        menuItemsLayout.addStyleName("valo-menuitems");

        for (ViewType view : ViewType.values()) {
            Component menuItemComponent = new ValoMenuItemButton(view);

            menuItemsLayout.addComponent(menuItemComponent);
        }
        return menuItemsLayout;

    }

    @Override
    public void attach() {
        super.attach();
    }

    @Subscribe
    public void updateUserNameAndLogo(AppEvent.ProfileUpdatedEvent event) {
        User user = getCurrentUser();
        settingsItem.setText(user.getUsername());
        settingsItem.setIcon(user.getLogo());
    }

    public class ValoMenuItemButton extends Button {

        ValoMenuItemButton(ViewType view) {
            setPrimaryStyleName("valo-menu-item");
            setIcon(view.getIcon());
            setCaption(view.getCaption());
            AppEventBus.register(this);
            addClickListener(new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {
                    UI.getCurrent().getNavigator()
                            .navigateTo(view.getViewName());
                }
            });

        }
    }
}
