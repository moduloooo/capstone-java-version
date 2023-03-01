import java.nio.*;
import javax.swing.*;

import java.lang.Math;
import java.lang.ref.Reference;

import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import org.joml.*;

import java.io.*;
import com.jogamp.opengl.util.texture.*;

public class Main extends JFrame implements GLEventListener {

    private GLCanvas myCanvas;
    private int renderingProgram;
    private int boxRenderingProgram;
    private int vao[] = new int[1];
    private int vbo[] = new int[10];
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
    private float pyrLocX, pyrLocY, pyrLocZ;
    private float spLocX, spLocY, spLocZ;
    private float torLocX, torLocY, torLocZ;

    private Sphere mySphere;
    private Torus myTorus;
    private int numSphereVerts;

    private int numTorusVertices;
    private int numTorusIndices;

    private FloatBuffer vals = Buffers.newDirectFloatBuffer(16); // utility buffer for transferring matrices
    private Matrix4f pMat = new Matrix4f(); // perspective matrix
    private Matrix4f vMat = new Matrix4f(); // view matrix
    private Matrix4f mMat = new Matrix4f(); // model matrix
    private Matrix4f mvMat = new Matrix4f(); // model-view matrix
    private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose matrix for converting normals

    private int mvLoc;
	private float aspect;

    private int brickTexture;
    private int boxTexture;
    
    //lightng stup
    private int mLoc, vLoc, pLoc, nLoc;
    private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc,mAmbLoc, mDiffLoc, mSpecLoc, mShiLoc;

    private Vector3f currentLightPos = new Vector3f(); 
    private float[ ] lightPos = new float[3];

    private Vector3f initialLightLoc = new Vector3f(5.0f, 2.0f, 2.0f);

    float[ ] globalAmbient = new float[ ] { 0.6f, 0.6f, 0.6f, 1.0f };
    float[ ] lightAmbient = new float[ ] { 0.1f, 0.1f, 0.1f, 1.0f };
    float[ ] lightDiffuse = new float[ ] { 1.0f, 1.0f, 1.0f, 1.0f };
    float[ ] lightSpecular = new float[ ] { 1.0f, 1.0f, 1.0f, 1.0f };

    float[ ] matAmbient = Utils.silverAmbient();
    float[ ] matDiffuse = Utils.silverDiffuse();
    float[ ] matSpecular = Utils.silverSpecular();
    float matShininess = Utils.silverShininess();

    public Main() {
        setTitle("Capstone Java Version !!");
        setSize(600, 600);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        this.setVisible(true);
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        renderingProgram = Utils.createShaderProgram("shaders/vert.glsl", "shaders/frag.glsl");
        boxRenderingProgram = Utils.createShaderProgram("shaders/boxvert.glsl", "shaders/fragvert.glsl");

        setupVertices();

        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
	    cubeLocX = 0.0f; cubeLocY = -2.0f; cubeLocZ = 0.0f;	
        pyrLocX = 1.0f; pyrLocY= 3.0f; pyrLocZ = 0.0f; 
        spLocX = 1.0f; spLocY = 1.0f; pyrLocZ = 0.0f;
        torLocX = -1.0f; torLocY = -1.0f; torLocZ = 0.0f;

        aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

        brickTexture = Utils.loadTexture("imgs/blu.jpg");
    }

    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glClear(GL_COLOR_BUFFER_BIT);
        gl.glUseProgram(renderingProgram);

        mMat.identity().setTranslation(cameraX, cameraY, cameraZ);


        mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
        vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
        pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
        nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");

        currentLightPos.set(initialLightLoc);
        installLights();

        mMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);
	 	 
        vMat.translation(-cameraX, -cameraY, -cameraZ);

        //drawing the cube 
	 	mMat.translation(cubeLocX, cubeLocY, cubeLocZ); //setting coordinates

        //setting up the view matrices
        mvMat.identity();
        mvMat.mul(vMat); 
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        //binding vertex data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //drawing to the screen
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDrawArrays(GL_TRIANGLES, 0, 36);

        //drawing the pyramid
        mMat.translation(pyrLocX, pyrLocY, pyrLocZ);

        //setting up the view matrices 
        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        // binding vertex data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //binding texture data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        //activate texture and bind
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, brickTexture);

        //draw textured pyramid
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDrawArrays(GL_TRIANGLES, 0, 18);

        //drawing the sphere
        mMat.translation(spLocX, spLocY, spLocZ);

        //setting up the view matrices 
        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        // binding vertex data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //binding texture data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
        gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        //activate texture and bind
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, brickTexture);

        //draw textured sphere
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);

        //drawing the torus
        mMat.translation(torLocX, torLocY, torLocZ);

        //setting up the view matrices 
        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        //binding vertex data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //binding texture data to buffers
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
        gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
        gl.glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(3);
        
        //activate texture and bind
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, brickTexture);

        //draw textured torus
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDrawElements(GL_TRIANGLES, numTorusIndices, GL_UNSIGNED_INT, 0);
    }

    private void setupVertices() { 
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //vertices of the cube
	 	float[ ] vertexPositions = { 
            -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f
        };

        float[ ] cubeTextureCoord = {
            1.00f, 0.66f, 1.00f, 0.33f, 0.75f, 0.33f, // back face lower right
            0.75f, 0.33f, 0.75f, 0.66f, 1.00f, 0.66f, // back face upper left
            0.75f, 0.33f, 0.50f, 0.33f, 0.75f, 0.66f, // right face lower right
            0.50f, 0.33f, 0.50f, 0.66f, 0.75f, 0.66f, // right face upper left
            0.50f, 0.33f, 0.25f, 0.33f, 0.50f, 0.66f, // front face lower right
            0.25f, 0.33f, 0.25f, 0.66f, 0.50f, 0.66f, // front face upper left
            0.25f, 0.33f, 0.00f, 0.33f, 0.25f, 0.66f, // left face lower right
            0.00f, 0.33f, 0.00f, 0.66f, 0.25f, 0.66f, // left face upper left
            0.25f, 0.33f, 0.50f, 0.33f, 0.50f, 0.00f, // bottom face upper right
            0.50f, 0.00f, 0.25f, 0.00f, 0.25f, 0.33f, // bottom face lower left
            0.25f, 1.00f, 0.50f, 1.00f, 0.50f, 0.66f, // top face upper right
            0.50f, 0.66f, 0.25f, 0.66f, 0.25f, 1.00f // top face lower left
        };

        //vertices of the pyramid
        float[ ] pyramidPositions ={ 
            -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, // front face
            1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, // right face
            1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, // back face
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, // left face
            -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, // base – left front
            1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f // base – right back
        };

        //vertices of the texture of the pyramid -- will remove later (probably)
        float[ ] pyrTextureCoordinates = { 
            0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f 
        };

        //setup sphere drawing 
        mySphere = new Sphere(24);
        numSphereVerts = mySphere.getIndices().length;

	    int[ ] sIndices = mySphere.getIndices();
	    Vector3f[ ] sVert = mySphere.getVertices();
	    Vector2f[ ] sTex = mySphere.getTexCoords();
	    Vector3f[ ] sNorm = mySphere.getNormals();

	    float[ ] sPvalues = new float[sIndices.length*3]; // vertex positions
	    float[ ] sTvalues = new float[sIndices.length*2]; // texture coordinates
	    float[ ] sNvalues = new float[sIndices.length*3]; // normal vectors

        for (int i=0; i<sIndices.length; i++) {	 
            sPvalues[i*3] = (float) (sVert[sIndices[i]]).x;
            sPvalues[i*3+1] = (float) (sVert[sIndices[i]]).y;
            sPvalues[i*3+2] = (float) (sVert[sIndices[i]]).z;
            sTvalues[i*2] = (float) (sTex[sIndices[i]]).x;
            sTvalues[i*2+1] = (float) (sTex[sIndices[i]]).y;
            sNvalues[i*3] = (float) (sNorm[sIndices[i]]).x;
            sNvalues[i*3+1]= (float)(sNorm[sIndices[i]]).y;
            sNvalues[i*3+2]=(float) (sNorm[sIndices[i]]).z;
        }

        //estup torus drawign 
        myTorus = new Torus(0.5f, 0.2f, 48);	

        numTorusVertices = myTorus.getNumVertices();
        numTorusIndices = myTorus.getNumIndices();

	    Vector3f[] tVert = myTorus.getVertices();
	    Vector2f[] tTex = myTorus.getTexCoords();
	    Vector3f[] tNorm = myTorus.getNormals();

	    int[] tIndices = myTorus.getIndices();
	    float[] tPvalues = new float[tVert.length*3];
	    float[] tTvalues = new float[tTex.length*2];
	    float[] tNvalues = new float[tNorm.length*3];

        for (int i=0; i<numTorusVertices; i++) {	 
            tPvalues[i*3] = (float) tVert[i].x;	 	 // vertex position
            tPvalues[i*3+1] = (float) tVert[i].y;
            tPvalues[i*3+2] = (float) tVert[i].z;
            tTvalues[i*2] = (float) tTex[i].x;	 	 // texture coordinates
            tTvalues[i*2+1] = (float) tTex[i].y;
            tNvalues[i*3] = (float) tNorm[i].x;	 	 // normal vector
            tNvalues[i*3+1] = (float) tNorm[i].y;
            tNvalues[i*3+2] = (float) tNorm[i].z;
        }   

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        //cube buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer cubBuf = Buffers.newDirectFloatBuffer(vertexPositions);
        gl.glBufferData(GL_ARRAY_BUFFER, cubBuf.limit()*4, cubBuf, GL_STATIC_DRAW);

        //pyramid buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer pyrBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
        gl.glBufferData(GL_ARRAY_BUFFER, pyrBuf.limit()*4, pyrBuf, GL_STATIC_DRAW);

        //pyramid texture buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer pTexBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
        gl.glBufferData(GL_ARRAY_BUFFER, pTexBuf.limit()*4, pTexBuf, GL_STATIC_DRAW);

        //sphere buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        FloatBuffer spBuf = Buffers.newDirectFloatBuffer(sPvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, spBuf.limit()*4, spBuf, GL_STATIC_DRAW);

        //sphere texture buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        FloatBuffer sTexBuf = Buffers.newDirectFloatBuffer(sTvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, sTexBuf.limit()*4, sTexBuf, GL_STATIC_DRAW);

        //sphere normals buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
	    FloatBuffer sNorBuf = Buffers.newDirectFloatBuffer(sNvalues);
	    gl.glBufferData(GL_ARRAY_BUFFER, sNorBuf.limit()*4, sNorBuf, GL_STATIC_DRAW);

        //torys buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);	 	 // vertex positions
	    FloatBuffer tVertBuf = Buffers.newDirectFloatBuffer(tPvalues);
	    gl.glBufferData(GL_ARRAY_BUFFER, tVertBuf.limit()*4, tVertBuf, GL_STATIC_DRAW);

        //torus texture buffer
	    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);	 	 // texture coordinates
	    FloatBuffer tTexBuf = Buffers.newDirectFloatBuffer(tTvalues);
	    gl.glBufferData(GL_ARRAY_BUFFER, tTexBuf.limit()*4, tTexBuf, GL_STATIC_DRAW);

        //torus normals buffer
	    gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);	 	 // normal vectors
	    FloatBuffer tNorBuf = Buffers.newDirectFloatBuffer(tNvalues);
	    gl.glBufferData(GL_ARRAY_BUFFER, tNorBuf.limit()*4, tNorBuf, GL_STATIC_DRAW);

        //torus indices buffer
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[9]); // indices
        IntBuffer tIdxBuf = Buffers.newDirectIntBuffer(tIndices);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, tIdxBuf.limit()*4, tIdxBuf, GL_STATIC_DRAW);

    }

    private void installLights() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        // save the light position in a float array
        lightPos[0]=currentLightPos.x();
        lightPos[1]=currentLightPos.y();
        lightPos[2]=currentLightPos.z();
        // get the locations of the light and material fields in the shader
        globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
        ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
        diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
        specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
        posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
        mAmbLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
        mDiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
        mSpecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
        mShiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
        // set the uniform light and material values in the shader
        gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
        gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
        gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
        gl.glProgramUniform4fv(renderingProgram, mAmbLoc, 1, matAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, mDiffLoc, 1, matDiffuse, 0);
        gl.glProgramUniform4fv(renderingProgram, mSpecLoc, 1, matSpecular, 0);
        gl.glProgramUniform1f(renderingProgram, mShiLoc, matShininess);
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { 
        GL4 gl = (GL4) GLContext.getCurrentGL();
        aspect = (float) width / (float) height; // new window width & height are provided by the callback
        gl.glViewport(0, 0, width, height); // sets region of screen associated with the frame buffer
        pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
    }

	public void dispose(GLAutoDrawable drawable) { }

    public static void main(String[] args) {
        new Main();
    }

}