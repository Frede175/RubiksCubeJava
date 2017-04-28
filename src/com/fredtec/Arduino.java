package com.fredtec;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import gnu.io.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 * Created by fsr19 on 4/25/2017.
 */
public class Arduino {
	
	private GUI gui;
	private boolean hasGUI;
	
	public void setGUI(GUI gui) {
		this.gui = gui;
		hasGUI = true;
	}
		
	//This class is for the com between the arduino and this program.
	public static int OUTPUT = 1;
	public static int INPUT = 0;


	private static String appName = "Arduino";
	private static int DATA_RATE = 9600;
	private static final String PORT_NAMES[] = {
			"/dev/tty.usbmodem", // Mac OS X
			"/dev/usbdev", // Linux
			"/dev/tty", // Linux
			"/dev/serial", // Linux
			"COM3", "COM4"// Windows
	};
	private CommPortIdentifier portId;
	private SerialPort serial;
	private OutputStream out;
	private BufferedReader in;


	private boolean dataAvailable = false;

	public boolean isConnected() {
		return isConnected;
	}

	private boolean isConnected = false;

	public void disconnect() {
		serial.removeEventListener();
		serial.close();
		isConnected = false;
		System.out.println("INFO: Arduino Disconnected");
	}

	public void connect() throws PortInUseException, UnsupportedCommOperationException, TooManyListenersException, IOException {
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		while (portId == null && portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if ( currPortId.getName().equals(portName) || currPortId.getName().startsWith(portName))
				{
					// Try to connect to the Arduino on this port
					serial = (SerialPort)currPortId.open(appName, 1000);
					portId = currPortId;
					break;
				}
			}
		}
		if (portId != null) {
			serial.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serial.addEventListener(serialPortEvent -> {
				try {
					if (hasGUI && in.ready()) {
						gui.notifyData(in.readLine());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			//serial.notifyOnDataAvailable(true);
			out = serial.getOutputStream();
			in = new BufferedReader(new InputStreamReader(serial.getInputStream()));
			isConnected = true;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.err.println("Failed to sleep!");
			}
			sendData("Is open! \n");
		}
	}
	
	public void sendStop() {
		if (isConnected()) sendData("STOP\n");
	}
	
	public void sendResume() {
		if (isConnected()) sendData("RESUME\n");
	}
	
	public void sendScan(int index) {
		if (isConnected()) sendData("s" + index +"\n");
	}
	
	public void sendMove(int move) {
		if (isConnected()) sendData("m" + move + "\n");
	}

	private void sendData(String data) {
		try {
			out.write(data.getBytes("ascii"));
		} catch (IOException e) {
			System.err.println("Error in write");
		}
	}

	public void sendGrab() {
		if (isConnected()) sendData("G");
	}
	
	public void sendRelease() {
		if (isConnected()) sendData("R");
	}
}
