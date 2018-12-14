#version 150

uniform mat4 transformMatrix;
uniform mat4 modelviewMatrix;
uniform mat3 normalMatrix;
uniform mat4 texMatrix;
uniform vec4 lightPosition[8];
uniform vec3 lightDiffuse[8];
uniform float time;

in vec4 position;
in vec4 color;
in vec3 normal;
in vec2 texCoord;
 
 
out FragData {
  vec4 color;
  vec3 ecVertex;
  vec3 normal;
  vec2 texCoord;
} FragOut;
 


void main() {
  
  gl_Position = transformMatrix * position;
  vec3 ecp = vec3(modelviewMatrix * position);
  FragOut.ecVertex = ecp;
  FragOut.normal = normalize(normalMatrix * normal);
  FragOut.color =  color;
  FragOut.texCoord = (texMatrix * vec4(texCoord, 1.0, 1.0)).st;
  
}