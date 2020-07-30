#version 150

// GLSL shader by Nick Galko from https://gist.github.com/galek/53557375251e1a942dfa
//Ported to Processing by Nacho Cossio (nachocossio.com, @nacho_cossio) 

#define SAMPLERCUBESUPPORT

uniform mat4 transformMatrix;
uniform mat4 modelviewMatrix;
uniform mat3 normalMatrix;
uniform mat4 modelviewInv;

#ifdef SAMPLERCUBESUPPORT
uniform samplerCube envd;  // prefiltered env cubemap
#else
uniform sampler2D envd;
#endif
 // These two texture are single channel, normally they are combined in one texture
 // I have kept it like this for making easier to get materials from different websites
uniform sampler2D roughnessMap;    // roughness texture
uniform sampler2D metalnessMap;    // metalness texture.
uniform sampler2D normalMap;    // normal map                    
                                    
uniform sampler2D iblbrdf; // IBL BRDF normalization precalculated tex
uniform sampler2D albedoTex;     // base texture (albedo)

uniform int lightCount;
uniform vec4 lightPosition[8];
uniform vec3 lightNormal[8];
uniform vec3 lightDiffuse[8];
uniform vec3 lightFalloff[8];
uniform vec3 lightAmbient[8];
uniform int mipLevels;
uniform vec4 material; // x - metallic, y - roughness, w - "rim" lighting
uniform float exposure;
uniform float gamma = 2.2;
uniform float diffuseIndirectAttenuate = 1;
uniform float reflectIndirectAttenuate = 1;
uniform vec3 iblSH[9];

const float one_float = 1.0;

in FragData {
  vec4 color;
  vec3 ecVertex;
  vec3 normal;
  vec2 texCoord;
} FragIn;
in vec4 gl_FragCoord;
   
out vec4 fragColor;
#define PI 3.1415926
#define COOK
#define COOK_GGX
#define USE_ALBEDO_MAP
#define USE_ROUGHNESS_MAP
#define USE_METALNESS_MAP

//Taken from Filament https://github.com/google/filament/blob/main/shaders/src/light_indirect.fs
//Have to invert Y axis
vec3 Irradiance_SphericalHarmonics(const vec3 n) {
    return max(
          iblSH[0]
// #if SPHERICAL_HARMONICS_BANDS >= 2
        + iblSH[1] * (-n.y)
        + iblSH[2] * (n.z)
        + iblSH[3] * (n.x)
// #endif
// #if SPHERICAL_HARMONICS_BANDS >= 3
        + iblSH[4] * (-n.y * n.x)
        + iblSH[5] * (-n.y * n.z)
        + iblSH[6] * (3.0 * n.z * n.z - 1.0)
        + iblSH[7] * (n.z * n.x)
        + iblSH[8] * (n.x * n.x - n.y * n.y)
// #endif
        , 0.0);
}

float falloffFactor(vec3 lightPos, vec3 vertPos, vec3 coeff) {
  vec3 lpv = lightPos - vertPos;
  vec3 dist = vec3(1);
  dist.z = dot(lpv, lpv);
  dist.y = sqrt(dist.z);
  return 1 / dot(dist, coeff);
}

// phong (lambertian) diffuse term
float phong_diffuse()
{
    return (1.0 / PI);
}


// compute fresnel specular factor for given base specular and product
// product could be NdV or VdH depending on used technique
vec3 fresnel_factor(in vec3 f0, in float product)
{
    return mix(f0, vec3(1.0), pow(1.01 - product, 5.0));
}


// following functions are copies of UE4
// for computing cook-torrance specular lighting terms

float D_blinn(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float n = 2.0 / m2 - 2.0;
    return (n + 2.0) / (2.0 * PI) * pow(NdH, n);
}

float D_beckmann(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float NdH2 = NdH * NdH;
    return exp((NdH2 - 1.0) / (m2 * NdH2)) / (PI * m2 * NdH2 * NdH2);
}

float D_GGX(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float d = (NdH * m2 - NdH) * NdH + 1.0;
    return m2 / (PI * d * d);
}

