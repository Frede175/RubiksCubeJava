package com.fredtec;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.TooManyListenersException;

import static org.opencv.core.Core.findNonZero;
import static org.opencv.core.Core.inRange;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by fsr19 on 4/25/2017.
 */
public class GUI extends JFrame implements Runnable {

	private Arduino arduino;
	
	private final static int scale = 7;
	
	private static Scalar targetColorsBGR[] = {
		new Scalar(255,255,255), //White
		new Scalar(0,0,255), //Red
		new Scalar(0, 128, 255), //Orange
		new Scalar(0, 255, 0), //Green
		new Scalar(255, 0, 0), //Blue
		new Scalar(0, 255, 255) //Yellow
	};
	
	private Scalar targetColors[] = {
		new Scalar(180, 0, 255), //White
		new Scalar(180, 255, 255), //Red
		new Scalar(15, 255, 255), //Orange
		new Scalar(75, 255, 255), //Green
		new Scalar(145, 255, 255), //Blue
		new Scalar(30, 255, 255) //Yellow
	};

	private static int NUMBER_OF_FRAMES_TO_SCAN = 30*2;

	private boolean scanning = false;
	private int scanningIndex = 0;
	private int sideIndex = 0;
	private CubeSide[][] scanCubes = new CubeSide[6][NUMBER_OF_FRAMES_TO_SCAN];

	private String solution;
	private boolean hasSolution = false;
	private boolean searching = false;
	
	private JButton startScan;
	private JButton STOP;
	private JButton RESUME;
	private JButton connect;
	private JButton openCam;
	
	private JLabel connectStatus;
	
	private final static int MAX_VALUE_HUE = 180;
	private final static int MAX_VALUE_SAT = 255;
	private final static int MAX_VALUE_VAL = 255;
	 
	private JSlider whiteH;
	private JSlider whiteS;
	private JSlider whiteV;
	
	private JSlider green;
	private JSlider blue;
	private JSlider red;
	private JSlider orange;
	private JSlider yellow;
	
	private JLabel whitePreview;
	private JLabel greenPreview;
	private JLabel bluePreview;
	private JLabel redPreview;
	private JLabel orangePreview;
	private JLabel yellowPreview;

	private JLabel previewSides;
	private CubeSide[] previewCubeSides = {
			new CubeSide(new int[] {4,4,4,4,4,4,4,4,4}), //U: Blue
			new CubeSide(new int[] {0,0,0,0,0,0,0,0,0}), //F: White
			new CubeSide(new int[] {3,3,3,3,3,3,3,3,3}), //D: Green
			new CubeSide(new int[] {2,2,2,2,2,2,2,2,2}), //L: Orange
			new CubeSide(new int[] {5,5,5,5,5,5,5,5,5}), //B: Yellow
			new CubeSide(new int[] {1,1,1,1,1,1,1,1,1})  //R: Red
	};
	
	private JLabel camera;
	Mat camFrame = new Mat();
	private VideoCapture cam;
	private boolean usesCamera = false;
	private int camWidth = 1280, camHeight = 1024;
	
	private boolean scanMove = false;
	private boolean moveMove = false;
	
	private boolean finishedSolution = false;
	
	private boolean waitingOnDone = false;
	private int solutionIndex = 0;
	
	
	public GUI(Arduino arduino) {
		this.arduino = arduino;
		setupFrame();

	}
		
