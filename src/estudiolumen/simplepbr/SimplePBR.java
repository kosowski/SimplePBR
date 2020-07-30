package estudiolumen.simplepbr;


import java.nio.IntBuffer;

import com.jogamp.opengl.GL3;

import processing.core.*;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

/*
 * SimplePBR Nacho Cossio - www.nachocossio.com
 * https://github.com/kosowski/
 * A Processing library for physically based rendering
 */

public class SimplePBR {
	
	
	public final static String VERSION = "0.3";
	
	private static PApplet papplet;
	private static PShader pbrShader;
	private static PImage whiteTexture, flatNormalTexture;
	private static PImage iblbrdf;
	private static PImage[] cubemapTextures;
	private static int mipLevels = 7;
	private static float exposure = 1f;
	private static float[] shCoeficients;

	public static void init(PApplet thePapplet, String iblpath){
		SimplePBR.papplet = thePapplet;

		PApplet.println("\t", PGraphicsOpenGL.OPENGL_VENDOR);
		PApplet.println("\t", PGraphicsOpenGL.OPENGL_RENDERER);
		PApplet.println("\t", PGraphicsOpenGL.OPENGL_VERSION);
		PApplet.println("\t", PGraphicsOpenGL.GLSL_VERSION);
		welcome();
		
		iblbrdf = thePapplet.loadImage("data/textures/integrateBrdf.png");
		whiteTexture = thePapplet.loadImage("data/textures/white1x1.png");
		flatNormalTexture = thePapplet.loadImage("data/textures/flatNoraml1x1.png");
		
		int enviromentmapTextureUnit = PGL.TEXTURE3+2; //minimum number of texture units is 16
	
		String[] vertSource = thePapplet.loadStrings("data/shaders/pbr/pbr.vert");
		String[] fragSource = thePapplet.loadStrings("data/shaders/pbr/simplepbr.frag");
		pbrShader = new PShader(thePapplet, vertSource, fragSource);
	
		pbrShader.set("mipLevels", mipLevels);
		pbrShader.set("envd", 5);
		pbrShader.set("iblbrdf", iblbrdf);
		pbrShader.set("exposure", exposure);
		
		boolean cubemapSupport = true;
		try{
			thePapplet.g.shader(pbrShader);
		}
		catch (Exception e) 
		{
			System.out.println("No support for cubemaps, falling back to equirectangular maps");
			System.out.println("Shader compiling can take a long time on this graphics card (10-20 secs) while showing blank window, please wait");
			cubemapSupport = false;
			
			// Remove the SAMPLERCUBESUPPORT define
			for(int i=0; i< fragSource.length;i++)
				if(fragSource[i].matches("#define SAMPLERCUBESUPPORT")) {
					fragSource[i] ="";
					break;
				}
			
			pbrShader = new PShader(thePapplet, vertSource, fragSource);	

			pbrShader.set("envd", 5);
			pbrShader.set("mipLevels", mipLevels);
			pbrShader.set("iblbrdf", iblbrdf);
			pbrShader.set("exposure", exposure);
			
			loadSHCoeficients(iblpath+"/sh.txt");
			pbrShader.set("iblSH", shCoeficients, 3);
			
			try {
				thePapplet.g.shader(pbrShader);
			}catch (Exception e2) {
				System.out.println("Ops, that did not work too" );
				e2.printStackTrace();
			}
		}finally 
		{
			if(cubemapSupport)
				cubemapTextures = loadPrefilteredEnviromentMap(thePapplet, enviromentmapTextureUnit, iblpath+"/PREM",
						iblpath+"/irradiance", mipLevels);
			else
				cubemapTextures = loadPrefilteredEnviromentMapLatLong(thePapplet, enviromentmapTextureUnit, iblpath+"_LatLong"+"/PREM",
						iblpath+"/irradiance", mipLevels);
		}
		thePapplet.g.resetShader();
	}

	public static PApplet getPapplet() {
		return papplet;
	}

	public static PShader getPbrShader() {
		return pbrShader;
	}

	public static PImage getWhiteTexture() {
		return whiteTexture;
	}
	
	public static PImage getFlatNormalTexture() {
		return flatNormalTexture;
	}
	
	public static void setExposure(float _exposure) {
		exposure = _exposure;
		pbrShader.set("exposure", exposure);
	}
	
	public static void setDiffuseAttenuation(float att) {
		pbrShader.set("diffuseIndirectAttenuate", att);
	}
	
