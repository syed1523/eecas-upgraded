package com.expense.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.EnableAspectJAutoProxy
@SpringBootApplication
public class ExpenseAuditSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseAuditSystemApplication.class, args);
	}

}
