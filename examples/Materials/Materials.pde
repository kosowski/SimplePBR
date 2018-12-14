import estudiolumen.simplepbr.*;

PBRMat mat, mat2, mat3;

void setup() {
  size(1080, 1080, P3D);

  String path = sketchPath()+"/../";
  println(path);
  SimplePBR.init(this, path+"data/textures/cubemap/Zion_Sunsetpeek");

  mat = new PBRMat(path+"data/textures/material/Wood_006/");
  mat2 = new PBRMat(path+"data/textures/material/Metal_01/");
  mat3 = new PBRMat();
  noStroke();
}

void draw() {
  
  noLights();

  translate(width/2, height/2, 0);

  pushMatrix();
  rotateY(PI); // Just because I like more this side of the cubemap as background
  background(30);
  blendMode(ADD);
  SimplePBR.drawCubemap(g, 2000); 
  blendMode(BLEND);
  popMatrix();

  pointLight(255, 255, 255, 1000, 1000, 1000);
  pointLight(255, 255, 255, -1000, -1000, 1000);

  rotateY(frameCount * 0.01f);

  //wood   sphere
  pushMatrix();
  translate(-400, 0, 0);	
  mat.bind();
  sphere(180);
  popMatrix();

  //metal sphere
  pushMatrix();
  translate(400, 0, 0);
  mat2.bind();
  sphere(180);
  popMatrix();

  // sphere with no textures
  // No textures material, roughness, metalness and color must be set manually
  mat3.setRougness(map(mouseX, 0, width, 0.001f, 1f));
  mat3.setMetallic(map(mouseY, 0, height, 0f, 1f));
  fill(200); //
  mat3.bind();
  sphere(180);
}