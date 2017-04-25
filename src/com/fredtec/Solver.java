package com.fredtec;


import javafx.geometry.Side;
import org.kociemba.twophase.Facelet;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;

/**
 * Created by fsr19 on 4/25/2017.
 */
public class Solver {

	private Solver() {

	}
	
	public enum Colors {
		 WHITE,
		 RED,
		 ORANGE,
		 GREEN,
		 BLUE,
		 YELLOW
	}
	
	public static Sides sideOrder[] = {Sides.U, Sides.R, Sides.F, Sides.D, Sides.L, Sides.B};
	public static Sides dectionOrder[] = { Sides.U, Sides.F, Sides.D, Sides.L, Sides.B, Sides.R};
	
	public enum Sides {
	 	
	 	U("U"), R("R"), F("F"), D("D"), L("L"), B("B");
	 	
	 	final String value;
	 	
	 	private Sides(String value) {
	 		this.value = value;
		}
		
		final String getValue() {
	 		return value;
		}
	}
	
	public static String SolveCube(CubeSide cubeSides[]) {
		String cubeString = convertToCubeString(cubeSides);
		if (cubeString.equals("")) return "";
		return Search.solution(cubeString, 22, 2, false);
	}
	
	public static String convertToCubeString(CubeSide cubeSides[]) {
		if (cubeSides.length != 6) return ""; 
		
		CubeSide correctOrder[] = { cubeSides[0], cubeSides[5], cubeSides[1], cubeSides[2], cubeSides[3], cubeSides[4] };
		
		Colors sideColors[] = new Colors[6];
		
		for (int i = 0; i < 6; i++) {
			sideColors[i] = correctOrder[i].colors[4]; //Middle color
		}
		
		String cubeString = "";
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 9; j++) {
				cubeString += sideOrder[getIndexFromArray(correctOrder[i].colors[j], sideColors)].getValue();
			}
		}
		
		if (Tools.verify(cubeString) != 0) return "";
		
		return cubeString;
		
	}
	
	private static int getIndexFromArray(Colors color, Colors arr[]) {
		for (int i = 0; i < arr.length; i++) {
			if (color == arr[i]) return i;
		}
		return -1;
	}
}


