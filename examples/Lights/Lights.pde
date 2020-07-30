/**
 * SimplePBR light integration example
 *
 * by Nacho Cossio 2020
 * www.nachocossio.com (@nacho_cossio)
 *
 */

import estudiolumen.simplepbr.*;

  PBRMat mat;
  PShape ico;

  PVector[] colors = {new PVector(202,50,50), new PVector(0,189,189)};
  PVector[] lightPositions = new PVector[8]; // Processing support 8 lights
  
  public void setup(){
    size(800, 800, P3D);
  
    // Path to common data folder
    String path = sketchPath("../data/");  
    
    SimplePBR.init(this, path + "textures/cubemap/Zion_Sunsetpeek"); // init PBR setting processed cubemap

    mat = new PBRMat(path + "textures/material/Metal10/");
    mat.setMetallic(1.5);
    noStroke();
    
    ico = loadShape(path+"models/platonic.obj");
    for(int i=0;i <8;i++) 
      lightPositions[i] = new PVector();
  }
  
  public void draw(){
    background(0);
    
    translate(width/2, height/2,0);
    rotateY(frameCount * 0.01f);
    
    resetShader();
    fill(255);
    
    noLights();
    lightFalloff(0f, 0.0001f, 0.00001f);
    
    //Draw lights positions as spheres
    float radius = 300;
    for(int i=0;i <8;i++) {
      float angle =TWO_PI/8f *i;
      lightPositions[i].x = radius * sin(angle);
      lightPositions[i].z = radius * cos(angle);
      lightPositions[i].y = 140f*sin(angle + frameCount*0.03f);
      
      pushMatrix();
      translate(lightPositions[i].x, lightPositions[i].y, lightPositions[i].z);
      sphere(4);
      popMatrix();
    }
    
    for(int i=0;i <8;i++) {
      PVector lightColor = colors[i%2];
      pointLight(lightColor.x,lightColor.y, lightColor.z, lightPositions[i].x, lightPositions[i].y, lightPositions[i].z);
    }
    
    mat.bind();
    rotateX(PI/4f);
    scale(2);
    shape(ico);
    
    surface.setTitle("Framerate: " + frameRate) ;
  }
