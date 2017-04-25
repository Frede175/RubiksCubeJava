package com.fredtec;

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
		
		new Thread(gui).start();
	}
	
	
    public static void main(String[] args) {
	// write your code here
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
}
