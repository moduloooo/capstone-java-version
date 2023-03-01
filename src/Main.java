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

    private int cube_renderer;
    private int obj_renderer;

    int vao[] = new int[2];
    int vboS[] = new int[12]; 
    int vboO[] = new int[4];

    private float cameraX, cameraY, cameraZ;
    private float objX, objY, objZ;
    private float sobjX[] = new float[25];
    private float sobjY[] = new float[25];
    private float sobjZ[] = new float[25];

    private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
    private Matrix4f pMat = new Matrix4f();
    private Matrix4f vMat = new Matrix4f();
    private Matrix4f mMat = new Matrix4f();
    private Matrix4f mvMat = new Matrix4f();
    private Matrix4f invTrMat = new Matrix4f();

    private int vLoc, mvLoc, projLoc, nLoc;
    private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mAmbLoc, mDiffLoc, mSpecLoc, mShiLoc;

    float[] globalAmbient = new float[] {0.6f, 0.6f, 0.6f, 1.0f};
    float[] lightAmbient = new float[] {0.1f, 0.1f, 0.1f, 1.0f}; 
    float[] lightDiffuse = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
    float[] lightSpecular = new float[] {1.0f, 1.0f, 1.0f, 1.0f};

    float[] matAmb = Utils.silverAmbient();
    float[] matDif = Utils.silverDiffuse();
    float[] matSpe = Utils.silverSpecular();
    float matShi = Utils.silverShininess();

    private Vector3f currentLightPos = new Vector3f();
    private float[] lightPos = new float[3];

    private Vector3f initialLightLoc = new Vector3f(5.0f, 2.0f, 2.0f);

    private int skyboxTexture;

    private int numSphereVerts;

    private double elapsedTime;
    private double startTime;
    private double tf;

    private int[] textures = new int[6];
    private String[] texNames = {
        "skyimages/1top.jpg",
        "skyimages/2bot.jpg",
        "skyimages/3front.jpg",
        "skyimages/4back.jpg",
        "skyimages/5right.jpg",
        "skyimages/6left.jpg"
    };

    private float aspect;

    public Main() {
        //creating the window
        setTitle("capstone java version");
        setSize(1000, 1000);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        this.setVisible(true);
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        //drawing the cubemap first
        cube_renderer = Utils.createShaderProgram("shaders/vert.glsl", "shaders/frag.glsl");
        setupVertices();
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 9.0f;
        objX = 0.0f; objY = -2.0f; objZ = 0.0f;

        for (int i = 0; i < textures.length; i++) {
            textures[i] = Utils.loadTexture(texNames[i]);
        }

        //now drawing all the objects
        obj_renderer = Utils.createShaderProgram("shaders/objvert.glsl", "shaders/objfrag.glsl");
        sphereVertices();

        skyboxTexture = Utils.loadTexture("imgs/white.jpg");

        for (int i = 0; i < 25; i++) {
            sobjX[i] = (float) (Math.random() * 10.0f) - 5.5f; 
            sobjY[i] = (float) (Math.random() * 10.0f) - 5.5f;
            sobjZ[i] = (float) (Math.random() * 10.0f) - 5.5f;
        }
    }

    public void display(GLAutoDrawable drawable) {
        drawSkyBox();  
        for (int i = 0; i < 25; i++) {
            drawSphere(sobjX[i], sobjY[i], sobjZ[i]);
        }  
    }

    private void drawSphere(float x, float y, float z) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glUseProgram(obj_renderer);

        mvLoc = gl.glGetUniformLocation(obj_renderer, "mv_matrix"); //model view matrix
        projLoc = gl.glGetUniformLocation(obj_renderer, "proj_matrix"); //projection matrix
        nLoc = gl.glGetUniformLocation(obj_renderer, "norm_matrix"); //normal matrix

        //positioning the object
        vMat.translation(-cameraX, -cameraY, -cameraZ);
        mMat.translation(x, y, z);

        //setting up the lighting 
        currentLightPos.set(initialLightLoc);
        installLights(vMat);

        //orienting the matrices
        mMat.identity();
        mMat.translate(x, y, z);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        mMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        //binding the vertex data
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //binding the texture and lighting data
        gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[2]); 
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0); 
        gl.glEnableVertexAttribArray(2); 
        
        //settings to make the sphere look nice and spherical
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboO[2]);

        //draw the sphere
        gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
    }

    private void drawSkyBox() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glUseProgram(cube_renderer);

        mvLoc = gl.glGetUniformLocation(cube_renderer, "mv_matrix"); //model view matrix
        projLoc = gl.glGetUniformLocation(cube_renderer, "proj_matrix"); //projection matrix

        //positioning the cube
        vMat.translation(-cameraX, -cameraY, -cameraZ);
        mMat.translation(objX, objY, objZ);

        mMat.identity();
        mMat.translate(cameraX, cameraY, cameraZ - 1.0f);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));

        //setting each side to its own texture
        gl.glDisable(GL_DEPTH_TEST);
        for (int i = 0; i < 6; i++) {
            gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[i + 6]);
            gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(i + 6);

            gl.glBindTexture(GL_TEXTURE_2D, textures[i]);
            gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[i]);
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            gl.glEnableVertexAttribArray(i);

            gl.glDepthFunc(GL_LEQUAL);
            gl.glFrontFace(GL_CCW);
            gl.glDrawArrays(GL_TRIANGLES, 0, 18);
            gl.glFrontFace(GL_CW);
            gl.glDrawArrays(GL_TRIANGLES, 0, 18);
            gl.glDisable(GL_TEXTURE_2D);
        }
        gl.glEnable(GL_DEPTH_TEST);
    }

    private void setupVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //I had to figure out all of these numbers by hand :')
        float[] topSide = {            
            -1.0f, 1.0f, 1.0f, 
            -1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,

            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f,-1.0f

        };

        float[] botSide = {
            -1.0f, -1.0f, 1.0f, 
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, -1.0f,-1.0f
        }; 

        float[] frontSide = {
            -1.0f, 1.0f, -1.0f, 
            -1.0f, -1.0f, -1.0f, 
            1.0f, -1.0f, -1.0f, 

            -1.0f, 1.0f, -1.0f, 
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f
        };

        float[] backSide = {
            1.0f, 1.0f, 1.0f, 
            1.0f, -1.0f, 1.0f, 
            -1.0f, -1.0f, 1.0f,

            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f
        };

        float[] rightSide = {
            1.0f, 1.0f, -1.0f, 
            1.0f, -1.0f, -1.0f, 
            1.0f, -1.0f, 1.0f,

            1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, 1.0f, 
            1.0f, -1.0f, 1.0f
        };

        float[] leftSide = {
            -1.0f, 1.0f, 1.0f, 
            -1.0f, -1.0f, 1.0f, 
            -1.0f, -1.0f, -1.0f,

            -1.0f, 1.0f, 1.0f, 
            -1.0f, 1.0f, -1.0f, 
            -1.0f, -1.0f, -1.0f
        };

        //textures

        float[] texTop = {
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
        };

        float[] texBot = {
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
        };

        float[] texFront = {
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
        };

        float[] texBack = {
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
        };

        float[] texRight = {
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
            
        };

        float[] texLeft = { 
            0.00f, 1.00f, 
            0.00f, 0.00f, 
            1.00f, 0.00f,
            
            0.00f, 1.00f, 
            1.00f, 1.00f,
            1.00f, 0.00f
        };

        //setting up the buffers to render each face separately
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vboS.length, vboS, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[0]);
        FloatBuffer TtopBuf = Buffers.newDirectFloatBuffer(topSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TtopBuf.limit() * 4, TtopBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[1]);
        FloatBuffer TbotBuf = Buffers.newDirectFloatBuffer(botSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TbotBuf.limit() * 4, TbotBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[2]);
        FloatBuffer TfrontBuf = Buffers.newDirectFloatBuffer(frontSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TfrontBuf.limit() * 4, TfrontBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[3]);
        FloatBuffer TbackBuf = Buffers.newDirectFloatBuffer(backSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TbackBuf.limit() * 4, TbackBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[4]);
        FloatBuffer TrightBuf = Buffers.newDirectFloatBuffer(rightSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TrightBuf.limit() * 4, TrightBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[5]);
        FloatBuffer TleftBuf = Buffers.newDirectFloatBuffer(leftSide);
        gl.glBufferData(GL_ARRAY_BUFFER, TleftBuf.limit() * 4, TleftBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[6]);
        FloatBuffer topBuf = Buffers.newDirectFloatBuffer(texTop);
        gl.glBufferData(GL_ARRAY_BUFFER, topBuf.limit() * 4, topBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[7]);
        FloatBuffer botBuf = Buffers.newDirectFloatBuffer(texBot);
        gl.glBufferData(GL_ARRAY_BUFFER, botBuf.limit() * 4, botBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[8]);
        FloatBuffer frontBuf = Buffers.newDirectFloatBuffer(texFront);
        gl.glBufferData(GL_ARRAY_BUFFER, frontBuf.limit() * 4, frontBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[9]);
        FloatBuffer backBuf = Buffers.newDirectFloatBuffer(texBack);
        gl.glBufferData(GL_ARRAY_BUFFER, backBuf.limit() * 4, backBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[10]);
        FloatBuffer rightBuf = Buffers.newDirectFloatBuffer(texRight);
        gl.glBufferData(GL_ARRAY_BUFFER, rightBuf.limit() * 4, rightBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[11]);
        FloatBuffer leftBuf = Buffers.newDirectFloatBuffer(texLeft);
        gl.glBufferData(GL_ARRAY_BUFFER, leftBuf.limit() * 4, leftBuf, GL_STATIC_DRAW);
    }

    public static void main(String[] args) {
        new Main();
    }

    public void sphereVertices() {
        //more procedural sphere generation so I don't have to do it by hand
        GL4 gl = (GL4) GLContext.getCurrentGL();
        Sphere mySphere =  new Sphere(24);
        numSphereVerts = mySphere.getIndices().length;

        int[] indices = mySphere.getIndices();
        Vector3f[] vert = mySphere.getVertices();
        Vector2f[] tex = mySphere.getTexCoords();
        Vector3f[] norm = mySphere.getNormals();

        float[] pvalues = new float[indices.length * 3];
        float[] tvalues = new float[indices.length * 2];
        float[] nvalues = new float[indices.length * 3];

        for (int i = 0; i < indices.length; i++) {
            pvalues[i * 3] = (float) (vert[indices[i]]).x;
            pvalues[i * 3 + 1] = (float) (vert[indices[i]]).y;
            pvalues[i * 3 + 2] = (float) (vert[indices[i]]).z;

            tvalues[i * 2] = (float) (tex[indices[i]]).x;
            tvalues[i * 2 + 1] = (float) (tex[indices[i]]).y;

            nvalues[i * 3] = (float) (norm[indices[i]]).x;
            nvalues[i * 3 + 1] = (float) (norm[indices[i]]).y;
            nvalues[i * 3 + 2] = (float) (norm[indices[i]]).z;
        }

        //setting up the bufferss
        gl.glGenVertexArrays(vao.length, vao, 0); 
        gl.glBindVertexArray(vao[0]); 
        gl.glGenBuffers(vboO.length, vboO, 0); 

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

    }

    private void installLights(Matrix4f vMatrix) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //placing the lights in the right spot
        currentLightPos.mulPosition(vMatrix);
        lightPos[0] = currentLightPos.x();
        lightPos[1] = currentLightPos.y();
        lightPos[2] = currentLightPos.z();

        //setting up the reflections 
        globalAmbLoc = gl.glGetUniformLocation(obj_renderer, "globalAmbient");
        ambLoc = gl.glGetUniformLocation(obj_renderer, "light.ambient");
        diffLoc = gl.glGetUniformLocation(obj_renderer, "light.diffuse");
        specLoc = gl.glGetUniformLocation(obj_renderer, "light.specular");
        posLoc = gl.glGetUniformLocation(obj_renderer, "light.position");
        mAmbLoc = gl.glGetUniformLocation(obj_renderer, "material.ambient");
        mDiffLoc = gl.glGetUniformLocation(obj_renderer, "material.diffuse");
        mSpecLoc = gl.glGetUniformLocation(obj_renderer, "material.specular");
        mShiLoc = gl.glGetUniformLocation(obj_renderer, "material.shininess");

        gl.glProgramUniform4fv(obj_renderer, globalAmbLoc, 1, globalAmbient, 0);
        gl.glProgramUniform4fv(obj_renderer, ambLoc, 1, lightAmbient, 0);
        gl.glProgramUniform4fv(obj_renderer, diffLoc, 1, lightDiffuse, 0);
        gl.glProgramUniform4fv(obj_renderer, specLoc, 1, lightSpecular, 0);
        gl.glProgramUniform3fv(obj_renderer, posLoc, 1, lightPos, 0);
        gl.glProgramUniform4fv(obj_renderer, mAmbLoc, 1, matAmb, 0);
        gl.glProgramUniform4fv(obj_renderer, mDiffLoc, 1, matDif, 0);
        gl.glProgramUniform4fv(obj_renderer, mSpecLoc, 1, matSpe, 0);
        gl.glProgramUniform1f(obj_renderer, mShiLoc, matShi);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { 
        //code to reshape the window
        //I know the final isn't centered I can't figure out why so we just have to deal with the borders for now 
        GL4 gl = (GL4) GLContext.getCurrentGL();
        aspect = (float) width / (float) height; 
        gl.glViewport(0, 0, width, height); 
        pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
    }
    
    public void dispose(GLAutoDrawable drawable) { }

}