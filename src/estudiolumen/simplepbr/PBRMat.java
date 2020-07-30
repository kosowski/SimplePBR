package estudiolumen.simplepbr;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.opengl.PShader;

public class PBRMat {
	PShader shader;
	PImage albedoTex, metallicTex, roughnessTex, normalTex;
	float metallic;
	float roughness;
	float rim;
		
	public PBRMat(){
		metallic = 1;
		roughness = 1;
		rim = 1f;
		shader = SimplePBR.getPbrShader();
		albedoTex = SimplePBR.getWhiteTexture();
		metallicTex = SimplePBR.getWhiteTexture();
		roughnessTex = SimplePBR.getWhiteTexture();
		normalTex = SimplePBR.getFlatNormalTexture();
	}
	
	public PBRMat( String path){
		this();	
		PImage img = SimplePBR.getPapplet().loadImage(path+"albedo.png"); if(img != null)	albedoTex = img;
		PImage m = SimplePBR.getPapplet().loadImage(path+"metalness.png"); if(m != null) metallicTex = m;
		PImage r  = SimplePBR.getPapplet().loadImage(path+"roughness.png"); if(r!= null) roughnessTex = r;
		PImage n = SimplePBR.getPapplet().loadImage(path+"normal.png"); if(n != null) normalTex = n;
	}
	
	public PBRMat(PBRMat copy){
		this();
		metallic = copy.metallic;
		roughness = copy.roughness;
		rim = copy.rim;
		shader = copy.shader;
		albedoTex = copy.albedoTex;
		metallicTex = copy.metallicTex;
		roughnessTex = copy.roughnessTex;
		normalTex = copy.roughnessTex;
	}
	
	public void bind(){
		bind(SimplePBR.getPapplet().g);
	}
	
	public void bind(PGraphics pg){
		pg.resetShader();
		shader.set("roughnessMap", roughnessTex);
		shader.set("metalnessMap", metallicTex);
		shader.set("albedoTex", albedoTex);	
		shader.set("normalMap", normalTex);
		shader.set("material", metallic, roughness,0f, rim);
		pg.shader(shader);
	}
	

	public PShader getShader() {
		return shader;
	}

	public PBRMat setShader(PShader shader) {
		this.shader = shader;
		return this;
	}
	
	public float getMetallic() {
		return metallic;
	}

	public PBRMat setMetallic(float metallic) {
		this.metallic = metallic;
		return this;
	}

	public float getRougness() {
		return roughness;
	}

	public PBRMat setRougness(float rougness) {
		this.roughness = rougness;
		return this;
	}

	public float getRim() {
		return rim;
	}

	public PBRMat setRim(float rim) {
		this.rim = rim;
		return this;
	}
}
