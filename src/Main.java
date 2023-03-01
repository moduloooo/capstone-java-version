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

    private int background_renderer;
    private int obj_renderer;

    int vao[] = new int[2];
    int vbo[] = new int[20]; 

    private float cameraX, cameraY, cameraZ;
    private float defX, defY, defZ;

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

    private int backgroundTexture;

    private int numSphereVerts;

    private double elapsedTime;
    private double startTime;
    private double tf;

    private float aspect;

    public Main() {
        setTitle("Capstone Java Version");
        setSize(1000, 1000);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        this.setVisible(true);
        FPSAnimator animator = new FPSAnimator(myCanvas, 60);
        animator.start();
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 9.0f;
        defX = 0.0f; defY = -2.0f; defZ = 0.0f;
        for (int i = 0; i < 25; i++) {
            sobjX[i] = (float) (Math.random() * 10.0f) - 5.5f; 
            sobjY[i] = (float) (Math.random() * 10.0f) - 5.5f;
            sobjZ[i] = (float) (Math.random() * 10.0f) - 5.5f;
        }

        background_renderer = Utils.createShaderProgram("shaders/backvert.glsl", "shaders/backfrag.glsl");
        obj_renderer = Utils.createShaderProgram("shaders/objvert.glsl", "shaders/objfrag.glsl");
        setupBackground();
        
        sphereVertices();
        torusVertices();
        backgroundTexture = Utils.loadCubeMap("skyimages");
        gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
    }

    public void display(GLAutoDrawable drawable) {
        drawBackground();
        // for (int i = 0; i < 25; i++) {
        //     drawSphere(sobjX[i], sobjY[i], sobjZ[i]);
        // }  
        //drawTorus(0.0f, 0.0f, 0.0f); //no torus?? 
    }

    private void drawBackground() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        //gl.glClear(GL_COLOR_BUFFER_BIT); 
        gl.glUseProgram(background_renderer);
        vLoc = gl.glGetUniformLocation(background_renderer, "v_matrix");
        projLoc = gl.glGetUniformLocation(background_renderer, "p_matrix");
        // set up vertices buffer for cube (buffer for texture coordinates not necessary)
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        // make the cube map the active texture
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_CUBE_MAP, backgroundTexture);
        // disable depth testing, and then draw the cube map
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glDisable(GL_DEPTH_TEST);
        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
        gl.glEnable(GL_DEPTH_TEST);
    }

    private void drawSphere(float x, float y, float z) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        //gl.glClear(GL_COLOR_BUFFER_BIT); 
        gl.glUseProgram(obj_renderer);

        mvLoc = gl.glGetUniformLocation(obj_renderer, "mv_matrix");
        projLoc = gl.glGetUniformLocation(obj_renderer, "proj_matrix");
        nLoc = gl.glGetUniformLocation(obj_renderer, "norm_matrix");

        vMat.translation(-cameraX, -cameraY, -cameraZ);
        mMat.translation(x, y, z);

        currentLightPos.set(initialLightLoc);
        installLights(vMat);

        elapsedTime = System.currentTimeMillis() - startTime;
        tf = elapsedTime / 1000.0;

        mMat.identity();
        mMat.translate(x, y, z);
        //mMat.translate((float) Math.sin (0.35 * (tf + scale)) * 2.0f, (float) Math.sin(0.52f * (tf + scale)) * 2.0f, (float) Math.sin(0.7f * (tf + scale)) * 2.0f);
        //mMat.translate((int) (Math.random() * 10), (int) (Math.random() * 10), (int) (Math.random() * 10));
        //mMat.rotateXYZ(1.75f * (float) (tf + scale), 1.75f * (float) (tf + scale), 1.75f * (float) (tf + scale));
        // mMat.rotateY((float) Math.sin (0.35 * (tf)) * 2.0f);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        mMat.invert(invTrMat);
        invTrMat.transpose(invTrMat);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); //this is wrong
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        //gl.glBindTexture(GL_TEXTURE_2D, skyboxTexture);
        //gl.glBindBuffer(GL_ARRAY_BUFFER, vboO[1]); //this is for textures
        //gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        //gl.glEnableVertexAttribArray(1);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]); //this is wrong
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0); //first value becomes 2 when using textures
        gl.glEnableVertexAttribArray(2); //turn to 2 when using textures

        
        
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]); //?
        gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
        //gl.glDisable(GL_TEXTURE_2D);
    }

    // private void drawSkyBox() {
    //     GL4 gl = (GL4) GLContext.getCurrentGL();

    //     gl.glUseProgram(background_renderer);

    //     mvLoc = gl.glGetUniformLocation(background_renderer, "mv_matrix");
    //     projLoc = gl.glGetUniformLocation(background_renderer, "proj_matrix");

    //     aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
    //     pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

    //     vMat.translation(-cameraX, -cameraY, -cameraZ);
    //     mMat.translation(defX, defY, defZ);
    //     //mMat.translation(cameraX, cameraY, cameraZ - 1.0f);

    //     elapsedTime =  (System.currentTimeMillis() - startTime);
    //     tf =  (elapsedTime / 1000.0);

    //     mMat.identity();
    //     mMat.translate(cameraX, cameraY, cameraZ - 1.0f);
    //     //mMat.rotateX(1.75f * (float) tf);

    //     mvMat.identity();
    //     mvMat.mul(vMat);
    //     mvMat.mul(mMat);

    //     gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
    //     gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));

    //     gl.glDisable(GL_DEPTH_TEST);
    //     for (int i = 0; i < 6; i++) {
    //         gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[i + 6]);
    //         gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
    //         gl.glEnableVertexAttribArray(i + 6);

    //         gl.glBindTexture(GL_TEXTURE_2D, textures[i]);
    //         gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[i]);
    //         gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
    //         gl.glEnableVertexAttribArray(i);

    //         // gl.glEnable(GL_DEPTH_TEST);
    //         gl.glDepthFunc(GL_LEQUAL);
    //         gl.glFrontFace(GL_CCW);
    //         gl.glDrawArrays(GL_TRIANGLES, 0, 18);
    //         gl.glFrontFace(GL_CW);
    //         gl.glDrawArrays(GL_TRIANGLES, 0, 18);
    //         gl.glDisable(GL_TEXTURE_2D);
    //     }
    //     gl.glEnable(GL_DEPTH_TEST);
    // }

    private void drawTorus(float x, float y, float z) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(obj_renderer);

        mvLoc = gl.glGetUniformLocation(obj_renderer, "mv_matrix");
        projLoc = gl.glGetUniformLocation(obj_renderer, "proj_matrix");
        nLoc = gl.glGetUniformLocation(obj_renderer, "norm_matrix");

        vMat.translation(-cameraX, -cameraY, -cameraZ);
        mMat.translation(x, y, z);

        currentLightPos.set(initialLightLoc);
        installLights(vMat);

        elapsedTime = System.currentTimeMillis() - startTime;
        tf = elapsedTime / 1000.0;

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

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); //this is wrong
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]); //this is wrong
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0); //first value becomes 2 when using textures
        gl.glEnableVertexAttribArray(2); //turn to 2 when using textures
        
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]); //?
        gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);
    }

    // private void setupVertices() {
    //     GL4 gl = (GL4) GLContext.getCurrentGL();

    //     float[] topSide = {            
    //         -1.0f, 1.0f, 1.0f, 
    //         -1.0f, 1.0f, -1.0f,
    //         1.0f, 1.0f, -1.0f,

    //         -1.0f, 1.0f, 1.0f,
    //         1.0f, 1.0f, 1.0f,
    //         1.0f, 1.0f,-1.0f
    //     };

    //     float[] botSide = {
    //         -1.0f, -1.0f, 1.0f, 
    //         -1.0f, -1.0f, -1.0f,
    //         1.0f, -1.0f, -1.0f,

    //         -1.0f, -1.0f, 1.0f,
    //         1.0f, -1.0f, 1.0f,
    //         1.0f, -1.0f,-1.0f
    //     }; 
        
    //     float[] frontSide = {
    //         -1.0f, 1.0f, -1.0f, 
    //         -1.0f, -1.0f, -1.0f, 
    //         1.0f, -1.0f, -1.0f, 

    //         -1.0f, 1.0f, -1.0f, 
    //         1.0f, 1.0f, -1.0f,
    //         1.0f, -1.0f, -1.0f
    //     };

    //     float[] backSide = {
    //         1.0f, 1.0f, 1.0f, 
    //         1.0f, -1.0f, 1.0f, 
    //         -1.0f, -1.0f, 1.0f,

    //         1.0f, 1.0f, 1.0f,
    //         -1.0f, 1.0f, 1.0f,
    //         -1.0f, -1.0f, 1.0f
    //     };

    //     float[] rightSide = {
    //         1.0f, 1.0f, -1.0f, 
    //         1.0f, -1.0f, -1.0f, 
    //         1.0f, -1.0f, 1.0f,

    //         1.0f, 1.0f, -1.0f,
    //         1.0f, 1.0f, 1.0f, 
    //         1.0f, -1.0f, 1.0f
    //     };

    //     float[] leftSide = {
    //         -1.0f, 1.0f, 1.0f, 
    //         -1.0f, -1.0f, 1.0f, 
    //         -1.0f, -1.0f, -1.0f,

    //         -1.0f, 1.0f, 1.0f, 
    //         -1.0f, 1.0f, -1.0f, 
    //         -1.0f, -1.0f, -1.0f
    //     };

    //     //textures

    //     float[] texTop = {
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
    //     };

    //     float[] texBot = {
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
    //     };

    //     float[] texFront = {
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
    //     };

    //     float[] texBack = {
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
    //     };

    //     float[] texRight = {
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
            
    //     };

    //     float[] texLeft = { 
    //         0.00f, 1.00f, 
    //         0.00f, 0.00f, 
    //         1.00f, 0.00f,
            
    //         0.00f, 1.00f, 
    //         1.00f, 1.00f,
    //         1.00f, 0.00f
    //     };

    //     gl.glGenVertexArrays(vao.length, vao, 0);
    //     gl.glBindVertexArray(vao[0]);
    //     gl.glGenBuffers(vboS.length, vboS, 0);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[0]);
    //     FloatBuffer TtopBuf = Buffers.newDirectFloatBuffer(topSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TtopBuf.limit() * 4, TtopBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[1]);
    //     FloatBuffer TbotBuf = Buffers.newDirectFloatBuffer(botSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TbotBuf.limit() * 4, TbotBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[2]);
    //     FloatBuffer TfrontBuf = Buffers.newDirectFloatBuffer(frontSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TfrontBuf.limit() * 4, TfrontBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[3]);
    //     FloatBuffer TbackBuf = Buffers.newDirectFloatBuffer(backSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TbackBuf.limit() * 4, TbackBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[4]);
    //     FloatBuffer TrightBuf = Buffers.newDirectFloatBuffer(rightSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TrightBuf.limit() * 4, TrightBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[5]);
    //     FloatBuffer TleftBuf = Buffers.newDirectFloatBuffer(leftSide);
    //     gl.glBufferData(GL_ARRAY_BUFFER, TleftBuf.limit() * 4, TleftBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[6]);
    //     FloatBuffer topBuf = Buffers.newDirectFloatBuffer(texTop);
    //     gl.glBufferData(GL_ARRAY_BUFFER, topBuf.limit() * 4, topBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[7]);
    //     FloatBuffer botBuf = Buffers.newDirectFloatBuffer(texBot);
    //     gl.glBufferData(GL_ARRAY_BUFFER, botBuf.limit() * 4, botBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[8]);
    //     FloatBuffer frontBuf = Buffers.newDirectFloatBuffer(texFront);
    //     gl.glBufferData(GL_ARRAY_BUFFER, frontBuf.limit() * 4, frontBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[9]);
    //     FloatBuffer backBuf = Buffers.newDirectFloatBuffer(texBack);
    //     gl.glBufferData(GL_ARRAY_BUFFER, backBuf.limit() * 4, backBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[10]);
    //     FloatBuffer rightBuf = Buffers.newDirectFloatBuffer(texRight);
    //     gl.glBufferData(GL_ARRAY_BUFFER, rightBuf.limit() * 4, rightBuf, GL_STATIC_DRAW);

    //     gl.glBindBuffer(GL_ARRAY_BUFFER, vboS[11]);
    //     FloatBuffer leftBuf = Buffers.newDirectFloatBuffer(texLeft);
    //     gl.glBufferData(GL_ARRAY_BUFFER, leftBuf.limit() * 4, leftBuf, GL_STATIC_DRAW);
    // }

    public static void main(String[] args) {
        new Main();
    }

    public void setupBackground() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        float[ ] cubeTextureCoord ={
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

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        // gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        // FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(cubeTextureCoord);
        // gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, backgroundTexture);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, 36);

    }

    public void sphereVertices() {
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
        gl.glGenVertexArrays(vao.length, vao, 0); //this is probably wrong
        gl.glBindVertexArray(vao[1]); //???
        gl.glGenBuffers(vbo.length, vbo, 0); //????

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

    }

    private void torusVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        Torus myTorus = new Torus(0.5f, 0.3f, 48);

        int numTorusVertices = myTorus.getNumVertices();
        int numTorusIndices = myTorus.getNumIndices();

        Vector3f[] vertices = myTorus.getVertices();
        Vector2f[] texCoords = myTorus.getTexCoords();
        Vector3f[] normals = myTorus.getNormals();
        int[] indices = myTorus.getIndices();

        float[] pvalues = new float[vertices.length * 3];
        float[] tvalues = new float[texCoords.length * 2];
        float[] nvalues = new float[normals.length * 3];

        for (int i = 0; i < numTorusVertices; i++) {
            pvalues[i * 3] = (float) vertices[i].x;
            pvalues[i * 3 + 1] = (float) vertices[i].y;
            pvalues[i * 3 + 2] = (float) vertices[i].z;

            tvalues[i * 2] = (float) texCoords[i].x;
            tvalues[i * 2 + 1] = (float) texCoords[i].y;
            
            nvalues[i * 3] = (float) normals[i].x;
            nvalues[i * 3 + 1] = (float) normals[i].y;
            nvalues[i * 3 + 2] = (float) normals[i].z;
        }

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(4, vbo, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
        IntBuffer idxBuf = Buffers.newDirectIntBuffer(indices);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit() * 4, idxBuf, GL_STATIC_DRAW);
    }

    private void installLights(Matrix4f vMatrix) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        currentLightPos.mulPosition(vMatrix);
        lightPos[0] = currentLightPos.x();
        lightPos[1] = currentLightPos.y();
        lightPos[2] = currentLightPos.z();

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
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
	public void dispose(GLAutoDrawable drawable) { }

}