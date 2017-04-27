package com.fredtec;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.kociemba.twophase.Facelet;
import org.kociemba.twophase.Search;
import org.kociemba.twophase.Tools;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.TooManyListenersException;

public class Main {

	private GUI gui;
	private Arduino arduino;
	
	private Main() {
		arduino = new Arduino();
		gui = new GUI(arduino);
		arduino.setGUI(gui);
		new Thread(gui).start();
	}
	
	
    public static void main(String[] args) {
	// write your code here
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new Main();
    }
}
