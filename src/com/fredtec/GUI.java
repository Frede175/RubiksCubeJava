package com.fredtec;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static org.opencv.core.Core.inRange;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by fsr19 on 4/25/2017.
 */
public class GUI extends JFrame implements Runnable {

	private Arduino arduino;
	
	
	private int scale = 7;

	private int iLowH = 0;
	private int iHighH = 179;

	private int iLowS = 0;
	private int iHighS = 255;

	private int iLowV = 0;
	private int iHighV = 255;
	
	
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
	
	
	
	public GUI(Arduino arduino) {
		this.arduino = arduino;
		setupFrame();
		
	}

	private boolean scanning = false;
	private int scanningIndex = 0;
	private int sideIndex = 0;
	private CubeSide[][] scanCubes = new CubeSide[6][NUMBER_OF_FRAMES_TO_SCAN];

	private String solution;
	private boolean hasSolution = false;
	private boolean searching = false;
	
	private JButton startScan;
	private JButton nextSide;
	private JButton stopScan;

	private JTextField test;

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
	
	private void setupFrame() {
		setName("Rubiks cube software");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		setSize(new Dimension(1580,1024));
		setResizable(false);
		
		//Connect to camera:
		cam = new VideoCapture();
		cam.open(0);
		
		if (cam.isOpened()) {
			usesCamera = true;
				//Need to calculate

			cam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,1280);
			cam.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);
			camWidth = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
			camHeight = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
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
		startScan.setSize(new Dimension(150, 30));
		startScan.setLocation(1300, 30);
		startScan.addActionListener(e ->  {
			scanning = true;
			scanningIndex = 0;
			sideIndex = 0;
			startScan.setEnabled(false);
			hasSolution = false;
		});
		
		add(startScan);

		nextSide = new JButton("Next side!");
		nextSide.setSize(new Dimension(150, 30));
		nextSide.setLocation(1300, 75);
		nextSide.addActionListener(e ->  {
			scanning = true;
		});

		add(nextSide);
		
		test = new JTextField();

		test.addFocusListener(new focusEvent(0,0, 255));
		
		
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
						
						if (scanning) {
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
									nextSide.setEnabled(false); //TODO temp
									startScan.setEnabled(true);
									searching = true;
									Solver.SolveCubeAsync(previewCubeSides, this);



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

				repaint();
			}
		}
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

		Mat imgThreshold = new Mat();

		inRange(imgHSV, new Scalar(iLowH, iLowS, iLowV), new Scalar(iHighH, iHighS, iHighV), imgThreshold);

		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				int index = (y + 1) * 3 + x + 1;
				Rect rect = getRectFromCenter(getCenter(x, y));
				Scalar avghsv = Core.mean(new Mat(imgHSV, rect));
				Scalar avgbgr = Core.mean(new Mat(image, rect));
				arr[index] = getColor(avghsv, avgbgr);
			}
		}

		return arr;
		
	}
	
	private int getColor(Scalar hsv, Scalar bgr) {

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
		} else if (hasSolution) {
			g2d.drawString("Solution: " + solution, startX, 20);
		} else if (scanning) {
			g2d.drawString("Scanning face: " + Solver.dectionOrder[sideIndex].getValue(), startX, 20);
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
	}

	public void searchComplete() {
		searching = false;
	}
}


class focusEvent implements FocusListener {

	int j, i, maxValue;

	public focusEvent(int j, int i, int maxValue) {
		this.j = j;
		this.i = i;
		this.maxValue = maxValue;
	}

	@Override
	public void focusGained(FocusEvent e) {

	}

	@Override
	public void focusLost(FocusEvent e) {

	}
}
