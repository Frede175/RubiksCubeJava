package com.fredtec;

import org.kociemba.twophase.Facelet;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Main {

	private GUI gui;
	private Solver solver;
	private Arduino arduino;
	
	private Main() {
		solver = new Solver();
		arduino = new Arduino();
		gui = new GUI(solver, arduino);
		
		System.out.println();
		
		
		new Thread(gui).start();
	}
	
	
    public static void main(String[] args) {
	// write your code here
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
}