float G_schlick(in float roughness, in float NdV, in float NdL)
{
    float k = roughness * roughness * 0.5;
    float V = NdV * (1.0 - k) + k;
    float L = NdL * (1.0 - k) + k;
    return 0.25 / (V * L);
}


// simple phong specular calculation with normalization
vec3 phong_specular(in vec3 V, in vec3 L, in vec3 N, in vec3 specular, in float roughness)
{
    vec3 R = reflect(-L, N);
    float spec = max(0.0, dot(V, R));

    float k = 1.999 / (roughness * roughness);

    return min(1.0, 3.0 * 0.0398 * k) * pow(spec, min(10000.0, k)) * specular;
}

// simple blinn specular calculation with normalization
vec3 blinn_specular(in float NdH, in vec3 specular, in float roughness)
{
    float k = 1.999 / (roughness * roughness);
    
    return min(1.0, 3.0 * 0.0398 * k) * pow(NdH, min(10000.0, k)) * specular;
}

// cook-torrance specular calculation                      
vec3 cooktorrance_specular(in float NdL, in float NdV, in float NdH, in vec3 specular, in float roughness)
{
#ifdef COOK_BLINN
    float D = D_blinn(roughness, NdH);
#endif

#ifdef COOK_BECKMANN
    float D = D_beckmann(roughness, NdH);
#endif

#ifdef COOK_GGX
    float D = D_GGX(roughness, NdH);
#endif

    float G = G_schlick(roughness, NdV, NdL);

    float rim = mix(1.0 - roughness * material.w * 0.9, 1.0, NdV);

    return (1.0 / rim) * specular * G * D;
}

mat3 computeTangentFrame(vec3 normal, vec3 position, vec2 texCoord)
{
    vec3 dpx = dFdx(position);
    vec3 dpy = dFdy(position);
    vec2 dtx = dFdx(texCoord);
    vec2 dty = dFdy(texCoord);
    
    vec3 tangent = normalize(dpy * dtx.t - dpx * dty.t);
    vec3 binormal = cross(tangent, normal);
   
    return mat3(tangent, binormal, normal);
}
                      
