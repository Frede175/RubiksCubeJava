package com.fredtec;

import java.awt.*;

public class CubeSide {
	public Solver.Sides side;
	public Solver.Colors colors[] = new Solver.Colors[9];
	
	public CubeSide() {
		
	}
	
	public CubeSide(int[] colors) {
		if (colors.length != 9) return;
		for (int i = 0; i < 9; i++) {
			switch (colors[i]) {
				case 0:
					this.colors[i] = Solver.Colors.WHITE;
					break;
				case 1:
					this.colors[i] = Solver.Colors.RED;
					break;
				case 2:
					this.colors[i] = Solver.Colors.ORANGE;
					break;
				case 3:
					this.colors[i] = Solver.Colors.GREEN;
					break;
				case 4:
					this.colors[i] = Solver.Colors.BLUE;
					break;
				case 5:
					this.colors[i] = Solver.Colors.YELLOW;
					break;
			}
		}
	}
}
