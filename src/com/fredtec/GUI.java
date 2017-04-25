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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static org.opencv.core.Core.inRange;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by fsr19 on 4/25/2017.
 */
public class GUI extends JFrame implements Runnable {
	
	private Solver solver;
	private Arduino arduino;
	
	
	private int scale = 7;

	private int iLowH = 0;
	private int iHighH = 179;

	private int iLowS = 0;
	private int iHighS = 255;

	private int iLowV = 0;
	private int iHighV = 255;
	
	
	private static Scalar targetColorsBGR[] = {
		new Scalar(255,255,255),
		new Scalar(0,0,255),
		new Scalar(0, 128, 255),
		new Scalar(0, 255, 255),
		new Scalar(255, 0, 0), 
		new Scalar(0, 255, 255)
	};
	
	private static Scalar targetColors[] = {
		new Scalar(180, 0, 255),
		new Scalar(0, 255, 255),
		new Scalar(15, 255, 255),
		new Scalar(50, 255, 255),
		new Scalar(120, 255, 255),
		new Scalar(30, 255, 255)
	};
	
	
	
	
	public GUI(Solver solver, Arduino arduino) {
		this.solver = solver;
		this.arduino = arduino;
		
		setupFrame();
		
	}
	
	
	private JLabel camera;
	Mat camFrame;
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
			int w = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
			if (w != camWidth) {
				//Need to calculate
				int h = (int)cam.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
				int ratio = w/h;
				cam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,camWidth);
				camHeight = camWidth * ratio;
				cam.set(Videoio.CAP_PROP_FRAME_HEIGHT, camHeight);
			}
		}
		
		camera = new JLabel();
		camera.setSize(new Dimension(1580,1024));
		
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
		
		
		setVisible(true);
	}
	
	
	
	@Override
	public void run() {
		long lastDraw = 0;
		while (true) {
			long now = System.currentTimeMillis();
			if (now - lastDraw >= 1000/30) {
				lastDraw = now;
				System.out.println("Draw");
				//Draw
				if (usesCamera) {
					if (cam.read(camFrame)) {
						int[] arr = getColors(camFrame);
						for (int y = -1; y <= 1; y++) {
							for (int x = -1; x <= 1; x++) {
								int index = (y + 1) * 3 + x + 1;
								Rect rect = getRectFromCenter(getCenter(x, y));
								if (arr[index] < 6) Imgproc.rectangle(camFrame, rect.tl(), rect.br(), targetColorsBGR[arr[index]], -1);
							}
						}
						camera.setIcon(new ImageIcon(convertToBufferedImage(camFrame)));
					}
				}
				
				
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


		
		//for (int i = 0; i < 6; i++) {
		//cv::inRange(imgHSV, cv::Scalar(colorBonds[i][0][0], colorBonds[i][0][1], colorBonds[i][0][2]), cv::Scalar(colorBonds[i][1][0], colorBonds[i][1][1], colorBonds[i][1][2]), imgThreshold);
		/*for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				int index = (y + 1) * 3 + x + 1;
				imshow(colorStrings[i], imgThreshold);
				cv::Rect rect = getRectFromCenter(getCenter(x, y));
				cv::Mat mask = imgThreshold(rect);
				cv::Scalar avg = cv::mean(mask);
				if (avg.val[0] > 20 && colors[index] == 255) {
					colors[index] = i;
				}

			}
		}*/

		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				int index = (y + 1) * 3 + x + 1;

				//imshow(colorStrings[i], imgThreshold);
				Rect rect = getRectFromCenter(getCenter(x, y));
				Scalar avghsv = Core.mean(imgHSV.submat(rect));
				Scalar avgbgr = Core.mean(image.submat(rect));
				arr[index] = getColor(avghsv, avgbgr);

				//if (x == 0 && y == 0) std::cout << "H: " << avghsv.val[0] << "S: " << avghsv.val[1] << "V: " << avghsv.val[2] << "\n";


				//std::cout << index << "\n";
			}
		}




		//}
	/*


	for (int i = 0; i < 9; i++) {
		if (colors[i] != 255) {
			std::cout << "I:" << i << " color: " << colorStrings[colors[i]] << "\n";
		}
	}
	*/
		return arr;
		
	}
	
	private int getColor(Scalar hsv, Scalar bgr) {
	/*
	std::cout << round((float)bgr.val[0] / (float)bgr.val[1]) << "\n";
	if (round((float)bgr.val[0] / (float)bgr.val[2]) > 2 && round((float)bgr.val[0] / (float)bgr.val[1]) >= 2) return 4; //BLUE
	else if (round((float)bgr.val[1] / (float)bgr.val[2]) > 2) return 3; //GREEN

	if (hsv.val[0] > 150) return 1; //RED
	else if (hsv.val[0] < 20 && hsv.val[1] < 150) return 0; //WHITE
	else if (hsv.val[0] < 20) return 2; //ORANGE
	else if (hsv.val[0] < 50) return 5; //YELLOW


	return 255;
	*/
/*
	cv::Scalar rgb(bgr[2], bgr[1], bgr[0]);

	for (int i = 0; i < 6; i++) {
		if (hsv[0] > cColors[i][0][0] && hsv[0] < cColors[i][1][0] && hsv[1] > cColors[i][0][1] && hsv[1] < cColors[i][1][1] && hsv[2] > cColors[i][0][2] && hsv[2] < cColors[i][1][2]) return i;
	}

	*/


		int index = 255;
		int min_d = 0;


		for (int i = 0; i < 6; i++) {
			int d = (int)((hsv.val[0] - targetColors[i].val[0])*(hsv.val[0] - targetColors[i].val[0])) +
					(int)((hsv.val[1] - targetColors[i].val[1])*(hsv.val[1] - targetColors[i].val[1])) +
					(int)((hsv.val[2] - targetColors[i].val[2])*(hsv.val[2] - targetColors[i].val[2]));


			if (d < min_d || index == 255) {
				index = i;
				min_d = d;
			}
		}

		System.out.println("Color index: " + index);

		return index;
	}
}
