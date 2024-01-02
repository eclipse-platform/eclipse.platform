package testPackage;

import java.io.File;

public class Javaclass1 {

	public static void main(String[] args) {
		
		
		int a=0;
		
		System.out.println("");
		
		call_me();
		
		callMe(a);

	}

	private static void callMe(int a) {
		// TODO Auto-generated method stub
		System.out.println();
		
	}

	private static void call_me() {
		File f= new File("");
		System.out.println("I am calledJavaclass1.java");
	}

}