	private void setupFrame() {
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (arduino.isConnected()) {
					arduino.disconnect();
				}
				if (usesCamera) {
					cam.release();
				}
			}
		});
		
		
		
		
		
		setTitle("Rubiks cube software");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		setSize(new Dimension(1580,1024));
		setResizable(false);
		
		//Connect to camera:
		cam = new VideoCapture();
		cam.open(0);
		
		if (cam.isOpened()) {
			usesCamera = true;
			cam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,1280);
			cam.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);
			camWidth = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
			camHeight = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
			//cam.set(Videoio.CAP_PROP_CONTRAST, 7000);
			//camera.set(cv::CAP_PROP_ISO_SPEED, 255);
			//cam.set(Videoio.CAP_PROP_SATURATION, 7400);
			//cam.set(15, -8.0);


			cam.set(Videoio.CAP_PROP_SETTINGS, 0);


		}
		
		camera = new JLabel();
		camera.setSize(new Dimension(1280,720));
		
		if (usesCamera) {
			cam.read(camFrame);

			int[] arr = getColors(camFrame);
			for (int y = -1; y <= 1; y++) {
				for (int x = -1; x <= 1; x++) {
					int index = (y + 1) * 3 + x + 1;
					Rect rect = getRectFromCenter(getCenter(x, y));
					if (arr[index] < 6) Imgproc.rectangle(camFrame, rect.tl(), rect.br(), targetColorsBGR[arr[index]], -1);
				}
			}
			
			camera.setIcon(new ImageIcon(convertToBufferedImage(camFrame)));
		} else {
			BufferedImage bi = new BufferedImage(camWidth, camHeight, BufferedImage.TYPE_3BYTE_BGR);
			camera.setIcon(new ImageIcon(bi));
		}
		add(camera);

		previewSides = new JLabel();
		previewSides.setSize(new Dimension(1280,1280-720));
		previewSides.setLocation(0, 720);

		drawPreviewCubeSides();

		add(previewSides);
		
		startScan = new JButton("Start scan!");
		startScan.setSize(new Dimension(120, 30));
		startScan.setLocation(1300, 15);
		startScan.addActionListener(e ->  {
			scanning = true;
			scanningIndex = 0;
			sideIndex = 0;
			startScan.setEnabled(false);
			hasSolution = false;
			scanMove = true;
		});
		startScan.setEnabled(false);
		
		add(startScan);

		openCam = new JButton("Open cam");
		openCam.setSize(new Dimension(120, 30));
		openCam.setLocation(1430, 15);
		openCam.addActionListener(e ->  {
			String index = JOptionPane.showInputDialog(this, "Index of camera? (number)", "0");
			try {
				int c = Integer.parseInt(index);
				cam.release();
				usesCamera = false;
				cam.open(c);

				if (cam.isOpened()) {
					usesCamera = true;
					cam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,1280);
					cam.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);
					camWidth = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
					camHeight = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
					//cam.set(Videoio.CAP_PROP_CONTRAST, 7000);
					//camera.set(cv::CAP_PROP_ISO_SPEED, 255);
					//cam.set(Videoio.CAP_PROP_SATURATION, 7400);
					//cam.set(15, -8.0);
					cam.set(Videoio.CAP_PROP_SETTINGS, 0);
				}
			} catch (NumberFormatException error) {
				JOptionPane.showMessageDialog(this, "Not a number!", "Error", JOptionPane.ERROR_MESSAGE);
			}
			
		});

		add(openCam);
		
		

		STOP = new JButton("STOP");
		STOP.setSize(new Dimension(120, 30));
		STOP.setLocation(1300, 60);
		STOP.addActionListener(e ->  {
			arduino.sendStop();
		});

		add(STOP);

		RESUME = new JButton("RESUME");
		RESUME.setSize(new Dimension(120, 30));
		RESUME.setLocation(1430, 60);
		RESUME.addActionListener(e ->  {
			arduino.sendResume();
		});

		add(RESUME);
		
		//Sliders:
		
		/* ------ WHITE ------ */
		
		JLabel whiteLabel = new JLabel("White HSV Color:");
		whiteLabel.setLocation(1300, 170);
		whiteLabel.setSize(new Dimension(150, 20));
		add(whiteLabel);
		
		whitePreview = new JLabel();
		whitePreview.setOpaque(true);
		whitePreview.setBackground(Color.getHSBColor((float)targetColors[0].val[0] / 180, (float)targetColors[0].val[1] / 255, (float)targetColors[0].val[2] / 255));
		whitePreview.setLocation(1475, 165);
		whitePreview.setSize(new Dimension(30,30));
		add(whitePreview);
		
		whiteH = new JSlider();
		whiteH.setLocation(1300, 200);
		whiteH.setSize(new Dimension(250, 50));
		whiteH.setMaximum(MAX_VALUE_HUE);
		whiteH.setMinorTickSpacing(10);
		whiteH.setMajorTickSpacing(90);
		whiteH.setPaintTicks(true);
		whiteH.setPaintLabels(true);
		whiteH.setValue((int)targetColors[0].val[0]);
		whiteH.addChangeListener(e -> {
			targetColors[0].val[0] = whiteH.getValue();
			whitePreview.setBackground(Color.getHSBColor((float)targetColors[0].val[0] / 180, (float)targetColors[0].val[1] / 255, (float)targetColors[0].val[2] / 255));
		});
		
		add(whiteH);

		whiteS = new JSlider();
		whiteS.setLocation(1300, 260);
		whiteS.setSize(new Dimension(250, 50));
		whiteS.setMaximum(MAX_VALUE_SAT);
		whiteS.setMinorTickSpacing(10);
		whiteS.setMajorTickSpacing(50);
		whiteS.setPaintTicks(true);
		whiteS.setPaintLabels(true);
		whiteS.setValue((int)targetColors[0].val[1]);
		whiteS.addChangeListener(e -> {
			targetColors[0].val[1] = whiteS.getValue();
			whitePreview.setBackground(Color.getHSBColor((float)targetColors[0].val[0] / 180, (float)targetColors[0].val[1] / 255, (float)targetColors[0].val[2] / 255));
		});

		add(whiteS);

		whiteV = new JSlider();
		whiteV.setLocation(1300, 320);
		whiteV.setSize(new Dimension(250, 50));
		whiteV.setMaximum(MAX_VALUE_VAL);
		whiteV.setMinorTickSpacing(10);
		whiteV.setMajorTickSpacing(50);
		whiteV.setPaintTicks(true);
		whiteV.setPaintLabels(true);
		whiteV.setValue((int)targetColors[0].val[2]);
		whiteV.addChangeListener(e -> {
			targetColors[0].val[2] = whiteV.getValue();
			whitePreview.setBackground(Color.getHSBColor((float)targetColors[0].val[0] / 180, (float)targetColors[0].val[1] / 255, (float)targetColors[0].val[2] / 255));
		});

		add(whiteV);
		
		/* ------ RED ------ */

		JLabel redLabel = new JLabel("Red Hue:");
		redLabel.setLocation(1300, 400);
		redLabel.setSize(new Dimension(150, 20));
		add(redLabel);

		redPreview = new JLabel();
		redPreview.setOpaque(true);
		redPreview.setBackground(Color.getHSBColor((float)targetColors[1].val[0] / 180, (float)targetColors[1].val[1] / 255, (float)targetColors[1].val[2] / 255));
		redPreview.setLocation(1475, 395);
		redPreview.setSize(new Dimension(30,30));
		add(redPreview);

		red = new JSlider();
		red.setLocation(1300, 430);
		red.setSize(new Dimension(250, 50));
		red.setMaximum(MAX_VALUE_HUE);
		red.setMinorTickSpacing(10);
		red.setMajorTickSpacing(90);
		red.setPaintTicks(true);
		red.setPaintLabels(true);
		red.setValue((int)targetColors[1].val[0]);
		red.addChangeListener(e -> {
			targetColors[1].val[0] = red.getValue();
			redPreview.setBackground(Color.getHSBColor((float)targetColors[1].val[0] / 180, (float)targetColors[1].val[1] / 255, (float)targetColors[1].val[2] / 255));
		});

		add(red);
		
		/* ------ Orange ------ */

		JLabel orangeLabel = new JLabel("Orange Hue:");
		orangeLabel.setLocation(1300, 510);
		orangeLabel.setSize(new Dimension(150, 20));
		add(orangeLabel);

		orangePreview = new JLabel();
		orangePreview.setOpaque(true);
		orangePreview.setBackground(Color.getHSBColor((float)targetColors[2].val[0] / 180, (float)targetColors[2].val[1] / 255, (float)targetColors[2].val[2] / 255));
		orangePreview.setLocation(1475, 505);
		orangePreview.setSize(new Dimension(30,30));
		add(orangePreview);

		orange = new JSlider();
		orange.setLocation(1300, 540);
		orange.setSize(new Dimension(250, 50));
		orange.setMaximum(MAX_VALUE_HUE);
		orange.setMinorTickSpacing(10);
		orange.setMajorTickSpacing(90);
		orange.setPaintTicks(true);
		orange.setPaintLabels(true);
		orange.setValue((int)targetColors[2].val[0]);
		orange.addChangeListener(e -> {
			targetColors[2].val[0] = orange.getValue();
			orangePreview.setBackground(Color.getHSBColor((float)targetColors[2].val[0] / 180, (float)targetColors[2].val[1] / 255, (float)targetColors[2].val[2] / 255));
		});

		add(orange);
		
		/* ------ Green ------ */

		JLabel greenLabel = new JLabel("Green Hue:");
		greenLabel.setLocation(1300, 620);
		greenLabel.setSize(new Dimension(150, 20));
		add(greenLabel);

		greenPreview = new JLabel();
		greenPreview.setOpaque(true);
		greenPreview.setBackground(Color.getHSBColor((float)targetColors[3].val[0] / 180, (float)targetColors[3].val[1] / 255, (float)targetColors[3].val[2] / 255));
		greenPreview.setLocation(1475, 615);
		greenPreview.setSize(new Dimension(30,30));
		add(greenPreview);

		green = new JSlider();
		green.setLocation(1300, 650);
		green.setSize(new Dimension(250, 50));
		green.setMaximum(MAX_VALUE_HUE);
		green.setMinorTickSpacing(10);
		green.setMajorTickSpacing(90);
		green.setPaintTicks(true);
		green.setPaintLabels(true);
		green.setValue((int)targetColors[3].val[0]);
		green.addChangeListener(e -> {
			targetColors[3].val[0] = green.getValue();
			greenPreview.setBackground(Color.getHSBColor((float)targetColors[3].val[0] / 180, (float)targetColors[3].val[1] / 255, (float)targetColors[3].val[2] / 255));
		});

		add(green);
		
		/* ------ Blue ------ */

		JLabel blueLabel = new JLabel("Blue Hue:");
		blueLabel.setLocation(1300, 730);
		blueLabel.setSize(new Dimension(150, 20));
		add(blueLabel);

		bluePreview = new JLabel();
		bluePreview.setOpaque(true);
		bluePreview.setBackground(Color.getHSBColor((float)targetColors[4].val[0] / 180, (float)targetColors[4].val[1] / 255, (float)targetColors[4].val[2] / 255));
		bluePreview.setLocation(1475, 725);
		bluePreview.setSize(new Dimension(30,30));
		add(bluePreview);

		blue = new JSlider();
		blue.setLocation(1300, 760);
		blue.setSize(new Dimension(250, 50));
		blue.setMaximum(MAX_VALUE_HUE);
		blue.setMinorTickSpacing(10);
		blue.setMajorTickSpacing(90);
		blue.setPaintTicks(true);
		blue.setPaintLabels(true);
		blue.setValue((int)targetColors[4].val[0]);
		blue.addChangeListener(e -> {
			targetColors[4].val[0] = blue.getValue();
			bluePreview.setBackground(Color.getHSBColor((float)targetColors[4].val[0] / 180, (float)targetColors[4].val[1] / 255, (float)targetColors[4].val[2] / 255));
		});

		add(blue);
		
		/* ------ Yellow ------ */

		JLabel yellowLabel = new JLabel("Yellow Hue:");
		yellowLabel.setLocation(1300, 840);
		yellowLabel.setSize(new Dimension(150, 20));
		add(yellowLabel);

		yellowPreview = new JLabel();
		yellowPreview.setOpaque(true);
		yellowPreview.setBackground(Color.getHSBColor((float)targetColors[5].val[0] / 180, (float)targetColors[5].val[1] / 255, (float)targetColors[5].val[2] / 255));
		yellowPreview.setLocation(1475, 835);
		yellowPreview.setSize(new Dimension(30,30));
		add(yellowPreview);

		yellow = new JSlider();
		yellow.setLocation(1300, 870);
		yellow.setSize(new Dimension(250, 50));
		yellow.setMaximum(MAX_VALUE_HUE);
		yellow.setMinorTickSpacing(10);
		yellow.setMajorTickSpacing(90);
		yellow.setPaintTicks(true);
		yellow.setPaintLabels(true);
		yellow.setValue((int)targetColors[5].val[0]);
		yellow.addChangeListener(e -> {
			targetColors[5].val[0] = yellow.getValue();
			yellowPreview.setBackground(Color.getHSBColor((float)targetColors[5].val[0] / 180, (float)targetColors[5].val[1] / 255, (float)targetColors[5].val[2] / 255));
		});

		add(yellow);

		connect = new JButton("Connect to arduino");
		connect.setBounds(1300, 105, 150, 30);
		connect.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Coonecting!");
				if (arduino.isConnected()) return;
				try {
					arduino.connect();
				} catch (PortInUseException | UnsupportedCommOperationException | TooManyListenersException | IOException e1) {
					System.err.println("Error in connection! " + e1.toString());
				}
				if (!arduino.isConnected()) {
					System.err.println("Failed to connect to Arduino!");
					connectStatus.setText("Error while connection!");
				}
				else {
					connectStatus.setText("Connected!");
					connect.setEnabled(false);
					startScan.setEnabled(true);
				}
			}
		});
		connect.setVisible(true);
		add(connect);

		connectStatus = new JLabel("Disconnected");
		connectStatus.setBounds(1300, 135, 150, 25);
		connectStatus.setVisible(true);
		add(connectStatus);
		
		
		setVisible(true);
	}
	
	
	
	@Override
	public void run() {
		long lastDraw = 0;
		while (true) {
			long now = System.currentTimeMillis();
			if (now - lastDraw >= 1000/30) {
				lastDraw = now;
				//Draw

				if (usesCamera) {
					if (cam.read(camFrame)) {
						int[] arr = getColors(camFrame);
						
						if (scanning && !waitingOnDone && !scanMove) {
							arduino.sendGrab();
							waitingOnDone = true;
							if (scanningIndex == 0) {
								scanCubes = new CubeSide[6][NUMBER_OF_FRAMES_TO_SCAN];
							}
							
							scanCubes[sideIndex][scanningIndex] = new CubeSide(arr);
							scanningIndex++;
							
							
							CubeSide currentSideScan = getAvgColorSide(scanCubes, sideIndex, scanningIndex);
							for (int y = -1; y <= 1; y++) {
								for (int x = -1; x <= 1; x++) {
									int index = (y + 1) * 3 + x + 1;
									Rect rect = getRectFromCenter(getCenter(x, y));
									if (currentSideScan != null && currentSideScan.colors[index] != null) Imgproc.rectangle(camFrame, rect.tl(), rect.br(), targetColorsBGR[currentSideScan.colors[index].ordinal()], -1);
								}
							}

							if (scanningIndex >= NUMBER_OF_FRAMES_TO_SCAN) {
								scanning = false; //Need the arduino to move the cube

								previewCubeSides[sideIndex] = getAvgColorSide(scanCubes, sideIndex, scanningIndex);

								scanningIndex = 0;

								sideIndex++;

								if (sideIndex >= 6) {
									startScan.setEnabled(true);
									searching = false;
									Solver.SolveCubeAsync(previewCubeSides, this);
								} else {
									scanMove = true;
								}

							}
							
							
						} else {
							for (int y = -1; y <= 1; y++) {
								for (int x = -1; x <= 1; x++) {
									int index = (y + 1) * 3 + x + 1;
									Rect rect = getRectFromCenter(getCenter(x, y));
									if (arr[index] < 6) Imgproc.rectangle(camFrame, rect.tl(), rect.br(), targetColorsBGR[arr[index]], -1);
								}
							}
						}
						
						
						camera.setIcon(new ImageIcon(convertToBufferedImage(camFrame)));
					}
				}

				drawPreviewCubeSides();
				
				if (scanMove && !waitingOnDone) {
					scanMove = true;
					waitingOnDone = true;
					arduino.sendScan(sideIndex);
				}
				
				if (hasSolution && !waitingOnDone && !finishedSolution) {
					String array[] = solution.split(" ");
					if (solutionIndex < array.length) {
						int move;
						if ((move = convertToMove(array[solutionIndex])) != -1) {
							arduino.sendMove(move);
							solutionIndex++;
							waitingOnDone = true;
						} else {
							System.err.println("Error: Move had a value of: -1!");
							hasSolution = false;
						}
						
					} else {
						finishedSolution = true;
						arduino.sendRelease();
						waitingOnDone = true;
					}
				}

				repaint();
			}
		}
	}

	private int convertToMove(String s) {
		for (int i = 0; i < Solver.moves.length; i++) {
			if (s.equals(Solver.moves[i])) return i;
		}
		return -1;
	}

	private BufferedImage convertToBufferedImage(Mat image) {
		BufferedImage bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
		byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
		image.get(0, 0, data);
		return bufferedImage;
	}


	private Point getCenter(int x, int y) {
		return new Point(camWidth / 2 + x * 19 * scale, camHeight / 2 + y * 19 * scale);
	}

	Rect getRectFromCenter(Point center) {
		return new Rect(center.x - 6 * scale / 2, center.y - 6 * scale / 2, 6 * scale, 6 * scale);
	}

	private int[] getColors(Mat image) {

		int arr[] = new int[9];
		
		Mat imgHSV = new Mat();
		
		Imgproc.cvtColor(image, imgHSV, Imgproc.COLOR_BGR2HSV_FULL);
		
		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				int index = (y + 1) * 3 + x + 1;
				Rect rect = getRectFromCenter(getCenter(x, y));
				Scalar avghsv = Core.mean(new Mat(imgHSV, rect));
				arr[index] = getColor(avghsv);
			}
		}

		return arr;
		
	}
	
	private int getColor(Scalar hsv) {

		int index = 255;
		int min_d = 0;

		for (int i = 0; i < 6; i++) {
			int d = (int) ((hsv.val[0] - targetColors[i].val[0]) * (hsv.val[0] - targetColors[i].val[0])) +
					(int) ((hsv.val[1] - targetColors[i].val[1]) * (hsv.val[1] - targetColors[i].val[1])) +
					(int) ((hsv.val[2] - targetColors[i].val[2]) * (hsv.val[2] - targetColors[i].val[2]));
			if (d < min_d || index == 255) {
				index = i;
				min_d = d;
			}
		}

		return index;
	}

	private CubeSide getAvgColorSide(CubeSide sides[][], int side, int count) {
		CubeSide color = new CubeSide();

		int w = 0, r = 0, o = 0, g = 0, b = 0, y = 0;

		for (int i = 0; i < 9; i++) {
			w = 0; r = 0; o = 0; g = 0; b = 0; y = 0;

			for (int j = 0; j < count; j++) {
				switch (sides[side][j].colors[i]) {
					case WHITE:
						w++;
						break;
					case RED:
						r++;
						break;
					case ORANGE:
						o++;
						break;
					case GREEN:
						g++;
						break;
					case BLUE:
						b++;
						break;
					case YELLOW:
						y++;
						break;
				}
			}

			int maxColorCount = w;
			Solver.Colors maxColor = Solver.Colors.WHITE;

			if (r > maxColorCount) {
				maxColor = Solver.Colors.RED;
				maxColorCount = r;
			}

			if (o > maxColorCount) {
				maxColor = Solver.Colors.ORANGE;
				maxColorCount = o;
			}

			if (g > maxColorCount) {
				maxColor = Solver.Colors.GREEN;
				maxColorCount = g;
			}

			if (b > maxColorCount) {
				maxColor = Solver.Colors.BLUE;
				maxColorCount = b;
			}

			if (y > maxColorCount) {
				maxColor = Solver.Colors.YELLOW;
				maxColorCount = y;
			}

			color.colors[i] = maxColor;
		}

		return color;

	}

	private void drawPreviewCubeSides() {
		int startY = 92; int startX = 92;
		int cubeWidthHeight = 120;
		int smallCubeSize = 30;
		int space = 50;

		BufferedImage bufferedImage = new BufferedImage(previewSides.getWidth(), previewSides.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

		Graphics2D g2d = bufferedImage.createGraphics();

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Aerial", Font.BOLD, 24));

		if (searching) {
			g2d.drawString("Solution: Searching!", startX, 20);
		} else if (hasSolution && !finishedSolution) {
			g2d.drawString("Solution: " + solution, startX, 20);
		} else if (scanning && !waitingOnDone) {
			g2d.drawString("Scanning face: " + Solver.dectionOrder[sideIndex].getValue(), startX, 20);
		} else if (waitingOnDone) {
			g2d.drawString("Waiting on ardiuno to rotate/turing cube", startX, 20);
		} else if (finishedSolution) {
			g2d.drawString("Solution: " + solution + ". Cube is solved!", startX, 20);
		}


		for (int i = 0; i < 6; i++) {
			int centerX = cubeWidthHeight / 2 + startX + (cubeWidthHeight + space) * i;
			int centerY = cubeWidthHeight / 2 + startY;

			g2d.setColor(Color.WHITE);

			g2d.drawString(Solver.dectionOrder[i].getValue(), centerX + smallCubeSize / 4, (int)(centerY - smallCubeSize * 2));

			for (int y = -1; y <= 1; y++) {
				for (int x = -1; x <= 1; x++) {
					int index = (y + 1) * 3 + x + 1;
					g2d.setColor(new Color((int)targetColorsBGR[previewCubeSides[i].colors[index].ordinal()].val[2], (int)targetColorsBGR[previewCubeSides[i].colors[index].ordinal()].val[1],(int)targetColorsBGR[previewCubeSides[i].colors[index].ordinal()].val[0]));
					g2d.fillRect((int)(centerX + (smallCubeSize * 1.5 + 6) * x), (int)(centerY + (smallCubeSize * 1.5 + 6) * y), smallCubeSize, smallCubeSize);
				}
			}



		}

		g2d.dispose();

		previewSides.setIcon(new ImageIcon(bufferedImage));

	}

	public void setSolution(String solution) {
		this.solution = solution;
		hasSolution = true;
		solutionIndex = 0;
	}

	public void searchComplete() {
		searching = false;
	}

	public void notifyData(String data) {
		if (data.equals("DONE")) waitingOnDone = false;
	}
}
