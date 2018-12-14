package estudiolumen.simplepbr;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.opengl.PShader;



public class PBRMat {
	PShader shader;
	PImage albedoTex, metallicTex, roughnessTex;
	float metallic;
	float roughness;
	float rim;
		
	public PBRMat(){
		metallic = 0;
		roughness = 1;
		rim = 0f;
		shader = SimplePBR.getPbrShaderNoMaps();
	}
	
	public PBRMat( String path){
		this();
		shader = SimplePBR.getPbrShader();
		albedoTex = SimplePBR.getPapplet().loadImage(path+"albedo.png");
		metallicTex = SimplePBR.getPapplet().loadImage(path+"metalness.png");
		roughnessTex = SimplePBR.getPapplet().loadImage(path+"roughness.png");
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
	}
	
	public void bind(){
		bind(SimplePBR.getPapplet().g);
	}
	
	public void bind(PGraphics pg){
		pg.resetShader();
		if(albedoTex != null){
			shader.set("roughnessMap", roughnessTex);
			shader.set("metalnessMap", metallicTex);
			shader.set("albedoTex", albedoTex);
		}
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
