package org.springframework.beans;

/**
 * @author wuhan
 * @description
 * @create 2023-04-09 21:21
 */
public class Student {
	private String name;

	private int age;

	private String addr;

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

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	@Override
	public String toString() {
		return "Student{" +
				"name='" + name + '\'' +
				", age=" + age +
				", addr='" + addr + '\'' +
				'}';
	}
}