void main() {
    // L, V, H vectors
    vec3 V = normalize(-FragIn.ecVertex);
    vec3 nn = normalize(FragIn.normal);
    vec2 texcoord = FragIn.texCoord ;
    //vec3 N = nn;

    // tbn basis
    vec3 unpacked = (texture(normalMap, texcoord).xyz * 2.0  - 1.0 );
    vec3 N = computeTangentFrame(nn, FragIn.ecVertex, texcoord) * unpacked;
    N = normalize(N);

    // albedo/specular base
 #ifdef USE_ALBEDO_MAP
    vec3 base = texture(albedoTex, texcoord).xyz * FragIn.color.xyz;
 #else
    vec3 base = FragIn.color.xyz;
 #endif

    // roughness
 #ifdef USE_ROUGHNESS_MAP
    float roughness = texture(roughnessMap, texcoord).y * material.y;
 #else
    float roughness = material.y;
 #endif

#ifdef USE_METALNESS_MAP
    float metallic = texture(metalnessMap, texcoord).y * material.x;
#else
    float metallic = material.x;
#endif

    // mix between metal and non-metal material, for non-metal
    // constant base specular factor of 0.04 grey is used
    vec3 specular = mix(vec3(0.04), base, metallic);

    //diffuse indirect light using IBL
    // diffuse IBL term  
    //    I know that my IBL cubemap has diffuse pre-integrated value in 10th MIP level
    //    actually level selection should be tweakable or from separate diffuse cubemap
    mat3x3 tnrm = transpose(normalMatrix);
    vec3 refl = tnrm * N;
    // refl = ( vec4(refl,1) * modelviewInv).xyz;
    #ifdef SAMPLERCUBESUPPORT
    vec3 envdiff = textureLod(envd, -refl, mipLevels).xyz;
    #else
    vec3 envdiff = Irradiance_SphericalHarmonics(refl);
    #endif


    // specular IBL term
    //    11 magic number is total MIP levels in cubemap, this is simplest way for picking
    //    MIP level from roughness value (but it's not correct, however it looks fine)
    refl = tnrm * reflect(-V, N);
    // refl = ( vec4(refl,1) * modelviewInv).xyz;
    // float mipLevel = roughness * 10.0 - pow(roughness, 6.0) * 1.5;
    float mipLevel = sqrt( roughness ) * (mipLevels - 1.0);
    #ifdef SAMPLERCUBESUPPORT
    vec3 envspec = textureLod( envd, -refl, mipLevel).xyz;
    // vec3 envspec = textureLod(envd, tnrm * N, 6).xyz;
    #else
	// vec2 tc = vec2(atan(refl.z, refl.x) + PI, acos(-refl.y)) / vec2(2.0 * PI, PI);
	// vec3 envspec = textureLod( envd, tc, mipLevel).xyz;
	float RECIPROCAL_PI2 = 0.15915494;
    vec2 uv;
	uv.x = atan( -refl.z, -refl.x ) * RECIPROCAL_PI2 + 0.5;
	uv.y = refl.y * 0.5 + 0.5;
	vec3 envspec = textureLod( envd, uv, mipLevel).xyz;
    #endif

    vec3 reflected_light = vec3(0);
    vec3 diffuse_light = vec3(0); // initial value == constant ambient light

    float NdL;// = max(0.0, dot(N, L));
    float NdV = max(0.001, dot(N, V));
    float NdH;// = max(0.001, dot(N, H));
    float HdV;// = max(0.001, dot(H, V));
    float LdV;// = max(0.001, dot(L, V));
    // loop though ligt count
    for (int i = 0; i < 8; i++) {
        if (lightCount == i) break;
        bool isDir = lightPosition[i].w < one_float;
        float A;
        vec3 L;
        // point light direction to point in view space
        //vec3 local_light_pos = (modelviewMatrix * ( lightPosition[i])).xyz;
         vec3 local_light_pos = lightPosition[i].xyz; //It seems that processing send light positions in eye coordinates

        if (isDir) {
            A = one_float;
            L = -one_float * lightNormal[i];
        } else {
            A = falloffFactor(local_light_pos, FragIn.ecVertex, lightFalloff[i]);  
            L = normalize(local_light_pos - FragIn.ecVertex);
        }
             
        vec3 H = normalize(L + V);

        // compute material reflectance
        NdL = max(0.0, dot(N, L));
        // float NdV = max(0.001, dot(N, V));  //equal for all lights
        NdH = max(0.001, dot(N, H));
        HdV = max(0.001, dot(H, V));
        LdV = max(0.001, dot(L, V));

        // fresnel term is common for any, except phong
        // so it will be calcuated inside ifdefs
    #ifdef PHONG
        // specular reflectance with PHONG
        vec3 specfresnel = fresnel_factor(specular, NdV);
        vec3 specref = phong_specular(V, L, N, specfresnel, roughness);
    #endif

    #ifdef BLINN
        // specular reflectance with BLINN
        vec3 specfresnel = fresnel_factor(specular, HdV);
        vec3 specref = blinn_specular(NdH, specfresnel, roughness);
    #endif

    #ifdef COOK
        // specular reflectance with COOK-TORRANCE
        vec3 specfresnel = fresnel_factor(specular, HdV);
        vec3 specref = cooktorrance_specular(NdL, NdV, NdH, specfresnel, roughness);
    #endif

        specref *= vec3(NdL);
        // diffuse is common for any model
        vec3 diffref = (vec3(1.0) - specfresnel) * phong_diffuse() * NdL;     
        // compute lighting       
        // point light
        vec3 light_color = lightDiffuse[i] * A;
        reflected_light += specref * light_color;
        diffuse_light += diffref * light_color;
    }

    // IBL lighting
    vec2 brdf = texture(iblbrdf, vec2(roughness, 1.0 - NdV)).xy;
    vec3 iblspec = min(vec3(0.99), fresnel_factor(specular, NdV) * brdf.x + brdf.y);
    
    reflected_light +=  iblspec * envspec * reflectIndirectAttenuate;
    diffuse_light +=  envdiff * (1.0 / PI) * diffuseIndirectAttenuate ;  

    // final result
    vec3 result =  diffuse_light * mix(base, vec3(0.0), metallic) +
         reflected_light ;

    result = pow(result * exposure,vec3(1.0/gamma)); // gamma correction
    fragColor = vec4(result , 1.0);
}