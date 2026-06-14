package com.librarysystem.service;

import com.librarysystem.dao.UserDao;
import com.librarysystem.model.RoleLevel;
import com.librarysystem.model.User;
import com.librarysystem.util.PasswordUtil;
import com.librarysystem.util.ValidationUtil;

import java.sql.SQLException;

public class AuthService {
    private final UserDao userDao = new UserDao();

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

    public User register(String studentNo, String name, String password) throws SQLException {
        String normalizedStudentNo = ValidationUtil.requireText(studentNo, "Student number");
        String normalizedName = ValidationUtil.requireText(name, "Name");
        String normalizedPassword = ValidationUtil.requireText(password, "Password");
        if (userDao.findByStudentNo(normalizedStudentNo).isPresent()) {
            throw new IllegalArgumentException("That student number is already registered.");
        }
        return userDao.createStudent(
                normalizedStudentNo,
                normalizedName,
                PasswordUtil.sha256(normalizedPassword),
                RoleLevel.NORMAL
        );
    }
}
