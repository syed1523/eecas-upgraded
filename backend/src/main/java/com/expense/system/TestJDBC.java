package com.expense.system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestJDBC {
    public static void main(String[] args) throws Exception {
        System.out.println("Listing users from database:");
        Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/expense_db_audit?useSSL=false&serverTimezone=UTC", "root", "syed");
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT email FROM users");
        while(rs.next()) {
            System.out.println(rs.getString("email"));
        }
        System.out.println("--- Done ---");
    }
}
