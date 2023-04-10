package org.springframework;

import org.springframework.beans.Student;
import org.springframework.beans.Teacher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author wuhan
 * @description
 * @create 2023-04-09 21:21
 */
public class Main {
	public static void main(String[] args) {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-beans.xml");
		Student student = applicationContext.getBean("student", Student.class);
		applicationContext = new AnnotationConfigApplicationContext("org.springframework.beans");
		Teacher teacher = applicationContext.getBean("teacher", Teacher.class);
		System.out.println("student = " + student);
		System.out.println("teacher = " + teacher);
	}
}