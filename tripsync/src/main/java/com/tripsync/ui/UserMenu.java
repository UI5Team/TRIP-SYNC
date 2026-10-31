package com.tripsync.ui;

import java.util.List;
import java.util.Optional;

import com.tripsync.data.DataAccessException;
import com.tripsync.data.UserRepository;
import com.tripsync.model.AppUser;
import com.tripsync.nav.AppContext;
import com.tripsync.nav.Route;
import com.tripsync.nav.Router;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;

public class UserMenu extends MenuButton {

    public UserMenu() {
        getStyleClass().add("user-chip");
        refresh();
    }

    public final void refresh() {
        List<AppUser> users;
        Optional<AppUser> current;
        try {
            users = UserRepository.findAll();
            current = UserRepository.currentUser();
        } catch (DataAccessException | IllegalStateException e) {
            setText("프로필 오류");
            return;
        }

        setText("  " + current.map(AppUser::displayName).orElse("프로필 없음") + "  ");
        getItems().clear();

        for (AppUser user : users) {
            MenuItem item = new MenuItem(user.displayName());
            item.setOnAction(e -> switchUser(user));
            getItems().add(item);
        }

        getItems().add(new SeparatorMenuItem());

        MenuItem addItem = new MenuItem("＋ 새 프로필 추가");
        addItem.setOnAction(e -> addUser());
        getItems().add(addItem);
    }

    private void switchUser(AppUser user) {
        UserRepository.setCurrentUser(user.id());
        AppContext.clear();
        Router.resetTo(Route.GROUP_LIST, null);
        Toast.success(user.displayName() + " 프로필로 전환했습니다.");
    }

    private void addUser() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("새 프로필 추가");
        dialog.setHeaderText("초대 코드 참여를 시연하기 위한 로컬 프로필입니다.");
        dialog.setContentText("표시 이름:");

        dialog.showAndWait().ifPresent(rawName -> {
            String name = rawName.trim();
            if (name.isEmpty()) {
                Toast.warn("이름을 입력해 주세요.");
                return;
            }
            try {
                switchUser(UserRepository.insert(name));
            } catch (DataAccessException e) {
                Toast.warn(e.getMessage());
            }
        });
    }
}
