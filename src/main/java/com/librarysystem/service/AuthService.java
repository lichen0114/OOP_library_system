package com.librarysystem.service;

import com.librarysystem.dao.UserDao;
import com.librarysystem.model.User;
import com.librarysystem.model.UserLevel;
import com.librarysystem.util.PasswordUtil;
import com.librarysystem.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

public class AuthService {
    private final UserDao userDao = new UserDao();
    private final LevelService levelService = new LevelService();

    public User login(String studentNo, String password) throws SQLException {
        String normalizedStudentNo = ValidationUtil.requireText(studentNo, "Student number");
        String normalizedPassword = ValidationUtil.requireText(password, "Password");
        User user = userDao.findByStudentNo(normalizedStudentNo)
                .orElseThrow(() -> new IllegalArgumentException("No account exists for " + normalizedStudentNo + "."));
        if (!PasswordUtil.verify(normalizedPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Password is incorrect.");
        }
        return user;
    }

    public User loginStudent(String studentNo, String password) throws SQLException {
        User user = login(studentNo, password);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("Use administrator sign in for admin accounts.");
        }
        return user;
    }

    public User loginAdmin(String account, String password) throws SQLException {
        User user = login(account, password);
        if (!user.isAdmin()) {
            throw new IllegalArgumentException("This account is not an administrator.");
        }
        return user;
    }

    public User register(String studentNo, String name, String password) throws SQLException {
        return register(studentNo, name, password, "NORMAL");
    }

    public User register(String studentNo, String name, String password, String levelCode) throws SQLException {
        String normalizedStudentNo = ValidationUtil.requireText(studentNo, "Student number");
        String normalizedName = ValidationUtil.requireText(name, "Name");
        String normalizedPassword = ValidationUtil.requireText(password, "Password");
        String normalizedLevelCode = ValidationUtil.requireText(levelCode, "Initial level");
        if (userDao.findByStudentNo(normalizedStudentNo).isPresent()) {
            throw new IllegalArgumentException("That student number is already registered.");
        }
        UserLevel level = listRegistrationLevels().stream()
                .filter(candidate -> candidate.getCode().equalsIgnoreCase(normalizedLevelCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selected registration level is not available."));
        return userDao.createStudent(
                normalizedStudentNo,
                normalizedName,
                PasswordUtil.sha256(normalizedPassword),
                level.getCode()
        );
    }

    public List<UserLevel> listRegistrationLevels() throws SQLException {
        return levelService.listRegistrationLevels();
    }
}
