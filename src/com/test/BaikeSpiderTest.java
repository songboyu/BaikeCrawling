package com.test;

import java.util.Scanner;

import com.baike.Spider;

public class BaikeSpiderTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Spider s;
		if(args.length == 2){
			int start = Integer.parseInt(args[0]);
			int end = Integer.parseInt(args[1]);
			s = new Spider(end, start);
		}
		else{
			Scanner in = new Scanner(System.in);
			System.out.print("y or n:");
			String c = in.next();
			if(c.equals("y")){

				System.out.print("start:");
				int start 	= in.nextInt();
				System.out.print("end:");
				int max 	= in.nextInt();
				s = new Spider(max, start);
			}
			else
				s = new Spider();
		}
		s.startCrawling();
	}

}
