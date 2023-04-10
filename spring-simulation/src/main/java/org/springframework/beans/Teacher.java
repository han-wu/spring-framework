package org.springframework.beans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author wuhan
 * @description
 * @create 2023-04-09 21:44
 */
@Component
public class Teacher {
	@Value("吴老师")
	private String name;

	@Value("30")

	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "Teacher{" +
				"name='" + name + '\'' +
				", age=" + age +
				'}';
	}
}