	public static void setReflectionAttenuation(float att) {
		pbrShader.set("reflectIndirectAttenuate", att);
	}
	
	static void loadSHCoeficients(String path) {
		int index = 0;
		shCoeficients = new float[27]; //3 x 9
		String[] lines = papplet.loadStrings(path);
		for(int i=0;i < lines.length;i++) {
			String[] line = PApplet.splitTokens( lines[i], "()");
			if(line.length > 0 ) {
				String[] values = PApplet.splitTokens( line[0], ", ");
				if(values.length >= 3) 
					for(int j=0; j<3; j++)
						shCoeficients[index++] = Float.parseFloat(values[j]);
			}
		}
	}
	
	static public PImage[] loadPrefilteredEnviromentMap(PApplet p5, int textureID, String texturesPath, String irradianceTexturePath, int minLevel){

		PImage[] textures = null;
		IntBuffer envMapTextureID;

		PGL pgl = p5.beginPGL();
		//		GL3 gl = ((PJOGL) pgl).gl.getGL3();
		// create the OpenGL-based cubeMap
		envMapTextureID = IntBuffer.allocate(1);
		pgl.genTextures(1, envMapTextureID);
		pgl.activeTexture(textureID);  
		pgl.enable(GL3.GL_TEXTURE_CUBE_MAP_SEAMLESS);  //without seamlees cube there are artifacts at higher roughness
		pgl.enable(PGL.TEXTURE_CUBE_MAP);
		pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, envMapTextureID.get(0));
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR_MIPMAP_LINEAR); //need this for mip maps
		pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);

		String[] cubeSideNames = { 
				"_posx_", "_negx_", "_posy_", "_negy_", "_posz_", "_negz_"
		};
		String textureName = "output_pmrem";


		for(int k=0; k<minLevel; k++){
			textures = new PImage[cubeSideNames.length];
			for (int i=0; i<textures.length; i++) {
				textures[i] = p5.loadImage(texturesPath+"/"+textureName + cubeSideNames[i]+k+".tga");
			}

			// put the textures in the cubeMap
			for (int i=0; i<textures.length; i++) {
				int w = textures[i].width;
				int h = textures[i].height;
				textures[i].loadPixels();
				int[] pix = textures[i].pixels;
				int[] rgbaPixels = new int[pix.length];
				for (int j = 0; j< pix.length; j++) {
					int pixel = pix[j];
					rgbaPixels[j] = 0xFF000000 | ((pixel & 0xFF) << 16) | ((pixel & 0xFF0000) >> 16) | (pixel & 0x0000FF00);
				}
				// load the image in the required face and mip level
				pgl.texImage2D(PGL.TEXTURE_CUBE_MAP_POSITIVE_X + i, k, PGL.RGBA, w, h, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, java.nio.IntBuffer.wrap(rgbaPixels));
			}
		}
		// irradiancemap after the last mip levevl
		String[] irradienceTextureNames = { 
				"_posx","_negx", "_posy", "_negy", "_posz", "_negz"
		};
		textures = new PImage[cubeSideNames.length];
		for (int i=0; i<textures.length; i++) {
			textures[i] = p5.loadImage(irradianceTexturePath+"/"+ "output_iem"+irradienceTextureNames[i]+".tga");

		}

		// put the textures in the cubeMap
		for (int i=0; i<textures.length; i++) {
			int w = textures[i].width;
			int h = textures[i].height;
			textures[i].loadPixels();
			int[] pix = textures[i].pixels;
			int[] rgbaPixels = new int[pix.length];
			for (int j = 0; j< pix.length; j++) {
				int pixel = pix[j];
				rgbaPixels[j] = 0xFF000000 | ((pixel & 0xFF) << 16) | ((pixel & 0xFF0000) >> 16) | (pixel & 0x0000FF00);
			}
			// load the image in the required face and mip level
			pgl.texImage2D(PGL.TEXTURE_CUBE_MAP_POSITIVE_X + i, minLevel, PGL.RGBA, w, h, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, java.nio.IntBuffer.wrap(rgbaPixels));
		}
		pgl.generateMipmap(PGL.TEXTURE_CUBE_MAP); //this is requiered for mipmaps
		p5.endPGL();

		return textures;
	}
	
	static public PImage[] loadPrefilteredEnviromentMapLatLong(PApplet p5, int textureID, String texturesPath, String irradianceTexturePath, int minLevel){
		PImage textures = null;
		IntBuffer envMapTextureID;

		PGL pgl = p5.beginPGL();
		//		GL3 gl = ((PJOGL) pgl).gl.getGL3();
		// create the OpenGL-based cubeMap
		envMapTextureID = IntBuffer.allocate(1);
		pgl.genTextures(1, envMapTextureID);
		pgl.activeTexture(textureID);  
		pgl.bindTexture(PGL.TEXTURE_2D, envMapTextureID.get(0));
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR_MIPMAP_LINEAR); //need this for mip maps
		pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);

		String textureName = "output_pmrem_";

		for(int k=0; k<minLevel; k++){
			textures = p5.loadImage(texturesPath+"/"+textureName + k+".tga");

			int w = textures.width;
			int h = textures.height;
			textures.loadPixels();
			int[] pix = textures.pixels;
			int[] rgbaPixels = new int[pix.length];
			for (int j = 0; j< pix.length; j++) {
				int pixel = pix[j];
				rgbaPixels[j] = 0xFF000000 | ((pixel & 0xFF) << 16) | ((pixel & 0xFF0000) >> 16) | (pixel & 0x0000FF00);
			}
			// load the image in the required mip level
			pgl.texImage2D(PGL.TEXTURE_2D, k, PGL.RGBA, w, h, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, java.nio.IntBuffer.wrap(rgbaPixels));
		
		}
		pgl.generateMipmap(PGL.TEXTURE_2D); //this is requiered for mipmaps
		p5.endPGL();

		return null;
	}

	static public void drawCubemap(PGraphics renderer, int size) {
		if(cubemapTextures == null)
			return;
		renderer.resetShader();
		renderer.textureMode(PApplet.NORMAL );
		renderer.noStroke();
		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[4]);
		// +Z "front" face
		renderer.vertex(-1 * size, -1 * size,  1 * size, 0, 0);
		renderer.vertex( 1 * size, -1 * size,  1 * size, 1, 0);
		renderer.vertex( 1 * size,  1 * size,  1 * size, 1, 1);
		renderer.vertex(-1 * size,  1 * size,  1 * size, 0, 1);
		renderer.endShape();

		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[5]);
		// -Z "back" face
		renderer.vertex( 1 * size, -1 * size, -1 * size, 0, 0);
		renderer.vertex(-1 * size, -1 * size, -1 * size, 1, 0);
		renderer.vertex(-1 * size,  1 * size, -1 * size, 1, 1);
		renderer.vertex( 1 * size,  1 * size, -1 * size, 0, 1);
		renderer.endShape();

		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[3]);
		// +Y "bottom" face
		renderer.vertex(-1 * size,  1 * size,  1 * size, 0, 0);
		renderer.vertex( 1 * size,  1 * size,  1 * size, 1, 0);
		renderer.vertex( 1 * size,  1 * size, -1 * size, 1, 1);
		renderer.vertex(-1 * size,  1 * size, -1 * size, 0, 1);
		renderer.endShape();

		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[2]);
		// -Y "top" face
		renderer.vertex(-1 * size, -1 * size, -1 * size, 0, 0);
		renderer.vertex( 1 * size, -1 * size, -1 * size, 1, 0);
		renderer.vertex( 1 * size, -1 * size,  1 * size, 1, 1);
		renderer.vertex(-1 * size, -1 * size,  1 * size, 0, 1);
		renderer.endShape();

		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[0]);
		// +X "right" face
		renderer.vertex( 1 * size, -1 * size,  1 * size, 0, 0);
		renderer.vertex( 1 * size, -1 * size, -1 * size, 1, 0);
		renderer.vertex( 1 * size,  1 * size, -1 * size, 1, 1);
		renderer.vertex( 1 * size,  1 * size,  1 * size, 0, 1);
		renderer.endShape();

		renderer.beginShape(PApplet.QUAD);
		renderer.texture(cubemapTextures[1]);
		// -X "left" face
		renderer.vertex(-1 * size, -1 * size, -1 * size, 0, 0);
		renderer.vertex(-1 * size, -1 * size,  1 * size, 1, 0);
		renderer.vertex(-1 * size,  1 * size,  1 * size, 1, 1);
		renderer.vertex(-1 * size,  1 * size, -1 * size, 0, 1);
		renderer.endShape();
	}

	private static void welcome() {
		System.out.println("SimplePBR "
				+ VERSION
				+ " "
				+ "by Nacho Cossio (@nacho_cossio) @ http://nachocossio.com");
	}
	
	
	/**
	 * return the version of the Library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}
}

