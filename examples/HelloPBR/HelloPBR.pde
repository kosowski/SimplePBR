import estudiolumen.simplepbr.*;

  PBRMat mat;

  public void setup() {
    size(1080, 1080, P3D);
    sphereDetail(20);

    String path = sketchPath()+"/../";
    //println(path);
    SimplePBR.init(this, path+"data/textures/cubemap/Zion_Sunsetpeek"); // init PBR setting processed cubemap
    SimplePBR.setExposure(1.2f); // simple exposure control
    
    mat = new PBRMat(path+"data/textures/material/Metal_Rusted_006/");

    noStroke();
  }

  public void draw() {
    
    noLights();
    pushMatrix();
    translate(width/2, height/2,0);
    rotateY(PI);
    // additive blend to lighten up the background cubemap
    background(40);
    blendMode(ADD);
    SimplePBR.drawCubemap(g, 8000); 
    blendMode(BLEND);
    popMatrix();
    
    
    fill(255);
    directionalLight(200, 200, 200, 0.8f, 0.8f, -0.6f);
    directionalLight(255, 255, 255, 0, -0.2f, 1);
//    pointLight(100, 100, 100, -200, 0, 200);
    directionalLight(120, 120, 120, -1f, -0.8f, -0.6f);
//    directionalLight(255, 255, 255, 0, 0, -1);

    int numRows = 8;
    float separation = width / numRows;
    float inc = 1f / numRows;

    translate(separation/2, height/4, -200);
    for(int i=0; i<numRows;i++){
      for(int j=0; j<numRows;j++)
      {
        pushMatrix();

        mat.setRougness(j*inc + 0.01f);
        mat.setMetallic(i*inc );
        mat.bind();
        float amp =  sin(i * PI/6f + j * 0.4f +frameCount * 0.02f);
        translate(i*separation , j*separation/2f + amp*120f, 60 * j);
        rotateY(frameCount *0.02f);
        sphere(separation/2 );                                                                                               
        popMatrix();
      }
    }

  }