package com.prestu.gambler;

import javax.servlet.annotation.WebServlet;

import com.google.common.eventbus.Subscribe;
import com.prestu.gambler.data.DataProvider;
import com.prestu.gambler.data.GamblerDataProvider;
import com.prestu.gambler.domain.User;
import com.prestu.gambler.event.AppEvent;
import com.prestu.gambler.event.AppEventBus;
import com.prestu.gambler.exceptions.AuthenticException;
import com.prestu.gambler.exceptions.UserExistsException;
import com.prestu.gambler.exceptions.UserIsOnlineException;
import com.prestu.gambler.utils.Notifications;
import com.prestu.gambler.view.LoginView;
import com.prestu.gambler.view.MainView;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

@Push
@Theme("gambler")
@Title("Gambler")
public class GamblerAppUI extends UI {

    private final DataProvider dataProvider = new GamblerDataProvider();
    private final AppEventBus appEventbus = new AppEventBus();

    @Override
    protected void init(VaadinRequest vaadinRequest) {

        AppEventBus.register(this);
        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);

        updateContent();

        Page.getCurrent().addBrowserWindowResizeListener(
                new Page.BrowserWindowResizeListener() {
                    @Override
                    public void browserWindowResized(
                            Page.BrowserWindowResizeEvent event) {
                        AppEventBus.post(new AppEvent.BrowserResizeEvent());
                    }
                });
    }

    private void updateContent() {
        User user = (User) VaadinSession.getCurrent().getAttribute(
                User.class.getName());
        if (user != null) {
            setContent(new MainView());
            removeStyleName("loginview");
            getNavigator().navigateTo(getNavigator().getState());
        } else {
            setContent(new LoginView());
            addStyleName("loginview");
        }
    }

    @WebServlet(urlPatterns = "/*", name = "ApplicationServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = GamblerAppUI.class, productionMode = false)
    public static class ApplicationServlet extends VaadinServlet {
    }

    @Subscribe
    public void userLoginRequested(AppEvent.UserLoginRequestedEvent event) {
        User user;
        try {
            user = getDataProvider().authenticate(event.getUserName(), event.getPasswordHash());
            OnlineUsersObserver.getInstance().userSignIn(user);
        } catch (AuthenticException | UserIsOnlineException ex) {
            Notifications.show("Ошибка", ex.getMessage(), Notification.Type.ERROR_MESSAGE.getStyle());
            return;
        }

        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(User.class.getName(), user);
        updateContent();
    }

    @Subscribe
    public void userRegistered(AppEvent.UserRegisteredEvent event) {
        try {
            getDataProvider().registerUser(event.getUserName(), event.getPasswordHash(), event.getFirstName(), event.getLastName(), event.getEmail());
            ((LoginView) getContent()).popupLoginForm();
        } catch (UserExistsException ex) {
            Notifications.show("Ошибка", ex.getMessage(), Notification.Type.ERROR_MESSAGE.getStyle());
        }
    }

    @Subscribe
    public void profileUpdated(AppEvent.ProfileUpdatedEvent event) {
        getDataProvider().updateUser(event.getUser());
    }

    @Subscribe
    public void userLoggedOut(AppEvent.UserLoggedOutEvent event) {
        OnlineUsersObserver.getInstance().userSignOut((User) VaadinSession.getCurrent().getAttribute(User.class.getName()));
        VaadinSession.getCurrent().close();
        Page.getCurrent().reload();
    }

    @Subscribe
    public void saveScore(AppEvent.EndGameEvent event) {
        getDataProvider().saveScore(((User) VaadinSession.getCurrent().getAttribute(User.class.getName())).getId(),
                event.getGameId(), event.getScore());
    }

    @Subscribe
    public void closeOpenWindows(AppEvent.CloseOpenWindowsEvent event) {
        for (Window window : getWindows()) {
            window.close();
        }
    }

    public static DataProvider getDataProvider() {
        return ((GamblerAppUI) getCurrent()).dataProvider;
    }

    public static AppEventBus getGamblerEventbus() {
        return ((GamblerAppUI) getCurrent()).appEventbus;
    }
}
