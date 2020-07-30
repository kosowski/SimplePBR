/**
 * SimplePBR Material showcase
 *
 * by Nacho Cossio 2018
 * www.nachocossio.com (@nacho_cossio)
 *
 */

import estudiolumen.simplepbr.*;

PBRMat mat;

public void setup() {
	size(800, 800, P3D);
	sphereDetail(20);

	// Path to common data folder
	String path = sketchPath("../data/textures/");

	// Init SimplePBR providing path to folder with cubemap, radiance and irrandiance textures
	SimplePBR.init(this, path + "cubemap/Zion_Sunsetpeek"); // init PBR setting processed cubemap
	SimplePBR.setExposure(2.6f); // simple exposure control

	// Create PBR material from a set of textures
	mat = new PBRMat(path + "material/Metal_Rusted_006/");

	noStroke();
}

public void draw() {

	noLights();
	pushMatrix();
	translate(width/2, height/2,0);
	rotateY(PI);
	// Additive blend to lighten the background, not really needed
	background(40);
	blendMode(ADD);
	SimplePBR.drawCubemap(g, 800); 
	blendMode(BLEND);
	popMatrix();

	// Three lights set up    
	directionalLight(200, 200, 200, 0.8f, 0.8f, -0.6f);
	directionalLight(255, 255, 255, 0, -0.2f, 1);
	directionalLight(120, 120, 120, -1f, -0.8f, -0.6f);

	fill(255);

	// Draw spheres, gradually increasing the roughness and metallic parameters
	int numRows = 8;
	float separation = width / numRows;
	float inc = 1f / numRows;

	translate(separation/2, height/4, -200);
	for(int i=0; i<numRows; i++) {
		for(int j=0; j<numRows; j++) {
			pushMatrix();

			mat.setRougness(j*inc + 0.01f);
			mat.setMetallic(i*inc );
			mat.bind();
			float amp =  sin(i * PI/6f + j * 0.4f +frameCount * 0.02f);
			translate(i*separation , j*separation/2f + amp*120f, 60 * j);
			rotateY(frameCount *0.02f);
			sphere(separation/2);                                                                                               
			popMatrix();
		}
	}
}
