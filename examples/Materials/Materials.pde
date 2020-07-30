/**
 * SimplePBR Basic use
 *
 * Move the mouse to modify center sphere's material
 *
 * by Nacho Cossio 2018
 * www.nachocossio.com (@nacho_cossio)
 *
 */

import estudiolumen.simplepbr.*;

PBRMat mat, mat2, mat3;

void setup() {
	size(800, 800, P3D);
	sphereDetail(20);
	// Path to common data folder
	String path = sketchPath("../data/textures/");

	// Init SimplePBR providing path to folder with cubemap, radiance and irrandiance textures
	SimplePBR.init(this, path + "cubemap/Zion_Sunsetpeek");
	SimplePBR.setExposure(2.6f); // simple exposure control

	// Create PBR materials from a set of textures
	mat = new PBRMat(path + "material/Leather_008_SD/");
	mat2 = new PBRMat(path + "material/Metal_Rusted_006/");
	// Create a textureless material controlled just by metallic and roughness parameters
	mat3 = new PBRMat();
	noStroke();
}

void draw() {
	background(0);
	noLights();
	
	translate(width/2, height/2,0);
	
	pushMatrix();
	rotateY(PI); // Just because I like more this side of the cubemap as background
	SimplePBR.drawCubemap(this.g, 800);
	popMatrix();
	
	pointLight(255, 255, 255, 1000, 1000, 1000);
	pointLight(255, 255, 255, -1000, -1000, 1000);
	
	// wood sphere
	pushMatrix();
	translate(-250, 0,0);	
	rotateY(frameCount * 0.01f);
	mat.bind();
	sphere(120);
	popMatrix();
	
	//metal sphere
	pushMatrix();
	translate(250, 0,0);
	rotateY(frameCount * 0.01f);
	
	mat2.bind();
	sphere(120);
	popMatrix();
	
	// sphere with no textures
	// No textures material, roughness, metalness and color must be set manually
	mat3.setRougness(map(mouseX,0,width,0f,1f));
	mat3.setMetallic(map(mouseY,0,height,0f,1f));
	fill(200); //
	mat3.bind();
	sphere(120);
}
