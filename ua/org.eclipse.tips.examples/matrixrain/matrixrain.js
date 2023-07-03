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
// var nice = [200, 200, 0.02, 0.002, 10, 2000, 4];
// var nice = [7280, 4320 , 0.02, 0.002, 10, 2000, 4];
var nice = [ 1440, 900, 0.02, 0.002, 20, 50, 4 ];
// var nice = [600, 600, 0.02, 0.002, 50, 1000, 3];
var myX;
var myY;
var inc;
var incTime;
var scl;
var parts;
var magnitude;

var fr;
var time = 0;
var iteration = 0;
var counter = 0;

var particles = [];

var flowField;

var lanes = new Array();

var pg;

function setup() {

	myX = nice[0];
	myY = nice[1];
	inc = nice[2];
	incTime = nice[3];
	scl = nice[4];
	parts = nice[5];
	magnitude = nice[6];

	createCanvas(window.innerWidth, window.innerHeight);
	document.body.scrollTop = 0; // <-- pull the page back up to the top
	document.body.style.overflow = 'hidden'; // <-- relevant addition

	pixelDensity(1);
	generateParticles();
}

function draw() {
	background(0, 120);
	updateParticles();
}

function generateParticles() {
	for (var i = 0; i < parts; i++) {
		particles[i] = new Particle();
	}
}

function updateParticles() {
	textSize(12);
	textStyle(BOLD);
	for (var i = 0; i < parts; i++) {
		p = particles[i];
		if (p.isActive()) {
			p.applyForce();
			p.update();
			show(p);
		}
	}
}

function show(particle) {
	doShow(particle, 0);
	for (var i = 1; i < particle.fTrail.length; i++) {
		doShow(particle, i, particle.fTrail.length);
	}
}

function doShow(particle, pos, len) {
	if (pos == 0) {
		fill(200, 255, 200, 255);
	} else if ((len - pos) < 4) {
		fill(255 - (len - pos) * 50, 255, 255 - (len - pos) * 50,
				200 - (len - pos) * 20);
	} else {
		fill(0, 255, 0, particle.getOpacity(pos));
	}
	text(particle.getChar(pos), particle.getX(pos), particle.getY(pos));
}