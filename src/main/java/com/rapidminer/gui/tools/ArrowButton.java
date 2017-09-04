/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.gui.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;


/**
 * 
 * @author Simon Fischer
 */
public class ArrowButton extends JButton {

	private static final long serialVersionUID = -355433594066885069L;

	protected int direction = SwingConstants.EAST;

	public ArrowButton(int direction) {
		super();
		setText(" ");
		this.direction = direction;
	}

	public ArrowButton(Action a) {
		this(a, SwingConstants.EAST);
	}

	public ArrowButton(Action a, int direction) {
		super(a);
		setText(" ");
		this.direction = direction;
	}

	public void setDirection(int dir) {
		this.direction = dir;
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		GeneralPath arrow = new GeneralPath();
		int w, h;
		switch (direction) {
			case SwingConstants.SOUTH:
				h = 2;
				w = 4;
				arrow.moveTo(getWidth() / 2 - w, getHeight() / 2);
				arrow.lineTo(getWidth() / 2 + w, getHeight() / 2);
				arrow.lineTo(getWidth() / 2, getHeight() / 2 + 2 * h);
				arrow.closePath();
				break;
			case SwingConstants.EAST:
				h = 4;
				w = 2;
				arrow.moveTo(getWidth() / 2 - w, getHeight() / 2 - h);
				arrow.lineTo(getWidth() / 2 + w, getHeight() / 2);
				arrow.lineTo(getWidth() / 2 - w, getHeight() / 2 + h);
				arrow.closePath();
				break;
			default:
				throw new IllegalArgumentException("Illegal direction: " + direction);
		}
		if (isEnabled()) {
			g.setColor(Color.BLACK);
		} else {
			g.setColor(Color.GRAY);
		}
		((Graphics2D) g).fill(arrow);
	}

}
