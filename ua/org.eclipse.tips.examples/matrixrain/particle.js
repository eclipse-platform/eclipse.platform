/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
// Copyright 2017 Wim Jongman
function Particle() {

	this.init = function(first) {
		this.fTrail = new Array();
		this.fChar = new Array();
		this.fOpac = new Array();
		this.scalar = 10;
		this.fForce = createVector(0, this.scalar);
		if (first) {
			this.active = Math.floor(random(50));
		}
		this.alive = 0;
		this.laneNumber = this.getFreeLane();
		lanes[this.laneNumber] = true;
		this.theY = 0;
		if (first) {
			this.theY = Math.floor(random(height / 2));
		}
		this.pos = createVector(this.laneNumber * this.scalar, this.theY);
		this.vel = createVector(0, 0);
		this.acc = createVector(0, 0);
		this.prv = createVector(0, 0);
		this.speed = Math.floor(random(10) + 1);
		this.char = this.getRandomChar();
		this.fTrail[this.fTrail.length] = this.pos;
		this.fChar[this.fChar.length] = this.char;
		this.fOpac[this.fOpac.length] = 255;
	}

	this.getRandomChar = function() {
		if (Math.round(random()) == 1) {
			return char(Math.floor(12458 + random(74)));
		}
		return char(Math.floor(47 + random(43)));
	}

	/**
	 * Become active after a while.
	 */
	this.isActive = function() {
		if (this.active <= 0) {
			return true;
		}
		this.active--;
		return false;
	}

	this.getFreeLane = function() {
		var numberOfLanes = Math.floor(width / this.scalar);
		var result = Math.floor(random(numberOfLanes));
		while (lanes[result] == true) {
			result = Math.floor(random(numberOfLanes));
		}
		return result;
	}

	this.update = function() {
		// console.log(this.alive, this.fTrail.length, this.pos.y);
		if (this.pos.y > height
				&& (this.alive > 0 && (this.alive + 5) >= this.fTrail.length)) {
			lanes[this.laneNumber] = false;
			this.init(false);
		}

		else if (this.pos.y <= height + 20) {
			var copy = this.pos.copy();
			this.fTrail[this.fTrail.length] = copy;
			this.fChar[this.fChar.length] = this.char;
			this.fOpac[this.fOpac.length] = 200;
			this.vel.add(this.acc);
			this.pos.add(this.vel);
			this.vel.limit(this.speed);
			this.acc.mult(0);
			this.char = this.getRandomChar();
		}
	}

	this.getOpacity = function(pos) {
		var count = this.fOpac[pos];
		this.fOpac[pos] -= this.speed;
		if (this.fTrail[pos].y > height) {
			this.fOpac[pos] = 0;// (this.speed * 100);
		}
		if (count > 0 && this.fOpac[pos] <= 0) {
			this.alive += 2;
		}
		return this.fOpac[pos];
	}

	this.getX = function(pos) {
		return this.fTrail[pos].x;
	}

	this.getY = function(pos) {
		return this.fTrail[pos].y;
	}

	this.getChar = function(pos) {
		if (Math.floor(random(10)) == 5) {
			this.fChar[pos] = this.getRandomChar();
		}
		if (pos == 0) {
			return this.getRandomChar();
		}
		return this.fChar[pos];
	}

	this.applyForce = function() {
		this.acc.add(this.fForce);
	}

	this.init(true);
}